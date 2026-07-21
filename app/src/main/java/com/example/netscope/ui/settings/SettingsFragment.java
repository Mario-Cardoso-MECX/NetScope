package com.example.netscope.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.netscope.R;
import com.example.netscope.utils.ExportHelper;
import com.example.netscope.utils.PermissionHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchThreats, switchAutoScan;
    private MaterialButton btnExportReport;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchThreats = view.findViewById(R.id.switchThreats);
        switchAutoScan = view.findViewById(R.id.switchAutoScan);
        btnExportReport = view.findViewById(R.id.btnExportReport);

        // Si activan Auto-Escaneo o Alertas, exigimos los permisos
        switchThreats.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && getActivity() != null) {
                if (!PermissionHelper.hasAllPermissions(getContext())) {
                    PermissionHelper.requestPermissions(getActivity());
                }
            }
        });

        switchAutoScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getContext(), "Auto-Escaneo en segundo plano activado", Toast.LENGTH_SHORT).show();
                // Aquí activaremos el ScanWorker de Android en el futuro
            }
        });

        btnExportReport.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Generando Ticket de Red...", Toast.LENGTH_SHORT).show();
            ExportHelper.exportDatabaseToImage(getContext());
        });

        return view;
    }
}