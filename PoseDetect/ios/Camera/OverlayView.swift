import UIKit
import os
//import PoseLib


enum Constants {
  // Configs for the TFLite interpreter.
  static let defaultThreadCount = 4
  static let defaultDelegate: Delegates = .gpu
  static let defaultModelType: ModelType = .movenetThunder

  // Minimum score to render the result.
  static let minimumScore: Float32 = 0.2
}

/// Custom view to visualize the pose estimation result on top of the input image.
//@available(iOS 13.0, *)
class OverlayView: UIImageView, CameraFeedManagerDelegate {

  private var isAddedObserver = false

  // MARK: Pose estimation model configs
  //private var modelType: ModelType = Constants.defaultModelType
  private var modelFilePath: String? = nil
  private var threadCount: Int = Constants.defaultThreadCount
  private var delegate: Delegates = Constants.defaultDelegate
  private let minimumScore = Constants.minimumScore

  // MARK: Visualization
  // Relative location of `overlayView` to `previewView`.
  private var imageViewFrame: CGRect?

  // MARK: Controllers that manage functionality
  // Handles all data preprocessing and makes calls to run inference.
  private var poseEstimator: PoseEstimator?
  private var cameraFeedManager: CameraFeedManager!


  // Serial queue to control all tasks related to the TFLite model.
  let queue = DispatchQueue(label: "serial_queue")

  // Flag to make sure there's only one frame processed at each moment.
  var isRunning = false
  var wasInit = false
  var isRemoved = false


  /*@objc var color: String = "" {
    didSet {
      print("OverlayView.color(\(color)) called")
      //self.backgroundColor = hexStringToUIColor(hexColor: color)
    }
  }*/

  @objc var modelPath: String = "" {
    didSet {
      print("OverlayView.modelPath(\(modelPath)) called")
      if !modelPath.isEmpty {
        modelFilePath = modelPath
        updateModel()
      }

      /*if onModelError != nil {
        onModelError!(["msg": "fake error message"])
        onCameraError?(["msg": "fake error message"])
      }*/
    }
  }

  @objc var onCameraError: RCTDirectEventBlock?
  @objc var onModelError: RCTDirectEventBlock?
  @objc var onPoseDetected: RCTDirectEventBlock?

  func startRecord() {
    print("TODO: startRecord")
    cameraFeedManager.startRecording()
  }
  
  func stopRecord() {
    print("TODO: stopRecord")
    cameraFeedManager.stopRecording()
  }
  
  init(_ frame: CGRect) {
    super.init(frame: frame)

    print("overlay.init called")
    //updateModel()
    //configCameraCapture()
  }

  required init?(coder aDecoder: NSCoder) { fatalError("init(coder:) has not been implemented"); }

  override func didMoveToSuperview() {
    print("overlay.didMoveToSuperview called \(isRemoved)")
    if isRemoved {
      return
    }
    
    if !wasInit {
      wasInit = true
      print("init CameraFeedManager")
      cameraFeedManager = CameraFeedManager(self)
    }
    cameraFeedManager?.startRunning()
    
    super.didMoveToSuperview()
  }

  override func removeFromSuperview() {
    print("overlay.removeFromSuperview called")
    //NotificationCenter.default.removeObserver(self, name: Notification.UI, object: <#T##Any?#>)
    //[[NSNotificationCenter defaultCenter] removeObserver:self name:UIDeviceOrientationDidChangeNotification object:[UIDevice currentDevice]];
    /*dispatch_async( self.sessionQueue, ^{
        if ( self.setupResult == CKSetupResultSuccess ) {
            [self.session stopRunning];
            [self removeObservers];
        }
    } );*/

    cameraFeedManager?.stopRunning()
    //self.removeObservers()

    isRemoved = true
    super.removeFromSuperview()
  }

  override func layoutSubviews() {
    print("overlay.layoutSubviews called")
    imageViewFrame = self.frame
  }

