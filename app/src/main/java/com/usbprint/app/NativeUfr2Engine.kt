package com.usbprint.app

/**
 * Boundary for a future native SFP/UFR II LT encoder.
 *
 * The native implementation is deliberately optional. Until a verified
 * LBP6030B-compatible implementation is supplied, this class reports that
 * the engine is unavailable and never sends data to the printer.
 */
class NativeUfr2Engine {

    val isAvailable: Boolean
        get() = nativeLoaded

    fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result {
        if (!nativeLoaded) {
            return Ufr2Encoder.Result(
                success = false,
                data = null,
                message = "Native UFR II LT engine is not available yet; no printer data was generated."
            )
        }

        return encodeNative(
            job.pages.first().data,
            job.pages.first().width,
            job.pages.first().height,
            job.dpi,
            job.paperWidthMm,
            job.paperHeightMm
        )
    }

    private external fun encodeNative(
        raster: ByteArray,
        width: Int,
        height: Int,
        dpi: Int,
        paperWidthMm: Int,
        paperHeightMm: Int
    ): Ufr2Encoder.Result

    companion object {
        private val nativeLoaded: Boolean = runCatching {
            System.loadLibrary("ufr2engine")
            true
        }.getOrDefault(false)
    }
}
