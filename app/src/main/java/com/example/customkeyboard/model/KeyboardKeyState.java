package com.example.customkeyboard.model;

import android.view.inputmethod.InputConnection;

public class KeyboardKeyState {

    private static KeyboardKeyState instance;
    private int pressedKey = -3;
    private int keyIndex = -10;
    private int hoverKey = -3;
    private int keycode = 0;
    private int mKeyboardState = 0;
    private boolean isShifted = false;
    private boolean isCapsLock = false;
    private boolean isLanguageChange = false;
    private boolean isSearchInput = false;
    private boolean isDoublePress = false;
    private boolean isNumberInputType = false;
    private boolean isAutoCorrected = false;
    private boolean isShiftLongPressed = false;
    private boolean isKeyPressHold = false;
    private boolean isEmojiSearchFocus = false;
    private boolean isEmailInput = false;
    private boolean isURLInput = false;
    private boolean isEmailUrlInput = false;
    private boolean isVoicePermissionGranted = false;
    private String packageName = "";
    private InputConnection inputConnection;

    public KeyboardKeyState() {
    }

    public boolean isEmailUrlInput() {
        return isEmailUrlInput;
    }

    public void setEmailUrlInput(boolean emailUrlInput) {
        isEmailUrlInput = emailUrlInput;
    }

    public boolean isURLInput() {
        return isURLInput;
    }

    public void setURLInput(boolean URLInput) {
        isURLInput = URLInput;
    }

    public boolean isVoicePermissionGranted() {
        return isVoicePermissionGranted;
    }

    public void setVoicePermissionGranted(boolean voicePermissionGranted) {
        isVoicePermissionGranted = voicePermissionGranted;
    }

    public boolean isEmailInput() {
        return isEmailInput;
    }

    public void setEmailInput(boolean emailInput) {
        isEmailInput = emailInput;
    }

    public boolean isEmojiSearchFocus() {
        return isEmojiSearchFocus;
    }

    public void setEmojiSearchFocus(boolean emojiSearchFocus) {
        isEmojiSearchFocus = emojiSearchFocus;
    }

    public boolean isKeyPressHold() {
        return isKeyPressHold;
    }

    public void setKeyPressHold(boolean keyHold) {
        isKeyPressHold = keyHold;
    }

    public InputConnection getInputConnection() {
        return inputConnection;
    }

    public void setInputConnection(InputConnection inputConnection) {
        this.inputConnection = inputConnection;
    }

    public boolean isShiftLongPressed() {
        return isShiftLongPressed;
    }

    public void setShiftLongPressed(boolean shiftLongPressed) {
        isShiftLongPressed = shiftLongPressed;
    }

    public boolean isAutoCorrected() {
        return isAutoCorrected;
    }

    public void setAutoCorrected(boolean autoCorrected) {
        isAutoCorrected = autoCorrected;
    }

    public static synchronized KeyboardKeyState getInstance() {
        if (instance == null) {
            instance = new KeyboardKeyState();
        }
        return instance;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public boolean isNumberInputType() {
        return isNumberInputType;
    }

    public void setNumberInputType(boolean numberInputType) {
        isNumberInputType = numberInputType;
    }

    public int getPressedKey() {
        return pressedKey;
    }

    public void setPressedKey(int pressedKey) {
        this.pressedKey = pressedKey;
    }

    public int getKeyIndex() {
        return keyIndex;
    }

    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }

    public int getHoverKey() {
        return hoverKey;
    }

    public void setHoverKey(int hoverKey) {
        this.hoverKey = hoverKey;
    }

    public int getKeycode() {
        return keycode;
    }

    public void setKeycode(int keycode) {
        this.keycode = keycode;
    }

    public int getmKeyboardState() {
        return mKeyboardState;
    }

    public void setmKeyboardState(int mKeyboardState) {
        this.mKeyboardState = mKeyboardState;
    }

    public boolean isShifted() {
        return isShifted;
    }

    public void setShifted(boolean shifted) {
        isShifted = shifted;
    }

    public boolean isCapsLock() {
        return isCapsLock;
    }

    public void setCapsLock(boolean capsLock) {
        isCapsLock = capsLock;
    }

    public boolean isSearchInput() {
        return isSearchInput;
    }

    public void setSearchInput(boolean searchInput) {
        isSearchInput = searchInput;
    }

    public boolean isDoublePress() {
        return isDoublePress;
    }

    public void setDoublePress(boolean doublePress) {
        isDoublePress = doublePress;
    }

    public boolean isLanguageChange() {
        return isLanguageChange;
    }

    public void setLanguageChange(boolean languageChange) {
        isLanguageChange = languageChange;
    }
}
