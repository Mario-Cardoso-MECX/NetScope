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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ToolsFragment extends Fragment {

    // ==========================================================
    // MEMORIA PERSISTENTE (Solución a la amnesia de Fragments)
    // ==========================================================
    private static final StringBuilder historialConsola = new StringBuilder("> Consola en espera...\n> Ingresa un objetivo y selecciona una herramienta.\n> (Mantén presionado aquí para copiar el resultado)");

    // Variables de UI
    private EditText etTarget;
    private TextView tvConsole;

    // Tarjetas de Herramientas
    private View btnPortScanner, btnDnsLookup, btnPing, btnTraceroute, btnWhois, btnSsl;

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
        btnTraceroute = view.findViewById(R.id.btnTraceroute);
        btnWhois = view.findViewById(R.id.btnWhois);
        btnSsl = view.findViewById(R.id.btnSsl);

        infoPortScanner = view.findViewById(R.id.infoPortScanner);
        infoPing = view.findViewById(R.id.infoPing);
        infoDns = view.findViewById(R.id.infoDns);
        infoTraceroute = view.findViewById(R.id.infoTraceroute);
        infoWhois = view.findViewById(R.id.infoWhois);
        infoSsl = view.findViewById(R.id.infoSsl);

        // ==========================================================
        // UX DE CONSOLA: Recuperar Memoria, Scroll y Portapapeles
        // ==========================================================
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

        // ==========================================================
        // EVENTOS DE CLICK - EJECUCIÓN DE HERRAMIENTAS
        // ==========================================================
        if (btnPortScanner != null) btnPortScanner.setOnClickListener(v -> iniciarEscanerPuertos());
        if (btnDnsLookup != null) btnDnsLookup.setOnClickListener(v -> iniciarBusquedaDNS());
        if (btnPing != null) btnPing.setOnClickListener(v -> iniciarBarridoPing());
        if (btnTraceroute != null) btnTraceroute.setOnClickListener(v -> iniciarTraceroute());
        if (btnWhois != null) btnWhois.setOnClickListener(v -> iniciarWhois());
        if (btnSsl != null) btnSsl.setOnClickListener(v -> iniciarInspectorSSL());

        // ==========================================================
        // EVENTOS DE CLICK - ALERTAS EDUCATIVAS (?)
        // ==========================================================
        if (infoPortScanner != null) infoPortScanner.setOnClickListener(v -> mostrarAlertaEducativa("Escáner de Puertos", "Prueba miles de 'puertas virtuales' (puertos) en un servidor o dispositivo para ver cuáles están abiertas.\n\nÚtil para encontrar cámaras ocultas, bases de datos expuestas o servicios inseguros."));
        if (infoPing != null) infoPing.setOnClickListener(v -> mostrarAlertaEducativa("Barrido Ping", "Envía pequeños paquetes de datos (ecos) al objetivo para verificar si está 'vivo' y respondiendo en la red.\n\nEs como tocar la puerta para ver si hay alguien en casa."));
        if (infoDns != null) infoDns.setOnClickListener(v -> mostrarAlertaEducativa("Búsqueda DNS", "El DNS es el directorio telefónico de Internet.\n\n• Si ingresas un nombre (google.com), busca su IP real.\n• Si ingresas una IP, descubre el nombre escondido detrás."));
        if (infoTraceroute != null) infoTraceroute.setOnClickListener(v -> mostrarAlertaEducativa("Traceroute", "Mapea todo el camino que recorren tus datos por Internet. Te muestra cada 'salto' (router) por el que pasas antes de llegar al objetivo final."));
        if (infoWhois != null) infoWhois.setOnClickListener(v -> mostrarAlertaEducativa("Whois", "Consulta las bases de datos públicas de Internet para decirte quién es el dueño registrado de un dominio web o a qué proveedor pertenece una dirección IP."));
        if (infoSsl != null) infoSsl.setOnClickListener(v -> mostrarAlertaEducativa("Inspector SSL", "Extrae el certificado de seguridad de una página web (el candadito del navegador) para verificar si la conexión está cifrada, quién emitió el certificado y cuándo caduca."));

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
    // 4. TRACEROUTE SIMULADO (TTL Hack)
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
                            respondio = true;
                            break;
                        } else if (linea.contains("64 bytes from")) {
                            imprimirEnConsola("> Salto " + ttl + ": [✓] DESTINO ALCANZADO (" + target + ")");
                            respondio = true;
                            break;
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
    // 5. WHOIS (WebView Fantasma para .mx / RDAP Nativo para otros)
    // ==========================================================
    private void iniciarWhois() {
        String target = etTarget.getText().toString().trim();

        if (target.isEmpty()) {
            imprimirEnConsola("> [ERROR] Ingresa un dominio válido.");
            return;
        }

        cambiarEstadoBoton(btnWhois, false);
        limpiarConsola("> Consultando WHOIS para: " + target);

        // =====================================================
        // DOMINIOS .MX -> MOTOR WEBVIEW JSF INVISIBLE
        // =====================================================
        if (target.toLowerCase().endsWith(".mx")) {
            imprimirEnConsola("> Detectado dominio .MX");
            imprimirEnConsola("> Abriendo navegador fantasma (Bypass JSF activo)...");

            if (getActivity() != null) {
                // El WebView OBLIGATORIAMENTE debe correr en el Hilo Principal (UI Thread)
                getActivity().runOnUiThread(() -> {
                    WhoisWebViewManager webViewManager = new WhoisWebViewManager(requireContext());
                    webViewManager.startWhois(target, new WhoisWebViewManager.WhoisCallback() {
                        @Override
                        public void onSuccess(String rawHtml) {
                            // Procesamos el HTML resultante en un hilo de fondo
                            new Thread(() -> procesarHtmlWhoisMx(rawHtml)).start();
                        }

                        @Override
                        public void onError(String error) {
                            imprimirEnConsola("> " + error);
                            cambiarEstadoBoton(btnWhois, true);
                        }
                    });
                });
            }
        }
        // =====================================================
        // RESTO DE DOMINIOS -> RDAP CON PARSER JSON NATIVO
        // =====================================================
        else {
            new Thread(() -> {
                try {
                    imprimirEnConsola("> Consultando vía estándar RDAP para " + target + "...");
                    URL url = new URL("https://rdap.org/domain/" + target);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

                    conn.setInstanceFollowRedirects(false); // Interceptamos redirecciones
                    conn.setConnectTimeout(25000);
                    conn.setReadTimeout(25000);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

                    int code = conn.getResponseCode();

                    // TRUCO DE INGENIERO: Seguir las redirecciones a Verisign
                    if (code == 301 || code == 302 || code == 307 || code == 308) {
                        String newUrl = conn.getHeaderField("Location");
                        conn = (HttpsURLConnection) new URL(newUrl).openConnection();
                        conn.setConnectTimeout(25000);
                        conn.setReadTimeout(25000);
                        conn.setRequestProperty("Accept", "application/json");
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        code = conn.getResponseCode();
                    }

                    if (code != 200) {
                        imprimirEnConsola("\n> [ERROR] Dominio no encontrado o sin soporte RDAP (Código HTTP: " + code + ")");
                        cambiarEstadoBoton(btnWhois, true);
                        return;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder jsonResponse = new StringBuilder();
                    String linea;
                    while ((linea = in.readLine()) != null) {
                        jsonResponse.append(linea);
                    }
                    in.close();

                    String json = jsonResponse.toString();

                    imprimirEnConsola("\n[✓] Servidor responsable contactado con éxito.\n");
                    imprimirEnConsola("==================================");
                    imprimirEnConsola("📋 REPORTE DE INTELIGENCIA RDAP");
                    imprimirEnConsola("==================================");

                    // USAMOS org.json.JSONObject nativo de Android para extraer los datos reales sin fallas
                    try {
                        org.json.JSONObject rdapJson = new org.json.JSONObject(json);

                        // Dominio
                        imprimirEnConsola("Dominio:");
                        imprimirEnConsola("  " + rdapJson.optString("ldhName", "No disponible"));

                        // Estado
                        imprimirEnConsola("\nEstado:");
                        if (rdapJson.has("status")) {
                            org.json.JSONArray statusArray = rdapJson.getJSONArray("status");
                            StringBuilder estados = new StringBuilder();
                            for(int i = 0; i < statusArray.length(); i++) {
                                estados.append(statusArray.getString(i)).append(i < statusArray.length() - 1 ? " / " : "");
                            }
                            imprimirEnConsola("  " + estados.toString());
                        } else {
                            imprimirEnConsola("  Desconocido");
                        }

                        // Fechas de Registro y Expiración (Buscadas dentro del arreglo "events")
                        String fechaRegistro = "No disponible";
                        String fechaExpiracion = "No disponible";
                        String ultimaActualizacion = "No disponible";

                        if (rdapJson.has("events")) {
                            org.json.JSONArray events = rdapJson.getJSONArray("events");
                            for (int i = 0; i < events.length(); i++) {
                                org.json.JSONObject event = events.getJSONObject(i);
                                if (event.has("eventAction") && event.has("eventDate")) {
                                    String action = event.getString("eventAction");
                                    String date = event.getString("eventDate").replace("T", " ").replace("Z", "");

                                    if (action.equalsIgnoreCase("registration")) fechaRegistro = date;
                                    else if (action.equalsIgnoreCase("expiration")) fechaExpiracion = date;
                                    else if (action.equalsIgnoreCase("last changed")) ultimaActualizacion = date;
                                }
                            }
                        }

                        imprimirEnConsola("\nFechas del Registro:");
                        imprimirEnConsola("  Creado/Registrado: " + fechaRegistro);
                        imprimirEnConsola("  Expira: " + fechaExpiracion);
                        if (!ultimaActualizacion.equals("No disponible")) {
                            imprimirEnConsola("  Última modif.: " + ultimaActualizacion);
                        }

                        // Servidores DNS limpios
                        imprimirEnConsola("\nServidores DNS (Nameservers):");
                        if (rdapJson.has("nameservers")) {
                            org.json.JSONArray nsArray = rdapJson.getJSONArray("nameservers");
                            for(int i = 0; i < nsArray.length(); i++) {
                                org.json.JSONObject ns = nsArray.getJSONObject(i);
                                imprimirEnConsola("  - " + ns.optString("ldhName", "Desconocido"));
                            }
                        } else {
                            imprimirEnConsola("  No expuestos en cabecera pública");
                        }

                    } catch (Exception jsonEx) {
                        // En caso de que un servidor envíe JSON malformado, usamos el parser viejo de respaldo
                        imprimirEnConsola("> [!] Advertencia: No se pudo formatear el JSON. Mostrando datos en bruto.");
                        imprimirEnConsola("Dominio: " + extraerValorJson(json, "ldhName"));
                    }

                    imprimirEnConsola("==================================");
                    imprimirEnConsola("> [✓] Auditoría RDAP completada con éxito.");

                } catch (Exception e) {
                    imprimirEnConsola("> [ERROR] Fallo crítico RDAP: " + e.getMessage());
                } finally {
                    cambiarEstadoBoton(btnWhois, true);
                }
            }).start();
        }
    }

    // Parser dedicado exclusivo para leer el DOM que extrajo el WebView
    private void procesarHtmlWhoisMx(String rawHtml) {
        imprimirEnConsola("\n====================================");
        imprimirEnConsola("📋 WHOIS OFICIAL (.MX)");
        imprimirEnConsola("====================================");

        try {
            // Usamos Jsoup ÚNICAMENTE como parser de texto local (DOM), no para peticiones web
            Document doc = Jsoup.parse(rawHtml);
            Elements tablas = doc.select("table");
            boolean encontroDatos = false;

            for (Element tabla : tablas) {
                // Saltamos el header de búsqueda
                if (tabla.text().contains("Consultar WHOIS") || tabla.text().contains("Nombre de Dominio")) {
                    continue;
                }

                Elements filas = tabla.select("tr");
                for (Element fila : filas) {
                    Elements columnas = fila.select("td, th");
                    if (columnas.size() >= 2) {
                        String clave = columnas.get(0).text().trim();
                        String valor = columnas.get(1).text().trim();

                        // Limpiamos basura HTML residual
                        valor = valor.replace("\u00a0", " ").trim();

                        if (!clave.isEmpty() && !valor.isEmpty()) {
                            imprimirEnConsola(clave + ": " + valor);
                            encontroDatos = true;
                        }
                    } else if (columnas.size() == 1) {
                        String subtitulo = columnas.get(0).text().trim();
                        if (!subtitulo.isEmpty()) {
                            imprimirEnConsola("\n--- " + subtitulo + " ---");
                        }
                    }
                }
            }

            if (!encontroDatos) {
                imprimirEnConsola("> Información protegida o dominio inexistente.");
            }
            imprimirEnConsola("====================================");
            imprimirEnConsola("> Consulta completada.");

        } catch (Exception e) {
            imprimirEnConsola("> [ERROR] Fallo al parsear la estructura web.");
        } finally {
            cambiarEstadoBoton(btnWhois, true);
        }
    }

    private String extraerValorJson(String json, String clave) {
        try {
            String searchKey = "\"" + clave + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return "No disponible";
            startIndex += searchKey.length();
            if (json.charAt(startIndex) == '"') {
                int endIndex = json.indexOf('"', startIndex + 1);
                return json.substring(startIndex + 1, endIndex);
            }
        } catch (Exception e) {}
        return "N/D";
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
                imprimirEnConsola("> [ERROR] No se pudo obtener el certificado. Verifica que la página soporte HTTPS.");
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