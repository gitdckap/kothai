package com.example.customkeyboard.view;

import static com.example.customkeyboard.utils.Utils.tamilKeys_1;
import static com.example.customkeyboard.utils.Utils.tamilKeys_2;
import static com.example.customkeyboard.utils.Utils.tamilKeys_3;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;

import com.example.customkeyboard.R;
import com.example.customkeyboard.model.KeyboardKeyState;

import java.util.Arrays;

public class TamilKeyboardView {

    Canvas canvas;
    Context context;
    Keyboard keyboard;
    Paint paint = new Paint();
    Paint textPain = new Paint();
    Paint secondaryTextPain = new Paint();
    SharedPreferences sharedPreferences;
    KeyboardKeyState keyState;

    public TamilKeyboardView(Context context, Keyboard keyboard, Canvas canvas) {
        this.context = context;
        this.keyboard = keyboard;
        this.canvas = canvas;
        this.keyState = KeyboardKeyState.getInstance();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void onDraw() {
        for (Keyboard.Key key : keyboard.getKeys()) {
            if (key.codes[0] != keyState.getPressedKey() && key.codes[0] == keyState.getHoverKey()) {
                paint.setColor(context.getResources().getColor(R.color.gray100)); // Grey color for pressed key
            } else if (key.codes[0] == -4 || key.codes[0] == keyState.getPressedKey()) {
                paint.setColor(context.getResources().getColor(R.color.blue200));
            } else {
                paint.setColor(context.getResources().getColor(determineKeyColor(key.codes[0]))); // Default color
            }

            int adjustedX = (int) (key.x + getDimension(R.dimen.tamil_key_4dp));
            int adjustedY = (int) (key.y + getDimension(R.dimen.tamil_key_4dp));

            paint.setShadowLayer(
                    context.getResources().getDimension(R.dimen.shadow_radius),
                    0.0f,
                    context.getResources().getDimension(R.dimen.shadow_Y),
                    context.getResources().getColor(R.color.character_shadow_color));

            RectF rect = new RectF(
                    adjustedX - getDimension(R.dimen.tamil_key_2dp),
                    adjustedY,
                    adjustedX + key.width - getDimension(R.dimen.tamil_key_6dp),
                    adjustedY + key.height - getDimension(R.dimen.tamil_key_4dp));
            canvas.drawRoundRect(rect, getDimension(R.dimen.key_radius), getDimension(R.dimen.key_radius), paint);

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

                assert icon != null;
                if (key.codes[0] == -4) {
                    icon.setTint(context.getColor(R.color.white));
                } else {
                    icon.setTint(determineIconColor());
                }
                int iconWidth = icon.getIntrinsicWidth();
                int iconHeight = icon.getIntrinsicHeight();
                int left = key.x + (key.width - iconWidth) / 2;
                int top = (int) (key.y + (float) (key.height - iconHeight) / 2 + getIntToDP(key.codes[0] == 32 ? 6 : 3));
                icon.setBounds(left, top, left + iconWidth, top + iconHeight);
                icon.draw(canvas);
            } else if (key.label != null) {
                textPain.setTextAlign(Paint.Align.CENTER);
                textPain.setTypeface(ResourcesCompat.getFont(context, R.font.inter));

                if (key.codes[0] == -2) {
                    textPain.setTextSize(getDimension(R.dimen.tamil_key_letter_14sp));
                } else if (key.codes[0] == -101) {
                    textPain.setTextSize(getDimension(R.dimen.tamil_key_letter_16sp));
                } else {
                    textPain.setTextSize(getDimension(R.dimen.tamil_key_letter_19sp));
                    secondaryTextPain.setTextSize(getDimension(R.dimen.tamil_key_letter_12sp));
                }

                if (keyState.isURLInput() && key.codes[0] == 44) {
                    key.label = "/";
                } else if (keyState.isEmailUrlInput() && key.codes[0] == 44) {
                    key.label = "@";
                } else if (key.codes[0] == 44) {
                    key.label = ",";
                }

                textPain.setColor(determineTextColor());
                secondaryTextPain.setColor(determineTextColor());
                textPain.setAntiAlias(true);
                secondaryTextPain.setAntiAlias(true);
                secondaryTextPain.setTypeface(ResourcesCompat.getFont(context, R.font.inter));

                float restCharX = key.x + (float) key.width / 2 + getDimension(R.dimen.tamil_key_12dp);
                float restCharY = key.y + (float) key.height / 3 + getDimension(R.dimen.tamil_key_2dp);

                if (key.label.length() == 2) {
                    customLabels(key, restCharX, restCharY);
                } else if (key.label.length() > 2 && key.label.toString().charAt(0) == 'ஂ') {
                    customLabels(key, restCharX, restCharY);
                } else if (key.label.length() > 2 && key.label.toString().charAt(0) == 'ா') {
                    customLabels(key, restCharX, restCharY);
                } else {
                    canvas.drawText(key.label.toString(),
                            key.x + (float) key.width / 2,
                            key.y + (float) key.height / 2 + getDimension(R.dimen.tamil_key_8dp),
                            textPain
                    );
                }

            }
        }
    }

    private void customLabels(Keyboard.Key key, float restCharX, float restCharY) {
        canvas.drawText(key.label.toString().substring(0, 1),
                key.x + (float) key.width / 2,
                key.y + (float) key.height / 2 + getDimension(R.dimen.tamil_key_10dp),
                textPain
        );
        canvas.drawText(key.label.toString().substring(1),
                restCharX,
                restCharY + getDimension(R.dimen.tamil_key_2dp),
                secondaryTextPain
        );
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

    public int determineKeyColor(int key) {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            if (Arrays.asList(tamilKeys_1()).contains(key)) {
                return R.color.tamil_dark_key_1;
            } else if (Arrays.asList(tamilKeys_2()).contains(key)) {
                return R.color.tamil_dark_key_2;
            } else if (Arrays.asList(tamilKeys_3()).contains(key)) {
                return R.color.tamil_dark_key_3;
            } else {
                return R.color.dark_key;
            }
        } else {
            if (Arrays.asList(tamilKeys_1()).contains(key)) {
                return R.color.tamil_key_1;
            } else if (Arrays.asList(tamilKeys_2()).contains(key)) {
                return R.color.tamil_key_2;
            } else if (Arrays.asList(tamilKeys_3()).contains(key)) {
                return R.color.tamil_key_3;
            } else {
                return R.color.white;
            }
        }
    }

    private float getIntToDP(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private float getDimension(int id) {
        return context.getResources().getDimension(id);
    }
}
