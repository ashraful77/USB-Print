package com.usbprint.app

/**
 * Kotlin implementation of the extracted Canon LBP6030B SFP/HB path.
 *
 * The original Linux binaries cannot be loaded by Android directly, so the
 * printer-specific PDL/transport behavior reconstructed from those binaries
 * is implemented here as a portable encoder.
 */
class CanonSfpUfr2Engine : Ufr2Engine {
    private val encoder = Ufr2Encoder(Ufr2PrinterProfile.LBP6030B)

    override val isAvailable: Boolean
        get() = true

    override fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result =
        encoder.encode(job)
}
