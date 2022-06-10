import React, { useEffect, useRef } from 'react';
import { UIManager, Platform, findNodeHandle } from 'react-native';

import { PdManager, Props } from './PdManager';

const postCreateFragment = (viewId: number | null) =>
  UIManager.dispatchViewManagerCommand(
    viewId,
    // @ts-ignore
    UIManager.PoseDetectView.Commands.create.toString(),
    [viewId],
  );

const postStartRecord = (viewId: number | null) => {
  if (Platform.OS === 'android') {
    UIManager.dispatchViewManagerCommand(
      viewId,
      // @ts-ignore
      UIManager.PoseDetectView.Commands.startRecord.toString(),
      [viewId],
    );
  } else {
    UIManager.dispatchViewManagerCommand(
      viewId,
      // @ts-ignore
      UIManager.PoseDetectView.Commands.startRecord,
      [],
    );
  }
};

const postStopRecord = (viewId: number | null) => {
  if (Platform.OS === 'android') {
    UIManager.dispatchViewManagerCommand(
      viewId,
      // @ts-ignore
      UIManager.PoseDetectView.Commands.stopRecord.toString(),
      [viewId],
    );
  } else {
    UIManager.dispatchViewManagerCommand(
      viewId,
      // @ts-ignore
      UIManager.PoseDetectView.Commands.stopRecord,
      [],
    );
  }
};

export const PoseDetectView = React.forwardRef((props: Props, ref) => {
  const refSelf = useRef(null);

  React.useImperativeHandle(ref, () => ({
    startRecord: () => {
      console.log('will call startRecord');
      postStartRecord(findNodeHandle(refSelf.current));
    },
    stopRecord: () => {
      console.log('will call stopRecord');
      postStopRecord(findNodeHandle(refSelf.current));
    },
  }));

  useEffect(() => {
    const viewId = findNodeHandle(refSelf.current);
    if (Platform.OS === 'android') {
      postCreateFragment(viewId);
    }
  }, []);

  const _onCameraError = (e: any) => {
    console.log('JS onCameraError:', e.nativeEvent?.msg);
    props.onCameraError(e.nativeEvent?.msg);
  };

  const _onModelError = (e: any) => {
    console.log('JS onModelError:', e.nativeEvent?.msg);
    props.onModelError(e.nativeEvent?.msg);
  };

  const _onPoseDetected = (e: any) => {
    const { pose, score } = e.nativeEvent ?? {};
    //console.log('JS onPoseDetected:', pose, score);
    props.onPoseDetected(pose, score);
  };

  return (
    <PdManager
      // @ts-ignore
      style={props.style}
      modelPath={props.modelPath}
      onCameraError={_onCameraError}
      onModelError={_onModelError}
      onPoseDetected={_onPoseDetected}
      ref={refSelf}
    />
  );
});
