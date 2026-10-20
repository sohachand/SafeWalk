package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.project.data.SessionStore;
import com.example.project.home.HomeDashboardActivity;
import com.example.project.onboarding.OnboardingActivity;
import com.example.project.onboarding.SignUpLoginActivity;

/**
 * SafeWalk launch screen. Shows the logo/branding briefly, then routes to onboarding, login,
 * or straight to the home dashboard depending on what's already been done (tracked locally via
 * SessionStore - no backend involved).
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Gentle entrance: logo pops in, wordmark + tagline fade up, progress bar fills.
        View logo = findViewById(R.id.logo);
        logo.setScaleX(0.6f); logo.setScaleY(0.6f); logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(700)
                .setInterpolator(new android.view.animation.OvershootInterpolator()).start();
        for (int id : new int[]{R.id.wordmark, R.id.tagline, R.id.footer}) {
            View v = findViewById(id);
            v.setAlpha(0f);
            v.setTranslationY(30f);
            v.animate().alpha(1f).translationY(0f).setStartDelay(400).setDuration(700).start();
        }
        android.animation.ObjectAnimator.ofInt((android.widget.ProgressBar) findViewById(R.id.splashProgress),
                "progress", 0, 100).setDuration(SPLASH_DELAY_MS - 200).start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionStore session = new SessionStore(this);
            Class<?> destination;
            if (!session.isOnboardingDone()) {
                destination = OnboardingActivity.class;
            } else if (!session.isLoggedIn()) {
                destination = SignUpLoginActivity.class;
            } else {
                destination = HomeDashboardActivity.class;
            }
            startActivity(new Intent(SplashActivity.this, destination));
            finish();
        }, SPLASH_DELAY_MS);
    }
}
