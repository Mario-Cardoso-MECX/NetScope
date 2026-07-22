package com.example.netscope.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.netscope.R;
import com.example.netscope.data.Device;
import com.example.netscope.data.NetScopeDbHelper;
import com.example.netscope.network.NsdResolver;
import com.example.netscope.network.PingSweepEngine;
import com.google.android.material.button.MaterialButton;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class RadarFragment extends Fragment {

    private RecyclerView recyclerDevices;
    private static DeviceAdapter adapter;
    private static List<Device> liveDeviceList = new ArrayList<>();

    private MaterialButton btnStartScan;
    private TextView tvDeviceCount;
    private ImageView ivRadarSweep;
    private FrameLayout dotsContainer; // El contenedor de los puntitos

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar, container, false);

        btnStartScan = view.findViewById(R.id.btnStartScan);
        recyclerDevices = view.findViewById(R.id.recyclerDevices);
        tvDeviceCount = view.findViewById(R.id.tvDeviceCount);
        ivRadarSweep = view.findViewById(R.id.ivRadarSweep);
        dotsContainer = view.findViewById(R.id.dotsContainer);

        recyclerDevices.setLayoutManager(new LinearLayoutManager(getContext()));

        if (adapter == null) {
            adapter = new DeviceAdapter(liveDeviceList);
        }
        recyclerDevices.setAdapter(adapter);

        if (!liveDeviceList.isEmpty()) {
            tvDeviceCount.setText(liveDeviceList.size() + " dispositivos detectados");
            // Si ya hay dispositivos, repintar los puntos al volver a la pestaña
            for(int i=0; i<liveDeviceList.size(); i++) dibujarPuntitoEnRadar();
        } else {
            tvDeviceCount.setText("0 dispositivos detectados");
        }

        btnStartScan.setOnClickListener(v -> iniciarEscaneo());

        return view;
    }

    private void iniciarEscaneo() {
        List<String> ipsToScan = getIpsToScan();

        if (ipsToScan == null || ipsToScan.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.toast_wifi_error), Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpiar lista, texto y borrar TODOS los puntitos anteriores
        liveDeviceList.clear();
        adapter.notifyDataSetChanged();
        tvDeviceCount.setText("0 dispositivos detectados");
        dotsContainer.removeAllViews();

        btnStartScan.setEnabled(false);
        btnStartScan.setText(getString(R.string.btn_scanning));

        // 🌀 INICIA ANIMACIÓN Y PRENDE LA LUZ
        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1500);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());

        if (ivRadarSweep != null) {
            ivRadarSweep.setVisibility(View.VISIBLE); // <-- AQUÍ PRENDEMOS LA LUZ
            ivRadarSweep.startAnimation(rotate);
        }

        Toast.makeText(getContext(), "Escaneando bloque de " + ipsToScan.size() + " IPs...", Toast.LENGTH_LONG).show();

        NsdResolver nsdResolver = new NsdResolver(getContext());
        nsdResolver.startDiscovery((ip, name) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    for (int i = 0; i < liveDeviceList.size(); i++) {
                        Device device = liveDeviceList.get(i);
                        if (device.getIp().equals(ip)) {
                            device.setName(name);
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                });
            }
        });

        PingSweepEngine engine = new PingSweepEngine();
        engine.startScan(ipsToScan, new PingSweepEngine.ScanListener() {
            @Override
            public void onDeviceFound(String ip, String name) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Device newDevice = new Device(name, ip);
                        liveDeviceList.add(newDevice);
                        adapter.notifyItemInserted(liveDeviceList.size() - 1);
                        tvDeviceCount.setText(liveDeviceList.size() + " dispositivos detectados");

                        // ✨ DIBUJAMOS EL PUNTITO EN EL RADAR
                        dibujarPuntitoEnRadar();

                        new Thread(() -> {
                            String vendor = PingSweepEngine.resolveDeviceIdentity(ip);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    newDevice.setVendor(vendor);
                                    adapter.notifyDataSetChanged();
                                });
                            }
                        }).start();
                    });
                }
            }

            @Override
            public void onScanComplete(List<String> activeIps) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        java.util.Collections.sort(liveDeviceList, (d1, d2) -> {
                            try {
                                String[] parts1 = d1.getIp().split("\\.");
                                String[] parts2 = d2.getIp().split("\\.");
                                for (int i = 0; i < 4; i++) {
                                    int p1 = Integer.parseInt(parts1[i]);
                                    int p2 = Integer.parseInt(parts2[i]);
                                    if (p1 != p2) return Integer.compare(p1, p2);
                                }
                            } catch (Exception e) { return 0; }
                            return 0;
                        });

                        adapter.notifyDataSetChanged();

                        // 🛑 DETENER ANIMACIÓN Y APAGAR LA LUZ
                        if (ivRadarSweep != null) {
                            ivRadarSweep.clearAnimation();
                            ivRadarSweep.setVisibility(View.INVISIBLE); // <-- AQUÍ LA APAGAMOS
                        }

                        btnStartScan.setEnabled(true);
                        btnStartScan.setText(getString(R.string.btn_start_scan));

                        NetScopeDbHelper dbHelper = new NetScopeDbHelper(getContext());
                        dbHelper.guardarEscaneoCompleto(liveDeviceList);

                        Toast.makeText(getContext(), "Completado: " + activeIps.size() + " equipos.", Toast.LENGTH_LONG).show();

                        nsdResolver.stopDiscovery();
                    });
                }
            }
        });
    }

    // =========================================================================
    // TRIGONOMETRÍA PARA INYECTAR PUNTOS DINÁMICOS EN EL RADAR (CON LÍMITE)
    // =========================================================================
    private void dibujarPuntitoEnRadar() {
        if (getContext() == null || dotsContainer.getWidth() == 0) return;

        // 🛡️ EL ESCUDO ANTI-SATURACIÓN (Máximo 20 puntitos visuales)
        if (dotsContainer.getChildCount() >= 20) {
            return; // Si ya hay 20 puntos en el radar, ignoramos el dibujo visual.
        }

        // Tamaño del contenedor (radio del radar)
        int size = dotsContainer.getWidth();
        int radioVisual = (size / 2) - 20; // 20px de margen para no salirse del borde

        // Generamos un ángulo (0 a 360 grados) y una distancia (0 al borde) aleatorios
        double angle = Math.random() * 2 * Math.PI;
        double r = Math.random() * radioVisual;

        // Convertimos a coordenadas X y Y cartesianas
        int dotSize = (int) (8 * getResources().getDisplayMetrics().density); // 8dp de tamaño
        int dotX = (int) ((size / 2) + r * Math.cos(angle)) - (dotSize / 2);
        int dotY = (int) ((size / 2) + r * Math.sin(angle)) - (dotSize / 2);

        // Creamos la imagen del puntito
        ImageView dot = new ImageView(getContext());
        dot.setImageResource(R.drawable.radar_dot);

        // Lo posicionamos exactamente en sus coordenadas X,Y
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dotSize, dotSize);
        params.leftMargin = dotX;
        params.topMargin = dotY;

        // Efecto visual hacker: un pequeño parpadeo al nacer
        dot.setAlpha(0f);
        dot.animate().alpha(1f).setDuration(400).start();

        dotsContainer.addView(dot, params);
    }

    private List<String> getIpsToScan() {
        List<String> ips = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.isLoopback() || !intf.isUp()) continue;

                for (java.net.InterfaceAddress intfAddr : intf.getInterfaceAddresses()) {
                    InetAddress inetAddress = intfAddr.getAddress();

                    if (inetAddress instanceof Inet4Address) {
                        int prefixLength = intfAddr.getNetworkPrefixLength();
                        byte[] ipBytes = inetAddress.getAddress();

                        int ipInt = ((ipBytes[0] & 0xFF) << 24) |
                                ((ipBytes[1] & 0xFF) << 16) |
                                ((ipBytes[2] & 0xFF) << 8)  |
                                (ipBytes[3] & 0xFF);

                        int maskInt = 0xFFFFFFFF << (32 - prefixLength);
                        int networkInt = ipInt & maskInt;
                        int broadcastInt = networkInt | ~maskInt;

                        for (int i = networkInt + 1; i < broadcastInt; i++) {
                            String ipStr = ((i >> 24) & 0xFF) + "." +
                                    ((i >> 16) & 0xFF) + "." +
                                    ((i >> 8) & 0xFF) + "." +
                                    (i & 0xFF);
                            ips.add(ipStr);
                        }
                        return ips;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}