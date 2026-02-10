package com.example.madproject.helpers;

import android.content.Context;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * GeminiAIHelper - AI Assistant for RebuildPak Construction Marketplace
 *
 * FIXED VERSION - Using "gemini-pro" model (most compatible)
 */
public class GeminiAIHelper {

    private static final String TAG = "GeminiAI";

    // ⚠️ IMPORTANT: Replace with your actual Gemini API key
    private static final String API_KEY = "***REMOVED_SECRET***";

    private GenerativeModelFutures model;
    private Executor executor;
    private Context context;

    public GeminiAIHelper(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();

        // ✅ FIXED: Using "gemini-pro" - most stable model
        GenerativeModel gm = new GenerativeModel(
                "gemini-1.5-flash",  // ← Changed from "gemini-1.5-flash"
                API_KEY
        );

        this.model = GenerativeModelFutures.from(gm);

        Log.d(TAG, "GeminiAI initialized with gemini-pro model");
    }

    public void sendMessage(String userMessage, AIResponseListener listener) {
        String systemContext = "You are an AI assistant for RebuildPak, a construction marketplace app in Pakistan. " +
                "You help users with construction-related queries.\n\n" +
                "Your capabilities:\n" +
                "- Provide cost estimates in Pakistani Rupees (PKR)\n" +
                "- Calculate project timelines and milestones\n" +
                "- Recommend construction materials available in Pakistan\n" +
                "- Suggest appropriate contractor types for jobs\n" +
                "- Explain construction terminology simply\n" +
                "- Provide safety guidelines\n" +
                "- Help write clear job descriptions\n\n" +
                "Guidelines:\n" +
                "- Keep responses concise and practical (2-3 paragraphs max)\n" +
                "- Use simple language, avoid jargon\n" +
                "- Always use PKR for costs\n" +
                "- Consider Pakistani construction standards and materials\n" +
                "- Be helpful and friendly\n\n" +
                "User question: " + userMessage;

        Content content = new Content.Builder()
                .addText(systemContext)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String aiResponse = result.getText();
                    Log.d(TAG, "✅ AI Response received successfully");
                    listener.onResponse(aiResponse);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing response: " + e.getMessage());
                    listener.onError("Error parsing response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "❌ AI Request failed: " + t.getMessage());
                String errorMsg = t.getMessage();

                if (errorMsg != null && errorMsg.contains("API key")) {
                    listener.onError("Invalid API key. Generate new key from Google AI Studio");
                } else if (errorMsg != null && errorMsg.contains("models/gemini")) {
                    listener.onError("Model not found. Try generating a new API key");
                } else if (errorMsg != null && errorMsg.contains("network")) {
                    listener.onError("Network error. Check internet connection");
                } else {
                    listener.onError(errorMsg != null ? errorMsg : "Unknown error occurred");
                }
            }
        }, executor);
    }

    public void getConstructionEstimate(String projectDescription, AIResponseListener listener) {
        String prompt = "Provide a rough cost estimate in PKR for this construction project in Pakistan: " +
                projectDescription +
                "\n\nBreak down costs by: Materials, Labor, Equipment, Total estimated cost. Keep it concise.";
        sendMessage(prompt, listener);
    }

    public void getTimelineEstimate(String projectDescription, AIResponseListener listener) {
        String prompt = "Estimate the timeline for this construction project: " +
                projectDescription +
                "\n\nProvide: Estimated total days, Key phases with durations, Factors that could affect timeline. Be brief.";
        sendMessage(prompt, listener);
    }

    public void getMaterialRecommendations(String projectType, AIResponseListener listener) {
        String prompt = "What materials are needed for " + projectType +
                " in Pakistan?\n\nList: Essential materials, Approximate quantities, Estimated costs in PKR. Keep it concise.";
        sendMessage(prompt, listener);
    }

    public void getContractorRecommendation(String jobDescription, AIResponseListener listener) {
        String prompt = "What type of contractor is best for this job: " +
                jobDescription +
                "\n\nSuggest: Primary contractor type, Required skills, Any additional specialists. Be brief.";
        sendMessage(prompt, listener);
    }

    public void helpWriteJobDescription(String basicInfo, AIResponseListener listener) {
        String prompt = "Help me write a clear job description for: " + basicInfo +
                "\n\nProvide: Clear title, Detailed description, Required skills, Expected deliverables. Keep it professional and concise.";
        sendMessage(prompt, listener);
    }

    public void getSafetyTips(String workType, AIResponseListener listener) {
        String prompt = "What are key safety considerations for " + workType +
                " work?\n\nProvide: Top 5 safety tips, Required safety equipment, Common hazards. Be brief and actionable.";
        sendMessage(prompt, listener);
    }

    public interface AIResponseListener {
        void onResponse(String response);
        void onError(String error);
    }
}