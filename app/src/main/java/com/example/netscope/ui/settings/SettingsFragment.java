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

import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    // Cambia SwitchCompat por Switch o MaterialSwitch dependiendo de qué usaste en tu XML
    private SwitchCompat switchAutoScan;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. Mapear el Switch (¡Verifica que este sea el ID correcto en tu XML!)
        switchAutoScan = view.findViewById(R.id.switchAutoScan);

        // 2. Inicializar la memoria de preferencias
        sharedPreferences = requireActivity().getSharedPreferences("NetScopePrefs", Context.MODE_PRIVATE);

        // 3. Cargar el estado anterior del switch (por defecto apagado / false)
        boolean isAutoScanEnabled = sharedPreferences.getBoolean("auto_scan", false);
        switchAutoScan.setChecked(isAutoScanEnabled);

        // 4. Escuchar cuando lo prendes o lo apagas
        switchAutoScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Guardamos el nuevo estado en la memoria
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
        // TRUCO DE INGENIERO: Android restringe los trabajos en segundo plano a un mínimo de 15 minutos
        // para no drenar la batería. No se puede poner menos tiempo en un PeriodicWorkRequest.
        PeriodicWorkRequest scanRequest = new PeriodicWorkRequest.Builder(ScanWorker.class, 15, TimeUnit.MINUTES)
                .build();

        // Encolamos el trabajo. "UPDATE" significa que si ya había uno corriendo, lo actualiza.
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "VigilanteNetScope",
                ExistingPeriodicWorkPolicy.UPDATE,
                scanRequest
        );

        Toast.makeText(getContext(), "🛡️ Escudo activado (Vigilancia cada 15 min)", Toast.LENGTH_SHORT).show();
    }

    private void desactivarGuardia() {
        // Matamos el proceso por su nombre único
        WorkManager.getInstance(requireContext()).cancelUniqueWork("VigilanteNetScope");
        Toast.makeText(getContext(), "🛑 Escudo desactivado", Toast.LENGTH_SHORT).show();
    }
}