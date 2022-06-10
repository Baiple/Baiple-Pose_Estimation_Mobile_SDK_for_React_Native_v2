#import "React/RCTViewManager.h"

@interface RCT_EXTERN_MODULE(PoseDetectViewManager, RCTViewManager)

RCT_EXTERN_METHOD(startRecord: (nonnull NSNumber *)viewTag)
RCT_EXTERN_METHOD(stopRecord: (nonnull NSNumber *)viewTag)

//RCT_EXPORT_VIEW_PROPERTY(color, NSString)
RCT_EXPORT_VIEW_PROPERTY(modelPath, NSString)
RCT_EXPORT_VIEW_PROPERTY(onCameraError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onModelError, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPoseDetected, RCTDirectEventBlock)

@end
