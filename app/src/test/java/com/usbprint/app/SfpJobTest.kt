package com.usbprint.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SfpJobTest {
    @Test
    fun fromRasterCopiesPageDataAndPreservesMetadata() {
        val source = byteArrayOf(0x01, 0x02, 0x03)
        val page = SfpRasterNormalizer.NormalizedPage(
            width = 17,
            height = 1,
            bytesPerRow = 3,
            data = source
        )

        val job = SfpJob.fromRaster(
            profile = Ufr2PrinterProfile.LBP6030B,
            dpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297,
            pages = listOf(page)
        )

        source[0] = 0x7F
        assertEquals(Ufr2PrinterProfile.LBP6030B, job.profile)
        assertEquals(600, job.dpi)
        assertEquals(210, job.paperWidthMm)
        assertEquals(297, job.paperHeightMm)
        assertEquals(1, job.pages.size)
        assertEquals(17, job.pages[0].width)
        assertEquals(1, job.pages[0].height)
        assertEquals(3, job.pages[0].bytesPerRow)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03), job.pages[0].data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromRasterRejectsEmptyPages() {
        SfpJob.fromRaster(
            profile = Ufr2PrinterProfile.LBP6030B,
            dpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297,
            pages = emptyList()
        )
    }
}
