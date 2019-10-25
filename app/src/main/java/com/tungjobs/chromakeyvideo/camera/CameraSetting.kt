@file:Suppress("DEPRECATION")

package com.tungjobs.chromakeyvideo.camera

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.widget.Toast
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.media.MediaRecorder
import java.io.File


class CameraSetting(private val activity: Activity) : CameraLoader() {
    override fun onReleaseCapture() {
    }

    override fun onStartCapture() {

    }

    private var cameraInstance: Camera? = null
    private var cameraFacing: Int = Camera.CameraInfo.CAMERA_FACING_BACK
    private val mSurfaceView: SurfaceView? = null
    private val mSurfaceHolder: SurfaceHolder? = null
    private val mRecordingStatus: Boolean = false
    private val mMediaRecorder: MediaRecorder? = null
    private val mOutputFile: File? = null
    private val isRecording: Boolean = false

    private val cameraManager: CameraManager by lazy {
        activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val isAllowCameraFlash: Boolean by lazy {
        activity.packageManager.hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FLASH
        )
    }

    override fun onResume(width: Int, height: Int) {
        setUpCamera()
    }

    override fun onPause() {
        releaseCamera()
    }

    override fun switchCamera() {
        cameraFacing = when (cameraFacing) {
            Camera.CameraInfo.CAMERA_FACING_FRONT -> Camera.CameraInfo.CAMERA_FACING_BACK
            Camera.CameraInfo.CAMERA_FACING_BACK -> Camera.CameraInfo.CAMERA_FACING_FRONT
            else -> return
        }
        releaseCamera()
        setUpCamera()
    }

    override fun getCameraOrientation(): Int {
        val degrees = when (activity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        return if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (90 + degrees) % 360
        } else { // back-facing
            (90 - degrees) % 360
        }
    }

    override fun hasMultipleCamera(): Boolean {
        return Camera.getNumberOfCameras() > 1
    }

    private fun setUpCamera() {
        val id = getCurrentCameraId()
        try {
            cameraInstance = getCameraInstance(id)
        } catch (e: IllegalAccessError) {
            Log.e(TAG, "Camera not found")
            return
        }
        val parameters = cameraInstance!!.parameters

        if (parameters.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        cameraInstance!!.parameters = parameters

        cameraInstance!!.setPreviewCallback { data, camera ->
            if (data == null || camera == null) {
                return@setPreviewCallback
            }
            val size = camera.parameters.previewSize
            onPreviewFrame?.invoke(data, size.width, size.height)
        }
        cameraInstance!!.startPreview()
    }

    private fun getCurrentCameraId(): Int {
        val cameraInfo = Camera.CameraInfo()
        for (id in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(id, cameraInfo)
            if (cameraInfo.facing == cameraFacing) {
                return id
            }
        }
        return 0
    }

    private fun getCameraInstance(id: Int): Camera {
        return try {
            Camera.open(id)
        } catch (e: Exception) {
            throw IllegalAccessError("Camera not found")
        }
    }

    private fun releaseCamera() {
        cameraInstance!!.setPreviewCallback(null)
        cameraInstance!!.release()
        cameraInstance = null
    }

    override fun turnOnFlash():Boolean {
        try {
            if (isAllowCameraFlash) {
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//                val cameraId = cameraManager.cameraIdList[0]
//                cameraManager.setTorchMode(cameraId, true)
                //else
                val parameters = cameraInstance!!.parameters
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                cameraInstance!!.parameters = parameters
                cameraInstance!!.startPreview()
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity, "Exception flashLightOn()",
                Toast.LENGTH_SHORT
            ).show()
        }

        return false
    }

    override fun turnOffFlash():Boolean {
        try {
            if (isAllowCameraFlash) {
                //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//                val cameraId = cameraManager.cameraIdList[0]
//                cameraManager.setTorchMode(cameraId, false)
                //else
                releaseCamera()
                setUpCamera()
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity, "Exception flashLightOff",
                Toast.LENGTH_SHORT
            ).show()
        }
        return false
    }

    companion object {
        private const val TAG = "Camera1Loader"
    }
}