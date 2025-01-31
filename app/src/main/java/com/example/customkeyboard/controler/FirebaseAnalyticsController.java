package com.example.customkeyboard.controler;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;

import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseAnalyticsController {
    private final FirebaseAnalytics analytics;

    public FirebaseAnalyticsController(Context context) {
        analytics = FirebaseAnalytics.getInstance(context);
    }

    public void logEvent(String eventName, String paramsKey, String paramsValue) {
        if (analytics != null) {
            Bundle params = new Bundle();
            if (paramsKey != null && paramsValue != null) {
                params.putString(paramsKey, paramsValue);
            }
            analytics.logEvent(eventName, params);
        }
    }

    public void logSpecialKeyEvent(int keyCode, KeyEvent event, String action) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putString("action", action); // Either "key_down" or "key_up"
            params.putInt("key_code", keyCode);
            params.putString("key_label", KeyEvent.keyCodeToString(keyCode)); // e.g., "KEYCODE_A"
            params.putInt("repeat_count", event.getRepeatCount());

            analytics.logEvent("key_event", params);
        }
    }

    public void logKeyEvents(int primaryCode) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putInt("key_code", primaryCode);
            params.putString("key_label", Character.toString((char) primaryCode));
            analytics.logEvent("key_pressed", params);
        }
    }

    public void logScreenView(String screenName, String screenClass) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
            params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
        }
    }

    public void logAutocorrectEvent(String word) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putString("word", word);
            analytics.logEvent("auto_correction", params);
        }
    }

    public void logSuggestionEvent(String word) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putString("word", word);
            analytics.logEvent("suggestion_word", params);
        }
    }

    public void logLanguageSwitch(String previousLanguage, String newLanguage) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putString("previous_language", previousLanguage);
            params.putString("new_language", newLanguage);
            analytics.logEvent("language_switch", params);
        }
    }

    public void logKeyPressEvent(int keyCode, String keyType) {
        if (analytics != null) {
            Bundle params = new Bundle();
            params.putInt("key_code", keyCode);
            params.putString("key_type", keyType);
            analytics.logEvent("key_press", params);
        }
    }
}
