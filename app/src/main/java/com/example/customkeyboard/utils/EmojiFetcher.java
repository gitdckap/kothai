package com.example.customkeyboard.utils;

import java.util.ArrayList;
import java.util.List;

public class EmojiFetcher {
    public static List<String> getAllEmojis() {
        List<String> emojis = new ArrayList<>();

        // Add emojis from different Unicode ranges
        addEmojiRange(emojis, 0x1F600, 0x1F64F); // Smileys & Emotion
        addEmojiRange(emojis, 0x1F300, 0x1F5FF); // Symbols & Pictographs
        addEmojiRange(emojis, 0x1F680, 0x1F6FF); // Transport & Map Symbols
        addEmojiRange(emojis, 0x2600, 0x26FF);   // Miscellaneous Symbols
        addEmojiRange(emojis, 0x1F1E6, 0x1F1FF); // Flags

        return emojis;
    }

    private static void addEmojiRange(List<String> emojis, int start, int end) {
        for (int codePoint = start; codePoint <= end; codePoint++) {
            if (Character.isValidCodePoint(codePoint)) {
                emojis.add(new String(Character.toChars(codePoint)));
            }
        }
    }
}