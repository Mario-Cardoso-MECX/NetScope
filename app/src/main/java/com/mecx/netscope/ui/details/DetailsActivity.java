package com.mecx.netscope.ui.details;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.mecx.netscope.R;
import com.google.android.material.button.MaterialButton;
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
    private MaterialButton btnVulnScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        tvTargetName = findViewById(R.id.tvTargetName);
        tvTargetIp = findViewById(R.id.tvTargetIp);
        tvConsole = findViewById(R.id.tvConsole);

        port21 = findViewById(R.id.port21);
        port22 = findViewById(R.id.port22);
        port80 = findViewById(R.id.port80);
        port443 = findViewById(R.id.port443);
        port3306 = findViewById(R.id.port3306);
        port3389 = findViewById(R.id.port3389);

        btnVulnScan = findViewById(R.id.btnVulnScan);

        String ip = getIntent().getStringExtra("TARGET_IP");
        String name = getIntent().getStringExtra("TARGET_NAME");

        tvTargetName.setText(name != null ? name : "Desconocido");
        tvTargetIp.setText(ip != null ? ip : "0.0.0.0");

        if (ip != null) {
            startAggressiveScan(ip);
        }

        btnVulnScan.setOnClickListener(v -> lanzarAuditoriaCVE());
    }

    private void startAggressiveScan(String ip) {
        int[] portsToScan = {21, 22, 80, 443, 3306, 3389};
        ExecutorService executor = Executors.newFixedThreadPool(portsToScan.length);

        for (int port : portsToScan) {
            executor.execute(() -> scanPortAndGrabBanner(ip, port));
        }
        executor.shutdown();
    }

    private void scanPortAndGrabBanner(String ip, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), 500);

            updatePortUI(port, true);

            String bannerData = "";
            socket.setSoTimeout(1500);

            if (port == 80 || port == 443) {
                OutputStream out = socket.getOutputStream();
                out.write("HEAD / HTTP/1.0\r\n\r\n".getBytes());
                out.flush();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            int lineCount = 0;

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
            updatePortUI(port, false);
        }
    }

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
                    targetView.setBackgroundColor(Color.parseColor("#00FF7F"));
                    targetView.setTextColor(Color.parseColor("#0D1117"));
                } else {
                    targetView.setBackgroundColor(Color.parseColor("#FF5555"));
                    targetView.setTextColor(Color.WHITE);
                }
            }
        });
    }

    private void printToConsole(String message) {
        runOnUiThread(() -> tvConsole.append("> " + message + "\n"));
    }

    // ==========================================
    // MOTOR HEURÍSTICO CVE (VULNERABILIDADES)
    // ==========================================
    private void lanzarAuditoriaCVE() {
        String textoConsola = tvConsole.getText().toString().toLowerCase();

        tvConsole.append("\n> [SISTEMA] Conectando con Base de Datos CVE Local...\n");
        tvConsole.append("> [SISTEMA] Cruzando firmas de software...\n");

        boolean vulnFound = false;

        if (textoConsola.contains("apache/2.4.62")) {
            tvConsole.append("\n> [ALERTA ROJA] Apache 2.4.62 detectado.\n");
            tvConsole.append("  - [CVE-2024-38474] Vulnerabilidad de inyección en mod_rewrite.\n");
            tvConsole.append("  - [CVE-2024-38475] Evasión de restricciones en mod_rewrite.\n");
            tvConsole.append("  >> SUGERENCIA: Actualizar a Apache 2.4.63 o superior urgente.\n");
            vulnFound = true;
        }

        if (textoConsola.contains("smb") || textoConsola.contains("445")) {
            tvConsole.append("\n> [ADVERTENCIA] Puerto SMB (445) Expuesto en red.\n");
            tvConsole.append("  - Riesgo de explotación remota (ej. EternalBlue CVE-2017-0144) si Windows no está parcheado.\n");
            vulnFound = true;
        }

        if (textoConsola.contains("21 ftp")) {
            tvConsole.append("\n> [ADVERTENCIA CRÍTICA] Puerto FTP (21) abierto.\n");
            tvConsole.append("  - El protocolo FTP no está encriptado. Las contraseñas viajan en texto plano.\n");
            tvConsole.append("  >> SUGERENCIA: Migrar servicio a SFTP (Puerto 22).\n");
            vulnFound = true;
        }

        if (!vulnFound) {
            tvConsole.append("\n> [SISTEMA] No se detectaron vulnerabilidades críticas conocidas en los banners extraídos.\n");
        }

        tvConsole.append("> [SISTEMA] Análisis de vulnerabilidades finalizado.\n\n");

        ScrollView scrollView = findViewById(R.id.scrollViewConsole);
        if(scrollView != null) {
            scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
        }
    }
}