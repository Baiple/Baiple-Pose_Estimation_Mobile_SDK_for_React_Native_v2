package com.posedetect

import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableType
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class PoseDetectViewManager(val reactContext: ReactApplicationContext) : SimpleViewManager<FrameLayout>() {
  companion object {
    private const val COMMAND_CREATE = 1
    private const val COMMAND_START_RECORD = 2
    private const val COMMAND_STOP_RECORD = 3
  }

  private var pdFrag: PdFragment? = null

  override fun getName() = "PoseDetectView"

  override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout {
    return FrameLayout(reactContext)
  }

  override fun getCommandsMap(): MutableMap<String, Int>? {
    return MapBuilder.of(
      "create", COMMAND_CREATE,
      "startRecord", COMMAND_START_RECORD,
      "stopRecord", COMMAND_STOP_RECORD
    )
  }

  override fun getExportedCustomBubblingEventTypeConstants(): MutableMap<String, Any>? {
    return MapBuilder.of(
      "onCameraError", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onCameraError")),
      "onModelError", MapBuilder.of("registrationName", MapBuilder.of("bubbled", "onModelError")),
      "onPoseDetected", MapBuilder.of("phasedRegistrationNames", MapBuilder.of("bubbled", "onPoseDetected"))
    )
  }

  override fun receiveCommand(root: FrameLayout, commandId: String?, args: ReadableArray?) {
    super.receiveCommand(root, commandId, args)

    val reactNativeViewId = args?.getInt(0)
    val commandIdInt = commandId?.toInt()

    /*var logCommand = "CameraManager received command $commandId("
    for (i in 0..(args?.size() ?: 0)) {
      if (i > 0) {
        logCommand += ", "
      }
      logCommand += when (args?.getType(0)) {
        ReadableType.Null -> "Null"
        ReadableType.Array -> "Array"
        ReadableType.Boolean -> "Boolean"
        ReadableType.Map -> "Map"
        ReadableType.Number -> "Number"
        ReadableType.String -> "String"
        else ->  ""
      }
    }
    logCommand += ")"
    Util.d(logCommand)*/

    when (commandIdInt) {
      COMMAND_CREATE -> {
        createFragment(root, reactNativeViewId!!)
      }
      COMMAND_START_RECORD -> {
        Util.i("TODO: start recording")
        pdFrag?.let{
          it.startRecordingVideo()
        }
      }
      COMMAND_STOP_RECORD -> {
        Util.i("TODO: stop recording")
        pdFrag?.let{
          it.stopRecordingVideo()
        }
      }
    }
  }

  @ReactProp(name = "modelPath")
  fun setModelPath(view: FrameLayout, modelPath: String) {
    pdFrag?.let {
      it.setModelPath(modelPath)
    }
  }

  private fun createFragment(root: FrameLayout, reactNativeViewId: Int) {
    val parentView = root.findViewById<FrameLayout>(reactNativeViewId) as ViewGroup
    setupLayout(parentView)
    pdFrag = PdFragment(reactContext)
    val activity = reactContext.currentActivity as FragmentActivity?
    activity!!.supportFragmentManager
      .beginTransaction()
      .replace(reactNativeViewId, pdFrag!!, reactNativeViewId.toString())
      .commit()
  }

  private fun setupLayout(view: ViewGroup) {
    Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
      override fun doFrame(frameTimeNanos: Long) {
        manuallyLayoutChildren(view)
        view.viewTreeObserver.dispatchOnGlobalLayout()
        Choreographer.getInstance().postFrameCallback(this)
      }
    })
  }

  fun manuallyLayoutChildren(view: ViewGroup) {
    for (i in 0 until view.childCount) {
      val child = view.getChildAt(i)
      child.measure(
        View.MeasureSpec.makeMeasureSpec(view.measuredWidth, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(view.measuredHeight, View.MeasureSpec.EXACTLY)
      )
      child.layout(0, 0, child.measuredWidth, child.measuredHeight)
    }
  }

}
