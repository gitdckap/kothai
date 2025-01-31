package com.example.customkeyboard.service;

import static com.example.customkeyboard.utils.Utils.browserList;
import static com.example.customkeyboard.utils.Utils.cachedEnglishCsv;
import static com.example.customkeyboard.utils.Utils.cachedTamilCsv;
import static com.example.customkeyboard.utils.Utils.clearGIF;
import static com.example.customkeyboard.utils.Utils.dpToPx;
import static com.example.customkeyboard.utils.Utils.initializeCSV;
import static com.example.customkeyboard.utils.Utils.initializeEnglishCSV;
import static com.example.customkeyboard.utils.Utils.isLandscape;
import static com.example.customkeyboard.utils.Utils.isLongPress;
import static com.example.customkeyboard.utils.Utils.isPortrait;
import static com.example.customkeyboard.utils.Utils.isSelectAll;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CursorAnchorInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.customkeyboard.R;
import com.example.customkeyboard.activities.CustomKeyboardSettingsActivity;
import com.example.customkeyboard.component.ClipboardView;
import com.example.customkeyboard.component.EmojiPickerView;
import com.example.customkeyboard.controler.FirebaseAnalyticsController;
import com.example.customkeyboard.controler.InputMethodController;
import com.example.customkeyboard.controler.KeyController;
import com.example.customkeyboard.database.DBHelper;
import com.example.customkeyboard.databinding.KeyboardViewBinding;
import com.example.customkeyboard.model.KeyboardKeyState;
import com.example.customkeyboard.utils.Utils;
import com.example.customkeyboard.viewmodel.EmojiViewModel;
import com.example.customkeyboard.viewmodel.VoiceSpeechTextViewModel;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import timber.log.Timber;

public class MyInputMethodService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {
    EmojiViewModel emojiViewModel;
    Keyboard firstTamilKeyboardLayout, secondTamilKeyboardLayout, symbolKeyBoard, englishKeyboard, secondSymbolKeyBoard, numberKeyboard, landscapeEnglishKeyboard, landscapeTamilKeyboard, landscapeSymbolicKeyboard, landscapeSymbolicSecondKeyboard;
    private int mKeyboardState = R.integer.keyboard_normal;
    int position = 0;
    boolean isBrowser = false;
    boolean isAlt = false;
    String word;

    int textLength;
    SharedPreferences sharedPreferences;

    DBHelper dbHelper;
    String clippedText = "";

    private Handler handler;
    private Runnable deleteRunnable;
    private boolean isDeleting = false;

    boolean isDarkMode = false;
    InputMethodManager inputMethodManager;
    int inputType = 0, isNumberInputType = 0;

    List<String> csv = new ArrayList<>();
    List<String> english_csv = new ArrayList<>();

    InputConnection inputConnection = getCurrentInputConnection();

    String tempTranscript = "";

    VoiceSpeechTextViewModel voiceSpeechTextViewModel;

    KeyboardViewBinding binding;
    KeyboardKeyState keyState;
    InputMethodController controller;
    KeyController keyController;
    FirebaseAnalyticsController analyticsController;
    AnimatorSet animatorSet = new AnimatorSet();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        initializeKeyboardsAndView();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        voiceSpeechTextViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(VoiceSpeechTextViewModel.class);
        keyState = KeyboardKeyState.getInstance();
        voiceSpeechTextViewModel.initializeSpeechClient(this);
        voiceSpeechTextViewModel.getVoiceStatus().observeForever(this::validateUIWithVoiceStatus);
        voiceSpeechTextViewModel.getTranscript().observeForever(this::commitVoiceText);

