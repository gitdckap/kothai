package com.example.customkeyboard.model;

import java.util.HashMap;
import java.util.Map;

public class EmojiRepository {
    private Map<String, String> emojiShortcuts;

    public EmojiRepository() {
        initialise();
    }

    private void initialise() {
        emojiShortcuts = new HashMap<>();
        emojiShortcuts.put(":)", "🙂");
        emojiShortcuts.put(":]", "🙂");
        emojiShortcuts.put(":[", "😡");
        emojiShortcuts.put(">:", "😡");
        emojiShortcuts.put(":D", "😄");
        emojiShortcuts.put(":(", "☹️");
        emojiShortcuts.put(";)", "😉");
        emojiShortcuts.put(";]", "😉");
        emojiShortcuts.put(":*", "😗");
        emojiShortcuts.put(":×", "😘");
        emojiShortcuts.put(":/", "🤔");
        emojiShortcuts.put(";(", "😢");
        emojiShortcuts.put(";p", "😜");
        emojiShortcuts.put("-_-", "😑");
        emojiShortcuts.put(":|", "😑");
        emojiShortcuts.put(":}", "😁");
        emojiShortcuts.put(":{", "😞");
        emojiShortcuts.put("XD", "😆");
        emojiShortcuts.put("o_O", "😲");
        emojiShortcuts.put(":S", "😵");
        emojiShortcuts.put(":s", "😵");
        emojiShortcuts.put(":X", "😶");
        emojiShortcuts.put("xoxo", "😍");
        emojiShortcuts.put(":o", "😱");
        emojiShortcuts.put(":O", "😮");
        emojiShortcuts.put("B)", "😎");
        emojiShortcuts.put("<3", "❤️");
    }

    public String getEmoji(String text) {
        return emojiShortcuts.get(text);
    }

}
