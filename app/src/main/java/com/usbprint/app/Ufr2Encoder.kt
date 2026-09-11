package com.usbprint.app

import java.io.ByteArrayOutputStream

/**
 * Canon LBP6030B UFR II LT / SFP HB encoder.
 *
 * #127 restores #121's 28 x 256-line HB bands and MSB-first 2-bit packing,
 * then tests the natural nested finalization order: end-page, end-media, end-job.
 */
class Ufr2Encoder(
    private val profile: Ufr2PrinterProfile = Ufr2PrinterProfile.LBP6030B
) {
    companion object {
        private const val COMMAND_CHUNK = 0x1000
        private const val STRIPE_LINES = 256

        private fun buildCmlpFrame(payload: ByteArray): ByteArray {
            val totalLength = payload.size + 6
            require(totalLength <= 0xFFFF)
            return ByteArray(totalLength).also { out ->
                out[0] = 0x01
                out[1] = 0x10
                out[2] = ((totalLength ushr 8) and 0xFF).toByte()
                out[3] = (totalLength and 0xFF).toByte()
                out[4] = 0x01
                out[5] = 0x00
                System.arraycopy(payload, 0, out, 6, payload.size)
            }
        }

        private fun appendRawPdl(out: ByteArrayOutputStream, data: ByteArray) {
            var offset = 0
            while (offset < data.size) {
                val count = minOf(COMMAND_CHUNK, data.size - offset)
                out.write(buildCmlpFrame(data.copyOfRange(offset, offset + count)))
                offset += count
            }
        }

        private fun u16(value: Int): ByteArray = byteArrayOf(
            ((value ushr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )

        private fun beginJob(dpi: Int): ByteArray = ByteArrayOutputStream().apply {
            write(byteArrayOf(0x01, 0xC1.toByte(), 0x85.toByte()))
            write(u16(dpi))
            write(u16(dpi))
            write(byteArrayOf(
                0xC2.toByte(), 0x00,
                0xD8.toByte(), 0x84.toByte(), 0x00, 0x01,
                0xDD.toByte(), 0x80.toByte(),
                0xC8.toByte(), 0xF0.toByte(),
                0x84.toByte(), 0x08, 0x00, 0x02
            ))
        }.toByteArray()

        private fun beginMedia(): ByteArray = hex("02 C3 7F F1 85 00 00 00 00 C5 00 C6 00")
        private fun setPaperSource(): ByteArray = hex("51 F2 00")

        private fun beginPage(width: Int, height: Int): ByteArray = ByteArrayOutputStream().apply {
            write(byteArrayOf(0x03, 0xE7.toByte(), 0x85.toByte()))
            write(u16(width))
            write(u16(height))
            write(byteArrayOf(
                0xDE.toByte(), 0x80.toByte(), 0x00,
                0xC8.toByte(), 0x00,
                0xCA.toByte(), 0xA1.toByte(), 0x00, 0x00,
                0xCB.toByte(), 0x00
            ))
        }.toByteArray()

        private fun prepareHalftone(): ByteArray = hex("61 E6 80 02 E5 00")

        private fun transferHeader(width: Int, lines: Int, dataLength: Int): ByteArray = ByteArrayOutputStream().apply {
            write(byteArrayOf(0x62, 0xE3.toByte(), 0x85.toByte()))
            write(u16(width))
            write(u16(lines))
            write(byteArrayOf(0xE8.toByte(), 0xA5.toByte()))
            write(u16(width))
            write(u16(lines))
            write(byteArrayOf(0xE1.toByte(), 0x00, 0xD7.toByte()))
            if (dataLength <= 0xFFFF) {
                write(0x84)
                write(u16(dataLength))
                write(0x9D)
                write(u16(dataLength))
            } else {
                write(0x88)
                write(byteArrayOf(
                    ((dataLength ushr 24) and 0xFF).toByte(),
                    ((dataLength ushr 16) and 0xFF).toByte(),
                    ((dataLength ushr 8) and 0xFF).toByte(),
                    (dataLength and 0xFF).toByte()
                ))
                write(0x9E)
                write(byteArrayOf(
                    ((dataLength ushr 24) and 0xFF).toByte(),
                    ((dataLength ushr 16) and 0xFF).toByte(),
                    ((dataLength ushr 8) and 0xFF).toByte(),
                    (dataLength and 0xFF).toByte()
                ))
            }
        }.toByteArray()

        private fun oneBitToTwoBit(page: PdfRasterizer.RasterPage): ByteArray {
            val width = page.width
            val height = page.height
            val srcBytesPerRow = page.bytesPerRow
            val dstBytesPerRow = (width + 3) / 4
            val dst = ByteArray(dstBytesPerRow * height)
            for (y in 0 until height) {
                val srcBase = y * srcBytesPerRow
                val dstBase = y * dstBytesPerRow
                for (x in 0 until width) {
                    val source = page.data[srcBase + (x ushr 3)].toInt() and 0xFF
                    val black = ((source ushr (7 - (x and 7))) and 1) != 0
                    val value = if (black) 0 else 3
                    val shift = 6 - ((x and 3) * 2)
                    val index = dstBase + (x ushr 2)
                    dst[index] = (dst[index].toInt() or (value shl shift)).toByte()
                }
            }
            return dst
        }

        private fun hex(value: String): ByteArray {
            val clean = value.replace(" ", "")
            require(clean.length % 2 == 0)
            return ByteArray(clean.length / 2) { i ->
                ((Character.digit(clean[i * 2], 16) shl 4) or Character.digit(clean[i * 2 + 1], 16)).toByte()
            }
        }
    }

    data class Job(val pages: List<PdfRasterizer.RasterPage>, val dpi: Int, val paperWidthMm: Int, val paperHeightMm: Int)
    data class Result(val success: Boolean, val data: ByteArray?, val message: String)

    fun newJob(
        pages: List<PdfRasterizer.RasterPage>,
        dpi: Int = profile.defaultDpi,
        paperWidthMm: Int = profile.paperWidthMm,
        paperHeightMm: Int = profile.paperHeightMm
    ): Job = Job(pages, dpi, paperWidthMm, paperHeightMm)

    fun encode(job: Job): Result = runCatching {
        require(job.pages.size == 1) { "The current Canon SFP path supports one page per USB job." }
        require(job.dpi in 72..600) { "DPI must be between 72 and 600" }
        require(job.paperWidthMm > 0 && job.paperHeightMm > 0) { "Paper dimensions must be positive" }
        require(profile == Ufr2PrinterProfile.LBP6030B) { "Unsupported Canon SFP profile" }

        val page = job.pages.single()
        require(page.width > 0 && page.height > 0)
        require(page.bytesPerRow == (page.width + 7) / 8)
        require(page.data.size == page.bytesPerRow * page.height)
        require(page.width <= 0xFFFF && page.height <= 0xFFFF)

        val twoBit = oneBitToTwoBit(page)
        val bytesPerLine = (page.width + 3) / 4
        val stream = ByteArrayOutputStream()

        appendRawPdl(stream, beginJob(job.dpi))
        appendRawPdl(stream, beginMedia())
        appendRawPdl(stream, setPaperSource())
        appendRawPdl(stream, beginPage(page.width, page.height))
        appendRawPdl(stream, prepareHalftone())

        var lineStart = 0
        var bands = 0
        while (lineStart < page.height) {
            val lines = minOf(STRIPE_LINES, page.height - lineStart)
            val dataLength = bytesPerLine * lines
            appendRawPdl(stream, transferHeader(page.width, lines, dataLength))
            val start = lineStart * bytesPerLine
            val end = start + dataLength
            appendRawPdl(stream, twoBit.copyOfRange(start, end))
            lineStart += lines
            bands++
        }

        // Natural nesting: close the page, then media, then job.
        appendRawPdl(stream, byteArrayOf(0x11))
        appendRawPdl(stream, byteArrayOf(0x12))
        appendRawPdl(stream, byteArrayOf(0x13))

        val result = stream.toByteArray()
        Result(true, result, "Canon LBP6030B HB raw 2-bit MSB-FIRST stream: ${result.size} bytes; PDL transfer blocks=$bands; ${page.width}x${page.height} @ ${job.dpi} DPI; raster2=${twoBit.size} bytes; CMLP payload=8192 (baseline #121 transport); end=11-12-13")
    }.getOrElse { error ->
        Result(false, null, "Canon HB encoder failed: ${error.message ?: error.javaClass.simpleName}")
    }
}
