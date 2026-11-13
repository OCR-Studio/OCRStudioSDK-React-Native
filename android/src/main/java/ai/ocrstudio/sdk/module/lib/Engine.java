/*
  Copyright (c) 2024-2026, OCR Studio
  All rights reserved.
*/

package ai.ocrstudio.sdk.module.lib;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import ai.ocrstudio.sdk.OCRStudioSDKImage;
import ai.ocrstudio.sdk.OCRStudioSDKInstance;
import ai.ocrstudio.sdk.OCRStudioSDKItemIterator;
import ai.ocrstudio.sdk.OCRStudioSDKResult;
import ai.ocrstudio.sdk.OCRStudioSDKSession;
import ai.ocrstudio.sdk.OCRStudioSDKTarget;
import ai.ocrstudio.sdk.module.ResultStore;
import ai.ocrstudio.sdk.module.lib.Settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/**
 * THE ENGINE WRAPPER SINGLETON
 * Implements engine loading
 * exposes the engine loading state as a LiveData
 */
public class Engine {
    private static final String TAG = "myapp.Engine";
    private static OCRStudioSDKInstance instance = null;
    private static long startTime;
    public static long totalTime;

    public enum LoadingState {Empty, Loading, Ready, Error}

    public static MutableLiveData<LoadingState> loadingState = new MutableLiveData<>(LoadingState.Empty);
    public static String error = null;

    // Get instance already initiated
    public static OCRStudioSDKInstance getInstance() {return instance;}

    private static final Character separator = ':';

    // Default data for UI
    @NonNull
    private static String documents = "";
    @NonNull
    private static final List<String> documents2 = new ArrayList<>();
    private static String sessionTypes;

    /**
     * Load instance without blocking UI. Typically used for native platform
     */
    public static void loadAsync(Context context) {
        if (loadingState.getValue() == LoadingState.Empty) {
            new Thread(() -> initEngine(context)).start();
        }
    }

    /**
     * Load instance synchronously.
     * Typically used for Flutter/ React Native where synchronous call does not block the UI
     */
    public static void loadSync(Context context) {
        if (loadingState.getValue() == LoadingState.Empty) {
            initEngine(context);
        }
    }


    /**
     * Load required data from local file system
     * and create the OCRStudioSDKInstance object as result.
     * @param context - the application context for file reading operations
     */
    private static void initEngine(Context context) {
        try {
            // Benchmark
            startTime = System.nanoTime();

            loadingState.postValue(LoadingState.Loading);
            // Load library
            System.loadLibrary("jniocrstudiosdk");

            // Load config file
            byte[] config_data = loadConfigFile(context);

            JSONObject p = new JSONObject();
            p.put("enable_lazy_initialization", true);
            p.put("enable_delayed_initialization", false);

            // Create Engine instance
            instance = OCRStudioSDKInstance.CreateFromBuffer(config_data, p.toString());
            Log.i(TAG, "Engine instance created successfully");

            loadingState.postValue(LoadingState.Ready);

			long elapsedTime = System.nanoTime() - startTime;
		    totalTime = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
		    Log.i("Init engine: ", "Engine load time: " + totalTime + " ms");

        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Native library not found", e);
            error = e.getMessage();
            loadingState.postValue(LoadingState.Error);
        } catch (Exception e) {
            Log.e(TAG, "Load engine", e);
            error = e.getMessage();
            loadingState.postValue(LoadingState.Error);
        }

    }


    /**
     * Load the content of your config file (it should be in assets/config folder)
     * @param context - the application context
     * @return the file content as array of bytes
     * @throws IOException
     */
    private static byte[] loadConfigFile(Context context) throws Exception {
        AssetManager assetManager = context.getAssets();
        String configFilename = "";
        String[] file_list;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        file_list = assetManager.list("config");

        for (String file : file_list) {
            if (file.endsWith(".ocr")) {
                configFilename = file;
                break;
            }
        }

        if (configFilename.isEmpty()) {
            throw new Exception("configuration file is not found");
        }

        final String configPath = "config" + File.separator + configFilename;

        InputStream is = assetManager.open(configPath);

        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        os.close();
        return os.toByteArray();
    }

    public static String engineDescription() {
        if (documents.isEmpty()) {
            try {
                documents = instance.Description();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            JSONObject engineJson;
            try {
                engineJson = new JSONObject(instance.Description());

                JSONArray targetGroupsArray = engineJson.getJSONArray("target_groups");
                for (int tg_index = 0; tg_index < targetGroupsArray.length(); tg_index++) {
                    JSONObject targetGroup = targetGroupsArray.getJSONObject(tg_index);
                    String mode = targetGroup.getString("target_group_type");
                    JSONArray masksArray = targetGroup.getJSONArray("target_masks");
                    for (int mask_index = 0; mask_index < masksArray.length(); mask_index++) {
                        String mask = masksArray.getString(mask_index);
                        documents2.add(mode + separator + mask);
                    }

                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return documents;
    }

    public static String getSessionTypes() {
        if (sessionTypes.isEmpty()) {
//            OCRStudioSDKInstance engine = instance;
//            JSONObject engineJson;
//            try {
//                engineJson = new JSONObject(engine.Description());
//                JSONArray sessionTypesArray = engineJson.getJSONArray("session_types");
//                for (int st_index = 0; st_index < sessionTypesArray.length(); st_index++) {
//                    String sessionType = sessionTypesArray.getString(st_index);
//                    sessionTypes.add(sessionType);
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
        }
        //return sessionTypes;
        return "";
    }
}

