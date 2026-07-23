package com.mecx.netscope.ui.history;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mecx.netscope.R;
import com.mecx.netscope.data.NetScopeDbHelper;
import com.mecx.netscope.data.ScanSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;
    private HistoryAdapter adapter;
    private List<ScanSession> records;
    private NetScopeDbHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        dbHelper = new NetScopeDbHelper(getContext());

        cargarHistorialReal();
        configurarSwipeParaEliminar();

        return view;
    }

    private void cargarHistorialReal() {
        Cursor cursor = dbHelper.getAllHistory();
        Map<String, ScanSession> sesiones = new LinkedHashMap<>();

        if (cursor.moveToFirst()) {
            do {
                String ip = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_IP));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_STATUS));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_TIMESTAMP));

                int lastDotIndex = ip.lastIndexOf('.');
                String subnet = (lastDotIndex != -1) ? ip.substring(0, lastDotIndex) + ".0/24" : "Red Desconocida";
                int threatCount = (status != null && status.contains("INTRUSO")) ? 1 : 0;

                if (sesiones.containsKey(time)) {
                    ScanSession sesionExistente = sesiones.get(time);
                    if (sesionExistente != null) {
                        sesionExistente.setTotalHosts(sesionExistente.getTotalHosts() + 1);
                        sesionExistente.setThreats(sesionExistente.getThreats() + threatCount);
                    }
                } else {
                    sesiones.put(time, new ScanSession(subnet, time, 1, threatCount));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        records = new ArrayList<>(sesiones.values());
        adapter = new HistoryAdapter(records);
        recyclerHistory.setAdapter(adapter);
    }

    // =======================================================
    // MAGIA UI/UX: DESLIZAR PARA ELIMINAR (ESTILO NU BANK)
    // =======================================================
    private void configurarSwipeParaEliminar() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // No soportamos mover elementos arriba o abajo, solo a los lados
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ScanSession sessionABorrar = records.get(position);

                // 1. Borrar todos los equipos de ESA sesión exacta de la Base de Datos SQLite
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("scans", "timestamp=?", new String[]{sessionABorrar.getDate()});

                // 2. Borrar la tarjeta visualmente con la animación nativa
                records.remove(position);
                adapter.notifyItemRemoved(position);

                Toast.makeText(getContext(), "Sesión de escaneo eliminada", Toast.LENGTH_SHORT).show();
            }
        };

        // Adjuntamos la lógica táctil a tu lista
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerHistory);
    }
}