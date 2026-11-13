// TurboMudules
import { NativeModules, Platform } from 'react-native';
import Ocrstudiosdk from './NativeOcrstudiosdk';

export const OCRStudioSDKModule = {

  init(signature: string): Promise<string> {
    return Ocrstudiosdk.init(signature);
  },
  initSettings(settings: Object): Promise<string> {
    return Ocrstudiosdk.initSettings(settings);
  },
  startRecognition(): Promise<Object> {
    return Ocrstudiosdk.startRecognition();
  },
  // startRecognitionVAuth(): Promise<Object> {
  //   return Ocrstudiosdk.startRecognitionVAuth();
  // },
  // startLiveness(): Promise<Object> {
  //   return Ocrstudiosdk.startLiveness();
  // },
  fileRecognition(uri: string): Promise<Object> {
    return Ocrstudiosdk.fileRecognition(uri);
  },
  getVersion(): Promise<string> {
    return Ocrstudiosdk.getVersion();
  },
  engineDescription(): Promise<string> {
    return Ocrstudiosdk.engineDescription();
  },
  faceComparison(facea: string, faceb: string): Promise<Number> {
    return Ocrstudiosdk.faceComparison(facea, faceb);
  },
  // myCustomMethod_GetCroppedPhoto(coords: Object): Promise<string> {
  //   return Ocrstudiosdk.myCustomMethodGetCroppedPhoto(coords);
  // },
  setLabelText(text: string): Promise<string> {
    return Ocrstudiosdk.setLabelText(text);
  },
  setNativeTimeout(ms: number): Promise<string> {
    return Ocrstudiosdk.setNativeTimeout(ms);
  },
};
