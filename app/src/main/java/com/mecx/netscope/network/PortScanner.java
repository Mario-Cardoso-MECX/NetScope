package com.mecx.netscope.network;

import android.util.Log;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PortScanner {

    private static final String TAG = "PortScanner";
    private static final int TIMEOUT_MS = 800; // Un poco más de tiempo para que el puerto responda

    // Los puertos más críticos que vamos a auditar
    private static final int[] CRITICAL_PORTS = {
            21,   // FTP (Transferencia de archivos)
            22,   // SSH (Acceso a consola remota)
            80,   // HTTP (Servidor web no seguro)
            443,  // HTTPS (Servidor web seguro)
            3306, // MySQL (Base de datos)
            3389  // RDP (Escritorio remoto Windows)
    };

    public interface PortScanListener {
        void onPortScanned(int port, boolean isOpen);
        void onScanFinished(List<Integer> openPorts);
    }

    public void scanCriticalPorts(String ipAddress, PortScanListener listener) {
        // Un hilo por cada puerto para hacerlo instantáneo
        ExecutorService executor = Executors.newFixedThreadPool(CRITICAL_PORTS.length);
        List<Integer> openPorts = new ArrayList<>();

        for (int port : CRITICAL_PORTS) {
            final int currentPort = port;
            executor.execute(() -> {
                boolean isOpen = false;
                try {
                    // Intentamos abrir un Socket TCP directo contra ese puerto
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ipAddress, currentPort), TIMEOUT_MS);
                    socket.close();
                    isOpen = true; // Si no crasheó, el puerto está abierto

                    synchronized (openPorts) {
                        openPorts.add(currentPort);
                    }
                    Log.d(TAG, "¡ALERTA! Puerto abierto en " + ipAddress + ":" + currentPort);
                } catch (Exception e) {
                    // Puerto cerrado o filtrado por Firewall
                }

                if (listener != null) {
                    listener.onPortScanned(currentPort, isOpen);
                }
            });
        }

        executor.shutdown();
        new Thread(() -> {
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
                if (listener != null) {
                    listener.onScanFinished(openPorts);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Escaneo de puertos interrumpido");
            }
        }).start();
    }
}