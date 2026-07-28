package com.example.madproject.helpers;

/**
 * Presentation-only formatting for people's names.
 *
 * Users sign up with whatever casing they type ("talha", "ahmad ali"), so names are capitalised at
 * the point of display rather than rewritten in Firestore - the stored value stays exactly what the
 * user entered.
 */
public final class NameFormatter {

    private NameFormatter() {
    }

    /**
     * Upper-cases the first letter of each whitespace-separated word.
     *
     * The rest of each word is left untouched on purpose: lower-casing it would mangle names that
     * are legitimately mixed-case, such as "McDonald" or "O'Brien".
     *
     * @return the formatted name, or the input unchanged if it is null or blank
     */
    public static String capitalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }

        StringBuilder out = new StringBuilder(name.length());
        boolean startOfWord = true;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c)) {
                startOfWord = true;
                out.append(c);
            } else if (startOfWord) {
                out.append(Character.toUpperCase(c));
                startOfWord = false;
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }
}
