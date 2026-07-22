package com.example.netscope.ui.main;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.netscope.R;
import com.example.netscope.data.Device;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> deviceList;

    public DeviceAdapter(List<Device> deviceList) {
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = deviceList.get(position);

        holder.tvName.setText(device.getName());
        holder.tvIp.setText(device.getIp());

        String vendor = device.getVendor() != null ? device.getVendor() : "Genérico";
        holder.tvVendor.setText(vendor);

        // LÓGICA DE ÍCONOS DINÁMICOS
        String nameLower = device.getName().toLowerCase();
        String vendorLower = vendor.toLowerCase();
        String ip = device.getIp();

        if (ip.endsWith(".1") || ip.endsWith(".254") || vendorLower.contains("enrutador") || nameLower.contains("gateway")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_dialer);
        } else if (nameLower.contains("tv") || vendorLower.contains("roku") || vendorLower.contains("cast") || vendorLower.contains("samsung")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        } else if (vendorLower.contains("windows") || vendorLower.contains("linux") || vendorLower.contains("apple") || vendorLower.contains("mac")) {
            // CORREGIDO: Usamos un icono de sistema universal que sí existe
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        } else if (vendorLower.contains("impresora")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        } else if (nameLower.contains("android") || vendorLower.contains("cubot")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_call);
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_mylocation);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), com.example.netscope.ui.details.DetailsActivity.class);
            intent.putExtra("TARGET_IP", device.getIp());
            intent.putExtra("TARGET_NAME", device.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvIp, tvVendor;
        ImageView ivIcon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvIp = itemView.findViewById(R.id.tvDeviceIp);
            tvVendor = itemView.findViewById(R.id.tvDeviceVendor);
            ivIcon = itemView.findViewById(R.id.ivDeviceIcon);
        }
    }
}