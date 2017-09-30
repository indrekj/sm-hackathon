package com.salemove;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;

public class StartCobrowsing extends Activity {
    private static final int CREATE_SCREEN_CAPTURE = 4242;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Context context = this.getApplicationContext();
        MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager)
                this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mMediaProjectionManager.createScreenCaptureIntent();
        this.startActivityForResult(intent, CREATE_SCREEN_CAPTURE);

        // Snackbar.make(view, "Your screen is now being shown to a creepy guy", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show();
    }
}
