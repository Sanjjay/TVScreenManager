package com.kscreensavermanager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.List;

public class MainActivity extends Activity {

    private EditText etOffCustom, etSleepCustom;
    private RadioGroup rgDaydreamApps;
    private LinearLayout adbWarningCard;
    private PackageManager packageManager;
    private boolean hasSecurePermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        packageManager = getPackageManager();

        etOffCustom = findViewById(R.id.et_off_custom);
        etSleepCustom = findViewById(R.id.et_sleep_custom);
        rgDaydreamApps = findViewById(R.id.rg_daydream_apps);
        adbWarningCard = findViewById(R.id.adb_warning_card);

        Button btnOff5m = findViewById(R.id.btn_off_5m);
        Button btnOff15m = findViewById(R.id.btn_off_15m);
        Button btnSleep30m = findViewById(R.id.btn_sleep_30m);
        Button btnSleepNever = findViewById(R.id.btn_sleep_never);
        Button btnApplyTimeouts = findViewById(R.id.btn_apply_timeouts);

        // CHECK PERMISSION STATUS RIGHT AT THE START
        checkAdbPermission();

        btnOff5m.setOnClickListener(v -> saveTimeouts(5 * 60 * 1000, -1));
        btnOff15m.setOnClickListener(v -> saveTimeouts(15 * 60 * 1000, -1));
        btnSleep30m.setOnClickListener(v -> saveTimeouts(-1, 30 * 60 * 1000));
        btnSleepNever.setOnClickListener(v -> saveTimeouts(-1, 2147483647)); 

        btnApplyTimeouts.setOnClickListener(v -> {
            if (!hasSecurePermission) {
                showPermissionToast();
                return;
            }
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

        discoverAndPopulateDaydreams();
    }

    private void checkAdbPermission() {
        try {
            // Test if we can read the variable (standard permission check)
            Settings.Secure.getString(getContentResolver(), "screensaver_components");
            // Run a dummy write to guarantee we actually have edit rights
            int currentVal = Settings.Secure.getInt(getContentResolver(), "screensaver_enabled", 1);
            Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", currentVal);
            
            // If we get here, permission is granted! Hide the card.
            adbWarningCard.setVisibility(View.GONE);
            hasSecurePermission = true;
        } catch (SecurityException se) {
            // Permission is NOT granted. Show the big red card with the command!
            adbWarningCard.setVisibility(View.VISIBLE);
            hasSecurePermission = false;
        }
    }

    private void showPermissionToast() {
        Toast.makeText(this, "ERROR: Run the ADB command shown at the top of the screen!", Toast.LENGTH_LONG).show();
    }

    private void saveTimeouts(int offMs, int sleepMs) {
        if (!hasSecurePermission) {
            showPermissionToast();
            return;
        }
        try {
            if (offMs != -1) {
                Settings.System.putInt(getContentResolver(), "screen_off_timeout", offMs);
                Settings.Secure.putInt(getContentResolver(), "contextual_screen_off_timeout", offMs);
            }
            if (sleepMs != -1) {
                Settings.Secure.putInt(getContentResolver(), "sleep_timeout", sleepMs);
            }
            Toast.makeText(this, "Timeout values saved!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void discoverAndPopulateDaydreams() {
        Intent dreamIntent = new Intent("android.service.dreams.DreamService");
        List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(dreamIntent, 0);

        String currentActiveSaver = "";
        if (hasSecurePermission) {
            currentActiveSaver = Settings.Secure.getString(getContentResolver(), "screensaver_components");
        }

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

                if (fullComponent.equals(currentActiveSaver)) {
                    rb.setChecked(true);
                }

                rb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        if (!hasSecurePermission) {
                            rb.setChecked(false);
                            showPermissionToast();
                            return;
                        }
                        try {
                            Settings.Secure.putString(getContentResolver(), "screensaver_components", fullComponent);
                            Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", 1);
                            Toast.makeText(MainActivity.this, "Set Active: " + appLabel, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Error setting screensaver", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                rgDaydreamApps.addView(rb);
            }
        }
    }
}
