package com.usbprint.app

/**
 * Adapts the SFP encoder boundary to the application-level UFR II LT engine.
 *
 * Keeping this adapter small lets the rest of the print pipeline remain
 * independent of the eventual encoder implementation (Kotlin/JNI/native).
 */
class SfpUfr2Engine(
    private val encoder: SfpEncoder
) : Ufr2Engine {
    override val isAvailable: Boolean
        get() = encoder.isAvailable

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result =
        if (!encoder.isAvailable) {
            Ufr2Encoder.Result(
                success = false,
                data = null,
                message = "Verified SFP/UFR II LT encoder is not available; no printer data was generated."
            )
        } else {
            encoder.encode(job)
        }
}
