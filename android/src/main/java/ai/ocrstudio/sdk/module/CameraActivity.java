/*
  Copyright (c) 2024-2026, OCR Studio
  All rights reserved.
*/

package ai.ocrstudio.sdk.module;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import ai.ocrstudio.sdk.OCRStudioSDKResult;

import ai.ocrstudio.sdk.R;
import ai.ocrstudio.sdk.databinding.ActivityCameraBinding;
import ai.ocrstudio.sdk.module.lib.Engine;
import ai.ocrstudio.sdk.module.lib.Session;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main sample activity for documents recognition
 */
public class CameraActivity extends AppCompatActivity implements CameraCallback {
    private static final String TAG = "myapp.CameraActivity";

    // VIEW MODEL
    CameraViewModel model;

    private final int REQUEST_CAMERA_PERMISSION = 1;
    private boolean pauseAnalysis = true;
    private Button button;

    public static Draw draw;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView cameraView;
    ActivityCameraBinding binding;
    RelativeLayout drawing;

    Context mContext;

    ExecutorService SessionExecutor = Executors.newSingleThreadExecutor();

    private double startTime = -1;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a ViewModel the first time the system calls an activity's onCreate() method.
        // Re-created activities receive the same MyViewModel instance created by the first activity.
        model = new ViewModelProvider(this).get(CameraViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);

        // Bind to UI DRAW class
        draw = new Draw(this.getBaseContext());
        drawing = binding.drawing;
        drawing.addView(draw);

        mContext = this.getBaseContext();

        button = binding.start;
        button.setVisibility(View.INVISIBLE);
        button.setEnabled(false);

        cameraView = binding.cameraView;

        intent = new Intent();

        // Bind label object to xml
        binding.setLabel(Label.getInstance());

        // Subscribe for callback
        Session.anOCRStudioSDKDelegateState.observe(this, result -> {
            draw.showMatching(result);
        });

        // Engine events
        Engine.loadingState.observe(this, loadingState -> {
            if (loadingState == Engine.LoadingState.Loading) {
                Label.getInstance().message.set("Engine loading...");
            } else if (loadingState == Engine.LoadingState.Ready) {
                runOnUiThread(this::onInitialized);
            } else if (loadingState == Engine.LoadingState.Error) {
                Label.getInstance().message.set("Engine init error");
            }
        });

        button.setOnClickListener(v -> {
            if (pauseAnalysis) {
                onStarted();
            } else {
                onCanceled();
            }
        });

        if (permission(Manifest.permission.CAMERA))
            request(Manifest.permission.CAMERA, REQUEST_CAMERA_PERMISSION);

        /**
         * * For native Android projects you can call initCamera() without ViewTreeObserver().
         * * We use TreeObserver because of an issue with flutter! After the first call of this activity in flutter
         * * we get cameraView.getHeight() equal to 0 in all subsequent calls.
         * ** We must wait for the rendering of the cameraView to be completed.
         */

        binding.main.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.main.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                initCamera();
            }
        });
    }

    private void initCamera() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        // "cameraView.getDisplay().getRotation()" some times null object reference error
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();

        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraView.getSurfaceProvider());

        // Camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Set up the image analysis
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setResolutionSelector(
                        new ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                        new ResolutionStrategy(new Size(1200, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                                ).build())
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetRotation(rotation)
                .build();

        ViewPort viewPort = ((PreviewView)findViewById(R.id.cameraView)).getViewPort();

        // Use case
        assert viewPort != null;
        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .setViewPort(viewPort)
                .addUseCase(imageAnalysis)
                .build();

        imageAnalysis.setAnalyzer(model.executor, image -> {

            if (pauseAnalysis){
                image.close();
                return;
            }

            // Try recognition
            try {
                OCRStudioSDKResult result = model.onVideoFrame(image, cameraView.getHeight());

                // Draw Feedback on screen. It works on session feedback
                // draw.showMatching(result);

                // Events. Sending intermediate results during the recognition session:
                runOnUiThread(() -> {
                  // for flutter
                  // OCRStudioSDKPlugin.streamRecognized(result, (int) (Math.random() * 100));
                  // for react-native
                  // OCRStudioSDKModule.streamRecognized(result, (int) (Math.random() * 100));
                });

                if (result.AllTargetsFinal()) {
                    // The result is terminal when the engine decides that the recognition result
                    // has had enough information and ready to produce result, or when the session
                    // is timed out

                    // This will stop data from streaming
                    imageAnalysis.clearAnalyzer();
                    pauseAnalysis = true;
                    onRecognized();
                }
            } catch (Exception e) {
                onError(e.getMessage());
                finish();
            }

            // Free resources
            image.close();
        });

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, useCaseGroup);
        preview.setSurfaceProvider(cameraView.getSurfaceProvider());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    void toast(final String message) {

        runOnUiThread(() -> {
            Toast t = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            t.show();
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean permission(String permission) {
        int result = ContextCompat.checkSelfPermission(this, permission);
        return result != PackageManager.PERMISSION_GRANTED;
    }

    public void request(String permission, int request_code) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, request_code);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                boolean granted = false;
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) { // Permission is granted
                        granted = true;
                    }
                }
                if (granted) {
                    // view.updatePreview();
                } else {
                    onError("Please allow Camera permission");
                }
            }
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onStarted() {
        button.setText("CANCEL");
        //feedbackMessageTextView.setText("");

        // Init session
        SessionExecutor.execute(() -> {
            try {
                // faceSession.Reset();
                Label.getInstance().message.set("Wait...");
                Session.createVideoSession();
                Label.getInstance().message.set("Session Ready: " + Session.totalTime + "ms");
                pauseAnalysis = false;
            } catch (Exception e) {
                onError(e.getMessage());
                Log.e("Session: ", e.getMessage());
            }
        });
    }

    public void onInitialized() {
        // enable buttons
        button.setEnabled(true);
        button.setVisibility(View.VISIBLE);

        try {
            Label.getInstance().message.set("Engine Ready: " + Engine.totalTime + "ms");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
        }
    }

    public void onRecognized() {
        double elapsedTime = System.nanoTime() - startTime;
        double t = TimeUnit.MILLISECONDS.convert((long) elapsedTime, TimeUnit.NANOSECONDS);

        toast("Time:" + t);
        OCRStudioSDKResult result = Session.session.CurrentResult();
        ResultStore.instance.addResult(result);

        // Free resources
         Session.session.Reset();

        setResult(RESULT_OK, intent);
        finish();
    }

    public void onCanceled() {
      pauseAnalysis = true;
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void onError(String message) {
      Log.d(TAG, message);
        toast(message);
    }



}
