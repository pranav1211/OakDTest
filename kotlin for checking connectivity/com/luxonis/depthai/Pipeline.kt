package com.luxonis.depthai

/**
 * Simple pipeline placeholder.
 * Replace node strings with your real node objects when you connect native code.
 */
class Pipeline {
    private val nodes = mutableListOf<String>()

    fun createColorCameraNode(): String {
        val node = "ColorCameraNode"
        nodes.add(node)
        return node
    }

    fun createMonoCameraNode(side: String): String {
        val node = "MonoCameraNode-$side"
        nodes.add(node)
        return node
    }

    fun linkNodes(outputNode: String, inputNode: String) {
        // placeholder linking
        println("Linking $outputNode -> $inputNode")
    }

    /** Called by Device.startPipeline() */
    fun start(device: Device) {
        println("Pipeline started for device: ${device.deviceInfo.name} (${device.deviceInfo.mxId})")
        // real start logic goes here
    }

    fun stop() {
        println("Pipeline stopped")
        // real stop logic goes here
    }
}
