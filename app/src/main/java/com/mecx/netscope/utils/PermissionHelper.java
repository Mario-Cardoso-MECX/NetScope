package com.mecx.netscope.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static final int PERMISSION_REQUEST_CODE = 1001;

    public static boolean hasAllPermissions(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void requestPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Android 13+ requiere permiso explícito para Push Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            // --- DIVULGACIÓN DESTACADA PARA GOOGLE PLAY ---
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Permisos Requeridos")
                    .setMessage("NetScope requiere acceso a tu ubicación y dispositivos cercanos para poder mapear la red local y encontrar las direcciones IP. Tus datos no salen de este teléfono.")
                    .setCancelable(false) // Obliga al usuario a interactuar con el botón
                    .setPositiveButton("Entendido", (dialog, which) -> {
                        // Una vez que el usuario lee y entiende, lanzamos la petición nativa de Android
                        ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                    })
                    .show();
        }
    }
}