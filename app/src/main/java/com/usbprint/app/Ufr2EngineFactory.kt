package com.usbprint.app

/**
 * Centralizes creation of the printer engine used by the app.
 *
 * The real encoder is intentionally not selected until it is verified for
 * the LBP6030B. Until then the application uses the safe SFP placeholder
 * behind the same validation gate that a real encoder will use.
 */
object Ufr2EngineFactory {
    fun create(): Ufr2Engine =
        SfpUfr2Engine(
            ValidatedSfpEncoder(UnavailableSfpEncoder())
        )
}
