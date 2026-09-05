package com.usbprint.app

/**
 * Type-safe boundary for bytes that have already been produced by a verified
 * SFP/UFR II LT encoder.
 *
 * Keeping this separate from ByteArray prevents the USB layer from being used
 * accidentally with PDF or raw raster data.
 */
class EncodedSfpJob private constructor(
    val data: ByteArray
) {
    init {
        require(data.isNotEmpty()) { "Encoded SFP job must not be empty" }
    }

    companion object {
        fun fromEncoderResult(result: Ufr2Encoder.Result): EncodedSfpJob? {
            if (!result.success) return null
            val bytes = result.data ?: return null
            if (bytes.isEmpty()) return null
            return EncodedSfpJob(bytes.copyOf())
        }
    }
}
