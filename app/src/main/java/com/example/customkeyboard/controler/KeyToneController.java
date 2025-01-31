package com.example.customkeyboard.controler;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.example.customkeyboard.view.CustomKeyboardView;

public class KeyToneController {
    Context context;
    SharedPreferences sharedPreferences;
    boolean isVibrateEnable;
    boolean isKeyToneEnable;
    AudioManager audioManager;
    Vibrator vibe;
    int effectType;
    CustomKeyboardView keyboardView;


    public KeyToneController(Context context, CustomKeyboardView keyboardView) {
        this.context = context;
        this.keyboardView = keyboardView;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        init();
    }

    private void init() {
        isVibrateEnable = sharedPreferences.getBoolean("vibrate_keyPress", false);
        isKeyToneEnable = sharedPreferences.getBoolean("sound_keyPress", true);
    }

    public void configTone(int code) {
        switch (code) {
            case 32:
                effectType = AudioManager.FX_KEYPRESS_SPACEBAR;
                break;
            case -5:
                effectType = AudioManager.FX_KEYPRESS_DELETE;
                break;
            default:
                effectType = AudioManager.FX_KEY_CLICK;
                break;
        }
    }

    public void play(int code) {
        init();
        configTone(code);
        if (isVibrateEnable) vibe.vibrate(100);
        if (isKeyToneEnable) {
            audioManager.playSoundEffect(effectType, Math.max(1, audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) / 10));
        }
    }
}
