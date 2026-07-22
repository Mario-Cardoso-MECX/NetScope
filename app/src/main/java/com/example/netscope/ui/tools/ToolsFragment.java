package com.example.netscope.ui.tools;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.netscope.R;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ToolsFragment extends Fragment {

    // Variables de UI (Adaptadas a tus IDs exactos del XML)
    private EditText etTarget;
    private View btnPortScanner; // Ahora es un View genérico porque en tu XML es un LinearLayout
    private TextView tvConsole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        // ==========================================================
        // MAPEO EXACTO DE VISTAS (Conectando el XML con Java)
        // ==========================================================
        etTarget = view.findViewById(R.id.etTarget);
        btnPortScanner = view.findViewById(R.id.btnPortScanner);
        tvConsole = view.findViewById(R.id.tvResult);

        // TRUCO UX: Hacemos que la consola tenga scroll nativo con el dedo
        if (tvConsole != null) {
            tvConsole.setMovementMethod(new ScrollingMovementMethod());
        }

        // ==========================================================
        // EVENTOS DE CLICK
        // ==========================================================
        if (btnPortScanner != null) {
            btnPortScanner.setOnClickListener(v -> iniciarEscanerPuertos());
        }

        return view;
    }

    // ==========================================================
    // MÉTODO PARA INYECTAR TEXTO A LA CONSOLA EN TIEMPO REAL
    // ==========================================================
    private void imprimirEnConsola(String texto) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                tvConsole.append("\n" + texto);

                // Auto-scroll táctico para que siempre se vea la última línea
                int scrollAmount = tvConsole.getLayout().getLineTop(tvConsole.getLineCount()) - tvConsole.getHeight();
                if (scrollAmount > 0) {
                    tvConsole.scrollTo(0, scrollAmount);
                } else {
                    tvConsole.scrollTo(0, 0);
                }
            });
        }
    }

    // ==========================================================
    // MOTOR ASÍNCRONO DEL ESCÁNER DE PUERTOS OBJETIVO
    // ==========================================================
    private void iniciarEscanerPuertos() {
        String targetInput = etTarget.getText().toString().trim();

        if (targetInput.isEmpty()) {
            imprimirEnConsola("> [ERROR] Ingresa una IP o dominio válido.");
            return;
        }

        // Bloqueamos la tarjeta para no lanzar escaneos duplicados
        btnPortScanner.setEnabled(false);
        btnPortScanner.setAlpha(0.5f);

        tvConsole.setText("> Inicializando Motor NetScope Nmap-lite...");
        imprimirEnConsola("> Objetivo fijado: " + targetInput);

        new Thread(() -> {
            // TRUCO DE INGENIERO: Resolver el dominio y FORZAR IPv4
            String targetIp = targetInput;
            try {
                imprimirEnConsola("> Resolviendo objetivo...");

                // Pedimos TODAS las IPs (IPv4 e IPv6)
                java.net.InetAddress[] direcciones = java.net.InetAddress.getAllByName(targetInput);

                // Buscamos a la fuerza la IPv4 clásica para que los sockets no fallen
                for (java.net.InetAddress direccion : direcciones) {
                    if (direccion instanceof java.net.Inet4Address) {
                        targetIp = direccion.getHostAddress();
                        break; // Encontramos la IPv4, salimos del ciclo
                    }
                }

                // Si el usuario puso un dominio, le mostramos la IP descubierta
                if (!targetInput.equals(targetIp)) {
                    imprimirEnConsola("> IP Resuelta (IPv4): " + targetIp);
                }
            } catch (Exception e) {
                imprimirEnConsola("> [ERROR] No se pudo resolver el dominio. Verifica tu internet.");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnPortScanner.setEnabled(true);
                        btnPortScanner.setAlpha(1.0f);
                    });
                }
                return; // Cortamos la ejecución si el dominio no existe
            }

            imprimirEnConsola("> Lanzando 50 hilos de auditoría TCP...");
            String finalTarget = targetIp; // Necesario para el lambda de los hilos

            // Pool de 50 hilos
            ExecutorService executor = Executors.newFixedThreadPool(50);

            // Escaneamos los puertos del 1 al 1024
            for (int i = 1; i <= 1024; i++) {
                final int puerto = i;
                executor.execute(() -> {
                    try (Socket socket = new Socket()) {
                        // Subimos a 500ms para mayor precisión en internet/Wi-Fi
                        socket.connect(new InetSocketAddress(finalTarget, puerto), 500);
                        String servicio = deducirServicio(puerto);
                        imprimirEnConsola("> [ABIERTO] Puerto " + puerto + " (" + servicio + ")");
                    } catch (Exception e) {}
                });
            }

            // Puertos pesados extra
            int[] puertosExtra = {1433, 1521, 3306, 3389, 5432, 5900, 8000, 8080, 8443};
            for (int puerto : puertosExtra) {
                executor.execute(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(finalTarget, puerto), 500);
                        String servicio = deducirServicio(puerto);
                        imprimirEnConsola("> [ABIERTO] Puerto " + puerto + " (" + servicio + ")");
                    } catch (Exception e) {}
                });
            }

            // Rutina de limpieza
            executor.shutdown();
            try {
                executor.awaitTermination(45, TimeUnit.SECONDS);
                imprimirEnConsola("> [✓] Escaneo completado con éxito.");
            } catch (InterruptedException e) {
                imprimirEnConsola("> [!] Escaneo abortado por el sistema.");
            }

            // Reactivamos la tarjeta
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    btnPortScanner.setEnabled(true);
                    btnPortScanner.setAlpha(1.0f);
                });
            }
        }).start();
    }

    // ==========================================================
    // DICCIONARIO BÁSICO DE PUERTOS PARA LA UI
    // ==========================================================
    private String deducirServicio(int puerto) {
        switch (puerto) {
            case 21: return "FTP";
            case 22: return "SSH";
            case 23: return "Telnet";
            case 25: return "SMTP";
            case 53: return "DNS";
            case 80: return "HTTP";
            case 110: return "POP3";
            case 139: return "NetBIOS";
            case 443: return "HTTPS";
            case 445: return "SMB / Windows";
            case 554: return "RTSP / Streaming";
            case 3306: return "MySQL";
            case 3389: return "RDP";
            case 5900: return "VNC";
            case 8080: return "HTTP Alterno";
            default: return "Desconocido";
        }
    }
}