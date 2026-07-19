package com.example.madproject.helpers;

import android.content.Context;
import android.util.Log;

import com.example.madproject.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiAIHelper {

    private static final String TAG = "GeminiAI";

    private static final String SYSTEM_CONTEXT =
            "You are an AI assistant for RebuildPak, a construction marketplace in Pakistan. " +
            "Help with: cost estimates (PKR), timelines, materials, safety, contractor advice, job descriptions. " +
            "Be concise (2-3 paragraphs max), use simple language, always use PKR for costs, " +
            "consider Pakistani standards and local material prices.";

    private final GenerativeModelFutures modelFutures;
    private final Executor executor;
    private ChatFutures chatSession;
    private boolean chatStarted = false;

    public GeminiAIHelper(Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash-lite", BuildConfig.GEMINI_API_KEY);
        this.modelFutures = GenerativeModelFutures.from(gm);
        resetConversation();
        Log.d(TAG, "GeminiAI initialized");
    }

    /** Multi-turn chat — maintains conversation history across calls. */
    public void sendMessage(String userMessage, AIResponseListener listener) {
        String text = chatStarted ? userMessage : SYSTEM_CONTEXT + "\n\nUser: " + userMessage;
        Content.Builder builder = new Content.Builder();
        builder.setRole("user");
        builder.addText(text);
        Content content = builder.build();
        chatStarted = true;

        ListenableFuture<GenerateContentResponse> response = chatSession.sendMessage(content);
        handleResponse(response, listener);
    }

    /** Resets conversation history — call when opening a new chat session. */
    public void resetConversation() {
        chatSession = modelFutures.startChat();
        chatStarted = false;
    }

    /** One-shot estimate — does NOT affect chat history. */
    public void getConstructionEstimate(String projectDescription, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nProvide a cost estimate in PKR for: " + projectDescription +
                "\nBreak down: Materials, Labor, Equipment, Total. Keep it concise.";
        sendOneShot(prompt, listener);
    }

    public void getTimelineEstimate(String projectDescription, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nEstimate timeline for: " + projectDescription +
                "\nProvide: Total days, Key phases with durations. Be brief.";
        sendOneShot(prompt, listener);
    }

    public void getMaterialRecommendations(String projectType, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nList materials needed for " + projectType +
                " in Pakistan. Include: material name, approx quantity, estimated PKR cost. Concise list format.";
        sendOneShot(prompt, listener);
    }

    public void getContractorRecommendation(String jobDescription, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nWhat contractor type is best for: " + jobDescription +
                "\nSuggest: Primary type, Required skills, Any specialists. Be brief.";
        sendOneShot(prompt, listener);
    }

    public void helpWriteJobDescription(String basicInfo, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nWrite a professional job description for: " + basicInfo +
                "\nInclude: clear title, detailed description, required skills, deliverables. Professional tone.";
        sendOneShot(prompt, listener);
    }

    public void getSafetyTips(String workType, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nTop 5 safety tips for " + workType +
                " in Pakistan. Include: required PPE, common hazards, brief actionable tips.";
        sendOneShot(prompt, listener);
    }

    private void sendOneShot(String prompt, AIResponseListener listener) {
        Content.Builder builder = new Content.Builder();
        builder.addText(prompt);
        Content content = builder.build();
        ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);
        handleResponse(response, listener);
    }

    private void handleResponse(ListenableFuture<GenerateContentResponse> future, AIResponseListener listener) {
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                try {
                    String text = result.getText();
                    Log.d(TAG, "AI response received");
                    listener.onResponse(text != null ? text : "No response generated.");
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response: " + e.getMessage());
                    listener.onError("Error parsing response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "AI request failed: " + t.getMessage());
                String msg = t.getMessage();
                if (msg != null && msg.contains("API key")) {
                    listener.onError("Invalid API key.");
                } else if (msg != null && msg.contains("network")) {
                    listener.onError("Network error. Check connection.");
                } else {
                    listener.onError(msg != null ? msg : "Unknown error.");
                }
            }
        }, executor);
    }

    public void generateContract(String jobTitle, String clientName, String contractorName,
                                  String description, double amount, String timeline,
                                  String location, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nGenerate a formal construction contract in English for Pakistan with these details:\n" +
                "Project: " + jobTitle + "\n" +
                "Client: " + clientName + "\n" +
                "Contractor: " + contractorName + "\n" +
                "Scope: " + description + "\n" +
                "Contract Amount: PKR " + String.format("%.0f", amount) + "\n" +
                "Timeline: " + timeline + "\n" +
                "Location: " + location + "\n\n" +
                "Include: parties, scope, payment schedule (30% deposit, 70% on completion), " +
                "timeline, dispute resolution, termination clause. Professional legal format. " +
                "Note: This is a template — advise parties to get legal review.";
        sendOneShot(prompt, listener);
    }

    public void checkPermitRequirements(String workType, String location, AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nWhat government permits, NOCs, and approvals are typically required in Pakistan for: " +
                workType + " work in " + location + "?\n\n" +
                "List: required permits, issuing authority, approximate fees (PKR), typical processing time. " +
                "Be specific to Pakistani regulations (CDA, LDA, KDA, etc. as applicable).";
        sendOneShot(prompt, listener);
    }

    public void generateProgressReport(String projectName, int total, int completed,
                                        int ongoing, int notStarted, AIResponseListener listener) {
        int progressPct = total > 0 ? (completed * 100 / total) : 0;
        String prompt = SYSTEM_CONTEXT + "\n\nGenerate a professional site progress report for:\n" +
                "Project: " + projectName + "\n" +
                "Total Tasks: " + total + "\n" +
                "Completed: " + completed + " (" + progressPct + "%)\n" +
                "Ongoing: " + ongoing + "\n" +
                "Not Started: " + notStarted + "\n\n" +
                "Include: executive summary, progress assessment, " +
                "risk flags (if behind schedule), recommended next steps. Keep it concise.";
        sendOneShot(prompt, listener);
    }

    public void getBudgetOptimizationTips(String jobType, String description, double budget,
                                           String location, String timeline, int totalBids,
                                           AIResponseListener listener) {
        String prompt = SYSTEM_CONTEXT + "\n\nProvide budget optimization tips for this construction job in Pakistan:\n" +
                "Type: " + jobType + "\n" +
                "Description: " + (description != null && !description.isEmpty() ? description : "N/A") + "\n" +
                "Budget: PKR " + String.format("%.0f", budget) + "\n" +
                "Location: " + location + "\n" +
                "Timeline: " + (timeline != null && !timeline.isEmpty() ? timeline : "Not specified") + "\n" +
                "Bids received so far: " + totalBids + "\n\n" +
                "Give: 5 specific cost-saving strategies, local sourcing tips, " +
                "common budget overrun causes to avoid, value-engineering options. Be practical.";
        sendOneShot(prompt, listener);
    }

    public interface AIResponseListener {
        void onResponse(String response);
        void onError(String error);
    }
}
