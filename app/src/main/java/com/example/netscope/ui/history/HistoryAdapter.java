package com.example.netscope.ui.history;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.netscope.R;
import com.example.netscope.data.ScanSession;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<ScanSession> sessionList;

    public HistoryAdapter(List<ScanSession> sessionList) {
        this.sessionList = sessionList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ScanSession session = sessionList.get(position);

        holder.tvSubnet.setText(session.getSubnet());
        holder.tvDate.setText(session.getDate());
        holder.tvHosts.setText(session.getTotalHosts() + " equipos");

        // Lógica de semáforo para amenazas
        if (session.getThreats() > 0) {
            holder.tvThreats.setText(session.getThreats() + " amenazas");
            holder.tvThreats.setTextColor(Color.parseColor("#FF5555")); // Rojo peligro
        } else {
            holder.tvThreats.setText("limpio");
            holder.tvThreats.setTextColor(Color.parseColor("#00FF7F")); // Verde seguro
        }
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubnet, tvDate, tvHosts, tvThreats;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubnet = itemView.findViewById(R.id.tvHistorySubnet);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvHosts = itemView.findViewById(R.id.tvHistoryHosts);
            tvThreats = itemView.findViewById(R.id.tvHistoryThreats);
        }
    }
}