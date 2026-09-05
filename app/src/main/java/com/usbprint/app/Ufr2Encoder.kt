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
class Ufr2Encoder(
    private val profile: Ufr2PrinterProfile = Ufr2PrinterProfile.LBP6030B
) {

    data class Job(
        val pages: List<PdfRasterizer.RasterPage>,
        val dpi: Int = profile.defaultDpi,
        val paperWidthMm: Int = profile.paperWidthMm,
        val paperHeightMm: Int = profile.paperHeightMm
    )

    fun encode(job: Job): Result {
        require(job.pages.isNotEmpty()) { "At least one page is required" }
        require(job.dpi in 72..600) { "DPI must be between 72 and 600" }
        require(job.paperWidthMm > 0) { "Paper width must be positive" }
        require(job.paperHeightMm > 0) { "Paper height must be positive" }

        for (page in job.pages) {
            require(page.width > 0 && page.height > 0) {
                "Raster page dimensions must be positive"
            }
            require(page.bytesPerRow == (page.width + 7) / 8) {
                "Raster bytesPerRow does not match page width"
            }
            require(page.data.size == page.bytesPerRow * page.height) {
                "Raster data size does not match page dimensions"
            }
        }

        return Result(
            success = false,
            data = null,
            message = "UFR II LT encoder for ${profile.name} is not integrated yet; no printer data was generated."
        )
    }

    data class Result(
        val success: Boolean,
        val data: ByteArray?,
        val message: String
    )
}
