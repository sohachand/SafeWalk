package com.example.project.cloud;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Single choke point for "is Firebase actually available?". The app builds and runs without a
 * google-services.json (see app/build.gradle.kts), in which case FirebaseApp never initialises.
 * Every cloud feature checks here first and quietly falls back to local-only behaviour instead
 * of crashing, so the demo still works with no Wi-Fi or no Firebase project.
 */
public final class FirebaseGate {
    private FirebaseGate() {
    }

    public static boolean isReady(Context context) {
        try {
            Context app = context.getApplicationContext();
            if (!FirebaseApp.getApps(app).isEmpty()) return true;
            return FirebaseApp.initializeApp(app) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** Firebase uid of the signed-in user, or null (offline mode / not signed in / no Firebase). */
    public static String uid(Context context) {
        if (!isReady(context)) return null;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public static void signOut(Context context) {
        if (isReady(context)) FirebaseAuth.getInstance().signOut();
    }
}
