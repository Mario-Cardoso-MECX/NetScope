package com.example.netscope.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.netscope.R;
import com.example.netscope.ui.main.MainActivity;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScanWorker extends Worker {

    private static final String TAG = "NetScope-ScanWorker";
    private static final String CHANNEL_ID = "netscope_alerts";

    public ScanWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "> Iniciando ronda de vigilancia en segundo plano...");

        Context context = getApplicationContext();

        // 1. Verificar conexión Wi-Fi (No queremos escanear en datos móviles)
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
            Log.d(TAG, "> No hay Wi-Fi. Vigilancia abortada.");
            return Result.success();
        }

        // 2. Obtener la IP local y calcular la subred
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();

        // Convertir IP entera a formato String (Ej. 192.168.1.55)
        String ipString = Formatter.formatIpAddress(ipInt);
        String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1); // Extrae "192.168.1."

        Log.d(TAG, "> Subred detectada: " + prefix + "0/24");

        // 3. Ejecutar el barrido de red (Ping Sweep)
        List<String> activeIps = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(40);

        for (int i = 1; i <= 254; i++) {
            final String targetIp = prefix + i;
            executor.execute(() -> {
                try {
                    InetAddress address = InetAddress.getByName(targetIp);
                    // Timeout de 300ms para hacerlo súper rápido en segundo plano
                    if (address.isReachable(300)) {
                        synchronized (activeIps) {
                            activeIps.add(targetIp);
                        }
                    }
                } catch (Exception ignored) { }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "> Barrido interrumpido.");
        }

        Log.d(TAG, "> Dispositivos vivos encontrados: " + activeIps.size());

        // 4. Lógica de Detección de Intrusos (Simulada para conectar a DB después)
        int intrusosDetectados = 0;
        for (String ip : activeIps) {
            // TODO: Aquí llamaremos a tu NetScopeDbHelper para verificar si la IP/MAC está registrada.
            // boolean esConocido = dbHelper.isDeviceKnown(ip);
            // if (!esConocido) { intrusosDetectados++; }

            // Para probar que el Worker funciona, vamos a simular que encontramos un intruso:
            if (ip.endsWith(".99")) { // Condición inventada solo de prueba
                intrusosDetectados++;
            }
        }

        // Simulación: Lanzamos alerta si hay intrusos (Por ahora siempre la lanzaremos si hay más de 3 dispositivos vivos para que veas que funciona)
        if (activeIps.size() > 0) {
            enviarAlertaNotificacion(activeIps.size() + " dispositivos activos", "El escaneo perimetral ha finalizado con éxito.");
        }

        return Result.success();
    }

    // ==========================================================
    // SISTEMA DE ALERTAS (Notificaciones Push Locales)
    // ==========================================================
    private void enviarAlertaNotificacion(String titulo, String mensaje) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Los teléfonos modernos (Android 8+) exigen un Canal de Notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alertas de Seguridad NetScope",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones sobre intrusos en la red local");
            notificationManager.createNotificationChannel(channel);
        }

        // Intención para abrir MainActivity al tocar la notificación
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Construir la notificación gráfica
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este icono o cámbialo por otro tuyo
                .setContentTitle("🛡️ " + titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Lanzar la alerta (ID aleatorio para que no se sobreescriban)
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}