package com.example.netscope.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
            Toast.makeText(getContext(), getString(R.string.toast_wifi_error), Toast.LENGTH_SHORT).show();
            return;
        }

        // Limpiamos la lista anterior y avisamos al usuario
        liveDeviceList.clear();
        adapter.notifyDataSetChanged();
        btnStartScan.setEnabled(false);
        btnStartScan.setText(getString(R.string.btn_scanning));
        Toast.makeText(getContext(), "Escaneando subred: " + subnet + "x", Toast.LENGTH_SHORT).show();

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
        engine.startScan(subnet, new PingSweepEngine.ScanListener() {
            @Override
            public void onDeviceFound(String ip, String name) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Device newDevice = new Device(name, ip);
                        liveDeviceList.add(newDevice);
                        adapter.notifyItemInserted(liveDeviceList.size() - 1);

                        // Consulta de Vendor en segundo plano
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
                        Toast.makeText(getContext(), getString(R.string.toast_scan_complete, activeIps.size()), Toast.LENGTH_LONG).show();

                        nsdResolver.stopDiscovery();
                    });
                }
            }
        });
    }

    private String getLocalSubnet() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        return ip.substring(0, ip.lastIndexOf('.') + 1);
                    }
                }
            }
        } catch (SocketException ex) { ex.printStackTrace(); }
        return null;
    }
}