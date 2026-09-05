package com.usbprint.app

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.usbprint.app.USB_PERMISSION"
        private const val REQUEST_DOCUMENT = 1001
    }

    private lateinit var usbManager: UsbManager
    private lateinit var usbConnection: UsbPrinterConnection
    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var connectionText: TextView
    private lateinit var documentText: TextView
    private lateinit var refreshButton: Button
    private lateinit var connectButton: Button
    private lateinit var selectDocumentButton: Button
    private lateinit var printButton: Button

    private var selectedDevice: UsbDevice? = null
    private var selectedDocumentUri: Uri? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_USB_PERMISSION) return
            val device = getUsbDeviceFromIntent(intent) ?: return
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                selectedDevice = device
                statusText.text = "USB permission granted ✓"
                deviceText.text = deviceDetails(device)
                connectToPrinter()
            } else {
                statusText.text = "USB permission was denied."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configureSystemBars()
        configureSafeScreenInsets()

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        usbConnection = UsbPrinterConnection(usbManager)
        statusText = findViewById(R.id.statusText)
        deviceText = findViewById(R.id.deviceText)
        connectionText = findViewById(R.id.connectionText)
        documentText = findViewById(R.id.documentText)
        refreshButton = findViewById(R.id.refreshButton)
        connectButton = findViewById(R.id.connectButton)
        selectDocumentButton = findViewById(R.id.selectDocumentButton)
        printButton = findViewById(R.id.printButton)

        registerUsbPermissionReceiver()
        refreshButton.setOnClickListener { scanUsbDevices() }
        connectButton.setOnClickListener { requestUsbPermission() }
        selectDocumentButton.setOnClickListener { selectPdf() }
        printButton.setOnClickListener {
            statusText.text = "Document ready for printing"
            connectionText.text = "Connected ✓\nPrint engine will be added next."
        }
        scanUsbDevices()
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        startActivityForResult(intent, REQUEST_DOCUMENT)
    }

    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_DOCUMENT || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        selectedDocumentUri = uri
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Some document providers do not offer persistable permissions.
        }
        documentText.text = getDocumentName(uri)
        printButton.isEnabled = usbConnection.isOpen && selectedDocumentUri != null
        statusText.text = "PDF selected ✓"
    }

    private fun getDocumentName(uri: Uri): String {
        var name: String? = null
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0)
        }
        return name ?: (uri.lastPathSegment ?: "Selected PDF")
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.rgb(23, 24, 29)
        window.navigationBarColor = Color.rgb(247, 247, 251)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    private fun configureSafeScreenInsets() {
        val root = findViewById<LinearLayout>(R.id.rootLayout)
        val toolbar = findViewById<LinearLayout>(R.id.toolbar)
        val originalToolbarTop = toolbar.paddingTop
        val originalToolbarBottom = toolbar.paddingBottom
        val originalRootLeft = root.paddingLeft
        val originalRootRight = root.paddingRight
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(maxOf(originalRootLeft, bars.left), 0, maxOf(originalRootRight, bars.right), bars.bottom)
            toolbar.setPadding(toolbar.paddingLeft, originalToolbarTop + bars.top, toolbar.paddingRight, originalToolbarBottom)
            insets
        }
        root.requestApplyInsets()
    }

    override fun onDestroy() {
        usbConnection.close()
        unregisterReceiver(usbPermissionReceiver)
        super.onDestroy()
    }

    private fun scanUsbDevices() {
        val devices = usbManager.deviceList.values.toList()
        selectedDevice = devices.firstOrNull { isPrinterLike(it) } ?: devices.firstOrNull()
        usbConnection.close()
        connectionText.text = "Not connected"
        printButton.isEnabled = false
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
        val device = selectedDevice ?: run { statusText.text = "No USB device selected."; return }
        if (usbManager.hasPermission(device)) {
            connectToPrinter()
            return
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION).setPackage(packageName), flags)
        usbManager.requestPermission(device, permissionIntent)
        statusText.text = "Waiting for USB permission..."
    }

    private fun connectToPrinter() {
        val device = selectedDevice ?: return
        val result = usbConnection.open(device)
        statusText.text = result.message
        connectionText.text = if (result.success) {
            buildString {
                append("Connected ✓")
                result.interfaceNumber?.let { append("\nInterface: ").append(it) }
                if (result.endpointSummary.isNotBlank()) append("\n").append(result.endpointSummary)
            }
        } else "Not connected"
        printButton.isEnabled = result.success && selectedDocumentUri != null
    }

    private fun isPrinterLike(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) if (device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
        return false
    }

    private fun deviceDetails(device: UsbDevice): String = buildString {
        append(if (isPrinterLike(device)) "Printer-class device" else "USB device")
        append("\nName: ").append(device.deviceName)
        append("\nVendor ID: 0x").append(device.vendorId.toString(16).uppercase())
        append("\nProduct ID: 0x").append(device.productId.toString(16).uppercase())
        append("\nInterfaces: ").append(device.interfaceCount)
    }

    @Suppress("DEPRECATION")
    private fun getUsbDeviceFromIntent(intent: Intent): UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
    } else intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    private fun registerUsbPermissionReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION") registerReceiver(usbPermissionReceiver, filter)
        }
    }
}
