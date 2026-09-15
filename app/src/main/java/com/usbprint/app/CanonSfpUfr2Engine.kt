package com.usbprint.app

/**
 * Reserved integration point for a future independently sourced encoder.
 *
 * Canon-specific protocol reconstruction is intentionally not implemented
 * here. The engine therefore fails closed and produces no printer data.
 */
class CanonSfpUfr2Engine : Ufr2Engine {
    override val isAvailable: Boolean = false

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result =
        Ufr2Encoder.Result(
            success = false,
            data = null,
            message = "No independently sourced UFR II LT encoder is available; no printer data was generated."
        )
}
