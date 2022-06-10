// Copyright 2021 The TensorFlow Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================

import AVFoundation
import Accelerate.vImage
import UIKit
//import AVAssetWriter

/// Delegate to receive the frames captured from the device's camera.
protocol CameraFeedManagerDelegate: AnyObject {
  func cameraInitFailed(_ reason: String)
  
  /// Callback method that receives frames from the camera.
  /// - Parameters:
  ///     - cameraFeedManager: The CameraFeedManager instance which calls the delegate.
  ///     - pixelBuffer: The frame received from the camera.
  func cameraFeedManager(
    _ cameraFeedManager: CameraFeedManager, didOutput pixelBuffer: CVPixelBuffer)
}

/// Manage the camera pipeline.
final class CameraFeedManager: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {

  /// Delegate to receive the frames captured by the device's camera.
  private var delegate: CameraFeedManagerDelegate?
  
  // 영상/사진 저장을 위한 설정
  let captureSession = AVCaptureSession()
  var videoDataOutput: AVCaptureVideoDataOutput?
  var assetWriter: AVAssetWriter?
  var assetWriterInput: AVAssetWriterInput?
  var filePath: URL?
  var sessionAtSourceTime: CMTime?
  var adpater: AVAssetWriterInputPixelBufferAdaptor?
  
  var recording = false {
      didSet {
          recording ? self.start() : self.stop()
      }
  }

  init(_ deleg: CameraFeedManagerDelegate) {
    super.init()
    self.delegate = deleg
    self.recording = false
//    configureSession()
    requestCameraPermission()
  }
  
  // 카메라 권한 접근
  func requestCameraPermission() {
      print("### CFM.requestCameraPermission")
      switch AVCaptureDevice.authorizationStatus(for: .video) {
      case .authorized:
          self.setupCaptureSession()
          
      case .notDetermined:
          AVCaptureDevice.requestAccess(for: .video) { granted in
              if granted {
                  DispatchQueue.main.async {
                      self.setupCaptureSession()
                  }
              }
          }
          
      case .denied:
          return
          
      case .restricted:
          return
      @unknown default:
          fatalError()
      }
  }
  /// Initialize the capture session.
  func setupCaptureSession() {
      print("### CFM.setupCaptureSession")
      guard let backCamera = AVCaptureDevice.default(
           .builtInWideAngleCamera, for: .video, position: .back)
           else {
             print("configureSession backCamera failed, \(delegate != nil)")
             delegate?.cameraInitFailed("BackCamera init failed")
             return
       }
      do {
          self.captureSession.beginConfiguration()
          let input = try AVCaptureDeviceInput(device: backCamera)
          captureSession.addInput(input)
          
          let tempVideoDataOutput = AVCaptureVideoDataOutput()
          tempVideoDataOutput.videoSettings = [
                   (kCVPixelBufferPixelFormatTypeKey as String): NSNumber(value: kCVPixelFormatType_32BGRA)
                   ]
          tempVideoDataOutput.alwaysDiscardsLateVideoFrames = true
          let dataOutputQueue = DispatchQueue(
                   label: "videoQueue",
                   qos: .userInitiated,
                   attributes: [],
                   autoreleaseFrequency: .workItem
          )
          tempVideoDataOutput.setSampleBufferDelegate(self, queue: dataOutputQueue)
          self.captureSession.addOutput(tempVideoDataOutput)
          self.captureSession.commitConfiguration()
          self.captureSession.startRunning()
          
          self.videoDataOutput = tempVideoDataOutput
          
      } catch {
        print("configureSession input failed \(delegate != nil)")
        delegate?.cameraInitFailed("Camera input not available")
        return
      }
  }

