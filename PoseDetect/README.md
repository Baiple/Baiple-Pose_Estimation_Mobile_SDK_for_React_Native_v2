# @baiple/pose-detect

Pose Detection Library

## Preparation

### Launch NPM Private Repository (Verdaccio)

```sh
$ docker run -d --name verdaccio -p 8873:4873 verdaccio/verdaccio
```

### Edit Verdaccio config (Max Body Size)
```sh
$ docker exec -it -u root verdaccio sh

$ vi /verdaccio/conf/config.yaml
```
Append the following line to the bottom of config.yaml
```
max_body_size: 500mb
```
Exit from docker shell then restart the verdaccio.
```sh
$ docker restart verdaccio
```

### Publish
Update .npmrc and Makefile for the Registry hostname then:
```sh
$ make login
$ make publish
```

## Installation (For Sample)
Before install, copy .npmrc to the project root.
```sh
npm install @baiple/pose-detect
```

## Usage

```js
import { PoseDetectView } from "@baiple/pose-detect";

// ...

<PoseDetectView
  modelPath={?}
  onCameraError={?}
  onModelError={?}
  onPoseDetected={?}
/>
```
