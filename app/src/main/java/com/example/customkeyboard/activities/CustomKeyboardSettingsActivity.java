package com.example.customkeyboard.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.customkeyboard.R;
import com.example.customkeyboard.fragments.SettingsFragment;
import com.example.customkeyboard.listners.GestureListener;
import com.example.customkeyboard.service.MyInputMethodService;

import java.util.Objects;

public class CustomKeyboardSettingsActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    FrameLayout frameLayout;
    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_keyboard_settings);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        linearLayout = findViewById(R.id.settings_layout);
        frameLayout = findViewById(R.id.idFrameLayout);
        if (findViewById(R.id.idFrameLayout) != null) {
            if (savedInstanceState != null) {
                return;
            }
            getFragmentManager().beginTransaction().add(R.id.idFrameLayout, new SettingsFragment()).commit();
        }
        applyTheme();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();
    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
        super.onBackPressed();
    }

    private void applyTheme() {
        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            linearLayout.setBackgroundColor(getColor(R.color.keyboard_background_dark));
        } else {
            linearLayout.setBackgroundColor(getColor(R.color.keyboard_background));
        }
    }
}