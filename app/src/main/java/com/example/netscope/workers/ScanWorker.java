package com.example.netscope.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.netscope.data.Device;
import com.example.netscope.data.NetScopeDbHelper;
import com.example.netscope.network.PingSweepEngine;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ScanWorker extends Worker {

    private static final String CHANNEL_ID = "netscope_threat_alerts";

    public ScanWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // AHORA USAMOS LA LISTA COMPLETA MATEMÁTICA EN LUGAR DEL STRING VIEJO
        List<String> ipsToScan = getIpsToScan();

        if (ipsToScan == null || ipsToScan.isEmpty()) return Result.retry(); // Si no hay Wi-Fi, lo intenta luego

        // Latch para pausar el Worker hasta que el escáner termine
        CountDownLatch latch = new CountDownLatch(1);
        List<Device> dispositivosEncontrados = new ArrayList<>();
        PingSweepEngine engine = new PingSweepEngine();

        // Lanzamos el motor en modo "Silencioso" con la lista de IPs masiva
        engine.startScan(ipsToScan, new PingSweepEngine.ScanListener() {
            @Override
            public void onDeviceFound(String ip, String name) {
                dispositivosEncontrados.add(new Device(name, ip));
            }

            @Override
            public void onScanComplete(List<String> activeIps) {
                latch.countDown(); // Liberamos el proceso
            }
        });

        try {
            latch.await(); // Esperamos pacientemente a que termine el barrido
        } catch (InterruptedException e) {
            return Result.failure();
        }

        // ==========================================
        // LÓGICA DE DETECCIÓN DE INTRUSOS
        // ==========================================
        NetScopeDbHelper dbHelper = new NetScopeDbHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int intrusosDetectados = 0;

        for (Device d : dispositivosEncontrados) {
            // Revisamos si esta IP ya estaba en la base de datos
            Cursor cursor = db.rawQuery("SELECT 1 FROM scans WHERE ip=?", new String[]{d.getIp()});
            if (cursor.getCount() == 0) {
                intrusosDetectados++;
            }
            cursor.close();
        }

        // Finalmente, guardamos todos (hace el UPSERT inteligente que programamos antes)
        dbHelper.guardarEscaneoCompleto(dispositivosEncontrados);

        // Si hay intrusos, disparamos la alarma al teléfono
        if (intrusosDetectados > 0) {
            lanzarNotificacionPush("⚠️ Alerta de Seguridad",
                    "NetScope detectó " + intrusosDetectados + " dispositivo(s) nuevo(s) o no reconocidos en tu red.");
        }

        return Result.success(); // Misión cumplida
    }

    private void lanzarNotificacionPush(String titulo, String mensaje) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // A partir de Android 8 es obligatorio crear un "Canal" de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alertas de Red", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Ícono de peligro nativo
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // =======================================================
    // LA NUEVA LÓGICA DE HACKEO DE SUBREDES (BITWISE MATH)
    // =======================================================
    private List<String> getIpsToScan() {
        List<String> ips = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.isLoopback() || !intf.isUp()) continue;

                // InterfaceAddress nos da tanto la IP como la Máscara real del router
                for (java.net.InterfaceAddress intfAddr : intf.getInterfaceAddresses()) {
                    InetAddress inetAddress = intfAddr.getAddress();

                    if (inetAddress instanceof Inet4Address) {
                        int prefixLength = intfAddr.getNetworkPrefixLength();
                        byte[] ipBytes = inetAddress.getAddress();

                        // Convertimos los bytes de la IP a un número entero de 32 bits
                        int ipInt = ((ipBytes[0] & 0xFF) << 24) |
                                ((ipBytes[1] & 0xFF) << 16) |
                                ((ipBytes[2] & 0xFF) << 8)  |
                                (ipBytes[3] & 0xFF);

                        // Calculamos la máscara y los límites exactos de la red
                        int maskInt = 0xFFFFFFFF << (32 - prefixLength);
                        int networkInt = ipInt & maskInt;
                        int broadcastInt = networkInt | ~maskInt;

                        // Iteramos solo en las IPs válidas de ese segmento gigante
                        for (int i = networkInt + 1; i < broadcastInt; i++) {
                            String ipStr = ((i >> 24) & 0xFF) + "." +
                                    ((i >> 16) & 0xFF) + "." +
                                    ((i >> 8) & 0xFF) + "." +
                                    (i & 0xFF);
                            ips.add(ipStr);
                        }
                        return ips;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}