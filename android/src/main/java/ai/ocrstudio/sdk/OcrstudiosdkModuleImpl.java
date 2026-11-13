package ai.ocrstudio.sdk;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.annotations.ReactModule;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import ai.ocrstudio.sdk.module.CameraActivity;
import ai.ocrstudio.sdk.module.Label;
import ai.ocrstudio.sdk.module.lib.Image;
import ai.ocrstudio.sdk.module.ResultStore;
import ai.ocrstudio.sdk.module.lib.Session;
import ai.ocrstudio.sdk.module.lib.Settings;
import ai.ocrstudio.sdk.module.lib.Engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// https://stackoverflow.com/a/51842524/2497351
@ReactModule(name = OcrstudiosdkModuleImpl.NAME)
public class OcrstudiosdkModuleImpl {
  public static final String NAME = "Ocrstudiosdk";

  private static final int OCR_REQUEST = 1;
  private static ReactApplicationContext reactContext;
  private Promise resultResponse;

  ExecutorService executor;

  public OcrstudiosdkModuleImpl(ReactApplicationContext reactContext) {

    super();
    OcrstudiosdkModuleImpl.reactContext = reactContext;
    // Add the listener for `onActivityResult`
    reactContext.addActivityEventListener(mActivityEventListener);
  }
  // https://reactnative.dev/docs/native-modules-android#getting-activity-result-from-startactivityforresult
  final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode == OCR_REQUEST) {
        if (resultCode == Activity.RESULT_OK) {
          // Log.i("JSON", result);
          resultResponse.resolve(prepareResult());
        }

        if (resultCode == Activity.RESULT_CANCELED) {
          resultResponse.resolve(null);
        }
      }
    }
  };

  @NonNull
  public String getName() {
    return NAME;
  }

  public void init(String signature, Promise promise) {
    executor = Executors.newSingleThreadExecutor();
    try {
      // signature
      Settings.SetSignature(signature);
      // Return result from store in json
      Settings.isJsonResult = true;

      if (Engine.getInstance() == null) {
        // Init engine
        Engine.loadSync(reactContext);
        // If something went wrong with initialization
        if (Engine.loadingState.getValue() == Engine.LoadingState.Error) {
          promise.reject("Engine.LoadingState", Engine.error);
        }
      }
      promise.resolve(null);
    } catch (Exception e) {
      Log.e("Init error", Objects.requireNonNull(e.getMessage()));
      promise.reject(e);
    }
    promise.resolve(null);
  }

  public void initSettings(ReadableMap settings, Promise promise) {
    try {

      // mode
      String mode = settings.getString("mode");
      Settings.SetMode(mode);

      // mask
      ReadableArray mask = settings.getArray("mask");
      ArrayList<String> arrayList = new ArrayList<>();

      for (int i = 0; i < Objects.requireNonNull(mask).size(); ++i) {
        arrayList.add(mask.getString(i));
      }

      Settings.SetMask(arrayList);

      // options
      ReadableMap options = settings.getMap("options");
      Map<String, String> newMap = new HashMap(options.toHashMap());
      Settings.SetOptions(newMap);

      promise.resolve(null);
    } catch (Exception e) {
      Log.e("Init settings error", Objects.requireNonNull(e.getMessage()));
      promise.reject(e);
    }
  }

  public void startRecognition(Promise promise) {
    try {
      Activity activity = reactContext.getCurrentActivity();
      // Store the promise to resolve/reject when data returns
      resultResponse = promise;

      if(activity != null) {
        activity.startActivityForResult(new Intent(activity, CameraActivity.class), OCR_REQUEST);
      } else {
        throw new Exception();
      }

    } catch (Exception e) {
      Log.e("Start recognition error", Objects.requireNonNull(e.getMessage()));
      resultResponse.reject(e);
    }
  }

  public void fileRecognition(String imageData, Promise promise) {

   //SessionExecutor.execute(() -> {
      try {
        // Create image
        OCRStudioSDKImage im = magicStringParser(imageData);
        // Create session
        Session.createImageSession();
        // Process
        Session.session.ProcessImage(im);
        // Store result
        OCRStudioSDKResult result = Session.session.CurrentResult();
        ResultStore.instance.addResult(result);
        // Free resources
        Session.session.Reset();
        // Send result
        promise.resolve(prepareResult());
      } catch (Exception e) {
        Log.e("File recognition error", Objects.requireNonNull(e.getMessage()));
        promise.reject(e);
      }
   // });
  }

  public void getVersion(Promise promise) {
    try {
      System.loadLibrary("jniocrstudiosdk");
      String version = OCRStudioSDKInstance.LibraryVersion();
      promise.resolve(version);
    } catch (Exception e) {
      Log.e("get version error", Objects.requireNonNull(e.getMessage()));
      promise.reject(e);
    }
  }

  public void engineDescription(Promise promise) {
    try {
      String desc = Engine.engineDescription();
      promise.resolve(desc);
    } catch (Exception e) {
      Log.e("Engine description error", Objects.requireNonNull(e.getMessage()));
      promise.reject(e);
    }
  }

  public void faceComparison( String faceA, String faceB, Promise promise) {
    executor.execute(() -> {
      try {
        // Create image
        OCRStudioSDKImage imageA = magicStringParser(faceA);
        OCRStudioSDKImage imageB = magicStringParser(faceB);
        // Create session
        String result = Session.getFaceComparison(imageA,imageB);
        promise.resolve(result);
      } catch (Exception e) {
        Log.e("Face comparison error", Objects.requireNonNull(e.getMessage()));
        promise.reject(e);
      }
    });
  }

  public void setLabelText(String txt, Promise promise) {
    Label.getInstance().message.set(txt);
    promise.resolve(null);
  }

  public void setNativeTimeout(double ms, Promise promise) {
    new android.os.Handler().postDelayed(() -> promise.resolve(null),(long) ms);
  }

