package com.example.customkeyboard.model;

import java.util.List;

public class Emoji {
    String title;
    List<String> emojis;

    public Emoji(String title, List<String> emojis) {
        this.title = title;
        this.emojis = emojis;
    }

    public void setEmojis(List<String> emojis) {
        this.emojis = emojis;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getEmojis() {
        return emojis;
    }
}
