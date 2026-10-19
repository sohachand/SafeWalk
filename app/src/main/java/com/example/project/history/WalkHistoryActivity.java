package com.example.project.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project.R;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WalkHistoryActivity extends AppCompatActivity {

    private LinearLayout walkList;
    private String activeFilter = "ALL";
    private TextView filterAll, filterLow, filterMedium, filterHigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_history);

        walkList = findViewById(R.id.walkList);
        filterAll = findViewById(R.id.filterAll);
        filterLow = findViewById(R.id.filterLow);
        filterMedium = findViewById(R.id.filterMedium);
        filterHigh = findViewById(R.id.filterHigh);

        filterAll.setOnClickListener(v -> setFilter("ALL"));
        filterLow.setOnClickListener(v -> setFilter("LOW"));
        filterMedium.setOnClickListener(v -> setFilter("MEDIUM"));
        filterHigh.setOnClickListener(v -> setFilter("HIGH"));

        renderList();
    }

    private void setFilter(String filter) {
        activeFilter = filter;
        updateFilterChips();
        renderList();
    }

    private void updateFilterChips() {
        TextView[] chips = {filterAll, filterLow, filterMedium, filterHigh};
        String[] keys = {"ALL", "LOW", "MEDIUM", "HIGH"};
        for (int i = 0; i < chips.length; i++) {
            boolean active = keys[i].equals(activeFilter);
            chips[i].setBackgroundColor(ContextCompat.getColor(this,
                    active ? R.color.accent_purple : R.color.surface_dark));
        }
    }

    private void renderList() {
        walkList.removeAllViews();
        List<WalkRecord> walks = new WalkHistoryStore(this).getAll();
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM, h:mm a", Locale.getDefault());

        for (WalkRecord walk : walks) {
            String tier = walk.finalTier == null ? "LOW" : walk.finalTier;
            if (!activeFilter.equals("ALL") && !activeFilter.equals(tier)) continue;

            View row = LayoutInflater.from(this).inflate(R.layout.item_walk_history, walkList, false);
            ((TextView) row.findViewById(R.id.walkDate)).setText(fmt.format(walk.startTimeMillis));
            String route = emptyOr(walk.fromLabel, "Start") + " → " + emptyOr(walk.toLabel, "Destination");
            ((TextView) row.findViewById(R.id.walkRoute)).setText(route);

            TextView tierView = row.findViewById(R.id.walkTier);
            int colorRes;
            String label;
            switch (tier) {
                case "HIGH":
                    colorRes = R.color.risk_high;
                    label = "High Risk";
                    break;
                case "MEDIUM":
                    colorRes = R.color.risk_medium;
                    label = "Medium Risk";
                    break;
                default:
                    colorRes = R.color.risk_low;
                    label = "Low Risk";
            }
            tierView.setBackgroundColor(ContextCompat.getColor(this, colorRes));
            tierView.setText(label);

            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, WalkDetailActivity.class);
                intent.putExtra(WalkDetailActivity.EXTRA_WALK_ID, walk.id);
                startActivity(intent);
            });
            walkList.addView(row);
        }

        if (walkList.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No walks in this filter yet.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark_secondary));
            empty.setPadding(0, 16, 0, 16);
            walkList.addView(empty);
        }
    }

    private String emptyOr(String value, String fallback) {
        return (value == null || value.isEmpty()) ? fallback : value;
    }
}
