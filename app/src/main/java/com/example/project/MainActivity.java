package com.example.project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.risk.FuzzyRiskEngine;

import java.util.Calendar;
import java.util.Locale;

/**
 * SafeWalk main screen.
 *
 * Tracks the user's live GPS position while a walk is active, derives the three FuzzyRiskEngine
 * inputs from that tracking data (no map / routing API required):
 *   - stationaryMinutes : minutes since the last time the user moved more than STATIONARY_THRESHOLD_M
 *   - deviationMeters    : straight-line distance from where the walk started
 *   - hoursSince6pm      : derived from the device clock
 * and recomputes risk on a fixed interval, updating the UI and offering an SMS alert to a saved
 * emergency contact.
 */
public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final float STATIONARY_THRESHOLD_M = 5f;
    private static final long RISK_TICK_INTERVAL_MS = 15_000L;
    private static final long LOCATION_UPDATE_INTERVAL_MS = 5_000L;
    private static final float LOCATION_UPDATE_MIN_DISTANCE_M = 0f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "safewalk_prefs";
    private static final String PREF_EMERGENCY_NUMBER = "emergency_number";

    private TextView riskTierText;
    private FrameLayout riskTierBanner;
    private TextView riskScoreText;
    private TextView statsText;
    private Spinner sensitivitySpinner;
    private Button startStopButton;
    private EditText emergencyPhoneInput;
    private Button saveContactButton;
    private Button alertButton;

    private LocationManager locationManager;
    private final Handler riskHandler = new Handler(Looper.getMainLooper());
    private final GradientDrawable bannerBackground = new GradientDrawable();

    private boolean walking = false;
    private Location startLocation;
    private Location lastLocation;
    private long lastMovementTimestamp;
    private float deviationMeters = 0f;
    private FuzzyRiskEngine.Sensitivity sensitivity = FuzzyRiskEngine.Sensitivity.STANDARD;
    private boolean highRiskDialogShowing = false;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setUpSensitivitySpinner();
        setUpBannerBackground();
        loadSavedEmergencyContact();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startStopButton.setOnClickListener(v -> onStartStopClicked());
        saveContactButton.setOnClickListener(v -> saveEmergencyContact());
        alertButton.setOnClickListener(v -> sendEmergencyAlert());

        showNotWalkingState();
    }

    private void bindViews() {
        riskTierBanner = findViewById(R.id.riskTierBanner);
        riskTierText = findViewById(R.id.riskTierText);
        riskScoreText = findViewById(R.id.riskScoreText);
        statsText = findViewById(R.id.statsText);
        sensitivitySpinner = findViewById(R.id.sensitivitySpinner);
        startStopButton = findViewById(R.id.startStopButton);
        emergencyPhoneInput = findViewById(R.id.emergencyPhoneInput);
        saveContactButton = findViewById(R.id.saveContactButton);
        alertButton = findViewById(R.id.alertButton);
    }

    private void setUpSensitivitySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sensitivity_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sensitivitySpinner.setAdapter(adapter);
        sensitivitySpinner.setSelection(1); // Standard
        sensitivitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                switch (position) {
                    case 0:
                        sensitivity = FuzzyRiskEngine.Sensitivity.RELAXED;
                        break;
                    case 2:
                        sensitivity = FuzzyRiskEngine.Sensitivity.CAUTIOUS;
                        break;
                    default:
                        sensitivity = FuzzyRiskEngine.Sensitivity.STANDARD;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                sensitivity = FuzzyRiskEngine.Sensitivity.STANDARD;
            }
        });
    }

    private void setUpBannerBackground() {
        bannerBackground.setShape(GradientDrawable.RECTANGLE);
        bannerBackground.setCornerRadius(24f);
        riskTierBanner.setBackground(bannerBackground);
    }

    private void loadSavedEmergencyContact() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String saved = prefs.getString(PREF_EMERGENCY_NUMBER, "");
        if (!saved.isEmpty()) {
            emergencyPhoneInput.setText(saved);
        }
    }

    private void saveEmergencyContact() {
        String number = emergencyPhoneInput.getText().toString().trim();
        if (number.isEmpty()) {
            Toast.makeText(this, R.string.enter_contact_first, Toast.LENGTH_SHORT).show();
            return;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(PREF_EMERGENCY_NUMBER, number)
                .apply();
        Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show();
    }

    private void sendEmergencyAlert() {
        String number = emergencyPhoneInput.getText().toString().trim();
        if (number.isEmpty()) {
            Toast.makeText(this, R.string.enter_contact_first, Toast.LENGTH_SHORT).show();
            return;
        }
        saveEmergencyContact();

        String tierLabel = walking ? riskTierText.getText().toString() : "UNKNOWN";
        String body = getString(R.string.sms_body_template, tierLabel);

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        smsIntent.putExtra("sms_body", body);
        if (smsIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(smsIntent);
        } else {
            Toast.makeText(this, "No messaging app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void onStartStopClicked() {
        if (walking) {
            stopWalk();
        } else {
            if (hasLocationPermission()) {
                startWalk();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
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
                startWalk();
            } else {
                Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startWalk() {
        if (!hasLocationPermission() || locationManager == null) {
            return;
        }

        String provider = bestAvailableProvider();
        if (provider == null) {
            Toast.makeText(this, "No location provider available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        startLocation = null;
        lastLocation = null;
        deviationMeters = 0f;
        lastMovementTimestamp = System.currentTimeMillis();
        highRiskDialogShowing = false;

        Location last = locationManager.getLastKnownLocation(provider);
        if (last != null) {
            startLocation = last;
            lastLocation = last;
        }

        locationManager.requestLocationUpdates(provider, LOCATION_UPDATE_INTERVAL_MS,
                LOCATION_UPDATE_MIN_DISTANCE_M, this);

        walking = true;
        startStopButton.setText(R.string.stop_walk);
        riskHandler.post(riskTick);
    }

    @SuppressLint("MissingPermission")
    private void stopWalk() {
        walking = false;
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        riskHandler.removeCallbacks(riskTick);
        startStopButton.setText(R.string.start_walk);
        showNotWalkingState();
    }

    private String bestAvailableProvider() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    private void showNotWalkingState() {
        bannerBackground.setColor(ContextCompat.getColor(this, R.color.risk_neutral));
        riskTierText.setText(R.string.risk_tier_not_walking);
        riskScoreText.setText(R.string.risk_score_placeholder);
        statsText.setText(R.string.stats_placeholder);
    }

    private void computeAndShowRisk() {
        if (!walking || lastLocation == null || startLocation == null) {
            return;
        }

        float stationaryMinutes = (System.currentTimeMillis() - lastMovementTimestamp) / 60000f;

        Calendar now = Calendar.getInstance();
        double hoursSince6pm = FuzzyRiskEngine.hoursSince6pmFromClock(
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));

        double risk = FuzzyRiskEngine.computeRisk(stationaryMinutes, deviationMeters, hoursSince6pm, sensitivity);
        FuzzyRiskEngine.RiskTier tier = FuzzyRiskEngine.tierFor(risk);

        riskScoreText.setText(getString(R.string.risk_score_format, risk));
        statsText.setText(getString(R.string.stats_format, stationaryMinutes, deviationMeters,
                String.format(Locale.getDefault(), "%02d:%02d",
                        now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))));

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
        if (highRiskDialogShowing || isFinishing()) {
            return;
        }
        highRiskDialogShowing = true;
        new AlertDialog.Builder(this)
                .setTitle(R.string.high_risk_alert_title)
                .setMessage(R.string.high_risk_alert_message)
                .setPositiveButton(R.string.alert_contact, (dialog, which) -> sendEmergencyAlert())
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(dialog -> highRiskDialogShowing = false)
                .show();
    }

    // ---------- LocationListener ----------

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (startLocation == null) {
            startLocation = location;
        }
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
        // No-op: required by the pre-API-29 LocationListener interface.
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // No-op.
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (walking) {
            Toast.makeText(this, provider + " was turned off", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        riskHandler.removeCallbacks(riskTick);
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException ignored) {
                // Permission may already be revoked during teardown; safe to ignore here.
            }
        }
    }
}
