package com.example.netscope.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import com.example.netscope.data.NetScopeDbHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExportHelper {

    public static void exportDatabaseToImage(Context context) {
        NetScopeDbHelper dbHelper = new NetScopeDbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 1. Averiguar cuál fue la fecha/hora exacta del ÚLTIMO escaneo
        Cursor timeCursor = db.rawQuery("SELECT timestamp FROM scans ORDER BY id DESC LIMIT 1", null);
        String lastTimestamp = "";
        if (timeCursor.moveToFirst()) {
            lastTimestamp = timeCursor.getString(0);
        }
        timeCursor.close();

        if (lastTimestamp.isEmpty()) {
            Toast.makeText(context, "No hay escaneos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Extraer SOLAMENTE los equipos de ese escaneo específico
        Cursor cursor = db.rawQuery("SELECT ip, status FROM scans WHERE timestamp = ?", new String[]{lastTimestamp});

        List<String> threatIps = new ArrayList<>();
        int totalHosts = 0;
        int totalThreats = 0;

        if (cursor.moveToFirst()) {
            do {
                String ip = cursor.getString(0);
                String status = cursor.getString(1);

                totalHosts++;
                if (status != null && status.contains("INTRUSO")) {
                    totalThreats++;
                    threatIps.add(ip);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        // 3. Configuramos los pinceles más grandes y legibles
        Paint paintTitle = new Paint();
        paintTitle.setColor(Color.parseColor("#00FF7F")); // Verde Neón
        paintTitle.setTextSize(40f);
        paintTitle.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paintTitle.setAntiAlias(true);

        Paint paintNormal = new Paint();
        paintNormal.setColor(Color.WHITE);
        paintNormal.setTextSize(32f);
        paintNormal.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        paintNormal.setAntiAlias(true);

        Paint paintThreat = new Paint();
        paintThreat.setColor(Color.parseColor("#FF5555")); // Rojo Peligro
        paintThreat.setTextSize(32f);
        paintThreat.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        paintThreat.setAntiAlias(true);

        // 4. Calculamos altura compacta
        int lineHeight = 55;
        int baseLines = 14;
        int threatLines = threatIps.size();

        int width = 900;
        int height = ((baseLines + threatLines) * lineHeight) + 100;
        if (height > 4000) height = 4000;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#121212"));

        int y = 80;
        int x = 40;

        // Dibujar Cabecera usando la fecha real del escaneo
        canvas.drawText("=================================", x, y, paintTitle); y += lineHeight;
        canvas.drawText(" NETSCOPE - REPORTE EJECUTIVO", x, y, paintTitle); y += lineHeight;
        canvas.drawText("=================================", x, y, paintTitle); y += lineHeight;
        canvas.drawText("Fecha: " + lastTimestamp, x, y, paintNormal); y += lineHeight;
        canvas.drawText("---------------------------------", x, y, paintNormal); y += lineHeight;

        // Dibujar Resumen
        canvas.drawText("RESUMEN DE RED:", x, y, paintTitle); y += lineHeight;
        canvas.drawText("Total de Equipos : " + totalHosts, x, y, paintNormal); y += lineHeight;

        if (totalThreats == 0) {
            canvas.drawText("Estado de la Red : LIMPIA", x, y, paintTitle); y += lineHeight;
        } else {
            canvas.drawText("Amenazas Activas : " + totalThreats, x, y, paintThreat); y += lineHeight;
            canvas.drawText("---------------------------------", x, y, paintNormal); y += lineHeight;
            canvas.drawText("DETALLE DE INTRUSOS:", x, y, paintThreat); y += lineHeight;

            int maxDraw = Math.min(threatIps.size(), 40);
            for (int i = 0; i < maxDraw; i++) {
                canvas.drawText("[!] IP: " + threatIps.get(i), x, y, paintThreat); y += lineHeight;
            }
            if (threatIps.size() > 40) {
                canvas.drawText("... y " + (threatIps.size() - 40) + " más ocultas.", x, y, paintNormal); y += lineHeight;
            }
        }

        canvas.drawText("=================================", x, y, paintTitle); y += lineHeight;
        canvas.drawText("Motor de Auditoría: NetScope V2.4", x, y, paintNormal);

        // 5. Exportar y compartir
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "ticket_auditoria.png");
            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri imageUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", imageFile);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.setType("image/png");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(shareIntent, "Enviar Ticket por..."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}