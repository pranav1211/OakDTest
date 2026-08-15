package com.example.oakdtest

import android.os.Bundle
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.luxonis.depthai.DepthAI
import com.luxonis.depthai.UsbPermissionManager
import android.content.Context


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // UI Components
    private lateinit var statusText: TextView
    private lateinit var scrollView: ScrollView

    private lateinit var btnCheckLibrary: Button
    private lateinit var btnCheckUsbPermissions: Button
    private lateinit var btnCheckDevices: Button
    private lateinit var btnDiagnostic: Button
    private lateinit var btnStartStream: Button
    private lateinit var btnStopStream: Button

    // USB Manager
    private lateinit var usbPermissionManager: UsbPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()
        setupUsbManager()
    }

    private fun initializeUI() {
        statusText = findViewById(R.id.statusText)
        scrollView = findViewById(R.id.scrollView)

        btnCheckLibrary = findViewById(R.id.btnCheckLibrary)
        btnCheckUsbPermissions = findViewById(R.id.btnCheckUsbPermissions)
        btnCheckDevices = findViewById(R.id.btnCheckDevices)
        btnDiagnostic = findViewById(R.id.btnDiagnostic)
        btnStartStream = findViewById(R.id.btnStartStream)
        btnStopStream = findViewById(R.id.btnStopStream)

        // ALL BUTTONS ENABLED - Independent testing
        btnCheckLibrary.isEnabled = true
        btnCheckUsbPermissions.isEnabled = true
        btnCheckDevices.isEnabled = true
        btnDiagnostic.isEnabled = true
        btnStartStream.isEnabled = true
        btnStopStream.isEnabled = false

        // Button click listeners
        btnCheckLibrary.setOnClickListener { checkLibrary() }
        btnCheckUsbPermissions.setOnClickListener { checkUsbPermissions() }
        btnCheckDevices.setOnClickListener { checkDevices() }
        btnDiagnostic.setOnClickListener { runDiagnostics() }
        btnStartStream.setOnClickListener { startStream() }
        btnStopStream.setOnClickListener { stopStream() }

        appendStatus("Welcome to DepthAI v3 Android Test")
        appendStatus("All buttons are independent - test anything!\n")
    }

    private fun setupUsbManager() {
        usbPermissionManager = UsbPermissionManager(this)
    }

    private fun runDiagnostics() {
        appendStatus("\n=== FULL DIAGNOSTICS ===")

        val androidSupport = DepthAI.checkAndroidSupport()
        Log.d("MainActivity", androidSupport)

        // 1. USB Host Support
        val usbHostSupported = usbPermissionManager.isUsbHostSupported()
        appendStatus("USB Host Supported: $usbHostSupported")

        // 2. Check all Android permissions
        appendStatus("\n[Android Permissions]")

        val hasCameraPermission = checkSelfPermission(android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        appendStatus("Camera Permission: $hasCameraPermission")

        if (!hasCameraPermission) {
            appendStatus("Requesting camera permission...")
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val hasStorageRead = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasStorageWrite = checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            appendStatus("Storage Read: $hasStorageRead")
            appendStatus("Storage Write: $hasStorageWrite")
        } else {
            appendStatus("Storage: Not required on Android 13+")
        }

        // 3. USB Devices with STATE check
        appendStatus("\n[USB Devices]")
        val usbManager = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val allDevices = usbManager.deviceList
        appendStatus("Total USB devices: ${allDevices.size}")

        for ((name, device) in allDevices) {
            appendStatus("  $name:")
            appendStatus("    Vendor: 0x${device.vendorId.toString(16)}")
            appendStatus("    Product: 0x${device.productId.toString(16)}")
            appendStatus("    Device Class: ${device.deviceClass}")
            appendStatus("    Device Subclass: ${device.deviceSubclass}")
            appendStatus("    Device Protocol: ${device.deviceProtocol}")

            val hasPermission = usbManager.hasPermission(device)
            appendStatus("    Has permission: $hasPermission")

            // Identify device state by product ID
            if (device.vendorId == 0x3e7) {
                val state = when(device.productId) {
                    0x2485 -> "UNBOOTED/BOOTLOADER (needs boot)"
                    0xf63b -> "BOOTED (OAK-D)"
                    0xf63d -> "BOOTED (OAK-D Pro)"
                    0xf63c -> "BOOTED (OAK-D Lite)"
                    else -> "UNKNOWN STATE"
                }
                appendStatus("    ⚠ Device State: $state")
            }

            if (!hasPermission && device.vendorId == 0x3e7) {
                appendStatus("    ⚠ This is an OAK device - requesting permission...")
                usbPermissionManager.requestPermission(device) { granted, _ ->
                    runOnUiThread {
                        if (granted) {
                            appendStatus("    ✓ Permission granted!")
                        } else {
                            appendStatus("    ✗ Permission denied")
                        }
                    }
                }
            }
        }

        // 4. OAK Devices
        val oakDevices = usbPermissionManager.getConnectedOakDevices()
        appendStatus("\n[OAK Devices]")
        appendStatus("OAK Devices found: ${oakDevices.size}")

        // 5. DepthAI Library
        appendStatus("\n[DepthAI Library]")
        appendStatus("Version: ${DepthAI.getLibraryVersion()}")
        appendStatus("Library Present: ${DepthAI.isLibraryPresent()}")

        // Get actual device info from DepthAI
        val devices = DepthAI.getAvailableDevices()
        appendStatus("Device Count: ${devices.size}")

        if (devices.isNotEmpty()) {
            for ((index, device) in devices.withIndex()) {
                appendStatus("\nDepthAI Device $index:")
                appendStatus("  ID: ${device.deviceId}")
                appendStatus("  Name: ${device.name}")
                appendStatus("  Protocol: ${device.protocol}")
                appendStatus("  State: ${device.state}")
            }
        } else {
            appendStatus("⚠ DepthAI found 0 devices")
            appendStatus("Possible reasons:")
            appendStatus("  1. Device in UNBOOTED state (Product ID 0x2485)")
            appendStatus("  2. Device needs bootloader firmware")
            appendStatus("  3. libusb access issue")
        }

        appendStatus("\n=== END DIAGNOSTICS ===\n")
    }

    private fun checkLibrary() {
        appendStatus("=== Checking DepthAI Library ===")
        try {
            val isPresent = DepthAI.isLibraryPresent()

            if (isPresent) {
                val version = DepthAI.getLibraryVersion()
                appendStatus("✓ DepthAI library loaded")
                appendStatus("✓ Version: $version")
                appendStatus("✓ Library is functional\n")
                setButtonColor(btnCheckLibrary, true)
            } else {
                appendStatus("✗ Library is not functional\n", true)
                setButtonColor(btnCheckLibrary, false)
            }
        } catch (e: Exception) {
            appendStatus("✗ Failed to check library: ${e.message}\n", true)
            setButtonColor(btnCheckLibrary, false)
        }
    }

    private fun checkUsbPermissions() {
        appendStatus("=== Checking USB Permissions ===")

        if (!usbPermissionManager.isUsbHostSupported()) {
            appendStatus("✗ USB Host mode not supported on this device\n", true)
            setButtonColor(btnCheckUsbPermissions, false)
            return
        }

        appendStatus("✓ USB Host mode supported")

        val oakDevices = usbPermissionManager.getConnectedOakDevices()

        if (oakDevices.isEmpty()) {
            appendStatus("✗ No OAK USB devices found")
            appendStatus("Please connect an OAK camera via USB\n", true)
            setButtonColor(btnCheckUsbPermissions, false)
            return
        }

        appendStatus("Found ${oakDevices.size} OAK USB device(s)")

        var allPermissionsGranted = true
        val devicesNeedingPermission = mutableListOf<android.hardware.usb.UsbDevice>()

        for (device in oakDevices) {
            appendStatus("\nDevice: ${device.deviceName}")
            appendStatus("  Vendor ID: 0x${device.vendorId.toString(16)}")
            appendStatus("  Product ID: 0x${device.productId.toString(16)}")

            if (usbPermissionManager.hasPermission(device)) {
                appendStatus("  ✓ Permission already granted")
            } else {
                appendStatus("  ⚠ Permission needed")
                devicesNeedingPermission.add(device)
                allPermissionsGranted = false
            }
        }

        if (allPermissionsGranted) {
            appendStatus("\n✓ All USB permissions granted\n")
            setButtonColor(btnCheckUsbPermissions, true)
        } else {
            appendStatus("\nRequesting permissions for ${devicesNeedingPermission.size} device(s)...")
            requestPermissionsForDevices(devicesNeedingPermission)
        }
    }

    private fun requestPermissionsForDevices(devices: List<android.hardware.usb.UsbDevice>) {
        if (devices.isEmpty()) return

        val device = devices.first()
        usbPermissionManager.requestPermission(device) { granted, _ ->
            runOnUiThread {
                if (granted) {
                    appendStatus("✓ Permission granted for ${device.deviceName}")

                    val remaining = devices.drop(1)
                    if (remaining.isEmpty()) {
                        appendStatus("\n✓ All USB permissions granted\n")
                        setButtonColor(btnCheckUsbPermissions, true)
                    } else {
                        requestPermissionsForDevices(remaining)
                    }
                } else {
                    appendStatus("✗ Permission denied for ${device.deviceName}\n", true)
                    setButtonColor(btnCheckUsbPermissions, false)
                }
            }
        }
    }

    private fun checkDevices() {
        appendStatus("=== Checking for DepthAI Devices ===")
        try {
            val hasConnection = DepthAI.checkConnection()

            if (hasConnection) {
                val deviceCount = DepthAI.getDeviceCount()
                appendStatus("✓ Found $deviceCount device(s)")

                val deviceNames = DepthAI.getDeviceNames()
                for ((index, name) in deviceNames.withIndex()) {
                    appendStatus("  Device $index: $name")
                }

                val devices = DepthAI.getAvailableDevices()
                appendStatus("\nDetailed Information:")
                for ((index, device) in devices.withIndex()) {
                    appendStatus("Device $index:")
                    appendStatus("  ID: ${device.deviceId}")
                    appendStatus("  Name: ${device.name}")
                    appendStatus("  Protocol: ${device.protocol}")
                    appendStatus("  State: ${device.state}")
                }

                appendStatus("\n✓ DepthAI devices detected!\n")
                setButtonColor(btnCheckDevices, true)
            } else {
                appendStatus("✗ No DepthAI devices found\n", true)
                setButtonColor(btnCheckDevices, false)
            }
        } catch (e: Exception) {
            appendStatus("✗ Error checking devices: ${e.message}\n", true)
            appendStatus("Stack trace: ${e.stackTraceToString()}\n", true)
            setButtonColor(btnCheckDevices, false)
        }
    }

    private fun startStream() {
        appendStatus("=== Attempting to Start Stream ===")

        try {
            // Get OAK device from USB manager
            val oakDevices = usbPermissionManager.getConnectedOakDevices()

            if (oakDevices.isEmpty()) {
                appendStatus("✗ No OAK USB devices found\n", true)
                return
            }

            val usbDevice = oakDevices[0]
            appendStatus("Found device: ${usbDevice.deviceName}")
            appendStatus("  Vendor: 0x${usbDevice.vendorId.toString(16)}")
            appendStatus("  Product: 0x${usbDevice.productId.toString(16)}")

            // Check permission
            val usbManager = getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
            if (!usbManager.hasPermission(usbDevice)) {
                appendStatus("✗ No USB permission. Please grant permission first.\n", true)
                return
            }

            appendStatus("\nAttempting to connect using Android USB API...")

            // Create device connection
            val deviceConnection = com.luxonis.depthai.DeviceConnection(this, usbDevice)

            if (deviceConnection.connect()) {
                appendStatus("✓ Device connected successfully!")
                appendStatus("Handle: ${deviceConnection.getHandle()}")
                appendStatus("\nDevice is now ready for operations")
                appendStatus("(Streaming implementation coming next)\n")

                btnStopStream.isEnabled = true

                // Store connection for later use
                // deviceConnection will be used for streaming

            } else {
                appendStatus("✗ Failed to connect to device", true)
                appendStatus("Check logcat for details\n", true)
            }

        } catch (e: Exception) {
            appendStatus("✗ Error: ${e.message}", true)
            appendStatus("Stack: ${e.stackTraceToString()}\n", true)
        }
    }

    private fun stopStream() {
        appendStatus("=== Stopping Stream ===")
        btnStopStream.isEnabled = false
        appendStatus("Stream stopped (placeholder)\n")
    }

    // Helper methods
    private fun appendStatus(message: String, isError: Boolean = false) {
        runOnUiThread {
            val currentText = statusText.text.toString()
            statusText.text = "$currentText\n$message"

            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

        Log.d(TAG, message)
    }

    private fun setButtonColor(button: Button, success: Boolean) {
        runOnUiThread {
            val color = if (success) {
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            } else {
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            }
            button.setBackgroundColor(color)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        usbPermissionManager.cleanup()
    }
}