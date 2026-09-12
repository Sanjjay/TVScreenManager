/*
 * Copyright (C) 2026 TVScreensaverManager
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 */

package com.kscreensavermanager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private EditText etOffCustom;
    private EditText etSleepCustom;

    private RadioGroup rgDaydreamApps;

    private LinearLayout adbWarningCard;

    private TextView tvPlatform;
    private TextView tvDevice;
    private TextView tvAccessStatus;

    private TextView tvOffCurrent;
    private TextView tvOffPending;

    private TextView tvSleepCurrent;
    private TextView tvSleepPending;

    private Button btnApply;
    private Button btnPreview;

    private PackageManager packageManager;

    private boolean hasSecurePermission = false;

    private int currentOffMs = -1;
    private int currentSleepMs = -1;

    private int pendingOffMs = -1;
    private int pendingSleepMs = -1;

    private String currentDaydreamComponent = "";
    private String pendingDaydreamComponent = "";

    private TvPlatform.Type platform;

    private static final int NEVER_TIMEOUT = Integer.MAX_VALUE;

    private static final int TEAL = 0xFF65D6CC;
    private static final int AMBER = 0xFFFFB547;
    private static final int NORMAL_BUTTON = 0xFF303A47;
    private static final int DISABLED_BUTTON = 0xFF303640;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        packageManager = getPackageManager();

        initializeViews();

        platform = TvPlatform.detect(this);

        updatePlatformHeader();

        checkAdbPermission();

        loadCurrentSettings();

        setupTimeoutControls();

        setupPreviewButton();

        discoverAndPopulateDaydreams();

        updateUi();
    }


    // ================================================================
    // VIEWS
    // ================================================================

    private void initializeViews() {

        etOffCustom = findViewById(R.id.et_off_custom);
        etSleepCustom = findViewById(R.id.et_sleep_custom);

        rgDaydreamApps = findViewById(R.id.rg_daydream_apps);

        adbWarningCard = findViewById(R.id.adb_warning_card);

        tvPlatform = findViewById(R.id.tv_platform);
        tvDevice = findViewById(R.id.tv_device);

        tvAccessStatus = findViewById(R.id.tv_access_status);

        tvOffCurrent = findViewById(R.id.tv_off_current);
        tvOffPending = findViewById(R.id.tv_off_pending);

        tvSleepCurrent = findViewById(R.id.tv_sleep_current);
        tvSleepPending = findViewById(R.id.tv_sleep_pending);

        btnApply = findViewById(R.id.btn_apply_timeouts);
        btnPreview = findViewById(R.id.btn_start_screensaver);
    }


    // ================================================================
    // PLATFORM
    // ================================================================

    private void updatePlatformHeader() {

        tvPlatform.setText(
                "● " + TvPlatform.getDisplayName(platform)
        );

        String model = Build.MODEL;

        if (model == null || model.trim().isEmpty()) {
            model = "TV device";
        }

        String androidVersion = Build.VERSION.RELEASE;

        if (androidVersion == null) {
            androidVersion = "?";
        }

        tvDevice.setText(
                model + "  ·  Android " + androidVersion
        );


        if (platform == TvPlatform.Type.FIRE_TV) {

            tvPlatform.setTextColor(0xFFFFA726);

        } else {

            tvPlatform.setTextColor(TEAL);
        }
    }


    // ================================================================
    // PERMISSION
    // ================================================================

    private void checkAdbPermission() {

        try {

            Settings.Secure.getString(
                    getContentResolver(),
                    "screensaver_components"
            );

            int enabled = Settings.Secure.getInt(
                    getContentResolver(),
                    "screensaver_enabled",
                    1
            );

            Settings.Secure.putInt(
                    getContentResolver(),
                    "screensaver_enabled",
                    enabled
            );

            hasSecurePermission = true;

            adbWarningCard.setVisibility(View.GONE);

            tvAccessStatus.setText("● SYSTEM ACCESS");
            tvAccessStatus.setTextColor(TEAL);

        } catch (SecurityException e) {

            hasSecurePermission = false;

            adbWarningCard.setVisibility(View.VISIBLE);

            tvAccessStatus.setText("● ACCESS REQUIRED");
            tvAccessStatus.setTextColor(AMBER);
        }
    }


    // ================================================================
    // CURRENT SETTINGS
    // ================================================================

    private void loadCurrentSettings() {

        try {

            currentOffMs = Settings.System.getInt(
                    getContentResolver(),
                    "screen_off_timeout"
            );

        } catch (Exception e) {

            currentOffMs = -1;
        }


        if (hasSecurePermission) {

            try {

                currentSleepMs = Settings.Secure.getInt(
                        getContentResolver(),
                        "sleep_timeout"
                );

            } catch (Exception e) {

                currentSleepMs = -1;
            }

            String component =
                    Settings.Secure.getString(
                            getContentResolver(),
                            "screensaver_components"
                    );

            currentDaydreamComponent =
                    component == null ? "" : component;

        } else {

            currentSleepMs = -1;
            currentDaydreamComponent = "";
        }


        pendingOffMs = currentOffMs;
        pendingSleepMs = currentSleepMs;
        pendingDaydreamComponent = currentDaydreamComponent;
    }


    // ================================================================
    // TIMEOUT CONTROLS
    // ================================================================

    private void setupTimeoutControls() {

        Button off5 = findViewById(R.id.btn_off_5m);
        Button off15 = findViewById(R.id.btn_off_15m);

        Button sleep30 = findViewById(R.id.btn_sleep_30m);
        Button sleepNever = findViewById(R.id.btn_sleep_never);


        off5.setOnClickListener(v -> {

            pendingOffMs = 5 * 60 * 1000;

            etOffCustom.setText("");

            updateUi();
        });


        off15.setOnClickListener(v -> {

            pendingOffMs = 15 * 60 * 1000;

            etOffCustom.setText("");

            updateUi();
        });


        sleep30.setOnClickListener(v -> {

            pendingSleepMs = 30 * 60 * 1000;

            etSleepCustom.setText("");

            updateUi();
        });


        sleepNever.setOnClickListener(v -> {

            pendingSleepMs = NEVER_TIMEOUT;

            etSleepCustom.setText("");

            updateUi();
        });


        etOffCustom.setOnFocusChangeListener(
                (v, focused) -> {

                    if (!focused) {
                        readCustomOff();
                    }
                }
        );


        etSleepCustom.setOnFocusChangeListener(
                (v, focused) -> {

                    if (!focused) {
                        readCustomSleep();
                    }
                }
        );
    }


    private boolean readCustomOff() {

        String text =
                etOffCustom.getText().toString().trim();

        if (text.isEmpty()) {
            return true;
        }

        try {

            int minutes = Integer.parseInt(text);

            if (minutes <= 0) {
                showToast("Screensaver timeout must be greater than 0.");
                return false;
            }

            pendingOffMs =
                    minutes * 60 * 1000;

            updateUi();

            return true;

        } catch (NumberFormatException e) {

            showToast("Enter a valid screensaver timeout.");
            return false;
        }
    }


    private boolean readCustomSleep() {

        String text =
                etSleepCustom.getText().toString().trim();

        if (text.isEmpty()) {
            return true;
        }

        try {

            int minutes = Integer.parseInt(text);

            if (minutes <= 0) {
                showToast("Sleep timeout must be greater than 0.");
                return false;
            }

            pendingSleepMs =
                    minutes * 60 * 1000;

            updateUi();

            return true;

        } catch (NumberFormatException e) {

            showToast("Enter a valid sleep timeout.");
            return false;
        }
    }


    // ================================================================
    // UI STATE
    // ================================================================

    private void updateUi() {

        tvOffCurrent.setText(
                "Currently set to: "
                        + formatTimeout(currentOffMs)
        );

        tvSleepCurrent.setText(
                "Currently set to: "
                        + formatTimeout(currentSleepMs)
        );


        boolean offChanged =
                pendingOffMs != currentOffMs;

        boolean sleepChanged =
                pendingSleepMs != currentSleepMs;


        if (offChanged) {

            tvOffPending.setVisibility(View.VISIBLE);

            tvOffPending.setText(
                    "New setting: "
                            + formatTimeout(pendingOffMs)
            );

        } else {

            tvOffPending.setVisibility(View.GONE);
        }


        if (sleepChanged) {

            tvSleepPending.setVisibility(View.VISIBLE);

            tvSleepPending.setText(
                    "New setting: "
                            + formatTimeout(pendingSleepMs)
            );

        } else {

            tvSleepPending.setVisibility(View.GONE);
        }


        updatePresetSelection();

        updateSaveButton();
    }


    private void updatePresetSelection() {

        updateButton(
                R.id.btn_off_5m,
                pendingOffMs == 5 * 60 * 1000
        );

        updateButton(
                R.id.btn_off_15m,
                pendingOffMs == 15 * 60 * 1000
        );

        updateButton(
                R.id.btn_sleep_30m,
                pendingSleepMs == 30 * 60 * 1000
        );

        updateButton(
                R.id.btn_sleep_never,
                pendingSleepMs == NEVER_TIMEOUT
        );
    }


    private void updateButton(
            int id,
            boolean selected) {

        View view = findViewById(id);

        if (!(view instanceof Button)) {
            return;
        }

        Button button = (Button) view;

        if (selected) {

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            0xFF24545B
                    )
            );

            button.setTextColor(0xFFFFFFFF);

        } else {

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            NORMAL_BUTTON
                    )
            );

            button.setTextColor(0xFFFFFFFF);
        }
    }


    private void updateSaveButton() {

        boolean changed = hasChanges();

        if (changed) {

            btnApply.setBackgroundTintList(
                    ColorStateList.valueOf(AMBER)
            );

            btnApply.setTextColor(0xFF111820);

            btnApply.setText("✓  SAVE CHANGES");

            btnApply.setEnabled(true);

        } else {

            btnApply.setBackgroundTintList(
                    ColorStateList.valueOf(DISABLED_BUTTON)
            );

            btnApply.setTextColor(0xFF777F89);

            btnApply.setText("SAVE CHANGES");

            btnApply.setEnabled(false);
        }
    }


    private boolean hasChanges() {

        return pendingOffMs != currentOffMs
                || pendingSleepMs != currentSleepMs
                || !pendingDaydreamComponent.equals(
                currentDaydreamComponent
        );
    }


    // ================================================================
    // SAVE
    // ================================================================

    private void setupPreviewButton() {

        btnPreview.setOnClickListener(
                v -> previewScreensaver()
        );


        btnApply.setOnClickListener(
                v -> saveSettings()
        );
    }


    private void saveSettings() {

        if (!hasSecurePermission) {

            showPermissionToast();
            return;
        }


        if (!readCustomOff() ||
                !readCustomSleep()) {

            return;
        }


        if (!hasChanges()) {
            return;
        }


        try {

            if (pendingOffMs != currentOffMs
                    && pendingOffMs >= 0) {

                Settings.System.putInt(
                        getContentResolver(),
                        "screen_off_timeout",
                        pendingOffMs
                );

                try {

                    Settings.Secure.putInt(
                            getContentResolver(),
                            "contextual_screen_off_timeout",
                            pendingOffMs
                    );

                } catch (Exception ignored) {
                }
            }


            if (pendingSleepMs != currentSleepMs
                    && pendingSleepMs >= 0) {

                Settings.Secure.putInt(
                        getContentResolver(),
                        "sleep_timeout",
                        pendingSleepMs
                );
            }


            if (!pendingDaydreamComponent.equals(
                    currentDaydreamComponent)) {

                Settings.Secure.putString(
                        getContentResolver(),
                        "screensaver_components",
                        pendingDaydreamComponent
                );

                Settings.Secure.putInt(
                        getContentResolver(),
                        "screensaver_enabled",
                        1
                );
            }


            currentOffMs = pendingOffMs;
            currentSleepMs = pendingSleepMs;
            currentDaydreamComponent =
                    pendingDaydreamComponent;


            updateUi();

            showToast("Settings saved successfully.");

        } catch (SecurityException e) {

            checkAdbPermission();

            showPermissionToast();

        } catch (Exception e) {

            showToast(
                    "Unable to save settings."
            );
        }
    }


    // ================================================================
    // DAYDREAM DISCOVERY
    // ================================================================

    private void discoverAndPopulateDaydreams() {

        rgDaydreamApps.removeAllViews();

        Intent intent =
                new Intent("android.service.dreams.DreamService");

        List<ResolveInfo> services =
                packageManager.queryIntentServices(
                        intent,
                        PackageManager.GET_META_DATA
                );


        if (services == null || services.isEmpty()) {

            TextView empty = new TextView(this);

            empty.setText(
                    "No compatible screensavers were found."
            );

            empty.setTextColor(0xFF8995A5);
            empty.setTextSize(14);
            empty.setPadding(18, 18, 18, 18);

            rgDaydreamApps.addView(empty);

            return;
        }


        for (ResolveInfo info : services) {

            if (info.serviceInfo == null) {
                continue;
            }

            String packageName =
                    info.serviceInfo.packageName;

            String serviceName =
                    info.serviceInfo.name;

            String component =
                    packageName + "/" + serviceName;

            CharSequence label =
                    info.loadLabel(packageManager);


            RadioButton row =
                    new RadioButton(this);

            row.setText(
                    label
                            + "\n"
                            + packageName
            );

            row.setTextColor(0xFFF5F7FA);
            row.setTextSize(14);

            row.setButtonTintList(
                    new ColorStateList(
                            new int[][]{
                                    new int[]{
                                            android.R.attr.state_checked
                                    },
                                    new int[]{}
                            },
                            new int[]{
                                    TEAL,
                                    0xFF687383
                            }
                    )
            );

            row.setPadding(
                    14,
                    9,
                    14,
                    9
            );

            row.setMinHeight(58);

            row.setFocusable(true);
            row.setFocusableInTouchMode(true);

            row.setTag(component);


            if (component.equals(
                    pendingDaydreamComponent)) {

                row.setChecked(true);
            }


            row.setOnClickListener(v -> {

                if (!hasSecurePermission) {

                    showPermissionToast();

                    row.setChecked(false);

                    return;
                }


                pendingDaydreamComponent =
                        component;

                updateDaydreamRows();

                updateSaveButton();
            });


            rgDaydreamApps.addView(row);
        }


        updateDaydreamRows();
    }


    private void updateDaydreamRows() {

        for (int i = 0;
             i < rgDaydreamApps.getChildCount();
             i++) {

            View view =
                    rgDaydreamApps.getChildAt(i);

            if (!(view instanceof RadioButton)) {
                continue;
            }

            RadioButton row =
                    (RadioButton) view;

            String component =
                    String.valueOf(row.getTag());

            if (component.equals(
                    pendingDaydreamComponent)) {

                row.setBackgroundColor(
                        0xFF17363E
                );

            } else {

                row.setBackgroundColor(
                        0x00151C27
                );
            }
        }
    }


    // ================================================================
    // PREVIEW
    // ================================================================

    private void previewScreensaver() {

        if (!hasSecurePermission) {

            showPermissionToast();
            return;
        }


        /*
         * First ensure the selected screensaver is active.
         *
         * We deliberately don't pretend that merely changing
         * screensaver_enabled starts the DreamService.
         */

        try {

            if (!pendingDaydreamComponent.equals(
                    currentDaydreamComponent)) {

                Settings.Secure.putString(
                        getContentResolver(),
                        "screensaver_components",
                        pendingDaydreamComponent
                );

                Settings.Secure.putInt(
                        getContentResolver(),
                        "screensaver_enabled",
                        1
                );

                currentDaydreamComponent =
                        pendingDaydreamComponent;
            }


            /*
             * Android/Google TV:
             *
             * Try the system Dream preview/settings activity.
             */

            Intent dreamSettings =
                    new Intent(
                            "android.settings.DREAM_SETTINGS"
                    );

            if (dreamSettings.resolveActivity(
                    packageManager) != null) {

                startActivity(dreamSettings);

                showToast(
                        "Opening screensaver controls..."
                );

                return;
            }


            /*
             * Fire TV does not guarantee the Android Dream
             * settings activity.
             */

            if (platform ==
                    TvPlatform.Type.FIRE_TV) {

                showToast(
                        "Fire TV controls this screensaver through Fire OS."
                );

                return;
            }


            showToast(
                    "Screensaver preview is not available on this device."
            );

        } catch (Exception e) {

            showToast(
                    "Unable to open screensaver preview."
            );
        }
    }


    // ================================================================
    // HELPERS
    // ================================================================

    private String formatTimeout(int milliseconds) {

        if (milliseconds < 0) {
            return "Unavailable";
        }

        if (milliseconds == NEVER_TIMEOUT) {
            return "Never";
        }

        long minutes =
                milliseconds / 60000L;

        if (minutes == 1) {
            return "1 minute";
        }

        if (minutes < 60) {
            return minutes + " minutes";
        }

        long hours = minutes / 60;
        long remaining = minutes % 60;

        if (remaining == 0) {

            if (hours == 1) {
                return "1 hour";
            }

            return hours + " hours";
        }

        return hours + "h " + remaining + "m";
    }


    private void showPermissionToast() {

        Toast.makeText(
                this,
                "Run the ADB permission command shown above.",
                Toast.LENGTH_LONG
        ).show();
    }


    private void showToast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }


    @Override
    protected void onResume() {

        super.onResume();

        if (etOffCustom == null) {
            return;
        }

        boolean hadChanges = hasChanges();

        checkAdbPermission();

        if (!hadChanges) {
            loadCurrentSettings();
        }

        updateUi();
    }
}
