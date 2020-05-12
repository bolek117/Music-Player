package pl.satiw.repeatomusicplayer.errors;


import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Thread.UncaughtExceptionHandler defaultUEH;
    private Thread thread;
    private Throwable ex;

    public CustomExceptionHandler(Thread.UncaughtExceptionHandler defaultUEH) {
        this.defaultUEH = defaultUEH;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        this.thread = thread;
        this.ex = ex;

        String stackTrace = Log.getStackTraceString(ex);
        String time = new Date(System.currentTimeMillis()).toString();
        String message = ex.getMessage();

        String filename = "error_log_repeato_" + time + ".txt";

        String text = message + '\n' + stackTrace;
        if (saveExceptionDetails(filename, text)) {
            defaultUEH.uncaughtException(thread, ex);
        }
    }

    private boolean saveExceptionDetails(String filename, String text) {
        String filenameRegex = "[^a-zA-Z0-9.]+";
        filename = filename.replaceAll(filenameRegex, "_");
        File baseDir = Environment.getExternalStorageDirectory();
        String logPath = baseDir.toString() + File.separatorChar +  filename;

        File logfile = new File(logPath);
        if (!logfile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                logfile.createNewFile();
            } catch (IOException e) {
                defaultUEH.uncaughtException(thread, e);
                return false;
            }
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(logfile, true));
            writer.append(text);
            writer.close();
        } catch(IOException e) {
            defaultUEH.uncaughtException(thread, e);
            return false;
        }

        return true;
    }
}
