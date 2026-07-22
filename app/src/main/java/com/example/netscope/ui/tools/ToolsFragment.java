package com.example.netscope.ui.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.netscope.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

public class ToolsFragment extends Fragment {

    // ==========================================================
    // MEMORIA PERSISTENTE
    // ==========================================================
    private static final StringBuilder historialConsola = new StringBuilder("> Consola en espera...\n> Ingresa un objetivo y selecciona una herramienta.\n> (Mantén presionado aquí para copiar el resultado)");

    private EditText etTarget;
    private TextView tvConsole;
    private View btnPortScanner, btnDnsLookup, btnPing, btnTraceroute, btnWhois, btnSsl;
    private View infoPortScanner, infoPing, infoDns, infoTraceroute, infoWhois, infoSsl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        etTarget = view.findViewById(R.id.etTarget);
        tvConsole = view.findViewById(R.id.tvResult);

        btnPortScanner = view.findViewById(R.id.btnPortScanner);
        btnDnsLookup = view.findViewById(R.id.btnDnsLookup);
        btnPing = view.findViewById(R.id.btnPing);
        btnTraceroute = view.findViewById(R.id.btnTraceroute);
        btnWhois = view.findViewById(R.id.btnWhois);
        btnSsl = view.findViewById(R.id.btnSsl);

        infoPortScanner = view.findViewById(R.id.infoPortScanner);
        infoPing = view.findViewById(R.id.infoPing);
        infoDns = view.findViewById(R.id.infoDns);
        infoTraceroute = view.findViewById(R.id.infoTraceroute);
        infoWhois = view.findViewById(R.id.infoWhois);
        infoSsl = view.findViewById(R.id.infoSsl);

        if (tvConsole != null) {
            tvConsole.setText(historialConsola.toString());
            tvConsole.setMovementMethod(new ScrollingMovementMethod());
            tvConsole.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Consola NetScope", tvConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "¡Consola copiada al portapapeles!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        if (btnPortScanner != null) btnPortScanner.setOnClickListener(v -> iniciarEscanerPuertos());
        if (btnDnsLookup != null) btnDnsLookup.setOnClickListener(v -> iniciarBusquedaDNS());
        if (btnPing != null) btnPing.setOnClickListener(v -> iniciarBarridoPing());
        if (btnTraceroute != null) btnTraceroute.setOnClickListener(v -> iniciarTraceroute());
        if (btnWhois != null) btnWhois.setOnClickListener(v -> iniciarWhois());
        if (btnSsl != null) btnSsl.setOnClickListener(v -> iniciarInspectorSSL());

        return view;
    }

