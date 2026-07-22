package com.example.netscope.ui.main;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
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
    private FrameLayout dotsContainer;

    // Menú desplegable y su lista dinámica
    private Spinner spinnerFilter;
    private List<String> categoriasActivas = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar, container, false);

        btnStartScan = view.findViewById(R.id.btnStartScan);
        recyclerDevices = view.findViewById(R.id.recyclerDevices);
        tvDeviceCount = view.findViewById(R.id.tvDeviceCount);
        ivRadarSweep = view.findViewById(R.id.ivRadarSweep);
        dotsContainer = view.findViewById(R.id.dotsContainer);
        spinnerFilter = view.findViewById(R.id.spinnerFilter);

        recyclerDevices.setLayoutManager(new LinearLayoutManager(getContext()));

        if (adapter == null) {
            adapter = new DeviceAdapter(liveDeviceList);
        }
        recyclerDevices.setAdapter(adapter);

        // ==========================================================
        // CONFIGURACIÓN INICIAL DEL FILTRO INTELIGENTE
        // ==========================================================
        categoriasActivas.add("Todos");

        if (spinnerFilter != null) {
            spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, categoriasActivas) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.neon_accent)); // Verde neón
                    return tv;
                }

                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                    tv.setTextColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.neon_accent));
                    tv.setBackgroundColor(Color.parseColor("#12181B")); // Fondo oscuro Material 3
                    tv.setPadding(30, 30, 30, 30);
                    return tv;
                }
            };
            spinnerFilter.setAdapter(spinnerAdapter);

            spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    aplicarFiltro(categoriasActivas.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Si ya hay datos (ej. giró la pantalla)
        if (!liveDeviceList.isEmpty()) {
            tvDeviceCount.setText(liveDeviceList.size() + " firmas en red");
            for(int i=0; i<liveDeviceList.size(); i++) dibujarPuntitoEnRadar();
            actualizarFiltroDinamico();
        } else {
            tvDeviceCount.setText("0 firmas detectadas");
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

        liveDeviceList.clear();

        // Usamos actualizarLista para restaurar la vista si estaba filtrada
        adapter.actualizarLista(liveDeviceList);

        tvDeviceCount.setText("0 firmas detectadas");
        dotsContainer.removeAllViews();

        // Ocultamos el filtro mientras escanea
        if (spinnerFilter != null) spinnerFilter.setVisibility(View.GONE);

        btnStartScan.setEnabled(false);
        btnStartScan.setText("ENRUTANDO PAQUETES...");

        RotateAnimation rotate = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1500);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());

        if (ivRadarSweep != null) {
            ivRadarSweep.setVisibility(View.VISIBLE);
            ivRadarSweep.startAnimation(rotate);
        }

        Toast.makeText(getContext(), "Iniciando barrido táctico en " + ipsToScan.size() + " objetivos...", Toast.LENGTH_LONG).show();

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
                        tvDeviceCount.setText(liveDeviceList.size() + " firmas detectadas");

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

                        if (ivRadarSweep != null) {
                            ivRadarSweep.clearAnimation();
                            ivRadarSweep.setVisibility(View.INVISIBLE);
                        }

                        btnStartScan.setEnabled(true);
                        btnStartScan.setText(getString(R.string.btn_start_scan));

                        // Mostramos las categorías que realmente se encontraron
                        actualizarFiltroDinamico();

                        NetScopeDbHelper dbHelper = new NetScopeDbHelper(getContext());
                        dbHelper.guardarEscaneoCompleto(liveDeviceList);

                        Toast.makeText(getContext(), "Análisis completado. " + activeIps.size() + " firmas enlazadas.", Toast.LENGTH_LONG).show();

                        nsdResolver.stopDiscovery();
                    });
                }
            }
        });
    }

    // ==========================================================
    // METODO PARA CREAR CATEGORÍAS SEGÚN LO ENCONTRADO
    // ==========================================================
    private void actualizarFiltroDinamico() {
        categoriasActivas.clear();
        categoriasActivas.add("Todos");

        boolean hayWindows = false, hayLinux = false, hayApple = false, hayAndroid = false, hayRouter = false, hayImpresora = false, hayTv = false;

        // Analizamos cada dispositivo para ver qué categorías activamos
        for (Device d : liveDeviceList) {
            String v = d.getVendor() != null ? d.getVendor().toLowerCase() : "genérico";
            String n = d.getName() != null ? d.getName().toLowerCase() : "";
            String ip = d.getIp() != null ? d.getIp() : "";

            if (v.contains("windows") || n.contains("pc")) hayWindows = true;
            if (v.contains("linux") || v.contains("raspberry")) hayLinux = true;
            // Usamos la validación corregida (anti-direcciones MAC)
            if (v.contains("apple") || n.contains("macbook") || n.contains("imac") || n.contains("iphone") || n.contains("ipad")) hayApple = true;
            if (n.contains("android") || v.contains("cubot") || n.contains("phone")) hayAndroid = true;
            if (v.contains("enrutador") || n.contains("gateway") || ip.endsWith(".1") || ip.endsWith(".254")) hayRouter = true;
            if (v.contains("impresora")) hayImpresora = true;
            if (n.contains("tv") || v.contains("roku") || v.contains("cast") || v.contains("samsung")) hayTv = true;
        }

        // Si existen en la red, los agregamos al menú
        if (hayRouter) categoriasActivas.add("Router");
        if (hayWindows) categoriasActivas.add("Windows");
        if (hayLinux) categoriasActivas.add("Linux");
        if (hayApple) categoriasActivas.add("Apple");
        if (hayAndroid) categoriasActivas.add("Android");
        if (hayTv) categoriasActivas.add("Smart TV");
        if (hayImpresora) categoriasActivas.add("Impresora");

        spinnerAdapter.notifyDataSetChanged();

        // Solo mostrar el menú si encontramos al menos UNA categoría aparte de "Todos"
        if (categoriasActivas.size() > 1 && spinnerFilter != null) {
            spinnerFilter.setVisibility(View.VISIBLE);
            spinnerFilter.setSelection(0);
        }
    }

    private void dibujarPuntitoEnRadar() {
        if (getContext() == null || dotsContainer.getWidth() == 0) return;

        if (dotsContainer.getChildCount() >= 20) return;

        int size = dotsContainer.getWidth();
        int radioVisual = (size / 2) - 20;

        double angle = Math.random() * 2 * Math.PI;
        double r = Math.random() * radioVisual;

        int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
        int dotX = (int) ((size / 2) + r * Math.cos(angle)) - (dotSize / 2);
        int dotY = (int) ((size / 2) + r * Math.sin(angle)) - (dotSize / 2);

        ImageView dot = new ImageView(getContext());
        dot.setImageResource(R.drawable.radar_dot);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dotSize, dotSize);
        params.leftMargin = dotX;
        params.topMargin = dotY;

        android.view.animation.AlphaAnimation blink = new android.view.animation.AlphaAnimation(1.0f, 0.2f);
        blink.setDuration(600);
        blink.setRepeatMode(android.view.animation.Animation.REVERSE);
        blink.setRepeatCount(android.view.animation.Animation.INFINITE);
        dot.startAnimation(blink);

        dotsContainer.addView(dot, params);
    }

    // ==========================================================
    // MOTOR DE FILTRADO (Con bug Anti-MAC parchado)
    // ==========================================================
    private void aplicarFiltro(String categoria) {
        if (liveDeviceList.isEmpty()) return;

        List<Device> filtrados = new ArrayList<>();
        for (Device d : liveDeviceList) {
            String v = d.getVendor() != null ? d.getVendor().toLowerCase() : "genérico";
            String n = d.getName() != null ? d.getName().toLowerCase() : "";
            String ip = d.getIp() != null ? d.getIp() : "";

            if (categoria.equals("Todos")) {
                filtrados.add(d);
            } else if (categoria.equals("Windows") && (v.contains("windows") || n.contains("pc"))) {
                filtrados.add(d);
            } else if (categoria.equals("Linux") && (v.contains("linux") || v.contains("raspberry"))) {
                filtrados.add(d);
            } else if (categoria.equals("Apple") && (v.contains("apple") || n.contains("macbook") || n.contains("imac") || n.contains("iphone") || n.contains("ipad"))) {
                filtrados.add(d);
            } else if (categoria.equals("Android") && (n.contains("android") || v.contains("cubot") || n.contains("phone"))) {
                filtrados.add(d);
            } else if (categoria.equals("Router") && (v.contains("enrutador") || n.contains("gateway") || ip.endsWith(".1") || ip.endsWith(".254"))) {
                filtrados.add(d);
            } else if (categoria.equals("Impresora") && v.contains("impresora")) {
                filtrados.add(d);
            } else if (categoria.equals("Smart TV") && (n.contains("tv") || v.contains("roku") || v.contains("cast") || v.contains("samsung"))) {
                filtrados.add(d);
            }
        }

        if (adapter != null) {
            adapter.actualizarLista(filtrados);
        }

        tvDeviceCount.setText(filtrados.size() + " firmas detectadas (" + categoria + ")");
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