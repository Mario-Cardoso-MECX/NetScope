package com.example.netscope.ui.tools;

import android.os.Bundle;
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

import java.net.InetAddress;

public class ToolsFragment extends Fragment {

    private EditText etTarget;
    private TextView tvResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        etTarget = view.findViewById(R.id.etTarget);
        tvResult = view.findViewById(R.id.tvResult);

        // Enlazamos las tarjetas del Grid (Son LinearLayouts interactivos, no botones planos)
        View btnDns = view.findViewById(R.id.btnDnsLookup);
        View btnPing = view.findViewById(R.id.btnPing);
        View btnWhois = view.findViewById(R.id.btnWhois);
        View btnTraceroute = view.findViewById(R.id.btnTraceroute);

        // Asignamos las funciones reales
        btnDns.setOnClickListener(v -> performDnsLookup());
        btnPing.setOnClickListener(v -> performPing());

        // Dejamos preparados los módulos que requieren desarrollo de bajo nivel
        btnWhois.setOnClickListener(v -> mostrarProximamente("WHOIS"));
        btnTraceroute.setOnClickListener(v -> mostrarProximamente("TRACEROUTE"));

        return view;
    }

    private void performDnsLookup() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) {
            etTarget.setError("Ingresa un objetivo primero");
            return;
        }

        tvResult.setText("> Resolviendo DNS para " + target + "...\n");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(target);
                String result = "> HOST:\n  " + address.getHostName() + "\n\n> IP:\n  " + address.getHostAddress();
                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText(result));
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText("> Error: No se pudo resolver DNS. Verifique su conexión."));
            }
        }).start();
    }

    private void performPing() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) {
            etTarget.setError("Ingresa un objetivo primero");
            return;
        }

        tvResult.setText("> Haciendo Ping a " + target + "...\n");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(target);
                long start = System.currentTimeMillis();
                boolean reachable = address.isReachable(2000);
                long time = System.currentTimeMillis() - start;

                String result = reachable ?
                        "> RESPUESTA DE " + target + ":\n  [OK] Host Alcanzable\n  [TIEMPO] " + time + "ms" :
                        "> ERROR: Tiempo de espera agotado para " + target;

                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText(result));
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText("> Error en Ping: " + e.getMessage()));
            }
        }).start();
    }

    private void mostrarProximamente(String modulo) {
        Toast.makeText(getContext(), "El módulo " + modulo + " se integrará en la fase de auditoría profunda.", Toast.LENGTH_SHORT).show();
    }
}