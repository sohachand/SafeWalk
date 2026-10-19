package com.example.project.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.R;
import com.example.project.data.SessionStore;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;
import com.example.project.history.WalkDetailActivity;
import com.example.project.history.WalkHistoryActivity;
import com.example.project.profile.ProfileActivity;
import com.example.project.settings.GeneralSettingsActivity;
import com.example.project.trustedcircle.TrustedCircleListActivity;
import com.example.project.walk.StartWalkSetupActivity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HomeDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SessionStore session = new SessionStore(this);
        ((TextView) findViewById(R.id.greetingText)).setText(greeting() + ", " + firstName(session.getName()));

        findViewById(R.id.startWalkCard).setOnClickListener(v ->
                startActivity(new Intent(this, StartWalkSetupActivity.class)));
        findViewById(R.id.profileButton).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.seeAllHistory).setOnClickListener(v ->
                startActivity(new Intent(this, WalkHistoryActivity.class)));
        findViewById(R.id.trustedCircleLink).setOnClickListener(v ->
                startActivity(new Intent(this, TrustedCircleListActivity.class)));
        findViewById(R.id.followWalkLink).setOnClickListener(v ->
                startActivity(new Intent(this, com.example.project.walk.FollowWalkActivity.class)));
        findViewById(R.id.settingsLink).setOnClickListener(v ->
                startActivity(new Intent(this, GeneralSettingsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderRecentWalks();
        com.example.project.cloud.CloudSync.pullWalks(this, added -> {
            if (added > 0) renderRecentWalks();
        });
    }

    /** Aggregate stats from the stored walk history. */
    private void renderInsights(List<WalkRecord> walks) {
        TextView insights = findViewById(R.id.insightsText);
        if (walks.isEmpty()) {
            insights.setText("Your safety insights will appear after your first walk.");
            return;
        }
        double sumPeak = 0;
        int high = 0;
        long totalMin = 0;
        int[] byHour = new int[24];
        int[] highByHour = new int[24];
        for (WalkRecord w : walks) {
            sumPeak += w.peakRisk;
            totalMin += w.durationMinutes();
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.setTimeInMillis(w.startTimeMillis);
            int h = c.get(java.util.Calendar.HOUR_OF_DAY);
            byHour[h]++;
            if ("HIGH".equals(w.finalTier)) {
                high++;
                highByHour[h]++;
            }
        }
        double avg = sumPeak / walks.size();
        int score = (int) Math.max(0, Math.min(100, Math.round(100 - avg)));
        int riskyHour = -1;
        for (int h = 0; h < 24; h++) {
            if (highByHour[h] > 0 && (riskyHour < 0 || highByHour[h] > highByHour[riskyHour])) riskyHour = h;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "Your safety insights\nSafety score: %d/100\n", score));
        sb.append(String.format(Locale.getDefault(), "%d walks - %d min walked - %d high-risk\n",
                walks.size(), totalMin, high));
        sb.append(String.format(Locale.getDefault(), "Average peak risk: %.0f/100", avg));
        if (riskyHour >= 0) {
            sb.append(String.format(Locale.getDefault(), "\nRiskiest time: around %02d:00", riskyHour));
        }
        insights.setText(sb.toString());
    }

    private void renderRecentWalks() {
        renderInsights(new WalkHistoryStore(this).getAll());
        LinearLayout list = findViewById(R.id.recentWalksList);
        list.removeAllViews();
        List<WalkRecord> walks = new WalkHistoryStore(this).getAll();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM, h:mm a", Locale.getDefault());

        if (walks.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No walks yet - start one above.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark_secondary));
            empty.setPadding(0, 8, 0, 8);
            list.addView(empty);
            return;
        }

        int count = 0;
        for (WalkRecord walk : walks) {
            if (count >= 3) break;
            count++;
            View row = LayoutInflater.from(this).inflate(R.layout.item_walk_history, list, false);
            ((TextView) row.findViewById(R.id.walkDate)).setText(fmt.format(walk.startTimeMillis));
            String route = (walk.fromLabel == null || walk.fromLabel.isEmpty() ? "Start" : walk.fromLabel)
                    + " → " + (walk.toLabel == null || walk.toLabel.isEmpty() ? "Destination" : walk.toLabel);
            ((TextView) row.findViewById(R.id.walkRoute)).setText(route);
            TextView tierView = row.findViewById(R.id.walkTier);
            applyTier(tierView, walk.finalTier);
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, WalkDetailActivity.class);
                intent.putExtra(WalkDetailActivity.EXTRA_WALK_ID, walk.id);
                startActivity(intent);
            });
            list.addView(row);
        }
    }

    private void applyTier(TextView tierView, String tier) {
        if (tier == null) tier = "LOW";
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
    }

    private String greeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "there";
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }
}
