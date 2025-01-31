package com.example.customkeyboard.controler;

import static com.example.customkeyboard.utils.Utils.dpToPx;
import static com.example.customkeyboard.utils.Utils.isPortrait;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import com.example.customkeyboard.R;
import com.example.customkeyboard.model.KeyboardKeyState;
import com.example.customkeyboard.view.CustomKeyboardView;

public class KeyPopUpController {
    KeyboardKeyState keyState = KeyboardKeyState.getInstance();
    Context context;
    FrameLayout frameLayout;
    TextView keyPreviewText;
    PopupWindow keyPreviewPopup;
    View keyPreviewView;
    CustomKeyboardView keyboardLayoutView;
    SharedPreferences sharedPreferences;
    Handler handler = new Handler(Looper.getMainLooper());

    public KeyPopUpController(Context context, CustomKeyboardView keyboardLayoutView, SharedPreferences sharedPreferences) {
        this.context = context;
        this.keyboardLayoutView = keyboardLayoutView;
        this.sharedPreferences = sharedPreferences;
        initializeKeyPreview();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initializeKeyPreview() {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        keyPreviewView = layoutInflater.inflate(R.layout.key_preview, null);
        frameLayout = keyPreviewView.findViewById(R.id.key_preview_frame);
        keyPreviewText = keyPreviewView.findViewById(R.id.preview_text);

        keyPreviewPopup = new PopupWindow(keyPreviewView, dpToPx(55, context), dpToPx(65, context));
        keyPreviewPopup.setElevation(5);
        keyPreviewPopup.setBackgroundDrawable(null);
        keyPreviewPopup.setFocusable(false);
        keyPreviewPopup.setTouchable(false);
        keyPreviewPopup.setOutsideTouchable(false);
        keyPreviewPopup.setClippingEnabled(false);
    }

    public void showKeyPreview(Keyboard.Key key) {
        if (keyPreviewPopup == null || key == null || keyboardLayoutView == null || key.codes[0] == 32 || keyState.getKeyIndex() == -1 || keyState.getKeyIndex() == -10 || key.codes[0] == -48 || key.codes[0] == 0 || key.codes[0] == -4 ||
                key.codes[0] == -5 || key.codes[0] == -7 || key.codes[0] == -2 || key.codes[0] == Keyboard.KEYCODE_SHIFT)
            return;

        updateTheme();
        String previewText = key.label != null ? key.label.toString() : String.valueOf((char) key.codes[0]);
        keyPreviewText.setText(validateLabel(previewText));
        int[] keyboardLocation = new int[2];
        keyboardLayoutView.getLocationOnScreen(keyboardLocation);

        int popupX = keyboardLocation[0] + key.x + key.width / 2 - keyPreviewPopup.getWidth() / 2;
        if (!keyPreviewPopup.isShowing()) {
            keyPreviewPopup.showAtLocation(keyboardLayoutView, Gravity.NO_GRAVITY, popupX, getY(key));
        } else {
            keyPreviewPopup.update(popupX, getY(key), keyPreviewPopup.getWidth(), keyPreviewPopup.getHeight());
        }
    }

    private Integer getY(Keyboard.Key key) {
        if (KeyboardKeyState.getInstance().isNumberInputType()) {
            return key.y - key.height;
        } else if (KeyboardKeyState.getInstance().isEmojiSearchFocus()) {
            return key.y - key.height / 2 + (int) context.getResources().getDimension(R.dimen.key_pop_up_Y);
        }
        return key.y - key.height / 2;
    }

    public void showHoldKeyPreview(Keyboard.Key key) {
        if (keyPreviewPopup == null || key == null || keyboardLayoutView == null || key.codes[0] == 32 || keyState.getKeyIndex() == -1 || keyState.getKeyIndex() == -10 || key.codes[0] == -48 || key.codes[0] == 0 || key.codes[0] == -4 ||
                key.codes[0] == -5 || key.codes[0] == -7 || key.codes[0] == -2 || key.codes[0] == Keyboard.KEYCODE_SHIFT || key.label.length() <= 1)
            return;
        keyPreviewText.setText(key.label.toString().substring(1));
        int[] keyboardLocation = new int[2];
        keyboardLayoutView.getLocationOnScreen(keyboardLocation);
        int popupX = keyboardLocation[0] + key.x + key.width / 2 - keyPreviewPopup.getWidth() / 2;
        if (keyPreviewPopup.isShowing()) {
            keyPreviewPopup.update(popupX, getY(key), keyPreviewPopup.getWidth(), keyPreviewPopup.getHeight());
        }
    }

    private String validateLabel(String label) {
        if (KeyboardKeyState.getInstance().isLanguageChange() && KeyboardKeyState.getInstance().isShifted()) {
            return label.substring(0, 1).toUpperCase();
        }
        return !KeyboardKeyState.getInstance().isLanguageChange() && isPortrait(context) && !KeyboardKeyState.getInstance().isNumberInputType() ? label : label.substring(0, 1);
    }

    public void updateTheme() {
        frameLayout.setBackground(AppCompatResources.getDrawable(context, keyboardLayoutView.determineKeyPreviewColor()));
        keyPreviewText.setTextColor(keyboardLayoutView.determineTextColor());
    }

    public void dismissKeyPreview() {
        if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
            keyPreviewPopup.dismiss();
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void holdKeyPreview(Keyboard.Key key) {
        if (key != null && key.label != null && key.label.length() == 2) {
            commitAndPreview(key);
        } else if (key != null && key.label != null && key.label.length() > 2 && key.label.toString().charAt(0) == 'ஂ') {
            commitAndPreview(key);
        } else if (key != null && key.label != null && key.label.length() > 2 && key.label.toString().charAt(0) == 'ா') {
            commitAndPreview(key);
            KeyboardKeyState.getInstance().setPressedKey(-3);
        }
    }

    private void commitAndPreview(Keyboard.Key key) {
        System.out.println(KeyboardKeyState.getInstance().getPressedKey());
        KeyboardKeyState.getInstance().setKeyPressHold(true);
        if (KeyboardKeyState.getInstance().isLanguageChange()) {
            KeyboardKeyState.getInstance().getInputConnection().commitText(key.label.toString().substring(1), 1);
        } else {
            String label = key.label.toString().substring(1);
            if (key.label.toString().substring(1).equals("ஸ்ரீ")) {
                label = "ஶ்ரீ";
            }
            KeyboardKeyState.getInstance().getInputConnection().commitText(label, 1);
            if (KeyboardKeyState.getInstance().getPressedKey() == -3 || key.label.toString().charAt(0) == 'ா' || key.label.toString().substring(1).equals("ஃ"))
                return;
            KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(KeyboardKeyState.getInstance().getPressedKey())), 1);
            KeyboardKeyState.getInstance().setPressedKey(-3);
        }
    }
}
