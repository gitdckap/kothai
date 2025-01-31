package com.example.customkeyboard.model;

public class Clipboard {
    String textValue;
    String id;

    public Clipboard(String textValue, String id) {
        this.textValue = textValue;
        this.id = id;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
