package com.usbprint.app

/** Creates the portable Canon LBP6030B UFR II LT/SFP engine. */
object Ufr2EngineFactory {
    fun create(): Ufr2Engine = CanonSfpUfr2Engine()
}
