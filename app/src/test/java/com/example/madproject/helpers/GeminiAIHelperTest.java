package com.example.madproject.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.util.Log;

import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Tests GeminiAIHelper using a mocked GenerativeModelFutures/ChatFutures pair,
 * via the package-private GeminiAIHelper(GenerativeModelFutures) test constructor.
 * That constructor wires executor = Runnable::run, so FutureCallback completion
 * (via Futures.addCallback) happens synchronously — no latch/threading needed.
 */
public class GeminiAIHelperTest {

    private GenerativeModelFutures mockModelFutures;
    private ChatFutures mockChatFutures;
    private GeminiAIHelper helper;
    // android.util.Log is an unmocked SDK stub in plain JVM unit tests: it throws at runtime.
    // GeminiAIHelper's FutureCallback logs on both onSuccess and onFailure before invoking the
    // listener, and Guava's immediateFuture/immediateFailedFuture callback dispatch silently
    // swallows exceptions thrown by the listener runnable, so an unmocked Log call would
    // silently prevent the AIResponseListener from ever being invoked. Stub Log out.
    private MockedStatic<Log> mockedLog;

    @Before
    public void setUp() {
        mockedLog = Mockito.mockStatic(Log.class);
        mockModelFutures = mock(GenerativeModelFutures.class);
        mockChatFutures = mock(ChatFutures.class);
        when(mockModelFutures.startChat()).thenReturn(mockChatFutures);

        helper = new GeminiAIHelper(mockModelFutures);
    }

    @After
    public void tearDown() {
        mockedLog.close();
    }

    private static String textOf(Content content) {
        assertNotNull(content);
        assertFalse(content.getParts().isEmpty());
        Part part = content.getParts().get(0);
        assertTrue("Expected a TextPart, was: " + part, part instanceof TextPart);
        return ((TextPart) part).getText();
    }

    private static class RecordingListener implements GeminiAIHelper.AIResponseListener {
        String response;
        String error;

        @Override
        public void onResponse(String response) {
            this.response = response;
        }

        @Override
        public void onError(String error) {
            this.error = error;
        }
    }

    // --- sendMessage: system-context prefix only on first turn ---------------------------

    @Test
    public void sendMessage_firstCall_includesSystemContextPrefix() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("Hi there!");
        ListenableFuture<GenerateContentResponse> future = Futures.immediateFuture(mockResponse);
        when(mockChatFutures.sendMessage(any(Content.class))).thenReturn(future);

        RecordingListener listener = new RecordingListener();
        helper.sendMessage("What is the cost?", listener);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(mockChatFutures).sendMessage(captor.capture());

