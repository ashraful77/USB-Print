package com.usbprint.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SfpRasterNormalizerTest {
    @Test
    fun normalizeCopiesValidRasterWithoutChangingBits() {
        val source = byteArrayOf(0x55, 0x0F)
        val page = PdfRasterizer.RasterPage(
            pageNumber = 0,
            width = 9,
            height = 2,
            bytesPerRow = 2,
            data = source
        )

        val normalized = SfpRasterNormalizer.normalize(page)

        assertEquals(9, normalized.width)
        assertEquals(2, normalized.height)
        assertEquals(2, normalized.bytesPerRow)
        assertArrayEquals(source, normalized.data)
    }

    @Test
    fun normalizeDoesNotAliasSourceBuffer() {
        val source = byteArrayOf(0x01, 0x02)
        val page = PdfRasterizer.RasterPage(0, 8, 2, 1, source)

        val normalized = SfpRasterNormalizer.normalize(page)
        source[0] = 0x7F

        assertArrayEquals(byteArrayOf(0x01, 0x02), normalized.data)
    }

    @Test(expected = IllegalArgumentException::class)
    fun normalizeRejectsBadStride() {
        SfpRasterNormalizer.normalize(
            PdfRasterizer.RasterPage(0, 9, 1, 1, byteArrayOf(0))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun normalizeAllRejectsEmptyPages() {
        SfpRasterNormalizer.normalizeAll(emptyList())
    }
}
