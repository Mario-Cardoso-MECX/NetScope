package com.example.netscope.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.example.netscope.R;
import com.example.netscope.utils.ExportHelper;
import com.example.netscope.workers.ScanWorker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button btnExport = view.findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> ExportHelper.exportHistoryToWhatsApp(getContext()));

        // Inyectamos la lógica del WorkManager en el Switch de Alertas
        SwitchMaterial switchAlerts = view.findViewById(R.id.switchAlerts); // Asegúrate que en tu XML este switch tenga este ID
        switchAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Programamos escaneo cada 30 minutos
                PeriodicWorkRequest scanRequest = new PeriodicWorkRequest.Builder(
                        ScanWorker.class, 30, TimeUnit.MINUTES).build();
                WorkManager.getInstance(requireContext()).enqueue(scanRequest);
            } else {
                WorkManager.getInstance(requireContext()).cancelAllWork();
            }
        });

        return view;
    }
}