    private void mostrarAlertaEducativa(String titulo, String mensaje) {
        if (getContext() != null) {
            new MaterialAlertDialogBuilder(getContext()).setTitle("🔍 " + titulo).setMessage(mensaje).setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss()).show();
        }
    }

    private void limpiarConsola(String textoInicial) {
        historialConsola.setLength(0);
        historialConsola.append(textoInicial);
        if (getActivity() != null && tvConsole != null) {
            getActivity().runOnUiThread(() -> tvConsole.setText(historialConsola.toString()));
        }
    }

    private void imprimirEnConsola(String texto) {
        historialConsola.append("\n").append(texto);
        if (getActivity() != null && tvConsole != null) {
            getActivity().runOnUiThread(() -> {
                tvConsole.setText(historialConsola.toString());
                int scrollAmount = tvConsole.getLayout().getLineTop(tvConsole.getLineCount()) - tvConsole.getHeight();
                if (scrollAmount > 0) tvConsole.scrollTo(0, scrollAmount);
                else tvConsole.scrollTo(0, 0);
            });
        }
    }

    private void cambiarEstadoBoton(View boton, boolean habilitar) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                boton.setEnabled(habilitar);
                boton.setAlpha(habilitar ? 1.0f : 0.5f);
            });
        }
    }

    // ==========================================================
    // 1. ESCÁNER DE PUERTOS
    // ==========================================================
    private void iniciarEscanerPuertos() {
        String targetInput = etTarget.getText().toString().trim();
        if (targetInput.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa un objetivo válido."); return; }
        cambiarEstadoBoton(btnPortScanner, false);
        limpiarConsola("> Inicializando Escáner de Puertos...\n> Objetivo: " + targetInput);

        new Thread(() -> {
            String targetIp = targetInput;
            try {
                java.net.InetAddress[] direcciones = java.net.InetAddress.getAllByName(targetInput);
                for (java.net.InetAddress dir : direcciones) {
                    if (dir instanceof java.net.Inet4Address) { targetIp = dir.getHostAddress(); break; }
                }
            } catch (Exception e) {
                imprimirEnConsola("> [ERROR] Dominio inválido.");
                cambiarEstadoBoton(btnPortScanner, true);
                return;
            }

            imprimirEnConsola("> Lanzando hilos TCP...");
            String finalTarget = targetIp;
            ExecutorService executor = Executors.newFixedThreadPool(50);
            for (int i = 1; i <= 1024; i++) {
                final int puerto = i;
                executor.execute(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(finalTarget, puerto), 500);
                        imprimirEnConsola("> [ABIERTO] Puerto " + puerto + " (" + deducirServicio(puerto) + ")");
                    } catch (Exception e) {}
                });
            }
            int[] puertosExtra = {1433, 1521, 3306, 3389, 5432, 5900, 8000, 8080, 8443};
            for (int puerto : puertosExtra) {
                executor.execute(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(finalTarget, puerto), 500);
                        imprimirEnConsola("> [ABIERTO] Puerto " + puerto + " (" + deducirServicio(puerto) + ")");
                    } catch (Exception e) {}
                });
            }
            executor.shutdown();
            try {
                executor.awaitTermination(45, TimeUnit.SECONDS);
                imprimirEnConsola("> [✓] Escaneo completado.");
            } catch (InterruptedException e) { imprimirEnConsola("> [!] Abortado."); }
            cambiarEstadoBoton(btnPortScanner, true);
        }).start();
    }

    // ==========================================================
    // 2. BÚSQUEDA DNS
    // ==========================================================
    private void iniciarBusquedaDNS() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa un objetivo válido."); return; }
        cambiarEstadoBoton(btnDnsLookup, false);
        limpiarConsola("> Interrogatorio DNS para: " + target);

        new Thread(() -> {
            try {
                java.net.InetAddress[] direcciones = java.net.InetAddress.getAllByName(target);
                for (java.net.InetAddress dir : direcciones) {
                    String tipo = (dir instanceof java.net.Inet4Address) ? "IPv4" : "IPv6";
                    imprimirEnConsola("> Registro [" + tipo + "] : " + dir.getHostAddress());
                }
                java.net.InetAddress main = java.net.InetAddress.getByName(target);
                String hostname = main.getCanonicalHostName();
                if (!hostname.equals(main.getHostAddress()) && !hostname.equals(target)) {
                    imprimirEnConsola("> Hostname Inverso: " + hostname);
                }
                imprimirEnConsola("> [✓] Completado.");
            } catch (Exception e) { imprimirEnConsola("> [ERROR] Sin registros DNS."); }
            cambiarEstadoBoton(btnDnsLookup, true);
        }).start();
    }

    // ==========================================================
    // 3. BARRIDO PING
    // ==========================================================
    private void iniciarBarridoPing() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa un objetivo válido."); return; }
        cambiarEstadoBoton(btnPing, false);
        limpiarConsola("> Pinging a: " + target + "...");

        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("ping -c 4 " + target);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String linea;
                while ((linea = reader.readLine()) != null) imprimirEnConsola(linea);
                int exitCode = process.waitFor();
                if (exitCode == 0) imprimirEnConsola("> [✓] Objetivo ALCANZABLE.");
                else imprimirEnConsola("> [!] Objetivo inactivo o filtrado.");
            } catch (Exception e) { imprimirEnConsola("> [ERROR] Fallo al ejecutar Ping."); }
            cambiarEstadoBoton(btnPing, true);
        }).start();
    }

    // ==========================================================
    // 4. TRACEROUTE SIMULADO
    // ==========================================================
    private void iniciarTraceroute() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa un objetivo válido."); return; }
        cambiarEstadoBoton(btnTraceroute, false);
        limpiarConsola("> Trazando ruta hacia: " + target + " (Máx 15 saltos)");

        new Thread(() -> {
            try {
                for (int ttl = 1; ttl <= 15; ttl++) {
                    Process process = Runtime.getRuntime().exec("ping -c 1 -t " + ttl + " -W 1 " + target);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String linea;
                    boolean respondio = false;
                    while ((linea = reader.readLine()) != null) {
                        if (linea.contains("Time to live exceeded") || linea.contains("exceeded")) {
                            imprimirEnConsola("> Salto " + ttl + " detectado: Router intermedio");
                            respondio = true; break;
                        } else if (linea.contains("64 bytes from")) {
                            imprimirEnConsola("> Salto " + ttl + ": [✓] DESTINO ALCANZADO (" + target + ")");
                            respondio = true; break;
                        }
                    }
                    if (!respondio) imprimirEnConsola("> Salto " + ttl + ": * * * (Tiempo agotado)");
                    if (process.waitFor() == 0) break;
                }
                imprimirEnConsola("> [✓] Traceroute finalizado.");
            } catch (Exception e) { imprimirEnConsola("> [ERROR] Fallo en Traceroute."); }
            cambiarEstadoBoton(btnTraceroute, true);
        }).start();
    }

    // ==========================================================
    // 5. WHOIS (Clásico TCP Puerto 43)
    // ==========================================================
    private void iniciarWhois() {
        String target = etTarget.getText().toString().trim();

        if (target.isEmpty()) {
            imprimirEnConsola("> [ERROR] Ingresa un dominio válido.");
            return;
        }

        cambiarEstadoBoton(btnWhois, false);
        limpiarConsola("> Consultando WHOIS (Puerto 43 TCP) para: " + target);

        new Thread(() -> {
            try {
                // Enrutamiento inicial táctico
                String whoisServer = target.toLowerCase().endsWith(".mx") ? "whois.mx" : "whois.iana.org";
                String resultado = realizarConsultaSocketWhois(whoisServer, target);

                // Auto-búsqueda de saltos (Si IANA nos manda a Verisign u otro)
                String nextServer = buscarServidorReferencia(resultado);
                if (nextServer != null && !nextServer.isEmpty() && !nextServer.equals(whoisServer)) {
                    imprimirEnConsola("> Redirección detectada a: " + nextServer);
                    imprimirEnConsola("> Consultando servidor final...");
                    resultado = realizarConsultaSocketWhois(nextServer, target);
                }

                imprimirEnConsola("\n====================================");
                imprimirEnConsola("📋 REPORTE WHOIS CRUDO");
                imprimirEnConsola("====================================");

                String[] lineas = resultado.split("\n");
                boolean recibioDatos = false;

                for (String linea : lineas) {
                    // Limpiamos comentarios y líneas vacías para no ensuciar la consola
                    if (!linea.trim().isEmpty() && !linea.startsWith("%") && !linea.startsWith("#") && !linea.startsWith(">>>")) {
                        imprimirEnConsola(linea.trim());
                        recibioDatos = true;
                    }
                }

                if (!recibioDatos) {
                    imprimirEnConsola("> [!] Conexión establecida, pero el servidor no devolvió datos legibles.");
                }

                imprimirEnConsola("====================================");
                imprimirEnConsola("> [✓] Consulta finalizada.");

            } catch (Exception e) {
                imprimirEnConsola("\n> [ERROR] " + e.getMessage());
                imprimirEnConsola("> [!] El Puerto 43 está bloqueado por tu red actual (ISP o Firewall).");
                imprimirEnConsola("> [INFO] Intenta nuevamente conectado a la red de la Universidad.");
            } finally {
                cambiarEstadoBoton(btnWhois, true);
            }
        }).start();
    }

    // Método de bajo nivel para establecer conexión socket cruda (Port 43)
    private String realizarConsultaSocketWhois(String server, String target) throws Exception {
        imprimirEnConsola("> Conectando a " + server + ":43...");
        Socket socket = new Socket();
        // Timeout de 10 segundos para no dejar la app colgada si el puerto está dropeado
        socket.connect(new InetSocketAddress(server, 43), 10000);
        socket.setSoTimeout(10000);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.print(target + "\r\n");
        out.flush();

        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = in.readLine()) != null) {
            sb.append(linea).append("\n");
        }
        socket.close();
        return sb.toString();
    }

    // Lógica para detectar si el servidor WHOIS nos manda a otro lado
    private String buscarServidorReferencia(String texto) {
        String[] lineas = texto.split("\n");
        for (String linea : lineas) {
            String lower = linea.toLowerCase().trim();
            if (lower.startsWith("refer:")) {
                return linea.substring(linea.indexOf(":") + 1).trim();
            } else if (lower.startsWith("whois server:")) {
                return linea.substring(linea.indexOf(":") + 1).trim();
            }
        }
        return null;
    }

    // ==========================================================
    // 6. INSPECTOR SSL (Robo de Certificado)
    // ==========================================================
    private void iniciarInspectorSSL() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa un dominio (ej. google.com)."); return; }
        cambiarEstadoBoton(btnSsl, false);
        limpiarConsola("> Iniciando Handshake TLS con: " + target + "...\n> Extrayendo certificado de seguridad...");

        new Thread(() -> {
            try {
                URL url = new URL("https://" + target);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.connect();

                Certificate[] certs = conn.getServerCertificates();
                if (certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) certs[0];
                    imprimirEnConsola("==================================");
                    imprimirEnConsola("🔒 CERTIFICADO DE SEGURIDAD");
                    imprimirEnConsola("==================================");
                    imprimirEnConsola("> Sujeto (Dueño): " + x509.getSubjectDN().getName());
                    imprimirEnConsola("> Emisor (Autoridad): " + x509.getIssuerDN().getName());
                    imprimirEnConsola("> Válido desde: " + x509.getNotBefore().toString());
                    imprimirEnConsola("> Válido hasta: " + x509.getNotAfter().toString());
                    imprimirEnConsola("> Algoritmo de Firma: " + x509.getSigAlgName());
                    imprimirEnConsola("> Versión: V" + x509.getVersion());
                    imprimirEnConsola("==================================");
                }
                conn.disconnect();
                imprimirEnConsola("> [✓] Inspección SSL finalizada.");
            } catch (Exception e) {
                imprimirEnConsola("> [ERROR] No se pudo obtener el certificado. Verifica que soporte HTTPS.");
            }
            cambiarEstadoBoton(btnSsl, true);
        }).start();
    }

    private String deducirServicio(int puerto) {
        switch (puerto) {
            case 21: return "FTP"; case 22: return "SSH"; case 23: return "Telnet";
            case 25: return "SMTP"; case 53: return "DNS"; case 80: return "HTTP";
            case 110: return "POP3"; case 139: return "NetBIOS"; case 443: return "HTTPS";
            case 445: return "SMB / Windows"; case 554: return "RTSP / Streaming";
            case 3306: return "MySQL"; case 3389: return "RDP"; case 5900: return "VNC";
            case 8080: return "HTTP Alterno"; default: return "Desconocido";
        }
    }
}