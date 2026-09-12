/*
 * Copyright (C) 2026 TVScreensaverManager
 * 
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 */

package com.tvscreensavermanager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class TvPlatform {

    public enum Type {
        ANDROID_TV,
        GOOGLE_TV,
        FIRE_TV,
        UNKNOWN
    }

    private TvPlatform() {
    }

    public static Type detect(Context context) {

        String manufacturer = safe(Build.MANUFACTURER);
        String brand = safe(Build.BRAND);
        String model = safe(Build.MODEL);

        if (manufacturer.toLowerCase().contains("amazon")
                || brand.toLowerCase().contains("amazon")
                || model.toUpperCase().startsWith("AFT")) {

            return Type.FIRE_TV;
        }

        PackageManager pm = context.getPackageManager();

        if (hasPackage(pm, "com.google.android.apps.tv.launcher")) {
            return Type.GOOGLE_TV;
        }

        if (hasPackage(pm, "com.google.android.tvlauncher")) {
            return Type.ANDROID_TV;
        }

        return Type.UNKNOWN;
    }

    private static boolean hasPackage(
            PackageManager pm,
            String packageName) {

        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String getDisplayName(Type type) {

        switch (type) {

            case FIRE_TV:
                return "FIRE TV";

            case GOOGLE_TV:
                return "GOOGLE TV";

            case ANDROID_TV:
                return "ANDROID TV";

            default:
                return "ANDROID TV";
        }
    }
}