  private func createPoseEstimator(threadCount: Int, modelType: ModelType, modelPath: String) -> PoseEstimator? {
    var esti: PoseEstimator? = nil
    do {
      if modelType == .posenet {
        esti = try PoseNet(threadCount: threadCount, delegate: Delegates.gpu, modelPath: modelPath)
      } else {
        esti = try MoveNet(threadCount: threadCount, delegate: Delegates.gpu, modelPath: modelPath)
      }
    } catch let error {
      print("ModelError: \(String(describing: error))")
      onModelError!(["msg": String(describing: error)])
      return nil
    }
    return esti
  }

  /// Call this method when there's change in pose estimation model config, including changing model
  /// or updating runtime config.
  private func updateModel() {
    // Update the model in the same serial queue with the inference logic to avoid race condition
//    print("updateModel called model=\(modelFilePath)")
    if let filePath = modelFilePath {
      queue.async {
        self.poseEstimator = self.createPoseEstimator(
          threadCount: self.threadCount,
          modelType: .movenetThunder,
          modelPath: filePath //Bundle.main.path(forResource: "movenet_thunder", ofType: "tflite")!
        )
      }
    }
  }

  func cameraInitFailed(_ reason: String) {
    print("fire event to js: onCameraError=\(onCameraError != nil)")
    onCameraError?(["msg": reason])
  }

  func cameraFeedManager(_ cameraFeedManager: CameraFeedManager, didOutput pixelBuffer: CVPixelBuffer) {
    self.runModel(pixelBuffer)
  }

  /// Run pose estimation on the input frame from the camera.
  private func runModel(_ pixelBuffer: CVPixelBuffer) {
    // Guard to make sure that there's only 1 frame process at each moment.
    guard !isRunning else { return }

    // Guard to make sure that the pose estimator is already initialized.
    guard let estimator = poseEstimator else { return }

    // Run inference on a serial queue to avoid race condition.
    queue.async {
      self.isRunning = true
      defer { self.isRunning = false }

      // Run pose estimation
      do {
        let (result, times) = try estimator.estimateSinglePose(
            on: pixelBuffer)

        // Return to main thread to show detection results on the app UI.
        DispatchQueue.main.async {
          //self.totalTimeLabel.text = String(format: "%.2fms", times.total * 1000)
          //self.scoreLabel.text = String(format: "%.3f", result.score)

          // Allowed to set image and overlay
          let image = UIImage(ciImage: CIImage(cvPixelBuffer: pixelBuffer))

          // If score is too low, clear result remaining in the overlayView.
          if result.score < self.minimumScore {
            //self.overlayView.image = image
            self.image = image
            return
          }

          // Visualize the pose estimation result.
          //self.overlayView.draw(at: image, person: result)
          self.draw(at: image, person: result)
          
          if let poseClass = result.poseClass {
            self.onPoseDetected!(["pose": poseClass, "score": result.poseScore!])
          }
        }
      } catch {
        os_log("Error running pose estimation.", type: .error)
        return
      }
    }
  }


  /// Visualization configs
  private enum Config {
    static let dot = (radius: CGFloat(10), color: UIColor.yellow)
    static let line = (width: CGFloat(5.0), color: UIColor.green)
  }

  /// List of lines connecting each part to be visualized.
  private static let lines = [
    (from: BodyPart.leftWrist, to: BodyPart.leftElbow),
    (from: BodyPart.leftElbow, to: BodyPart.leftShoulder),
    (from: BodyPart.leftShoulder, to: BodyPart.rightShoulder),
    (from: BodyPart.rightShoulder, to: BodyPart.rightElbow),
    (from: BodyPart.rightElbow, to: BodyPart.rightWrist),
    (from: BodyPart.leftShoulder, to: BodyPart.leftHip),
    (from: BodyPart.leftHip, to: BodyPart.rightHip),
    (from: BodyPart.rightHip, to: BodyPart.rightShoulder),
    (from: BodyPart.leftHip, to: BodyPart.leftKnee),
    (from: BodyPart.leftKnee, to: BodyPart.leftAnkle),
    (from: BodyPart.rightHip, to: BodyPart.rightKnee),
    (from: BodyPart.rightKnee, to: BodyPart.rightAnkle),
  ]

