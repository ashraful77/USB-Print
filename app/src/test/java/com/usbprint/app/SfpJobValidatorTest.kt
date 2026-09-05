package com.usbprint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SfpJobValidatorTest {

    @Test
    fun acceptsValidLbp6030bRasterJob() {
        val page = rasterPage(width = 16, height = 8, bytes = 16)
        val job = Ufr2Encoder.Job(
            pages = listOf(page),
            dpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297
        )

        val result = SfpJobValidator.validate(job)

        assertTrue(result.valid)
    }

    @Test
    fun rejectsWrongDpi() {
        val job = Ufr2Encoder.Job(
            pages = listOf(rasterPage(16, 8, 16)),
            dpi = 300,
            paperWidthMm = 210,
            paperHeightMm = 297
        )

        assertFalse(SfpJobValidator.validate(job).valid)
    }

    @Test
    fun rejectsInvalidRasterSize() {
        val job = Ufr2Encoder.Job(
            pages = listOf(rasterPage(16, 8, 15)),
            dpi = 600,
            paperWidthMm = 210,
            paperHeightMm = 297
        )

        assertFalse(SfpJobValidator.validate(job).valid)
    }

    private fun rasterPage(width: Int, height: Int, bytes: Int): PdfRasterizer.RasterPage =
        PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = width,
            height = height,
            bytesPerRow = (width + 7) / 8,
            data = ByteArray(bytes)
        )
}
