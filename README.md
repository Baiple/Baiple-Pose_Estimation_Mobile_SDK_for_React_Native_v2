## React-Native 공통

React-Native 프로젝트의 디렉토리 구조는 베이스 디렉토리에 package.json 및 내용이 되는 js 파일들이 위치하고 ios, android 디렉토리에 네이티브 관련 내용이 있습니다.

React-Native 프로젝트의 빌드 방법은 공통적으로 다음과 같습니다:

```sh
$ npm install
$ npm run android (안드로이드 실행, 폰이 연결되어 있다면 폰에서 뜸)

$ npm start (만약 내용이 안나오고 흰색 화면만 보일 경우 실행 후 r 누름)

$ npx pod-install (iOS전용)
$ npm run ios (iOS 실행이지만 시뮬레이터가 뜨므로 이건 거의 안씀)

$ cd ios
$ open ???.xcworkspace (XCode 실행됨, 타겟 디바이스를 아이폰으로 맞춰놓고 실행하면 됨)
```


## PdSample

위 방식대로 빌드/실행이 되어야 정상

새 프로젝트 생성 후 혹은 기존 프로젝트에 PoseDetect 모듈 추가시, 일반 node 프로젝트처럼 하면 됩니다.

```sh
$ npm install @baiple/pose-detect
```

iOS의 경우 Info.plist 파일에 카메라 관련 내용을 추가해줘야 함 (안하면 에러없이 다운됨)
```xml
<key>NSCameraUsageDescription</key>
<string>For taking photos</string>
```

위 작업은 기본적으로 되어 있으므로 PdSample 은 그냥 빌드/실행만 하면 됩니다.

앱이 실행되면 최초 1회 Model Download 후에 카메라 화면으로 가야 정상 동작합니다.

## PoseDetect

라이브러리 개발에 사용되는 디렉토리 입니다.

라이브러리 수정 후 package.json 에서 버전을 올리고

```sh
$ make login
$ make publish
```
하시면 라이브러리가 배포됩니다.

Makefile 및 .npmrc 파일에 보이는 urizip 호스트명은 테스트용이므로 PoseDetect/README.md 참고하시어 전용의 NPM Repository를 만드시거나 npmjs.org 를 사용하셔도 됩니다.