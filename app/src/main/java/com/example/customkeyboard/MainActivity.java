package com.example.customkeyboard;

import static com.example.customkeyboard.utils.Utils.initializeCSV;
import static com.example.customkeyboard.utils.Utils.initializeEnglishCSV;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.customkeyboard.activities.PermissionActivity;
import com.example.customkeyboard.observer.InputMethodObserver;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.FirebaseApp;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    InputMethodManager inputMethodManager;
    SharedPreferences preferences;
    InputMethodObserver inputMethodObserver;

    BroadcastReceiver permissionRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("REQUEST_PERMISSION".equals(intent.getAction())) {
                startActivity(new Intent(getApplicationContext(), PermissionActivity.class));
                finish();
            }
        }
    };

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        preferences = getSharedPreferences("SystemPreference", MODE_PRIVATE);
        inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        String list = inputMethodManager.getEnabledInputMethodList().toString();
        initializeTamilDataInBackground();

        inputMethodObserver = new InputMethodObserver(new Handler(), this, preferences);
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD), true, inputMethodObserver);

        if (!list.contains(getPackageName())) {
            Intent enableIntent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            enableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(enableIntent);
        }
        IntentFilter filter = new IntentFilter("REQUEST_PERMISSION");
        registerReceiver(permissionRequestReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    public void initializeTamilDataInBackground() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            initializeCSV(MainActivity.this);
            initializeEnglishDataInBackground();
        });
    }

    public void initializeEnglishDataInBackground() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            initializeEnglishCSV(MainActivity.this);
        });
    }


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onResume() {
        super.onResume();
        String id = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
        );
        if (!preferences.getBoolean("isKeyboardEnable", false)) {
            if (!id.contains("customkeyboard")) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        inputMethodManager.showInputMethodPicker();
                    }
                };
                new Handler().postDelayed(runnable, 1000);
            }
        }
    }
}