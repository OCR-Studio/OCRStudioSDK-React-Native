package ai.ocrstudio.sdk;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
        
@ReactModule(name = OcrstudiosdkModule.NAME)
@SuppressWarnings("unused")
public class OcrstudiosdkModule extends ReactContextBaseJavaModule {

    // declare an instance of the implementation
    private final OcrstudiosdkModuleImpl implementation;

    OcrstudiosdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        implementation = new OcrstudiosdkModuleImpl(reactContext);
    }

    public static final String NAME = OcrstudiosdkModuleImpl.NAME;

    @Override
    public String getName() {
        return OcrstudiosdkModuleImpl.NAME;
    }

    @ReactMethod
    public void init(String signature, Promise promise){
        implementation.init(signature, promise);
    }

    @ReactMethod
    public void initSettings(ReadableMap settings, Promise promise) {
        implementation.initSettings(settings, promise);
    }

    @ReactMethod
    public void startRecognition(final Promise promise) {
        implementation.startRecognition(promise);
    }

    @ReactMethod
    public void fileRecognition(String imageData, Promise promise) {
        implementation.fileRecognition(imageData, promise);
    }

    @ReactMethod
    public void getVersion(Promise promise) {
        implementation.getVersion(promise);
    }
    
    @ReactMethod
    public void engineDescription(Promise promise) {
        implementation.engineDescription(promise);
    }

    @ReactMethod
    public void faceComparison( String faceA, String faceB, Promise promise){
        implementation.faceComparison( faceA, faceB, promise);
    }

    @ReactMethod
    public void setLabelText(String txt, Promise promise) {
        implementation.setLabelText(txt, promise);
    }

    @ReactMethod
    public void setNativeTimeout(double ms, Promise promise) {
        implementation.setNativeTimeout(ms, promise);
    }

}
