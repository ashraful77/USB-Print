package com.usbprint.app

import android.net.Uri

/**
 * Coordinates the complete print path without allowing unencoded data to reach USB.
 *
 * The pipeline is intentionally usable before the real SFP encoder exists: it
 * validates the raster job, asks the UFR II LT engine to encode it, and only then
 * transfers the returned printer bytes. A failed/unavailable encoder therefore
 * cannot accidentally print raw PDF or raster data.
 */
class Ufr2PrintPipeline(
    private val rasterizer: PdfRasterizer,
    private val engine: Ufr2Engine,
    private val usbConnection: UsbPrinterConnection
) {
    fun printFirstPage(uri: Uri): Result {
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

        val transfer = usbConnection.sendEncodedJob(encoded.data)
        return Result(transfer.success, transfer.message)
    }

    data class Result(val success: Boolean, val message: String)
}