  /// CGContext to draw the detection result.
  var context: CGContext!

  /// Draw the detected keypoints on top of the input image.
  ///
  /// - Parameters:
  ///     - image: The input image.
  ///     - person: Keypoints of the person detected (i.e. output of a pose estimation model)
  func draw(at image: UIImage, person: Person) {
    if context == nil {
      UIGraphicsBeginImageContext(image.size)
      guard let context = UIGraphicsGetCurrentContext() else {
        fatalError("set current context faild")
      }
      self.context = context
    }
    guard let strokes = strokes(from: person) else { return }
    image.draw(at: .zero)
    context.setLineWidth(Config.dot.radius)
    drawDots(at: context, dots: strokes.dots)
    drawLines(at: context, lines: strokes.lines)
    context.setStrokeColor(UIColor.green.cgColor)
    context.strokePath()
    guard let newImage = UIGraphicsGetImageFromCurrentImageContext() else { fatalError() }
    self.image = newImage
  }

  /// Draw the dots (i.e. keypoints).
  ///
  /// - Parameters:
  ///     - context: The context to be drawn on.
  ///     - dots: The list of dots to be drawn.
  private func drawDots(at context: CGContext, dots: [CGPoint]) {
    for dot in dots {
      let dotRect = CGRect(
        x: dot.x - Config.dot.radius / 2, y: dot.y - Config.dot.radius / 2,
        width: Config.dot.radius, height: Config.dot.radius)
      let path = CGPath(
        roundedRect: dotRect, cornerWidth: Config.dot.radius, cornerHeight: Config.dot.radius,
        transform: nil)
      context.addPath(path)
    }
  }

  /// Draw the lines (i.e. conneting the keypoints).
  ///
  /// - Parameters:
  ///     - context: The context to be drawn on.
  ///     - lines: The list of lines to be drawn.
  private func drawLines(at context: CGContext, lines: [Line]) {
    for line in lines {
      context.move(to: CGPoint(x: line.from.x, y: line.from.y))
      context.addLine(to: CGPoint(x: line.to.x, y: line.to.y))
    }
  }

  /// Generate a list of strokes to draw in order to visualize the pose estimation result.
  ///
  /// - Parameters:
  ///     - person: The detected person (i.e. output of a pose estimation model).
  private func strokes(from person: Person) -> Strokes? {
    var strokes = Strokes(dots: [], lines: [])
    // MARK: Visualization of detection result
    var bodyPartToDotMap: [BodyPart: CGPoint] = [:]
    for (index, part) in BodyPart.allCases.enumerated() {
      let position = CGPoint(
        x: person.keyPoints[index].coordinate.x,
        y: person.keyPoints[index].coordinate.y)
      bodyPartToDotMap[part] = position
      strokes.dots.append(position)
    }

    do {
      try strokes.lines = OverlayView.lines.map { map throws -> Line in
        guard let from = bodyPartToDotMap[map.from] else {
          throw VisualizationError.missingBodyPart(of: map.from)
        }
        guard let to = bodyPartToDotMap[map.to] else {
          throw VisualizationError.missingBodyPart(of: map.to)
        }
        return Line(from: from, to: to)
      }
    } catch VisualizationError.missingBodyPart(let missingPart) {
      os_log("Visualization error: %s is missing.", type: .error, missingPart.rawValue)
      return nil
    } catch {
      os_log("Visualization error: %s", type: .error, error.localizedDescription)
      return nil
    }
    return strokes
  }
}

/// The strokes to be drawn in order to visualize a pose estimation result.
fileprivate struct Strokes {
  var dots: [CGPoint]
  var lines: [Line]
}

/// A straight line.
fileprivate struct Line {
  let from: CGPoint
  let to: CGPoint
}

fileprivate enum VisualizationError: Error {
  case missingBodyPart(of: BodyPart)
}
