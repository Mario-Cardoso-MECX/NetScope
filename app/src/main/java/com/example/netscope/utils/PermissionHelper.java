package com.example.netscope.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    // El arsenal de permisos obligatorios para pentesting de red en Android
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    public static final int PERMISSION_REQUEST_CODE = 666; // Código hacker 😈

    /**
     * Revisa silenciosamente si ya tenemos todos los permisos
     */
    public static boolean hasAllPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false; // Nos falta al menos uno
            }
        }
        return true; // Tenemos luz verde
    }

    /**
     * Le lanza el popup al usuario exigiéndole los permisos
     */
    public static void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
}