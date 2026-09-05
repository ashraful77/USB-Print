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
        val page = PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = 8,
            height = 1,
            bytesPerRow = 1,
            data = byteArrayOf(0)
        )
        val job = Ufr2Encoder().newJob(listOf(page))
        val engine = NativeUfr2Engine()

        if (!engine.isAvailable) {
            val result = engine.encode(job)

            assertFalse(result.success)
            assertNull(result.data)
        }
    }
}
