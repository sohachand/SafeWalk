package com.example.project.safety;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.SessionStore;
import com.example.project.data.TrustedCircleStore;
import com.example.project.data.TrustedContact;

import java.util.List;

/**
 * Confirms an alert was triggered. Sends a real SMS (via the device's messaging app) to every
 * saved trusted contact; "live location sharing" and "audio recording" are shown as part of the
 * checklist based on what's actually turned on in Settings, since there's no backend here to
 * really transmit a live location feed or upload audio.
 */
public class AlertSentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_sent);

        SessionStore session = new SessionStore(this);
        TextView checklist = findViewById(R.id.checklistText);
        StringBuilder sb = new StringBuilder();
        final String liveCode = com.example.project.cloud.LiveShare.currentCode;
        sb.append(liveCode != null
                ? "✓ Live location sharing ON - code " + liveCode + "\n"
                : "✓ Live location sharing (local tracking only - sign in to share online)\n");
        sb.append(session.isAudioRecordingOn()
                ? "✓ Audio recording started\n"
                : "✗ Audio recording is off (enable in Settings)\n");

        List<TrustedContact> contacts = new TrustedCircleStore(this).getAll();
        if (contacts.isEmpty()) {
            sb.append("✗ No trusted contacts saved yet");
        } else {
            sb.append(sendSms(contacts, liveCode));
        }
        checklist.setText(sb.toString());

        findViewById(R.id.viewLiveLocationButton).setOnClickListener(v ->
        {
            if (liveCode == null) {
                Toast.makeText(this, "Sign in to share your live location", Toast.LENGTH_LONG).show();
            } else {
                Intent f = new Intent(this, com.example.project.walk.FollowWalkActivity.class);
                f.putExtra(com.example.project.walk.FollowWalkActivity.EXTRA_CODE, liveCode);
                startActivity(f);
            }
        });
        Button siren = findViewById(R.id.sirenButton);
        siren.setOnClickListener(v -> {
            if (SirenStrobe.isRunning()) {
                SirenStrobe.stop();
                siren.setText("Sound siren + strobe");
            } else {
                SirenStrobe.start(this);
                siren.setText("Stop siren");
            }
        });
        findViewById(R.id.cancelAlertButton).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SirenStrobe.stop();
    }

    /** Battery level plus a Google Maps link to the last known position, for the SMS body. */
    private String statusSuffix() {
        StringBuilder sb = new StringBuilder();
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
            int pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (pct > 0) sb.append(" Battery ").append(pct).append("%.");
        } catch (Exception ignored) {
        }
        try {
            android.location.LocationManager lm =
                    (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
            android.location.Location best = null;
            for (String p : lm.getProviders(true)) {
                android.location.Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            String addr = com.example.project.cloud.PlaceApi.lastAddress;
            if (addr != null && !addr.isEmpty()) sb.append(" Near: ").append(addr).append(".");
            if (best != null) {
                sb.append(String.format(java.util.Locale.US,
                        " My location: https://maps.google.com/?q=%.5f,%.5f",
                        best.getLatitude(), best.getLongitude()));
            }
        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private String sendSms(List<TrustedContact> contacts, String liveCode) {
        String body = "SafeWalk alert: I may be in an unsafe situation. Please check on me."
                + (liveCode != null ? " Follow me live in SafeWalk > Follow a walk, code " + liveCode + "." : "")
                + statusSuffix();

        // Preferred: send automatically in the background, so help still gets called even if the
        // user cannot touch the phone. Needs the SEND_SMS permission (requested when a walk starts).
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager sms = Build.VERSION.SDK_INT >= 31
                        ? getSystemService(SmsManager.class) : SmsManager.getDefault();
                ArrayList<String> parts = sms.divideMessage(body);
                int sent = 0;
                for (TrustedContact c : contacts) {
                    if (c.phone == null || c.phone.trim().isEmpty()) continue;
                    sms.sendMultipartTextMessage(c.phone.trim(), null, parts, null, null);
                    sent++;
                }
                Toast.makeText(this, "Alert SMS sent automatically to " + sent + " contact(s)",
                        Toast.LENGTH_LONG).show();
                return "✓ SMS alert sent automatically to " + sent + " contact(s)";
            } catch (Exception e) {
                // fall through to the manual composer below
            }
        }

        // Fallback: open the messaging app prefilled (user must press Send).
        StringBuilder numbers = new StringBuilder();
        for (TrustedContact c : contacts) {
            if (numbers.length() > 0) numbers.append(";");
            numbers.append(c.phone);
        }
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numbers));
        smsIntent.putExtra("sms_body", body);
        if (smsIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(smsIntent);
            return "! SMS permission missing - messaging app opened, press Send to alert "
                    + contacts.size() + " contact(s)";
        }
        return "✗ Could not send SMS (no SMS permission or messaging app)";
    }
}
