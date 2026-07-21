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
                // Esperamos a que los 40 hilos terminen los pings rápidos
                executor.awaitTermination(120, TimeUnit.SECONDS);

                // ==========================================================
                // TRUCO TÁCTICO: TIEMPO DE GRACIA mDNS (5 Segundos)
                // ==========================================================
                // Mantenemos el hilo "congelado" pero la UI sigue viva.
                // Esto le da a los dispositivos IoT (Refris, Roku, TVs)
                // el tiempo exacto que necesitan para responder con su nombre.
                Log.d(TAG, "Pings terminados. Esperando nombres de red...");
                Thread.sleep(5000);

                // Ahora sí, cerramos el escaneo y guardamos en Base de Datos
                if (listener != null) listener.onScanComplete(activeIps);
            } catch (InterruptedException e) {
                Log.e(TAG, "Escaneo interrumpido", e);
            }
        }).start();
    }

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

    public static String resolveDeviceIdentity(String ip) {
        String mac = extractMacFromArp(ip);
        if (mac != null) {
            String vendor = queryMacVendorApi(mac);
            if (!vendor.equals("Genérico")) {
                return vendor;
            }
        }
        return guessBrandByPorts(ip);
    }

    private static String extractMacFromArp(String ip) {
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

        return null;
    }

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

    // FINGERPRINTING HEURÍSTICO (Modo Sigiloso + Arsenal Completo en Reserva)
    private static String guessBrandByPorts(String ip) {

        // ======================================================================
        // LOS 10 PUERTOS DE ORO (Activos en Modo Sigiloso)
        // ======================================================================
        if (isPortOpen(ip, 7676) || isPortOpen(ip, 8001)) return "Samsung Smart Device";
        if (isPortOpen(ip, 8060)) return "Roku Streaming Device";
        if (isPortOpen(ip, 8009) || isPortOpen(ip, 8008)) return "Google Cast / Smart TV";
        if (isPortOpen(ip, 445)) return "Máquina Windows (SMB)";
        if (isPortOpen(ip, 62078)) return "Apple Device (AirPlay)";
        if (isPortOpen(ip, 5555)) return "Android Device (ADB) / FireTV";
        if (isPortOpen(ip, 22)) return "Servidor Linux / Raspberry Pi (SSH)";
        if (isPortOpen(ip, 9100) || isPortOpen(ip, 631)) return "Impresora de Red";
        if (isPortOpen(ip, 1900)) return "Dispositivo IoT (UPnP)";
        if (isPortOpen(ip, 80) || isPortOpen(ip, 443)) return "Panel Web / Enrutador Local";

        // ======================================================================
        // ARSENAL PESADO (Reglas extras - Comentado para auditoría profunda)
        // ======================================================================
        /*
        // Ecosistemas Smart Home y Streaming adicionales
        if (isPortOpen(ip, 1400)) return "Sonos System / Logitech Harmony";
        if (isPortOpen(ip, 5000) || isPortOpen(ip, 5001)) return "Synology NAS / HomeKit";
        if (isPortOpen(ip, 6668)) return "Tuya / SmartLife IoT Device";

        // Dispositivos de Juego y Entretenimiento
        if (isPortOpen(ip, 3074)) return "Consola Xbox";
        if (isPortOpen(ip, 9304)) return "Consola PlayStation";
        if (isPortOpen(ip, 32400)) return "Plex Media Server";

        // Servicios de Red, Infraestructura e IoT (Desarrollo)
        if (isPortOpen(ip, 1883) || isPortOpen(ip, 8883)) return "Servidor MQTT / Placa ESP32";
        if (isPortOpen(ip, 3389)) return "Windows Remote Desktop (RDP)";
        if (isPortOpen(ip, 5900)) return "Servidor VNC / macOS Screen Share";
        if (isPortOpen(ip, 23)) return "Router / Switch Legacy (Telnet)";
        if (isPortOpen(ip, 21)) return "Servidor FTP / NAS";
        if (isPortOpen(ip, 3306)) return "Servidor Base de Datos (MySQL)";

        // Periféricos y Cámaras
        if (isPortOpen(ip, 554)) return "Cámara de Seguridad IP (RTSP)";
        */

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