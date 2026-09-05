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

    /**
     * Performs a USB Printer Class GET_PORT_STATUS request.
     * This does not send a print job; it verifies that Android can communicate
     * with the claimed printer interface before we attempt UFR II LT printing.
     */
    fun testCommunication(): CommunicationResult {
        val currentConnection = connection ?: return CommunicationResult(false, "Printer is not connected.")
        val currentInterface = printerInterface ?: return CommunicationResult(false, "Printer interface is not available.")
        val buffer = ByteArray(1)
        val transferred = currentConnection.controlTransfer(
            0xA1,
            0x01,
            0,
            currentInterface.id,
            buffer,
            buffer.size,
            1500
        )
        if (transferred == 1) {
            val status = buffer[0].toInt() and 0xFF
            val selected = (status and 0x01) != 0
            val noError = (status and 0x08) != 0
            val paperEmpty = (status and 0x20) != 0
            return CommunicationResult(
                true,
                buildString {
                    append("Printer responded ✓")
                    append("\nUSB status: 0x").append(status.toString(16).padStart(2, '0').uppercase())
                    if (selected) append("\nPrinter selected")
                    if (noError) append("\nNo printer error reported")
                    if (paperEmpty) append("\nPaper-out status reported")
                }
            )
        }
        return CommunicationResult(false, "Printer did not respond to the USB Printer Class status request.")
    }

    /**
     * Sends only a type-safe, already-encoded SFP/UFR II LT job.
     * Raw PDF/raster ByteArray values cannot be passed through this API.
     */
    fun sendEncodedJob(job: EncodedSfpJob, timeoutMs: Int = DEFAULT_TRANSFER_TIMEOUT_MS): PrintTransferResult {
        val currentConnection = connection
            ?: return PrintTransferResult(false, "Printer is not connected.")
        val endpoint = outEndpoint
            ?: return PrintTransferResult(false, "Printer bulk OUT endpoint is not available.")
        require(timeoutMs > 0) { "USB transfer timeout must be positive" }

        val data = job.data
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(DEFAULT_TRANSFER_CHUNK_BYTES, data.size - offset)
            val transferred = currentConnection.bulkTransfer(
                endpoint,
                data,
                offset,
                chunkSize,
                timeoutMs
            )
            if (transferred != chunkSize) {
                return PrintTransferResult(
                    false,
                    "USB print transfer stopped after $offset bytes (expected $chunkSize, sent $transferred).",
                    offset
                )
            }
            offset += transferred
        }

        return PrintTransferResult(true, "Encoded printer job sent ✓ ($offset bytes)", offset)
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

    val isOpen: Boolean
        get() = connection != null && outEndpoint != null

    private fun formatEndpoint(endpoint: UsbEndpoint): String = "0x${endpoint.address.toString(16).uppercase()} (${endpoint.maxPacketSize} bytes)"

    companion object {
        private const val DEFAULT_TRANSFER_CHUNK_BYTES = 16 * 1024
        private const val DEFAULT_TRANSFER_TIMEOUT_MS = 5000
    }
}
