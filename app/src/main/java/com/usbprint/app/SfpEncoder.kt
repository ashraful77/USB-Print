package com.usbprint.app

/**
 * Boundary for a verified Canon SFP/UFR II LT encoder.
 *
 * Implementations must return a complete printer job, not a PDF or generic
 * raster. Until an implementation is independently verified against the
 * target printer, callers must keep printing disabled.
 */
interface SfpEncoder {
    val isAvailable: Boolean

    fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result
}

/**
 * Safe placeholder used while the real encoder is being developed.
 * It deliberately emits no bytes.
 */
class UnavailableSfpEncoder : SfpEncoder {
    override val isAvailable: Boolean = false

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result =
        Ufr2Encoder.Result(
            success = false,
            data = null,
            message = "Verified SFP/UFR II LT encoder is not available; no printer data was generated."
        )
}
