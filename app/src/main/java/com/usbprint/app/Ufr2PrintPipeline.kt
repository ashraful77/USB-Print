package com.usbprint.app

import android.net.Uri

/**
 * Coordinates the complete print path without allowing unencoded data to reach USB.
 *
 * The pipeline renders the first PDF page, encodes it with the Canon SFP/HB engine,
 * and only then transfers the generated printer bytes.
 */
class Ufr2PrintPipeline(
    private val rasterizer: PdfRasterizer,
    private val engine: Ufr2Engine,
    private val usbConnection: UsbPrinterConnection
) {
    fun printFirstPage(uri: Uri, sourceBytes: Long? = null): Result {
        if (!usbConnection.isOpen) {
            return Result(false, "Printer is not connected.")
        }
        if (!engine.isAvailable) {
            return Result(false, "UFR II LT engine is not available; no printer data was sent.")
        }

        val page = runCatching {
            rasterizer.renderFirstPage(uri, Ufr2PrinterProfile.LBP6030B.defaultDpi)
        }.getOrElse { error ->
            return Result(false, "Could not render PDF: ${error.message ?: "unknown error"}")
        }

        val encoder = Ufr2Encoder(Ufr2PrinterProfile.LBP6030B)
        val job = encoder.newJob(
            pages = listOf(page),
            dpi = Ufr2PrinterProfile.LBP6030B.defaultDpi,
            paperWidthMm = Ufr2PrinterProfile.LBP6030B.paperWidthMm,
            paperHeightMm = Ufr2PrinterProfile.LBP6030B.paperHeightMm
        )

        val validation = SfpJobValidator.validate(job)
        if (!validation.valid) {
            return Result(false, validation.message)
        }

        val encoded = engine.encode(job)
        if (!encoded.success || encoded.data == null || encoded.data.isEmpty()) {
            return Result(false, encoded.message)
        }

        val transfer = usbConnection.sendEncodedJob(encoded.data, sourceBytes = sourceBytes)
        return Result(
            transfer.success,
            buildString {
                append(transfer.message)
                if (encoded.message.isNotBlank()) {
                    append("\n\nEncoder diagnostics:\n")
                    append(encoded.message)
                }
            }
        )
    }

    data class Result(val success: Boolean, val message: String)
}
