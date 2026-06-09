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
    private static final int NUM_THREADS = 50;

    public interface ScanListener {
        void onDeviceFound(String ip, String name);
        void onScanComplete(List<String> activeIps);
    }

    // Método para obtener el fabricante de forma asíncrona
    public static String getVendorFromMac(String ip) {
        try {
            // Usamos una API pública de OUI (macvendors.com o similar)
            URL url = new URL("https://api.macvendors.com/" + ip); // Nota: esto es conceptual, ajustaremos según la API real
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

    public void startScan(final String subnet, final ScanListener listener) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final List<String> activeIps = new ArrayList<>();

        for (int i = 1; i <= 254; i++) {
            final int lastOctet = i;
            executor.execute(() -> {
                String targetIp = subnet + lastOctet;
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
                executor.awaitTermination(60, TimeUnit.SECONDS);
                if (listener != null) listener.onScanComplete(activeIps);
            } catch (InterruptedException e) {
                Log.e(TAG, "Escaneo interrumpido", e);
            }
        }).start();
    }
}