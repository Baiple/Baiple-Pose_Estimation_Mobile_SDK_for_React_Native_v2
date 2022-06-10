package com.posedetect

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.posedetect.lib.Device
import com.posedetect.lib.MoveNet
import com.posedetect.lib.PoseClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class PdFragment(private val reactContext: ReactContext) : Fragment() {
  companion object {
    private const val FRAGMENT_DIALOG = "dialog"
  }

  private var device = Device.CPU
  private var isClassifyPose = true
  private var isAttached = false
  private var modelPath: String? = null

  private lateinit var surfaceView: SurfaceView
  private var cameraSource: CameraSource? = null

  private val requestPermissionLauncher =
    registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
      if (isGranted) {
        // Permission is granted. Continue the action or workflow in your app.
        Util.d("requestPermissionLauncher: granted, will openCamera")
        openCamera()
      } else {
        // Explain to the user that the feature is unavailable because the
        // features requires a permission that the user has denied. At the
        // same time, respect the user's decision. Don't link to system
        // settings in an effort to convince the user to change their decision.
        Util.d("requestPermissionLauncher: user denied")
        fireCameraError("Permission denied")
      }
    }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    //requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    surfaceView = SurfaceView(requireContext())

    if (!isCameraPermissionGranted()) {
      requestPermission()
    }

    return surfaceView // this CustomView could be any view that you want to render
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    isAttached = true
    modelPath?.let {
      if (it.isNotBlank()) createPoseEstimator(it)
    }
  }

  fun setModelPath(modelPath: String) {
    this.modelPath = modelPath
    if (isAttached) {
      createPoseEstimator(modelPath)
    }
  }

  /*override fun onDestroyView() {
    super.onDestroyView()
    //Util.d("PdFragment.onDestroyView")
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    //Util.d("PdFragment.onViewCreated")
  }

  override fun onDestroy() {
    super.onDestroy()
    //Util.d("PdFragment.onDestroy")
  }
  */

  override fun onStart() {
    super.onStart()
    Util.d("PdFragment.onStart")
    openCamera()
  }

  override fun onPause() {
    super.onPause()
    Util.d("PdFragment.onPause")
    cameraSource?.close()
    cameraSource = null
  }

  override fun onResume() {
    super.onResume()
    Util.d("PdFragment.onResume")
    if (cameraSource == null) {
      openCamera()
    } else {
      cameraSource?.resume()
    }
  }



  // check if permission is granted or not.
  private fun isCameraPermissionGranted(): Boolean {
    return requireContext().checkPermission(
      Manifest.permission.CAMERA,
      Process.myPid(),
      Process.myUid()
    ) == PackageManager.PERMISSION_GRANTED
  }

  // open camera
  private fun openCamera() {
    if (!isCameraPermissionGranted()) {
      Util.w("openCamera: Camera not granted")
      fireCameraError("Permission not granted")
    } else {
      if (cameraSource == null) {
        Util.d("will createSource")
        cameraSource =
          CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
            /*override fun onFPSListener(fps: Int) {
              //tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
            }*/

            override fun onDetectedInfo(
              personScore: Float,
              poseLabels: List<Pair<String, Float>>?
            ) {
              poseLabels?.sortedByDescending { it.second }?.let {
                if (it.isNotEmpty()) {
                  val event: WritableMap = Arguments.createMap()
                  event.putString("pose", it[0].first)
                  event.putDouble("score", it[0].second.toDouble())
                  fireEventToJs("onPoseDetected", event)
                }
              }
            }

          }).apply {
            prepareCamera()
          }
        isPoseClassifier()
        lifecycleScope.launch(Dispatchers.Main) {
          cameraSource?.initCamera()
        }
      }
      modelPath?.let {
        if (isAttached && it.isNotBlank()) {
          createPoseEstimator(it)
        }
      }
    }
  }

  private fun isPoseClassifier() {
    cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(requireContext()) else null)
  }

  private fun createPoseEstimator(modelFilePath: String) {
    var modelData: ByteBuffer
    try {
      val file = File(modelFilePath)
      val ins = FileInputStream(file)
      modelData = ins.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
    } catch (ex1: Exception) {
      fireModelError("Model($modelFilePath) reading failed: ${ex1.message}")
      return
    }

    try {
      val poseDetector = MoveNet.create(requireContext(), device, modelData)
      poseDetector.let { detector ->
        cameraSource?.setDetector(detector)
      }
    } catch (ex2: Exception) {
      fireModelError("Model($modelFilePath) creation failed: ${ex2.message}")
    }
  }

  private fun requestPermission() {
    when (PackageManager.PERMISSION_GRANTED) {
      ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
      ) -> {
        // You can use the API that requires the permission.
        openCamera()
      }
      else -> {
        // You can directly ask for the permission.
        // The registered ActivityResultCallback gets the result of this request.
        requestPermissionLauncher.launch(
          Manifest.permission.CAMERA
        )
      }
    }
  }

  fun startRecordingVideo() {
    lifecycleScope.launch(Dispatchers.Main) {
      cameraSource?.startRecordingVideo()
    }
  }

  fun stopRecordingVideo() {
    lifecycleScope.launch(Dispatchers.Main) {
      cameraSource?.stopRecordingVideo()
    }
  }


  private fun fireEventToJs(name: String, payload: WritableMap) {
    reactContext.getJSModule(RCTEventEmitter::class.java).receiveEvent(id, name, payload)
  }

  private fun fireCameraError(msg: String) {
    Util.d("will fireCameraError id=$id, msg=$msg")
    val event: WritableMap = Arguments.createMap()
    event.putString("msg", msg)
    fireEventToJs("onCameraError", event)
  }

  private fun fireModelError(msg: String) {
    Util.d("will fireModelError msg=$msg")
    val event: WritableMap = Arguments.createMap()
    event.putString("msg", msg)
    fireEventToJs("onModelError", event)
  }

}
