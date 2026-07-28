package com.example.madproject.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Log;

import com.example.madproject.models.GeminiRequest;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

/**
 * Tests GeminiHelper.sendMessage / sendMessageWithContext against a MockWebServer
 * standing in for the real Generative Language API. GeminiHelper.BASE_URL is
 * package-private and non-final specifically so it can be redirected here.
 */
public class GeminiHelperTest {

    // Captured at class-load time, before any test mutates GeminiHelper.BASE_URL / client.
    private static final String ORIGINAL_BASE_URL = GeminiHelper.BASE_URL;
    private static final OkHttpClient ORIGINAL_CLIENT = GeminiHelper.client;

    private MockWebServer mockWebServer;
    // android.util.Log is an unmocked SDK stub in plain JVM unit tests: it throws at runtime,
    // which would silently break the async callback chain. Stub it out so Log.d/e/w calls in
    // GeminiHelper are no-ops. Mockito's static mocking is thread-confined to the thread that
    // registered it, so it only helps if the callback also fires on this (the test) thread --
    // see the Dispatcher swap below.
    private MockedStatic<Log> mockedLog;

    @Before
    public void setUp() throws IOException {
        mockedLog = Mockito.mockStatic(Log.class);
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        GeminiHelper.BASE_URL = mockWebServer.url("/").toString();
        // OkHttp normally runs enqueue() callbacks on its own Dispatcher thread pool, which
        // would be a different thread than the one holding the Log mock above (and would force
        // every test to synchronize via the CountDownLatch instead of asserting inline). Using
        // a direct executor makes enqueue() execute the whole request/parse/callback chain
        // synchronously on the calling (test) thread, matching Log's mock scope.
        GeminiHelper.client = new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(MoreExecutors.newDirectExecutorService()))
                .build();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
        GeminiHelper.BASE_URL = ORIGINAL_BASE_URL;
        GeminiHelper.client = ORIGINAL_CLIENT;
        mockedLog.close();
    }

    private static final Gson GSON = new Gson();

    private static String successJson(String text) {
        return "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [ { \"text\": " + GSON.toJson(text) + " } ],\n" +
                "        \"role\": \"model\"\n" +
                "      },\n" +
                "      \"finishReason\": \"STOP\",\n" +
                "      \"index\": 0\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    /** Simple test double capturing the async GeminiCallback result via a latch. */
    private static class TestCallback implements GeminiHelper.GeminiCallback {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile String successResponse;
        volatile String errorResponse;

        @Override
        public void onSuccess(String response) {
            successResponse = response;
            latch.countDown();
        }

        @Override
        public void onError(String error) {
            errorResponse = error;
            latch.countDown();
        }

        void await() throws InterruptedException {
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            if (!completed) {
                fail("Callback did not fire within the 5s timeout");
            }
        }
    }

    @Test
    public void sendMessage_success_returnsExtractedText() throws Exception {
        String expectedText = "Estimated cost is PKR 500,000.";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(successJson(expectedText)));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "How much to build a house?", callback);
        callback.await();

        assertNull(callback.errorResponse);
        assertEquals(expectedText, callback.successResponse);

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("POST", recordedRequest.getMethod());
        String contentType = recordedRequest.getHeader("Content-Type");
        assertNotNull(contentType);
        assertTrue("Content-Type should be application/json, was: " + contentType,
                contentType.contains("application/json"));

        String body = recordedRequest.getBody().readUtf8();
        assertTrue("Request body should contain the sent message: " + body,
                body.contains("How much to build a house?"));

        GeminiRequest parsedRequest = GSON.fromJson(body, GeminiRequest.class);
        assertEquals("How much to build a house?",
                parsedRequest.getContents()[0].getParts()[0].getText());
    }

    @Test
    public void sendMessage_emptyCandidatesArray_reportsInvalidResponseStructure() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{ \"candidates\": [] }"));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertEquals("Invalid response structure", callback.errorResponse);
    }

    @Test
    public void sendMessage_emptyPartText_reportsEmptyResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(successJson("")));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertEquals("Empty response from Gemini", callback.errorResponse);
    }

    @Test
    public void sendMessage_httpError400_reportsApiErrorWithCode() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"bad request\"}"));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertNotNull(callback.errorResponse);
        assertTrue(callback.errorResponse, callback.errorResponse.contains("API error: 400"));
    }

    @Test
    public void sendMessage_httpError403_reportsApiErrorWithCode() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(403).setBody("{\"error\":\"forbidden\"}"));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertNotNull(callback.errorResponse);
        assertTrue(callback.errorResponse, callback.errorResponse.contains("API error: 403"));
    }

    @Test
    public void sendMessage_httpError500_reportsApiErrorWithCode() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\":\"server error\"}"));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertNotNull(callback.errorResponse);
        assertTrue(callback.errorResponse, callback.errorResponse.contains("API error: 500"));
    }

    @Test
    public void sendMessage_malformedJsonBody_reportsParseError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{this is not valid json,,, ][}"));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertNotNull(callback.errorResponse);
        assertTrue(callback.errorResponse, callback.errorResponse.startsWith("Parse error: "));
    }

    @Test
    public void sendMessage_networkFailure_reportsNetworkError() throws Exception {
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "hello", callback);
        callback.await();

        assertNull(callback.successResponse);
        assertNotNull(callback.errorResponse);
        assertTrue(callback.errorResponse, callback.errorResponse.startsWith("Network error: "));
    }

    @Test
    public void sendMessage_nullApiKey_reportsErrorWithoutNetworkCall() throws Exception {
        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage(null, "hello", callback);
        callback.await();

        assertEquals("API key is required", callback.errorResponse);
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    public void sendMessage_emptyApiKey_reportsErrorWithoutNetworkCall() throws Exception {
        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("", "hello", callback);
        callback.await();

        assertEquals("API key is required", callback.errorResponse);
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    public void sendMessage_nullMessage_reportsErrorWithoutNetworkCall() throws Exception {
        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", null, callback);
        callback.await();

        assertEquals("Message cannot be empty", callback.errorResponse);
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    public void sendMessage_blankMessage_reportsErrorWithoutNetworkCall() throws Exception {
        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessage("test-api-key", "   \t\n  ", callback);
        callback.await();

        assertEquals("Message cannot be empty", callback.errorResponse);
        assertEquals(0, mockWebServer.getRequestCount());
    }

    @Test
    public void sendMessageWithContext_combinesSystemInstructionAndMessage() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(successJson("ok")));

        String systemInstruction = "You are a helpful assistant.";
        String message = "What is the weather?";

        TestCallback callback = new TestCallback();
        GeminiHelper.sendMessageWithContext("test-api-key", systemInstruction, message, callback);
        callback.await();

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        String body = recordedRequest.getBody().readUtf8();

        GeminiRequest parsedRequest = GSON.fromJson(body, GeminiRequest.class);
        String expectedFullMessage = systemInstruction + "\n\nUser: " + message;
        assertEquals(expectedFullMessage, parsedRequest.getContents()[0].getParts()[0].getText());
    }
}
