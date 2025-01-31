package com.example.customkeyboard.controler;

import static com.example.customkeyboard.utils.Utils.containsEmoji;
import static com.example.customkeyboard.utils.Utils.specialCharacters;
import static com.example.customkeyboard.utils.Utils.specialLetter;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.ExtractedTextRequest;

import com.example.customkeyboard.model.KeyboardKeyState;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import timber.log.Timber;

public class InputMethodController {
    Dialog dialog;
    SharedPreferences sharedPreferences;
    KeyController keyController;
    List<String> english;
    List<String> tamil;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Objects.requireNonNull(Looper.getMainLooper()));

    public InputMethodController(KeyController keyController, List<String> english, List<String> tamil, Context context, Dialog dialog) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.keyController = keyController;
        this.english = english;
        this.tamil = tamil;
    }

    public void validateURLAndEmailInput(char primaryCode) {
        if (KeyboardKeyState.getInstance().isURLInput()) {
            KeyboardKeyState.getInstance().getInputConnection().commitText("/", 1);
        } else if (KeyboardKeyState.getInstance().isEmailUrlInput()) {
            KeyboardKeyState.getInstance().getInputConnection().commitText("@", 1);
        } else {
            KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(primaryCode), 1);
        }
    }

    public void validateFirstCharacter(char code) {
        if (KeyboardKeyState.getInstance().isKeyPressHold()) return;
        if (KeyboardKeyState.getInstance().getPressedKey() >= 3014 && KeyboardKeyState.getInstance().getPressedKey() <= 3016) {
            KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(code), 1);
            KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(KeyboardKeyState.getInstance().getPressedKey())), 1);
        } else {
            KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(code), 1);
        }
    }

    public void validateSecondCharacter(int code, onCallback onCallback) {
        try {
            if (KeyboardKeyState.getInstance().isKeyPressHold()) return;
            CharSequence lastCharaSequence = !KeyboardKeyState.getInstance().getInputConnection().getExtractedText(new ExtractedTextRequest(), 0).text.toString().trim().isEmpty() ? KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(1, 0) : null;
            CharSequence lastTwoCharaSequence = KeyboardKeyState.getInstance().getInputConnection().getExtractedText(new ExtractedTextRequest(), 0).text.length() >= 2 ? KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(2, 0) : null;
            int point = 0;
            if (lastCharaSequence == null) {
                return;
            }
            int i = Character.codePointAt(lastCharaSequence, 0);
            if (lastTwoCharaSequence != null && lastTwoCharaSequence.length() >= 2) {
                point = Character.codePointAt(lastTwoCharaSequence, 1);
            }

            if (i >= 3006 && i <= 3010 || i > 8000) return;
            if (point >= 3014 && point <= 3016) {
                if (point == 3014 && code == 3006 || point == 3015 && code == 3006) {
                    KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(code)), 1);
                    onCallback.onSuggest();
                }
                return;
            }

            if (i >= 56447) {
                return;
            }

            if (i >= 2964 && i != 8203 && i != 3021) {
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(code)), 1);
                onCallback.onSuggest();
            }

            if (lastCharaSequence.toString().equals("ௌ") && lastTwoCharaSequence != null) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(3, 0);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(Character.codePointAt(lastTwoCharaSequence, 0))), 1);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(3014)), 1);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(2995)), 1);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(code)), 1);
                onCallback.onSuggest();
            }

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().log("validateSecondCharacter :" + e.getMessage());
            Timber.tag("validateSecondCharacter Exception").i(Objects.requireNonNull(e.getMessage()));
        }
    }

    public void validateCharacter(int currentChar) {
        CharSequence sequence = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(2, 0);
        CharSequence firstSequence = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(1, 0);
        if (sequence == null) return;
        if (firstSequence == null) return;
        if (firstSequence.length() != 0) {
            int firstSequenceCode = Character.codePointAt(sequence, 0);
            if (firstSequenceCode == 2962 && currentChar == 2995) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(2, 0);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(2964)), 1);
            }
        }
        if (sequence.length() > 0) {
            int sequenceCode = Character.codePointAt(sequence, 0);
            if (sequenceCode == 3014 && currentChar == 2995) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(2, 0);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(3020)), 1);
            }
        }
    }

    public void autoCorrection(String word, List<String> tamil, List<String> english) {
        KeyboardKeyState.getInstance().setAutoCorrected(true);
        final int[] max_length = {KeyboardKeyState.getInstance().isLanguageChange() ? 4 : 2};

        if (word != null && word.trim().length() >= max_length[0]) {
            AtomicReference<List<String>> stringStream = new AtomicReference<>();
            executorService.execute(() -> {
                final String[] lastLine = new String[1];
                if (KeyboardKeyState.getInstance().isLanguageChange()) {
                    stringStream.set(english.stream().filter(string -> string.toLowerCase().startsWith(word.toLowerCase())).collect(Collectors.toList()));
                } else {
                    stringStream.set(tamil.stream().filter(string -> string.startsWith(word)).collect(Collectors.toList()));
                }

                lastLine[0] = !stringStream.get().isEmpty() ? stringStream.get().get(0) : null;
                CharSequence currentText = KeyboardKeyState.getInstance().getInputConnection().getExtractedText(new ExtractedTextRequest(), 0).text;
                CharSequence before = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(currentText.length(), 0);
                mainHandler.post(() -> {
                    if (before != null && lastLine[0] != null) {
                        if (Character.isUpperCase(word.charAt(0)) && KeyboardKeyState.getInstance().isLanguageChange()) {
                            lastLine[0] = lastLine[0].substring(0, 1).toUpperCase() + lastLine[0].substring(1);
                        }
                        KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(word.trim().length() + 1, 0);
                        KeyboardKeyState.getInstance().getInputConnection().commitText(lastLine[0] + " ", 1);
                    }
                });
            });
        }
    }

    public void deleteLastEmojiOrCharacter() {
        try {
            CharSequence lastCharacter = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(1, 0);
            CharSequence beforeCursor = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(2, 0);
            CharSequence threeBeforeCursor = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(3, 0);
            CharSequence fourBeforeCursor = KeyboardKeyState.getInstance().getInputConnection().getTextBeforeCursor(4, 0);
            CharSequence selectedText = KeyboardKeyState.getInstance().getInputConnection().getSelectedText(0);

            if (!TextUtils.isEmpty(selectedText)) {
                KeyboardKeyState.getInstance().getInputConnection().commitText("", 1);
                return;
            }

            if (lastCharacter == null || beforeCursor == null || beforeCursor.length() == 0 || threeBeforeCursor == null || fourBeforeCursor == null) {
                return;
            }

            if (containsEmoji(beforeCursor.toString())) {
                KeyboardKeyState.getInstance().getInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                KeyboardKeyState.getInstance().getInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
                return;
            }
            if (threeBeforeCursor.toString().equals("க்ஷ")) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(3, 0);
            } else if (hasSpecialLetter(fourBeforeCursor) || fourBeforeCursor.toString().equals("ஶ்ரீ")) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(4, 0);
            } else if (hasSpecialCharacters(lastCharacter)) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(2, 0);
            } else if (lastCharacter.toString().equals("ௌ")) {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(2, 0);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(Character.codePointAt(beforeCursor, 0))), 1);
                KeyboardKeyState.getInstance().getInputConnection().commitText(String.valueOf(Character.toChars(3014)), 1);
            } else {
                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(1, 0);
            }

