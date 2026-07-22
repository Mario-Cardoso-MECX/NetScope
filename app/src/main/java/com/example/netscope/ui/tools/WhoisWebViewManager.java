package com.example.netscope.ui.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WhoisWebViewManager {

    public interface WhoisCallback {
        void onSuccess(String html);
        void onError(String error);
    }

    private WebView webView;
    private final Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean isFinished = false;
    private String targetDomain;
    private WhoisCallback callback;

    @SuppressLint("SetJavaScriptEnabled")
    public WhoisWebViewManager(Context context) {
        timeoutHandler = new Handler(Looper.getMainLooper());

        // Inicializamos el WebView fantasma (sin agregarlo a la interfaz)
        webView = new WebView(context);
        WebSettings settings = webView.getSettings();

        // Configuraciones exactas de un navegador moderno
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(false); // Ahorra memoria y red
        settings.setBlockNetworkImage(true);
        settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // Aceptamos cookies (Vital para JavaServer Faces JSF)
        CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        // Interfaz puente entre el JavaScript de la web y nuestro código Java
        webView.addJavascriptInterface(new HtmlBridge(), "AndroidHtmlBridge");
    }

    public void startWhois(String domain, WhoisCallback callback) {
        this.targetDomain = domain;
        this.callback = callback;
        this.isFinished = false;

        // Timeout de seguridad ampliado (60 segundos)
        timeoutRunnable = () -> {
            if (!isFinished) {
                isFinished = true;
                webView.stopLoading();
                callback.onError("Timeout: La página whois.mx tardó demasiado en responder.");
                destroy();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 100000); // <-- ¡SÚBELE AQUÍ A 60000!

        webView.setWebViewClient(new WebViewClient() {
            private boolean isSearchTriggered = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (isFinished) return;

                // FASE 1: La página de inicio cargó. Inyectamos JS para escribir y hacer clic.
                if (url.equals("https://whois.mx/") && !isSearchTriggered) {
                    isSearchTriggered = true;
                    String jsFillForm = "javascript:(function() {" +
                            "var input = document.getElementById('searchForm:searchTextField');" +
                            "var btn = document.getElementById('searchForm:searchButton');" +
                            "if(input && btn) {" +
                            "   input.value = '" + targetDomain + "';" +
                            "   btn.click();" +
                            "}" +
                            "})()";
                    view.evaluateJavascript(jsFillForm, null);
                }
                // FASE 2: La página recargó tras hacer clic (index.jsf). Extraemos el HTML.
                else if (url.contains("index.jsf") && isSearchTriggered) {
                    // Damos un pequeño respiro de 1 segundo para asegurar que JSF renderizó la tabla
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (!isFinished) {
                            view.evaluateJavascript(
                                    "javascript:window.AndroidHtmlBridge.processHTML(document.documentElement.outerHTML);",
                                    null);
                        }
                    }, 1000);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && !isFinished) {
                    isFinished = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                    callback.onError("Error de conexión al cargar el navegador invisible.");
                    destroy();
                }
            }
        });

        // Lanzamos el ataque simulado
        webView.loadUrl("https://whois.mx/");
    }

    // Puente para recibir el HTML desde el WebView hacia Java
    private class HtmlBridge {
        @JavascriptInterface
        public void processHTML(String html) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!isFinished) {
                    isFinished = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    if (html == null || !html.contains("Detalle del Dominio")) {
                        callback.onError("El dominio no fue encontrado o está protegido.");
                    } else {
                        callback.onSuccess(html);
                    }
                    destroy();
                }
            });
        }
    }

    private void destroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.destroy();
            webView = null;
        }
    }
}