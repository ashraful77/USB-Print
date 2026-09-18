package com.usbprint.app

/** Creates the fail-closed UFR II LT engine. */
object Ufr2EngineFactory {
    fun create(): Ufr2Engine = object : Ufr2Engine {
        override val isAvailable = false
        override fun encode(job: Ufr2Encoder.Job) = Ufr2Encoder().encode(job)
    }
}