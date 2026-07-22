package com.example.netscope.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.netscope.R;
import com.example.netscope.workers.ScanWorker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    private SwitchCompat switchAutoScan;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchAutoScan = view.findViewById(R.id.switchAutoScan);
        sharedPreferences = requireActivity().getSharedPreferences("NetScopePrefs", Context.MODE_PRIVATE);

        boolean isAutoScanEnabled = sharedPreferences.getBoolean("auto_scan", false);
        switchAutoScan.setChecked(isAutoScanEnabled);

        switchAutoScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_scan", isChecked).apply();

            if (isChecked) {
                activarGuardia();
            } else {
                desactivarGuardia();
            }
        });

        return view;
    }

    private void activarGuardia() {
        PeriodicWorkRequest scanRequest = new PeriodicWorkRequest.Builder(ScanWorker.class, 15, TimeUnit.MINUTES).build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "VigilanteNetScope",
                ExistingPeriodicWorkPolicy.UPDATE,
                scanRequest
        );

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🛡️ Escudo Activado")
                .setMessage("El guardia en segundo plano escaneará la red cada 15 minutos.\n\nRecibirás una notificación push si se detectan dispositivos sospechosos.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void desactivarGuardia() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("VigilanteNetScope");
        Toast.makeText(getContext(), "🛑 Escudo desactivado", Toast.LENGTH_SHORT).show();
    }
}