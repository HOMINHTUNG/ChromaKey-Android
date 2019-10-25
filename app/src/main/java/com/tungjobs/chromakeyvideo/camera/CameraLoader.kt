package com.tungjobs.chromakeyvideo.camera


abstract class CameraLoader {

    protected var onPreviewFrame: ((data: ByteArray, width: Int, height: Int) -> Unit)? = null

    abstract fun onResume(width: Int, height: Int)

    abstract fun onPause()

    abstract fun switchCamera()

    abstract fun onStartCapture()

    abstract fun onReleaseCapture()

    abstract fun turnOnFlash(): Boolean

    abstract fun turnOffFlash(): Boolean

    abstract fun getCameraOrientation(): Int

    abstract fun hasMultipleCamera(): Boolean

    fun setOnPreviewFrameListener(onPreviewFrame: (data: ByteArray, width: Int, height: Int) -> Unit) {
        this.onPreviewFrame = onPreviewFrame
    }
}