package com.example.customkeyboard.component;

import static com.example.customkeyboard.utils.Utils.isLongPress;
import static com.example.customkeyboard.utils.Utils.isSelectAll;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.example.customkeyboard.R;
import com.example.customkeyboard.adapters.ClipBoardGridViewAdapter;
import com.example.customkeyboard.database.DBHelper;
import com.example.customkeyboard.databinding.ViewClipboardBinding;
import com.example.customkeyboard.listners.OnClickListner;
import com.example.customkeyboard.model.Clipboard;
import com.example.customkeyboard.model.KeyboardKeyState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClipboardView extends LinearLayout {
    ViewClipboardBinding binding;
    List<String> idList = new ArrayList<>();
    DBHelper dbHelper;
    ClipBoardGridViewAdapter gridViewAdapter;
    onClipboardListner onClipboardListner;
    SharedPreferences sharedPreferences;
    boolean isDarkMode;

    public interface onClipboardListner {
        void onClear(MotionEvent motionEvent);

        void onSwitchKeyboard();
    }

    public ClipboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context) {
        binding = ViewClipboardBinding.inflate(LayoutInflater.from(context), this);
        dbHelper = new DBHelper(context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);

        binding.clipboardDeleteButton.setOnTouchListener((view, motionEvent) -> {
            onClipboardListner.onClear(motionEvent);
            return false;
        });

        binding.checkboxAll.setOnClickListener(view -> {
            isSelectAll = !isSelectAll;
            if (!isSelectAll) {
                idList.clear();
                gridViewAdapter.notifyDataSetChanged();
                return;
            }
            List<Clipboard> clipBoardList = dbHelper.getAllContent();
            for (Clipboard clip : clipBoardList) {
                idList.add(clip.getId());
            }
            gridViewAdapter.notifyDataSetChanged();
        });
        binding.clipDeleteButton.setOnClickListener(view -> {
            dbHelper.deleteRow(idList);
            idList.clear();
            isSelectAll = false;
            binding.checkboxAll.setChecked(false);
            load();
        });
        binding.clipboardBackButton.setOnClickListener(view -> {
            if (isLongPress) {
                idList.clear();
                isLongPress = false;
                isSelectAll = false;
                binding.checkboxAll.setChecked(false);
                binding.checkboxAll.setVisibility(View.GONE);
                binding.clipDeleteButton.setVisibility(View.GONE);
                gridViewAdapter.notifyDataSetChanged();
                return;
            }
            onClipboardListner.onSwitchKeyboard();
        });
    }

    private void load() {
        List<Clipboard> clipBoardList = dbHelper.getAllContent();
        if (clipBoardList.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.clipboardView.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            gridViewAdapter = new ClipBoardGridViewAdapter(getContext(), clipBoardList, idList, new OnClickListner() {
                @Override
                public void onPress(String content) {
                    KeyboardKeyState.getInstance().getInputConnection().commitText(content, 1);
                }

                @Override
                public void onLongClick(boolean isLongClick) {
                    isLongPress = isLongClick;
                    binding.checkboxAll.setVisibility(View.VISIBLE);
                    binding.clipDeleteButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onCheckBoxClick(String id) {
                    boolean isExist = idList.stream().anyMatch(string -> Objects.equals(string, id));
                    if (isExist) {
                        idList.remove(id);
                    } else {
                        idList.add(id);
                    }
                    isSelectAll = clipBoardList.size() == idList.size();
                    binding.checkboxAll.setChecked(isSelectAll);
                    gridViewAdapter.notifyDataSetChanged();
                }
            });
            binding.clipboardView.setAdapter(gridViewAdapter);
            gridViewAdapter.notifyDataSetChanged();
        }
        binding.clipboardBar.setVisibility(View.VISIBLE);
    }

    private void applyTheme() {
        if (isDarkMode) {
            binding.clipboardBackButton.setColorFilter(getContext().getColor(R.color.dark_icon));
            binding.clipboardBar.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background_dark));
            binding.clipboardLayout.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background_dark));
            binding.emptyView.setTextColor(getContext().getColor(R.color.dark_icon));
            binding.clipboardDeleteButton.setColorFilter(getContext().getColor(R.color.dark_icon));
            binding.clipDeleteButton.setColorFilter(getContext().getColor(R.color.dark_icon));
            binding.checkboxAll.setTextColor(getContext().getColor(R.color.dark_icon));
        } else {
            binding.clipboardBackButton.setColorFilter(getContext().getColor(R.color.light_intro_icon_color));
            binding.clipboardBar.setBackground(AppCompatResources.getDrawable(getContext(), R.color.keyboard_background));
            binding.clipboardLayout.setBackground(AppCompatResources.getDrawable(getContext(), R.color.emoji_view_background_light));
            binding.emptyView.setTextColor(getContext().getColor(R.color.light_intro_icon_color));
            binding.clipboardDeleteButton.setColorFilter(getContext().getColor(R.color.light_intro_icon_color));
            binding.clipDeleteButton.setColorFilter(getContext().getColor(R.color.light_intro_icon_color));
            binding.checkboxAll.setTextColor(getContext().getColor(R.color.light_intro_icon_color));
        }
    }

    public void setHeight(int height) {
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        binding.clipboardMainContainer.getLayoutParams().height = height;
        applyTheme();
        if (dbHelper == null) {
            dbHelper = new DBHelper(getContext());
        }
        load();
    }

    public void setonClipboardListner(onClipboardListner listener) {
        this.onClipboardListner = listener;
    }

    public void refreshClipboard() {
        load();
    }
}
