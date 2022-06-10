import {
  requireNativeComponent,
  UIManager,
  Platform,
} from 'react-native';

const LINKING_ERROR =
  `The package 'pose-detect' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const ComponentName = 'PoseDetectView';

export const PdManager = UIManager.getViewManagerConfig(ComponentName) != null
  ? requireNativeComponent(ComponentName) : () => { throw new Error(LINKING_ERROR); };

export type Props = {
  style: any;
  modelPath: string;

  onCameraError: (msg: string) => void;
  onModelError: (msg: string) => void;
  onPoseDetected: (pose: string, score: number) => void;
}
