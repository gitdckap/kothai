package com.example.customkeyboard.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.example.customkeyboard.R;
import com.example.customkeyboard.controler.KeyPopUpController;
import com.example.customkeyboard.controler.KeyToneController;
import com.example.customkeyboard.model.KeyboardKeyState;
import com.example.customkeyboard.utils.Utils;

import java.util.List;

public class CustomKeyboardView extends KeyboardView implements GestureDetector.OnDoubleTapListener {
    SharedPreferences sharedPreferences;
    private final GestureDetector gestureDetector;
    private final KeyboardKeyState keyState;
    KeyPopUpController keyPopUpController;
    KeyToneController keyToneController;
    private final Handler handler = new Handler(Looper.getMainLooper());


    private long keyPressStartTime;
    private boolean isKeyHeld = false;
    private static final long KEY_HOLD_THRESHOLD = 500;

    public CustomKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        keyPopUpController = new KeyPopUpController(getContext(), this, sharedPreferences);
        keyToneController = new KeyToneController(getContext(), this);
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener());
        gestureDetector.setOnDoubleTapListener(this);
        keyState = KeyboardKeyState.getInstance();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getKeyboard() == null) {
            return;
        }
        if (keyState.isNumberInputType()) {
            new NumberKeyboardView(getContext(), canvas, getKeyboard()).onDraw();
            return;
        }
        if (keyState.isLanguageChange() || isSymbolic()) {
            new EnglishKeyboardView(getContext(), canvas, getKeyboard()).onDraw();
            return;
        }
        if (keyState.isLanguageChange() && Utils.isLandscape(getContext())) {
            new EnglishKeyboardView(getContext(), canvas, getKeyboard()).onDraw();
            return;
        }
        new TamilKeyboardView(getContext(), getKeyboard(), canvas).onDraw();

    }

    private boolean isSymbolic() {
        return keyState.getmKeyboardState() == R.integer.keyboard_symbol;
    }

    public int determineTextColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return getResources().getColor(R.color.dark_number_key);
        } else {
            return getResources().getColor(R.color.light_number_key);
        }
    }

    public int determineKeyPreviewColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return R.drawable.dark_key_preview;
        } else {
            return R.drawable.light_key_preview;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float touchX = event.getX();
        float touchY = event.getY();

        Keyboard.Key key = getKeyFromCoordinates(touchX, touchY);
        if (key == null) {
            return false; // Early exit if no key is found
        }

        int keyCode = key.codes[0]; // Cache the key code
        Keyboard.Key cachedKey = getKeyForCode(keyCode);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                keyPressStartTime = System.currentTimeMillis();
                isKeyHeld = false;

                if (cachedKey != null) {
                    if (sharedPreferences.getBoolean("key_preview", true)) {
                        keyPopUpController.showKeyPreview(cachedKey);
                    }
                    keyToneController.play(cachedKey.codes[0]);
                }
                keyState.setHoverKey(keyCode);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isKeyHeld) {
                    handler.postDelayed(() -> keyState.setKeyPressHold(false), 100);
                }
                keyPopUpController.dismissKeyPreview();
                keyState.setHoverKey(-3);
                break;
            case MotionEvent.ACTION_MOVE:
                long currentTime = System.currentTimeMillis();
                if (!isKeyHeld && currentTime - keyPressStartTime > KEY_HOLD_THRESHOLD) {
                    isKeyHeld = true;
                    keyPopUpController.holdKeyPreview(cachedKey);
                    if (sharedPreferences.getBoolean("key_preview", true)) {
                        keyPopUpController.showHoldKeyPreview(cachedKey);
                    }
                }
                break;
        }
        keyState.setKeyIndex(getKeyIndex(touchX, touchY));
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private Keyboard.Key getKeyForCode(int primaryCode) {
        for (Keyboard.Key key : getKeyboard().getKeys()) {
            if (key.codes[0] == primaryCode) {
                return key;
            }
        }
        return null;
    }

    private List<Keyboard.Key> cachedKeys;

    private List<Keyboard.Key> getCachedKeys() {
        if (cachedKeys == null || cachedKeys.size() != getKeyboard().getKeys().size()) {
            cachedKeys = getKeyboard().getKeys();
        }
        return cachedKeys;
    }

    private Keyboard.Key getKeyFromCoordinates(float x, float y) {
        for (Keyboard.Key key : getKeyboard().getKeys()) {
            if (key.isInside((int) x, (int) y)) {
                return key;
            }
        }
        return null;
    }

    private int getKeyIndex(float touchX, float touchY) {
        for (int i = 0; i < getCachedKeys().size(); i++) {
            Keyboard.Key key = cachedKeys.get(i);
            if (touchX >= key.x && touchX <= key.x + key.width &&
                    touchY >= key.y && touchY <= key.y + key.height) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent motionEvent) {
        if (keyState.getKeyIndex() == 19) {
            keyState.setDoublePress(!keyState.isDoublePress());
            return true;
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(@NonNull MotionEvent motionEvent) {
        return false;
    }
}
