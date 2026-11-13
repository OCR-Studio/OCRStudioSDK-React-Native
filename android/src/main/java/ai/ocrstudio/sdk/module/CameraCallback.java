/*
  Copyright (c) 2016-2026, Smart Engines Service LLC
  All rights reserved.
*/

package ai.ocrstudio.sdk.module;

public interface CameraCallback {
  void onInitialized();
  void onStarted();
  void onCanceled();
  void onRecognized();
  void onError(String message);
}
