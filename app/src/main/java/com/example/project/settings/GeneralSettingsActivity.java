package com.example.project.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.SessionStore;

public class GeneralSettingsActivity extends AppCompatActivity {

    private SessionStore session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);

        session = new SessionStore(this);

        Switch darkModeSwitch = findViewById(R.id.darkModeSwitch);
        Switch notificationsSwitch = findViewById(R.id.notificationsSwitch);
        Switch fakeCallSwitch = findViewById(R.id.fakeCallSwitch);
        Switch audioRecordingSwitch = findViewById(R.id.audioRecordingSwitch);
        Switch autoStartSwitch = findViewById(R.id.autoStartSwitch);

        darkModeSwitch.setChecked(session.isDarkMode());
        notificationsSwitch.setChecked(session.isNotificationsOn());
        fakeCallSwitch.setChecked(session.isFakeCallEnabled());
        audioRecordingSwitch.setChecked(session.isAudioRecordingOn());
        autoStartSwitch.setChecked(session.isAutoStartWalkOn());

        darkModeSwitch.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> session.setDarkMode(checked));
        notificationsSwitch.setOnCheckedChangeListener((b, checked) -> session.setNotificationsOn(checked));
        fakeCallSwitch.setOnCheckedChangeListener((b, checked) -> session.setFakeCallEnabled(checked));
        audioRecordingSwitch.setOnCheckedChangeListener((b, checked) -> session.setAudioRecordingOn(checked));
        autoStartSwitch.setOnCheckedChangeListener((b, checked) -> session.setAutoStartWalkOn(checked));

        findViewById(R.id.riskSensitivityRow).setOnClickListener(v ->
                startActivity(new Intent(this, RiskSensitivityActivity.class)));
        findViewById(R.id.permissionsRow).setOnClickListener(v ->
                startActivity(new Intent(this, PermissionsManagementActivity.class)));
        findViewById(R.id.languageRow).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Language")
                        .setMessage("Only English is available in this build.")
                        .setPositiveButton("OK", null)
                        .show());
    }
}
