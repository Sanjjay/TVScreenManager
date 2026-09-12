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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    // ---------------------------------------------------------------
    // Views
    // ---------------------------------------------------------------

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

    /*
     * Shows the currently applied screensaver next to:
     * "Choose which screensaver to use"
     */
    private TextView tvCurrentScreensaver;

    private Button btnApply;

    // ---------------------------------------------------------------
    // System
    // ---------------------------------------------------------------

    private PackageManager packageManager;

    private boolean hasSecurePermission = false;

    // Current values actually stored on the device.
    private int currentOffMs = -1;
    private int currentSleepMs = -1;

    // Values selected by the user but not necessarily saved yet.
    private int pendingOffMs = -1;
    private int pendingSleepMs = -1;

    private String currentDaydreamComponent = "";
    private String pendingDaydreamComponent = "";

    private TvPlatform.Type platform;

    // ---------------------------------------------------------------
    // Constants
    // ---------------------------------------------------------------

    private static final int NEVER_TIMEOUT = Integer.MAX_VALUE;

    private static final int TEAL = 0xFF65D6CC;
    private static final int AMBER = 0xFFFFB547;

    private static final int NORMAL_BUTTON = 0xFF303A47;
    private static final int SELECTED_BUTTON = 0xFF24545B;
    private static final int DISABLED_BUTTON = 0xFF303640;

    // ---------------------------------------------------------------
    // Activity
    // ---------------------------------------------------------------

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

        setupSaveButton();

        discoverAndPopulateDaydreams();

        updateUi();
    }

    // ===============================================================
    // VIEWS
    // ===============================================================

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

        /*
         * This TextView must exist in activity_main.xml.
         *
         * It displays:
         * "Currently applied: <app name>"
         */
        tvCurrentScreensaver =
                findViewById(R.id.tv_current_screensaver);

        btnApply =
                findViewById(R.id.btn_apply_timeouts);
    }

    // ===============================================================
    // PLATFORM
    // ===============================================================

    private void updatePlatformHeader() {

        if (tvPlatform == null || tvDevice == null) {
            return;
        }

        tvPlatform.setText(
                "● " + TvPlatform.getDisplayName(platform)
        );

        String model = Build.MODEL;

        if (model == null || model.trim().isEmpty()) {
            model = "TV device";
        }

        String androidVersion = Build.VERSION.RELEASE;

        if (androidVersion == null ||
                androidVersion.trim().isEmpty()) {

            androidVersion = "?";
        }

        tvDevice.setText(
                model + "  ·  Android " + androidVersion
        );

        if (platform == TvPlatform.Type.FIRE_TV) {

            tvPlatform.setTextColor(
                    0xFFFFA726
            );

        } else {

            tvPlatform.setTextColor(
                    TEAL
            );
        }
    }

    // ===============================================================
    // ADB / SECURE SETTINGS PERMISSION
    // ===============================================================

    private void checkAdbPermission() {

        try {

            /*
             * Read first.
             */
            Settings.Secure.getString(
                    getContentResolver(),
                    "screensaver_components"
            );

            int enabled =
                    Settings.Secure.getInt(
                            getContentResolver(),
                            "screensaver_enabled",
                            1
                    );

            /*
             * Intentional write.
             *
             * This is what actually confirms that
             * WRITE_SECURE_SETTINGS is available.
             */
            Settings.Secure.putInt(
                    getContentResolver(),
                    "screensaver_enabled",
                    enabled
            );

            hasSecurePermission = true;

            if (adbWarningCard != null) {

                adbWarningCard.setVisibility(
                        View.GONE
                );
            }

            if (tvAccessStatus != null) {

                tvAccessStatus.setText(
                        "● SYSTEM ACCESS"
                );

                tvAccessStatus.setTextColor(
                        TEAL
                );
            }

        } catch (SecurityException e) {

            hasSecurePermission = false;

            if (adbWarningCard != null) {

                adbWarningCard.setVisibility(
                        View.VISIBLE
                );
            }

            if (tvAccessStatus != null) {

                tvAccessStatus.setText(
                        "● ACCESS REQUIRED"
                );

                tvAccessStatus.setTextColor(
                        AMBER
                );
            }
        }
    }

    // ===============================================================
    // LOAD CURRENT SETTINGS
    // ===============================================================

    private void loadCurrentSettings() {

        // -----------------------------------------------------------
        // Screen-off / screensaver timeout
        // -----------------------------------------------------------

        try {

            currentOffMs =
                    Settings.System.getInt(
                            getContentResolver(),
                            "screen_off_timeout"
                    );

        } catch (Exception e) {

            currentOffMs = -1;
        }

        // -----------------------------------------------------------
        // Secure settings
        // -----------------------------------------------------------

        if (hasSecurePermission) {

            // -------------------------------------------------------
            // System sleep timeout
            // -------------------------------------------------------

            try {

                currentSleepMs =
                        Settings.Secure.getInt(
                                getContentResolver(),
                                "sleep_timeout"
                        );

            } catch (Exception e) {

                currentSleepMs = -1;
            }

            // -------------------------------------------------------
            // Current screensaver component
            // -------------------------------------------------------

            try {

                String component =
                        Settings.Secure.getString(
                                getContentResolver(),
                                "screensaver_components"
                        );

                currentDaydreamComponent =
                        component == null
                                ? ""
                                : component;

            } catch (Exception e) {

                currentDaydreamComponent = "";
            }

        } else {

            currentSleepMs = -1;

            currentDaydreamComponent = "";
        }

        // -----------------------------------------------------------
        // Initially no unsaved changes
        // -----------------------------------------------------------

        pendingOffMs =
                currentOffMs;

        pendingSleepMs =
                currentSleepMs;

        pendingDaydreamComponent =
                currentDaydreamComponent;
    }

    // ===============================================================
    // TIMEOUT CONTROLS
    // ===============================================================

    private void setupTimeoutControls() {

        Button off5 =
                findViewById(
                        R.id.btn_off_5m
                );

        Button off15 =
                findViewById(
                        R.id.btn_off_15m
                );

        Button sleep30 =
                findViewById(
                        R.id.btn_sleep_30m
                );

        Button sleepNever =
                findViewById(
                        R.id.btn_sleep_never
                );

        // -----------------------------------------------------------
        // Screensaver 5 minutes
        // -----------------------------------------------------------

        off5.setOnClickListener(v -> {

            pendingOffMs =
                    5 * 60 * 1000;

            etOffCustom.setText("");

            updateUi();
        });

        // -----------------------------------------------------------
        // Screensaver 15 minutes
        // -----------------------------------------------------------

        off15.setOnClickListener(v -> {

            pendingOffMs =
                    15 * 60 * 1000;

            etOffCustom.setText("");

            updateUi();
        });

        // -----------------------------------------------------------
        // Sleep 30 minutes
        // -----------------------------------------------------------

        sleep30.setOnClickListener(v -> {

            pendingSleepMs =
                    30 * 60 * 1000;

            etSleepCustom.setText("");

            updateUi();
        });

        // -----------------------------------------------------------
        // Sleep never
        // -----------------------------------------------------------

        sleepNever.setOnClickListener(v -> {

            pendingSleepMs =
                    NEVER_TIMEOUT;

            etSleepCustom.setText("");

            updateUi();
        });

        // -----------------------------------------------------------
        // Custom screensaver timeout
        // -----------------------------------------------------------

        etOffCustom.setOnFocusChangeListener(
                (v, focused) -> {

                    if (!focused) {

                        readCustomOff();
                    }
                }
        );

        // -----------------------------------------------------------
        // Custom sleep timeout
        // -----------------------------------------------------------

        etSleepCustom.setOnFocusChangeListener(
                (v, focused) -> {

                    if (!focused) {

                        readCustomSleep();
                    }
                }
        );
    }

    // ===============================================================
    // CUSTOM TIMEOUT INPUT
    // ===============================================================

    private boolean readCustomOff() {

        String text =
                etOffCustom
                        .getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {
            return true;
        }

        try {

            int minutes =
                    Integer.parseInt(text);

            if (minutes <= 0) {

                showToast(
                        "Screensaver timeout must be greater than 0."
                );

                return false;
            }

            if (minutes >
                    Integer.MAX_VALUE / 60000) {

                showToast(
                        "Screensaver timeout is too large."
                );

                return false;
            }

            pendingOffMs =
                    minutes * 60 * 1000;

            updateUi();

            return true;

        } catch (NumberFormatException e) {

            showToast(
                    "Enter a valid screensaver timeout."
            );

            return false;
        }
    }

    private boolean readCustomSleep() {

        String text =
                etSleepCustom
                        .getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {
            return true;
        }

        try {

            int minutes =
                    Integer.parseInt(text);

            if (minutes <= 0) {

                showToast(
                        "Sleep timeout must be greater than 0."
                );

                return false;
            }

            if (minutes >
                    Integer.MAX_VALUE / 60000) {

                showToast(
                        "Sleep timeout is too large."
                );

                return false;
            }

            pendingSleepMs =
                    minutes * 60 * 1000;

            updateUi();

            return true;

        } catch (NumberFormatException e) {

            showToast(
                    "Enter a valid sleep timeout."
            );

            return false;
        }
    }

    // ===============================================================
    // UI STATE
    // ===============================================================

    private void updateUi() {

        if (tvOffCurrent == null ||
                tvSleepCurrent == null) {

            return;
        }

        // -----------------------------------------------------------
        // Current values
        // -----------------------------------------------------------

        tvOffCurrent.setText(
                "Currently set to: "
                        + formatTimeout(
                        currentOffMs
                )
        );

        tvSleepCurrent.setText(
                "Currently set to: "
                        + formatTimeout(
                        currentSleepMs
                )
        );

        // -----------------------------------------------------------
        // Pending screensaver timeout
        // -----------------------------------------------------------

        boolean offChanged =
                pendingOffMs != currentOffMs;

        if (offChanged) {

            tvOffPending.setVisibility(
                    View.VISIBLE
            );

            tvOffPending.setText(
                    "New setting: "
                            + formatTimeout(
                            pendingOffMs
                    )
            );

        } else {

            tvOffPending.setVisibility(
                    View.GONE
            );
        }

        // -----------------------------------------------------------
        // Pending sleep timeout
        // -----------------------------------------------------------

        boolean sleepChanged =
                pendingSleepMs != currentSleepMs;

        if (sleepChanged) {

            tvSleepPending.setVisibility(
                    View.VISIBLE
            );

            tvSleepPending.setText(
                    "New setting: "
                            + formatTimeout(
                            pendingSleepMs
                    )
            );

        } else {

            tvSleepPending.setVisibility(
                    View.GONE
            );
        }

        // -----------------------------------------------------------
        // Current screensaver name
        // -----------------------------------------------------------

        updateCurrentScreensaverLabel();

        // -----------------------------------------------------------
        // Preset selection
        // -----------------------------------------------------------

        updatePresetSelection();

        // -----------------------------------------------------------
        // Screensaver rows
        // -----------------------------------------------------------

        updateDaydreamRows();

        // -----------------------------------------------------------
        // Save button
        // -----------------------------------------------------------

        updateSaveButton();
    }

    // ===============================================================
    // CURRENT SCREENSAVER LABEL
    // ===============================================================

    private void updateCurrentScreensaverLabel() {

        if (tvCurrentScreensaver == null) {
            return;
        }

        /*
         * The label represents the setting currently stored on the
         * device, NOT the pending selection.
         */
        String currentName =
                getScreensaverDisplayName(
                        currentDaydreamComponent
                );

        if (currentName == null ||
                currentName.trim().isEmpty()) {

            tvCurrentScreensaver.setText(
                    "Currently applied: None"
            );

            tvCurrentScreensaver.setTextColor(
                    0xFF8995A5
            );

        } else {

            tvCurrentScreensaver.setText(
                    "Currently applied: "
                            + currentName
            );

            tvCurrentScreensaver.setTextColor(
                    TEAL
            );
        }
    }

    // ===============================================================
    // GET SCREENSAVER DISPLAY NAME
    // ===============================================================

    private String getScreensaverDisplayName(
            String component
    ) {

        if (component == null ||
                component.trim().isEmpty()) {

            return "";
        }

        try {

            android.content.ComponentName componentName =
                    android.content.ComponentName
                            .unflattenFromString(
                                    component
                            );

            if (componentName == null) {
                return component;
            }

            android.content.pm.ServiceInfo serviceInfo =
                    packageManager.getServiceInfo(
                            componentName,
                            0
                    );

            if (serviceInfo != null) {

                CharSequence label =
                        serviceInfo.loadLabel(
                                packageManager
                        );

                if (label != null) {

                    String name =
                            label.toString().trim();

                    if (!name.isEmpty()) {
                        return name;
                    }
                }
            }

        } catch (Exception ignored) {
        }

        /*
         * Fallback: use the service class name rather than showing
         * nothing if the package/service can no longer be resolved.
         */
        int slash =
                component.indexOf('/');

        if (slash >= 0 &&
                slash + 1 < component.length()) {

            String service =
                    component.substring(
                            slash + 1
                    );

            int lastDot =
                    service.lastIndexOf('.');

            if (lastDot >= 0 &&
                    lastDot + 1 < service.length()) {

                service =
                        service.substring(
                                lastDot + 1
                        );
            }

            return service;
        }

        return component;
    }

    // ===============================================================
    // PRESET HIGHLIGHTING
    // ===============================================================

    private void updatePresetSelection() {

        updateButton(
                R.id.btn_off_5m,
                pendingOffMs ==
                        5 * 60 * 1000
        );

        updateButton(
                R.id.btn_off_15m,
                pendingOffMs ==
                        15 * 60 * 1000
        );

        updateButton(
                R.id.btn_sleep_30m,
                pendingSleepMs ==
                        30 * 60 * 1000
        );

        updateButton(
                R.id.btn_sleep_never,
                pendingSleepMs ==
                        NEVER_TIMEOUT
        );
    }

    // ===============================================================
    // BUTTON APPEARANCE
    // ===============================================================

    private void updateButton(
            int id,
            boolean selected
    ) {

        View view =
                findViewById(id);

        if (!(view instanceof Button)) {
            return;
        }

        Button button =
                (Button) view;

        /*
         * Keep the normal drawable so the focus selector from
         * bg_tv_button.xml / bg_tv_button_focus.xml can operate.
         *
         * We only tint the normal state here.
         */
        if (selected) {

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            SELECTED_BUTTON
                    )
            );

        } else {

            button.setBackgroundTintList(
                    ColorStateList.valueOf(
                            NORMAL_BUTTON
                    )
            );
        }

        button.setTextColor(
                0xFFFFFFFF
        );
    }

    // ===============================================================
    // SAVE BUTTON
    // ===============================================================

    private void updateSaveButton() {

        if (btnApply == null) {
            return;
        }

        boolean changed =
                hasChanges();

        if (changed) {

            btnApply.setBackgroundTintList(
                    ColorStateList.valueOf(
                            AMBER
                    )
            );

            btnApply.setTextColor(
                    0xFF111820
            );

            btnApply.setText(
                    "✓  SAVE CHANGES"
            );

            btnApply.setEnabled(true);

        } else {

            btnApply.setBackgroundTintList(
                    ColorStateList.valueOf(
                            DISABLED_BUTTON
                    )
            );

            btnApply.setTextColor(
                    0xFF777F89
            );

            btnApply.setText(
                    "SAVE CHANGES"
            );

            btnApply.setEnabled(false);
        }
    }

    // ===============================================================
    // HAS CHANGES
    // ===============================================================

    private boolean hasChanges() {

        return pendingOffMs != currentOffMs
                || pendingSleepMs != currentSleepMs
                || !pendingDaydreamComponent.equals(
                        currentDaydreamComponent
                );
    }

    // ===============================================================
    // SAVE BUTTON
    // ===============================================================

    private void setupSaveButton() {

        if (btnApply == null) {
            return;
        }

        btnApply.setOnClickListener(
                v -> saveSettings()
        );
    }

    // ===============================================================
    // SAVE SETTINGS
    // ===============================================================

    private void saveSettings() {

        if (!hasSecurePermission) {

            showPermissionToast();

            return;
        }

        // -----------------------------------------------------------
        // Read custom fields before saving
        // -----------------------------------------------------------

        if (!readCustomOff() ||
                !readCustomSleep()) {

            return;
        }

        if (!hasChanges()) {
            return;
        }

        try {

            // -------------------------------------------------------
            // Screen-off / screensaver timeout
            // -------------------------------------------------------

            if (pendingOffMs != currentOffMs &&
                    pendingOffMs >= 0) {

                Settings.System.putInt(
                        getContentResolver(),
                        "screen_off_timeout",
                        pendingOffMs
                );

                /*
                 * Some TV firmware uses this additional value.
                 * Failure is intentionally ignored.
                 */
                try {

                    Settings.Secure.putInt(
                            getContentResolver(),
                            "contextual_screen_off_timeout",
                            pendingOffMs
                    );

                } catch (Exception ignored) {
                }
            }

            // -------------------------------------------------------
            // Sleep timeout
            // -------------------------------------------------------

            if (pendingSleepMs != currentSleepMs &&
                    pendingSleepMs >= 0) {

                Settings.Secure.putInt(
                        getContentResolver(),
                        "sleep_timeout",
                        pendingSleepMs
                );
            }

            // -------------------------------------------------------
            // Screensaver application
            // -------------------------------------------------------

            if (!pendingDaydreamComponent.equals(
                    currentDaydreamComponent
            )) {

                if (!pendingDaydreamComponent.isEmpty()) {

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
            }

            // -------------------------------------------------------
            // Update current values
            // -------------------------------------------------------

            currentOffMs =
                    pendingOffMs;

            currentSleepMs =
                    pendingSleepMs;

            currentDaydreamComponent =
                    pendingDaydreamComponent;

            updateUi();

            showToast(
                    "Settings saved successfully."
            );

        } catch (SecurityException e) {

            checkAdbPermission();

            showPermissionToast();

        } catch (Exception e) {

            showToast(
                    "Unable to save settings."
            );
        }
    }

    // ===============================================================
    // SCREENSAVER DISCOVERY
    // ===============================================================

    private void discoverAndPopulateDaydreams() {

        if (rgDaydreamApps == null) {
            return;
        }

        rgDaydreamApps.removeAllViews();

        Intent intent =
                new Intent(
                        "android.service.dreams.DreamService"
                );

        List<ResolveInfo> services =
                packageManager.queryIntentServices(
                        intent,
                        PackageManager.GET_META_DATA
                );

        if (services == null ||
                services.isEmpty()) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "No compatible screensavers were found."
            );

            empty.setTextColor(
                    0xFF8995A5
            );

            empty.setTextSize(
                    14
            );

            empty.setPadding(
                    18,
                    18,
                    18,
                    18
            );

            rgDaydreamApps.addView(
                    empty
            );

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
                    packageName
                            + "/"
                            + serviceName;

            CharSequence label =
                    info.loadLabel(
                            packageManager
                    );

            RadioButton row =
                    new RadioButton(this);

            row.setText(
                    label
                            + "\n"
                            + packageName
            );

            row.setTextColor(
                    0xFFF5F7FA
            );

            row.setTextSize(
                    14
            );

            row.setButtonTintList(
                    new ColorStateList(
                            new int[][]{
                                    new int[]{
                                            android.R.attr.state_checked
                                    },
                                    new int[]{
                                            android.R.attr.state_focused
                                    },
                                    new int[]{}
                            },
                            new int[]{
                                    TEAL,
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

            /*
             * Keep rows compact enough to fit approximately five
             * entries on the TV screen.
             */
            row.setMinHeight(
                    58
            );

            row.setFocusable(
                    true
            );

            row.setFocusableInTouchMode(
                    true
            );

            row.setTag(
                    component
            );

            /*
             * Use the drawable created for navigation focus.
             *
             * The drawable should contain your normal/focused states.
             */
            row.setBackgroundResource(
                    R.drawable.bg_daydream_row
            );

            if (component.equals(
                    pendingDaydreamComponent
            )) {

                row.setChecked(
                        true
                );
            }

            row.setOnClickListener(
                    v -> {

                        if (!hasSecurePermission) {

                            showPermissionToast();

                            row.setChecked(
                                    false
                            );

                            return;
                        }

                        pendingDaydreamComponent =
                                component;

                        updateDaydreamRows();

                        updateUi();
                    }
            );

            rgDaydreamApps.addView(
                    row
            );
        }

        updateDaydreamRows();
    }

    // ===============================================================
    // SCREENSAVER ROW HIGHLIGHTING
    // ===============================================================

    private void updateDaydreamRows() {

        if (rgDaydreamApps == null) {
            return;
        }

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
                    String.valueOf(
                            row.getTag()
                    );

            boolean selected =
                    component.equals(
                            pendingDaydreamComponent
                    );

            row.setChecked(
                    selected
            );

            /*
             * Do not manually set a solid background here.
             *
             * bg_daydream_row.xml is responsible for:
             * - normal
             * - focused
             * - selected
             * - selected + focused
             *
             * This allows the TV remote focus border to remain visible.
             */
            row.setBackgroundResource(
                    R.drawable.bg_daydream_row
            );
        }
    }

    // ===============================================================
    // FORMAT TIMEOUT
    // ===============================================================

    private String formatTimeout(
            int milliseconds
    ) {

        if (milliseconds < 0) {
            return "Unavailable";
        }

        if (milliseconds ==
                NEVER_TIMEOUT) {

            return "Never";
        }

        long minutes =
                milliseconds / 60000L;

        if (minutes <= 0) {
            return "Less than 1 minute";
        }

        if (minutes == 1) {
            return "1 minute";
        }

        if (minutes < 60) {

            return minutes
                    + " minutes";
        }

        long hours =
                minutes / 60;

        long remaining =
                minutes % 60;

        if (remaining == 0) {

            if (hours == 1) {
                return "1 hour";
            }

            return hours
                    + " hours";
        }

        return hours
                + "h "
                + remaining
                + "m";
    }

    // ===============================================================
    // TOASTS
    // ===============================================================

    private void showPermissionToast() {

        Toast.makeText(
                this,
                "Run the ADB permission command shown above.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // ===============================================================
    // RESUME
    // ===============================================================

    @Override
    protected void onResume() {

        super.onResume();

        /*
         * During initial Activity creation the views may not yet
         * have been initialized.
         */
        if (etOffCustom == null) {
            return;
        }

        /*
         * Remember whether the user has unsaved changes.
         */
        boolean hadChanges =
                hasChanges();

        /*
         * Re-check WRITE_SECURE_SETTINGS.
         *
         * This is important because the user can grant the ADB
         * permission while the app is already open.
         */
        checkAdbPermission();

        /*
         * Once permission has been granted, reload the current
         * settings and screensaver list.
         */
        if (!hadChanges) {

            loadCurrentSettings();

            /*
             * The currently applied screensaver may have been
             * inaccessible before the permission was granted.
             */
            discoverAndPopulateDaydreams();
        }

        updateUi();
    }
}