        String sentText = textOf(captor.getValue());
        assertTrue(sentText, sentText.contains("Thaika.co"));
        assertEquals("Hi there!", listener.response);
    }

    @Test
    public void sendMessage_secondCall_omitsSystemContextPrefix() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("response");
        ListenableFuture<GenerateContentResponse> future = Futures.immediateFuture(mockResponse);
        when(mockChatFutures.sendMessage(any(Content.class))).thenReturn(future);

        RecordingListener listener = new RecordingListener();
        helper.sendMessage("first message", listener);
        helper.sendMessage("second message", listener);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(mockChatFutures, org.mockito.Mockito.times(2)).sendMessage(captor.capture());

        String secondSentText = textOf(captor.getAllValues().get(1));
        assertFalse(secondSentText, secondSentText.contains("Thaika.co"));
        assertEquals("second message", secondSentText);
    }

    // --- One-shot prompt methods: prompt content -------------------------------------------

    @Test
    public void getConstructionEstimate_promptContainsProjectDescription() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("estimate text");
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFuture(mockResponse));

        RecordingListener listener = new RecordingListener();
        helper.getConstructionEstimate("2-story house in Lahore", listener);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(mockModelFutures).generateContent(captor.capture());

        String prompt = textOf(captor.getValue());
        assertTrue(prompt, prompt.contains("Thaika.co"));
        assertTrue(prompt, prompt.contains("2-story house in Lahore"));
        assertTrue(prompt, prompt.contains("cost estimate in PKR"));
        assertEquals("estimate text", listener.response);
    }

    @Test
    public void generateContract_promptContainsFormattedAmountAndDetails() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("contract text");
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFuture(mockResponse));

        RecordingListener listener = new RecordingListener();
        helper.generateContract(
                "Roof Repair", "Ali Khan", "Bilal Contractors",
                "Repair the roof after storm damage", 50000.0, "2 weeks",
                "Karachi", listener);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(mockModelFutures).generateContent(captor.capture());

        String prompt = textOf(captor.getValue());
        assertTrue(prompt, prompt.contains("Roof Repair"));
        assertTrue(prompt, prompt.contains("Ali Khan"));
        assertTrue(prompt, prompt.contains("Bilal Contractors"));
        assertTrue(prompt, prompt.contains("Karachi"));
        assertTrue(prompt, prompt.contains("PKR " + String.format("%.0f", 50000.0)));
        assertTrue(prompt, prompt.contains("PKR 50000"));
        assertEquals("contract text", listener.response);
    }

    @Test
    public void generateProgressReport_computesPercentageCorrectly() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("report text");
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFuture(mockResponse));

        RecordingListener listener = new RecordingListener();
        // completed=5, total=20 -> 5*100/20 = 25%
        helper.generateProgressReport("Site A", 20, 5, 10, 5, listener);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(mockModelFutures).generateContent(captor.capture());

        String prompt = textOf(captor.getValue());
        assertTrue(prompt, prompt.contains("Site A"));
        assertTrue(prompt, prompt.contains("Total Tasks: 20"));
        assertTrue(prompt, prompt.contains("Completed: 5 (25%)"));
        assertTrue(prompt, prompt.contains("Ongoing: 10"));
        assertTrue(prompt, prompt.contains("Not Started: 5"));
        assertEquals("report text", listener.response);
    }

    @Test
    public void generateProgressReport_zeroTotal_showsZeroPercentWithoutDivideByZero() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("report text");
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFuture(mockResponse));

        RecordingListener listener = new RecordingListener();
        helper.generateProgressReport("Empty Site", 0, 0, 0, 0, listener);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(mockModelFutures).generateContent(captor.capture());

        String prompt = textOf(captor.getValue());
        assertTrue(prompt, prompt.contains("Total Tasks: 0"));
        assertTrue(prompt, prompt.contains("Completed: 0 (0%)"));
        assertEquals("report text", listener.response);
    }

    // --- Response handling: success / null text / failures ----------------------------------

    @Test
    public void handleResponse_successWithText_deliversTextToListener() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn("Here is your answer.");
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFuture(mockResponse));

        RecordingListener listener = new RecordingListener();
        helper.getSafetyTips("welding", listener);

        assertEquals("Here is your answer.", listener.response);
        assertNull(listener.error);
    }

    @Test
    public void handleResponse_successWithNullText_deliversFallbackMessage() {
        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.getText()).thenReturn(null);
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFuture(mockResponse));

        RecordingListener listener = new RecordingListener();
        helper.getSafetyTips("welding", listener);

        assertEquals("No response generated.", listener.response);
        assertNull(listener.error);
    }

    @Test
    public void handleResponse_failureWithApiKeyMessage_deliversInvalidApiKeyError() {
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("API key not valid")));

        RecordingListener listener = new RecordingListener();
        helper.getSafetyTips("welding", listener);

        assertEquals("Invalid API key.", listener.error);
        assertNull(listener.response);
    }

    @Test
    public void handleResponse_failureWithNetworkMessage_deliversNetworkError() {
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("network unreachable")));

        RecordingListener listener = new RecordingListener();
        helper.getSafetyTips("welding", listener);

        assertEquals("Network error. Check connection.", listener.error);
        assertNull(listener.response);
    }

    @Test
    public void handleResponse_failureWithUnrelatedMessage_deliversMessageUnchanged() {
        when(mockModelFutures.generateContent(any(Content.class)))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("something else broke")));

        RecordingListener listener = new RecordingListener();
        helper.getSafetyTips("welding", listener);

        assertEquals("something else broke", listener.error);
        assertNull(listener.response);
    }
}
