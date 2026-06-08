package com.example.netscope.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.netscope.R;
import com.example.netscope.data.Device;
import com.example.netscope.network.PingSweepEngine;
import com.google.android.material.button.MaterialButton;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class RadarFragment extends Fragment {

    private RecyclerView recyclerDevices;
    private DeviceAdapter adapter;
    private List<Device> liveDeviceList;
    private MaterialButton btnStartScan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar, container, false);

        btnStartScan = view.findViewById(R.id.btnStartScan);
        recyclerDevices = view.findViewById(R.id.recyclerDevices);

        // Configuramos la lista en blanco
        recyclerDevices.setLayoutManager(new LinearLayoutManager(getContext()));
        liveDeviceList = new ArrayList<>();
        adapter = new DeviceAdapter(liveDeviceList);
        recyclerDevices.setAdapter(adapter);

        // Al presionar el botón de escanear
        btnStartScan.setOnClickListener(v -> {
            iniciarEscaneo();
        });

        return view;
    }

    private void iniciarEscaneo() {
        String subnet = getLocalSubnet();

        if (subnet == null) {
            Toast.makeText(getContext(), "Error: Conéctate a una red Wi-Fi", Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpiamos la lista anterior y avisamos al usuario
        liveDeviceList.clear();
        adapter.notifyDataSetChanged();
        btnStartScan.setEnabled(false);
        btnStartScan.setText("ESCANEANDO...");
        Toast.makeText(getContext(), "Escaneando subred: " + subnet + "x", Toast.LENGTH_SHORT).show();

        // Instanciamos TU motor de red
        PingSweepEngine engine = new PingSweepEngine();

        engine.startScan(subnet, new PingSweepEngine.ScanListener() {
            @Override
            public void onDeviceFound(String ip) {
                // IMPORTANTE: Los hilos de red no pueden modificar la interfaz.
                // Le pedimos permiso a la Activity principal para pintar la pantalla:
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Como el Dev 2 aún no hace el mDNS, ponemos "Dispositivo Detectado" temporalmente
                        liveDeviceList.add(new Device("Dispositivo Detectado", ip));
                        adapter.notifyItemInserted(liveDeviceList.size() - 1);
                    });
                }
            }

            @Override
            public void onScanComplete(List<String> activeIps) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnStartScan.setEnabled(true);
                        btnStartScan.setText("START QUICK SCAN");
                        Toast.makeText(getContext(), "Completado: " + activeIps.size() + " dispositivos", Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    /**
     * Truco ninja para sacar la subred (Ej: 192.168.1.) evadiendo permisos de ubicación
     * leyendo directamente las interfaces de red del sistema Linux/Android.
     */
    private String getLocalSubnet() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // Buscamos una IP versión 4 que NO sea el localhost (127.0.0.1)
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        // Cortamos el último número para dejar solo la subred (Ej: 192.168.1.72 -> 192.168.1.)
                        return ip.substring(0, ip.lastIndexOf('.') + 1);
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}