package com.example.netscope.ui.history;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.netscope.R;
import com.example.netscope.data.NetScopeDbHelper;
import com.example.netscope.data.ScanSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        cargarHistorialReal();

        return view;
    }

    private void cargarHistorialReal() {
        NetScopeDbHelper db = new NetScopeDbHelper(getContext());
        Cursor cursor = db.getAllHistory();

        // Usamos un mapa para agrupar los dispositivos individuales por Fecha/Sesión
        Map<String, ScanSession> sesiones = new LinkedHashMap<>();

        if (cursor.moveToFirst()) {
            do {
                String ip = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_IP));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_STATUS));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_TIMESTAMP));

                // Deducimos la subred matemáticamente (ej. 192.168.1.15 -> 192.168.1.0/24)
                int lastDotIndex = ip.lastIndexOf('.');
                String subnet = (lastDotIndex != -1) ? ip.substring(0, lastDotIndex) + ".0/24" : "Red Desconocida";

                // Contamos si la base de datos lo marcó como intruso
                int threatCount = (status != null && status.contains("INTRUSO")) ? 1 : 0;

                // Si ya existe la sesión de esa fecha, le sumamos los equipos
                if (sesiones.containsKey(time)) {
                    ScanSession sesionExistente = sesiones.get(time);
                    if (sesionExistente != null) {
                        sesionExistente.setTotalHosts(sesionExistente.getTotalHosts() + 1);
                        sesionExistente.setThreats(sesionExistente.getThreats() + threatCount);
                    }
                } else {
                    // Si es nueva sesión, la creamos
                    sesiones.put(time, new ScanSession(subnet, time, 1, threatCount));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Convertimos nuestro mapa a una lista y la inyectamos al adaptador externo
        List<ScanSession> records = new ArrayList<>(sesiones.values());
        recyclerHistory.setAdapter(new HistoryAdapter(records));
    }
}