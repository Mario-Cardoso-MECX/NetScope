package com.example.netscope.network;

import android.util.Log;
import java.io.BufferedReader;
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
    // Bajamos a 40 hilos: más sigiloso para no disparar las alarmas de la universidad
    private static final int NUM_THREADS = 40;

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

    public void startScan(final List<String> ipsToScan, final ScanListener listener) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final List<String> activeIps = new ArrayList<>();

        for (String targetIp : ipsToScan) {
            executor.execute(() -> {
                // Usamos nuestro propio método de evasión en lugar del isReachable de Java
                if (isHostAlive(targetIp)) {
                    // Quitamos la resolución DNS de Java porque congela el escáner.
                    // El NsdResolver (mDNS) se encargará de los nombres en el RadarFragment.
                    String finalName = "Dispositivo Detectado";

                    synchronized (activeIps) { activeIps.add(targetIp); }
                    if (listener != null) listener.onDeviceFound(targetIp, finalName);
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
    // LÓGICA DE EVASIÓN: PING NATIVO + TCP STEALTH SCAN
    // ==========================================================
    private boolean isHostAlive(String ip) {
        // INTENTO 1: Ping puro desde el sistema operativo de Android (Kernel Linux)
        try {
            // Mandamos 1 solo paquete (-c 1) esperando máximo 1 segundo (-W 1)
            Process process = java.lang.Runtime.getRuntime().exec("ping -c 1 -W 1 " + ip);
            int returnVal = process.waitFor();
            if (returnVal == 0) {
                return true; // El equipo respondió al Ping nativo
            }
        } catch (Exception e) {
            // Falló la ejecución, pasamos al Plan B
        }

        // INTENTO 2: Evasión TCP. Si el firewall bloquea el Ping, probamos puertas traseras
        int[] puertosComunes = {80, 443, 53};
        for (int puerto : puertosComunes) {
            try {
                Socket socket = new Socket();
                // 300ms de timeout para probar rápido y no quedarnos congelados
                socket.connect(new InetSocketAddress(ip, puerto), 300);
                socket.close();
                return true; // Si logró conectar al puerto, el equipo está vivo
            } catch (Exception e) {
                // El puerto está cerrado o filtrado, probamos el siguiente
            }
        }

        return false; // El equipo es un fantasma o de verdad está apagado
    }
}