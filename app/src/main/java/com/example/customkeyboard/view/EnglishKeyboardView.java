package com.example.customkeyboard.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.GestureDetector;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.example.customkeyboard.R;
import com.example.customkeyboard.model.KeyboardKeyState;

public class EnglishKeyboardView {

    Canvas canvas;
    Context context;
    Keyboard keyboard;
    Paint paint = new Paint();
    Paint numberPain = new Paint();
    Paint textPain = new Paint();
    SharedPreferences sharedPreferences;
    KeyboardKeyState keyState;

    public EnglishKeyboardView(Context context, Canvas canvas, Keyboard keyboard) {
        this.context = context;
        this.canvas = canvas;
        this.keyboard = keyboard;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.keyState = KeyboardKeyState.getInstance();
    }

    @SuppressLint("NewApi")
    public void onDraw() {
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] == keyState.getHoverKey()) {
                paint.setColor(context.getResources().getColor(R.color.gray100));
            } else if (key.codes[0] == -1 || key.codes[0] == -2 || key.codes[0] == -5 || key.codes[0] == -7) {
                paint.setColor(context.getResources().getColor(determineKeyColor()));
                if (sharedPreferences.getBoolean("isDarkMode", false)) {
                    paint.setColor(context.getResources().getColor(R.color.dark_number_symbol));
                } else {
                    paint.setColor(context.getResources().getColor(R.color.number_symbol));
                }
            } else if (key.codes[0] == -4) {
                paint.setColor(context.getResources().getColor(R.color.blue200));
            } else {
                paint.setColor(context.getResources().getColor(determineKeyColor())); // Default color
            }

            int adjustedX = key.x + (int) context.getResources().getDimension(R.dimen.english_key_X);
            int adjustedY = key.y + (int) context.getResources().getDimension(R.dimen.english_key_Y);

            paint.setShadowLayer(
                    context.getResources().getDimension(R.dimen.shadow_radius),
                    0.0f,
                    context.getResources().getDimension(R.dimen.shadow_Y),
                    context.getResources().getColor(R.color.character_shadow_color));

            RectF rect = new RectF(
                    adjustedX - context.getResources().getDimension(R.dimen.english_key_left),
                    adjustedY - context.getResources().getDimension(R.dimen.english_key_top),
                    adjustedX + key.width - context.getResources().getDimension(R.dimen.english_key_right),
                    adjustedY + key.height - context.getResources().getDimension(R.dimen.english_key_bottom)
            );
            canvas.drawRoundRect(rect, context.getResources().getDimension(R.dimen.key_radius), context.getResources().getDimension(R.dimen.key_radius), paint);

            if (key.icon != null) {
                Drawable icon = key.icon;

                if (key.codes[0] == -4) {
                    if (keyState.isSearchInput() && !keyState.isEmailUrlInput() && !keyState.isURLInput()) {
                        icon = AppCompatResources.getDrawable(context, R.drawable.ic_search);
                    } else if (keyState.isEmailInput() || keyState.isEmailUrlInput()) {
                        icon = AppCompatResources.getDrawable(context, R.drawable.ic_email);
                    } else if (keyState.isURLInput()) {
                        icon = AppCompatResources.getDrawable(context, R.drawable.ic_urlinput);
                    } else {
                        icon = AppCompatResources.getDrawable(context, R.drawable.ic_enter);
                    }
                }

                if (key.codes[0] == -1 && keyState.isShifted()) {
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_shifted);
                } else if (key.codes[0] == -1 && keyState.isCapsLock()) {
                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_capslock);
                }

                assert icon != null;
                if (key.codes[0] == -7) {
                    icon.setTint(determineLanguageChange());
                } else if (key.codes[0] == -4) {
                    icon.setTint(context.getColor(R.color.white));
                } else {
                    icon.setTint(determineIconColor());
                }

                int iconWidth = icon.getIntrinsicWidth();
                int iconHeight = icon.getIntrinsicHeight();
                int left = (int) (key.x + (float) (key.width - iconWidth) / 2 + getIntToDP(2));
                int top = (int) (key.y + (float) (key.height - iconHeight) / 2 + getIntToDP(key.codes[0] == 32 ? 4 : 0));
                icon.setBounds(left, top, left + iconWidth, top + iconHeight);
                icon.draw(canvas);
            } else if (key.label != null) {
                textPain.setTextAlign(Paint.Align.CENTER);
                textPain.setTypeface(ResourcesCompat.getFont(context, R.font.inter));

                numberPain.setTextAlign(Paint.Align.CENTER);
                numberPain.setTypeface(ResourcesCompat.getFont(context, R.font.inter));

                String label = validateLabel(key);

                if (key.codes[0] == 32) {
                    label = keyState.isLanguageChange() ? "English" : "தமிழ்";
                }

                if (key.codes[0] == -2) {
                    textPain.setTextSize(context.getResources().getDimension(R.dimen.symbol_letter_size));
                    textPain.setColor(determineIconColor());
                } else if (key.codes[0] == 32) {
                    textPain.setTextSize(context.getResources().getDimension(R.dimen.english_number_size));
                    textPain.setColor(determineIconColor());
                } else {
                    textPain.setTextSize(context.getResources().getDimension(R.dimen.english_letter_size));
                    textPain.setColor(determineTextColor());
                    numberPain.setTextSize(context.getResources().getDimension(R.dimen.english_number_size));
                    numberPain.setColor(determineTextColor());
                }

                if (keyState.isURLInput() && key.codes[0] == 44) {
                    key.label = "/";
                } else if (keyState.isEmailUrlInput() && key.codes[0] == 44) {
                    key.label = "@";
                } else if (key.codes[0] == 44) {
                    key.label = ",";
                }

                if (key.codes[0] == -6) {
                    label = key.label.toString();
                    textPain.setTextSize(context.getResources().getDimension(R.dimen.symbol_letter_size));
                }

                textPain.setAntiAlias(true);
                numberPain.setAntiAlias(true);

                float restCharX = key.x + (float) key.width / 2 + context.getResources().getDimension(R.dimen.english_key_label_main_X);
                float restCharY = key.y + (float) key.height / 3;

                if (label.length() == 2) {
                    canvas.drawText(label.substring(0, 1),
                            key.x + (float) key.width / 2 + context.getResources().getDimension(R.dimen.english_key_label_X),
                            key.y + (float) key.height / 2 + context.getResources().getDimension(R.dimen.english_key_label_number_Y),
                            textPain
                    );
                    canvas.drawText(label.substring(1),
                            restCharX,
                            restCharY,
                            numberPain
                    );
                } else {
                    canvas.drawText(
                            label,
                            key.x + (float) key.width / 2 + context.getResources().getDimension(R.dimen.english_key_label_X),
                            key.y + (float) key.height / 2 + context.getResources().getDimension(R.dimen.english_key_label_text_Y),
                            textPain
                    );
                }

            }
        }
    }

    private String validateLabel(Keyboard.Key key) {
        if (!isSymbolic()) {
            if (key.codes[0] == -10) {
                return key.label.toString();
            } else if (keyState.isShifted()) {
                return key.label.toString().toUpperCase();
            } else if (keyState.isCapsLock()) {
                return key.label.toString().toUpperCase();
            } else {
                return key.label.toString().toLowerCase();
            }
        }
        return key.label.toString();
    }

    public int determineIconColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return context.getResources().getColor(R.color.white);
        } else {
            return context.getResources().getColor(R.color.light_icon);
        }
    }

    public int determineLanguageChange() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return context.getResources().getColor(R.color.dark_number_key);
        } else {
            return context.getResources().getColor(R.color.light_language_mode);
        }
    }

    public int determineTextColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return context.getResources().getColor(R.color.white);
        } else {
            return context.getResources().getColor(R.color.light_number_key);
        }
    }

    private float getIntToDP(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    public int determineKeyColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return R.color.dark_key;
        } else {
            return R.color.light_key;
        }
    }

    private boolean isSymbolic() {
        return keyState.getmKeyboardState() == R.integer.keyboard_symbol;
    }
}
