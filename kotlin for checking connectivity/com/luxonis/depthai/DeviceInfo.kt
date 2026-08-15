package com.luxonis.depthai

/**
 * Lightweight placeholder for device metadata.
 * mxId = unique device id (MXID)
 */
data class DeviceInfo(
    val mxId: String,
    val name: String = "DepthAI Device",
    val boardName: String = "OAK-D"
)

object DeviceManager {
    /**
     * Example enumeration — replace with native/device logic when you hook native code.
     */
    fun enumerateDevices(): List<DeviceInfo> {
        return listOf(
            DeviceInfo("14442C10711AD2D600", "OAK-D", "OAK-D"),
        )
    }

    /** Find device by MXID */
    fun getDeviceByMxId(mxId: String): DeviceInfo? {
        return enumerateDevices().find { it.mxId == mxId }
    }
}