/*
  public void myCustomMethod_GetCroppedPhoto(ReadableMap coords, Promise promise) {
    try {
      HashMap<String, Integer> typedCoords = new HashMap<>();

      typedCoords.put("resizeTo", coords.getInt("resizeTo"));
      typedCoords.put("x", coords.getInt("x"));
      typedCoords.put("y", coords.getInt("y"));
      typedCoords.put("width", coords.getInt("width"));
      typedCoords.put("height", coords.getInt("height"));

      SettingsStore.cropCoords = typedCoords;
      promise.resolve(null);
    } catch (Exception e) {
      promise.reject("Create Event Error", e);
    }
  }*/

  private String prepareResult() {
    // Get data from store
    return ResultStore.instance.getResult();
  }
  // https://stackoverflow.com/a/58395357/2497351

  private byte[] readBytes(Uri uri) throws IOException {
    InputStream inputStream = reactContext.getContentResolver().openInputStream(uri);
    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

    // this is storage overwritten on each iteration with bytes
    int bufferSize = 1024;
    byte[] buffer = new byte[bufferSize];

    int len = 0;
    while ((len = inputStream.read(buffer)) != -1) {
      byteBuffer.write(buffer, 0, len);
    }
    inputStream.close();
    return byteBuffer.toByteArray();
  }

  private static OCRStudioSDKImage magicStringParser(String img) throws Exception {
    String jpg = "data:image/jpg;base64,";
    String png = "data:image/png;base64,";

    if (img.startsWith(jpg)) {
      String cleanedImage = img.substring(jpg.length());
      return Image.createFromBase64(cleanedImage.trim());
    } else if (img.startsWith(png)) {
      String cleanedImage = img.substring(png.length());
      return Image.createFromBase64(cleanedImage.trim());
    } else if (img.startsWith("file:///")) {
      return Image.createFromUri(img, reactContext);
    } else {
      // not supported
      throw new Exception("Image format is not supported");
    }
  }

/*   private static void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Set up any upstream listeners or background tasks as necessary
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    // Remove upstream listeners, stop unnecessary background tasks
  }

  public static void streamRecognized(OCRStudioSDKResult result, int random) {

    WritableMap params = Arguments.createMap();
    params.putString("random", String.valueOf(random));

    sendEvent(reactContext, "onFrameUpdate", params);

  } */

}
