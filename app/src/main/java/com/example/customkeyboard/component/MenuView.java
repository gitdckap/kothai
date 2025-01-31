package com.example.customkeyboard.component;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.customkeyboard.R;
import com.example.customkeyboard.databinding.ViewMenuBinding;

import java.util.Objects;

public class MenuView extends LinearLayout {
    ViewMenuBinding binding;
    SharedPreferences sharedPreferences;
    boolean isDarkMode;
    onMenuViewClickListner onMenuViewClickListner;

    public interface onMenuViewClickListner {
        void onClipboard();
    }

    public MenuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        binding = ViewMenuBinding.inflate(LayoutInflater.from(getContext()), this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        binding.clipboardButton.setOnClickListener(view -> onMenuViewClickListner.onClipboard());
        applyTheme();
    }

    private void applyTheme() {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_clipboard);
        if (isDarkMode) {
            binding.clipboardButton.setBackground(AppCompatResources.getDrawable(getContext(), R.color.dark_key));
            Objects.requireNonNull(drawable).setColorFilter(new PorterDuffColorFilter(getContext().getColor(R.color.white), PorterDuff.Mode.SRC_IN));
            binding.clipboardButton.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            binding.clipboardButton.setTextColor(getContext().getColor(R.color.white));
        } else {
            binding.clipboardButton.setBackground(AppCompatResources.getDrawable(getContext(), R.color.emoji_light_background));
            Objects.requireNonNull(drawable).setColorFilter(new PorterDuffColorFilter(getContext().getColor(R.color.light_intro_icon_color), PorterDuff.Mode.SRC_IN));
            binding.clipboardButton.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
            binding.clipboardButton.setTextColor(getContext().getColor(R.color.light_intro_icon_color));
        }
    }

    public void setOnMenuViewClickListner(onMenuViewClickListner listener) {
        this.onMenuViewClickListner = listener;
    }

    public void setHeight(int height) {
        binding.mainMenuView.getLayoutParams().height = height;
    }

    public void updateTheme() {
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        applyTheme();
    }
}
