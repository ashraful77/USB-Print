package com.usbprint.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.roundToInt

/**
 * Canon LBP6030B raster input.
 *
 * #129: render an 8-bit grayscale raster instead of thresholding to 1-bit.
 * Canon's SFP/CUPS pipeline receives grayscale raster data before the HB
 * output-depth conversion. The native encoder performs the controlled
 * grayscale -> 2-bit conversion.
 */
object PdfRasterizer {
    const val WIDTH = 4958
    const val HEIGHT = 7016
    const val DPI = 600

    data class RasterPage(
        val data: ByteArray,
        val width: Int,
        val height: Int,
        val dpi: Int,
        val bitsPerPixel: Int
    )

    fun renderFirstPage(pdfFile: File): RasterPage {
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(renderer.pageCount > 0) { "PDF contains no pages" }
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    val grayscale = ByteArray(WIDTH * HEIGHT)
                    val row = IntArray(WIDTH)
                    for (y in 0 until HEIGHT) {
                        bitmap.getPixels(row, 0, WIDTH, 0, y, WIDTH, 1)
                        val rowBase = y * WIDTH
                        for (x in 0 until WIDTH) {
                            val c = row[x]
                            // Standard luminance conversion; preserve 8-bit
                            // grayscale rather than applying a binary threshold.
                            val r = Color.red(c)
                            val g = Color.green(c)
                            val b = Color.blue(c)
                            grayscale[rowBase + x] =
                                (0.299f * r + 0.587f * g + 0.114f * b).roundToInt().coerceIn(0, 255).toByte()
                        }
                    }
                    bitmap.recycle()
                    return RasterPage(grayscale, WIDTH, HEIGHT, DPI, 8)
                }
            }
        }
    }
}
