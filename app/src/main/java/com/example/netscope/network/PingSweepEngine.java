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
    // Tiempo máximo de espera por IP. 500 ms es el estándar agresivo en pentesting
    private static final int TIMEOUT_MS = 500;
    // Cantidad de hilos paralelos. 50 es un buen balance para no ahogar el procesador del celular
    private static final int NUM_THREADS = 50;

    // Interfaz para mandarle los resultados al UI (al Dev 3)
    public interface ScanListener {
        void onDeviceFound(String ip);
        void onScanComplete(List<String> activeIps);
    }

    /**
     * Inicia el barrido de red.
     * @param subnet La subred a escanear, ej.: "192.168.1." (incluye el punto final)
     * @param listener El callback para recibir los datos
     */
    public void startScan(final String subnet, final ScanListener listener) {
        // Levantamos la piscina de 50 hilos
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        final List<String> activeIps = new ArrayList<>();

        Log.d(TAG, "Iniciando escaneo en subred: " + subnet + "x");

        // Lanzamos las 254 tareas al mismo tiempo
        for (int i = 1; i <= 254; i++) {
            final int lastOctet = i;

            executor.execute(() -> {
                String targetIp = subnet + lastOctet;
                try {
                    InetAddress address = InetAddress.getByName(targetIp);
                    // Aquí ocurre la magia. ¿Está vivo?
                    if (address.isReachable(TIMEOUT_MS)) {
                        Log.d(TAG, "¡Host Vivo Detectado!: " + targetIp);

                        // Bloque sincronizado para evitar que dos hilos choquen al guardar
                        synchronized (activeIps) {
                            activeIps.add(targetIp);
                        }

                        // Le avisamos a la interfaz en tiempo real
                        if (listener != null) {
                            listener.onDeviceFound(targetIp);
                        }
                    }
                } catch (Exception e) {
                    // Si la IP no existe o lanza error, la ignoramos en silencio
                }
            });
        }

        // Le decimos al ejecutor que ya no acepte más tareas
        executor.shutdown();

        // Creamos un hilo supervisor para avisar cuando los 50 albañiles terminen
        new Thread(() -> {
            try {
                // Le damos 5 segundos máximos de tolerancia para terminar todo
                if (executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    Log.d(TAG, "Escaneo finalizado. Total encontrados: " + activeIps.size());
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