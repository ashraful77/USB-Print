package com.usbprint.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlin.math.roundToInt

/**
 * Renders PDF pages into packed 1-bit monochrome raster data.
 *
 * This is deliberately separate from the printer protocol. The Canon LBP6030
 * family expects UFR II LT, so the raster output must be encoded by the UFR
 * engine before it is sent to USB.
 */
class PdfRasterizer(private val context: Context) {
    data class RasterPage(
        val pageNumber: Int,
        val width: Int,
        val height: Int,
        val bytesPerRow: Int,
        val data: ByteArray
    )

    fun renderFirstPage(uri: Uri, dpi: Int = DEFAULT_DPI): RasterPage {
        return renderPage(uri, 0, dpi)
    }

    fun renderPage(uri: Uri, pageNumber: Int, dpi: Int = DEFAULT_DPI): RasterPage {
        require(dpi in 72..600) { "DPI must be between 72 and 600" }

        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Could not open the selected PDF")

        descriptor.use { pfd: ParcelFileDescriptor ->
            PdfRenderer(pfd).use { renderer ->
                require(pageNumber in 0 until renderer.pageCount) {
                    "PDF page $pageNumber is outside the document"
                }
                renderer.openPage(pageNumber).use { page ->
                    val scale = dpi / 72f
                    val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                    val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        return RasterPage(
                            pageNumber = pageNumber,
                            width = width,
                            height = height,
                            bytesPerRow = (width + 7) / 8,
                            data = packMonochrome(bitmap)
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    private fun packMonochrome(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8
        val packed = ByteArray(bytesPerRow * height)
        val pixels = IntArray(width)

        for (y in 0 until height) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            val rowOffset = y * bytesPerRow
            for (x in 0 until width) {
                val pixel = pixels[x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (299 * r + 587 * g + 114 * b) / 1000
                if (luminance < THRESHOLD) {
                    packed[rowOffset + (x ushr 3)] =
                        (packed[rowOffset + (x ushr 3)].toInt() or (0x80 ushr (x and 7))).toByte()
                }
            }
        }
        return packed
    }

    companion object {
        const val DEFAULT_DPI = 150
        private const val THRESHOLD = 180
    }
}
