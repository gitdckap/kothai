package com.example.customkeyboard.component;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.customkeyboard.R;
import com.example.customkeyboard.async.DownloadGIF;
import com.example.customkeyboard.databinding.GiphyPickerViewBinding;
import com.example.customkeyboard.model.KeyboardKeyState;
import com.example.customkeyboard.utils.Utils;
import com.giphy.sdk.core.models.Media;
import com.giphy.sdk.core.models.enums.MediaType;
import com.giphy.sdk.core.models.enums.RatingType;
import com.giphy.sdk.ui.Giphy;
import com.giphy.sdk.ui.pagination.GPHContent;
import com.giphy.sdk.ui.views.GPHGridCallback;

public class GiphyPickerView extends LinearLayout implements GPHGridCallback {
    GiphyPickerViewBinding binding;
    SharedPreferences sharedPreferences;
    String selectText = Utils.giphyCategories[0];
    boolean isDarkMode;
    DownloadGIF downloadGIF;
    int i = 0;

    public GiphyPickerView(Context context) {
        super(context);
        init();
    }

    public GiphyPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Giphy.INSTANCE.configure(getContext(), "KDGgtN6Jl79slBJjf1FqLDntX17gNFHe", false);
        binding = GiphyPickerViewBinding.inflate(LayoutInflater.from(getContext()), this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        binding.mainContainer.setCallback(this);
        binding.cancelButton.setOnClickListener(view -> {
            binding.mainContainer.setVisibility(VISIBLE);
            binding.gifClickProgress.setVisibility(GONE);
            downloadGIF.cancel(true);
        });
        applyTheme();
        loadContent(selectText);
        loadCategories();
    }

    private void loadContent(String search) {
        binding.mainContainer.setContent(GPHContent.Companion.searchQuery(search, MediaType.gif, RatingType.pg13));
    }

    private void loadCategories() {
        binding.giphyCategories.removeAllViews();
        for (String title : Utils.giphyCategories) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    Utils.dpToPx(140, getContext()),
                    LayoutParams.MATCH_PARENT
            );
            Button button = new Button(getContext());
            button.setLayoutParams(layoutParams);
            button.setText(title);
            button.setTextColor(selectText.equals(title) ? getSelectedTextColor() : getIdealTextColor());
            button.setBackground(selectText.equals(title) ? getTopBorder() : null);
            button.setTypeface(button.getTypeface(), selectText.equals(title) ? Typeface.BOLD : Typeface.NORMAL);
            button.setOnClickListener(view -> {
                selectText = title;
                loadContent(selectText);
                loadCategories();
            });
            binding.giphyCategories.addView(button);
        }
    }

    private int getSelectedTextColor() {
        if (isDarkMode) {
            return getContext().getColor(R.color.white);
        } else {
            return getContext().getColor(R.color.black);
        }
    }

    private int getIdealTextColor() {
        if (isDarkMode) {
            return getContext().getColor(R.color.dark_icon);
        } else {
            return getContext().getColor(R.color.light_intro_icon_color);
        }
    }

    private void applyTheme() {
        if (isDarkMode) {
            binding.footerGiphyScroll.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background_dark));
            binding.mainContainer.setBackground(AppCompatResources.getDrawable(getContext(), R.color.emoji_view_background));
        } else {
            binding.footerGiphyScroll.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background));
            binding.mainContainer.setBackground(AppCompatResources.getDrawable(getContext(), R.color.emoji_view_background_light));
        }
    }

    private int getDividerColor() {
        if (isDarkMode) {
            return Color.WHITE;
        } else {
            return Color.BLACK;
        }
    }

    private Drawable getTopBorder() {
        return new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                Paint paint = new Paint();
                paint.setColor(getDividerColor());
                paint.setStyle(Paint.Style.FILL);

                canvas.drawRect(0, 0, getBounds().width(), 6, paint);
            }

            @Override
            public void setAlpha(int alpha) {
                // Not needed for this use case
            }

            @Override
            public void setColorFilter(android.graphics.ColorFilter colorFilter) {
                // Not needed for this use case
            }

            @Override
            public int getOpacity() {
                return android.graphics.PixelFormat.OPAQUE;
            }
        };
    }

    public void updateTheme() {
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        applyTheme();
        loadCategories();
    }

    public void initView() {
        selectText = Utils.giphyCategories[0];
        loadContent(selectText);
        updateTheme();
    }

    public void getSearchGiphyByText(String text) {
        loadContent(text.isEmpty() ? selectText : text);
    }

    @Override
    public void contentDidUpdate(int i) {

    }

    @Override
    public void didSelectMedia(@NonNull Media media) {
        try {
            assert media.getImages().getOriginal() != null;
            String gifUrl = media.getImages().getOriginal().getGifUrl();
            i += 1;
            downloadGIF = new DownloadGIF(i, gifUrl, getContext(), KeyboardKeyState.getInstance().getInputConnection(), binding.gifClickProgress, binding.mainContainer);
            downloadGIF.execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
