package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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

    private static final long SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

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