  // This mothod will overwrite previous video files
  func videoFileLocation() -> URL {
    let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
      let videoOutputUrl = URL(fileURLWithPath: documentsPath.appendingPathComponent("videoFile")).appendingPathExtension("mov")
      do {
        if FileManager.default.fileExists(atPath: videoOutputUrl.path) {
            try FileManager.default.removeItem(at: videoOutputUrl)
            print("file removed")
        }
      } catch {
          print(error)
      }
    return videoOutputUrl
  }
  //AVAssetWriter 설정
  func setUpWriter() {
//      print("### CFM.setUpWriter() START")
      do {
          filePath = videoFileLocation()
          assetWriter = try AVAssetWriter(outputURL: filePath!, fileType: AVFileType.mov)

          // add video input
          let settings = self.videoDataOutput?.recommendedVideoSettingsForAssetWriter(writingTo: .mov)
        
          assetWriterInput = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
        
//          영상 녹화 파일 확인 후 해당 항목을 조치 필요함.
//          assetWriterInput = AVAssetWriterInput(mediaType: .video, outputSettings: [
//          AVVideoCodecKey : AVVideoCodecType.h264,
//          AVVideoWidthKey : 720,
//          AVVideoHeightKey : 1280,
//          AVVideoCompressionPropertiesKey : [
//              AVVideoAverageBitRateKey : 2300000,
//              ],
//          ])
          guard let assetWriterInput = assetWriterInput, let assetWriter = assetWriter else { return }
          assetWriterInput.expectsMediaDataInRealTime = true
          
          if assetWriter.canAdd(assetWriterInput) {
              assetWriter.add(assetWriterInput)
              print("asset input added")
          } else {
              print("no input added")
          }

          assetWriter.startWriting()
          
          self.assetWriter = assetWriter
          self.assetWriterInput = assetWriterInput
         
//          print("### CFM.setUpWriter() END")
      } catch let error {
          debugPrint(error.localizedDescription)
      }
  }
  
  func canWrite() -> Bool {
      return recording && assetWriter != nil && assetWriter?.status == .writing
  }
  
  func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
      
      guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else {
            return
          }
      CVPixelBufferLockBaseAddress(pixelBuffer, CVPixelBufferLockFlags.readOnly)
      delegate?.cameraFeedManager(self, didOutput: pixelBuffer)
      CVPixelBufferUnlockBaseAddress(pixelBuffer, CVPixelBufferLockFlags.readOnly)
      
      connection.videoOrientation = .portrait
      guard self.recording else { return }
      
      let writable = self.canWrite()

      if writable, self.sessionAtSourceTime == nil {
          // start writing
          sessionAtSourceTime = CMSampleBufferGetOutputPresentationTimeStamp(sampleBuffer)
          self.assetWriter?.startSession(atSourceTime: sessionAtSourceTime!)
      }
      guard let assetWriterInput = self.assetWriterInput else { return }
     
      if writable, assetWriterInput.isReadyForMoreMediaData {
          // write video buffer
          assetWriterInput.append(sampleBuffer)
      }
  }

  func start() {
      self.sessionAtSourceTime = nil
      self.setUpWriter()
      switch self.assetWriter?.status {
      case .writing:
          print("status writing")
      case .failed:
          print("status failed")
      case .cancelled:
          print("status cancelled")
      case .unknown:
          print("status unknown")
      default:
          print("status completed")
      }
  }

  func stop() {
      self.assetWriterInput?.markAsFinished()
      // 자동 촬영시 죽는 형상을 수정함.
      if self.assetWriter?.status == .writing {
        self.assetWriter?.finishWriting { [weak self] in
            self?.sessionAtSourceTime = nil
        }
      }
      
      let fileSize = self.fileSize(forURL:self.filePath)
      print("finished writing for filePath=\(self.filePath)")
      print("finished writing for fileSize=\(fileSize)")
  }
  
  // Start capturing frames from the camera.
    func startRunning() {
      print("CFM.startRunning")
      captureSession.startRunning()
    }

    /// Stop capturing frames from the camera.
    func stopRunning() {
      print("CFM.stopRunning")
      captureSession.stopRunning()
    }
    // 영상 녹화 시작
    func startRecording() {
      print("CFM.stopRunning")
      self.recording = true
    }
    // 영상 녹화 끝
    func stopRecording() {
      print("CFM.stopRecording")
      self.recording = false
    }
    
    func fileSize(forURL url: Any) -> Double {
        print("CFM.fileSize")
        print("## url \(url)")
        var fileURL: URL?
        var fileSize: Double = 0.0
        if (url is URL) || (url is String)
        {
            if (url is URL) {
                fileURL = url as? URL
            }
            else {
                fileURL = URL(fileURLWithPath: url as! String)
            }
            var fileSizeValue = 0.0
            try? fileSizeValue = (fileURL?.resourceValues(forKeys: [URLResourceKey.fileSizeKey]).allValues.first?.value as! Double?)!
            if fileSizeValue > 0.0 {
                fileSize = (Double(fileSizeValue) / (1024 * 1024))
            }
        }
        return fileSize
    }
  
}



