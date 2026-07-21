package com.example.netscope.network;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PingSweepEngine {

    private static final String TAG = "PingSweep";
    private static final int NUM_THREADS = 40;

    public interface ScanListener {
        void onDeviceFound(String ip, String name);
        void onScanComplete(List<String> activeIps);
    }

    public void startScan(final List<String> ipsToScan, final ScanListener listener) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final List<String> activeIps = new ArrayList<>();

        for (String targetIp : ipsToScan) {
            executor.execute(() -> {
                if (isHostAlive(targetIp)) {
                    synchronized (activeIps) { activeIps.add(targetIp); }
                    if (listener != null) listener.onDeviceFound(targetIp, "Dispositivo Detectado");
                }
            });
        }

        executor.shutdown();
        new Thread(() -> {
            try {
                executor.awaitTermination(120, TimeUnit.SECONDS);
                if (listener != null) listener.onScanComplete(activeIps);
            } catch (InterruptedException e) {
                Log.e(TAG, "Escaneo interrumpido", e);
            }
        }).start();
    }

    // ==========================================================
    // FASE 1: CONFIRMAR VIDA (Ping o Puertos TCP)
    // ==========================================================
    private boolean isHostAlive(String ip) {
        try {
            Process process = java.lang.Runtime.getRuntime().exec("ping -c 1 -W 1 " + ip);
            if (process.waitFor() == 0) return true;
        } catch (Exception e) {}

        int[] puertosComunes = {80, 443, 53};
        for (int puerto : puertosComunes) {
            if (isPortOpen(ip, puerto)) return true;
        }
        return false;
    }

    // ==========================================================
    // FASE 2: EXTRACCIÓN HÍBRIDA DE IDENTIDAD (ARP -> MAC -> PUERTOS)
    // ==========================================================
    public static String resolveDeviceIdentity(String ip) {
        // Intento 1: Robar la MAC de la tabla ARP de Android
        String mac = extractMacFromArp(ip);

        // Intento 2: Si logramos robar la MAC, consultamos al fabricante oficial
        if (mac != null) {
            String vendor = queryMacVendorApi(mac);
            if (!vendor.equals("Genérico")) {
                return vendor; // Ej: "Espressif Systems" (ESP32) o "Apple, Inc."
            }
        }

        // Intento 3: Si Android 14 nos bloqueó la MAC, hacemos Fingerprinting por puertos
        return guessBrandByPorts(ip);
    }

    // TÉCNICA AGRESIVA: Leer caché ARP local
    private static String extractMacFromArp(String ip) {
        // Intento A: Leer el archivo puro del Kernel
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(ip)) {
                    String[] parts = line.trim().split(" +");
                    if (parts.length >= 4) {
                        String mac = parts[3].toUpperCase();
                        if (mac.matches("..:..:..:..:..:..") && !mac.equals("00:00:00:00:00:00")) {
                            return mac;
                        }
                    }
                }
            }
        } catch (Exception e) {}

        // Intento B: Invocar binario de red (A veces sobrevive en teléfonos chinos/robustos)
        try {
            Process p = Runtime.getRuntime().exec("ip neigh show " + ip);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line != null && line.contains("lladdr")) {
                String[] parts = line.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("lladdr") && i + 1 < parts.length) {
                        return parts[i + 1].toUpperCase();
                    }
                }
            }
        } catch (Exception e) {}

        return null; // Android ganó esta batalla, pasamos a puertos
    }

    // LLAMADA A LA API (Con la MAC robada)
    private static String queryMacVendorApi(String mac) {
        try {
            URL url = new URL("https://api.macvendors.com/" + mac);
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

    // FINGERPRINTING HEURÍSTICO (Plan de Respaldo)
    private static String guessBrandByPorts(String ip) {
        if (isPortOpen(ip, 8060)) return "Roku Streaming Device";
        if (isPortOpen(ip, 8009) || isPortOpen(ip, 8008)) return "Google Cast / Smart TV";
        if (isPortOpen(ip, 445)) return "Máquina Windows / Servidor SMB";
        if (isPortOpen(ip, 62078)) return "Dispositivo Apple (AirPlay)";
        if (isPortOpen(ip, 1900)) return "Dispositivo IoT (UPnP)"; // Focos, microondas, Alexa
        if (isPortOpen(ip, 554)) return "Cámara de Seguridad IP";

        return "Dispositivo Genérico";
    }

    private static boolean isPortOpen(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 150);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}