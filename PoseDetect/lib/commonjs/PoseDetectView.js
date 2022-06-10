"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.PoseDetectView = void 0;

var _react = _interopRequireWildcard(require("react"));

var _reactNative = require("react-native");

var _PdManager = require("./PdManager");

function _getRequireWildcardCache(nodeInterop) { if (typeof WeakMap !== "function") return null; var cacheBabelInterop = new WeakMap(); var cacheNodeInterop = new WeakMap(); return (_getRequireWildcardCache = function (nodeInterop) { return nodeInterop ? cacheNodeInterop : cacheBabelInterop; })(nodeInterop); }

function _interopRequireWildcard(obj, nodeInterop) { if (!nodeInterop && obj && obj.__esModule) { return obj; } if (obj === null || typeof obj !== "object" && typeof obj !== "function") { return { default: obj }; } var cache = _getRequireWildcardCache(nodeInterop); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (key !== "default" && Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj.default = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

const postCreateFragment = viewId => _reactNative.UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
_reactNative.UIManager.PoseDetectView.Commands.create.toString(), [viewId]);

const postStartRecord = viewId => {
  if (_reactNative.Platform.OS === 'android') {
    _reactNative.UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    _reactNative.UIManager.PoseDetectView.Commands.startRecord.toString(), [viewId]);
  } else {
    _reactNative.UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    _reactNative.UIManager.PoseDetectView.Commands.startRecord, []);
  }
};

const postStopRecord = viewId => {
  if (_reactNative.Platform.OS === 'android') {
    _reactNative.UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    _reactNative.UIManager.PoseDetectView.Commands.stopRecord.toString(), [viewId]);
  } else {
    _reactNative.UIManager.dispatchViewManagerCommand(viewId, // @ts-ignore
    _reactNative.UIManager.PoseDetectView.Commands.stopRecord, []);
  }
};

const PoseDetectView = /*#__PURE__*/_react.default.forwardRef((props, ref) => {
  const refSelf = (0, _react.useRef)(null);

  _react.default.useImperativeHandle(ref, () => ({
    startRecord: () => {
      console.log('will call startRecord');
      postStartRecord((0, _reactNative.findNodeHandle)(refSelf.current));
    },
    stopRecord: () => {
      console.log('will call stopRecord');
      postStopRecord((0, _reactNative.findNodeHandle)(refSelf.current));
    }
  }));

  (0, _react.useEffect)(() => {
    const viewId = (0, _reactNative.findNodeHandle)(refSelf.current);

    if (_reactNative.Platform.OS === 'android') {
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

  return /*#__PURE__*/_react.default.createElement(_PdManager.PdManager // @ts-ignore
  , {
    style: props.style,
    modelPath: props.modelPath,
    onCameraError: _onCameraError,
    onModelError: _onModelError,
    onPoseDetected: _onPoseDetected,
    ref: refSelf
  });
});

exports.PoseDetectView = PoseDetectView;
//# sourceMappingURL=PoseDetectView.js.map