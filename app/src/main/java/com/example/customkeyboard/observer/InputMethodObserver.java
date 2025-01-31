package com.example.customkeyboard.observer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

public class InputMethodObserver extends ContentObserver {
    private final SharedPreferences preferences;
    private final Context context;

    public InputMethodObserver(Handler handler, Context context, SharedPreferences preferences) {
        super(handler);
        this.context = context;
        this.preferences = preferences;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onChange(boolean selfChange, @Nullable Uri uri) {
        super.onChange(selfChange, uri);
        SharedPreferences.Editor editor = preferences.edit();
        if (uri != null && uri.equals(Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD))) {
            // Input method has changed
            String newInputMethod = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (newInputMethod.contains("customkeyboard")) {
                editor.putBoolean("isKeyboardEnable", false).apply();
            }
            Log.d("InputMethodObserver", "Input method changed to: " + newInputMethod);
        }
    }
}
