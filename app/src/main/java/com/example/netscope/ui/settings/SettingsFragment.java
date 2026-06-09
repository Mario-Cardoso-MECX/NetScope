package com.example.netscope.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.netscope.R;
import com.example.netscope.utils.ExportHelper;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button btnExport = view.findViewById(R.id.btnExport);

        // Al presionar el botón, llamamos al exportador
        btnExport.setOnClickListener(v -> {
            ExportHelper.exportHistoryToWhatsApp(getContext());
        });

        return view;
    }
}