        emojiViewModel = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(EmojiViewModel.class);
        emojiViewModel.getSuggestEmoji().observeForever(this::replaceShortcutWithEmoji);
        keyController = new KeyController(getApplicationContext());
        controller = new InputMethodController(keyController, cachedEnglishCsv, cachedTamilCsv, getApplicationContext(), getWindow());
        keyState.setInputConnection(inputConnection);
        analyticsController = new FirebaseAnalyticsController(getApplicationContext());
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateInputView() {
        initializeKeyboardsAndView();
        setScaleAnimation();

        analyticsController.logScreenView("Keyboard", "InputMethodService");

        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        dbHelper = new DBHelper(this);

        csv = initializeCSV(this);
        english_csv = initializeEnglishCSV(this);

        binding.keyboardLayoutView.setPreviewEnabled(false);

        binding.voiceButton.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                if (binding.voiceStatus.getVisibility() == View.VISIBLE) {
                    resetVoice();
                } else {
                    binding.voiceButtonLayout.setBackground(AppCompatResources.getDrawable(this, R.drawable.circle_background));
                    binding.voiceButton.setColorFilter(getColor(R.color.white));
                    animatorSet.start();
                }
            }
            voiceSpeechTextViewModel.checkPermission(keyState.isLanguageChange());
        });

        binding.emojiPicker.setOnEmojiPickedListener(new EmojiPickerView.OnEmojiClickListener() {
            @Override
            public void onEmojiClick(String value) {
                getCurrentInputConnection().commitText(value, 1);
            }

            @Override
            public void onDeleteClick(MotionEvent motionEvent) {
                cleanButtonAction(motionEvent);
            }

            @Override
            public void onSwitchKeyboard() {
                switchingEmojiToKeyboard();
            }

            @Override
            public void onKeyboardView(InputConnection newConnection) {
                if (newConnection == null) {
                    inputConnection = getCurrentInputConnection();
                    KeyboardKeyState.getInstance().setInputConnection(getCurrentInputConnection());
                    binding.keyboardLayoutView.setVisibility(View.GONE);
                    keyState.setSearchInput(false);
                    binding.emojiPicker.setHeight(binding.keyboardLayoutView.getKeyboard().getHeight() + dpToPx(42, getApplicationContext()));
                } else {
                    inputConnection = newConnection;
                    KeyboardKeyState.getInstance().setInputConnection(newConnection);
                    binding.keyboardLayoutView.setVisibility(View.VISIBLE);
                    keyController.validateKeyboardShift();
                    keyState.setSearchInput(true);
                }
                applyTheme();
            }
        });

        binding.clipboardView.setonClipboardListner(new ClipboardView.onClipboardListner() {
            @Override
            public void onClear(MotionEvent motionEvent) {
                cleanButtonAction(motionEvent);
            }

            @Override
            public void onSwitchKeyboard() {
                switchingClipboardToKeyboard();
            }
        });

        binding.menuView.setOnMenuViewClickListner(this::clipboardViewValidation);

        binding.modeButton.setOnClickListener(view -> {
            isDarkMode = !isDarkMode;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isModeSelected", true);
            editor.putBoolean("isDarkMode", isDarkMode);
            editor.apply();
            applyTheme();
        });

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        clipboard.addPrimaryClipChangedListener(() -> {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence pasteData = clip.getItemAt(0).getText();
                if (!clippedText.equals(pasteData.toString())) {
                    dbHelper.insertClipboard(String.valueOf(pasteData));
                    clippedText = String.valueOf(pasteData);
                }

                binding.emojiButtonLayout.setVisibility(View.GONE);
                binding.gifButtonLayout.setVisibility(View.GONE);
                binding.settingsButtonLayout.setVisibility(View.GONE);
                binding.statusModeButtonLayout.setVisibility(View.GONE);
                binding.clipboardButtonLayout.setVisibility(View.VISIBLE);
                binding.clipBar.setText(pasteData);
                binding.clipboardView.refreshClipboard();
            }
        });
        binding.clipboardButtonLayout.setOnClickListener(view -> {
            if (!binding.clipBar.getText().toString().isEmpty()) {
                inputConnection.commitText(binding.clipBar.getText(), 1);
                resetClipLayout();
            }
        });

        binding.keyboardLayoutView.setOnKeyboardActionListener(this);

        handler = new Handler(Looper.getMainLooper());
        deleteRunnable = new Runnable() {
            @Override
            public void run() {
                controller.deleteLastEmojiOrCharacter();
                if (isDeleting) {
                    handler.postDelayed(this, 50); // Adjust delay as needed
                }
            }
        };

        binding.menuButton.setOnClickListener(view -> {
            analyticsController.logScreenView("MenuView", "InputMethodService");
            menuViewValidation();
            resetClipLayout();
        });

        binding.emojiButton.setOnClickListener(view -> {
            analyticsController.logScreenView("Emoji Picker", "InputMethodService");
            emojiViewValidation();
        });

        binding.gifButton.setOnClickListener(view -> {
            analyticsController.logScreenView("Gif Picker", "InputMethodService");
            gifViewValidation();
        });

        binding.settingsButton.setOnClickListener((view) -> {
            analyticsController.logScreenView("Keyboard Settings", "InputMethodService");
            Intent intent = new Intent(this, CustomKeyboardSettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        firstTimeModeSelect();
        validateStartInput();
        disableCapitalization();
        keyController.validateKeyboardShift();
        applyTheme();

        return binding.getRoot();
    }

    private void resetClipLayout() {
        binding.clipBar.setBackground(null);
        binding.clipBar.setText("");
        binding.clipboardButtonLayout.setVisibility(View.GONE);
        binding.emojiButtonLayout.setVisibility(View.VISIBLE);
        binding.gifButtonLayout.setVisibility(View.VISIBLE);
        binding.settingsButtonLayout.setVisibility(View.VISIBLE);
        binding.statusModeButtonLayout.setVisibility(View.VISIBLE);
    }

    private void setScaleAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.voiceButtonLayout, "scaleX", 0.6f, 1f);
        scaleX.setDuration(500);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.voiceButtonLayout, "scaleY", 0.6f, 1f);
        scaleY.setDuration(500);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator alpha = ObjectAnimator.ofFloat(binding.voiceButtonLayout, "alpha", 1f, 0.5f);
        alpha.setDuration(500);
        alpha.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatMode(ValueAnimator.REVERSE);

        animatorSet.playTogether(scaleX, scaleY, alpha);
    }

    private void initializeKeyboardsAndView() {
        binding = KeyboardViewBinding.inflate(getLayoutInflater());

        firstTamilKeyboardLayout = new Keyboard(this, R.xml.tamil_keyboard);
        secondTamilKeyboardLayout = new Keyboard(this, R.xml.tamil_keyboard_second);
        symbolKeyBoard = new Keyboard(this, R.xml.symbolic_keyboard);
        secondSymbolKeyBoard = new Keyboard(this, R.xml.symbolic_keyboard_second);
        englishKeyboard = new Keyboard(this, R.xml.english_keyboard);
        numberKeyboard = new Keyboard(this, R.xml.number_keyboard);
        landscapeEnglishKeyboard = new Keyboard(this, R.xml.landscape_english_keyboard);
        landscapeTamilKeyboard = new Keyboard(this, R.xml.landscape_tamil_keyboard);
        landscapeSymbolicKeyboard = new Keyboard(this, R.xml.landscape_symbolic_keyboard);
        landscapeSymbolicSecondKeyboard = new Keyboard(this, R.xml.landscape_symbolic_keyboard_second);
    }

    private void switchingMenuToKeyboard() {
        binding.keyboardLayoutView.setVisibility(View.VISIBLE);
        binding.menuView.setVisibility(View.GONE);
        validateStartInput();
        keyController.validateKeyboardShift();
        keyController.autoCapitalization();
        applyTheme();
    }

    private void switchingEmojiToKeyboard() {
        binding.keyboardLayoutView.setVisibility(View.VISIBLE);
        binding.emojiPicker.setVisibility(View.GONE);
        binding.statusBar.setVisibility(View.VISIBLE);
        validateStartInput();
        keyController.validateKeyboardShift();
        keyController.autoCapitalization();
        applyTheme();
    }

    private void switchingClipboardToKeyboard() {
        binding.keyboardLayoutView.setVisibility(View.VISIBLE);
        binding.clipboardView.setVisibility(View.GONE);
        binding.statusBar.setVisibility(View.VISIBLE);
        validateStartInput();
        keyController.validateKeyboardShift();
        keyController.autoCapitalization();
        applyTheme();
    }

    private void commitVoiceText(String transcript) {
        if (transcript.startsWith(tempTranscript)) {
            inputConnection.deleteSurroundingText(tempTranscript.length() + 1, 1);
            inputConnection.commitText(transcript + " ", 1);
        } else {
            inputConnection.commitText(transcript + " ", 1);
        }
        tempTranscript = transcript;
    }

    @SuppressLint("SetTextI18n")
    private void validateUIWithVoiceStatus(Utils.VoiceStatus status) {
        switch (status) {
            case IDLE:
                tempTranscript = "";
                binding.emojiButtonLayout.setVisibility(View.GONE);
                binding.gifButtonLayout.setVisibility(View.GONE);
                binding.settingsButtonLayout.setVisibility(View.GONE);
                binding.clipboardButtonLayout.setVisibility(View.GONE);
                binding.clipBar.setVisibility(View.GONE);
                binding.voiceStatus.setVisibility(View.VISIBLE);
                binding.voiceStatus.setText("Speak now");
                break;
            case LISTENING:
                binding.voiceStatus.setText("Listening now...");
                break;
            case WAIT:
                binding.voiceStatus.setText("Please wait");
                break;
            default:
                binding.emojiButtonLayout.setVisibility(View.VISIBLE);
                validateLayoutInSearchInput();
                binding.settingsButtonLayout.setVisibility(View.VISIBLE);
                binding.voiceStatus.setVisibility(View.GONE);
                resetVoice();
                break;
        }
    }

    private void resetVoice() {
        animatorSet.end();
        binding.voiceButtonLayout.setScaleX(1f);
        binding.voiceButtonLayout.setScaleY(1f);
        binding.voiceButtonLayout.setAlpha(1f);
        binding.voiceButtonLayout.setBackground(null);
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            binding.voiceButton.setColorFilter(getColor(R.color.dark_icon));
        } else {
            binding.voiceButton.setColorFilter(getColor(R.color.light_intro_icon_color));
        }
    }

    private void replaceShortcutWithEmoji(String emoji) {
        if (emoji != null) {
            inputConnection.deleteSurroundingText(word.length(), 0); // Remove the shortcut
            inputConnection.commitText(emoji, 1); // Commit the emoji
        }
    }

    private void validateStartInput() {
        keyState.setLanguageChange(controller.getCacheCurrentKeyBoard());
        keyState.setURLInput(inputType == 301989890 && isNumberInputType == 524305 || inputType == 318767106 && isNumberInputType == 524305 || inputType == 436207618 && isNumberInputType == 17);
        keyState.setEmailUrlInput(inputType == 301989890 && isNumberInputType == 209 || inputType == 318767106 && isNumberInputType == 209 || inputType == 436207621 && isNumberInputType == 33);
        keyState.setSearchInput(inputType == 318767107 || inputType == EditorInfo.IME_ACTION_SEARCH || inputType == 268435459 || inputType == 301989891 || inputType == 33554435 || inputType == 301989888 || inputType == 318767106);
        if (binding.clipboardButtonLayout.getVisibility() == View.VISIBLE) return;
        if ((isNumberInputType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
            binding.keyboardLayoutView.setKeyboard(numberKeyboard);
            keyState.setNumberInputType(true);
            binding.emojiButton.setVisibility(View.GONE);
            binding.gifButton.setVisibility(View.GONE);
            binding.statusBar.setVisibility(View.GONE);
        } else if (keyState.isLanguageChange()) {
            validateEnglishKeyboardOrientationWise();
            keyState.setNumberInputType(false);
            binding.emojiButton.setVisibility(View.VISIBLE);
            binding.gifButton.setVisibility(View.VISIBLE);
            binding.statusBar.setVisibility(View.VISIBLE);
        } else {
            validateTamilKeyboardOrientationWise();
            keyState.setNumberInputType(false);
            binding.emojiButton.setVisibility(View.VISIBLE);
            binding.gifButton.setVisibility(View.VISIBLE);
            binding.statusBar.setVisibility(View.VISIBLE);
        }
        validateLayoutInSearchInput();
    }

    public void validateLayoutInSearchInput() {
        if (keyState.isSearchInput() || keyState.isURLInput()) {
            binding.gifButtonLayout.setVisibility(View.GONE);
        } else {
            binding.gifButtonLayout.setVisibility(View.VISIBLE);
        }
    }

    private void validateEnglishKeyboardOrientationWise() {
        if (isLandscape(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(landscapeEnglishKeyboard);
        } else if (isPortrait(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(englishKeyboard);
        }
    }

    private void validateTamilKeyboardOrientationWise() {
        if (isLandscape(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(landscapeTamilKeyboard);
        } else if (isPortrait(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(getTamilKeyboardLayout());
        }
    }

    private void validateSymbolicKeyboardOrientationWise() {
        if (isLandscape(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(landscapeSymbolicKeyboard);
        } else if (isPortrait(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(symbolKeyBoard);
        }
    }

    private void validateSecondSymbolicKeyboardOrientationWise() {
        if (isLandscape(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(landscapeSymbolicSecondKeyboard);
        } else if (isPortrait(getApplicationContext())) {
            binding.keyboardLayoutView.setKeyboard(secondSymbolKeyBoard);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void validateModeButton() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            binding.modeButton.setBackground(getResources().getDrawable(R.drawable.ic_lightmode));
            binding.modeButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray200)));
        } else {
            binding.modeButton.setBackground(getResources().getDrawable(R.drawable.ic_darkmode));
            binding.modeButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark_mode_icon)));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void firstTimeModeSelect() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (!sharedPreferences.getBoolean("isModeSelected", false)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isDarkMode", nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
            editor.apply();
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                binding.modeButton.setBackground(getResources().getDrawable(R.drawable.ic_darkmode));
            } else {
                binding.modeButton.setBackground(getResources().getDrawable(R.drawable.ic_lightmode));
            }
        }
    }

    private void searchGIF() {
        binding.emojiPicker.searchGiphyByText();
    }

    @Override
    public boolean onShowInputRequested(int flags, boolean configChange) {
        if (keyState.isEmojiSearchFocus()) {
            binding.emojiPicker.switchToDefault();
            binding.keyboardLayoutView.setVisibility(View.VISIBLE);
            binding.emojiPicker.setVisibility(View.GONE);
            binding.statusBar.setVisibility(View.VISIBLE);
            keyController.autoCapitalization();
        }
        return super.onShowInputRequested(flags, configChange);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        keyState.setInputConnection(getCurrentInputConnection());
        validateStartInput();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        if (attribute.hintText != null && attribute.hintText.toString().toLowerCase().contains("captcha")) {
            keyState.setShifted(false);
        }

        clearGIF(this);
        inputType = attribute.imeOptions;
        isNumberInputType = attribute.inputType;

        inputConnection = getCurrentInputConnection();

        disableCapitalization();
        keyState.setInputConnection(inputConnection);
        keyState.setEmailInput(attribute.inputType == 442417);

        mKeyboardState = R.integer.keyboard_normal;

        keyState.setmKeyboardState(mKeyboardState);
        binding.keyboardLayoutView.setVisibility(View.VISIBLE);
        binding.suggestionScrollBar.setVisibility(View.GONE);
        binding.introBar.setVisibility(View.VISIBLE);
        keyState.setDoublePress(false);
        keyState.setCapsLock(false);

        isLongPress = false;
        isSelectAll = false;

        keyController.validateKeyboardShift();
        keyController.autoCapitalization();
        applyTheme();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        resetKeyboard();
    }

    @Override
    public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
        super.onStartInputView(editorInfo, restarting);
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            CharSequence beforeText = ic.getTextBeforeCursor(10, 0);
            CharSequence afterText = ic.getTextAfterCursor(10, 0);
            if ((beforeText != null && beforeText.toString().toLowerCase().contains("captcha")) ||
                    (afterText != null && afterText.toString().toLowerCase().contains("captcha"))) {
                keyState.setShifted(false);
            }
        }
        switchingEmojiToKeyboard();
        switchingClipboardToKeyboard();
        switchingMenuToKeyboard();
    }

    private void resetKeyboard() {
        binding.keyboardLayoutView.setVisibility(View.VISIBLE);
        binding.emojiPicker.setVisibility(View.GONE);
        binding.emojiPicker.setHeight(0);
        binding.statusBar.setVisibility(View.VISIBLE);
        isLongPress = false;
        isSelectAll = false;
    }

    private void cleanButtonAction(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDeleting = true;
                handler.post(deleteRunnable);
                return;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDeleting = false;
                handler.removeCallbacks(deleteRunnable);
        }
    }

    private void menuViewValidation() {
        if (binding.menuView.getVisibility() == View.VISIBLE) {
            switchingMenuToKeyboard();
        } else {
            binding.keyboardLayoutView.setVisibility(View.GONE);
            binding.menuView.setHeight(binding.keyboardLayoutView.getKeyboard().getHeight());
            binding.menuView.setVisibility(View.VISIBLE);
        }
    }

    private void clipboardViewValidation() {
        binding.keyboardLayoutView.setVisibility(View.GONE);
        binding.clipboardView.setHeight(binding.keyboardLayoutView.getKeyboard().getHeight() + dpToPx(42, getApplicationContext()));
        binding.clipboardView.setVisibility(View.VISIBLE);
        binding.statusBar.setVisibility(View.GONE);
        binding.menuView.setVisibility(View.GONE);
        binding.emojiPicker.openGIF();
    }

    private void gifViewValidation() {
        binding.keyboardLayoutView.setVisibility(View.GONE);
        binding.emojiPicker.setHeight(binding.keyboardLayoutView.getKeyboard().getHeight() + dpToPx(42, getApplicationContext()));
        binding.emojiPicker.setVisibility(View.VISIBLE);
        binding.statusBar.setVisibility(View.GONE);
        binding.menuView.setVisibility(View.GONE);
        binding.emojiPicker.openGIF();
    }

    @SuppressLint("NewApi")
    private void emojiViewValidation() {
        binding.keyboardLayoutView.setVisibility(View.GONE);
        binding.emojiPicker.setHeight(binding.keyboardLayoutView.getKeyboard().getHeight() + dpToPx(42, getApplicationContext()));
        binding.emojiPicker.setVisibility(View.VISIBLE);
        binding.statusBar.setVisibility(View.GONE);
        binding.menuView.setVisibility(View.GONE);
        binding.emojiPicker.openEmoji();
    }


    @Override
    public void onPress(int i) {
        keyController.enableLongPressShift(i);
        keyController.openInputMethodManager(i);
    }

    @Override
    public void onRelease(int i) {
        keyController.dismissLongShiftPress(i);
        keyController.dismissMethodPicker(i);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        voiceSpeechTextViewModel.stopVoiceRecorderByKeyPress();
        analyticsController.logKeyEvents(primaryCode);
        if (inputConnection == null) return;
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                analyticsController.logKeyPressEvent(Keyboard.KEYCODE_DELETE, "delete_key");
                controller.deleteLastEmojiOrCharacter();
                keyController.autoCapitalization();
                keyState.setHoverKey(Keyboard.KEYCODE_DELETE);
                break;
            case Keyboard.KEYCODE_MODE_CHANGE:
                isAlt = false;
                if (binding != null) {
                    if (mKeyboardState == R.integer.keyboard_normal) {
                        validateSymbolicKeyboardOrientationWise();
                        mKeyboardState = R.integer.keyboard_symbol;
                    } else if (keyState.isLanguageChange() && mKeyboardState == R.integer.keyboard_symbol) {
                        validateEnglishKeyboardOrientationWise();
                        mKeyboardState = R.integer.keyboard_normal;
                        analyticsController.logLanguageSwitch("tamil_keyboard", "english_keyboard");
                    } else {
                        validateTamilKeyboardOrientationWise();
                        mKeyboardState = R.integer.keyboard_normal;
                        analyticsController.logLanguageSwitch("english_keyboard", "tamil_keyboard");
                    }
                    keyState.setPressedKey(-3);
                }
                keyState.setmKeyboardState(mKeyboardState);
                break;
            case Keyboard.KEYCODE_DONE:
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                if (keyState.isNumberInputType()) return;
                analyticsController.logKeyPressEvent(Keyboard.KEYCODE_DELETE, "done_key");
                if (keyState.isEmojiSearchFocus()) searchGIF();
                if (keyState.isLanguageChange()) keyState.setShifted(true);
                if (keyState.getPackageName().contains("Slack")) {
                    inputConnection.commitText("\n", 1);
                    return;
                }
                break;
            case Keyboard.KEYCODE_SHIFT:
                if (!keyState.isDoublePress()) keyState.setShifted(!keyState.isShifted());
                if (keyState.isShiftLongPressed() || keyState.isDoublePress()) {
                    keyState.setCapsLock(true);
                    keyState.setShiftLongPressed(false);
                    keyState.setDoublePress(false);
                } else {
                    keyState.setCapsLock(false);
                    keyState.setShifted(keyState.isShifted());
                }
                break;
            case Keyboard.KEYCODE_ALT:
                if (!isAlt) {
                    isAlt = true;
                    validateSecondSymbolicKeyboardOrientationWise();
                } else {
                    validateSymbolicKeyboardOrientationWise();
                    isAlt = false;
                }
                break;
            case -7:
                changeKeyboardLanguageWise();
                keyController.validateKeyboardShift();
                keyController.autoCapitalization();
                keyState.setPressedKey(-3);
                controller.cacheCurrentKeyBoard(keyState.isLanguageChange());
                break;
            case -100: // Special code for 'ஸ்ரீ'
                inputConnection.commitText("ஶ்ரீ", 1);
                keyState.setPressedKey(-3);
                break;
            case -101:
                if (keyState.getPressedKey() >= 3014 && keyState.getPressedKey() <= 3016) {
                    inputConnection.commitText("க்ஷ", 1);
                    inputConnection.commitText(String.valueOf(Character.toChars(keyState.getPressedKey())), 1);
                    keyState.setPressedKey(-3);
                    return;
                }
                inputConnection.commitText("க்ஷ", 1);
                break;
            case 3014:
                if (keyState.getPressedKey() == 3014) {
                    keyState.setPressedKey(-3);
                    return;
                }
                keyState.setPressedKey(3014);
                break;
            case 3015:
                if (keyState.getPressedKey() == 3015) {
                    keyState.setPressedKey(-3);
                    return;
                }
                keyState.setPressedKey(3015);
                break;
            case 3016:
                if (keyState.getPressedKey() == 3016) {
                    keyState.setPressedKey(-3);
                    return;
                }
                keyState.setPressedKey(3016);
                break;
            case 3006:
                controller.validateSecondCharacter(3006, this::CheckSuggestion);
                break;
            case 3007:
                controller.validateSecondCharacter(3007, this::CheckSuggestion);
                break;
            case 3008:
                controller.validateSecondCharacter(3008, this::CheckSuggestion);
                break;
            case 3009:
                controller.validateSecondCharacter(3009, this::CheckSuggestion);
                break;
            case 3010:
                controller.validateSecondCharacter(3010, this::CheckSuggestion);
                break;
            case 3021:
                controller.validateSecondCharacter(3021, this::CheckSuggestion);
                break;
            default:
                char code = (char) primaryCode;
                if (primaryCode == 44) {
                    controller.validateURLAndEmailInput(code);
                    return;
                }

                if (primaryCode > 2964) {
                    controller.validateFirstCharacter(code);
                } else if (keyState.isShifted() || keyState.isCapsLock()) {
                    if (!keyState.isKeyPressHold()) {
                        inputConnection.commitText(String.valueOf(code).toUpperCase(), 1);
                    }
                } else {
                    if (!keyState.isKeyPressHold()) {
                        inputConnection.commitText(String.valueOf(code), 1);
                    }
                }

                if (primaryCode == 32) {
                    binding.suggestionScrollBar.setVisibility(View.GONE);
                    CheckAutoCorrection();
                } else {
                    keyState.setAutoCorrected(false);
                }
                if (!keyState.isCapsLock() && keyState.isShifted()) {
                    keyState.setShifted(false);
                }
                keyState.setPressedKey(-3);
                keyController.autoCapitalization();
                controller.validateCharacter(primaryCode);
        }
    }

    private void changeKeyboardLanguageWise() {
        if (!keyState.isLanguageChange()) {
            validateEnglishKeyboardOrientationWise();
            keyState.setLanguageChange(true);
        } else {
            validateTamilKeyboardOrientationWise();
            keyState.setLanguageChange(false);
        }
    }

    @Override
    public void onText(CharSequence charSequence) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private void runOnUiThread(Runnable action) {
        Objects.requireNonNull(getWindow().getWindow()).getDecorView().post(action);
    }

    private void updateSuggestions() {
        binding.suggestionBar.removeAllViews();
        try {
            AtomicReference<List<String>> suggestResult = new AtomicReference<>();
            executorService.execute(() -> {
//                word = getWordAtCursor(inputConnection).trim().split("\n")[0].trim();
                textLength = word.trim().length();

                if (keyState.isLanguageChange()) {
                    suggestResult.set(english_csv.stream().filter(string -> string.toLowerCase().startsWith(word.toLowerCase())).collect(Collectors.toList()));
                } else {
                    suggestResult.set(csv.stream().filter(string -> string.startsWith(word)).collect(Collectors.toList()));
                }

                if (word.trim().isEmpty()) {
                    suggestResult.set(new ArrayList<>());
                }

                int length = Math.min(suggestResult.get().size(), 6);
                runOnUiThread(() -> {
                    if (!suggestResult.get().isEmpty()) {
                        binding.suggestionScrollBar.setVisibility(View.VISIBLE);
                        binding.introBar.setVisibility(View.GONE);
                        for (int i = 0; i < length; i++) {
                            String word = suggestResult.get().get(i);
                            LinearLayout linearLayout = new LinearLayout(this);
                            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(2, 30));
                            linearLayout.setBackgroundColor(binding.keyboardLayoutView.determineTextColor());
                            TextView suggestionView = getTextView(word);
                            binding.suggestionBar.addView(suggestionView);
                            binding.suggestionBar.addView(linearLayout);
                        }
                        if (binding.suggestionBar.getChildCount() > 0) {
                            int lastIndex = binding.suggestionBar.getChildCount() - 1;
                            View lastView = binding.suggestionBar.getChildAt(lastIndex);
                            if (lastView instanceof LinearLayout) {
                                binding.suggestionBar.removeViewAt(lastIndex);
                            }
                        }
                    } else {
                        binding.suggestionScrollBar.setVisibility(View.GONE);
                        binding.introBar.setVisibility(View.VISIBLE);
                    }
                });
            });


        } catch (Exception e) {
            binding.suggestionScrollBar.setVisibility(View.GONE);
            binding.introBar.setVisibility(View.VISIBLE);
            FirebaseCrashlytics.getInstance().log(Objects.requireNonNull(e.getMessage()));
            Timber.tag("Exceptions").i(Objects.requireNonNull(e.getMessage()));
        }
    }

    private @NonNull TextView getTextView(String suggestWord) {
        TextView suggestionView = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        suggestionView.setLayoutParams(textParams);
        suggestionView.setPadding(dpToPx(50, getApplicationContext()), 0, dpToPx(50, getApplicationContext()), 0);
        suggestionView.setText(suggestWord);
        suggestionView.setTextSize(16);
        suggestionView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        suggestionView.setGravity(Gravity.CENTER);
        suggestionView.setTextColor(binding.keyboardLayoutView.determineTextColor());
        suggestionView.setTypeface(ResourcesCompat.getFont(this, R.font.inter));

        suggestionView.setOnClickListener(v -> {
            binding.suggestionScrollBar.setVisibility(View.GONE);
            binding.introBar.setVisibility(View.VISIBLE);
            CharSequence currentText = inputConnection.getExtractedText(new ExtractedTextRequest(), 0).text;
            CharSequence before = inputConnection.getTextBeforeCursor(currentText.length(), 0);
            analyticsController.logSuggestionEvent(word);

            if (suggestWord.equalsIgnoreCase(word)) return;
            assert before != null;
            String suggest_text = suggestWord;
            if (Character.isUpperCase(word.charAt(0)) && keyState.isLanguageChange()) {
                suggest_text = suggest_text.substring(0, 1).toUpperCase() + suggest_text.substring(1);
            }

            inputConnection.deleteSurroundingText(textLength, 0);
            inputConnection.commitText(suggest_text + " ", 1);
        });
        return suggestionView;
    }

    @Override
    public void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo) {
        position = cursorAnchorInfo.getSelectionStart();
        super.onUpdateCursorAnchorInfo(cursorAnchorInfo);
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        if (oldSelStart != newSelStart || oldSelEnd != newSelEnd) {
            CharSequence sequence = inputConnection.getTextBeforeCursor(newSelStart, 0);
            assert sequence != null;
            if (sequence.length() >= 1 && Character.codePointAt(sequence, sequence.length() - 1) != 32) {
                String[] strings = sequence.toString().split(" ");
                keyController.autoCapitalization();
                word = strings[strings.length - 1];
                CheckSuggestion();
            } else {
                binding.suggestionScrollBar.setVisibility(View.GONE);
                binding.introBar.setVisibility(View.VISIBLE);
            }
        }
        emojiViewModel.proceedEmojiSearch(word);
    }

    private void CheckAutoCorrection() {
        if (sharedPreferences.getBoolean("auto_correction", false) && binding.emojiPicker.getVisibility() == View.GONE && binding.clipboardView.getVisibility() == View.GONE && !keyState.isNumberInputType())
            if (!keyState.isAutoCorrected()) controller.autoCorrection(word, csv, english_csv);
    }

    private Keyboard getTamilKeyboardLayout() {
        if (sharedPreferences.getString("layout_preference", "1").equals("1")) {
            return secondTamilKeyboardLayout;
        } else {
            return firstTamilKeyboardLayout;
        }
    }

    private void CheckSuggestion() {
        if (sharedPreferences.getBoolean("dictionary_suggestion", true) && binding.emojiPicker.getVisibility() == View.GONE && binding.clipboardView.getVisibility() == View.GONE && !keyState.isNumberInputType())
            updateSuggestions();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void applyTheme() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            binding.mainlinearContainer.setBackgroundColor(getResources().getColor(R.color.keyboard_background_dark));
            binding.keyboardLayoutView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.keyboard_background_dark)));
            binding.statusBar.setBackgroundColor(getColor(R.color.statusBar_background_dark));
            binding.introBar.setBackgroundColor(getColor(R.color.statusBar_background_dark));
            binding.suggestionBar.setBackgroundColor(getColor(R.color.keyboard_background_dark));
            binding.gifButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.statusBar_background)));
            binding.emojiButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.statusBar_background)));
            binding.clipIcon.setColorFilter(getColor(R.color.status_title_dark));
            binding.clipTextLayout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark_key)));
            binding.settingsButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.statusBar_background)));
            binding.voiceButton.setColorFilter(getColor(R.color.statusBar_background));
            binding.voiceStatus.setTextColor(getColor(R.color.grey300));
            binding.clipBar.setTextColor(getColor(R.color.status_title_dark));
            binding.voiceStatus.setTextColor(getColor(R.color.status_title_dark));
            binding.emojiPicker.setForegroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.black)));
        } else {
            binding.mainlinearContainer.setBackgroundColor(getResources().getColor(R.color.keyboard_background));
            binding.keyboardLayoutView.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.keyboard_background)));
            binding.statusBar.setBackgroundColor(getColor(R.color.statusBar_background));
            binding.introBar.setBackgroundColor(getColor(R.color.statusBar_background));
            binding.suggestionBar.setBackgroundColor(getColor(R.color.keyboard_background));
            binding.gifButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_intro_icon_color)));
            binding.emojiButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_intro_icon_color)));
            binding.clipIcon.setColorFilter(getColor(R.color.status_title));
            binding.clipTextLayout.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.copy_light)));
            binding.settingsButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_intro_icon_color)));
            binding.voiceButton.setColorFilter(getColor(R.color.light_intro_icon_color));
            binding.voiceStatus.setTextColor(getColor(R.color.dark_mode_icon));
            binding.clipBar.setTextColor(getColor(R.color.status_title));
        }
        validateModeButton();
        binding.menuView.updateTheme();
    }

//    public void validateKeyboardSession(String name) {
//        if (!keyState.getPackageName().equals(name) && !name.contains("launcher")) {
//            keyState.setPackageName(name);
//            mKeyboardState = R.integer.keyboard_normal;
//            keyState.setLanguageChange(false);
//            validateTamilKeyboardOrientationWise();
//            validateKeyboardMode(mKeyboardState, false);
//        } else if (keyState.isLanguageChange()) {
//            validateEnglishKeyboardOrientationWise();
//            validateKeyboardMode(R.integer.keyboard_english, true);
//        }
//    }
//
//    private void validateKeyboardMode(int mKeyboardState, boolean isLanguageChange) {
//        keyState.setmKeyboardState(mKeyboardState);
//        keyState.setLanguageChange(isLanguageChange);
//    }

    private void disableCapitalization() {
        isBrowser = Arrays.stream(browserList).anyMatch(string -> string.equals(keyState.getPackageName()));
    }
}