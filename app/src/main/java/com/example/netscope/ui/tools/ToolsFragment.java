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

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ToolsFragment extends Fragment {

    // Variables de UI
    private EditText etTarget;
    private TextView tvConsole;

    // Tarjetas de Herramientas
    private View btnPortScanner;
    private View btnDnsLookup;
    private View btnPing;

    // Iconos de Ayuda (?)
    private View infoPortScanner, infoPing, infoDns, infoTraceroute, infoWhois, infoSsl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        // ==========================================================
        // MAPEO DE VISTAS
        // ==========================================================
        etTarget = view.findViewById(R.id.etTarget);
        tvConsole = view.findViewById(R.id.tvResult);

        btnPortScanner = view.findViewById(R.id.btnPortScanner);
        btnDnsLookup = view.findViewById(R.id.btnDnsLookup);
        btnPing = view.findViewById(R.id.btnPing);

        infoPortScanner = view.findViewById(R.id.infoPortScanner);
        infoPing = view.findViewById(R.id.infoPing);
        infoDns = view.findViewById(R.id.infoDns);
        infoTraceroute = view.findViewById(R.id.infoTraceroute);
        infoWhois = view.findViewById(R.id.infoWhois);
        infoSsl = view.findViewById(R.id.infoSsl);

        // ==========================================================
        // UX DE CONSOLA: Scroll y Portapapeles (Copy-Paste Ninja)
        // ==========================================================
        if (tvConsole != null) {
            tvConsole.setMovementMethod(new ScrollingMovementMethod());

            // Al mantener presionado, copia todo el texto
            tvConsole.setOnLongClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Consola NetScope", tvConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getContext(), "¡Consola copiada al portapapeles!", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // ==========================================================
        // EVENTOS DE CLICK - EJECUCIÓN DE HERRAMIENTAS
        // ==========================================================
        if (btnPortScanner != null) btnPortScanner.setOnClickListener(v -> iniciarEscanerPuertos());
        if (btnDnsLookup != null) btnDnsLookup.setOnClickListener(v -> iniciarBusquedaDNS());
        if (btnPing != null) btnPing.setOnClickListener(v -> iniciarBarridoPing()); // <- AGREGA ESTA LÍNEA

        // ==========================================================
        // EVENTOS DE CLICK - ALERTAS EDUCATIVAS (?)
        // ==========================================================
        if (infoPortScanner != null) infoPortScanner.setOnClickListener(v -> mostrarAlertaEducativa(
                "Escáner de Puertos",
                "Prueba miles de 'puertas virtuales' (puertos) en un servidor o dispositivo para ver cuáles están abiertas.\n\nÚtil para encontrar cámaras ocultas, bases de datos expuestas o servicios inseguros."));

        if (infoPing != null) infoPing.setOnClickListener(v -> mostrarAlertaEducativa(
                "Barrido Ping",
                "Envía pequeños paquetes de datos (ecos) al objetivo para verificar si está 'vivo' y respondiendo en la red.\n\nEs como tocar la puerta para ver si hay alguien en casa."));

        if (infoDns != null) infoDns.setOnClickListener(v -> mostrarAlertaEducativa(
                "Búsqueda DNS",
                "El DNS es el directorio telefónico de Internet.\n\n• Si ingresas un nombre (google.com), busca su IP real.\n• Si ingresas una IP, descubre el nombre escondido detrás."));

        if (infoTraceroute != null) infoTraceroute.setOnClickListener(v -> mostrarAlertaEducativa(
                "Traceroute",
                "Mapea todo el camino que recorren tus datos por Internet. Te muestra cada 'salto' (router) por el que pasas antes de llegar al objetivo final."));

        if (infoWhois != null) infoWhois.setOnClickListener(v -> mostrarAlertaEducativa(
                "Whois",
                "Consulta las bases de datos públicas de Internet para decirte quién es el dueño registrado de un dominio web o a qué proveedor pertenece una dirección IP."));

        if (infoSsl != null) infoSsl.setOnClickListener(v -> mostrarAlertaEducativa(
                "Inspector SSL",
                "Extrae el certificado de seguridad de una página web (el candadito del navegador) para verificar si la conexión está cifrada, quién emitió el certificado y cuándo caduca."));

        return view;
    }

    // ==========================================================
    // CREADOR DE ALERTAS EDUCATIVAS (UX PLAY STORE)
    // ==========================================================
    private void mostrarAlertaEducativa(String titulo, String mensaje) {
        if (getContext() != null) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("🔍 " + titulo)
                    .setMessage(mensaje)
                    .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    // ==========================================================
    // MÉTODO PARA INYECTAR TEXTO A LA CONSOLA EN TIEMPO REAL
    // ==========================================================
    private void imprimirEnConsola(String texto) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvConsole.append("\n" + texto);
                int scrollAmount = tvConsole.getLayout().getLineTop(tvConsole.getLineCount()) - tvConsole.getHeight();
                if (scrollAmount > 0) tvConsole.scrollTo(0, scrollAmount);
                else tvConsole.scrollTo(0, 0);
            });
        }
    }

    // ==========================================================
    // MOTOR ASÍNCRONO DEL ESCÁNER DE PUERTOS
    // ==========================================================
    private void iniciarEscanerPuertos() {
        String targetInput = etTarget.getText().toString().trim();
        if (targetInput.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa una IP o dominio válido."); return; }

        btnPortScanner.setEnabled(false);
        btnPortScanner.setAlpha(0.5f);
        tvConsole.setText("> Inicializando Motor NetScope Nmap-lite...");
        imprimirEnConsola("> Objetivo fijado: " + targetInput);

        new Thread(() -> {
            String targetIp = targetInput;
            try {
                imprimirEnConsola("> Resolviendo objetivo...");
                java.net.InetAddress[] direcciones = java.net.InetAddress.getAllByName(targetInput);
                for (java.net.InetAddress direccion : direcciones) {
                    if (direccion instanceof java.net.Inet4Address) { targetIp = direccion.getHostAddress(); break; }
                }
                if (!targetInput.equals(targetIp)) imprimirEnConsola("> IP Resuelta (IPv4): " + targetIp);
            } catch (Exception e) {
                imprimirEnConsola("> [ERROR] No se pudo resolver el dominio.");
                if (getActivity() != null) getActivity().runOnUiThread(() -> { btnPortScanner.setEnabled(true); btnPortScanner.setAlpha(1.0f); });
                return;
            }

            imprimirEnConsola("> Lanzando 50 hilos de auditoría TCP...");
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
                imprimirEnConsola("> [✓] Escaneo completado con éxito.");
            } catch (InterruptedException e) { imprimirEnConsola("> [!] Escaneo abortado por el sistema."); }

            if (getActivity() != null) getActivity().runOnUiThread(() -> { btnPortScanner.setEnabled(true); btnPortScanner.setAlpha(1.0f); });
        }).start();
    }

    // ==========================================================
    // MOTOR ASÍNCRONO DE BÚSQUEDA DNS
    // ==========================================================
    private void iniciarBusquedaDNS() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) { imprimirEnConsola("> [ERROR] Ingresa una IP o dominio válido."); return; }

        btnDnsLookup.setEnabled(false);
        btnDnsLookup.setAlpha(0.5f);
        tvConsole.setText("> Iniciando interrogatorio DNS para: " + target);

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
                imprimirEnConsola("> [✓] Interrogatorio DNS completado.");
            } catch (Exception e) {
                imprimirEnConsola("> [ERROR] No se encontraron registros DNS. Verifica el objetivo.");
            }

            if (getActivity() != null) getActivity().runOnUiThread(() -> { btnDnsLookup.setEnabled(true); btnDnsLookup.setAlpha(1.0f); });
        }).start();
    }

    // ==========================================================
    // MOTOR ASÍNCRONO DE BARRIDO PING (NATIVO LINUX)
    // ==========================================================
    private void iniciarBarridoPing() {
        String targetInput = etTarget.getText().toString().trim();
        if (targetInput.isEmpty()) {
            imprimirEnConsola("> [ERROR] Ingresa una IP o dominio válido.");
            return;
        }

        // Apagamos la tarjeta mientras trabaja
        btnPing.setEnabled(false);
        btnPing.setAlpha(0.5f);
        tvConsole.setText("> Iniciando envío de paquetes ICMP (Ping) a: " + targetInput + "\n> Por favor, espera...");

        new Thread(() -> {
            try {
                // Ejecutamos el comando 'ping' de Linux con 4 paquetes (-c 4)
                Process process = Runtime.getRuntime().exec("ping -c 4 " + targetInput);

                // Leemos la salida de la terminal en tiempo real
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));

                String linea;
                while ((linea = reader.readLine()) != null) {
                    imprimirEnConsola(linea);
                }

                // Esperamos a que el proceso termine para saber si fue exitoso
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    imprimirEnConsola("> [✓] Barrido Ping finalizado. Objetivo ALCANZABLE.");
                } else {
                    imprimirEnConsola("> [!] El objetivo está inactivo o bloqueando paquetes ICMP (Firewall).");
                }

            } catch (Exception e) {
                imprimirEnConsola("> [ERROR] Fallo crítico al ejecutar el comando nativo.");
            }

            // Reactivamos la tarjeta
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    btnPing.setEnabled(true);
                    btnPing.setAlpha(1.0f);
                });
            }
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