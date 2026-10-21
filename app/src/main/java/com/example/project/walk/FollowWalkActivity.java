package com.example.project.walk;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project.R;
import com.example.project.cloud.FirebaseGate;
import com.example.project.cloud.LiveShare;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;
import java.util.Map;

/** Trusted-contact view: streams another user's live walk (position + fuzzy risk) from Firestore. */
public class FollowWalkActivity extends AppCompatActivity {
    public static final String EXTRA_CODE = "code";

    private EditText codeInput;
    private FrameLayout tierBanner;
    private TextView tierText;
    private TextView statusText;
    private Button openMapsButton;
    private ListenerRegistration registration;
    private double lat;
    private double lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_walk);

        codeInput = findViewById(R.id.codeInput);
        tierBanner = findViewById(R.id.tierBanner);
        tierText = findViewById(R.id.tierText);
        statusText = findViewById(R.id.statusText);
        openMapsButton = findViewById(R.id.openMapsButton);

        findViewById(R.id.followButton).setOnClickListener(v -> startFollowing());
        openMapsButton.setOnClickListener(v -> {
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(this, "Location not available yet (no GPS fix on their phone)",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(String.format(Locale.US, "geo:%f,%f?q=%f,%f(SafeWalk)", lat, lng, lat, lng)));
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Locale.US,
                        "https://www.google.com/maps/search/?api=1&query=%f,%f", lat, lng))));
            }
        });

        String code = getIntent().getStringExtra(EXTRA_CODE);
        if (code != null) {
            codeInput.setText(code);
            startFollowing();
        }
    }

    private void startFollowing() {
        String code = codeInput.getText().toString().trim();
        if (code.length() != 6) {
            codeInput.setError("Enter the 6-digit code");
            return;
        }
        if (!FirebaseGate.isReady(this)) {
            Toast.makeText(this, "Cloud features aren't set up in this build", Toast.LENGTH_LONG).show();
            return;
        }
        if (registration != null) registration.remove();
        statusText.setText("Connecting...");
        registration = LiveShare.listen(code, new LiveShare.Listener() {
            @Override
            public void onUpdate(Map<String, Object> d) {
                render(d);
            }

            @Override
            public void onMissing() {
                tierBanner.setVisibility(View.GONE);
                openMapsButton.setVisibility(View.GONE);
                statusText.setText("No walk found for that code.");
            }

            @Override
            public void onError(String message) {
                statusText.setText("Couldn't follow: " + message
                        + "\n(You may need to sign in first.)");
            }
        });
    }

    private void render(Map<String, Object> d) {
        String name = String.valueOf(d.get("name"));
        String tier = String.valueOf(d.get("tier"));
        double risk = num(d.get("risk"));
        lat = num(d.get("lat"));
        lng = num(d.get("lng"));
        long updated = (long) num(d.get("updatedAt"));
        boolean active = Boolean.TRUE.equals(d.get("active"));

        int color;
        switch (tier) {
            case "HIGH":
                color = R.color.risk_high;
                break;
            case "MEDIUM":
                color = R.color.risk_medium;
                break;
            default:
                color = R.color.risk_low;
        }
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(24f);
        bg.setColor(ContextCompat.getColor(this, color));
        tierBanner.setBackground(bg);
        tierBanner.setVisibility(View.VISIBLE);
        tierText.setText(tier + " RISK");

        String ago = DateUtils.getRelativeTimeSpanString(updated, System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS).toString();
        statusText.setText(String.format(Locale.getDefault(),
                "%s - %s\nRisk score: %.1f\n%s\nLocation: %.5f, %.5f\nUpdated %s",
                name, active ? "walk in progress" : "walk ended", risk,
                d.get("why") == null ? "" : String.valueOf(d.get("why")), lat, lng, ago));
        openMapsButton.setVisibility(View.VISIBLE);
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
