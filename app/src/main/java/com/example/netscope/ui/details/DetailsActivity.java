package com.example.netscope.ui.details;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.netscope.R;
import com.example.netscope.network.PortScanner;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DetailsActivity extends AppCompatActivity {

    private String targetIp;
    private String targetName;

    private TextView tvName, tvIp;
    private MaterialButton btnAudit;

    // Referencias a los textos de los puertos
    private TextView p21, p22, p80, p443, p3306, p3389;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // 1. Recibimos los datos del viaje (Intent)
        targetIp = getIntent().getStringExtra("TARGET_IP");
        targetName = getIntent().getStringExtra("TARGET_NAME");

        // 2. Enlazamos la interfaz
        tvName = findViewById(R.id.tvDetailName);
        tvIp = findViewById(R.id.tvDetailIp);
        btnAudit = findViewById(R.id.btnAudit);

        p21 = findViewById(R.id.port21);
        p22 = findViewById(R.id.port22);
        p80 = findViewById(R.id.port80);
        p443 = findViewById(R.id.port443);
        p3306 = findViewById(R.id.port3306);
        p3389 = findViewById(R.id.port3389);

        // Ponemos los datos en pantalla
        tvName.setText(targetName != null ? targetName : "Unknown Target");
        tvIp.setText(targetIp != null ? targetIp : "0.0.0.0");

        // 3. El Botonazo de Auditoría
        btnAudit.setOnClickListener(v -> iniciarAuditoria());
    }

    private void iniciarAuditoria() {
        if (targetIp == null) return;

        btnAudit.setEnabled(false);
        btnAudit.setText(getString(R.string.btn_audit_loading));
        Toast.makeText(this, getString(R.string.toast_audit_start), Toast.LENGTH_SHORT).show();

        // Limpiamos la consola visualmente
        resetearTextos();

        // Instanciamos el código pesado del Dev 2
        PortScanner scanner = new PortScanner();
        scanner.scanCriticalPorts(targetIp, new PortScanner.PortScanListener() {
            @Override
            public void onPortScanned(int port, boolean isOpen) {
                runOnUiThread(() -> actualizarPuertoUI(port, isOpen));
            }

            @Override
            public void onScanFinished(List<Integer> openPorts) {
                runOnUiThread(() -> {
                    btnAudit.setEnabled(true);
                    btnAudit.setText(getString(R.string.btn_audit_ports));
                    Toast.makeText(DetailsActivity.this, getString(R.string.toast_audit_finish, openPorts.size()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void actualizarPuertoUI(int port, boolean isOpen) {
        TextView targetText = null;
        String baseText = "";

        switch (port) {
            case 21: targetText = p21; baseText = "[ 21 ] FTP - "; break;
            case 22: targetText = p22; baseText = "[ 22 ] SSH - "; break;
            case 80: targetText = p80; baseText = "[ 80 ] HTTP - "; break;
            case 443: targetText = p443; baseText = "[ 443 ] HTTPS - "; break;
            case 3306: targetText = p3306; baseText = "[ 3306 ] MySQL - "; break;
            case 3389: targetText = p3389; baseText = "[ 3389 ] RDP - "; break;
        }

        if (targetText != null) {
            if (isOpen) {
                targetText.setText(baseText + getString(R.string.port_open));
                targetText.setTextColor(Color.parseColor("#00FF7F")); // Verde hacker
            } else {
                targetText.setText(baseText + getString(R.string.port_closed));
                targetText.setTextColor(Color.parseColor("#FF5555")); // Rojo
            }
        }
    }

    private void resetearTextos() {
        int colorGris = Color.parseColor("#888888");
        String escaneando = getString(R.string.port_scanning);

        p21.setText("[ 21 ] FTP - " + escaneando); p21.setTextColor(colorGris);
        p22.setText("[ 22 ] SSH - " + escaneando); p22.setTextColor(colorGris);
        p80.setText("[ 80 ] HTTP - " + escaneando); p80.setTextColor(colorGris);
        p443.setText("[ 443 ] HTTPS - " + escaneando); p443.setTextColor(colorGris);
        p3306.setText("[ 3306 ] MySQL - " + escaneando); p3306.setTextColor(colorGris);
        p3389.setText("[ 3389 ] RDP - " + escaneando); p3389.setTextColor(colorGris);
    }
}