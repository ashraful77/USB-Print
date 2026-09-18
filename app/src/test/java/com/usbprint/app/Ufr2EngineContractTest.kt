package com.usbprint.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class Ufr2EngineContractTest {
    @Test
    fun factoryCreatesUnavailableEngineBoundary() {
        val engine = Ufr2EngineFactory.create()
        assertFalse(engine.isAvailable)
    }

    @Test
    fun failClosedEngineNeverProducesPrinterBytes() {
        val page = PdfRasterizer.RasterPage(0, 8, 1, 1, byteArrayOf(0))
        val job = Ufr2Encoder().newJob(listOf(page))
        val result = Ufr2EngineFactory.create().encode(job)
        assertFalse(result.success)
        assertNull(result.data)
    }
}