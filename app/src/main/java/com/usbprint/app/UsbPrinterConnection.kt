package com.usbprint.app

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager

class UsbPrinterConnection(private val usbManager: UsbManager) {
    data class Result(val success: Boolean, val message: String, val interfaceNumber: Int? = null, val endpointSummary: String = "")
    data class CommunicationResult(val success: Boolean, val message: String)
    data class PrintTransferResult(val success: Boolean, val message: String, val bytesSent: Int = 0)

    private var connection: UsbDeviceConnection? = null
    private var printerInterface: UsbInterface? = null
    private var outEndpoint: UsbEndpoint? = null
    private var inEndpoint: UsbEndpoint? = null

    fun open(device: UsbDevice): Result {
        close()
        if (!usbManager.hasPermission(device)) return Result(false, "USB permission is required before connecting.")
        for (i in 0 until device.interfaceCount) {
            val candidate = device.getInterface(i)
            if (candidate.interfaceClass != UsbConstants.USB_CLASS_PRINTER) continue
            var candidateOut: UsbEndpoint? = null
            var candidateIn: UsbEndpoint? = null
            for (e in 0 until candidate.endpointCount) {
                val endpoint = candidate.getEndpoint(e)
                if (endpoint.type != UsbConstants.USB_ENDPOINT_XFER_BULK) continue
                if (endpoint.direction == UsbConstants.USB_DIR_OUT && candidateOut == null) candidateOut = endpoint
                else if (endpoint.direction == UsbConstants.USB_DIR_IN && candidateIn == null) candidateIn = endpoint
            }
            if (candidateOut == null) continue
            val opened = usbManager.openDevice(device) ?: return Result(false, "Android could not open the USB device.")
            if (!opened.claimInterface(candidate, true)) {
                opened.close()
                return Result(false, "Could not claim the Canon printer USB interface.")
            }
            connection = opened
            printerInterface = candidate
            outEndpoint = candidateOut
            inEndpoint = candidateIn
            return Result(true, "USB printer interface connected ✓", candidate.id, buildString {
                append("OUT endpoint: ").append(formatEndpoint(candidateOut))
                append("\nIN endpoint: ").append(candidateIn?.let { formatEndpoint(it) } ?: "none")
            })
        }
        return Result(false, "No USB printer interface with a bulk OUT endpoint was found.")
    }

    fun testCommunication(): CommunicationResult {
        val currentConnection = connection ?: return CommunicationResult(false, "Printer is not connected.")
        val currentInterface = printerInterface ?: return CommunicationResult(false, "Printer interface is not available.")
        val buffer = ByteArray(1)
        val transferred = currentConnection.controlTransfer(0xA1, 0x01, 0, currentInterface.id, buffer, 1, 1500)
        if (transferred == 1) {
            val status = buffer[0].toInt() and 0xFF
            val selected = (status and 0x01) != 0
            val noError = (status and 0x08) != 0
            val paperEmpty = (status and 0x20) != 0
            return CommunicationResult(true, buildString {
                append("Printer responded ✓")
                append("\nUSB status: 0x").append(status.toString(16).padStart(2, '0').uppercase())
                if (selected) append("\nPrinter selected")
                if (noError) append("\nNo printer error reported")
                if (paperEmpty) append("\nPaper-out status reported")
            })
        }
        return CommunicationResult(false, "Printer did not respond to the USB Printer Class status request.")
    }

    /** Sends an already-encoded Canon job and then checks both USB status and the IN endpoint. */
    fun sendEncodedJob(data: ByteArray, timeoutMs: Int = DEFAULT_TRANSFER_TIMEOUT_MS): PrintTransferResult {
        val currentConnection = connection ?: return PrintTransferResult(false, "Printer is not connected.")
        val endpoint = outEndpoint ?: return PrintTransferResult(false, "Printer bulk OUT endpoint is not available.")
        if (data.isEmpty()) return PrintTransferResult(false, "Encoded printer job is empty.")
        require(timeoutMs > 0) { "USB transfer timeout must be positive" }

        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(DEFAULT_TRANSFER_CHUNK_BYTES, data.size - offset)
            val transferred = currentConnection.bulkTransfer(endpoint, data, offset, chunkSize, timeoutMs)
            if (transferred != chunkSize) {
                return PrintTransferResult(false, "USB print transfer stopped after $offset bytes (expected $chunkSize, sent $transferred).", offset)
            }
            offset += transferred
        }

        val status = readPrinterClassStatus()
        val inResult = readImmediatePrinterResponse()
        val message = buildString {
            append("USB transfer completed ✓ ($offset bytes)")
            append("\n").append(status)
            append("\n").append(inResult)
            append("\nIf the printer remains idle, the USB transport is working but the Canon HB stream is still being rejected; use the next diagnostic result to refine the PDL.")
        }
        return PrintTransferResult(true, message, offset)
    }

    private fun readPrinterClassStatus(): String {
        val currentConnection = connection ?: return "USB status unavailable"
        val currentInterface = printerInterface ?: return "USB status unavailable"
        val buffer = ByteArray(1)
        val n = currentConnection.controlTransfer(0xA1, 0x01, 0, currentInterface.id, buffer, 1, 1000)
        if (n != 1) return "USB Printer Class status: no response"
        return "USB Printer Class status: 0x${(buffer[0].toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()}"
    }

    private fun readImmediatePrinterResponse(): String {
        val currentConnection = connection ?: return "IN endpoint: unavailable"
        val endpoint = inEndpoint ?: return "IN endpoint: unavailable"
        val buffer = ByteArray(endpoint.maxPacketSize.coerceAtLeast(64))
        val n = currentConnection.bulkTransfer(endpoint, buffer, 0, buffer.size, 1000)
        return when {
            n > 0 -> "IN endpoint response: $n byte(s) ${buffer.copyOf(n).joinToString(" ") { "%02X".format(it) }}"
            n == 0 -> "IN endpoint response: empty"
            else -> "IN endpoint response: none within timeout"
        }
    }

    fun close() {
        val currentConnection = connection
        val currentInterface = printerInterface
        if (currentConnection != null && currentInterface != null) runCatching { currentConnection.releaseInterface(currentInterface) }
        runCatching { currentConnection?.close() }
        connection = null
        printerInterface = null
        outEndpoint = null
        inEndpoint = null
    }

    val isOpen: Boolean get() = connection != null && outEndpoint != null
    private fun formatEndpoint(endpoint: UsbEndpoint): String = "0x${endpoint.address.toString(16).uppercase()} (${endpoint.maxPacketSize} bytes)"

    companion object {
        private const val DEFAULT_TRANSFER_CHUNK_BYTES = 16 * 1024
        private const val DEFAULT_TRANSFER_TIMEOUT_MS = 5000
    }
}
