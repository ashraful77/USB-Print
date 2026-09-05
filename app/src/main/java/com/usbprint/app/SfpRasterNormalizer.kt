package com.usbprint.app

/**
 * Normalizes the application's 1-bit raster into the deterministic form that
 * an eventual SFP encoder consumes.
 *
 * This is deliberately NOT a printer protocol encoder: the returned bytes are
 * still raster data and must never be sent directly to a printer.
 */
object SfpRasterNormalizer {
    data class NormalizedPage(
        val width: Int,
        val height: Int,
        val bytesPerRow: Int,
        val data: ByteArray
    )

    fun normalize(page: PdfRasterizer.RasterPage): NormalizedPage {
        require(page.width > 0 && page.height > 0) { "Raster page dimensions must be positive" }
        val expectedStride = (page.width + 7) / 8
        require(page.bytesPerRow == expectedStride) {
            "Raster bytesPerRow does not match page width"
        }
        require(page.data.size == page.bytesPerRow * page.height) {
            "Raster data size does not match page dimensions"
        }

        // Copy rather than aliasing the source buffer. This makes the encoder
        // boundary deterministic even if the PDF renderer reuses its buffer.
        return NormalizedPage(
            width = page.width,
            height = page.height,
            bytesPerRow = expectedStride,
            data = page.data.copyOf()
        )
    }

    fun normalizeAll(pages: List<PdfRasterizer.RasterPage>): List<NormalizedPage> {
        require(pages.isNotEmpty()) { "At least one page is required" }
        return pages.map(::normalize)
    }
}
