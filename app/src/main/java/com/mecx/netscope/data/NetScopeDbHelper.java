package com.mecx.netscope.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NetScopeDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "netscope_history.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HISTORY = "scans";
    public static final String COL_ID = "id";
    public static final String COL_IP = "ip";
    public static final String COL_NAME = "name";
    public static final String COL_STATUS = "status";
    public static final String COL_TIMESTAMP = "timestamp";

    public NetScopeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_IP + " TEXT, " +
                COL_NAME + " TEXT, " +
                COL_STATUS + " TEXT, " +
                COL_TIMESTAMP + " TEXT)"; // Cambiamos DATETIME por TEXT para inyectarle la hora exacta de Java
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // Truco para sacar la hora exacta del celular en formato legible
    private String getHoraActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * LÓGICA INTELIGENTE DE UPSERT (Update / Insert)
     */
    public void guardarEscaneoCompleto(List<Device> devices) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            String horaEscaneo = getHoraActual();

            for (Device d : devices) {
                // Buscamos si la IP ya existe en la base de datos
                Cursor cursor = db.rawQuery("SELECT 1 FROM " + TABLE_HISTORY + " WHERE " + COL_IP + "=?", new String[]{d.getIp()});
                boolean isNew = (cursor.getCount() == 0);
                cursor.close();

                ContentValues values = new ContentValues();
                values.put(COL_NAME, d.getName());
                values.put(COL_TIMESTAMP, horaEscaneo); // Siempre actualizamos a la hora de AHORITA

                if (isNew) {
                    // 1. ES NUEVO: Lo insertamos como intruso
                    values.put(COL_IP, d.getIp());
                    values.put(COL_STATUS, "NUEVO / POSIBLE INTRUSO");
                    db.insert(TABLE_HISTORY, null, values);
                } else {
                    // 2. YA EXISTE: Solo actualizamos su fecha, su nombre y lo ponemos en verde
                    values.put(COL_STATUS, "CONOCIDO");
                    db.update(TABLE_HISTORY, values, COL_IP + "=?", new String[]{d.getIp()});
                }
            }
            db.setTransactionSuccessful();
            Log.d("DB_SCOPE", "Historial actualizado inteligente sin clones.");
        } catch (Exception e) {
            Log.e("DB_SCOPE", "Error guardando historial", e);
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public Cursor getAllHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Los ordenamos para que los que se acaban de escanear salgan hasta arriba
        return db.rawQuery("SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COL_TIMESTAMP + " DESC", null);
    }
}