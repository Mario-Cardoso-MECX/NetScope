package com.example.netscope.network;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PingSweepEngine {

    private static final String TAG = "PingSweep";
    private static final int TIMEOUT_MS = 500;
    // Subimos a 100 hilos para procesar bloques masivos de IPs sin crashear
    private static final int NUM_THREADS = 100;

    public interface ScanListener {
        void onDeviceFound(String ip, String name);
        void onScanComplete(List<String> activeIps);
    }

    public static String getVendorFromMac(String ip) {
        try {
            URL url = new URL("https://api.macvendors.com/" + ip);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                return reader.readLine();
            }
        } catch (Exception e) {
            return "Genérico";
        }
        return "Genérico";
    }

    // AHORA RECIBE UNA LISTA DE IPs EN LUGAR DE UN STRING BÁSICO
    public void startScan(final List<String> ipsToScan, final ScanListener listener) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final List<String> activeIps = new ArrayList<>();

        for (String targetIp : ipsToScan) {
            executor.execute(() -> {
                try {
                    InetAddress address = InetAddress.getByName(targetIp);
                    if (address.isReachable(TIMEOUT_MS)) {
                        String hostName = address.getHostName();
                        String finalName = (hostName != null && !hostName.equals(targetIp)) ? hostName : "Dispositivo Detectado";
                        synchronized (activeIps) { activeIps.add(targetIp); }
                        if (listener != null) listener.onDeviceFound(targetIp, finalName);
                    }
                } catch (Exception ignored) {}
            });
        }

        executor.shutdown();
        new Thread(() -> {
            try {
                // Aumentamos el tiempo de espera a 120 seg por si la red es de +1000 dispositivos
                executor.awaitTermination(120, TimeUnit.SECONDS);
                if (listener != null) listener.onScanComplete(activeIps);
            } catch (InterruptedException e) {
                Log.e(TAG, "Escaneo interrumpido", e);
            }
        }).start();
    }
}