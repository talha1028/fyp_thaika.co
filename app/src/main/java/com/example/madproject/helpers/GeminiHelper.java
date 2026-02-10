package com.example.madproject.helpers;

import android.util.Log;

import com.example.madproject.models.GeminiRequest;
import com.example.madproject.models.GeminiResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiHelper {
    private static final String TAG = "GeminiHelper";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Gson gson = new Gson();

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public static void sendMessage(String apiKey, String message, GeminiCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key is required");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            callback.onError("Message cannot be empty");
            return;
        }

        try {
            GeminiRequest request = new GeminiRequest(message.trim());
            String json = gson.toJson(request);

            RequestBody body = RequestBody.create(json, JSON);
            Request httpRequest = new Request.Builder()
                    .url(BASE_URL + apiKey)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error: " + e.getMessage(), e);
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";

                    if (response.isSuccessful()) {
                        try {
                            GeminiResponse geminiResponse = gson.fromJson(responseBody, GeminiResponse.class);

                            if (geminiResponse != null &&
                                    geminiResponse.getCandidates() != null &&
                                    geminiResponse.getCandidates().length > 0 &&
                                    geminiResponse.getCandidates()[0].getContent() != null &&
                                    geminiResponse.getCandidates()[0].getContent().getParts() != null &&
                                    geminiResponse.getCandidates()[0].getContent().getParts().length > 0) {

                                String text = geminiResponse.getCandidates()[0].getContent().getParts()[0].getText();
                                if (text != null && !text.isEmpty()) {
                                    callback.onSuccess(text);
                                } else {
                                    callback.onError("Empty response from Gemini");
                                }
                            } else {
                                Log.e(TAG, "Invalid response structure: " + responseBody);
                                callback.onError("Invalid response structure");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Parse error: " + e.getMessage(), e);
                            Log.e(TAG, "Response body: " + responseBody);
                            callback.onError("Parse error: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "API error: " + response.code() + " - " + responseBody);
                        callback.onError("API error: " + response.code() + " - " + responseBody);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }

    public static void sendMessageWithContext(String apiKey, String systemInstruction, String message, GeminiCallback callback) {
        String fullMessage = systemInstruction + "\n\nUser: " + message;
        sendMessage(apiKey, fullMessage, callback);
    }
}
