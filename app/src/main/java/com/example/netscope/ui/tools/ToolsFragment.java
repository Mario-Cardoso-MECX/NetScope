package com.example.netscope.ui.tools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.netscope.R;

import java.net.InetAddress;

public class ToolsFragment extends Fragment {

    private EditText etTarget, etPingTarget;
    private TextView tvResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        etTarget = view.findViewById(R.id.etTarget);
        etPingTarget = view.findViewById(R.id.etPingTarget);
        tvResult = view.findViewById(R.id.tvResult);

        Button btnDns = view.findViewById(R.id.btnDnsLookup);
        Button btnPing = view.findViewById(R.id.btnPing);

        btnDns.setOnClickListener(v -> performDnsLookup());
        btnPing.setOnClickListener(v -> performPing());

        return view;
    }

    private void performDnsLookup() {
        String target = etTarget.getText().toString().trim();
        if (target.isEmpty()) return;

        tvResult.setText("Resolviendo DNS...");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(target);
                String result = "Host: " + address.getHostName() + "\nIP: " + address.getHostAddress();
                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText(result));
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText("Error: No se pudo resolver."));
            }
        }).start();
    }

    private void performPing() {
        String target = etPingTarget.getText().toString().trim();
        if (target.isEmpty()) return;

        tvResult.setText("Haciendo Ping a " + target + "...");

        new Thread(() -> {
            try {
                InetAddress address = InetAddress.getByName(target);
                long start = System.currentTimeMillis();
                boolean reachable = address.isReachable(2000);
                long time = System.currentTimeMillis() - start;

                String result = reachable ?
                        "Respuesta de " + target + ":\nHost Alcanzable\nTiempo: " + time + "ms" :
                        "Tiempo de espera agotado para " + target;

                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText(result));
            } catch (Exception e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> tvResult.setText("Error en Ping: " + e.getMessage()));
            }
        }).start();
    }
}