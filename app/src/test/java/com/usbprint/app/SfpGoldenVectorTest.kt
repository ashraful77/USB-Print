package com.usbprint.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Deterministic golden-vector checks for the raster input that will eventually
 * be consumed by the SFP/UFR II LT encoder.
 *
 * These vectors intentionally describe only the encoder input. They are not
 * printer protocol bytes and must never be sent to the physical printer.
 */
class SfpGoldenVectorTest {

    @Test
    fun eightPixelRowsPackMostSignificantBitFirst() {
        val page = PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = 8,
            height = 2,
            bytesPerRow = 1,
            data = byteArrayOf(
                0b1010_0000.toByte(),
                0b0101_0000.toByte()
            )
        )

        assertEquals(2, page.height)
        assertEquals(1, page.bytesPerRow)
        assertArrayEquals(
            byteArrayOf(0xA0.toByte(), 0x50.toByte()),
            page.data
        )
    }

    @Test
    fun paddedFinalByteDoesNotChangeExpectedRowLength() {
        val width = 10
        val height = 1
        val bytesPerRow = (width + 7) / 8
        val page = PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = width,
            height = height,
            bytesPerRow = bytesPerRow,
            data = byteArrayOf(0xFF.toByte(), 0xC0.toByte())
        )

        assertEquals(2, page.bytesPerRow)
        assertEquals(bytesPerRow * height, page.data.size)
        assertEquals(0xFF.toByte(), page.data[0])
        assertEquals(0xC0.toByte(), page.data[1])
    }
}
