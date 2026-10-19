package com.example.project.cloud;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Live walk sharing. While a walk is active the walker's position and fuzzy risk are written to
 * Firestore document live/{code} on every risk tick; a trusted contact enters the 6-digit code
 * in "Follow a walk" and a snapshot listener streams the updates to their screen in real time.
 */
public final class LiveShare {
    private static final String TAG = "SafeWalkCloud";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Code of the walk currently being shared from this device, or null. */
    public static volatile String currentCode;

    private LiveShare() {
    }

    public interface Listener {
        void onUpdate(Map<String, Object> data);

        void onMissing();

        void onError(String message);
    }

    public static String newCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static void publish(Context context, String code, String name, double lat, double lng,
                               double risk, String tier, String why) {
        String uid = FirebaseGate.uid(context);
        if (uid == null || code == null) return;

        Map<String, Object> m = new HashMap<>();
        m.put("ownerUid", uid);
        m.put("name", name);
        m.put("lat", lat);
        m.put("lng", lng);
        m.put("risk", risk);
        m.put("tier", tier);
        m.put("why", why);
        m.put("active", true);
        m.put("updatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("live").document(code).set(m)
                .addOnFailureListener(e -> Log.w(TAG, "Live publish failed", e));
    }

    public static void stop(Context context, String code) {
        if (FirebaseGate.uid(context) == null || code == null) return;
        FirebaseFirestore.getInstance().collection("live").document(code)
                .update("active", false, "updatedAt", System.currentTimeMillis())
                .addOnFailureListener(e -> Log.w(TAG, "Live stop failed", e));
    }

    public static ListenerRegistration listen(String code, Listener listener) {
        return FirebaseFirestore.getInstance().collection("live").document(code)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        listener.onError(err.getMessage());
                        return;
                    }
                    if (snap == null || !snap.exists()) {
                        listener.onMissing();
                        return;
                    }
                    listener.onUpdate(snap.getData());
                });
    }
}
