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

        String deviceName = device.getName() != null ? device.getName() : "Desconocido";
        String vendor = device.getVendor() != null ? device.getVendor() : "Genérico";

        holder.tvName.setText(deviceName);
        holder.tvIp.setText(device.getIp());
        holder.tvVendor.setText(vendor);

        String nameLower = deviceName.toLowerCase();
        String vendorLower = vendor.toLowerCase();
        String ip = device.getIp();

        if (ip.endsWith(".1") || ip.endsWith(".254") || vendorLower.contains("enrutador") || nameLower.contains("gateway")) {
            holder.ivIcon.setImageResource(R.drawable.ic_router_neon);
        } else if (nameLower.contains("tv") || vendorLower.contains("roku") || vendorLower.contains("cast") || vendorLower.contains("samsung")) {
            holder.ivIcon.setImageResource(R.drawable.ic_tv_neon);
        } else if (vendorLower.contains("windows") || nameLower.contains("pc")) {
            holder.ivIcon.setImageResource(R.drawable.ic_windows_neon);
        }
        // ¡ESTA ES LA LÍNEA CORREGIDA PARA QUE NO CONFUNDA DIRECCIONES MAC CON APPLE!
        else if (vendorLower.contains("apple") || nameLower.contains("macbook") || nameLower.contains("imac") || nameLower.contains("iphone") || nameLower.contains("ipad")) {
            holder.ivIcon.setImageResource(R.drawable.ic_apple_neon);
        }
        else if (vendorLower.contains("linux") || vendorLower.contains("raspberry")) {
            holder.ivIcon.setImageResource(R.drawable.ic_linux_neon);
        } else if (vendorLower.contains("impresora")) {
            holder.ivIcon.setImageResource(R.drawable.ic_printer_neon);
        } else if (nameLower.contains("android") || vendorLower.contains("cubot") || nameLower.contains("phone")) {
            holder.ivIcon.setImageResource(R.drawable.ic_android_neon);
        } else {
            // El escudo táctico
            holder.ivIcon.setImageResource(R.drawable.ic_generic_neon);
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

    public void actualizarLista(List<Device> nuevaLista) {
        this.deviceList = nuevaLista;
        notifyDataSetChanged();
    }
}