package com.example.indrek.myfancyapp;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import timber.log.Timber;

public class StartEngagement extends AppCompatActivity {
    private final Handler handler = new Handler();
    WebSocket ws = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.plant(new Timber.DebugTree());

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(15000);
                try {
                    // ws = factory.createSocket("ws://cobrowsing-server.herokuapp.com/");
                    ws = factory.createSocket("ws://10.200.0.118:9443/operator");
                    ws.connect();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (WebSocketException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();

        setContentView(R.layout.activity_start_engagement);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Your screen is now being shown to a creepy guy", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                final StartEngagement activity = StartEngagement.this;

                final Runnable runnableCode = new Runnable() {
                    @Override
                    public void run() {
                        activity.sendScreenshot();
                        handler.postDelayed(this, 150);
                    }
                };
                handler.post(runnableCode);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start_engagement, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendScreenshot() {
        Timber.i("Capturing screen %s", Math.random());

        View v1 = getWindow().getDecorView().getRootView();
        int quality = 100;

        v1.setDrawingCacheEnabled(true);
        v1.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        ws.sendBinary(stream.toByteArray());
    }
}