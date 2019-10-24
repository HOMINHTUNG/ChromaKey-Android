package com.tungjobs.chromakeyvideo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.doOnLayout
import jp.co.cyberagent.android.gpuimage.GLTextureView
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLevelsFilter
import jp.co.cyberagent.android.gpuimage.util.Rotation
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), View.OnTouchListener{

    private val gpuImageView: GPUImageView by lazy { findViewById<GPUImageView>(R.id.surfaceView) }
    private val imageLayer: GestureImage by lazy { findViewById<GestureImage>(R.id.imageView) }
    private val btnColor: ImageButton by lazy { findViewById<ImageButton>(R.id.button_choose_filter) }
    private val btnShowLayer: ImageButton by lazy { findViewById<ImageButton>(R.id.btnShowLayer) }
    //    private val gpuImageView1: GPUImageView by lazy { findViewById<GPUImageView>(R.id.surfaceView1) }
    private val seekBar: SeekBar by lazy { findViewById<SeekBar>(R.id.seekBar) }
    private val cameraLoader: CameraLoader by lazy {
        CameraSetting(this)
    }

    private var filterAdjuster: GPUImageFilterTools.FilterAdjuster? = null
    private var isPickColor = false
    private var isFlash = false
    private var isRecord = false

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ResourceAsColor", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonEvent()

        gpuImageView.setRotation(getRotation(cameraLoader.getCameraOrientation()))
        gpuImageView.setRenderMode(GPUImageView.RENDERMODE_WHEN_DIRTY)
        gpuImageView.setOnTouchListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun buttonEvent(){
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
            updateFilterColor(Color.RED.red,Color.RED.green,Color.RED.blue)
        }
        findViewById<View>(R.id.choose_color_gray).setOnClickListener {
            updateFilterColor(Color.GRAY.red,Color.GRAY.green,Color.GRAY.blue)
        }
        findViewById<View>(R.id.choose_color_black).setOnClickListener {
            updateFilterColor(Color.BLACK.red,Color.BLACK.green,Color.BLACK.blue)
        }

        findViewById<View>(R.id.btn_record).setOnClickListener {
            if (!isRecord){
                cameraLoader.onStartCapture()
                findViewById<Button>(R.id.btn_record).text = "Stop"
            }else{

                onResume()
                findViewById<Button>(R.id.btn_record).text = "Record"
            }

            isRecord = !isRecord
        }

        findViewById<ImageButton>(R.id.choose_flash).setOnClickListener {
            if (!isFlash){
                if (cameraLoader.turnOnFlash()){
                    findViewById<ImageButton>(R.id.choose_flash).setImageResource(R.drawable.ic_turn_on_flash)
                    isFlash = true
                }
            }else{
                if (cameraLoader.turnOffFlash()) {
                    findViewById<ImageButton>(R.id.choose_flash).setImageResource(R.drawable.ic_turn_off_flash)
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
                gpuImageView.setRotation(getRotation(cameraLoader.getCameraOrientation()))
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
            if (!isPickColor){
                onPause()
            }else{
                onResume()
            }

            isPickColor = !isPickColor
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
    }

    private fun saveSnapshot() {
//        val folderName = "GPUImage"
//        val fileName = System.currentTimeMillis().toString() + ".png"

        val filterChromaKey = ChromaKeyFilter(ChromaKeyFilter.CHROMA_KEY_BLEND_FRAGMENT_SHADER_CAMERA)
        val filterChromaKeyBlend = GPUImageChromaKeyBlendFilter()

        val group = GPUImageFilterGroup()
        group.addFilter(filterChromaKeyBlend)
        group.addFilter(filterChromaKey)
        group.addFilter(GPUImageLevelsFilter())
        group.updateMergedFilters()

        val gpuimage = GPUImage(this)
        gpuimage.setImage(gpuImageView.capture())
        gpuimage.setFilter(group)

        imageLayer.setImageBitmap(gpuimage.getBitmapWithFilterApplied())
        imageLayer.visibility = View.VISIBLE

        switchFilterTo(GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, GPUImageFilter. NO_FILTER_FRAGMENT_SHADER))

//        gpuImageView.saveToPictures(folderName, fileName) {
//            Toast.makeText(this, "$folderName/$fileName saved", Toast.LENGTH_SHORT).show()
//        }
        if (isPickColor){
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

    private fun switchFilterTo(filter: GPUImageFilter) {
        gpuImageView.filter = filter
        filterAdjuster = GPUImageFilterTools.FilterAdjuster(filter)
        filterAdjuster?.adjust(seekBar.progress)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        if (isPickColor){
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
            Toast.LENGTH_LONG).show()

        val pixel = gpuImageView.capture().getPixel(x, y)
        updateFilterColor(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateFilterColor(_red:Int, _reen:Int, _blue:Int){
        val red = 1.0f * _red / 255.0f
        val green = 1.0f * _reen / 255.0f
        val blue = 1.0f * _blue / 255.0f

        Toast.makeText(
            this,
            "Color: ( " + "%.2f".format(red) + " ; "
                    + "%.2f".format(green) + " ; "
                    + "%.2f".format(blue) + " )", Toast.LENGTH_LONG).show()

        val filter = ChromaKeyFilter(
            ChromaKeyFilter.CHROMA_KEY_BLEND_FRAGMENT_SHADER_CAMERA,
            red, green, blue)
        filter.setSmoothing(1.0f)
//        btnColor.setBackgroundColor(Color.rgb(red, green, blue))
        switchFilterTo(filter)
    }
}
