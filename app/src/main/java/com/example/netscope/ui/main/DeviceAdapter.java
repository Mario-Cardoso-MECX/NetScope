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

        // 1. Textos principales
        holder.tvName.setText(device.getName());
        holder.tvIp.setText(device.getIp());

        // 2. Extracción del Vendor (Fabricante)
        String vendor = device.getVendor() != null ? device.getVendor() : "Genérico";
        holder.tvVendor.setText(vendor);

        // 3. LÓGICA DE ÍCONOS DINÁMICOS
        String nameLower = device.getName().toLowerCase();
        String vendorLower = vendor.toLowerCase();
        String ip = device.getIp();

        // Regla A: Si la IP termina en .1 o .254, casi seguro es el Módem/Router de la casa
        if (ip.endsWith(".1") || ip.endsWith(".254") || nameLower.contains("gateway")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_share); // Ícono de nodos/red
        }
        // Regla B: Pantallas, Rokus y Smart TVs
        else if (nameLower.contains("tv") || nameLower.contains("roku") || nameLower.contains("chromecast")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_gallery); // Ícono de marco/pantalla
        }
        // Regla C: Teléfonos (Xiaomi, Samsung, Cubot, etc.)
        else if (nameLower.contains("android") || vendorLower.contains("xiaomi") || vendorLower.contains("samsung") || vendorLower.contains("cubot")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_call); // Ícono de teléfono nativo
        }
        // Regla D: Computadoras (Mac, PC, Laptops)
        else if (nameLower.contains("mac") || nameLower.contains("pc") || nameLower.contains("desktop") || vendorLower.contains("apple")) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_info_details); // Ícono de sistema/info
        }
        // Por Defecto: Dispositivo Genérico
        else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_mylocation); // Ícono tipo radar/objetivo
        }

        // 4. Tu lógica intacta: El clic en la tarjeta
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
            // Enlazamos con los IDs del nuevo XML que acabamos de crear
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvIp = itemView.findViewById(R.id.tvDeviceIp);
            tvVendor = itemView.findViewById(R.id.tvDeviceVendor);
            ivIcon = itemView.findViewById(R.id.ivDeviceIcon);
        }
    }
}