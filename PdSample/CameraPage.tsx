import * as React from 'react';
import { StyleSheet, View, TextInput, Button } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { PoseDetectView } from '@baiple/pose-detect';

let counter = 0;

export default function CameraPage() {
  const [modelPath, setModelPath] = React.useState('');
  const [logMsgs, setLogMsgs] = React.useState('');
  const pvRef = React.useRef();

  const appendLogMsg = (msg?: string) => {
    if (msg) {
      if (++counter > 100) {
        counter = 0;
        setLogMsgs(msg + '\n');
      } else {
        setLogMsgs(logMsgs + msg + '\n');
      }
    }
  };

  React.useEffect(() => {
    AsyncStorage.getItem('modelPath', (err, result) => {
      if (err) {
        appendLogMsg(`Could not get model path: ${err}`);
      } else if (result) {
        appendLogMsg(`ModelPath: ${result}`);
        setModelPath(result);
      } else {
        appendLogMsg('Model file is not downloaded.');
      }
    });
  }, []);

  const onCameraError = (msg?: string) => {
    console.log('App onCameraError:', msg);
    appendLogMsg(`Camera error: ${msg}`);
  };
  const onModelError = (msg?: string) => {
    console.log('App onModelError:', msg);
    appendLogMsg(`Model error: ${msg}`);
  };
  const onPoseDetected = (pose?: string, score?: number) => {
    console.log('App onPoseDetected:', pose, score);
    appendLogMsg(`Pose[${pose}]: ${score}`);
  };

  const onStartRecord = () => {
    // @ts-ignore
    pvRef.current?.startRecord();
  };
  const onStopRecord = () => {
    // @ts-ignore
    pvRef.current?.stopRecord();
  };

  return (
    <View style={styles.container}>
      <PoseDetectView
        ref={pvRef}
        style={styles.box}
        modelPath={modelPath}
        onCameraError={onCameraError}
        onModelError={onModelError}
        onPoseDetected={onPoseDetected}
      />
      <View style={styles.textAreaContainer}>
        <View style={styles.buttonContainer}>
          <Button title="Start Record" onPress={onStartRecord} />
          <Button title="Stop Record" onPress={onStopRecord} />
        </View>
        <TextInput
          style={styles.textArea}
          value={logMsgs}
          underlineColorAndroid="transparent"
          placeholder="Log Messages..."
          placeholderTextColor="grey"
          numberOfLines={10}
          multiline={true}
          editable={false}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  box: {
    position: 'absolute',
    left: 10,
    top: 10,
    right: 10,
    bottom: 260,
  },

  textAreaContainer: {
    borderColor: 'red',
    borderWidth: 1,
    paddingLeft: 5,
    position: 'absolute',
    bottom: 10,
    left: 10,
    right: 10,
  },
  buttonContainer: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  textArea: {
    height: 200,
    justifyContent: 'flex-start',
  },
});
