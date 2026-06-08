package com.example.netscope.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.netscope.R;
import com.example.netscope.data.Device;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class RadarFragment extends Fragment {

    private RecyclerView recyclerDevices;
    private DeviceAdapter adapter;
    private List<Device> dummyList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar, container, false);

        MaterialButton btnStartScan = view.findViewById(R.id.btnStartScan);
        recyclerDevices = view.findViewById(R.id.recyclerDevices);

        // Configuramos la lista
        recyclerDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        dummyList = new ArrayList<>();

        // Datos falsos para probar que el diseño se ve brutal
        dummyList.add(new Device("Gateway-Router", "192.168.1.1"));
        dummyList.add(new Device("Desktop-Mario", "192.168.1.15"));
        dummyList.add(new Device("Smart-TV-LG", "192.168.1.20"));

        adapter = new DeviceAdapter(dummyList);
        recyclerDevices.setAdapter(adapter);

        btnStartScan.setOnClickListener(v -> {
            Toast.makeText(getContext(), "¡Iniciando motor de escaneo!", Toast.LENGTH_SHORT).show();
            // Aquí conectaremos TU motor de red (Dev 1) más adelante
        });

        return view;
    }
}