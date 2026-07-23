package com.mecx.netscope.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class NsdResolver {

    private static final String TAG = "NsdResolver";

    // Ampliamos el radar a los 5 protocolos más comunes en IoT (Refris, Tvs, Bocinas)
    private static final String[] SERVICE_TYPES = {
            "_http._tcp.",             // Paneles Web (Routers, Refris Samsung, ESP32)
            "_googlecast._tcp.",       // Chromecasts y Android TVs
            "_spotify-connect._tcp.",  // Bocinas Inteligentes (Alexa, Sonos)
            "_airplay._tcp.",          // Dispositivos Apple (Apple TV, Mac)
            "_ipp._tcp."               // Impresoras (Epson, HP)
    };

    private NsdManager nsdManager;
    private List<NsdManager.DiscoveryListener> activeListeners = new ArrayList<>();
    private WifiManager.MulticastLock multicastLock;

    public interface NameResolveListener {
        void onNameResolved(String ip, String name);
    }

    public NsdResolver(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("CyberSweep_mDNS_Lock");
            multicastLock.setReferenceCounted(true);
        }
    }

    public void startDiscovery(NameResolveListener uiListener) {
        if (multicastLock != null && !multicastLock.isHeld()) {
            multicastLock.acquire();
        }

        // Lanzamos un radar independiente por cada protocolo
        for (String serviceType : SERVICE_TYPES) {
            NsdManager.DiscoveryListener listener = createDiscoveryListener(uiListener);
            activeListeners.add(listener);
            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener);
            } catch (Exception e) {
                Log.e(TAG, "Error iniciando NSD para " + serviceType, e);
            }
        }
    }

    private NsdManager.DiscoveryListener createDiscoveryListener(NameResolveListener uiListener) {
        return new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {}

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        InetAddress host = serviceInfo.getHost();
                        if (host != null) {
                            String ip = host.getHostAddress();
                            String name = serviceInfo.getServiceName();
                            if (uiListener != null) uiListener.onNameResolved(ip, name);
                        }
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {}
            @Override
            public void onDiscoveryStopped(String serviceType) {}
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {}
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {}
        };
    }

    public void stopDiscovery() {
        for (NsdManager.DiscoveryListener listener : activeListeners) {
            try {
                nsdManager.stopServiceDiscovery(listener);
            } catch (Exception e) {}
        }
        activeListeners.clear();

        if (multicastLock != null && multicastLock.isHeld()) {
            multicastLock.release();
        }
    }
}