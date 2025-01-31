package com.example.customkeyboard.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.core.content.res.ResourcesCompat;

import com.example.customkeyboard.R;
import com.example.customkeyboard.model.KeyboardKeyState;

public class NumberKeyboardView {

    Paint mKeyPaint = new Paint();
    Paint paintLarge = new Paint();
    Paint paintSmall = new Paint();
    SharedPreferences sharedPreferences;
    Canvas canvas;
    Context context;
    Keyboard keyboard;
    KeyboardKeyState keyState;

    public NumberKeyboardView(Context context, Canvas canvas, Keyboard keyboard) {
        this.context = context;
        this.canvas = canvas;
        this.keyboard = keyboard;
        this.keyState = KeyboardKeyState.getInstance();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void onDraw() {
        paintLarge.setAntiAlias(true);
        paintLarge.setTextSize(context.getResources().getDimension(R.dimen.key_letter_30sp));
        paintLarge.setColor(determineTextColor());
        paintLarge.setTypeface(ResourcesCompat.getFont(context, R.font.inter));

        paintSmall.setAntiAlias(true);
        paintSmall.setTextSize(context.getResources().getDimension(R.dimen.tamil_key_12dp));
        paintSmall.setTypeface(ResourcesCompat.getFont(context, R.font.inter));
        paintSmall.setColor(sharedPreferences.getBoolean("isDarkMode", false) ?
                context.getResources().getColor(R.color.dark_number_second_subtext) :
                context.getResources().getColor(R.color.grey200));

        mKeyPaint.setShadowLayer(
                context.getResources().getDimension(R.dimen.number_pad_shadow_radius),
                0.0f,
                context.getResources().getDimension(R.dimen.number_pad_shadow_Y),
                context.getResources().getColor(R.color.number_shadow_color));

        for (Keyboard.Key key : keyboard.getKeys()) {
            switch (key.codes[0]) {
                case -4:
                    hoverNumericKeys(key, R.color.blue200);
                    break;
                case 45:
                case 32:
                case -5:
                    hoverNumericKeys(key, getIconColor());
                    break;
                default:
                    hoverNumericKeys(key, determineKeyColor());
            }

            int adjustedX = key.x + 5;
            int adjustedY = key.y + 20;

            RectF rect = getRectF(key, adjustedX, adjustedY);
            canvas.drawRoundRect(rect, context.getResources().getDimension(R.dimen.number_pad_key_radius), context.getResources().getDimension(R.dimen.number_pad_key_radius), mKeyPaint);

            if (key.icon != null) {
                createKeyIcon(key);
            }
            if (key.label != null) {
                createKeyLabels(key);
            }
        }
    }

    private void createKeyLabels(Keyboard.Key key) {
        String label = key.label.toString();
        String firstChar = "";
        float firstCharX = key.x + ((float) key.width / 2) - (paintLarge.measureText(label) / 2);
        float firstCharY = key.y + ((float) key.height / 2) + (paintLarge.getTextSize() / 2);
        if (label.length() > 1) {
            firstChar = label.substring(0, 1);
            firstCharX = key.x + ((float) key.width / 2) - (paintLarge.measureText(firstChar) / 2);
            firstCharY = key.y + ((float) key.height / 2) + (paintLarge.getTextSize() / 2);
            canvas.drawText(firstChar, firstCharX - 30, firstCharY + 2, paintLarge);
        } else {
            canvas.drawText(label, firstCharX, firstCharY, paintLarge);
        }

        if (label.length() > 1) {
            String restChars = label.substring(1);
            float restCharX = key.x + ((float) key.width / 2) + (paintLarge.measureText(label.substring(0, 1)) / 2);
            float restCharY = key.y + ((float) key.height / 2) + (paintSmall.getTextSize() / 2);
            canvas.drawText(restChars, restCharX - 15, restCharY + 8, paintSmall);
        }
    }

    private void createKeyIcon(Keyboard.Key key) {
        Drawable icon = key.icon;
        if (key.codes[0] == -4) {
            icon.setTint(context.getResources().getColor(R.color.white));
        } else {
            icon.setTint(determineTextColor());
        }

        int iconWidth = icon.getIntrinsicWidth();
        int iconHeight = icon.getIntrinsicHeight();
        int left = key.x + (key.width - iconWidth) / 2 - 5;
        int top = key.y + (key.height - iconHeight) / 2 + 15;
        icon.setBounds(left, top, left + iconWidth, top + iconHeight);
        icon.draw(canvas);
    }

    private int getIconColor() {
        return sharedPreferences.getBoolean("isDarkMode", false) ? R.color.dark_number_symbol : R.color.number_symbol;
    }

    private static RectF getRectF(Keyboard.Key key, int adjustedX, int adjustedY) {
        return new RectF(adjustedX, adjustedY, adjustedX + key.width - 12, adjustedY + (key.height - 12));
    }

    public int determineTextColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return context.getResources().getColor(R.color.dark_number_key);
        } else {
            return context.getResources().getColor(R.color.light_number_key);
        }
    }

    public int determineKeyColor() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            return R.color.dark_key;
        } else {
            return R.color.light_key;
        }
    }

    private void hoverNumericKeys(Keyboard.Key key, int color) {
        if (keyState.getHoverKey() == key.codes[0]) {
            mKeyPaint.setColor(context.getResources().getColor(R.color.gray100));
        } else {
            mKeyPaint.setColor(context.getResources().getColor(color));
        }
    }
}
