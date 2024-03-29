package com.tungjobs.chromakeyvideo.activity

import android.annotation.SuppressLint
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.doOnLayout
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException
import com.tungjobs.chromakeyvideo.gpuimage.filter.ChromaKeyFilter
import com.tungjobs.chromakeyvideo.gpuimage.filter.GPUImageFilterTools
import com.tungjobs.chromakeyvideo.gpuimage.filter.GPUImageMovieWriter
import com.tungjobs.chromakeyvideo.R
import com.tungjobs.chromakeyvideo.camera.Camera2Setting
import com.tungjobs.chromakeyvideo.camera.CameraLoader
import com.tungjobs.chromakeyvideo.camera.CameraSetting
import com.tungjobs.chromakeyvideo.gpuimage.ImageAdapter
import com.tungjobs.chromakeyvideo.view.GestureImage
import jp.co.cyberagent.android.gpuimage.GLTextureView
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.*
import jp.co.cyberagent.android.gpuimage.util.Rotation
import kotlinx.android.synthetic.main.activity_main.view.*
import org.jcodec.api.SequenceEncoder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), View.OnTouchListener {

    private val gpuImageView: GPUImageView by lazy { findViewById<GPUImageView>(R.id.surfaceView) }
    private val imageLayer: GestureImage by lazy { findViewById<GestureImage>(R.id.imageView) }
    private val btnColor: ImageButton by lazy { findViewById<ImageButton>(R.id.button_choose_filter) }
    private val btnShowLayer: ImageButton by lazy { findViewById<ImageButton>(R.id.btnShowLayer) }
    private val seekBar: SeekBar by lazy { findViewById<SeekBar>(R.id.seekBar) }
    private val cameraLoader: CameraLoader by lazy {
        CameraSetting(this)
    }
    private var filterAdjuster: GPUImageFilterTools.FilterAdjuster? = null
    private var isPickColor = false
    private var isFlash = false
    private var isRecord = false
    private var imageAdapter: ImageAdapter? = null

    lateinit var handler: Handler
    private var isConvert = false
    private val updateTextTask = object : Runnable {
        override fun run() {
            if (!isConvert) {
                handler.postDelayed(this, 100)
                convertImagetoVideo()
            }
        }
    }

    fun convertImagetoVideo() {
            val bitmap = gpuImageView.capture()

            val newBitmap = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                bitmap.config
            )
            val canvas = Canvas(newBitmap)
            canvas.drawColor(Color.GREEN)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            imageAdapter?.addImage(newBitmap)
            Log.d("convertImagetoVideo", "imageAdapter count = " + imageAdapter!!.count)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonEvent()

        handler = Handler(Looper.getMainLooper())
        imageAdapter = ImageAdapter(this)
        gpuImageView.setRotation(getRotation(cameraLoader.getCameraOrientation()))
        gpuImageView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        gpuImageView.setBackgroundColor(Color.GREEN)
        gpuImageView.setOnTouchListener(this)
    }


    fun buttonEvent() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                filterAdjuster?.adjust(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        findViewById<View>(R.id.button_capture).setOnClickListener {
            saveSnapshot()
        }

        findViewById<View>(R.id.choose_color_red).setOnClickListener {
            updateFilterColor(Color.RED.red, Color.RED.green, Color.RED.blue)
        }
        findViewById<View>(R.id.choose_color_gray).setOnClickListener {
            updateFilterColor(Color.GRAY.red, Color.GRAY.green, Color.GRAY.blue)
        }
        findViewById<View>(R.id.choose_color_black).setOnClickListener {
            updateFilterColor(Color.BLACK.red, Color.BLACK.green, Color.BLACK.blue)
        }

        findViewById<View>(R.id.btn_record).setOnClickListener {
            if (!isRecord) {
                isConvert = false
                handler.post(updateTextTask)

                findViewById<Button>(R.id.btn_record).text = "Stop"
            } else {
                isConvert = true
                Log.d("convertImagetoVideo", "-----createMP4-----")
                Toast.makeText(this,"Process MP4...",Toast.LENGTH_LONG).show()
                val recordFile = getOutputMediaFile(MEDIA_TYPE_VIDEO)
                imageAdapter?.exportToMP4(recordFile)

                findViewById<Button>(R.id.btn_record).text = "Record"
            }

            isRecord = !isRecord
        }

        findViewById<ImageButton>(R.id.choose_flash).setOnClickListener {
            if (!isFlash) {
                if (cameraLoader.turnOnFlash()) {
                    findViewById<ImageButton>(R.id.choose_flash).setImageResource(
                        R.drawable.ic_turn_on_flash
                    )
                    isFlash = true
                }
            } else {
                if (cameraLoader.turnOffFlash()) {
                    findViewById<ImageButton>(R.id.choose_flash).setImageResource(
                        R.drawable.ic_turn_off_flash
                    )
                    isFlash = false
                }
            }
        }


        findViewById<View>(R.id.img_switch_camera).run {
            if (!cameraLoader.hasMultipleCamera()) {
                visibility = View.GONE
            }
            setOnClickListener {
                cameraLoader.switchCamera()
                gpuImageView.setRotation(1.0f * cameraLoader.getCameraOrientation())
            }
        }

        cameraLoader.setOnPreviewFrameListener { data, width, height ->
            gpuImageView.updatePreviewFrame(data, width, height)
        }

        btnShowLayer.setOnClickListener {
            if (imageLayer.visibility == View.VISIBLE) {
                imageLayer.visibility = View.GONE
                btnShowLayer.setImageResource(R.drawable.ic_show)
            } else {
                imageLayer.visibility = View.VISIBLE
                btnShowLayer.setImageResource(R.drawable.ic_hide)
            }
        }

        btnColor.setOnClickListener {
            if (!isPickColor) {
                onPause()
            } else {
                onResume()
            }

            isPickColor = !isPickColor

            GPUImageFilterTools.showDialog(this) { filter -> switchFilterTo(filter) }
        }
    }

    override fun onResume() {
        super.onResume()
        gpuImageView.doOnLayout {
            cameraLoader.onResume(it.width, it.height)
        }
    }

    override fun onPause() {
        cameraLoader.onPause()
        super.onPause()

//        if (isRecord) {
//            mMovieWriter!!.stopRecording()
//        }
    }

    private fun saveSnapshot() {
        val folderName = "GPUImage"
        val fileName = System.currentTimeMillis().toString() + ".png"

        val filterChromaKey =
            ChromaKeyFilter(ChromaKeyFilter.CHROMA_KEY_BLEND_FRAGMENT_SHADER_CAMERA)
        val filterChromaKeyBlend = GPUImageChromaKeyBlendFilter()

        val group = GPUImageFilterGroup()
        group.addFilter(filterChromaKeyBlend)
        group.addFilter(filterChromaKey)
        group.addFilter(GPUImageLevelsFilter())
        group.updateMergedFilters()

        val gpuimage = GPUImage(this)
//        gpuimage.setImage(gpuImageView.capture())
        gpuimage.setFilter(group)

        imageLayer.setImageBitmap(gpuimage.getBitmapWithFilterApplied())
//        imageLayer(this,gpuimage.getBitmapWithFilterApplied())
//        imageLayer.initGesture()
        imageLayer.visibility = View.VISIBLE
        switchFilterTo(
            GPUImageFilter(
                GPUImageFilter.NO_FILTER_VERTEX_SHADER,
                GPUImageFilter.NO_FILTER_FRAGMENT_SHADER
            )
        )
//        gpuImageView.saveToPictures(folderName, fileName) {
//            Toast.makeText(this, "$folderName/$fileName saved", Toast.LENGTH_SHORT).show()
//        }
        if (isPickColor) {
            onResume()
            isPickColor = false
        }
    }

    private fun getRotation(orientation: Int): Rotation {
        return when (orientation) {
            90 -> Rotation.ROTATION_90
            180 -> Rotation.ROTATION_180
            270 -> Rotation.ROTATION_270
            else -> Rotation.NORMAL
        }
    }

    private fun setupFilterCameraRecord() {

//        val bitmapTransparent = gpuImageView.capture()
//        val newBitmap = Bitmap.createBitmap(
//            bitmapTransparent.width,
//            bitmapTransparent.height,
//            bitmapTransparent.config
//        )
//        val canvas = Canvas(newBitmap)
//        canvas.drawColor(Color.GREEN)
//        canvas.drawBitmap(bitmapTransparent, 0f, 0f, null)
//
//
//        val filter = GPUImageChromaKeyBlendFilter()
//        filter.setThresholdSensitivity(0.4f)
//        filter.setColorToReplace(
//            Color.BLACK.red.toFloat(),
//            Color.BLACK.green.toFloat(), Color.BLACK.blue.toFloat()
//        )
//        filter.setSmoothing(0.1f)
//        filter.bitmap = newBitmap
//
//        val group = GPUImageFilterGroup()
//        group.addFilter(filter)
//        group.addFilter(mMovieWriter)
//        group.updateMergedFilters()
//        gpuImageView.filter = group
//        gpuImageView.requestRender()
    }

    private fun switchFilterTo(filter: GPUImageFilter) {
        gpuImageView.filter = filter
        gpuImageView.requestRender()

        filterAdjuster = GPUImageFilterTools.FilterAdjuster(filter)
        filterAdjuster?.adjust(seekBar.progress)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        if (isPickColor) {
            val x: Int = p1?.x?.roundToInt() ?: 0
            val y: Int = p1?.y?.roundToInt() ?: 0
            getPixelColor(x, y)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPixelColor(x: Int, y: Int) {

        Toast.makeText(
            this,
            "position: ( " + x + " ; " + y + " )",
            Toast.LENGTH_LONG
        ).show()

        val pixel = gpuImageView.capture().getPixel(x, y)
        updateFilterColor(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
    }

    fun updateFilterColor(_red: Int, _reen: Int, _blue: Int) {
        val red = 1.0f * _red / 255.0f
        val green = 1.0f * _reen / 255.0f
        val blue = 1.0f * _blue / 255.0f

        Toast.makeText(
            this,
            "Color: ( " + "%.2f".format(red) + " ; "
                    + "%.2f".format(green) + " ; "
                    + "%.2f".format(blue) + " )", Toast.LENGTH_LONG
        ).show()

        val filter = ChromaKeyFilter(
            ChromaKeyFilter.CHROMA_KEY_BLEND_FRAGMENT_SHADER_CAMERA,
            red, green, blue
        )

        filter.setThresholdSensitivity(0.4f)
        filter.setColorToReplace(red, green, blue)
        filter.setSmoothing(1.0f)
//        btnColor.setBackgroundColor(Color)
        switchFilterTo(filter)
    }

    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "MyCameraApp"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory")
                return null
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val mediaFile: File
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = File(
                mediaStorageDir.path + File.separator +
                        "IMG_" + timeStamp + ".jpg"
            )
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = File(
                (mediaStorageDir.path + File.separator +
                        "VID_" + timeStamp + ".mp4")
            )
        } else {
            return null
        }

        return mediaFile
    }
}
