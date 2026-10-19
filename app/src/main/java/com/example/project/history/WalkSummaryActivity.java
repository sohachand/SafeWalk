package com.example.project.history;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;
import com.example.project.home.HomeDashboardActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class WalkSummaryActivity extends AppCompatActivity {

    public static final String EXTRA_WALK_ID = "extra_walk_id";
    private String walkId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_summary);

        walkId = getIntent().getStringExtra(EXTRA_WALK_ID);
        WalkRecord walk = new WalkHistoryStore(this).getById(walkId);

        TextView details = findViewById(R.id.summaryDetails);
        if (walk != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("d MMM, h:mm a", Locale.getDefault());
            String tierLabel = walk.finalTier == null ? "Low" : capitalize(walk.finalTier);
            details.setText(String.format(Locale.getDefault(),
                    "Date: %s\nFrom: %s\nTo: %s\nDuration: %d min\nPeak Risk: %.0f/100 (%s)",
                    fmt.format(walk.startTimeMillis),
                    emptyOr(walk.fromLabel, "Current Location"),
                    emptyOr(walk.toLabel, "Destination"),
                    walk.durationMinutes(),
                    walk.peakRisk,
                    tierLabel));
        }

        findViewById(R.id.viewDetailsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, WalkDetailActivity.class);
            intent.putExtra(WalkDetailActivity.EXTRA_WALK_ID, walkId);
            startActivity(intent);
        });

        findViewById(R.id.doneButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String emptyOr(String value, String fallback) {
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1).toLowerCase(Locale.getDefault());
    }
}
