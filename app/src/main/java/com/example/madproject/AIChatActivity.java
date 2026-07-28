package com.example.madproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.helpers.KeyboardInsets;
import com.example.madproject.adapters.ChatMessageAdapter;
import com.example.madproject.helpers.GeminiAIHelper;
import com.example.madproject.models.ChatMessage;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class AIChatActivity extends AppCompatActivity {

    private static final String TAG = "AIChat";

    private Toolbar toolbar;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private HorizontalScrollView suggestionsContainer;
    private ChipGroup chipGroupSuggestions;
    private LinearLayout emptyState;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private GeminiAIHelper aiHelper;
    private boolean destroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        initViews();

        // Keep the input row above the keyboard (adjustResize is a no-op under enforced
        // edge-to-edge on SDK 35+, so the IME inset has to be consumed).
        KeyboardInsets.liftAboveKeyboard(findViewById(R.id.inputContainer));

        setupToolbar();
        setupRecyclerView();
        setupSuggestions();
        setupClickListeners();

        // Initialize AI Helper
        aiHelper = new GeminiAIHelper(this);

        // Show welcome message
        showWelcomeMessage();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        suggestionsContainer = findViewById(R.id.suggestionsContainer);
        chipGroupSuggestions = findViewById(R.id.chipGroupSuggestions);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }
        toolbar.inflateMenu(R.menu.menu_ai_chat);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_reset_chat) {
                resetChat();
                return true;
            }
            return false;
        });
    }

    private void resetChat() {
        aiHelper.resetConversation();
        messageList.clear();
        messageAdapter.notifyDataSetChanged();
        suggestionsContainer.setVisibility(View.VISIBLE);
        showWelcomeMessage();
        Toast.makeText(this, "Conversation reset", Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(this, messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupSuggestions() {
        String[] suggestions = {
                "💰 Cost estimate",
                "⏱️ Timeline help",
                "🏗️ Materials needed",
                "⚠️ Safety tips",
                "✍️ Write job description"
        };

        for (String suggestion : suggestions) {
            Chip chip = new Chip(this);
            chip.setText(suggestion);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setOnClickListener(v -> handleSuggestionClick(suggestion));
            chipGroupSuggestions.addView(chip);
        }
    }

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void showWelcomeMessage() {
        emptyState.setVisibility(View.GONE);
        String welcomeText = "👋 Hello! I'm your AI construction assistant.\n\n" +
                "I can help you with:\n" +
                "• Cost estimates (in PKR)\n" +
                "• Project timelines\n" +
                "• Material recommendations\n" +
                "• Contractor suggestions\n" +
                "• Safety guidelines\n" +
                "• Job description writing\n\n" +
                "How can I help you today?";

        ChatMessage welcomeMessage = new ChatMessage(welcomeText, false, System.currentTimeMillis());
        addMessage(welcomeMessage);
    }

    private void handleSuggestionClick(String suggestion) {
        String prompt = "";

        if (suggestion.contains("Cost estimate")) {
            prompt = "I need a cost estimate for my construction project";
        } else if (suggestion.contains("Timeline")) {
            prompt = "How long will my project take?";
        } else if (suggestion.contains("Materials")) {
            prompt = "What materials do I need?";
        } else if (suggestion.contains("Safety")) {
            prompt = "What safety precautions should I take?";
        } else if (suggestion.contains("job description")) {
            prompt = "Help me write a job description";
        }

        etMessage.setText(prompt);
        sendMessage();
    }

    private void sendMessage() {
        String message = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            return;
        }

        // Hide suggestions and empty state after first message
        if (suggestionsContainer.getVisibility() == View.VISIBLE) {
            suggestionsContainer.setVisibility(View.GONE);
        }
        if (emptyState.getVisibility() == View.VISIBLE) {
            emptyState.setVisibility(View.GONE);
        }

        // Add user message
        ChatMessage userMessage = new ChatMessage(message, true, System.currentTimeMillis());
        addMessage(userMessage);

        // Clear input
        etMessage.setText("");

        // Show loading
        showLoading(true);

        // Get AI response
        aiHelper.sendMessage(message, new GeminiAIHelper.AIResponseListener() {
            @Override
            public void onResponse(String response) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    showLoading(false);
                    ChatMessage aiMessage = new ChatMessage(response, false, System.currentTimeMillis());
                    addMessage(aiMessage);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (destroyed) return;
                    showLoading(false);
                    String errorMessage = "Sorry, I encountered an error. Please try again.\n\n" +
                            "Error: " + error + "\n\n" +
                            "Make sure you have:\n" +
                            "1. Added your Gemini API key\n" +
                            "2. Added Gemini dependencies to build.gradle\n" +
                            "3. Internet connection";
                    ChatMessage aiMessage = new ChatMessage(errorMessage, false, System.currentTimeMillis());
                    addMessage(aiMessage);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyed = true;
        if (aiHelper != null) aiHelper.shutdown();
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.smoothScrollToPosition(messageList.size() - 1);
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnSend.setEnabled(false);
            etMessage.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSend.setEnabled(true);
            etMessage.setEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
