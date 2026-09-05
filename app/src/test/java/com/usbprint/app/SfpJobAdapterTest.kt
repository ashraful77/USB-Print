package com.usbprint.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SfpJobAdapterTest {
    @Test
    fun convertsJobMetadataAndPageData() {
        val profile = Ufr2PrinterProfile.LBP6030B
        val source = SfpJob(
            profile = profile,
            dpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297,
            pages = listOf(
                SfpJob.SfpPage(
                    width = 8,
                    height = 2,
                    bytesPerRow = 1,
                    data = byteArrayOf(0x55, 0xAA.toByte())
                )
            )
        )

        val converted = SfpJobAdapter.toUfr2Job(source)

        assertEquals(600, converted.dpi)
        assertEquals(210, converted.paperWidthMm)
        assertEquals(297, converted.paperHeightMm)
        assertEquals(1, converted.pages.size)
        assertEquals(8, converted.pages[0].width)
        assertEquals(2, converted.pages[0].height)
        assertEquals(1, converted.pages[0].bytesPerRow)
        assertArrayEquals(byteArrayOf(0x55, 0xAA.toByte()), converted.pages[0].data)
    }

    @Test
    fun convertedDataDoesNotAliasSource() {
        val data = byteArrayOf(0x11)
        val source = SfpJob(
            profile = Ufr2PrinterProfile.LBP6030B,
            dpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297,
            pages = listOf(SfpJob.SfpPage(8, 1, 1, data))
        )

        val converted = SfpJobAdapter.toUfr2Job(source)
        data[0] = 0x22

        assertEquals(0x11, converted.pages[0].data[0].toInt())
    }
}
