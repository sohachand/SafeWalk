package com.example.project.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.project.R;

/**
 * Real runtime permission requests for Location (required), Contacts and Microphone (optional
 * but recorded - used later by the "Alert Emergency Contact" and audio-recording-on-alert flow).
 */
public class PermissionsSetupActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 1;
    private static final int REQ_CONTACTS = 2;
    private static final int REQ_MIC = 3;

    private TextView locationStatus;
    private TextView contactsStatus;
    private TextView micStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_setup);

        locationStatus = findViewById(R.id.locationStatus);
        contactsStatus = findViewById(R.id.contactsStatus);
        micStatus = findViewById(R.id.micStatus);
        Button continueButton = findViewById(R.id.continueButton);

        locationStatus.setOnClickListener(v -> ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION));
        contactsStatus.setOnClickListener(v -> ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS));
        micStatus.setOnClickListener(v -> ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC));

        continueButton.setOnClickListener(v -> {
            if (!granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Location access is required for SafeWalk to work",
                        Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
                return;
            }
            startActivity(new Intent(this, TrustedCircleSetupActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatuses();
    }

    private void refreshStatuses() {
        setStatus(locationStatus, granted(Manifest.permission.ACCESS_FINE_LOCATION));
        setStatus(contactsStatus, granted(Manifest.permission.READ_CONTACTS));
        setStatus(micStatus, granted(Manifest.permission.RECORD_AUDIO));
    }

    private void setStatus(TextView view, boolean isGranted) {
        view.setText(isGranted ? "Allowed" : "Allow");
        view.setTextColor(ContextCompat.getColor(this,
                isGranted ? R.color.risk_low : R.color.accent_purple));
    }

    private boolean granted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshStatuses();
    }
}
