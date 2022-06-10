import React, { useEffect, useRef } from 'react';
import { UIManager, Platform, findNodeHandle } from 'react-native';
import { PdManager } from './PdManager';

const postCreateFragment = viewId => UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
UIManager.PoseDetectView.Commands.create.toString(), [viewId]);

const postStartRecord = viewId => {
  if (Platform.OS === 'android') {
    UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    UIManager.PoseDetectView.Commands.startRecord.toString(), [viewId]);
  } else {
    UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    UIManager.PoseDetectView.Commands.startRecord, []);
  }
};

const postStopRecord = viewId => {
  if (Platform.OS === 'android') {
    UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    UIManager.PoseDetectView.Commands.stopRecord.toString(), [viewId]);
  } else {
    UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    UIManager.PoseDetectView.Commands.stopRecord, []);
  }
};

export const PoseDetectView = /*#__PURE__*/React.forwardRef((props, ref) => {
  const refSelf = useRef(null);
  React.useImperativeHandle(ref, () => ({
    startRecord: () => {
      console.log('will call startRecord');
      postStartRecord(findNodeHandle(refSelf.current));
    },
    stopRecord: () => {
      console.log('will call stopRecord');
      postStopRecord(findNodeHandle(refSelf.current));
    }
  }));
  useEffect(() => {
    const viewId = findNodeHandle(refSelf.current);

    if (Platform.OS === 'android') {
      postCreateFragment(viewId);
    }
  }, []);

  const _onCameraError = e => {
    var _e$nativeEvent, _e$nativeEvent2;

    console.log('JS onCameraError:', (_e$nativeEvent = e.nativeEvent) === null || _e$nativeEvent === void 0 ? void 0 : _e$nativeEvent.msg);
    props.onCameraError((_e$nativeEvent2 = e.nativeEvent) === null || _e$nativeEvent2 === void 0 ? void 0 : _e$nativeEvent2.msg);
  };

  const _onModelError = e => {
    var _e$nativeEvent3, _e$nativeEvent4;

    console.log('JS onModelError:', (_e$nativeEvent3 = e.nativeEvent) === null || _e$nativeEvent3 === void 0 ? void 0 : _e$nativeEvent3.msg);
    props.onModelError((_e$nativeEvent4 = e.nativeEvent) === null || _e$nativeEvent4 === void 0 ? void 0 : _e$nativeEvent4.msg);
  };

  const _onPoseDetected = e => {
    var _e$nativeEvent5;

    const {
      pose,
      score
    } = (_e$nativeEvent5 = e.nativeEvent) !== null && _e$nativeEvent5 !== void 0 ? _e$nativeEvent5 : {}; //console.log('JS onPoseDetected:', pose, score);

    props.onPoseDetected(pose, score);
  };

  return /*#__PURE__*/React.createElement(PdManager // @ts-ignore
  , {
    style: props.style,
    modelPath: props.modelPath,
    onCameraError: _onCameraError,
    onModelError: _onModelError,
    onPoseDetected: _onPoseDetected,
    ref: refSelf
  });
});
//# sourceMappingURL=PoseDetectView.js.map