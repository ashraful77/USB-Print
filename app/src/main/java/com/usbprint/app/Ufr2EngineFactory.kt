package com.usbprint.app

/**
 * Centralizes creation of the printer engine used by the app.
 *
 * The native engine remains optional until a verified LBP6030B-compatible
 * encoder is integrated.
 */
object Ufr2EngineFactory {
    fun create(): Ufr2Engine = NativeUfr2Engine()
}
