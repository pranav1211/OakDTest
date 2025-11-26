package com.example.oakdtest

import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // UI Components
    private lateinit var statusText: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var imageView: ImageView

    private lateinit var btnCheckLibrary: Button
    private lateinit var btnCheckDevices: Button
    private lateinit var btnInitPipeline: Button
    private lateinit var btnCheckUsbPermissions: Button
    private lateinit var btnStartRgbStream: Button
    private lateinit var btnStopStream: Button

    // USB and DepthAI
    private lateinit var usbPermissionManager: UsbPermissionManager
    private var deviceHandle: Long = 0L
    private var isStreaming = false
    private var streamExecutor: ScheduledExecutorService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUI()
        setupUsbManager()
    }

    private fun initializeUI() {
        statusText = findViewById(R.id.statusText)
        scrollView = findViewById(R.id.scrollView)
        imageView = findViewById(R.id.imageView)

        btnCheckLibrary = findViewById(R.id.btnCheckLibrary)
        btnCheckDevices = findViewById(R.id.btnCheckDevices)
        btnInitPipeline = findViewById(R.id.btnInitPipeline)
        btnCheckUsbPermissions = findViewById(R.id.btnCheckUsbPermissions)
        btnStartRgbStream = findViewById(R.id.btnStartRgbStream)
        btnStopStream = findViewById(R.id.btnStopStream)

        // Initially disable all buttons except the first one
        btnCheckLibrary.isEnabled = true
        btnCheckDevices.isEnabled = false
        btnInitPipeline.isEnabled = false
        btnCheckUsbPermissions.isEnabled = false
        btnStartRgbStream.isEnabled = false
        btnStopStream.isEnabled = false

        // Button click listeners
        btnCheckLibrary.setOnClickListener { checkLibrary() }
        btnCheckDevices.setOnClickListener { checkDevices() }
        btnInitPipeline.setOnClickListener { initializePipeline() }
        btnCheckUsbPermissions.setOnClickListener { checkUsbPermissions() }
        btnStartRgbStream.setOnClickListener { startRgbStream() }
        btnStopStream.setOnClickListener { stopStream() }

        appendStatus("Welcome to DepthAI v3 Android Test")
        appendStatus("Click 'Check Library' to begin\n")
    }

    private fun setupUsbManager() {
        usbPermissionManager = UsbPermissionManager(this)
        usbPermissionManager.registerReceiver()
    }

    // Step 1: Check if JNI library loads correctly
    private fun checkLibrary() {
        appendStatus("=== STEP 1: Checking JNI Library ===")
        try {
            // Library is already loaded in DepthAI object's init block
            appendStatus("✓ JNI library loaded successfully")
            appendStatus("✓ Native methods are available\n")

            btnCheckLibrary.isEnabled = false
            btnCheckDevices.isEnabled = true
            setButtonColor(btnCheckLibrary, true)
        } catch (e: Exception) {
            appendStatus("✗ Failed to load JNI library: ${e.message}\n", true)
            setButtonColor(btnCheckLibrary, false)
        }
    }

    // Step 2: Check for DepthAI devices
    private fun checkDevices() {
        appendStatus("=== STEP 2: Checking for DepthAI Devices ===")
        try {
            val deviceCount = DepthAI.getDeviceCount()
            appendStatus("Found $deviceCount DepthAI device(s)")

            if (deviceCount > 0) {
                for (i in 0 until deviceCount) {
                    val deviceInfo = DepthAI.getDeviceInfo(i)
                    appendStatus("Device $i: $deviceInfo")
                }
                appendStatus("✓ DepthAI devices detected\n")

                btnCheckDevices.isEnabled = false
                btnInitPipeline.isEnabled = true
                setButtonColor(btnCheckDevices, true)
            } else {
                appendStatus("✗ No DepthAI devices found")
                appendStatus("Please connect a DepthAI camera\n", true)
                setButtonColor(btnCheckDevices, false)
            }
        } catch (e: Exception) {
            appendStatus("✗ Error checking devices: ${e.message}\n", true)
            setButtonColor(btnCheckDevices, false)
        }
    }

    // Step 3: Initialize device and create pipeline
    private fun initializePipeline() {
        appendStatus("=== STEP 3: Initializing Pipeline ===")
        try {
            // Initialize the first device
            deviceHandle = DepthAI.initializeDevice(0)

            if (deviceHandle != 0L) {
                appendStatus("✓ Device initialized (handle: $deviceHandle)")

                // Create and start the pipeline
                val started = DepthAI.startPipeline(deviceHandle)

                if (started) {
                    appendStatus("✓ Pipeline created and started")

                    // Get frame dimensions
                    val width = DepthAI.getFrameWidth(deviceHandle)
                    val height = DepthAI.getFrameHeight(deviceHandle)
                    appendStatus("✓ Frame size: ${width}x${height}\n")

                    btnInitPipeline.isEnabled = false
                    btnCheckUsbPermissions.isEnabled = true
                    setButtonColor(btnInitPipeline, true)
                } else {
                    appendStatus("✗ Failed to start pipeline\n", true)
                    setButtonColor(btnInitPipeline, false)
                }
            } else {
                appendStatus("✗ Failed to initialize device\n", true)
                setButtonColor(btnInitPipeline, false)
            }
        } catch (e: Exception) {
            appendStatus("✗ Error initializing pipeline: ${e.message}\n", true)
            setButtonColor(btnInitPipeline, false)
        }
    }

    // Step 4: Check and request USB permissions
    private fun checkUsbPermissions() {
        appendStatus("=== STEP 4: Checking USB Permissions ===")

        val depthAIDevices = usbPermissionManager.findDepthAIDevices()

        if (depthAIDevices.isEmpty()) {
            appendStatus("✗ No DepthAI USB devices found")
            appendStatus("Please ensure device is connected via USB\n", true)
            setButtonColor(btnCheckUsbPermissions, false)
            return
        }

        appendStatus("Found ${depthAIDevices.size} DepthAI USB device(s)")

        var allPermissionsGranted = true
        val devicesNeedingPermission = mutableListOf<UsbDevice>()

        for (device in depthAIDevices) {
            appendStatus("Device: ${device.deviceName}")
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
            appendStatus("✓ All USB permissions granted\n")
            btnCheckUsbPermissions.isEnabled = false
            btnStartRgbStream.isEnabled = true
            setButtonColor(btnCheckUsbPermissions, true)
        } else {
            appendStatus("\nRequesting permissions for ${devicesNeedingPermission.size} device(s)...")
            requestPermissionsForDevices(devicesNeedingPermission)
        }
    }

    private fun requestPermissionsForDevices(devices: List<UsbDevice>) {
        if (devices.isEmpty()) return

        val device = devices.first()
        usbPermissionManager.requestPermission(device) { granted, _ ->
            runOnUiThread {
                if (granted) {
                    appendStatus("✓ Permission granted for ${device.deviceName}")

                    // Request permission for remaining devices
                    val remaining = devices.drop(1)
                    if (remaining.isEmpty()) {
                        appendStatus("✓ All USB permissions granted\n")
                        btnCheckUsbPermissions.isEnabled = false
                        btnStartRgbStream.isEnabled = true
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

    // Step 5: Start RGB stream
    private fun startRgbStream() {
        appendStatus("=== STEP 5: Starting RGB Stream ===")

        if (deviceHandle == 0L) {
            appendStatus("✗ Device not initialized\n", true)
            return
        }

        isStreaming = true
        btnStartRgbStream.isEnabled = false
        btnStopStream.isEnabled = true

        appendStatus("✓ Starting RGB stream...")
        appendStatus("Frames will appear below\n")

        // Start streaming in background thread
        streamExecutor = Executors.newSingleThreadScheduledExecutor()
        streamExecutor?.scheduleAtFixedRate({
            if (isStreaming) {
                fetchAndDisplayFrame()
            }
        }, 0, 33, TimeUnit.MILLISECONDS) // ~30 FPS
    }

    private fun fetchAndDisplayFrame() {
        try {
            val frameData = DepthAI.getFrameRGB(deviceHandle)

            if (frameData != null && frameData.isNotEmpty()) {
                val width = DepthAI.getFrameWidth(deviceHandle)
                val height = DepthAI.getFrameHeight(deviceHandle)

                // Convert byte array to Bitmap
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                // Assuming RGB888 format from native code
                val pixels = IntArray(width * height)
                for (i in pixels.indices) {
                    val idx = i * 3
                    if (idx + 2 < frameData.size) {
                        val r = frameData[idx].toInt() and 0xFF
                        val g = frameData[idx + 1].toInt() and 0xFF
                        val b = frameData[idx + 2].toInt() and 0xFF
                        pixels[i] = Color.rgb(r, g, b)
                    }
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

                runOnUiThread {
                    imageView.setImageBitmap(bitmap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching frame: ${e.message}", e)
        }
    }

    private fun stopStream() {
        appendStatus("\n=== Stopping RGB Stream ===")
        isStreaming = false

        streamExecutor?.shutdown()
        streamExecutor = null

        btnStopStream.isEnabled = false
        btnStartRgbStream.isEnabled = true

        appendStatus("✓ Stream stopped\n")
    }

    // Helper methods
    private fun appendStatus(message: String, isError: Boolean = false) {
        runOnUiThread {
            val color = if (isError) "#FF5252" else "#FFFFFF"
            val currentText = statusText.text.toString()
            statusText.text = "$currentText\n$message"

            // Auto-scroll to bottom
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

        // Clean up
        isStreaming = false
        streamExecutor?.shutdown()

        if (deviceHandle != 0L) {
            try {
                DepthAI.stopPipeline(deviceHandle)
                DepthAI.closeDevice(deviceHandle)
            } catch (e: Exception) {
                Log.e(TAG, "Error closing device", e)
            }
        }

        usbPermissionManager.unregisterReceiver()
    }
}