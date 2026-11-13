/*
  Copyright (c) 2024-2026, OCR Studio
  All rights reserved.
*/

package ai.ocrstudio.sdk.module.lib;

import android.util.Log;


import androidx.lifecycle.MutableLiveData;

import ai.ocrstudio.sdk.OCRStudioSDKDelegate;
import ai.ocrstudio.sdk.OCRStudioSDKImage;
import ai.ocrstudio.sdk.OCRStudioSDKInstance;
import ai.ocrstudio.sdk.OCRStudioSDKItem;
import ai.ocrstudio.sdk.OCRStudioSDKItemIterator;
import ai.ocrstudio.sdk.OCRStudioSDKResult;
import ai.ocrstudio.sdk.OCRStudioSDKSession;
import ai.ocrstudio.sdk.OCRStudioSDKTarget;
import ai.ocrstudio.sdk.module.CameraActivity;
import ai.ocrstudio.sdk.module.Label;
import ai.ocrstudio.sdk.module.lib.Settings;
import ai.ocrstudio.sdk.module.nfc.SessionNfcData;
import ai.ocrstudio.sdk.module.nfc.SessionNfcResult;
import ai.ocrstudio.sdk.module.nfc.SessionNfcResultError;
import ai.ocrstudio.sdk.module.nfc.SessionNfcResultSuccess;



import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import java.net.HttpURLConnection;


/**
 * SESSION helper class
 */
public class Session {
    static String TAG = "Session";
    private static long startTime;
    public static long totalTime;

    // The session object
    public static OCRStudioSDKSession session;


    //----------------------------------------------------------------------------------------------
    // PUBLIC DATA
    public static boolean isRoi;
    public String error = null;

    // Vauth Case
    public static boolean sessionAskForSelfie = false;

    // ==================================================================
    //                  For subscriptions in other classes
    // ==================================================================

    public static MutableLiveData<JSONObject> anOCRStudioSDKDelegateState = new MutableLiveData<>(null);

    // ==================================================================
    //                          SESSION CALLBACKS
    // ==================================================================

    /**
     * SESSION CALLBACK
     */
    static OCRStudioSDKDelegate anOCRStudioSDKDelegate = new OCRStudioSDKDelegate() {
        public void Callback(String json_message) {
            try {
                JSONObject json = new JSONObject(json_message);
                anOCRStudioSDKDelegateState.postValue(json);
                Log.w(TAG, "   --> Callback: " + json);
            } catch (Exception e) {
                Log.e(TAG,"Session Callback json parsing error",e);
            }
        }
    };


    // ==================================================================
    //                          SESSION CREATION
    // ==================================================================

    /**
     * Example of creating video session.
     * 1. Get Engine instance
     * 2. Create session
     * 3. Start processing frames to Session
     * 4. Waiting for terminal
     * 5. Reset session
     */

    public static void createVideoSession() {
        try {
            Label.getInstance().message.set("Wait...");

            // Benchmark
            startTime = System.nanoTime();

            // 1. Get engine instance
            OCRStudioSDKInstance engine = Engine.getInstance();

            // 2. Create new session settings object
            JSONObject sessionParamsJson = new JSONObject();
            // 2.1 Set session_type
            // session_type: "document_recognition" for recognition by one frame
            sessionParamsJson.put("session_type", "video_recognition");
            // 2.2 Set target_group_type
            sessionParamsJson.put("target_group_type", Settings.currentMode);
            // 2.3 Set target_masks
            sessionParamsJson.put("target_masks", new JSONArray(Settings.currentMask));
            // 2.4 Set field_geometry to draw quads during recognition
            sessionParamsJson.put("output_modes", "field_geometry");


            // 2.5 Set custom options
            JSONObject optionsJson = new JSONObject();
            Map<String, String> map = Settings.options;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                optionsJson.put(key, value);
            }
            String timeStamp = new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime());
            optionsJson.put("currentDate", timeStamp);
            // Adding session options
            sessionParamsJson.put("options", optionsJson);

            String sessionParams = sessionParamsJson.toString();

