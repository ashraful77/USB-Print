package com.usbprint.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders PDF pages into packed 1-bit monochrome raster data.
 *
 * Canon's LBP6030 UFR-II LT Linux path uses an exact 4958x7016 A4 device
 * raster at 600 dpi. The printer-side HB/SLC geometry must match those rows,
 * so A4 pages are rendered to those exact dimensions rather than the generic
 * 595x842-point PDF-to-600dpi rounding (which produces slightly different
 * dimensions).
 */
class PdfRasterizer(private val context: Context) {
    data class RasterPage(
        val pageNumber: Int,
        val width: Int,
        val height: Int,
        val bytesPerRow: Int,
        val data: ByteArray
    )

    fun renderFirstPage(uri: Uri, dpi: Int = DEFAULT_DPI): RasterPage =
        renderPage(uri, 0, dpi)

    fun renderPage(uri: Uri, pageNumber: Int, dpi: Int = DEFAULT_DPI): RasterPage {
        require(dpi in 72..600) { "DPI must be between 72 and 600" }

        openDescriptor(uri).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                require(pageNumber in 0 until renderer.pageCount) {
                    "PDF page $pageNumber is outside the document"
                }
                renderer.openPage(pageNumber).use { page ->
                    val scale = dpi / 72f
                    val a4 = isA4Page(page.width, page.height)
                    val width = if (dpi == 600 && a4) CANON_A4_WIDTH else
                        (page.width * scale).roundToInt().coerceAtLeast(1)
                    val height = if (dpi == 600 && a4) CANON_A4_HEIGHT else
                        (page.height * scale).roundToInt().coerceAtLeast(1)
                    val bytesPerRow = (width + 7) / 8
                    val packed = ByteArray(bytesPerRow * height)
                    val scaleX = width.toFloat() / page.width.toFloat()
                    val scaleY = height.toFloat() / page.height.toFloat()

                    var stripeTop = 0
                    while (stripeTop < height) {
                        val stripeHeight = min(STRIPE_HEIGHT_PX, height - stripeTop)
                        val bitmap = Bitmap.createBitmap(
                            width,
                            stripeHeight,
                            Bitmap.Config.ARGB_8888
                        )
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            val transform = Matrix().apply {
                                setScale(scaleX, scaleY)
                                postTranslate(0f, -stripeTop.toFloat())
                            }
                            page.render(
                                bitmap,
                                null,
                                transform,
                                PdfRenderer.Page.RENDER_MODE_FOR_PRINT
                            )
                            packMonochrome(bitmap, packed, stripeTop, bytesPerRow)
                        } finally {
                            bitmap.recycle()
                        }
                        stripeTop += stripeHeight
                    }

                    return RasterPage(
                        pageNumber = pageNumber,
                        width = width,
                        height = height,
                        bytesPerRow = bytesPerRow,
                        data = packed
                    )
                }
            }
        }
    }

    private fun isA4Page(widthPoints: Int, heightPoints: Int): Boolean {
        val ratio = widthPoints.toFloat() / heightPoints.toFloat()
        return ratio in 0.69f..0.73f
    }

    private fun openDescriptor(uri: Uri): ParcelFileDescriptor {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = uri.path ?: throw IllegalArgumentException("Local PDF path is missing")
            return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        }
        return context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("Could not open the selected PDF")
    }

    private fun packMonochrome(
        bitmap: Bitmap,
        destination: ByteArray,
        destinationTop: Int,
        bytesPerRow: Int
    ) {
        val width = bitmap.width
        val pixels = IntArray(width)

        for (y in 0 until bitmap.height) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
            val rowOffset = (destinationTop + y) * bytesPerRow
            for (x in 0 until width) {
                val pixel = pixels[x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luminance = (299 * r + 587 * g + 114 * b) / 1000
                if (luminance < THRESHOLD) {
                    destination[rowOffset + (x ushr 3)] =
                        (destination[rowOffset + (x ushr 3)].toInt() or
                            (0x80 ushr (x and 7))).toByte()
                }
            }
        }
    }

    companion object {
        const val DEFAULT_DPI = 600
        private const val CANON_A4_WIDTH = 4958
        private const val CANON_A4_HEIGHT = 7016
        private const val STRIPE_HEIGHT_PX = 512
        private const val THRESHOLD = 180
    }
}
