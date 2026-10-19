package com.example.project.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project.R;

public class PermissionsManagementActivity extends AppCompatActivity {

    private LinearLayout permsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_management);

        permsList = findViewById(R.id.permsList);
        findViewById(R.id.openSettingsButton).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderPermissions();
    }

    private void renderPermissions() {
        permsList.removeAllViews();
        addRow("Location", Manifest.permission.ACCESS_FINE_LOCATION, "Always Allow");
        addRow("Contacts", Manifest.permission.READ_CONTACTS, "Allow");
        addRow("Microphone", Manifest.permission.RECORD_AUDIO, "Allow");
    }

    private void addRow(String label, String permission, String grantedLabel) {
        boolean granted = ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(32, 32, 32, 32);
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_dark));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = 16;
        row.setLayoutParams(rowParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelView.setLayoutParams(labelParams);

        TextView statusView = new TextView(this);
        statusView.setText(granted ? grantedLabel : "Not allowed");
        statusView.setTextColor(ContextCompat.getColor(this, granted ? R.color.risk_low : R.color.risk_high));

        row.addView(labelView);
        row.addView(statusView);
        permsList.addView(row);
    }
}
