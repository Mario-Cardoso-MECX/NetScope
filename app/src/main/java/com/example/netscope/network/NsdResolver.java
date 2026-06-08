package com.example.netscope.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.net.InetAddress;

public class NsdResolver {

    private static final String TAG = "NsdResolver";

    // Cambiamos HTTP por IPP (Internet Printing Protocol) para cazar tu Epson L3250
    private static final String SERVICE_TYPE_PRINTER = "_ipp._tcp.";

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private WifiManager.MulticastLock multicastLock;

    public interface NameResolveListener {
        void onNameResolved(String ip, String name);
    }

    public NsdResolver(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        // TRUCO NINJA: Obligamos a la antena Wi-Fi del celular a escuchar paquetes mDNS
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("CyberSweep_mDNS_Lock");
            multicastLock.setReferenceCounted(true);
        }
    }

    public void startDiscovery(NameResolveListener uiListener) {
        // Encendemos la antena a la fuerza
        if (multicastLock != null && !multicastLock.isHeld()) {
            multicastLock.acquire();
            Log.d(TAG, "MulticastLock activado: Antena escuchando");
        }

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Búsqueda mDNS iniciada para impresoras");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "¡Servicio encontrado!: " + service.getServiceName());

                nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "Fallo al resolver: " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        InetAddress host = serviceInfo.getHost();
                        if (host != null) {
                            String ip = host.getHostAddress();
                            String name = serviceInfo.getServiceName();

                            if (uiListener != null) {
                                uiListener.onNameResolved(ip, name);
                            }
                        }
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) { }

            @Override
            public void onDiscoveryStopped(String serviceType) { }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                nsdManager.stopServiceDiscovery(this);
            }
        };

        try {
            nsdManager.discoverServices(SERVICE_TYPE_PRINTER, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando NSD", e);
        }
    }

    public void stopDiscovery() {
        if (nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo NSD", e);
            }
        }

        // Apagamos el candado de la antena para no gastar batería
        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
            Log.d(TAG, "MulticastLock liberado");
        }
    }
}