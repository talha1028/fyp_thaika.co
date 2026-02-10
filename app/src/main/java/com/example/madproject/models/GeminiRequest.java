package com.example.madproject.models;

public class GeminiRequest {
    private Content[] contents;

    public GeminiRequest(String message) {
        this.contents = new Content[]{new Content(message)};
    }

    public Content[] getContents() {
        return contents;
    }

    public void setContents(Content[] contents) {
        this.contents = contents;
    }

    public static class Content {
        private Part[] parts;

        public Content(String text) {
            this.parts = new Part[]{new Part(text)};
        }

        public Part[] getParts() {
            return parts;
        }

        public void setParts(Part[] parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
