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
            val selected = (status and 0x10) != 0
            val noError = (status and 0x08) != 0
            val paperEmpty = (status and 0x20) != 0
            return CommunicationResult(true, buildString {
                append("Printer responded ✓")
                append("\nUSB status: 0x").append(status.toString(16).padStart(2, '0').uppercase())
                if (selected) append("\nPrinter online / selected ✓")
                if (noError) append("\nNo printer error reported ✓")
                if (paperEmpty) append("\nPaper-out status reported")
            })
        }
        return CommunicationResult(false, "Printer did not respond to the USB Printer Class status request.")
    }

    /**
     * Canon's MLC USB transport has an initialization exchange before the
     * channel-1 print stream. The Linux driver first writes the 8-byte MLC
     * initialization record, then opens channel 1 through channel 0.
     */
    fun sendEncodedJob(data: ByteArray, sourceBytes: Long? = null, timeoutMs: Int = DEFAULT_TRANSFER_TIMEOUT_MS): PrintTransferResult {
        val currentConnection = connection ?: return PrintTransferResult(false, "Printer is not connected.")
        val endpoint = outEndpoint ?: return PrintTransferResult(false, "Printer bulk OUT endpoint is not available.")
        if (data.isEmpty()) return PrintTransferResult(false, "Encoded printer job is empty.")
        require(timeoutMs > 0) { "USB transfer timeout must be positive" }

        val transport = initializeCanonMlc(currentConnection, endpoint, timeoutMs)
        if (!transport.success) return PrintTransferResult(false, transport.message)

        var offset = 0
        var frameCount = 0
        while (offset < data.size) {
            if (data.size - offset < CMLP_HEADER_SIZE) {
                return PrintTransferResult(false, "Invalid Canon CMLP stream: truncated frame header at byte $offset.", offset)
            }
            if ((data[offset].toInt() and 0xFF) != 0x01 || (data[offset + 1].toInt() and 0xFF) != 0x10) {
                return PrintTransferResult(false, "Invalid Canon CMLP stream: expected channel-1 frame at byte $offset.", offset)
            }

            val frameLength = ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
            if (frameLength < CMLP_HEADER_SIZE || offset + frameLength > data.size) {
                return PrintTransferResult(false, "Invalid Canon CMLP frame length $frameLength at byte $offset.", offset)
            }
            if ((data[offset + 4].toInt() and 0xFF) != 0x01 || (data[offset + 5].toInt() and 0xFF) != 0x00) {
                return PrintTransferResult(false, "Invalid Canon channel-1 frame flags at byte $offset.", offset)
            }

            val transferred = currentConnection.bulkTransfer(endpoint, data, offset, frameLength, timeoutMs)
            if (transferred != frameLength) {
                return PrintTransferResult(
                    false,
                    "Canon USB transfer stopped in frame ${frameCount + 1}: expected $frameLength bytes, sent $transferred.",
                    offset + transferred.coerceAtLeast(0)
                )
            }
            offset += transferred
            frameCount++
        }

        val status = readPrinterClassStatus()
        val inResult = readImmediatePrinterResponse()
        val message = buildString {
            append("USB transfer completed ✓")
            if (sourceBytes != null && sourceBytes >= 0) append("\nPDF file: ").append(formatBytes(sourceBytes))
            append("\nGenerated Canon print stream: ").append(formatBytes(offset.toLong()))
            append("\nCMLP channel-1 frames sent: ").append(frameCount)
            append("\nCanon MLC initialization: ").append(transport.message)
            append("\n").append(status)
            append("\n").append(inResult)
            append("\nCanon stream handed to USB endpoint frame-by-frame.")
        }
        return PrintTransferResult(true, message, offset)
    }

    private fun initializeCanonMlc(
        usb: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        timeoutMs: Int
    ): CommunicationResult {
        val inEp = inEndpoint
            ?: return CommunicationResult(false, "Canon MLC initialization requires the printer IN endpoint, but none was found.")

        // C_USBPort::InitSub() from Canon libcomm_usbmlportr writes this exact
        // 8-byte record with WritePort before any MLC channel is opened.
        val init = byteArrayOf(0x00, 0x00, 0x00, 0x08, 0x01, 0x00, 0x00, 0x08)
        val initSent = usb.bulkTransfer(endpoint, init, 0, init.size, timeoutMs)
        if (initSent != init.size) {
            return CommunicationResult(false, "Canon MLC initialization failed: sent $initSent/${init.size} bytes.")
        }

        // The Canon driver immediately reads and validates a 9-byte response
        // before it attempts to open an MLC service. Keep this read separate so
        // the response cannot be mistaken for the later channel-open response.
        val initResponse = readBulkResponse(usb, inEp, 9, 2000)
        if (initResponse == null) {
            return CommunicationResult(false, "Canon MLC init record sent, but the required 9-byte initialization response was not received.")
        }
        val initHex = initResponse.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
        val initLength = ((initResponse[2].toInt() and 0xFF) shl 8) or (initResponse[3].toInt() and 0xFF)
        val initValid = initResponse.size == 9 &&
            (initResponse[0].toInt() and 0xFF) == 0x00 &&
            (initResponse[1].toInt() and 0xFF) == 0x00 &&
            initLength == 9 &&
            (initResponse[6].toInt() and 0xFF) == 0x80 &&
            (initResponse[7].toInt() and 0xFF) == 0x00 &&
            (initResponse[8].toInt() and 0xFF) == 0x08
        if (!initValid) {
            return CommunicationResult(false, "Canon MLC initialization response was unexpected: $initHex")
        }

        // C_MLCChannel::OpenSub() sends channel 0 with the service-open
        // request payload: 01 <channel=01> <service=10> FF FF FF FF FF FF.
        val requestPayload = byteArrayOf(
            0x01, 0x01, 0x10,
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
        )
        val frameLength = requestPayload.size + CMLP_HEADER_SIZE
        val request = ByteArray(frameLength)
        request[0] = 0x00
        request[1] = 0x00
        request[2] = ((frameLength ushr 8) and 0xFF).toByte()
        request[3] = (frameLength and 0xFF).toByte()
        request[4] = 0x01
        request[5] = 0x00
        System.arraycopy(requestPayload, 0, request, CMLP_HEADER_SIZE, requestPayload.size)
        val requestSent = usb.bulkTransfer(endpoint, request, 0, request.size, timeoutMs)
        if (requestSent != request.size) {
            return CommunicationResult(false, "Canon MLC channel-open request failed: sent $requestSent/${request.size} bytes.")
        }

        // RecvSub() receives a CMLP frame, strips its 6-byte header, then gives
        // OpenSub() a 12-byte payload. Accept the raw payload as a fallback too,
        // because Android's bulk endpoint exposes the transport below that layer.
        val rawResponse = readBulkResponse(usb, inEp, 12, 3000, allowLarger = true)
            ?: return CommunicationResult(false, "Canon MLC channel-open request sent, but no back-channel response was received.")
        val payload = extractCmlpPayload(rawResponse)
        val hex = rawResponse.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
        val payloadHex = payload.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
        if (payload.size != 12 ||
            (payload[0].toInt() and 0xFF) != 0x81 ||
            (payload[1].toInt() and 0xFF) != 0x00 ||
            (payload[2].toInt() and 0xFF) != 0x01 ||
            (payload[3].toInt() and 0xFF) != 0x10
        ) {
            return CommunicationResult(false, "Canon MLC channel-open response was unexpected: $hex | payload: $payloadHex")
        }
        val sendSize = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
        val recvSize = ((payload[6].toInt() and 0xFF) shl 8) or (payload[7].toInt() and 0xFF)
        if (sendSize <= 6) {
            return CommunicationResult(false, "Canon MLC channel-open response rejected the service: $payloadHex")
        }
        return CommunicationResult(true, "channel-open response received ✓ (send=$sendSize, recv=$recvSize; payload: $payloadHex)")
    }

    private fun readBulkResponse(
        usb: UsbDeviceConnection,
        endpoint: UsbEndpoint,
        expectedBytes: Int,
        timeoutMs: Int,
        allowLarger: Boolean = false
    ): ByteArray? {
        val capacity = endpoint.maxPacketSize.coerceAtLeast(expectedBytes).coerceAtMost(4096)
        val buffer = ByteArray(capacity)
        val n = usb.bulkTransfer(endpoint, buffer, 0, buffer.size, timeoutMs)
        if (n <= 0) return null
        if (!allowLarger && n != expectedBytes) return buffer.copyOf(n)
        if (allowLarger && n < expectedBytes) return null
        return buffer.copyOf(n)
    }

    private fun extractCmlpPayload(raw: ByteArray): ByteArray {
        if (raw.size >= CMLP_HEADER_SIZE) {
            val totalLength = ((raw[2].toInt() and 0xFF) shl 8) or (raw[3].toInt() and 0xFF)
            if ((raw[0].toInt() and 0xFF) == 0x00 &&
                (raw[1].toInt() and 0xFF) == 0x00 &&
                totalLength >= CMLP_HEADER_SIZE &&
                totalLength <= raw.size
            ) {
                return raw.copyOfRange(CMLP_HEADER_SIZE, totalLength)
            }
        }
        return raw
    }

    private fun readPrinterClassStatus(): String {
        val currentConnection = connection ?: return "USB Printer Class status: unavailable"
        val currentInterface = printerInterface ?: return "USB Printer Class status: unavailable"
        val buffer = ByteArray(1)
        val n = currentConnection.controlTransfer(0xA1, 0x01, 0, currentInterface.id, buffer, 1, 1000)
        if (n != 1) return "USB Printer Class status: no response"
        return formatPrinterStatus(buffer[0].toInt() and 0xFF)
    }

    private fun formatPrinterStatus(status: Int): String {
        val paperEmpty = (status and 0x20) != 0
        val selected = (status and 0x10) != 0
        val noError = (status and 0x08) != 0
        return buildString {
            append("USB Printer Class status: 0x")
                .append(status.toString(16).padStart(2, '0').uppercase())
            append("\nPrinter: ").append(if (selected) "ONLINE / SELECTED ✓" else "NOT SELECTED")
            append("\nError state: ").append(if (noError) "NO ERROR ✓" else "ERROR REPORTED")
            append("\nPaper: ").append(if (paperEmpty) "EMPTY" else "READY ✓")
        }
    }

    private fun readImmediatePrinterResponse(): String {
        val currentConnection = connection ?: return "IN endpoint response: unavailable"
        val endpoint = inEndpoint ?: return "IN endpoint response: unavailable"
        val buffer = ByteArray(endpoint.maxPacketSize.coerceAtLeast(64))
        val n = currentConnection.bulkTransfer(endpoint, buffer, 0, buffer.size, 1000)
        return when {
            n > 0 -> "IN endpoint response: $n byte(s) ${buffer.copyOf(n).joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }}"
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
    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
    }

    companion object {
        private const val CMLP_HEADER_SIZE = 6
        private const val DEFAULT_TRANSFER_TIMEOUT_MS = 5000
    }
}
