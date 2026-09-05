package com.usbprint.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EncodedSfpJobTest {

    @Test
    fun rejectsFailedEncoderResult() {
        val result = Ufr2Encoder.Result(
            success = false,
            data = null,
            message = "encoder unavailable"
        )

        assertNull(EncodedSfpJob.fromEncoderResult(result))
    }

    @Test
    fun rejectsSuccessfulResultWithoutBytes() {
        val result = Ufr2Encoder.Result(
            success = true,
            data = ByteArray(0),
            message = ""
        )

        assertNull(EncodedSfpJob.fromEncoderResult(result))
    }

    @Test
    fun copiesSuccessfulEncodedBytes() {
        val source = byteArrayOf(0x10, 0x20, 0x30)
        val result = Ufr2Encoder.Result(true, source, "ok")

        val job = requireNotNull(EncodedSfpJob.fromEncoderResult(result))
        source[0] = 0x7F

        assertArrayEquals(byteArrayOf(0x10, 0x20, 0x30), job.data)
    }
}
