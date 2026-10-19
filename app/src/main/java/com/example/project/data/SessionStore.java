package com.example.project.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.project.risk.FuzzyRiskEngine;

/**
 * One place for every small piece of app state that isn't a list (login, profile, settings).
 * Backed by SharedPreferences - no backend involved.
 */
public class SessionStore {
    private static final String PREFS = "safewalk_session";

    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_PHONE = "profile_phone";
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_EMAIL = "profile_email";

    private static final String KEY_DARK_MODE = "setting_dark_mode";
    private static final String KEY_NOTIFICATIONS = "setting_notifications";
    private static final String KEY_FAKE_CALL = "setting_fake_call";
    private static final String KEY_AUDIO_RECORDING = "setting_audio_recording";
    private static final String KEY_AUTO_START_WALK = "setting_auto_start_walk";
    private static final String KEY_SENSITIVITY = "setting_sensitivity"; // RELAXED|STANDARD|CAUTIOUS

    private final SharedPreferences prefs;

    public SessionStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(String phone) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_PHONE, phone)
                .apply();
    }

    public void logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply();
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    public String getName() {
        String n = prefs.getString(KEY_NAME, "");
        return n.isEmpty() ? "SafeWalk User" : n;
    }

    public void setName(String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void setEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    public void setDarkMode(boolean on) {
        prefs.edit().putBoolean(KEY_DARK_MODE, on).apply();
    }

    public boolean isNotificationsOn() {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsOn(boolean on) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, on).apply();
    }

    public boolean isFakeCallEnabled() {
        return prefs.getBoolean(KEY_FAKE_CALL, true);
    }

    public void setFakeCallEnabled(boolean on) {
        prefs.edit().putBoolean(KEY_FAKE_CALL, on).apply();
    }

    public boolean isAudioRecordingOn() {
        return prefs.getBoolean(KEY_AUDIO_RECORDING, true);
    }

    public void setAudioRecordingOn(boolean on) {
        prefs.edit().putBoolean(KEY_AUDIO_RECORDING, on).apply();
    }

    public boolean isAutoStartWalkOn() {
        return prefs.getBoolean(KEY_AUTO_START_WALK, false);
    }

    public void setAutoStartWalkOn(boolean on) {
        prefs.edit().putBoolean(KEY_AUTO_START_WALK, on).apply();
    }

    public FuzzyRiskEngine.Sensitivity getSensitivity() {
        String s = prefs.getString(KEY_SENSITIVITY, FuzzyRiskEngine.Sensitivity.STANDARD.name());
        try {
            return FuzzyRiskEngine.Sensitivity.valueOf(s);
        } catch (IllegalArgumentException e) {
            return FuzzyRiskEngine.Sensitivity.STANDARD;
        }
    }

    public void setSensitivity(FuzzyRiskEngine.Sensitivity sensitivity) {
        prefs.edit().putString(KEY_SENSITIVITY, sensitivity.name()).apply();
    }
}
