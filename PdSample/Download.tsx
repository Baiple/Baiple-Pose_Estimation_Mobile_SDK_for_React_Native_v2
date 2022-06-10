import React from 'react';
import { Button, View, Text } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import RNFetchBlob from 'rn-fetch-blob';

export default function DownloadPage() {
  const [modelPath, setModelPath] = React.useState('');

  if (!modelPath) {
    AsyncStorage.getItem('modelPath', (err, result) => {
      if (err) {
        console.error(err);
      } else if (result) {
        setModelPath(result);
      }
    });
  }

  const startDownload = () => {
    console.log('will startDownload');
    RNFetchBlob
      .config({
        fileCache : true,
      })
      .fetch('GET', 'https://urizip.ogp.kr/test/movenet_thunder.tflite', {
        //some headers ..
      })
      .then((res) => {
        AsyncStorage.setItem('modelPath', res.path(), () => {
          console.log('The file saved to ', res.path());
          setModelPath(res.path());
        });
      });
  };

  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
      <Text>Model Path: { modelPath }</Text>
      <View style={{margin:30}}></View>
      <Button
        title="Start Download"
        onPress={startDownload}
      />
    </View>
  );
}
