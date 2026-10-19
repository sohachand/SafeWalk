package com.example.project.safety;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

/** Loud alarm tone plus a flashlight strobe, to attract attention during an alert. */
public final class SirenStrobe {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static ToneGenerator tone;
    private static CameraManager cameraManager;
    private static String cameraId;
    private static boolean running;
    private static boolean torchOn;

    private SirenStrobe() {
    }

    public static boolean isRunning() {
        return running;
    }

    public static void start(Context context) {
        if (running) return;
        running = true;
        try {
            tone = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        } catch (RuntimeException e) {
            tone = null;
        }
        try {
            cameraManager = (CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
            String[] ids = cameraManager.getCameraIdList();
            cameraId = ids.length > 0 ? ids[0] : null;
        } catch (Exception e) {
            cameraId = null;
        }
        HANDLER.post(TICK);
    }

    public static void stop() {
        running = false;
        HANDLER.removeCallbacks(TICK);
        if (tone != null) {
            tone.stopTone();
            tone.release();
            tone = null;
        }
        setTorch(false);
    }

    private static void setTorch(boolean on) {
        torchOn = on;
        if (cameraManager == null || cameraId == null) return;
        try {
            cameraManager.setTorchMode(cameraId, on);
        } catch (Exception ignored) {
        }
    }

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (tone != null) tone.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 400);
            setTorch(!torchOn);
            HANDLER.postDelayed(this, 450);
        }
    };
}
