package com.example.project.walk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.project.R;
import com.example.project.cloud.CloudSync;
import com.example.project.cloud.FirebaseGate;
import com.example.project.cloud.LiveShare;
import com.example.project.data.RiskPoint;
import com.example.project.data.SessionStore;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;
import com.example.project.history.WalkSummaryActivity;
import com.example.project.risk.FuzzyRiskEngine;
import com.example.project.safety.AlertSentActivity;
import com.example.project.safety.FakeCallActivity;
import com.example.project.safety.ManualSosActivity;

import java.util.Calendar;
import java.util.Locale;

/**
 * The real, functional core of SafeWalk: live GPS tracking feeding FuzzyRiskEngine every 15s.
 * Sensitivity is read from SessionStore, set on the Risk Sensitivity settings screen - so that
 * screen genuinely changes how this engine behaves, not just a cosmetic toggle.
 *
 * Also hosts the shake-to-SOS gesture: the accelerometer is watched the whole time a walk is
 * active, and a hard shake jumps straight to ManualSosActivity in its auto-triggered mode.
 */
public class ActiveWalkActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    public static final String EXTRA_FROM = "extra_from";
    public static final String EXTRA_TO = "extra_to";

    private static final float STATIONARY_THRESHOLD_M = 5f;
    private static final long RISK_TICK_INTERVAL_MS = 5_000L;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    // DEMO_TIME_MULTIPLIER speeds up how fast "stationary minutes" builds up, purely for live
    // demos - it does NOT touch the fuzzy engine's logic or thresholds, it just compresses the
    // wall-clock time fed into it. At 1f, "Long" stationary (fully triggers around 8 real
    // minutes on STANDARD sensitivity) behaves exactly as designed for real-world use. At 8f,
    // the same fuzzy threshold is reached in about 1 real minute of standing still, which is
    // practical to show live in a classroom. Set back to 1f before any real-world/final build.
    private static final float DEMO_TIME_MULTIPLIER = 8f;

    // Shake detection tuning. SHAKE_THRESHOLD_G is in multiples of g (9.81 m/s^2) of combined
    // acceleration minus gravity - a firm deliberate shake comfortably clears 2.0-2.5g, normal
    // handheld walking/pocket jostle stays well under 1g. SHAKE_COOLDOWN_MS stops one shake
    // gesture from firing the detector a dozen times in a row.
    private static final float SHAKE_THRESHOLD_G = 2.2f;
    private static final long SHAKE_COOLDOWN_MS = 2_000L;

    private TextView riskTierText;
    private FrameLayout riskTierBanner;
    private TextView riskScoreText;
    private TextView statsText;
    private TextView routeText;
    private TextView explanationText;
    private String shareCode;
    private Button arriveTimerButton;
    private CountDownTimer arriveTimer;
    private CountDownTimer safetyPromptTimer;
    private AlertDialog safetyPrompt;

    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTimestamp = 0L;

    private final Handler riskHandler = new Handler(Looper.getMainLooper());
    private final GradientDrawable bannerBackground = new GradientDrawable();
    private WalkHistoryStore walkHistoryStore;
    private WalkRecord record;
    private SessionStore session;

    private Location startLocation;
    private Location lastLocation;
    private long lastMovementTimestamp;
    private float deviationMeters = 0f;
    private boolean highRiskDialogShowing = false;
    private boolean ended = false;

    private final Runnable riskTick = new Runnable() {
        @Override
        public void run() {
            computeAndShowRisk();
            riskHandler.postDelayed(this, RISK_TICK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_walk);

        riskTierBanner = findViewById(R.id.riskTierBanner);
        riskTierText = findViewById(R.id.riskTierText);
        riskScoreText = findViewById(R.id.riskScoreText);
        statsText = findViewById(R.id.statsText);
        routeText = findViewById(R.id.routeText);
        explanationText = findViewById(R.id.explanationText);

        bannerBackground.setShape(GradientDrawable.RECTANGLE);
        bannerBackground.setCornerRadius(24f);
        riskTierBanner.setBackground(bannerBackground);

        session = new SessionStore(this);
        walkHistoryStore = new WalkHistoryStore(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        String from = getIntent().getStringExtra(EXTRA_FROM);
        String to = getIntent().getStringExtra(EXTRA_TO);
        routeText.setText((from == null ? "Current Location" : from) + " → " + (to == null ? "Destination" : to));

        record = new WalkRecord();
        record.fromLabel = from;
        record.toLabel = to;
        record.startTimeMillis = System.currentTimeMillis();
        record.finalTier = "LOW";

        if (FirebaseGate.uid(this) != null) {
            shareCode = LiveShare.newCode();
            LiveShare.currentCode = shareCode;
            TextView shareCodeText = findViewById(R.id.shareCodeText);
            shareCodeText.setVisibility(android.view.View.VISIBLE);
            shareCodeText.setText("Live sharing ON - give a trusted contact this code: " + shareCode);
        }

        arriveTimerButton = findViewById(R.id.arriveTimerButton);
        arriveTimerButton.setOnClickListener(v -> {
            if (arriveTimer != null) {
                cancelArriveTimer();
            } else {
                chooseArriveTimer();
            }
        });

        findViewById(R.id.imSafeButton).setOnClickListener(v -> checkIn());
        findViewById(R.id.endWalkButton).setOnClickListener(v -> endWalk());
        findViewById(R.id.manualSosButton).setOnClickListener(v ->
                startActivity(new Intent(this, ManualSosActivity.class)));

        if (hasLocationPermission()) {
            beginTracking();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER || ended) return;

        float gx = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gy = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gz = event.values[2] / SensorManager.GRAVITY_EARTH;
        // Magnitude of acceleration in g, with gravity's resting 1g subtracted out so a
        // stationary phone reads ~0 rather than ~1.
        float magnitude = (float) Math.sqrt(gx * gx + gy * gy + gz * gz) - 1f;

        if (magnitude > SHAKE_THRESHOLD_G) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTimestamp > SHAKE_COOLDOWN_MS) {
                lastShakeTimestamp = now;
                onShakeDetected();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void onShakeDetected() {
        Intent intent = new Intent(this, ManualSosActivity.class);
        intent.putExtra(ManualSosActivity.EXTRA_AUTO_TRIGGERED, true);
        startActivity(intent);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (hasLocationPermission()) {
                beginTracking();
            } else {
                Toast.makeText(this, "Location permission is required to track this walk",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void beginTracking() {
        String provider = bestAvailableProvider();
        if (provider == null) {
            Toast.makeText(this, "No location provider available on this device", Toast.LENGTH_LONG).show();
            return;
        }
        lastMovementTimestamp = System.currentTimeMillis();
        Location last = locationManager.getLastKnownLocation(provider);
        if (last != null) {
            startLocation = last;
            lastLocation = last;
        }
        locationManager.requestLocationUpdates(provider, 5_000L, 0f, this);
        riskHandler.post(riskTick);
    }

    private String bestAvailableProvider() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return LocationManager.GPS_PROVIDER;
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return LocationManager.NETWORK_PROVIDER;
        return null;
    }

    /**
     * "Dead-man's switch": the walker says how long the walk should take. If the timer runs out
     * and they don't answer "I'm safe" within 20 seconds, an SOS is triggered automatically -
     * useful when the person can't reach their phone.
     */
    private void chooseArriveTimer() {
        final String[] labels = {"1 minute (demo)", "10 minutes", "20 minutes", "30 minutes", "60 minutes"};
        final long[] minutes = {1, 10, 20, 30, 60};
        new AlertDialog.Builder(this)
                .setTitle("I should arrive within...")
                .setItems(labels, (d, which) -> startArriveTimer(minutes[which] * 60_000L))
                .show();
    }

    private void startArriveTimer(long millis) {
        cancelArriveTimer();
        arriveTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long left) {
                arriveTimerButton.setText(String.format(Locale.getDefault(),
                        "Arrive-by timer: %02d:%02d (tap to cancel)", left / 60000, (left / 1000) % 60));
            }

            @Override
            public void onFinish() {
                arriveTimer = null;
                arriveTimerButton.setText("Set arrive-by timer");
                askAreYouSafe();
            }
        }.start();
    }

    private void cancelArriveTimer() {
        if (arriveTimer != null) {
            arriveTimer.cancel();
            arriveTimer = null;
        }
        if (arriveTimerButton != null) arriveTimerButton.setText("Set arrive-by timer");
    }

    private void askAreYouSafe() {
        if (ended || isFinishing()) return;
        safetyPrompt = new AlertDialog.Builder(this)
                .setTitle("Are you safe?")
                .setMessage("Your arrive-by time is up. Confirm within 20 seconds or an SOS is sent automatically.")
                .setCancelable(false)
                .setPositiveButton("I'm safe", (d, w) -> dismissSafetyPrompt())
                .create();
        safetyPrompt.show();
        safetyPromptTimer = new CountDownTimer(20_000, 1000) {
            @Override
            public void onTick(long left) {
                if (safetyPrompt != null) {
                    safetyPrompt.setMessage("Confirm within " + (left / 1000 + 1)
                            + " seconds or an SOS is sent automatically.");
                }
            }

            @Override
            public void onFinish() {
                dismissSafetyPrompt();
                if (ended || isFinishing()) return;
                Intent sos = new Intent(ActiveWalkActivity.this, ManualSosActivity.class);
                sos.putExtra(ManualSosActivity.EXTRA_AUTO_TRIGGERED, true);
                startActivity(sos);
            }
        }.start();
    }

    private void dismissSafetyPrompt() {
        if (safetyPromptTimer != null) {
            safetyPromptTimer.cancel();
            safetyPromptTimer = null;
        }
        if (safetyPrompt != null) {
            safetyPrompt.dismiss();
            safetyPrompt = null;
        }
    }

    private void checkIn() {
        lastMovementTimestamp = System.currentTimeMillis();
        Toast.makeText(this, "Check-in recorded - you're marked as safe for now.", Toast.LENGTH_SHORT).show();
        computeAndShowRisk();
    }

    @SuppressLint("MissingPermission")
    private void endWalk() {
        if (ended) return;
        ended = true;
        cancelArriveTimer();
        dismissSafetyPrompt();
        if (locationManager != null) locationManager.removeUpdates(this);
        riskHandler.removeCallbacks(riskTick);
        record.endTimeMillis = System.currentTimeMillis();
        walkHistoryStore.save(record);
        CloudSync.uploadWalk(this, record);
        if (shareCode != null) {
            LiveShare.stop(this, shareCode);
            LiveShare.currentCode = null;
        }

        Intent intent = new Intent(this, WalkSummaryActivity.class);
        intent.putExtra(WalkSummaryActivity.EXTRA_WALK_ID, record.id);
        startActivity(intent);
        finish();
    }

    private void computeAndShowRisk() {
        if (lastLocation == null || startLocation == null) {
            return;
        }
        float stationaryMinutes = (System.currentTimeMillis() - lastMovementTimestamp) / 60000f * DEMO_TIME_MULTIPLIER;

        Calendar now = Calendar.getInstance();
        double hoursSince6pm = FuzzyRiskEngine.hoursSince6pmFromClock(
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        FuzzyRiskEngine.RiskExplanation explanation = FuzzyRiskEngine.computeRiskExplained(
                stationaryMinutes, deviationMeters, hoursSince6pm, session.getSensitivity());
        double risk = explanation.risk;
        FuzzyRiskEngine.RiskTier tier = FuzzyRiskEngine.tierFor(risk);

        record.peakRisk = Math.max(record.peakRisk, risk);
        record.finalTier = tier.name();
        RiskPoint sample = new RiskPoint(System.currentTimeMillis(), risk);
        sample.lat = lastLocation.getLatitude();
        sample.lng = lastLocation.getLongitude();
        record.timeline.add(sample);
        walkHistoryStore.save(record);

        riskScoreText.setText(String.format(Locale.getDefault(), "Risk score: %.1f", risk));
        statsText.setText(String.format(Locale.getDefault(), "Stationary: %.1f min · Deviation: %.0f m · Time: %02d:%02d",
                stationaryMinutes, deviationMeters, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
        explanationText.setText(explanation.summary);
        if (shareCode != null) {
            LiveShare.publish(this, shareCode, session.getName() == null || session.getName().isEmpty() ? "SafeWalk user" : session.getName(),
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    risk, tier.name(), explanation.summary);
        }

        int colorRes;
        String tierLabel;
        switch (tier) {
            case HIGH:
                colorRes = R.color.risk_high;
                tierLabel = "HIGH";
                break;
            case MEDIUM:
                colorRes = R.color.risk_medium;
                tierLabel = "MEDIUM";
                break;
            default:
                colorRes = R.color.risk_low;
                tierLabel = "LOW";
        }
        bannerBackground.setColor(ContextCompat.getColor(this, colorRes));
        riskTierText.setText(tierLabel);

        if (tier == FuzzyRiskEngine.RiskTier.HIGH) {
            maybeShowHighRiskDialog();
        } else {
            highRiskDialogShowing = false;
        }
    }

    private void maybeShowHighRiskDialog() {
        if (highRiskDialogShowing || isFinishing()) return;
        highRiskDialogShowing = true;
        if (!record.triggers.contains("High risk detected")) {
            record.triggers.add("High risk detected");
        }
        new AlertDialog.Builder(this)
                .setTitle("High risk detected")
                .setMessage("SafeWalk thinks your situation may be risky right now. What would you like to do?")
                .setPositiveButton("Alert trusted circle", (dialog, which) ->
                        startActivity(new Intent(this, AlertSentActivity.class)))
                .setNeutralButton("Fake incoming call", (dialog, which) ->
                        startActivity(new Intent(this, FakeCallActivity.class)))
                .setNegativeButton("I'm safe", (dialog, which) -> checkIn())
                .setOnDismissListener(dialog -> highRiskDialogShowing = false)
                .setCancelable(false)
                .show();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (startLocation == null) startLocation = location;
        if (lastLocation != null) {
            float movedSinceLast = location.distanceTo(lastLocation);
            if (movedSinceLast >= STATIONARY_THRESHOLD_M) {
                lastMovementTimestamp = System.currentTimeMillis();
            }
        }
        lastLocation = location;
        deviationMeters = startLocation.distanceTo(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (!ended) {
            Toast.makeText(this, provider + " was turned off", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("End this walk?")
                .setMessage("Going back will end tracking for this walk.")
                .setPositiveButton("End Walk", (d, w) -> endWalk())
                .setNegativeButton("Keep walking", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelArriveTimer();
        dismissSafetyPrompt();
        riskHandler.removeCallbacks(riskTick);
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException ignored) {
            }
        }
    }
}