//            if (hasSpecialLetter(fourBeforeCursor) || fourBeforeCursor.toString().equals("ஶ்ரீ")) {
//                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(4, 0);
//            } else if (hasSpecialLetter(fiveBeforeCursor)) {
//                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(5, 0);
//            } else if (hasSpecialCharacters(String.valueOf(beforeCursor.charAt(0))) && hasSpecialCharacters(String.valueOf(beforeCursor.charAt(1))) || threeBeforeCursor.toString().equals("க்ஷ")) {
//                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(3, 0);
//            } else if (hasSpecialCharacters(lastCharacter)) {
//                KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(2, 0);
//            } else {
//                if (point == 56447) {
//                    KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(14, 0);
//                } else {
//                    KeyboardKeyState.getInstance().getInputConnection().deleteSurroundingText(1, 0);
//                }
//            }
            keyController.validateKeyboardShift();
        } catch (
                Exception e) {
            FirebaseCrashlytics.getInstance().log(Objects.requireNonNull(e.getMessage()));
            Timber.tag("DeleteLastEmojiOrCharacter Exception").i(Objects.requireNonNull(e.getMessage()));
        }
    }

    private boolean hasSpecialCharacters(CharSequence lastCharacter) {
        return specialCharacters.stream().anyMatch(character -> character.toString().equals(lastCharacter.toString()));
    }

    private boolean hasSpecialLetter(CharSequence lastCharacter) {
        return specialLetter.stream().anyMatch(character -> {
            assert lastCharacter != null;
            return character.equals(lastCharacter.toString());
        });
    }

    @SuppressLint("CommitPrefEdits")
    public void cacheCurrentKeyBoard(boolean isLanguageChanged) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLanguageChanged", isLanguageChanged);
        editor.apply();
    }

    public boolean getCacheCurrentKeyBoard() {
        return sharedPreferences.getBoolean("isLanguageChanged", false);
    }

    public interface onCallback {
        void onSuggest();
    }
}
