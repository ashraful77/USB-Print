package com.usbprint.app

/**
 * Placeholder for a future independently sourced UFR II LT encoder.
 *
 * No Canon proprietary binary is loaded or bundled. Until a redistributable,
 * independently implemented encoder is available, this engine fails closed
 * and generates no printer data.
 */
class NativeUfr2Engine : Ufr2Engine {

    override val isAvailable: Boolean = false

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result {
        return Ufr2Encoder.Result(
            success = false,
            data = null,
            message = "No redistributable UFR II LT encoder is available; no printer data was generated."
        )
    }
}
