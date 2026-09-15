package com.usbprint.app

/**
 * Integration boundary for a future independently sourced UFR II LT encoder.
 *
 * This class intentionally does not generate printer protocol bytes. Until a
 * redistributable encoder is available, encoding fails closed.
 */
class Ufr2Encoder(private val profile: Ufr2PrinterProfile = Ufr2PrinterProfile.LBP6030B) {
    data class Job(
        val pages: List<PdfRasterizer.RasterPage>,
        val dpi: Int,
        val paperWidthMm: Int,
        val paperHeightMm: Int
    )

    data class Result(
        val success: Boolean,
        val data: ByteArray?,
        val message: String
    )

    fun newJob(
        pages: List<PdfRasterizer.RasterPage>,
        dpi: Int = profile.defaultDpi,
        paperWidthMm: Int = profile.paperWidthMm,
        paperHeightMm: Int = profile.paperHeightMm
    ): Job = Job(pages, dpi, paperWidthMm, paperHeightMm)

    fun encode(job: Job): Result = Result(
        success = false,
        data = null,
        message = "No independently sourced UFR II LT encoder is available; no printer data was generated."
    )
}
