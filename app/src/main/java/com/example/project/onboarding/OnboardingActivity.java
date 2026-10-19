package com.example.project.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project.R;
import com.example.project.data.SessionStore;

/**
 * Simple 3-page onboarding carousel (Next-button driven, no ViewPager dependency needed).
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String[] TITLES = {
            "A Safer You, Every Step.",
            "Same walks. A safer you.",
            "Safety shouldn't require a panic button."
    };

    private static final String[][] BULLETS = {
            {"Silent monitoring", "Smart risk detection", "Discreet intervention", "Trusted circle support"},
            {"Live GPS tracking", "Fuzzy logic risk engine", "Automatic alerts", "Offline-first queuing"},
            {"Your trusted circle", "Always with you", "Walk freely, we've got you"}
    };

    private int pageIndex = 0;
    private TextView pageTitle;
    private LinearLayout pageBulletList;
    private LinearLayout dotsContainer;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pageTitle = findViewById(R.id.pageTitle);
        pageBulletList = findViewById(R.id.pageBulletList);
        dotsContainer = findViewById(R.id.dotsContainer);
        nextButton = findViewById(R.id.nextButton);
        TextView skipButton = findViewById(R.id.skipButton);

        skipButton.setOnClickListener(v -> goToSignUp());
        nextButton.setOnClickListener(v -> {
            if (pageIndex < TITLES.length - 1) {
                pageIndex++;
                renderPage();
            } else {
                goToSignUp();
            }
        });

        renderPage();
    }

    private void renderPage() {
        pageTitle.setText(TITLES[pageIndex]);

        pageBulletList.removeAllViews();
        for (String bullet : BULLETS[pageIndex]) {
            TextView row = new TextView(this);
            row.setText("•  " + bullet);
            row.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark_secondary));
            row.setTextSize(15f);
            row.setPadding(0, 12, 0, 12);
            pageBulletList.addView(row);
        }

        dotsContainer.removeAllViews();
        for (int i = 0; i < TITLES.length; i++) {
            TextView dot = new TextView(this);
            dot.setText(i == pageIndex ? "●" : "○");
            dot.setTextColor(ContextCompat.getColor(this,
                    i == pageIndex ? R.color.accent_purple : R.color.text_on_dark_secondary));
            dot.setTextSize(16f);
            dot.setPadding(10, 0, 10, 0);
            dotsContainer.addView(dot);
        }

        nextButton.setText(pageIndex == TITLES.length - 1 ? "Get Started" : "Next");
    }

    private void goToSignUp() {
        new SessionStore(this).setOnboardingDone(true);
        startActivity(new Intent(this, SignUpLoginActivity.class));
        finish();
    }
}
