package com.salemove;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;

import timber.log.Timber;

public class CobrowsingService extends Service {
    private static final String EXTRA_RESULT_CODE = "result-code";
    private static final String EXTRA_DATA = "data";
    private boolean running;
    private RecordingSession recordingSession;

    public static Intent newIntent(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, CobrowsingService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("Creating Cobrowsing Service");
    }

    @Override public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        if (running) {
            System.out.println("Already running! Ignoring...");
            return START_NOT_STICKY;
        }
        System.out.println("Starting up!");
        running = true;

        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (resultCode == 0 || data == null) {
            throw new IllegalStateException("Result code or data missing.");
        }

        recordingSession =
                new RecordingSession(this, resultCode, data);
        recordingSession.startRecording();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new AssertionError("Not supported");
    }
}
