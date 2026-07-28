package com.example.madproject.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Deserializes realistic Gemini API JSON payloads into {@link GeminiResponse}
 * and verifies the getter chain used by GeminiHelper to extract text.
 */
public class GeminiResponseGsonTest {

    private final Gson gson = new Gson();

    @Test
    public void deserialize_fullSuccessPayload_extractsTextViaGetterChain() {
        String json = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          { \"text\": \"Estimated cost is PKR 500,000 for this project.\" }\n" +
                "        ],\n" +
                "        \"role\": \"model\"\n" +
                "      },\n" +
                "      \"finishReason\": \"STOP\",\n" +
                "      \"index\": 0\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        GeminiResponse response = gson.fromJson(json, GeminiResponse.class);

        assertNotNull(response);
        assertNotNull(response.getCandidates());
        assertEquals(1, response.getCandidates().length);

        GeminiResponse.Candidate candidate = response.getCandidates()[0];
        assertEquals("STOP", candidate.getFinishReason());
        assertEquals(0, candidate.getIndex());

        assertNotNull(candidate.getContent());
        assertEquals("model", candidate.getContent().getRole());
        assertNotNull(candidate.getContent().getParts());
        assertEquals(1, candidate.getContent().getParts().length);

        String text = candidate.getContent().getParts()[0].getText();
        assertEquals("Estimated cost is PKR 500,000 for this project.", text);
    }

    @Test
    public void deserialize_payloadWithPromptFeedback_mapsSafetyRatings() {
        String json = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [ { \"text\": \"Some response text.\" } ],\n" +
                "        \"role\": \"model\"\n" +
                "      },\n" +
                "      \"finishReason\": \"STOP\",\n" +
                "      \"index\": 0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"promptFeedback\": {\n" +
                "    \"safetyRatings\": [\n" +
                "      { \"category\": \"HARM_CATEGORY_HARASSMENT\", \"probability\": \"NEGLIGIBLE\" },\n" +
                "      { \"category\": \"HARM_CATEGORY_DANGEROUS_CONTENT\", \"probability\": \"LOW\" }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        GeminiResponse response = gson.fromJson(json, GeminiResponse.class);

        assertNotNull(response);
        assertNotNull(response.getPromptFeedback());
        GeminiResponse.SafetyRating[] ratings = response.getPromptFeedback().getSafetyRatings();
        assertNotNull(ratings);
        assertEquals(2, ratings.length);

        assertEquals("HARM_CATEGORY_HARASSMENT", ratings[0].getCategory());
        assertEquals("NEGLIGIBLE", ratings[0].getProbability());

        assertEquals("HARM_CATEGORY_DANGEROUS_CONTENT", ratings[1].getCategory());
        assertEquals("LOW", ratings[1].getProbability());
    }

    @Test
    public void deserialize_missingCandidatesKey_returnsNullCandidatesWithoutThrowing() {
        String json = "{}";

        GeminiResponse response = gson.fromJson(json, GeminiResponse.class);

        assertNotNull(response);
        assertNull(response.getCandidates());
    }

    @Test
    public void deserialize_emptyCandidatesArray_returnsEmptyArray() {
        String json = "{ \"candidates\": [] }";

        GeminiResponse response = gson.fromJson(json, GeminiResponse.class);

        assertNotNull(response);
        assertNotNull(response.getCandidates());
        assertEquals(0, response.getCandidates().length);
    }
}
