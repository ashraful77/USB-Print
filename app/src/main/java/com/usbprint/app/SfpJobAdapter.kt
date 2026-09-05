package com.usbprint.app

/**
 * Compatibility adapter between the explicit SfpJob model and the existing
 * Ufr2Encoder boundary.
 *
 * This adapter performs only structural conversion. It does not create or
 * transmit printer protocol bytes. The real SFP encoder remains responsible
 * for turning a validated SfpJob into UFR II LT output.
 */
object SfpJobAdapter {
    fun toUfr2Job(job: SfpJob): Ufr2Encoder.Job {
        require(job.pages.isNotEmpty()) { "SFP job must contain at least one page" }
        return Ufr2Encoder.Job(
            pages = job.pages.map { page ->
                PdfRasterizer.RasterPage(
                    width = page.width,
                    height = page.height,
                    bytesPerRow = page.bytesPerRow,
                    data = page.data.copyOf()
                )
            },
            dpi = job.dpi,
            paperWidthMm = job.paperWidthMm,
            paperHeightMm = job.paperHeightMm
        )
    }
}
