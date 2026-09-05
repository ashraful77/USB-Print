package com.usbprint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SfpEncoderTest {

    private fun validJob() = Ufr2Encoder.Job(
        pages = listOf(
            PdfRasterizer.RasterPage(
                pageNumber = 0,
                width = 8,
                height = 1,
                bytesPerRow = 1,
                data = ByteArray(1)
            )
        ),
        dpi = Ufr2PrinterProfile.LBP6030B.defaultDpi,
        paperWidthMm = Ufr2PrinterProfile.LBP6030B.paperWidthMm,
        paperHeightMm = Ufr2PrinterProfile.LBP6030B.paperHeightMm
    )

    @Test
    fun unavailableEncoderReportsUnavailable() {
        val encoder = UnavailableSfpEncoder()

        assertFalse(encoder.isAvailable)
    }

    @Test
    fun unavailableEncoderNeverProducesPrinterBytes() {
        val result = UnavailableSfpEncoder().encode(validJob())

        assertFalse(result.success)
        assertNull(result.data)
    }
}
