import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  init(signature: string): Promise<string>;
  initSettings(settings: Object): Promise<string>;
  startRecognition(): Promise<Object>;
  //startRecognitionVAuth(): Promise<Object>;
  //startLiveness(): Promise<Object>;
  fileRecognition(uri: string): Promise<Object>;
  getVersion(): Promise<string>;
  engineDescription(): Promise<string>;
  faceComparison(faceA: string, faceB: string): Promise<Number>;
  //myCustomMethodGetCroppedPhoto(coords: Object): Promise<string>;
  setLabelText(text: string): Promise<string>;
  setNativeTimeout(ms: number): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Ocrstudiosdk');
