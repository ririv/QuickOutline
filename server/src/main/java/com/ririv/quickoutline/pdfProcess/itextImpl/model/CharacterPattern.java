package com.ririv.quickoutline.pdfProcess.itextImpl.model;

import java.util.Map;
import java.util.HashMap;

public class CharacterPattern {
    private final int[] counts = new int[12];
    private int total = 0;

    public CharacterPattern(String text) {
        for (char c : text.toCharArray()) {
            int type = getCharType(c);
            counts[type]++;
            total++;
        }
    }

    private int getCharType(char c) {
        if (Character.isLowerCase(c)) return 3;
        if (Character.isUpperCase(c)) return 2;
        if (Character.isTitleCase(c)) return 2; // Treat TitleCase as UpperCase
        if (Character.isDigit(c)) return 1;
        if (Character.isWhitespace(c)) return 10;
        if (c == '.' || c == '?' || c == '!') return 6; // Sentence-ending
        if (c == '-' || c == '_' || c == '–' || c == '—') return 7; // Dash punctuation
        if (c == '(' || c == '[' || c == '{') return 8; // Open punctuation
        if (c == ')' || c == ']' || c == '}') return 8; // Close punctuation
        if (Character.getType(c) == Character.MATH_SYMBOL) return 9;
        if (Character.isISOControl(c)) return 0;
        return 8; // Other punctuation/symbols
    }

    public int getPatternType() {
        if (total == 0) return 0;

        Map<Integer, Double> scores = new HashMap<>();
        for (int i = 0; i < 11; i++) {
            scores.put(i, 0.0);
        }

        // Type 2: Uppercase, Type 3: Lowercase, etc.
        addScore(scores, 2, counts[2] * 10);
        addScore(scores, 0, counts[0] + counts[2]);
        addScore(scores, 3, counts[3] - counts[4]*3 - counts[5]*3 - counts[6]*3 - counts[7]*3 - counts[8]*3 - counts[9]*10);
        addScore(scores, 4, counts[4]);
        addScore(scores, 5, counts[5] - counts[6]*10 - counts[7]*10);
        addScore(scores, 6, counts[6]);
        addScore(scores, 7, counts[7]);
        addScore(scores, 8, counts[8]);
        addScore(scores, 9, counts[9]);
        addScore(scores, 10, counts[10]);

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);
    }

    private void addScore(Map<Integer, Double> scores, int type, double score) {
        scores.put(type, scores.getOrDefault(type, 0.0) + score);
    }
}