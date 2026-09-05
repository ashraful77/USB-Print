package com.usbprint.app

/**
 * Single application-level boundary for the printer-specific UFR II LT engine.
 *
 * Keeping native availability and encoding behind this interface means the
 * Android UI and USB transport do not need to know whether the implementation
 * is Kotlin, JNI, or another verified portable engine.
 */
interface Ufr2Engine {
    val isAvailable: Boolean

    fun encode(job: Ufr2Encoder.Job): Ufr2Encoder.Result
}
