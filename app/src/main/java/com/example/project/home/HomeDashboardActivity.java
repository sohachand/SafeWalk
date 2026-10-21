package com.example.project.home;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.R;
import com.example.project.data.SessionStore;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;
import com.example.project.history.WalkDetailActivity;
import com.example.project.history.WalkHistoryActivity;
import com.example.project.profile.ProfileActivity;
import com.example.project.safety.EmergencyHelpActivity;
import com.example.project.safety.FakeCallActivity;
import com.example.project.safety.SirenStrobe;
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
        findViewById(R.id.emergencyLink).setOnClickListener(v ->
                startActivity(new Intent(this, EmergencyHelpActivity.class)));
        findViewById(R.id.fakeCallLink).setOnClickListener(v ->
                startActivity(new Intent(this, FakeCallActivity.class)));
        findViewById(R.id.historyLink).setOnClickListener(v ->
                startActivity(new Intent(this, WalkHistoryActivity.class)));
        findViewById(R.id.nearbyHelpLink).setOnClickListener(v -> showNearbyHelp());
        findViewById(R.id.riskLabLink).setOnClickListener(v ->
                startActivity(new Intent(this, com.example.project.risk.RiskLabActivity.class)));
        TextView sirenLabel = findViewById(R.id.sirenLabel);
        findViewById(R.id.sirenLink).setOnClickListener(v -> {
            if (SirenStrobe.isRunning()) {
                SirenStrobe.stop();
                sirenLabel.setText("Siren");
            } else {
                SirenStrobe.start(this);
                sirenLabel.setText("Stop siren");
            }
        });
        TextView avatar = findViewById(R.id.profileButton);
        String fn = firstName(session.getName());
        avatar.setText(fn.substring(0, 1).toUpperCase(Locale.getDefault()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderRecentWalks();
        com.example.project.cloud.CloudSync.pullWalks(this, added -> {
            if (added > 0) renderRecentWalks();
        });
    }

    /** Opens Google Maps (or the browser) searching for the chosen kind of place near the user. */
    private void showNearbyHelp() {
        final String[] labels = {"Police station", "Hospital", "Pharmacy", "Petrol pump (open late)"};
        final String[] queries = {"police station", "hospital", "pharmacy", "petrol pump"};
        new AlertDialog.Builder(this)
                .setTitle("Find nearby help")
                .setItems(labels, (d, which) -> {
                    Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                            + Uri.encode(queries[which] + " near me"));
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SirenStrobe.stop();
    }

    /** Aggregate stats from the stored walk history, shown next to the safety ring. */
    private void renderInsights(List<WalkRecord> walks) {
        TextView insights = findViewById(R.id.insightsText);
        com.example.project.home.ScoreRingView ring = findViewById(R.id.scoreRing);
        if (walks.isEmpty()) {
            ring.setScore(-1);
            insights.setText("Your safety score appears after your first walk.");
            return;
        }
        double sumPeak = 0;
        int high = 0;
        long totalMin = 0;
        int[] highByHour = new int[24];
        for (WalkRecord w : walks) {
            sumPeak += w.peakRisk;
            // Only finished walks count, and each is capped at 24h, so a walk that was never
            // ended cannot add months of "minutes" to the total.
            if (w.endTimeMillis > 0) totalMin += Math.min(w.durationMinutes(), 24 * 60);
            if ("HIGH".equals(w.finalTier)) {
                high++;
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.setTimeInMillis(w.startTimeMillis);
                highByHour[c.get(java.util.Calendar.HOUR_OF_DAY)]++;
            }
        }
        double avg = sumPeak / walks.size();
        int score = (int) Math.max(0, Math.min(100, Math.round(100 - avg)));
        ring.setScore(score);
        int riskyHour = -1;
        for (int h = 0; h < 24; h++) {
            if (highByHour[h] > 0 && (riskyHour < 0 || highByHour[h] > highByHour[riskyHour])) riskyHour = h;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "%d walks \u00b7 %d min\n", walks.size(), totalMin));
        sb.append(String.format(Locale.getDefault(), "%d high-risk \u00b7 avg peak %.0f\n", high, avg));
        sb.append(riskyHour >= 0
                ? String.format(Locale.getDefault(), "Riskiest time: around %02d:00", riskyHour)
                : "No high-risk walks - great job!");
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
            empty.setText("No walks yet - tap Start a Safe Walk.");
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
        tierView.setBackgroundResource(R.drawable.bg_pill);
        androidx.core.view.ViewCompat.setBackgroundTintList(tierView,
                ColorStateList.valueOf(ContextCompat.getColor(this, colorRes)));
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