            // 3. Create recognition session
            try {
                session = engine.CreateSession( Settings.signature, sessionParams, anOCRStudioSDKDelegate );

                // Dynamic activation
                // String dynKey = session.UnlockRequest().CStr();
                // String actKey = Engine.getActivationKey(dynKey);
                // session.Unlock(actKey);

                long elapsedTime = System.nanoTime() - startTime;
                totalTime = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
                Log.d(TAG, "Create VideoSession time: " + totalTime);

            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }

        } catch (Exception e) {
            Log.e(TAG, "Create VideoSession error", e);
        }
    }

    /**
     * Example of creating session for image recognition.
     * 1. Get Engine instance
     * 2. Create session
     * 3. Send file to Session
     * 4. Get first result
     * 5. Reset session
     */

    public static void createImageSession() {
        try {
            // Benchmark
            startTime = System.nanoTime();

            // 1. Get engine instance
            OCRStudioSDKInstance engine = Engine.getInstance();
            // 2. Create new session params
            JSONObject sessionParamsJson = new JSONObject();
            // 2.1 Set session_type
            // session_type: "document_recognition" for recognition by one frame
            sessionParamsJson.put("session_type", "document_recognition");
            // 2.2 Set target_group_type
            sessionParamsJson.put("target_group_type", Settings.currentMode);
            // 2.3 Set target_masks
            sessionParamsJson.put("target_masks", new JSONArray(Settings.currentMask));
            // 2.4 Set field_geometry to draw quads during recognition
            sessionParamsJson.put("output_modes", "field_geometry");

            // 2.5 Set custom options
            JSONObject optionsJson = new JSONObject();
            Map<String, String> map = Settings.options;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                optionsJson.put(key, value);
            }

            String timeStamp = new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime());
            optionsJson.put("currentDate", timeStamp);

            // Adding session options
            sessionParamsJson.put("options", optionsJson);
            String sessionParams = sessionParamsJson.toString();

            long startTime = System.nanoTime();
            // 3. Create recognition session
            session = engine.CreateSession(Settings.signature, sessionParams);

            // Dynamic activation
            // String dynKey = session.UnlockRequest().CStr();
            // String actKey = getActivationKey(dynKey);
            // session.Unlock(actKey);

            long elapsedTime = System.nanoTime() - startTime;
            totalTime = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);

            Log.i(TAG, "Create ImageSession time: " + totalTime);

        } catch (Exception e) {
            Log.e(TAG, "Create ImageSession error", e);
        }
    }

    public static String getFaceComparison(OCRStudioSDKImage imageA, OCRStudioSDKImage imageB) throws Exception {

        String similarity = "";
        String status = "";

        try {
            // Benchmark
            startTime = System.nanoTime();

            // 1. Get engine instance
            OCRStudioSDKInstance engine = Engine.getInstance();

            JSONObject sessionParamsJson = new JSONObject();
            // 2.1 Set session_type
            // session_type: "face_matching" for face comparative
            sessionParamsJson.put("session_type", "face_matching");
            // 2.2 Set target_group_type
            sessionParamsJson.put("target_group_type", "default");
            String sessionParams = sessionParamsJson.toString();

            OCRStudioSDKSession session = engine.CreateSession(Settings.signature, sessionParams);

            // Dynamic activation
            // String dynKey = session.UnlockRequest().CStr();
            // String actKey = getActivationKey(dynKey);
            // session.Unlock(actKey);

            // PROCESS images
            session.ProcessImage(imageA);
            session.ProcessImage(imageB);

            // Read the result
            OCRStudioSDKResult result = session.CurrentResult();
            OCRStudioSDKTarget target = result.TargetByIndex(0);
            for (OCRStudioSDKItemIterator item_it = target.ItemsBegin("string"); !item_it.IsEqualTo(target.ItemsEnd("string")); item_it.Step()) {
                if (item_it.Item().Name().equals("status")) {
                    status = item_it.Item().Value();
                }
                if (item_it.Item().Name().equals("similarity_estimation")) {
                    similarity = item_it.Item().Value();
                }
            }

            // No similarity => error => return status (which describes the error)
            if(similarity == null) throw new Exception(status);

            long elapsedTime = System.nanoTime() - startTime;
            totalTime = TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
            Log.i(TAG, "Face comparison Time: " + totalTime + " ms");
            // Free resources
            // session.Reset();

        } catch (Exception e) {
            Log.e(TAG,"Face comparison error",e);
            throw new Exception(e.getMessage());
        }

        return similarity;
    }

    public static String getActivationKey(String dynKey) {
        try {
            String apiKey = "";
            // http or https
            String actUrl = "";

            String data = "{\"action\": \"activate_id_session\",  \"message\": \"" + dynKey + "\" }";
            System.out.printf("data is " + data + "\n");
            URL requestUrl = new URL(actUrl);

            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setConnectTimeout(4 * 1000);
            connection.setReadTimeout(4 * 1000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-API-Key", apiKey);
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("User-Agent", System.getProperty("http.agent"));
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            byte[] message = data.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", "" + message.length);

            OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
            out.write(data);
            out.flush();
            out.close();

            if (connection.getResponseCode() != 200) {

                String error;
                try (Scanner scanner = new Scanner(connection.getErrorStream())) {
                    String json = scanner.useDelimiter("\\A").next();
                    System.out.printf("json0 " + json + "\n");
                    JSONObject object = (JSONObject) new JSONTokener(json).nextValue();
                    error = object.getString("detail");
                }
                throw new RuntimeException(error);
            }

            InputStream response = connection.getInputStream();
            try (Scanner scanner = new Scanner(response)) {
                String json = scanner.useDelimiter("\\A").next();
                JSONObject object = (JSONObject) new JSONTokener(json).nextValue();
                return object.getString("message");
            }

        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException("Network activation: " + e.getMessage());
        }
    }

    // ===========================================================================

    private static final String FRAUD_CHECK_ITEMS_TYPE = "fraud_attempt";
    private static final String DATA_CHECK_ITEM_NAME = "fraud_attempt_data";
    private static final String FACE_CHECK_ITEM_NAME = "fraud_attempt_face";

    public static SessionNfcResult processNfc(SessionNfcData nfcData) {
        Log.w(TAG,"processNfc, mrz:"+nfcData.mrz);
        try {
            OCRStudioSDKTarget target = session.CurrentResult().TargetByIndex(0);
            // Pack photo to string
            String photoString = nfcData.image.ExportBase64JPEG().CStr();

            // PREPARE DATA
            JSONObject doc = new JSONObject(target.Description());
            JSONObject json = new JSONObject()
                    .put("doc_type", doc.getString("specific_type"))
                    .put("physical_fields", new JSONObject()
                            .put("rfid_mrz", new JSONObject()
                                    .put("value", nfcData.mrz)
                                    .put("type", "String")
                            ).put("rfid_photo", new JSONObject().put("value", photoString).put("type", "Image"))
                    );

            // PROCESS
            session.ProcessData(json.toString());

            // PARSE THE RESULT
            target = session.CurrentResult().TargetByIndex(0); // RETAKE THE RESULT TARGET
            Map<String,String> items = loadTargetItems(target, FRAUD_CHECK_ITEMS_TYPE);
            return new SessionNfcResultSuccess(
                    items.get(DATA_CHECK_ITEM_NAME),
                    items.get(FACE_CHECK_ITEM_NAME)
            );
        } catch(Exception e) {
            Log.e(TAG,"processNfc error",e);
            return new SessionNfcResultError(e.getMessage());
        }
    }

    private static Map<String,String> loadTargetItems(OCRStudioSDKTarget target, String type){
        HashMap<String,String> map = new HashMap<>();
        OCRStudioSDKItemIterator iterator    = target.ItemsBegin(type);
        OCRStudioSDKItemIterator iteratorEnd = target.ItemsEnd  (type);
        while (!iterator.IsEqualTo(iteratorEnd)) {
            OCRStudioSDKItem item = iterator.Item();
            map.put(item.Name(), item.Value());
            iterator.Step();
        }
        return map;
    }
}
