package com.example.netscope.ui.main;

import android.database.Cursor;
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
    // Hacemos la lista estática o persistente en memoria RAM durante el ciclo de vida de la app
    private static List<Device> liveDeviceList = new ArrayList<>();

    private MaterialButton btnStartScan;
    private TextView tvDeviceCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radar, container, false);

        btnStartScan = view.findViewById(R.id.btnStartScan);
        recyclerDevices = view.findViewById(R.id.recyclerDevices);
        tvDeviceCount = view.findViewById(R.id.tvDeviceCount);

        recyclerDevices.setLayoutManager(new LinearLayoutManager(getContext()));

        if (adapter == null) {
            adapter = new DeviceAdapter(liveDeviceList);
        }
        recyclerDevices.setAdapter(adapter);

        // Si la lista ya tenía elementos previos (porque navega entre menús), actualizamos el contador visual
        if (!liveDeviceList.isEmpty()) {
            tvDeviceCount.setText(liveDeviceList.size() + " dispositivos detectados");
        } else {
            // Si está vacía al arrancar, intentamos rescatar el último escaneo de la BD local
            cargarUltimoEscaneoBD();
        }

        btnStartScan.setOnClickListener(v -> iniciarEscaneo());

        return view;
    }

    private void cargarUltimoEscaneoBD() {
        try {
            NetScopeDbHelper dbHelper = new NetScopeDbHelper(getContext());
            android.database.sqlite.SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT ip, name, status FROM scans", null);

            if (cursor.moveToFirst()) {
                liveDeviceList.clear();
                do {
                    String ip = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_IP));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_NAME));
                    liveDeviceList.add(new Device(name != null ? name : "Dispositivo Detectado", ip));
                } while (cursor.moveToNext());
                adapter.notifyDataSetChanged();
                tvDeviceCount.setText(liveDeviceList.size() + " dispositivos detectados");
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void iniciarEscaneo() {
        List<String> ipsToScan = getIpsToScan();

        if (ipsToScan == null || ipsToScan.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.toast_wifi_error), Toast.LENGTH_SHORT).show();
            return;
        }

        liveDeviceList.clear();
        adapter.notifyDataSetChanged();
        tvDeviceCount.setText("0 dispositivos detectados");

        btnStartScan.setEnabled(false);
        btnStartScan.setText(getString(R.string.btn_scanning));
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

                        new Thread(() -> {
                            String vendor = PingSweepEngine.getVendorFromMac(ip);
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
                        btnStartScan.setEnabled(true);

                        NetScopeDbHelper dbHelper = new NetScopeDbHelper(getContext());
                        dbHelper.guardarEscaneoCompleto(liveDeviceList);

                        btnStartScan.setText(getString(R.string.btn_start_scan));
                        Toast.makeText(getContext(), "Completado: " + activeIps.size() + " equipos.", Toast.LENGTH_LONG).show();

                        nsdResolver.stopDiscovery();
                    });
                }
            }
        });
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