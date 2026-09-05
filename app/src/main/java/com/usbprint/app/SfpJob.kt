package com.usbprint.app

/**
 * Printer-engine input after PDF rasterization and validation.
 *
 * This model deliberately carries raster data separately from encoded printer
 * bytes. It is not itself a wire protocol and must never be sent to USB.
 */
data class SfpJob(
    val profile: Ufr2PrinterProfile,
    val dpi: Int,
    val paperWidthMm: Int,
    val paperHeightMm: Int,
    val pages: List<SfpPage>
) {
    data class SfpPage(
        val width: Int,
        val height: Int,
        val bytesPerRow: Int,
        val data: ByteArray
    )

    companion object {
        fun fromRaster(
            profile: Ufr2PrinterProfile,
            dpi: Int,
            paperWidthMm: Int,
            paperHeightMm: Int,
            pages: List<SfpRasterNormalizer.NormalizedPage>
        ): SfpJob {
            require(pages.isNotEmpty()) { "SFP job must contain at least one page" }
            return SfpJob(
                profile = profile,
                dpi = dpi,
                paperWidthMm = paperWidthMm,
                paperHeightMm = paperHeightMm,
                pages = pages.map { page ->
                    SfpPage(
                        width = page.width,
                        height = page.height,
                        bytesPerRow = page.bytesPerRow,
                        data = page.data.copyOf()
                    )
                }
            )
        }
    }
}
