package com.usbprint.app

import android.net.Uri

/**
 * Printer protocol boundary.
 *
 * PDF rendering is independent of the Canon protocol. A concrete implementation
 * of this interface will encode RasterPage data as UFR II LT and send it through
 * UsbPrinterConnection. Keeping this boundary explicit prevents accidentally
 * sending a PDF or generic raster bytes directly to a UFR II LT printer.
 */
interface PrintEngine {
    fun printPdf(uri: Uri): Result

    data class Result(val success: Boolean, val message: String)
}
