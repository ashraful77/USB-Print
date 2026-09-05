package com.usbprint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class Ufr2EncoderTest {

    @Test
    fun encoderDoesNotGenerateUnverifiedPrinterData() {
        val page = PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = 8,
            height = 1,
            bytesPerRow = 1,
            data = byteArrayOf(0)
        )

        val result = Ufr2Encoder().encode(
            Ufr2Encoder.Job(pages = listOf(page), dpi = 150)
        )

        assertFalse(result.success)
        assertNull(result.data)
    }
}
