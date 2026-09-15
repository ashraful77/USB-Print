package com.usbprint.app

import android.content.Context
import android.net.Uri
import java.io.File

class Ufr2PrintPipeline(
    private val rasterizer: PdfRasterizer,
    private val engine: Ufr2Engine,
    private val usbConnection: UsbPrinterConnection
) {
    fun printFirstPage(uri: Uri, sourceBytes: Long? = null): Result {
        if (!usbConnection.isOpen) return Result(false, "Printer is not connected.")
        if (!engine.isAvailable) return Result(false, "UFR II LT engine is not available; no printer data was sent.")
        val page = runCatching {
            val file = when (uri.scheme) {
                "file" -> File(uri.path ?: error("Invalid file URI"))
                "content" -> error("PDF content URI requires a temporary-file rasterizer context")
                else -> error("Unsupported PDF URI scheme: ${uri.scheme}")
            }
            rasterizer.renderFirstPage(file)
        }.getOrElse { return Result(false, "Could not render PDF: ${it.message ?: "unknown error"}") }
        val encoder = Ufr2Encoder(Ufr2PrinterProfile.LBP6030B)
        val job = encoder.newJob(listOf(page), Ufr2PrinterProfile.LBP6030B.defaultDpi, Ufr2PrinterProfile.LBP6030B.paperWidthMm, Ufr2PrinterProfile.LBP6030B.paperHeightMm)
        val validation = SfpJobValidator.validate(job)
        if (!validation.valid) return Result(false, validation.message)
        val encoded = engine.encode(job)
        if (!encoded.success || encoded.data == null || encoded.data.isEmpty()) return Result(false, encoded.message)
        val transfer = usbConnection.sendEncodedJob(encoded.data, sourceBytes = sourceBytes)
        return Result(transfer.success, buildString { append(transfer.message); if (encoded.message.isNotBlank()) { append("\n\nEncoder diagnostics:\n"); append(encoded.message) } })
    }
    data class Result(val success:Boolean,val message:String)
}
