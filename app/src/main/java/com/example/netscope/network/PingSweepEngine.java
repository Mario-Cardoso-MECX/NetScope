package com.example.netscope.network;

import android.util.Log;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PingSweepEngine {

    private static final String TAG = "PingSweep";
    private static final int TIMEOUT_MS = 500;
    private static final int NUM_THREADS = 50;

    // ¡MODIFICADO! Ahora exige IP y Nombre
    public interface ScanListener {
        void onDeviceFound(String ip, String name);
        void onScanComplete(List<String> activeIps);
    }

    public void startScan(final String subnet, final ScanListener listener) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final List<String> activeIps = new ArrayList<>();

        Log.d(TAG, "Iniciando escaneo en subred: " + subnet + "x");

        for (int i = 1; i <= 254; i++) {
            final int lastOctet = i;

            executor.execute(() -> {
                String targetIp = subnet + lastOctet;
                try {
                    InetAddress address = InetAddress.getByName(targetIp);

                    if (address.isReachable(TIMEOUT_MS)) {

                        // EL PLAN B: Resolución DNS Inversa
                        String hostName = address.getHostName();
                        // Si el router no sabe el nombre, devuelve la misma IP. Lo validamos:
                        String finalName = (hostName != null && !hostName.equals(targetIp)) ? hostName : "Dispositivo Detectado";

                        Log.d(TAG, "¡Host Vivo!: " + targetIp + " | Nombre: " + finalName);

                        synchronized (activeIps) {
                            activeIps.add(targetIp);
                        }

                        if (listener != null) {
                            listener.onDeviceFound(targetIp, finalName);
                        }
                    }
                } catch (Exception e) {
                    // Silencio ante los errores
                }
            });
        }

        executor.shutdown();
        new Thread(() -> {
            try {
                if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.d(TAG, "Escaneo finalizado.");
                    if (listener != null) {
                        listener.onScanComplete(activeIps);
                    }
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "El escaneo fue interrumpido", e);
            }
        }).start();
    }
}