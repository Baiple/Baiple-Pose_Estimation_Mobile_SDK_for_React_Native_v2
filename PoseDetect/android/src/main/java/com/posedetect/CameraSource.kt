package com.posedetect

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.*
import android.hardware.camera2.CameraDevice.TEMPLATE_RECORD
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.SurfaceView
import com.posedetect.lib.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraSource(
  private val surfaceView: SurfaceView,
  private val listener: CameraSourceListener? = null
) {

  companion object {
    private const val PREVIEW_WIDTH = 640
    private const val PREVIEW_HEIGHT = 480

    /** Threshold for confidence score. */
    private const val MIN_CONFIDENCE = .2f
    private const val TAG = "Camera Source"
  }
  private val lock = Any()
  private var detector: PoseDetector? = null
  private var classifier: PoseClassifier? = null
  private var isTrackerEnabled = false
  private var yuvConverter: YuvToRgbConverter = YuvToRgbConverter(surfaceView.context)
  private lateinit var imageBitmap: Bitmap

  /** Frame count that have been processed so far in an one second interval to calculate FPS. */
  private var fpsTimer: Timer? = null
  private var frameProcessedInOneSecondInterval = 0
  private var framesPerSecond = 0

  /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
  private val cameraManager: CameraManager by lazy {
    val context = surfaceView.context
    context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  }

  /** Readers used as buffers for camera still shots */
  private var imageReader: ImageReader? = null

  /** The [CameraDevice] that will be opened in this fragment */
  private var camera: CameraDevice? = null

  /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
  private var session: CameraCaptureSession? = null
  private var sessionRecorder: CameraCaptureSession? = null

  /** [HandlerThread] where all buffer reading operations run */
  private var imageReaderThread: HandlerThread? = null

  /** [Handler] corresponding to [imageReaderThread] */
  private var imageReaderHandler: Handler? = null
  private var cameraId: String = ""

  //영상 녹화 : 2022-05-17 추가

  /** An additional thread for running tasks that shouldn't block the UI.  */
  private var backgroundThread: HandlerThread? = null
  /** A [Handler] for running tasks in the background. */
  private var backgroundHandler: Handler? = null
  private var mRecorderSurface: Surface? = null
  /**
   * Output file for video
   */
  private var isRecordingVideo: Boolean = false //Whether the app is recording video now
  private var nextVideoAbsolutePath: String? = null
  private var mediaRecorder: MediaRecorder? = null
  val MEDIA_TYPE_IMAGE = 1
  val MEDIA_TYPE_VIDEO = 2
  /**
   * A reference to the current [android.hardware.camera2.CameraCaptureSession] for
   * preview.
   */
  private var captureSession: CameraCaptureSession? = null


  suspend fun initCamera() {
    camera = openCamera(cameraManager, cameraId)

    imageReader =
      ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3)
    imageReader?.setOnImageAvailableListener({ reader ->
      val image = reader.acquireLatestImage()
      if (image != null) {
        if (!::imageBitmap.isInitialized) {
          imageBitmap =
            Bitmap.createBitmap(
              PREVIEW_WIDTH,
              PREVIEW_HEIGHT,
              Bitmap.Config.ARGB_8888
            )
        }
        yuvConverter.yuvToRgb(image, imageBitmap)
        // Create rotated version for portrait display
        val rotateMatrix = Matrix()
        rotateMatrix.postRotate(90.0f)

        val rotatedBitmap = Bitmap.createBitmap(
          imageBitmap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
          rotateMatrix, false
        )
        processImage(rotatedBitmap)
        image.close()
      }
    }, imageReaderHandler)

    imageReader?.surface?.let { surface ->
      session = createSession(listOf(surface))
      val cameraRequest = camera?.createCaptureRequest(
        CameraDevice.TEMPLATE_PREVIEW
      )?.apply {
        addTarget(surface)
      }
      cameraRequest?.build()?.let {
        session?.setRepeatingRequest(it, null, null)
      }
    }
  }

  private suspend fun createSession(targets: List<Surface>): CameraCaptureSession =
    suspendCancellableCoroutine { cont ->
      camera?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(captureSession: CameraCaptureSession) =
          cont.resume(captureSession)

        override fun onConfigureFailed(session: CameraCaptureSession) {
          cont.resumeWithException(Exception("Session error"))
        }
      }, null)
    }

  private suspend fun createSessionRecorder(targets: List<Surface>): CameraCaptureSession =
    suspendCancellableCoroutine { cont ->
      camera?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(captureSession: CameraCaptureSession) =
          cont.resume(captureSession)

        override fun onConfigureFailed(session: CameraCaptureSession) {
          cont.resumeWithException(Exception("Session error"))
        }
      }, null)
    }

  @SuppressLint("MissingPermission")
  private suspend fun openCamera(manager: CameraManager, cameraId: String): CameraDevice =
    suspendCancellableCoroutine { cont ->
      manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) = cont.resume(camera)

        override fun onDisconnected(camera: CameraDevice) {
          camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
          if (cont.isActive) cont.resumeWithException(Exception("Camera error"))
        }
      }, imageReaderHandler)
    }

  fun prepareCamera() {
    for (cameraId in cameraManager.cameraIdList) {
      val characteristics = cameraManager.getCameraCharacteristics(cameraId)

      // We don't use a front facing camera in this sample.
      val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
      if (cameraDirection != null &&
        cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
      ) {
        continue
      }
      this.cameraId = cameraId
    }
  }

  fun setDetector(detector: PoseDetector) {
    synchronized(lock) {
      if (this.detector != null) {
        this.detector?.close()
        this.detector = null
      }
      this.detector = detector
    }
  }

  fun setClassifier(classifier: PoseClassifier?) {
    synchronized(lock) {
      if (this.classifier != null) {
        this.classifier?.close()
        this.classifier = null
      }
      this.classifier = classifier
    }
  }

  /**
   * Set Tracker for Movenet MuiltiPose model.
   */
  fun setTracker(trackerType: TrackerType) {
    isTrackerEnabled = trackerType != TrackerType.OFF
    (this.detector as? MoveNetMultiPose)?.setTracker(trackerType)
  }

  fun resume() {
    imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    imageReaderHandler = Handler(imageReaderThread!!.looper)
    fpsTimer = Timer()
    fpsTimer?.scheduleAtFixedRate(
      object : TimerTask() {
        override fun run() {
          framesPerSecond = frameProcessedInOneSecondInterval
          frameProcessedInOneSecondInterval = 0
        }
      },
      0,
      1000
    )
    startBackgroundThread()
  }

  fun close() {
    Util.d("#### CmaeraSource.close Start")
    session?.close()
    session = null
    camera?.close()
    camera = null
    imageReader?.close()
    imageReader = null
    stopImageReaderThread()
    detector?.close()
    detector = null
    classifier?.close()
    classifier = null
    fpsTimer?.cancel()
    fpsTimer = null
    frameProcessedInOneSecondInterval = 0
    framesPerSecond = 0
    //2022-05-17 추가: 영상 녹화
    mediaRecorder?.release()
    mediaRecorder = null
    stopBackgroundThread()
    Util.d("#### CmaeraSource.close END")
  }

  // process image
  private fun processImage(bitmap: Bitmap) {
    val persons = mutableListOf<Person>()
    var classificationResult: List<Pair<String, Float>>? = null

    synchronized(lock) {
      detector?.estimatePoses(bitmap)?.let {
        persons.addAll(it)

        // if the model only returns one item, allow running the Pose classifier.
        if (persons.isNotEmpty()) {
          classifier?.run {
            classificationResult = classify(persons[0])
          }
        }
      }
    }
    frameProcessedInOneSecondInterval++
    if (frameProcessedInOneSecondInterval == 1) {
      // send fps to view
      //listener?.onFPSListener(framesPerSecond)
    }

    // if the model returns only one item, show that item's score.
    if (persons.isNotEmpty() && persons[0].score > 0.5f) {
      listener?.onDetectedInfo(persons[0].score, classificationResult)
    }
    visualize(persons, bitmap)
  }

  private fun visualize(persons: List<Person>, bitmap: Bitmap) {

    val outputBitmap = VisualizationUtils.drawBodyKeypoints(
      bitmap,
      persons.filter { it.score > MIN_CONFIDENCE }, isTrackerEnabled
    )

    val holder = surfaceView.holder
    val surfaceCanvas = holder.lockCanvas()
    surfaceCanvas?.let { canvas ->
      val screenWidth: Int
      val screenHeight: Int
      val left: Int
      val top: Int

      if (canvas.height > canvas.width) {
        val ratio = outputBitmap.height.toFloat() / outputBitmap.width
        screenWidth = canvas.width
        left = 0
        screenHeight = (canvas.width * ratio).toInt()
        top = (canvas.height - screenHeight) / 2
      } else {
        val ratio = outputBitmap.width.toFloat() / outputBitmap.height
        screenHeight = canvas.height
        top = 0
        screenWidth = (canvas.height * ratio).toInt()
        left = (canvas.width - screenWidth) / 2
      }
      val right: Int = left + screenWidth
      val bottom: Int = top + screenHeight

      canvas.drawBitmap(
        outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
        Rect(left, top, right, bottom), null
      )
      surfaceView.holder.unlockCanvasAndPost(canvas)
    }
  }

  private fun stopImageReaderThread() {
    imageReaderThread?.quitSafely()
    try {
      imageReaderThread?.join()
      imageReaderThread = null
      imageReaderHandler = null
    } catch (e: InterruptedException) {
      Log.d(TAG, e.message.toString())
    }
  }

  /**
   * Starts a background thread and its [Handler].
   */
  private fun startBackgroundThread() {
    backgroundThread = HandlerThread("CameraBackground")
    backgroundThread?.start()
    backgroundHandler = backgroundThread?.looper?.let { Handler(it) }
  }

  /**
   * Stops the background thread and its [Handler].
   */
  private fun stopBackgroundThread() {
    backgroundThread?.quitSafely()
    try {
      backgroundThread?.join()
      backgroundThread = null
      backgroundHandler = null
    } catch (e: InterruptedException) {
      Log.e(TAG, e.toString())
    }
  }
  /**
   * [CaptureRequest.Builder] for the camera preview
   */
  private lateinit var previewRequestBuilder: CaptureRequest.Builder

  /**
   * Update the camera preview. [startPreview] needs to be called in advance.
   */
  private fun updatePreview() {
    if (camera == null) return

    try {
      setUpCaptureRequestBuilder(previewRequestBuilder)
      HandlerThread("CameraPreview").start()
      session?.setRepeatingRequest(previewRequestBuilder.build(),
        null, backgroundHandler)
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    }

  }

  private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder?) {
    builder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
  }

  @Throws(IOException::class)
  private fun setUpMediaRecorder() {

    mediaRecorder = MediaRecorder() // 2022-05-17 추가

    var file = getOutputMediaFile(2)
//        videoFile = file.toString()
    Util.d("#### CmaeraSource.setUpMediaRecorder Start")
    // /storage/emulated/0/Android/data/com.example.posedetect/files/1652874900643.mp4
//        Util.d("#### videoFile=$videoFile")
    Util.d("#### videoFile=${file!!.absolutePath}")
    mediaRecorder?.apply {
      setVideoSource(MediaRecorder.VideoSource.SURFACE)
      setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      setOutputFile(file.absolutePath)
      setVideoEncodingBitRate(10000000)
      setVideoFrameRate(30)
      setVideoSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
      setOrientationHint(90)
      setVideoEncoder(MediaRecorder.VideoEncoder.H264)
      prepare()
    }
  }

  suspend fun startRecordingVideo() {
    Util.d("#### CmaeraSource.startRecordingVideo Start")
    try {
      releaseMediaRecorder()
      setUpMediaRecorder()
      // 기존 세션을 초기화
      closeSession()

      val mediaSurface = mediaRecorder!!.surface
      val imageSurface = imageReader!!.surface

      val allSurface: MutableList<Surface> = ArrayList()
      allSurface.add(imageSurface)
      allSurface.add(mediaSurface)

      val cameraRequest = camera?.createCaptureRequest(
          CameraDevice.TEMPLATE_RECORD
        )?.apply {
          addTarget(imageSurface)
          addTarget(mediaSurface)
        }
      sessionRecorder = createSessionRecorder(allSurface)
        cameraRequest?.build()?.let {
          sessionRecorder?.setRepeatingRequest(it, null, null)
          isRecordingVideo = true
          mediaRecorder?.start()
        }

      Util.d("#### 2 #####")
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    } catch (e: IOException) {
      Log.e(TAG, e.toString())
    }
    Util.d("#### CmaeraSource.startRecordingVideo END")
  }

  suspend fun stopRecordingVideo() {
    Util.d("#### CmaeraSource.stopRecordingVideo Start")
      closeSessionRecorder()
      releaseMediaRecorder()

    if(isRecordingVideo) {
      //원본 이미지 스트림으로 다시 연결한다.
      imageReader?.surface?.let { surface ->
        session = createSession(listOf(surface))
        val cameraRequest = camera?.createCaptureRequest(
          CameraDevice.TEMPLATE_PREVIEW
        )?.apply {
          addTarget(surface)
        }
        cameraRequest?.build()?.let {
          session?.setRepeatingRequest(it, null, null)
        }
      }
      isRecordingVideo = false
    }

    Util.d("#### CmaeraSource.stopRecordingVideo END")
  }

  private fun closePreviewSession() {
    captureSession?.close()
    captureSession = null
  }

  private fun closeSession() {
    if (session != null) {
      session?.close()
      session = null
    }
  }

  private fun closeSessionRecorder() {
    if (sessionRecorder != null) {
      sessionRecorder?.close()
      sessionRecorder = null
    }
  }

  private fun releaseMediaRecorder() {
    mediaRecorder?.reset() // clear recorder configuration
    mediaRecorder?.release() // release the recorder object
    mediaRecorder = null
  }

  /** Create a file Uri for saving an image or video */
  private fun getOutputMediaFileUri(type: Int): Uri {
    return Uri.fromFile(getOutputMediaFile(type))
  }

  /** Create a File for saving an image or video */
  private fun getOutputMediaFile(type: Int): File? {
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    val mediaStorageDir = File(
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
      "MyCameraApp"
    )
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.
    // Create the storage directory if it does not exist
    mediaStorageDir.apply {
      if (!exists()) {
        if (!mkdirs()) {
          Log.d("MyCameraApp", "failed to create directory")
          return null
        }
      }
    }

    // Create a media file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    return when (type) {
      MEDIA_TYPE_IMAGE -> {
        File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
      }
      MEDIA_TYPE_VIDEO -> {
        File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
      }
      else -> null
    }
  }

  internal class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
      return java.lang.Long.signum(
        lhs.width.toLong() * lhs.height -
          rhs.width.toLong() * rhs.height
      )
    }
  }

  interface CameraSourceListener {
    //fun onFPSListener(fps: Int)
    fun onDetectedInfo(personScore: Float, poseLabels: List<Pair<String, Float>>?)
  }
}
