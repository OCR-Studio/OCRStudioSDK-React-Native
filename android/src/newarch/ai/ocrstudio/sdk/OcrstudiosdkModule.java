package ai.ocrstudio.sdk;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;

// https://stackoverflow.com/a/51842524/2497351
@ReactModule(name = OcrstudiosdkModule.NAME)
public class OcrstudiosdkModule extends NativeOcrstudiosdkSpec {

    // declare an instance of the implementation
    private OcrstudiosdkModuleImpl implementation;

    OcrstudiosdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        implementation = new OcrstudiosdkModuleImpl(reactContext);
    }

    public static final String NAME = OcrstudiosdkModuleImpl.NAME;

    @NonNull
    @Override
    public String getName() {
        return OcrstudiosdkModuleImpl.NAME;
    }

    @Override
    public void init(String signature, Promise promise) {
        implementation.init(signature, promise);
    }

    @Override
    public void initSettings(ReadableMap settings, Promise promise) {
        implementation.initSettings(settings, promise);
    }

    @Override
    public void startRecognition(final Promise promise) {
        implementation.startRecognition(promise);
    }

    @Override
    public void fileRecognition(String uri, Promise promise) {
        implementation.fileRecognition(uri, promise);
    }

    @Override
    public void engineDescription(Promise promise) {
        implementation.engineDescription(promise);
    }

    @Override
    public void getVersion(Promise promise) {
        implementation.getVersion(promise);
    }

    @Override
    public void faceComparison(String faceA, String faceB, Promise promise) {
        implementation.faceComparison( faceA, faceB, promise);
    }

    @Override
    public void setLabelText(String txt, Promise promise) {
        implementation.setLabelText(txt, promise);
    }

    @Override
    public void setNativeTimeout(double ms, Promise promise) {
        implementation.setNativeTimeout(ms, promise);
    }

}
