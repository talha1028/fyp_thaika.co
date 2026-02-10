package com.example.madproject.models;

public class GeminiResponse {
    private Candidate[] candidates;
    private PromptFeedback promptFeedback;

    public Candidate[] getCandidates() {
        return candidates;
    }

    public void setCandidates(Candidate[] candidates) {
        this.candidates = candidates;
    }

    public PromptFeedback getPromptFeedback() {
        return promptFeedback;
    }

    public void setPromptFeedback(PromptFeedback promptFeedback) {
        this.promptFeedback = promptFeedback;
    }

    public static class Candidate {
        private Content content;
        private String finishReason;
        private int index;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    public static class Content {
        private Part[] parts;
        private String role;

        public Part[] getParts() {
            return parts;
        }

        public void setParts(Part[] parts) {
            this.parts = parts;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public static class PromptFeedback {
        private SafetyRating[] safetyRatings;

        public SafetyRating[] getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(SafetyRating[] safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    public static class SafetyRating {
        private String category;
        private String probability;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProbability() {
            return probability;
        }

        public void setProbability(String probability) {
            this.probability = probability;
        }
    }
}
