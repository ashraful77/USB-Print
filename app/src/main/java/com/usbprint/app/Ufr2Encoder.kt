package com.usbprint.app

/** Deliberately fail-closed UFR II LT encoder boundary. */
class Ufr2Encoder(private val profile: Ufr2PrinterProfile = Ufr2PrinterProfile.LBP6030B) {
    data class Job(val pages: List<PdfRasterizer.RasterPage>, val dpi: Int, val paperWidthMm: Int, val paperHeightMm: Int)
    data class Result(val success: Boolean, val data: ByteArray?, val message: String)
    fun newJob(pages: List<PdfRasterizer.RasterPage>, dpi: Int = profile.defaultDpi, paperWidthMm: Int = profile.paperWidthMm, paperHeightMm: Int = profile.paperHeightMm) = Job(pages, dpi, paperWidthMm, paperHeightMm)
    fun encode(job: Job): Result {
        val validation = SfpJobValidator.validate(job)
        if (!validation.valid) return Result(false, null, validation.message)
        return Result(false, null, "No verified redistributable UFR II LT/SFP encoder is bundled; no printer data was generated.")
    }
}