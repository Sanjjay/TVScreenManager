package com.kscreensavermanager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity {

    private EditText etOffCustom, etSleepCustom;
    private RadioGroup rgDaydreamApps;
    private PackageManager packageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        packageManager = getPackageManager();

        etOffCustom = findViewById(R.id.et_off_custom);
        etSleepCustom = findViewById(R.id.et_sleep_custom);
        rgDaydreamApps = findViewById(R.id.rg_daydream_apps);

        Button btnOff5m = findViewById(R.id.btn_off_5m);
        Button btnOff15m = findViewById(R.id.btn_off_15m);
        Button btnSleep30m = findViewById(R.id.btn_sleep_30m);
        Button btnSleepNever = findViewById(R.id.btn_sleep_never);
        Button btnApplyTimeouts = findViewById(R.id.btn_apply_timeouts);

        // Timers Presets (Minutes converted directly to Milliseconds)
        btnOff5m.setOnClickListener(v -> saveTimeouts(5 * 60 * 1000, -1));
        btnOff15m.setOnClickListener(v -> saveTimeouts(15 * 60 * 1000, -1));
        btnSleep30m.setOnClickListener(v -> saveTimeouts(-1, 30 * 60 * 1000));
        btnSleepNever.setOnClickListener(v -> saveTimeouts(-1, 2147483647)); 

        // Apply typing fields
        btnApplyTimeouts.setOnClickListener(v -> {
            int offMs = -1;
            int sleepMs = -1;
            try {
                if (!etOffCustom.getText().toString().isEmpty()) {
                    offMs = Integer.parseInt(etOffCustom.getText().toString()) * 60 * 1000;
                }
                if (!etSleepCustom.getText().toString().isEmpty()) {
                    sleepMs = Integer.parseInt(etSleepCustom.getText().toString()) * 60 * 1000;
                }
                saveTimeouts(offMs, sleepMs);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please type valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        // Scan and populate screen with compatible Daydream options
        discoverAndPopulateDaydreams();
    }

    private void saveTimeouts(int offMs, int sleepMs) {
        try {
            if (offMs != -1) {
                Settings.System.putInt(getContentResolver(), "screen_off_timeout", offMs);
                Settings.Secure.putInt(getContentResolver(), "contextual_screen_off_timeout", offMs);
            }
            if (sleepMs != -1) {
                Settings.Secure.putInt(getContentResolver(), "sleep_timeout", sleepMs);
            }
            Toast.makeText(this, "Timeout values saved!", Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {
            Toast.makeText(this, "Requires WRITE_SECURE_SETTINGS via ADB", Toast.LENGTH_LONG).show();
        }
    }

    private void discoverAndPopulateDaydreams() {
        // Query for services handling the base Dream Service Action
        Intent dreamIntent = new Intent("android.service.dreams.DreamService");
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(dreamIntent, 0);

        String currentActiveSaver = Settings.Secure.getString(getContentResolver(), "screensaver_components");

        if (resolveInfos == null || resolveInfos.isEmpty()) {
            RadioButton rbNone = new RadioButton(this);
            rbNone.setText("No Daydream Apps Found");
            rbNone.setTextColor(0xFF888888);
            rgDaydreamApps.addView(rbNone);
            return;
        }

        for (ResolveInfo info : resolveInfos) {
            if (info.serviceInfo != null) {
                String packageName = info.serviceInfo.packageName;
                String serviceName = info.serviceInfo.name;
                String fullComponent = packageName + "/" + serviceName;
                CharSequence appLabel = info.loadLabel(packageManager);

                RadioButton rb = new RadioButton(this);
                rb.setText(appLabel + " (" + packageName + ")");
                rb.setTextColor(0xFFFFFFFF);
                rb.setFocusable(true);
                rb.setTag(fullComponent);

                // Auto-tick if this app is already active in Android Core Settings
                if (fullComponent.equals(currentActiveSaver)) {
                    rb.setChecked(true);
                }

                // Interaction listener: Ticking a checkbox immediately locks setting
                rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        try {
                            Settings.Secure.putString(getContentResolver(), "screensaver_components", fullComponent);
                            Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 1);
                            Toast.makeText(MainActivity.this, "Set Active: " + appLabel, Toast.LENGTH_SHORT).show();
                        } catch (SecurityException se) {
                            Toast.makeText(MainActivity.this, "ADB secure settings bypass missing!", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                rgDaydreamApps.addView(rb);
            }
        }
    }
}