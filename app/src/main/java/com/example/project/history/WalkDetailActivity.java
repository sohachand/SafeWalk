package com.example.project.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class WalkDetailActivity extends AppCompatActivity {

    public static final String EXTRA_WALK_ID = "extra_walk_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_detail);

        String walkId = getIntent().getStringExtra(EXTRA_WALK_ID);
        WalkRecord walk = new WalkHistoryStore(this).getById(walkId);

        TextView detailMeta = findViewById(R.id.detailMeta);
        TextView triggersText = findViewById(R.id.triggersText);
        RiskTimelineView timelineView = findViewById(R.id.riskTimelineView);

        if (walk == null) {
            detailMeta.setText("Walk not found.");
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());
        String route = emptyOr(walk.fromLabel, "Current Location") + " → " + emptyOr(walk.toLabel, "Destination");
        detailMeta.setText(fmt.format(walk.startTimeMillis) + " · " + route + " · " + walk.durationMinutes() + " min");

        timelineView.setPoints(walk.timeline);
        ((TrailView) findViewById(R.id.trailView)).setPoints(walk.timeline);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "Peak risk: %.0f/100 (%s)\n",
                walk.peakRisk, walk.finalTier == null ? "Low" : walk.finalTier));
        if (walk.triggers.isEmpty()) {
            sb.append("Triggers: none");
        } else {
            sb.append("Triggers: ").append(String.join(", ", walk.triggers));
        }
        triggersText.setText(sb.toString());
    }

    private String emptyOr(String value, String fallback) {
        return (value == null || value.isEmpty()) ? fallback : value;
    }
}
