package com.example.netscope.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ListenableWorker.Result; // <--- Importación blindada
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ScanWorker extends Worker {
    public ScanWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Aquí vivirá la lógica invisible del escaneo automático
        return Result.success();
    }
}