package com.luxonis.depthai

import java.io.Closeable

/**
 * Minimal device wrapper that holds Pipeline + DeviceInfo.
 * Implements Closeable so callers can use `use { }`.
 */
class Device(
    val pipeline: Pipeline,
    val deviceInfo: DeviceInfo
) : Closeable {

    init {
        // placeholder init (native open would go here)
        println("Device initialized: ${deviceInfo.name} (${deviceInfo.mxId})")
    }

    /** Start pipeline (delegates to Pipeline.start) */
    fun startPipeline() {
        println("Starting pipeline for ${deviceInfo.name}")
        pipeline.start(this)
    }

    override fun close() {
        // placeholder close (native close would go here)
        println("Closing device: ${deviceInfo.name}")
        pipeline.stop()
    }
}
