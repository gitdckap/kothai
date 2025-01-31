package com.example.customkeyboard.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.View;
import android.widget.Toast;

import com.example.customkeyboard.R;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;

import timber.log.Timber;

public class SettingsFragment extends PreferenceFragment {

    private AppUpdateManager appUpdateManager;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        appUpdateManager = AppUpdateManagerFactory.create(getContext());

        PreferenceCategory suggestionGeneral = (PreferenceCategory) findPreference("suggestion_general");
        PreferenceCategory layoutGeneral = (PreferenceCategory) findPreference("layout_general");
        PreferenceCategory updateGeneral = (PreferenceCategory) findPreference("update_general");
        SwitchPreference dictionarySuggestion = (SwitchPreference) findPreference("dictionary_suggestion");
        SwitchPreference autoCorrection = (SwitchPreference) findPreference("auto_correction");
        SwitchPreference keyPreview = (SwitchPreference) findPreference("key_preview");
        SwitchPreference vibrateKeyPress = (SwitchPreference) findPreference("vibrate_keyPress");
        SwitchPreference soundKeyPress = (SwitchPreference) findPreference("sound_keyPress");
        Preference update = (Preference) findPreference("update_check");

        update.setOnPreferenceClickListener(preference -> {
            checkForAppUpdate(null);
            return true;
        });

        suggestionGeneral.setLayoutResource(R.layout.custom_preference_category);
        layoutGeneral.setLayoutResource(R.layout.custom_preference_category);
        updateGeneral.setLayoutResource(R.layout.custom_preference_category);
        checkForAppUpdate(update);

        if (sharedPreferences.getBoolean("isDarkMode", false)) {
            dictionarySuggestion.setLayoutResource(R.layout.custom_switch_preference_dark);
            autoCorrection.setLayoutResource(R.layout.custom_switch_preference_dark);
            keyPreview.setLayoutResource(R.layout.custom_switch_preference_dark);
            vibrateKeyPress.setLayoutResource(R.layout.custom_switch_preference_dark);
            soundKeyPress.setLayoutResource(R.layout.custom_switch_preference_dark);
            update.setLayoutResource(R.layout.custom_preference_dark);
        } else {
            dictionarySuggestion.setLayoutResource(R.layout.custom_switch_preference_light);
            autoCorrection.setLayoutResource(R.layout.custom_switch_preference_light);
            keyPreview.setLayoutResource(R.layout.custom_switch_preference_light);
            vibrateKeyPress.setLayoutResource(R.layout.custom_switch_preference_light);
            soundKeyPress.setLayoutResource(R.layout.custom_switch_preference_light);
            update.setLayoutResource(R.layout.custom_preference_light);
        }
    }

    private void checkForAppUpdate(Preference update) {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (update != null) {
                    update.setIcon(AppCompatResources.getDrawable(getContext(), R.drawable.ic_update));
                } else {
                    startAppUpdate();
                }
            } else {
                Toast.makeText(getContext(), "Your app is up-to-date!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Timber.e(e, "Error checking for app updates");
        });
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void startAppUpdate() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getContext().getPackageName()));
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getContext().getPackageName()));
            startActivity(webIntent);
        }
    }

}