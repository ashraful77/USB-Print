package com.usbprint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Ufr2EngineContractTest {

    @Test
    fun factoryCreatesNativeEngineBoundary() {
        val engine = Ufr2EngineFactory.create()

        assertTrue(engine is NativeUfr2Engine)
    }

    @Test
    fun unavailableNativeEngineNeverProducesPrinterBytes() {
        val page = rasterPage()
        val job = Ufr2Encoder().newJob(listOf(page))
        val engine = NativeUfr2Engine()

        if (!engine.isAvailable) {
            val result = engine.encode(job)

            assertFalse(result.success)
            assertNull(result.data)
        }
    }

    @Test
    fun nativeEngineRejectsInvalidSfpJobBeforeEncoding() {
        val page = rasterPage()
        val invalidJob = Ufr2Encoder.Job(
            pages = listOf(page),
            dpi = 300,
            paperWidthMm = 210,
            paperHeightMm = 297
        )
        val result = NativeUfr2Engine().encode(invalidJob)

        assertFalse(result.success)
        assertNull(result.data)
        assertTrue(result.message.contains("600 DPI"))
    }

    private fun rasterPage(): PdfRasterizer.RasterPage =
        PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = 8,
            height = 1,
            bytesPerRow = 1,
            data = byteArrayOf(0)
        )
}
