package com.example.customkeyboard.controler;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputMethodManager;

import com.example.customkeyboard.model.KeyboardKeyState;

public class KeyController {
    InputMethodManager inputMethodManager;
    Handler handler = new Handler(Looper.getMainLooper());

    public KeyController(Context context) {
        inputMethodManager = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
    }

    public void autoCapitalization() {
        if (KeyboardKeyState.getInstance().isCapsLock()) return;
        ExtractedText extractedText = KeyboardKeyState.getInstance().getInputConnection().getExtractedText(new ExtractedTextRequest(), 0);
        if (KeyboardKeyState.getInstance().isLanguageChange() && extractedText != null && extractedText.text != null && !KeyboardKeyState.getInstance().isSearchInput() && !KeyboardKeyState.getInstance().isEmojiSearchFocus() && !KeyboardKeyState.getInstance().isURLInput() && !KeyboardKeyState.getInstance().isEmailUrlInput() && !KeyboardKeyState.getInstance().isEmailInput()) {
            CharSequence sequence = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(2, 0);
            assert sequence != null;
            KeyboardKeyState.getInstance().setShifted(sequence.equals("") || sequence.equals(". ") || sequence.toString().endsWith("\n") || validateText(extractedText.text.toString()));
        }
    }

    public void validateKeyboardShift() {
        if (KeyboardKeyState.getInstance().isCapsLock()) return;
        ExtractedText extractedText = KeyboardKeyState.getInstance().getInputConnection().getExtractedText(new ExtractedTextRequest(), 0);
        if (KeyboardKeyState.getInstance().isLanguageChange() && extractedText != null && extractedText.text != null && !KeyboardKeyState.getInstance().isSearchInput() && !KeyboardKeyState.getInstance().isEmojiSearchFocus() && !KeyboardKeyState.getInstance().isURLInput() && !KeyboardKeyState.getInstance().isEmailUrlInput() && !KeyboardKeyState.getInstance().isEmailInput()) {
            KeyboardKeyState.getInstance().setShifted(extractedText.text.length() <= 0 || validateText(extractedText.text.toString()));
        } else {
            KeyboardKeyState.getInstance().setShifted(false);
        }
    }

    private boolean validateText(String text) {
        if (text.length() == 1 && Character.codePointAt(text, 0) == 10) return true;
        if (text.length() > 1) {
            return false;
        }
        return Character.codePointAt(text, 0) == 8203;
    }

    public void enableLongPressShift(int code) {
        if (code == Keyboard.KEYCODE_SHIFT) {
            handler.postDelayed(() -> {
                KeyboardKeyState.getInstance().setShiftLongPressed(true);
                KeyboardKeyState.getInstance().setCapsLock(true);
            }, 1000);
        }
    }

    public void openInputMethodManager(int code) {
        if (code == 32) {
            handler.postDelayed(() -> {
                inputMethodManager.showInputMethodPicker();
            }, 500);
        }
    }

    public void dismissLongShiftPress(int code) {
        if (code == Keyboard.KEYCODE_SHIFT) {
            handler.removeCallbacksAndMessages(null);
            KeyboardKeyState.getInstance().setShiftLongPressed(false);
        }
    }

    public void dismissMethodPicker(int code) {
        if (code == 32) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void hoverKey(int code) {
        if (code != -48) {
            KeyboardKeyState.getInstance().setHoverKey(code);
        }
    }

    public void dismissHoverKey() {
        KeyboardKeyState.getInstance().setHoverKey(-3);
    }

}
