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
 * #131 restores the #121 baseline 1-bit raster. PDL experiments are now
 * isolated from raster changes so the CMLP framing test is controlled.
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

                    val bytesPerRow = (WIDTH + 7) / 8
                    val raster = ByteArray(bytesPerRow * HEIGHT)
                    val row = IntArray(WIDTH)
                    for (y in 0 until HEIGHT) {
                        bitmap.getPixels(row, 0, WIDTH, 0, y, WIDTH, 1)
                        val rowBase = y * bytesPerRow
                        for (x in 0 until WIDTH) {
                            val c = row[x]
                            val r = Color.red(c)
                            val g = Color.green(c)
                            val b = Color.blue(c)
                            val gray = (0.299f * r + 0.587f * g + 0.114f * b)
                                .roundToInt().coerceIn(0, 255)
                            if (gray < 180) {
                                raster[rowBase + (x ushr 3)] =
                                    (raster[rowBase + (x ushr 3)].toInt() or
                                        (1 shl (7 - (x and 7)))).toByte()
                            }
                        }
                    }
                    bitmap.recycle()
                    return RasterPage(raster, WIDTH, HEIGHT, DPI, 1)
                }
            }
        }
    }
}
