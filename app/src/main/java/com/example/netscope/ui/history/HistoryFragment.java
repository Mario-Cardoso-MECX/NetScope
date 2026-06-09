package com.example.netscope.ui.history;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.netscope.R;
import com.example.netscope.data.NetScopeDbHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerHistory = view.findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        cargarHistorial();

        return view;
    }

    private void cargarHistorial() {
        NetScopeDbHelper db = new NetScopeDbHelper(getContext());
        Cursor cursor = db.getAllHistory();
        List<HistoryRecord> records = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                String ip = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_IP));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_NAME));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_STATUS));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_TIMESTAMP));

                records.add(new HistoryRecord(ip, name, status, time));
            } while (cursor.moveToNext());
        }
        cursor.close();

        recyclerHistory.setAdapter(new HistoryAdapter(records));
    }

    // ==========================================================
    // CLASES INTERNAS (Para ahorrarte la creación de archivos)
    // ==========================================================
    private static class HistoryRecord {
        String ip, name, status, time;
        HistoryRecord(String ip, String name, String status, String time) {
            this.ip = ip; this.name = name; this.status = status; this.time = time;
        }
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryRecord> data;

        HistoryAdapter(List<HistoryRecord> data) { this.data = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryRecord record = data.get(position);
            holder.tvName.setText(record.name != null ? record.name : holder.itemView.getContext().getString(R.string.text_detected_device));
            holder.tvIp.setText(record.ip);

            // Inyectamos la fecha dinámica en el string
            holder.tvTime.setText(holder.itemView.getContext().getString(R.string.history_last_seen, record.time));

            // La lógica visual del Dev 5 leyendo del XML
            if (record.status.contains("INTRUSO")) {
                holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_intruso));
                holder.tvStatus.setTextColor(Color.parseColor("#FF5555"));
            } else {
                holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_conocido));
                holder.tvStatus.setTextColor(Color.parseColor("#00FF7F"));
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvIp, tvStatus, tvTime;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvHistName);
                tvIp = v.findViewById(R.id.tvHistIp);
                tvStatus = v.findViewById(R.id.tvHistStatus);
                tvTime = v.findViewById(R.id.tvHistDate);
            }
        }
    }
}