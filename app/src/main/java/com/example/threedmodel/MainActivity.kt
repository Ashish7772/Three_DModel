package com.example.threedmodel

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity(), SensorEventListener {
    lateinit var progressBar: ProgressBar

    lateinit var sceneviewxml: SceneView

    private var localModel = "Blaze.glb"

    private var sensorRotation = MutableStateFlow(Quaternion.axisAngle(Vector3(0f, 0f, 0f), 0f))_
//    private lateinit var  sensorRotation: MutableStateFlow<Quaternion>

    private lateinit var sensorManager: SensorManager
    private lateinit var square: TextView
    private lateinit var gyrosquare: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sceneviewxml = findViewById<SceneView>(R.id.sceneView)
        progressBar = this.findViewById(R.id.progressbar)

        square = findViewById(R.id.sensorData)
        gyrosquare = findViewById(R.id.gyroData2)

        setUpSensorStuff()

        renderLocalObject()
    }


    fun renderLocalObject() {
        ModelRenderable.builder().setSource(
            this@MainActivity, RenderableSource.Builder().setSource(
                this@MainActivity, Uri.parse(localModel), RenderableSource.SourceType.GLB
            ).setScale(if (localModel.contains("x", true)) 50f else 8f)
                .setRecenterMode(RenderableSource.RecenterMode.CENTER).build()
        ).setRegistryId(localModel).build().thenAccept { modelRenderable: ModelRenderable ->
            progressBar.visibility = View.GONE
            addNodeToScene(modelRenderable)
        }.exceptionally { throwable: Throwable? ->
            progressBar.visibility = View.GONE
            Toast.makeText(this, throwable?.message, Toast.LENGTH_LONG).show()
            null
        }


    }

    private fun addNodeToScene(model: ModelRenderable) {
        Node().apply {
            setParent(sceneviewxml.scene)
            sceneviewxml.setBackgroundResource(R.color.black)
            localPosition = Vector3(0f, 0f, -2.3f)
//            var rot: Quaternion?
            CoroutineScope(Dispatchers.IO).launch {
                sensorRotation.collectLatest {
                    localRotation = it.inverted()

                    Log.e("Sensor data",it.inverted().toString())
                }
            }
            renderable = model
        }
    }


    //    private fun addNodeToScene(model: ModelRenderable) {
//        if (sceneviewxml != null) {
//            val transformationSystem = makeTransformationSystem()
//            var dragTransformableNode = DragTransformableNode(1f, transformationSystem)
//            dragTransformableNode?.renderable = model
//            sceneviewxml.scene.addChild(dragTransformableNode)
//            dragTransformableNode?.select()
//            sceneviewxml.scene
//                .addOnPeekTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
//                    transformationSystem.onTouch(
//                        hitTestResult,
//                        motionEvent
//                    )
//                }
//        }
//    }
    private fun makeTransformationSystem(): TransformationSystem {
        val footprintSelectionVisualizer = FootprintSelectionVisualizer()
        return TransformationSystem(resources.displayMetrics, footprintSelectionVisualizer)
    }

    override fun onResume() {
        super.onResume()
        try {
            sceneviewxml.resume()

        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        sceneviewxml.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            sceneviewxml.destroy()
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpSensorStuff() {
        // Create the sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // Specify the sensor you want to listen to
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        // Specify the sensor you want to listen to
//        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
//            sensorManager.registerListener(
//                this,
//                gyroscope,
//                SensorManager.SENSOR_DELAY_NORMAL
//            )
//        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Checks for the sensor we have registered
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            var sensorName = event?.sensor?.name
            square.text =
                ": X: " + event!!.values[0].toInt() + "; Y: " + event!!.values[1].toInt() + "; Z: " + event!!.values[2].toInt()
            sensorRotation.value=Quaternion.axisAngle(
                Vector3(event.values[0], event.values[1], event.values[2])
                , -45.0f
            )
        }

//        else if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
//
//            var sensorName = event?.sensor?.name
//            square.text =
//                ": X: " + event!!.values[0].toInt() + "; Y: " + event!!.values[1].toInt() + "; Z: " + event!!.values[2].toInt()
//            sensorRotation.value=Quaternion.axisAngle(
//                Vector3(event.values[0], event.values[1], event.values[2])
//                , -45.0f
//            )
//        }


    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }


}

class DragTransformableNode(val radius: Float, transformationSystem: TransformationSystem) :
    TransformableNode(transformationSystem) {
    val dragRotationController = DragRotationController(
        this,
        transformationSystem.dragRecognizer
    )
}

