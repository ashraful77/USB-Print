package com.usbprint.app

/**
 * Canon UFR II LT encoder boundary.
 *
 * The LBP6030 family does not accept a PDF or a generic 1-bit raster stream
 * directly. A printer-specific UFR II LT job must be produced first.
 *
 * This class intentionally does not emit guessed protocol bytes. The encoder
 * will be backed by a verified, compatible implementation before the app sends
 * anything to the physical printer.
 */
class Ufr2Encoder {

    data class Job(
        val pages: List<PdfRasterizer.RasterPage>,
        val dpi: Int,
        val paperWidthMm: Int = 210,
        val paperHeightMm: Int = 297
    )

    fun encode(job: Job): Result {
        require(job.pages.isNotEmpty()) { "At least one page is required" }
        require(job.dpi in 72..600) { "DPI must be between 72 and 600" }

        return Result(
            success = false,
            data = null,
            message = "UFR II LT encoder is not integrated yet; no printer data was generated."
        )
    }

    data class Result(
        val success: Boolean,
        val data: ByteArray?,
        val message: String
    )
}
