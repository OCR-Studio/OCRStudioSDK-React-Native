
# New architecture

This module supports old and new architecture. If you need to enable the new architecture:

**Android**:
Find your `example/android/gradle.properties` file and add `newArchEnabled=true`

  ```diff
  - newArchEnabled=false
  + newArchEnabled=true
  ```
**iOS**:
You can choose either of the following options to enable the new architecture for your iOS project:

  - Option 1: Open `example/ios/Podfile` Add this line at the top: `ENV['RCT_NEW_ARCH_ENABLED'] = '1'`
  - Option 2: Run this command in your terminal: `RCT_NEW_ARCH_ENABLED=1 bundle exec pod install`

## 🚀 How to run the example

### 1. Fill data

 In `example/src/App.tsx` fill values:

```js
  const signature =  'SET SIGNATURE HERE/';
```

### 2. Install deps

**Install Module deps:**

Run the following commands from the _root_ of `module` :

```sh
# Using npm
npm install

# OR using Yarn
yarn
```

**Install Example deps.**

Run the following commands from the _root_ of `module/example` :

```sh
yarn
```

### 2. Start the Metro Server

_You will need to start **Metro**, the JavaScript bundler that ships with React Native._

#### install Module deps. 

Run the following commands from the _root_ of `module` :

```sh
yarn start
```

### 3. Build and run your app

With Metro running, open a new terminal from the root of `example` folder (React Native project).

### Android

```sh
yarn android
```

### iOS

For iOS, remember to install CocoaPods dependencies (this only needs to be run on first clone or after updating native deps).

The first time you create a new project, run the Ruby bundler to install CocoaPods itself:

```sh
bundle install
```

Then, and every time you update your native dependencies, run:

```sh
bundle exec pod install
```

For more information, please visit [CocoaPods Getting Started guide](https://guides.cocoapods.org/using/getting-started.html).

```sh
yarn ios
```


If everything is set up _correctly_, you should see your new app running in your _Android Emulator_ or _iOS Simulator_ shortly provided you have set up your emulator/simulator correctly.

This is one way to run your app — you can also run it directly from within Android Studio and Xcode respectively.

### Dependencies

The example uses plugins: 
- "react-native-image-picker" 
- "react-native-permissions"

The app's `Info.plist` must contain an `NSCameraUsageDescription` key with a string value explaining to the user how the app uses this data.

```xml
<key>NSCameraUsageDescription</key>
<string>Recognition</string>
```
---

### 🛠️ Editing sources

**For Android:** open `example/android` folder in your IDE.

**For iOS:** open `example/ios/*.xcworkspace` project
**For js:**

- Install dependencies `yarn`
- Open `/src/index.tsx` and run `yarn prepack` after editing.

---

### 🔩 Install the npm package to your project

Installing the package into an application using the `yarn link` or `npm link` commands is not recommended.
These commands create symlinks in node_modules and have limitations for the purpose of building the application.
(We have issues with gradle on macOS.("not annotated as @Input." or ".xml is not located in [../src/main/res]"))

1) You can copy the module directly to node_modules or use a private registry.

<https://callstack.github.io/react-native-builder-bob/faq#how-to-test-the-library-in-an-app-locally>

2) You can integrate the module into your application, as the "example" folder inside the module is integrated.

<https://callstack.github.io/react-native-builder-bob/faq#how-does-the-library-get-linked-to-the-example-app-in-the-generated-project>

---

### 🌈 How to update libraries to a new version

If you have received a new npm package, you can update the binaries this way:

#### For android

1. Replace all folders (`arm64-v8a`,`armeabi-v7a` etc) in `android/src/main/jniLibs/`
2. Replace interface `*.jar` in `android/src/main/libs/`
3. Replace `*.se` bundle `android/src/assets/data/`

#### For iOS

1. Replace 2 folders `ios/OCRStudioSDK`, `ios/OCRStudioSDKCore`
2. Run `bundle exec pod install` in `example/ios`

---

### 📄 Additional info

1. <https://reactnative.dev/docs/native-modules-setup>

2. <https://github.com/callstack/react-native-builder-bob>
