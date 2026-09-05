package com.usbprint.app

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.usbprint.app.USB_PERMISSION"
    }

    private lateinit var usbManager: UsbManager
    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var refreshButton: Button
    private lateinit var connectButton: Button

    private var selectedDevice: UsbDevice? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return

            val device = getUsbDeviceFromIntent(intent) ?: return
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                selectedDevice = device
                statusText.text = "USB permission granted ✓"
                deviceText.text = deviceDetails(device)
            } else {
                statusText.text = "USB permission was denied."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        statusText = findViewById(R.id.statusText)
        deviceText = findViewById(R.id.deviceText)
        refreshButton = findViewById(R.id.refreshButton)
        connectButton = findViewById(R.id.connectButton)

        registerUsbPermissionReceiver()

        refreshButton.setOnClickListener { scanUsbDevices() }
        connectButton.setOnClickListener { requestUsbPermission() }

        scanUsbDevices()
    }

    override fun onDestroy() {
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    private fun scanUsbDevices() {
        val devices = usbManager.deviceList.values.toList()
        selectedDevice = devices.firstOrNull { isPrinterLike(it) } ?: devices.firstOrNull()

        if (devices.isEmpty()) {
            statusText.text = "No USB device detected"
            deviceText.text = "Connect your Canon printer using a USB OTG adapter, then tap Refresh."
            connectButton.isEnabled = false
            return
        }

        statusText.text = "USB device detected ✓"
        deviceText.text = devices.joinToString("\n\n") { deviceDetails(it) }
        connectButton.isEnabled = selectedDevice != null
    }

    private fun requestUsbPermission() {
        val device = selectedDevice
        if (device == null) {
            statusText.text = "No USB device selected."
            return
        }

        if (usbManager.hasPermission(device)) {
            statusText.text = "USB permission already granted ✓"
            return
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(packageName),
            flags
        )

        usbManager.requestPermission(device, permissionIntent)
        statusText.text = "Waiting for USB permission..."
    }

    private fun isPrinterLike(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) {
            if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
        }
        return false
    }

    private fun deviceDetails(device: UsbDevice): String {
        val printer = if (isPrinterLike(device)) "Printer-class device" else "USB device"
        return buildString {
            append(printer)
            append("\nName: ").append(device.deviceName)
            append("\nVendor ID: 0x").append(device.vendorId.toString(16).uppercase())
            append("\nProduct ID: 0x").append(device.productId.toString(16).uppercase())
            append("\nInterfaces: ").append(device.interfaceCount)
        }
    }

    @Suppress("DEPRECATION")
    private fun getUsbDeviceFromIntent(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun registerUsbPermissionReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbPermissionReceiver, filter)
        }
    }
}
