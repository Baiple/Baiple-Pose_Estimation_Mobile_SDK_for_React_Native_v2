/*import {
  requireNativeComponent,
  UIManager,
  Platform,
  ViewStyle,
} from 'react-native';

const LINKING_ERROR =
  `The package 'pose-detect' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

type PoseDetectProps = {
  color: string;
  style: ViewStyle;
};

const ComponentName = 'PoseDetectView';

export const PoseDetectView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<PoseDetectProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
*/
import { PoseDetectView } from './PoseDetectView';
export { PoseDetectView };
//# sourceMappingURL=index.js.map