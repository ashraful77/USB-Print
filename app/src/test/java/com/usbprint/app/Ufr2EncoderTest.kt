package com.usbprint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class Ufr2EncoderTest {

    private fun page(
        width: Int = 8,
        height: Int = 1,
        bytesPerRow: Int = 1,
        dataSize: Int = 1
    ) = PdfRasterizer.RasterPage(
        pageNumber = 0,
        width = width,
        height = height,
        bytesPerRow = bytesPerRow,
        data = ByteArray(dataSize)
    )

    private fun validJob(page: PdfRasterizer.RasterPage = page()) = Ufr2Encoder.Job(
        pages = listOf(page),
        dpi = Ufr2PrinterProfile.LBP6030B.defaultDpi,
        paperWidthMm = Ufr2PrinterProfile.LBP6030B.paperWidthMm,
        paperHeightMm = Ufr2PrinterProfile.LBP6030B.paperHeightMm
    )

    @Test
    fun encoderDoesNotGenerateUnverifiedPrinterData() {
        val result = Ufr2Encoder().encode(
            Ufr2Encoder.Job(pages = listOf(page()), dpi = 150)
        )

        assertFalse(result.success)
        assertNull(result.data)
    }

    @Test
    fun sfpValidatorAcceptsWellFormedLbp6030bJob() {
        assertTrue(SfpJobValidator.validate(validJob()).valid)
    }

    @Test
    fun sfpValidatorRejectsNonDefaultDpi() {
        assertFalse(SfpJobValidator.validate(validJob().copy(dpi = 300)).valid)
    }

    @Test
    fun sfpValidatorRejectsWrongRowStride() {
        assertFalse(SfpJobValidator.validate(validJob(page(bytesPerRow = 2))).valid)
    }

    @Test
    fun sfpValidatorRejectsTruncatedRasterData() {
        assertFalse(
            SfpJobValidator.validate(
                validJob(page(width = 16, bytesPerRow = 2, dataSize = 1))
            ).valid
        )
    }

    @Test
    fun sfpValidatorRejectsEmptyPageList() {
        assertFalse(SfpJobValidator.validate(validJob().copy(pages = emptyList())).valid)
    }
}
