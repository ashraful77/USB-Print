package com.usbprint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Ufr2PrinterProfileTest {

    @Test
    fun identifiesObservedLbp6030bUsbId() {
        val profile = Ufr2PrinterProfile.fromUsbIds(0x04A9, 0x2795)

        requireNotNull(profile)
        assertEquals("Canon imageCLASS LBP6030B", profile.name)
        assertEquals(600, profile.defaultDpi)
        assertEquals(210, profile.paperWidthMm)
        assertEquals(297, profile.paperHeightMm)
    }

    @Test
    fun rejectsUnknownUsbId() {
        assertNull(Ufr2PrinterProfile.fromUsbIds(0x1234, 0x5678))
    }
}
