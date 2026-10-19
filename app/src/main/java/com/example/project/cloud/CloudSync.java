package com.example.project.cloud;

import android.content.Context;
import android.util.Log;

import com.example.project.data.RiskPoint;
import com.example.project.data.WalkHistoryStore;
import com.example.project.data.WalkRecord;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Walk history in Firestore at users/{uid}/walks/{walkId}. Local SharedPreferences stays the
 * source the UI reads from (so the app is fully usable offline); this class mirrors finished
 * walks up, and pulls any walks the local device doesn't have yet back down, so a user's
 * history follows their account to a new phone.
 */
public final class CloudSync {
    private static final String TAG = "SafeWalkCloud";

    private CloudSync() {
    }

    public interface PullListener {
        void onPulled(int added);
    }

    public static void uploadWalk(Context context, WalkRecord r) {
        String uid = FirebaseGate.uid(context);
        if (uid == null) return;

        Map<String, Object> m = new HashMap<>();
        m.put("id", r.id);
        m.put("fromLabel", r.fromLabel == null ? "" : r.fromLabel);
        m.put("toLabel", r.toLabel == null ? "" : r.toLabel);
        m.put("startTimeMillis", r.startTimeMillis);
        m.put("endTimeMillis", r.endTimeMillis);
        m.put("peakRisk", r.peakRisk);
        m.put("finalTier", r.finalTier == null ? "LOW" : r.finalTier);
        m.put("triggers", new ArrayList<>(r.triggers));

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (RiskPoint p : r.timeline) {
            Map<String, Object> pt = new HashMap<>();
            pt.put("t", p.timestampMillis);
            pt.put("s", p.score);
            if (p.hasLocation()) {
                pt.put("la", p.lat);
                pt.put("lo", p.lng);
            }
            timeline.add(pt);
        }
        m.put("timeline", timeline);

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("walks").document(r.id)
                .set(m)
                .addOnFailureListener(e -> Log.w(TAG, "Walk upload failed", e));
    }

    @SuppressWarnings("unchecked")
    public static void pullWalks(Context context, PullListener listener) {
        String uid = FirebaseGate.uid(context);
        if (uid == null) return;
        final Context app = context.getApplicationContext();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("walks").get()
                .addOnSuccessListener(snap -> {
                    WalkHistoryStore store = new WalkHistoryStore(app);
                    int added = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id = d.getString("id");
                        if (id == null) id = d.getId();
                        if (store.getById(id) != null) continue;

                        WalkRecord r = new WalkRecord();
                        r.id = id;
                        r.fromLabel = d.getString("fromLabel");
                        r.toLabel = d.getString("toLabel");
                        Long start = d.getLong("startTimeMillis");
                        Long end = d.getLong("endTimeMillis");
                        Double peak = d.getDouble("peakRisk");
                        r.startTimeMillis = start == null ? 0L : start;
                        r.endTimeMillis = end == null ? 0L : end;
                        r.peakRisk = peak == null ? 0.0 : peak;
                        String tier = d.getString("finalTier");
                        r.finalTier = tier == null ? "LOW" : tier;

                        List<String> triggers = (List<String>) d.get("triggers");
                        if (triggers != null) r.triggers.addAll(triggers);

                        List<Map<String, Object>> tl = (List<Map<String, Object>>) d.get("timeline");
                        if (tl != null) {
                            for (Map<String, Object> pt : tl) {
                                Object t = pt.get("t");
                                Object s = pt.get("s");
                                if (t instanceof Number && s instanceof Number) {
                                    RiskPoint rp = new RiskPoint(((Number) t).longValue(),
                                            ((Number) s).doubleValue());
                                    Object la = pt.get("la");
                                    Object lo = pt.get("lo");
                                    if (la instanceof Number && lo instanceof Number) {
                                        rp.lat = ((Number) la).doubleValue();
                                        rp.lng = ((Number) lo).doubleValue();
                                    }
                                    r.timeline.add(rp);
                                }
                            }
                        }
                        store.save(r);
                        added++;
                    }
                    if (listener != null) listener.onPulled(added);
                })
                .addOnFailureListener(e -> Log.w(TAG, "Walk pull failed", e));
    }
}
