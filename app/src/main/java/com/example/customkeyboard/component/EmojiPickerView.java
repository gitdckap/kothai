package com.example.customkeyboard.component;

import static com.example.customkeyboard.utils.Utils.initializeEmojiCSV;
import static com.example.customkeyboard.utils.Utils.isLandscape;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.customkeyboard.R;
import com.example.customkeyboard.adapters.EmojiAdapter;
import com.example.customkeyboard.adapters.SearchEmojiAdapter;
import com.example.customkeyboard.databinding.ViewEmojiPickerBinding;
import com.example.customkeyboard.model.Emoji;
import com.example.customkeyboard.model.KeyboardKeyState;
import com.vdurmont.emoji.EmojiManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class EmojiPickerView extends LinearLayout implements TextWatcher {

    private ViewEmojiPickerBinding binding;
    private List<Emoji> emojiList = new ArrayList<>();
    LinearLayoutManager linearLayoutManager;
    int index = 0;
    LinearLayout[] layout;
    SharedPreferences sharedPreferences;
    boolean isDarkMode;
    boolean isEmojiEnable = true;
    String typeText = "";
    EmojiAdapter adapter;

    List<String> recent = new ArrayList<>();

    private Handler mainHandler;
    private ExecutorService executorService;

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if (!isEmojiEnable) return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            List<com.vdurmont.emoji.Emoji> emojis = findEmojisByKeyword(charSequence.toString().isEmpty() ? "smil" : charSequence.toString());
            binding.emojiSearchList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            SearchEmojiAdapter adapter = new SearchEmojiAdapter(getContext(), emojis, value -> onEmojiClickListener.onEmojiClick(value));
            binding.emojiSearchList.setAdapter(adapter);
        }, 500);
    }

    @Override
    public void afterTextChanged(Editable editable) {
        typeText = editable.toString();
    }

    public interface OnEmojiClickListener {
        void onEmojiClick(String value);

        void onDeleteClick(MotionEvent motionEvent);

        void onSwitchKeyboard();

        void onKeyboardView(InputConnection inputConnection);
    }

    private OnEmojiClickListener onEmojiClickListener;

    public EmojiPickerView(Context context) {
        super(context);
        init(context);
    }

    public EmojiPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        binding = ViewEmojiPickerBinding.inflate(LayoutInflater.from(context), this);
        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setInitialPrefetchItemCount(10);

        loadEmojiCategory();
        // Initialize the main handler to update UI
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        layout = new LinearLayout[]{binding.recentLayout, binding.smileysPeopleLayout, binding.animalsNatureLayout, binding.foodDrinkLayout, binding.activityLayout, binding.travelPlacesLayout, binding.objectsLayout, binding.symbolsLayout, binding.flagsLayout};

        applyTheme();

        binding.recent.setOnClickListener(view -> scrollTo(0));
        binding.smileysPeople.setOnClickListener(view -> scrollTo(1));
        binding.animalsNature.setOnClickListener(view -> scrollTo(2));
        binding.foodDrink.setOnClickListener(view -> scrollTo(3));
        binding.activity.setOnClickListener(view -> scrollTo(4));
        binding.travelPlaces.setOnClickListener(view -> scrollTo(5));
        binding.objects.setOnClickListener(view -> scrollTo(6));
        binding.symbols.setOnClickListener(view -> scrollTo(7));
        binding.flags.setOnClickListener(view -> scrollTo(8));

        binding.search.addTextChangedListener(this);

        binding.switchButton.setOnClickListener(view -> {
            if (KeyboardKeyState.getInstance().isEmojiSearchFocus()) {
                if (isEmojiEnable) {
                    binding.search.clearFocus();
                    return;
                }
                defaultView();
                return;
            }
            onEmojiClickListener.onSwitchKeyboard();
        });
        binding.deleteButton.setOnTouchListener((view, motionEvent) -> {
            onEmojiClickListener.onDeleteClick(motionEvent);
            return false;
        });
        binding.modeButton.setOnClickListener(view -> changeTheme());
        binding.abc.setOnClickListener(view -> onEmojiClickListener.onSwitchKeyboard());
        binding.topEmojiIcon.setOnClickListener(view -> showEmojiView());
        binding.gifIcon.setOnClickListener(view -> showGIFView());
        binding.search.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                if (isEmojiEnable) {
                    defaultView();
                }
            } else {
                searchView();
            }
        });

        binding.emojiListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                changeBackground(linearLayoutManager.findFirstVisibleItemPosition());
            }
        });
    }

    private void searchView() {
        KeyboardKeyState.getInstance().setEmojiSearchFocus(true);
        binding.mainEmojiPickerContainer.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        onEmojiClickListener.onKeyboardView(binding.search.onCreateInputConnection(new EditorInfo()));
        binding.topEmojiIconLayout.setVisibility(GONE);
        binding.gifIconLayout.setVisibility(GONE);
        binding.modeButtonLayout.setVisibility(GONE);
        binding.search.setText("");

        if (isEmojiEnable) {
            binding.emojiContainer.setVisibility(GONE);
            binding.emojiSearchList.setVisibility(VISIBLE);
        } else {
            binding.giphyView.setVisibility(GONE);
        }
    }

    private void defaultView() {
        KeyboardKeyState.getInstance().setEmojiSearchFocus(false);
        onEmojiClickListener.onKeyboardView(null);
        binding.topEmojiIconLayout.setVisibility(VISIBLE);
        binding.gifIconLayout.setVisibility(VISIBLE);
        binding.modeButtonLayout.setVisibility(VISIBLE);
        binding.search.setText("");

        if (isEmojiEnable) {
            binding.emojiContainer.setVisibility(VISIBLE);
            binding.emojiSearchList.setVisibility(GONE);
        } else {
            searchGiphyByText();
        }
    }

    private void showEmojiView() {
        isEmojiEnable = true;
        addTopEmojiItemBackground();
        loadDataInBackground();
        binding.giphyView.setVisibility(GONE);
        binding.emojiContainer.setVisibility(VISIBLE);
    }

    private void showGIFView() {
        isEmojiEnable = false;
        addTopEmojiItemBackground();
        binding.giphyView.setVisibility(VISIBLE);
        binding.emojiContainer.setVisibility(GONE);
        binding.giphyView.initView();
    }

    private void changeTheme() {
        isDarkMode = !isDarkMode;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isDarkMode", isDarkMode);
        editor.apply();
        applyTheme();
        binding.giphyView.updateTheme();
    }

    private void scrollTo(int position) {
        binding.emojiSearchList.setHasFixedSize(true);
        linearLayoutManager.setInitialPrefetchItemCount(10);
        binding.emojiSearchList.post(() -> linearLayoutManager.scrollToPositionWithOffset(position, 0));
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void changeBackground(int position) {
        if (index != position) {
            changeImageButtonTheme();
            layout[index].setBackground(null);
            index = position;
        }
        ImageButton imageButton = (ImageButton) layout[index].getChildAt(0);
        if (isDarkMode) {
            layout[index].setBackground(getContext().getDrawable(R.drawable.search_input_border));
            imageButton.setColorFilter(getResources().getColor(R.color.white));
        } else {
            layout[index].setBackground(getContext().getDrawable(R.drawable.search_input_border_light));
            imageButton.setColorFilter(getResources().getColor(R.color.black));
        }

    }

    public static List<com.vdurmont.emoji.Emoji> findEmojisByKeyword(String keyword) {
        return EmojiManager.getAll().stream().filter(emoji -> emoji.getAliases().stream().anyMatch(alias -> alias.toLowerCase().contains(keyword.isEmpty() ? "" : keyword.toLowerCase()))).collect(Collectors.toList());
    }

    private void loadEmojiCategory() {
        binding.emojiListView.setLayoutManager(linearLayoutManager);
        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
        binding.emojiListView.setRecycledViewPool(recycledViewPool);
        binding.emojiListView.setHasFixedSize(true);

        adapter = new EmojiAdapter(getContext(), emojiList, value -> {
            if (!recent.contains(value)) {
                recent.add(0, value); // Add to the top of the list
                if (recent.size() > getSizeOrientationWise()) {
                    recent.remove(recent.size() - 1); // Maintain size limit
                }

                Emoji recentCategory = findEmojiCategory();
                if (recentCategory != null) {
                    recentCategory.setEmojis(recent);
                } else {
                    emojiList.add(0, new Emoji("Recent", recent)); // Add "Recent" at the top
                }
            }
            onEmojiClickListener.onEmojiClick(value);
        });
        binding.emojiListView.setAdapter(adapter);
    }

    public void loadDataInBackground() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // Simulate background task (e.g., data fetching)
                emojiList = initializeEmojiCSV(getContext()); // Replace with actual data loading

                // Once data is loaded, update RecyclerView on the main thread
                updateUIWithData(emojiList);
            }
        });
    }

    private void updateUIWithData(List<Emoji> data) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                adapter.updateData(data);
            }
        });
    }

    private int getSizeOrientationWise() {
        return isLandscape(getContext()) ? 34 : 16;
    }

    private Emoji findEmojiCategory() {
        for (Emoji category : emojiList) {
            if (category.getTitle().equals("Recent")) {
                return category;
            }
        }
        return null;
    }

    private void addTopEmojiItemBackground() {
        if (!isEmojiEnable) {
            binding.topEmojiIconLayout.setBackground(null);
            binding.gifIconLayout.setBackground(getLayoutBackground());
            binding.topEmojiIcon.setColorFilter(getIconColor());
            binding.gifIcon.setColorFilter(getSelectedIconColor());
        } else {
            binding.topEmojiIconLayout.setBackground(getLayoutBackground());
            binding.gifIconLayout.setBackground(null);
            binding.topEmojiIcon.setColorFilter(getSelectedIconColor());
            binding.gifIcon.setColorFilter(getIconColor());
        }
    }

    private Drawable getLayoutBackground() {
        if (isDarkMode) {
            return AppCompatResources.getDrawable(getContext(), R.drawable.icon_background);
        } else {
            return AppCompatResources.getDrawable(getContext(), R.drawable.icon_background_light);
        }
    }

    private int getIconColor() {
        if (isDarkMode) {
            return getContext().getColor(R.color.white);
        } else {
            return getContext().getColor(R.color.light_intro_icon_color);
        }
    }

    private int getSelectedIconColor() {
        if (isDarkMode) {
            return getContext().getColor(R.color.black);
        } else {
            return getContext().getColor(R.color.white);
        }
    }

    private void applyTheme() {
        Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_search);
        if (isDarkMode) {
            binding.topEmojiBar.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background_dark));
            binding.footerEmojiBar.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background_dark));
            binding.modeButton.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_lightmode));
            binding.modeButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.gray200)));
            binding.switchButton.setColorFilter(getContext().getColor(R.color.dark_icon));
            binding.gifIcon.setColorFilter(getContext().getColor(R.color.dark_icon));
            binding.deleteButton.setColorFilter(getResources().getColor(R.color.dark_icon));
            binding.abc.setTextColor(getContext().getColor(R.color.dark_icon));
            binding.emojiListView.setBackground(AppCompatResources.getDrawable(getContext(), R.color.emoji_view_background));
            binding.emojiSearchList.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background_dark));
            binding.search.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.search_input_border));
            binding.search.setHintTextColor(getContext().getColor(R.color.white));
            binding.search.setTextColor(getContext().getColor(R.color.white));
            Objects.requireNonNull(drawable).setColorFilter(new PorterDuffColorFilter(getContext().getColor(R.color.white), PorterDuff.Mode.SRC_IN));
            binding.search.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        } else {
            binding.topEmojiBar.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background));
            binding.footerEmojiBar.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background));
            binding.modeButton.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_darkmode));
            binding.modeButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.dark_mode_icon)));
            binding.gifIcon.setColorFilter(getContext().getColor(R.color.light_intro_icon_color));
            binding.switchButton.setColorFilter(getContext().getColor(R.color.light_intro_icon_color));
            binding.deleteButton.setColorFilter(getResources().getColor(R.color.light_intro_icon_color));
            binding.abc.setTextColor(getContext().getColor(R.color.light_intro_icon_color));
            binding.emojiListView.setBackground(AppCompatResources.getDrawable(getContext(), R.color.emoji_view_background_light));
            binding.emojiSearchList.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background));
            binding.search.setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.search_input_border_light));
            binding.search.setHintTextColor(getContext().getColor(R.color.light_intro_icon_color));
            binding.search.setTextColor(getContext().getColor(R.color.black));
            Objects.requireNonNull(drawable).setColorFilter(new PorterDuffColorFilter(getContext().getColor(R.color.light_intro_icon_color), PorterDuff.Mode.SRC_IN));
            binding.search.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }
        addTopEmojiItemBackground();
        changeImageButtonTheme();
        changeBackground(index);
    }

    private void changeImageButtonTheme() {
        for (LinearLayout linearLayout : layout) {
            ImageButton button = (ImageButton) linearLayout.getChildAt(0);
            if (isDarkMode) {
                button.setColorFilter(getResources().getColor(R.color.dark_icon));
            } else {
                button.setColorFilter(getResources().getColor(R.color.light_intro_icon_color));
            }

        }
    }

    public void setOnEmojiPickedListener(OnEmojiClickListener listener) {
        this.onEmojiClickListener = listener;
    }

    public void setHeight(int height) {
        binding.mainEmojiPickerContainer.getLayoutParams().height = height;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void openEmoji() {
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        applyTheme();
        showEmojiView();
        loadDataInBackground();
        scrollTo(0);
        Objects.requireNonNull(binding.emojiListView.getAdapter()).notifyItemChanged(0);
        validateGIFButtonVisibility();
    }

    private void validateGIFButtonVisibility() {
        if (KeyboardKeyState.getInstance().isSearchInput() || KeyboardKeyState.getInstance().isURLInput()) {
            binding.gifIcon.setVisibility(GONE);
        } else {
            binding.gifIcon.setVisibility(VISIBLE);
        }
    }

    public void openGIF() {
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        showGIFView();
        applyTheme();
        binding.giphyView.updateTheme();
        validateGIFButtonVisibility();
    }

    public void searchGiphyByText() {
        onEmojiClickListener.onKeyboardView(null);
        binding.giphyView.setVisibility(VISIBLE);
        binding.giphyView.getSearchGiphyByText(typeText);
        binding.search.clearFocus();
    }

    public void switchToDefault() {
        if (KeyboardKeyState.getInstance().isEmojiSearchFocus()) {
            binding.search.clearFocus();
            defaultView();
        }

    }
}



