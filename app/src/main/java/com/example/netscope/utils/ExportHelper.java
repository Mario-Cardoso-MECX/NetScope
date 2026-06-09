package com.example.netscope.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import com.example.netscope.data.NetScopeDbHelper;

public class ExportHelper {

    public static void exportHistoryToWhatsApp(Context context) {
        NetScopeDbHelper dbHelper = new NetScopeDbHelper(context);
        Cursor cursor = dbHelper.getAllHistory();

        if (cursor.getCount() == 0) {
            Toast.makeText(context, "El historial está vacío, escanea primero.", Toast.LENGTH_SHORT).show();
            cursor.close();
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append("🔒 *REPORTE DE AUDITORÍA - CYBERSWEEP* 🔒\n\n");

        if (cursor.moveToFirst()) {
            do {
                String ip = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_IP));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_NAME));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_STATUS));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(NetScopeDbHelper.COL_TIMESTAMP));

                report.append("🖥️ *Dispositivo:* ").append(name != null ? name : "Desconocido").append("\n");
                report.append("📍 *IP:* ").append(ip).append("\n");
                report.append("⚠️ *Estado:* ").append(status).append("\n");
                report.append("🕒 *Última vez:* ").append(time).append("\n");
                report.append("---------------------------\n");
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Lanzamos el Intent de compartir apuntando a WhatsApp
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");

        try {
            context.startActivity(sendIntent);
        } catch (Exception e) {
            // Si el usuario no tiene WhatsApp instalado, abrimos el menú general
            Intent shareIntent = Intent.createChooser(sendIntent, "Exportar Reporte");
            context.startActivity(shareIntent);
        }
    }
}