package com.example.netscope.ui.details;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.netscope.R;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailsActivity extends AppCompatActivity {

    private TextView tvTargetName, tvTargetIp, tvConsole;
    private TextView port21, port22, port80, port443, port3306, port3389;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Enlaces UI
        tvTargetName = findViewById(R.id.tvTargetName);
        tvTargetIp = findViewById(R.id.tvTargetIp);
        tvConsole = findViewById(R.id.tvConsole);

        port21 = findViewById(R.id.port21);
        port22 = findViewById(R.id.port22);
        port80 = findViewById(R.id.port80);
        port443 = findViewById(R.id.port443);
        port3306 = findViewById(R.id.port3306);
        port3389 = findViewById(R.id.port3389);

        // Recibimos los datos del adaptador
        String ip = getIntent().getStringExtra("TARGET_IP");
        String name = getIntent().getStringExtra("TARGET_NAME");

        tvTargetName.setText(name != null ? name : "Desconocido");
        tvTargetIp.setText(ip != null ? ip : "0.0.0.0");

        if (ip != null) {
            startAggressiveScan(ip);
        }
    }

    private void startAggressiveScan(String ip) {
        int[] portsToScan = {21, 22, 80, 443, 3306, 3389};
        ExecutorService executor = Executors.newFixedThreadPool(portsToScan.length);

        for (int port : portsToScan) {
            executor.execute(() -> scanPortAndGrabBanner(ip, port));
        }
        executor.shutdown(); // Cerramos la admisión de hilos
    }

    private void scanPortAndGrabBanner(String ip, int port) {
        try {
            Socket socket = new Socket();
            // Límite de 500ms para evadir congelamiento (ANR)
            socket.connect(new InetSocketAddress(ip, port), 500);

            // ¡Conexión exitosa! Marcamos de verde.
            updatePortUI(port, true);

            // ==========================================
            // LÓGICA DE BANNER GRABBING
            // ==========================================
            String bannerData = "";
            socket.setSoTimeout(1500); // Damos 1.5s extra para que el servidor responda su saludo

            // Si es un puerto web, forzamos a que nos diga qué servidor es enviando un header falso
            if (port == 80 || port == 443) {
                OutputStream out = socket.getOutputStream();
                out.write("HEAD / HTTP/1.0\r\n\r\n".getBytes());
                out.flush();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 0;

            // Leemos solo las primeras 3 líneas para no saturar la consola
            while ((line = reader.readLine()) != null && lineCount < 3) {
                sb.append("    ").append(line).append("\n");
                lineCount++;
            }
            bannerData = sb.toString().trim();
            socket.close();

            if (!bannerData.isEmpty()) {
                printToConsole("[PUERTO " + port + "] Abierto. Respuesta:\n" + bannerData + "\n");
            } else {
                printToConsole("[PUERTO " + port + "] Abierto. (Sin respuesta visible)\n");
            }

        } catch (Exception e) {
            // Error: El puerto está cerrado o el Firewall lo filtró
            updatePortUI(port, false);
        }
    }

    // Actualiza la UI de las tarjetas (debe correr en el hilo principal)
    private void updatePortUI(int port, boolean isOpen) {
        runOnUiThread(() -> {
            TextView targetView = null;
            switch (port) {
                case 21: targetView = port21; break;
                case 22: targetView = port22; break;
                case 80: targetView = port80; break;
                case 443: targetView = port443; break;
                case 3306: targetView = port3306; break;
                case 3389: targetView = port3389; break;
            }
            if (targetView != null) {
                if (isOpen) {
                    targetView.setBackgroundColor(Color.parseColor("#00FF7F")); // Verde
                    targetView.setTextColor(Color.parseColor("#0D1117"));
                } else {
                    targetView.setBackgroundColor(Color.parseColor("#FF5555")); // Rojo
                    targetView.setTextColor(Color.WHITE);
                }
            }
        });
    }

    private void printToConsole(String message) {
        runOnUiThread(() -> tvConsole.append("> " + message + "\n"));
    }
}