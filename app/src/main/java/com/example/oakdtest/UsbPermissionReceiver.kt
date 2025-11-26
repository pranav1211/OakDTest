package com.example.oakdtest

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log

class UsbPermissionManager(private val context: Context) {

    companion object {
        private const val TAG = "UsbPermissionManager"
        private const val ACTION_USB_PERMISSION = "com.example.oakdtest.USB_PERMISSION"
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var permissionCallback: ((Boolean, UsbDevice?) -> Unit)? = null

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

                    if (granted && device != null) {
                        Log.d(TAG, "USB permission granted for device: ${device.deviceName}")
                        permissionCallback?.invoke(true, device)
                    } else {
                        Log.w(TAG, "USB permission denied for device: ${device?.deviceName}")
                        permissionCallback?.invoke(false, device)
                    }
                }
            }
        }
    }

    fun registerReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbPermissionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbPermissionReceiver, filter)
        }
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(usbPermissionReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver was not registered")
        }
    }

    fun requestPermission(device: UsbDevice, callback: (Boolean, UsbDevice?) -> Unit) {
        permissionCallback = callback

        if (usbManager.hasPermission(device)) {
            Log.d(TAG, "USB permission already granted for: ${device.deviceName}")
            callback(true, device)
        } else {
            Log.d(TAG, "Requesting USB permission for: ${device.deviceName}")
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    fun getConnectedDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.toList()
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun findDepthAIDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.filter { device ->
            // Luxonis vendor ID: 0x03e7 (999 in decimal)
            device.vendorId == 0x03e7
        }
    }
}