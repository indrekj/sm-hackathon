package com.salemove;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.VirtualDisplay;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import timber.log.Timber;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.media.MediaRecorder.OutputFormat.MPEG_4;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoSource.SURFACE;

class RecordingSession {
    static final int NOTIFICATION_ID = 522592;

    private static final String DISPLAY_NAME = "telecine";
    private static final String MIME_TYPE = "video/mp4";

    private final Handler mainThread = new Handler(Looper.getMainLooper());

    private final Context context;
    private final int resultCode;
    private final Intent data;

    // private final Provider<Boolean> showCountDown;
    // private final Provider<Integer> videoSizePercentage;

    // private final NotificationManager notificationManager;
    // private final WindowManager windowManager;
    private final MediaProjectionManager projectionManager;

    private MediaRecorder recorder;
    private MediaProjection projection;
    private VirtualDisplay display;
    private String outputFile;
    private boolean running;
    private long recordingStartNanos;

    RecordingSession(Context context, int resultCode, Intent data) {
        this.context = context;
        this.resultCode = resultCode;
        this.data = data;

        // File picturesDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
        // outputRoot = new File(picturesDir, "Telecine");

        // notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        //  windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        projectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    private RecordingInfo getRecordingInfo() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        int displayHeight = displayMetrics.heightPixels;
        int displayDensity = displayMetrics.densityDpi;
        Timber.i("Display size: %s x %s @ %s", displayWidth, displayHeight, displayDensity);

        Configuration configuration = context.getResources().getConfiguration();
        boolean isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE;
        Timber.i("Display landscape: %s", isLandscape);

        // Get the best camera profile available. We assume MediaRecorder supports the highest.
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        int cameraWidth = camcorderProfile != null ? camcorderProfile.videoFrameWidth : -1;
        int cameraHeight = camcorderProfile != null ? camcorderProfile.videoFrameHeight : -1;
        int cameraFrameRate = camcorderProfile != null ? camcorderProfile.videoFrameRate : 30;
        Timber.i("Camera size: %s x %s framerate: %s", cameraWidth, cameraHeight, cameraFrameRate);

        int sizePercentage = 100;
        Timber.i("Size percentage: %s", sizePercentage);

        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, isLandscape,
                cameraWidth, cameraHeight, cameraFrameRate, sizePercentage);
    }

    public void startRecording() {
        final RecordingSession session = this;
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    session.reallyStartRecording();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public void reallyStartRecording() throws IOException {
        Timber.i("Setting up socket");

        Socket socket = new Socket("0.tcp.ngrok.io", 14499);
        // ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
        // ParcelFileDescriptor read = descriptors[0];
        // ParcelFileDescriptor write = descriptors[1];

        ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(socket);
        socket.getOutputStream().write(123);



        Timber.i("Starting screen recording...");
        RecordingInfo recordingInfo = getRecordingInfo();

        Timber.i("Recording: %s x %s @ %s", recordingInfo.width, recordingInfo.height,
                recordingInfo.density);

        recorder = new MediaRecorder();
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        // recorder.setVideoFrameRate(recordingInfo.frameRate);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        // recorder.setVideoSize(recordingInfo.width, recordingInfo.height);
        // recorder.setVideoEncodingBitRate(8 * 1000 * 1000);
        // recorder.setOutputFile(sender.getFileDescriptor());
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                Timber.i("Recorder info %s %s", what, extra);

            }
        });
        recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mediaRecorder, int what, int extra) {
                System.out.println("AAAAAAAAAAAAAAAAAAAA");
                Timber.e("Error recording %s %s", what, extra);
            }
        });

        Timber.i("Preparing Media Recorder");

        try {
            recorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare MediaRecorder.", e);
        }

        recorder.start();
        running = true;

        Timber.i("Screen recording started.");
    }

    private void stopRecording() {
        Timber.i("Stopping screen recording...");

        if (!running) {
            throw new IllegalStateException("Not running.");
        }
        running = false;

        boolean propagate = false;
        // Stop the projection in order to flush everything to the recorder.
        projection.stop();
        // Stop the recorder which writes the contents to the file.
        recorder.stop();

        propagate = true;

        long recordingStopNanos = System.nanoTime();

        recorder.release();
        display.release();

        Timber.i("Screen recording stopped. Notifying media scanner of new video.");
    }

    static RecordingInfo calculateRecordingInfo(int displayWidth, int displayHeight,
                                                int displayDensity, boolean isLandscapeDevice, int cameraWidth, int cameraHeight,
                                                int cameraFrameRate, int sizePercentage) {
        // Scale the display size before any maximum size calculations.
        displayWidth = displayWidth * sizePercentage / 100;
        displayHeight = displayHeight * sizePercentage / 100;

        if (cameraWidth == -1 && cameraHeight == -1) {
            // No cameras. Fall back to the display size.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        int frameWidth = isLandscapeDevice ? cameraWidth : cameraHeight;
        int frameHeight = isLandscapeDevice ? cameraHeight : cameraWidth;
        if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
            // Frame can hold the entire display. Use exact values.
            return new RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity);
        }

        // Calculate new width or height to preserve aspect ratio.
        if (isLandscapeDevice) {
            frameWidth = displayWidth * frameHeight / displayHeight;
        } else {
            frameHeight = displayHeight * frameWidth / displayWidth;
        }
        return new RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity);
    }

    static final class RecordingInfo {
        final int width;
        final int height;
        final int frameRate;
        final int density;

        RecordingInfo(int width, int height, int frameRate, int density) {
            this.width = width;
            this.height = height;
            this.frameRate = frameRate;
            this.density = density;
        }
    }

    void destroy() {
        if (running) {
            Timber.i("Destroyed while running!");
            stopRecording();
        }
    }
}
