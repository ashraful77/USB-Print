package com.usbprint.app

/**
 * Validates the raster job before it reaches the future SFP/UFR II LT encoder.
 *
 * This class deliberately knows nothing about Canon wire-format bytes. Its job is
 * to reject malformed or unsupported input early so the eventual native encoder
 * receives a well-defined LBP6030B print job.
 */
object SfpJobValidator {

    fun validate(
        job: Ufr2Encoder.Job,
        profile: Ufr2PrinterProfile = Ufr2PrinterProfile.LBP6030B
    ): Result {
        if (profile.driverFamily != Ufr2PrinterProfile.DriverFamily.SFP) {
            return Result(false, "Printer profile is not an SFP profile")
        }
        if (job.pages.isEmpty()) {
            return Result(false, "At least one page is required")
        }
        if (job.dpi != profile.defaultDpi) {
            return Result(false, "SFP profile requires ${profile.defaultDpi} DPI")
        }
        if (job.paperWidthMm != profile.paperWidthMm ||
            job.paperHeightMm != profile.paperHeightMm
        ) {
            return Result(false, "SFP profile requires ${profile.paperWidthMm}x${profile.paperHeightMm} mm paper")
        }

        for (page in job.pages) {
            if (page.width <= 0 || page.height <= 0) {
                return Result(false, "Raster page dimensions must be positive")
            }
            val expectedBytesPerRow = (page.width + 7) / 8
            if (page.bytesPerRow != expectedBytesPerRow) {
                return Result(false, "Raster bytesPerRow does not match page width")
            }
            if (page.data.size != page.bytesPerRow * page.height) {
                return Result(false, "Raster data size does not match page dimensions")
            }
        }

        return Result(true, "SFP job input is valid; no printer bytes were generated")
    }

    data class Result(val valid: Boolean, val message: String)
}
