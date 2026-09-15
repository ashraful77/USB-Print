package com.usbprint.app

import android.content.Context
import android.net.Uri
import java.io.File

class Ufr2PrintPipeline(
    private val context: Context,
    private val rasterizer: PdfRasterizer,
    private val engine: Ufr2Engine,
    private val usbConnection: UsbPrinterConnection
) {
    fun printFirstPage(uri: Uri, sourceBytes: Long? = null): Result {
        if (!usbConnection.isOpen) return Result(false, "Printer is not connected.")
        if (!engine.isAvailable) return Result(false, "UFR II LT engine is not available; no printer data was sent.")
        var tempFile: File? = null
        val page = runCatching {
            val file = if (uri.scheme == "file") File(uri.path ?: error("Invalid file URI")) else {
                tempFile = File.createTempFile("usb_print_", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input -> tempFile!!.outputStream().use { output -> input.copyTo(output) } } ?: error("Could not open selected PDF")
                tempFile!!
            }
            rasterizer.renderFirstPage(file)
        }.getOrElse { error -> tempFile?.delete(); return Result(false, "Could not render PDF: ${error.message ?: "unknown error"}") }
        tempFile?.delete()
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
