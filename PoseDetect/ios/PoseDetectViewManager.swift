import Foundation
import UIKit


@objc(PoseDetectViewManager)
class PoseDetectViewManager: RCTViewManager {

  @objc override static func requiresMainQueueSetup() -> Bool {
    return false
  }
  
  @objc func startRecord(_ viewTag: NSNumber) {
    DispatchQueue.main.async {
      let myView = self.bridge.uiManager.view(forReactTag: viewTag) as! OverlayView
      myView.startRecord()
    }
  }
  
  @objc func stopRecord(_ viewTag: NSNumber) {
    DispatchQueue.main.async {
      let myView = self.bridge.uiManager.view(forReactTag: viewTag) as! OverlayView
      myView.stopRecord()
    }
  }
  
  override func view() -> (UIView) {
    //return PoseDetectView()
      if #available(iOS 13.0, *) {
          return OverlayView(CGRect(x: 0, y: 0, width: 100, height: 100))
      } else {
          // Fallback on earlier versions
          return UIView()
      }
  }
}

/*class PoseDetectView : UIView {

  @objc var color: String = "" {
    didSet {
      self.backgroundColor = hexStringToUIColor(hexColor: color)
    }
  }

  func hexStringToUIColor(hexColor: String) -> UIColor {
    let stringScanner = Scanner(string: hexColor)

    if(hexColor.hasPrefix("#")) {
      stringScanner.scanLocation = 1
    }
    var color: UInt32 = 0
    stringScanner.scanHexInt32(&color)

    let r = CGFloat(Int(color >> 16) & 0x000000FF)
    let g = CGFloat(Int(color >> 8) & 0x000000FF)
    let b = CGFloat(Int(color) & 0x000000FF)

    return UIColor(red: r / 255.0, green: g / 255.0, blue: b / 255.0, alpha: 1)
  }
}*/
