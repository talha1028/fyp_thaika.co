package com.example.madproject.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Round-trips {@link GeminiRequest} through Gson to verify the wire format
 * (contents[0].parts[0].text) survives serialization/deserialization,
 * including special characters and non-Latin scripts.
 */
public class GeminiRequestGsonTest {

    private final Gson gson = new Gson();

    private String roundTripText(String message) {
        GeminiRequest request = new GeminiRequest(message);
        String json = gson.toJson(request);
        GeminiRequest deserialized = gson.fromJson(json, GeminiRequest.class);

        assertNotNull("Deserialized request should not be null", deserialized);
        assertNotNull("contents should not be null", deserialized.getContents());
        assertEquals(1, deserialized.getContents().length);
        assertNotNull("parts should not be null", deserialized.getContents()[0].getParts());
        assertEquals(1, deserialized.getContents()[0].getParts().length);

        return deserialized.getContents()[0].getParts()[0].getText();
    }

    @Test
    public void roundTrip_simpleMessage_preservesText() {
        String message = "Hello, how much does it cost to build a house?";
        assertEquals(message, roundTripText(message));
    }

    @Test
    public void roundTrip_quotesAndNewlines_preserveText() {
        String message = "He said \"hello\"\nNext line\tTabbed\\backslash";
        assertEquals(message, roundTripText(message));
    }

    @Test
    public void roundTrip_unicodeAndUrduText_preservesText() {
        String message = "تعمیراتی لاگت کا تخمینہ؟ 建設費用の見積もり emoji test 🏗️";
        assertEquals(message, roundTripText(message));
    }

    @Test
    public void roundTrip_emptyMessage_preservesEmptyText() {
        assertEquals("", roundTripText(""));
    }

    @Test
    public void serializedJson_hasExpectedStructure() {
        GeminiRequest request = new GeminiRequest("test message");
        String json = gson.toJson(request);

        assertEquals(true, json.contains("\"contents\""));
        assertEquals(true, json.contains("\"parts\""));
        assertEquals(true, json.contains("\"text\""));
        assertEquals(true, json.contains("test message"));
    }
}
