export declare const PdManager: import("react-native").HostComponent<unknown> | (() => never);
export declare type Props = {
    style: any;
    modelPath: string;
    onCameraError: (msg: string) => void;
    onModelError: (msg: string) => void;
    onPoseDetected: (pose: string, score: number) => void;
};
