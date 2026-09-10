package com.usbprint.app

/**
 * Native Canon LBP6030B UFR II LT/SFP engine.
 *
 * The Android JNI layer calls the ARM64 Canon SLIM compressor bundled with
 * this application, then builds the verified HB/SFP PDL and CMLP channel-1
 * transport stream. If the native encoder cannot load, this engine fails
 * closed and never sends an unverified stream to the printer.
 */
class NativeUfr2Engine : Ufr2Engine {

    override val isAvailable: Boolean
        get() = nativeLoaded

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result {
        val validation = SfpJobValidator.validate(job)
        if (!validation.valid) {
            return Ufr2Encoder.Result(
                success = false,
                data = null,
                message = validation.message
            )
        }

        if (!nativeLoaded) {
            return Ufr2Encoder.Result(
                success = false,
                data = null,
                message = "Native Canon SFP/SLIM encoder could not be loaded; no printer data was generated."
            )
        }

        val page = job.pages.singleOrNull()
            ?: return Ufr2Encoder.Result(
                success = false,
                data = null,
                message = "Native Canon SFP engine currently accepts one raster page at a time"
            )

        return runCatching {
            encodeNative(
                page.data,
                page.width,
                page.height,
                job.dpi,
                job.paperWidthMm,
                job.paperHeightMm
            )
        }.getOrElse { error ->
            Ufr2Encoder.Result(
                success = false,
                data = null,
                message = "Native Canon SFP encoder failed safely: ${error.message ?: error.javaClass.simpleName}"
            )
        }
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
