package com.example.project.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A completed (or in-progress) walk, with its risk timeline. */
public class WalkRecord {
    public String id;
    public String fromLabel;
    public String toLabel;
    public long startTimeMillis;
    public long endTimeMillis;
    public double peakRisk;
    public String finalTier; // "LOW" | "MEDIUM" | "HIGH"
    public List<String> triggers = new ArrayList<>();
    public List<RiskPoint> timeline = new ArrayList<>();

    public WalkRecord() {
        this.id = UUID.randomUUID().toString();
    }

    public long durationMinutes() {
        long end = endTimeMillis > 0 ? endTimeMillis : System.currentTimeMillis();
        return Math.max(0, (end - startTimeMillis) / 60000L);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("fromLabel", fromLabel == null ? "" : fromLabel);
        o.put("toLabel", toLabel == null ? "" : toLabel);
        o.put("startTimeMillis", startTimeMillis);
        o.put("endTimeMillis", endTimeMillis);
        o.put("peakRisk", peakRisk);
        o.put("finalTier", finalTier == null ? "LOW" : finalTier);

        JSONArray trig = new JSONArray();
        for (String t : triggers) trig.put(t);
        o.put("triggers", trig);

        JSONArray tl = new JSONArray();
        for (RiskPoint p : timeline) tl.put(p.toJson());
        o.put("timeline", tl);
        return o;
    }

    public static WalkRecord fromJson(JSONObject o) throws JSONException {
        WalkRecord r = new WalkRecord();
        r.id = o.optString("id", UUID.randomUUID().toString());
        r.fromLabel = o.optString("fromLabel", "");
        r.toLabel = o.optString("toLabel", "");
        r.startTimeMillis = o.optLong("startTimeMillis");
        r.endTimeMillis = o.optLong("endTimeMillis");
        r.peakRisk = o.optDouble("peakRisk", 0);
        r.finalTier = o.optString("finalTier", "LOW");

        JSONArray trig = o.optJSONArray("triggers");
        if (trig != null) {
            for (int i = 0; i < trig.length(); i++) r.triggers.add(trig.optString(i));
        }
        JSONArray tl = o.optJSONArray("timeline");
        if (tl != null) {
            for (int i = 0; i < tl.length(); i++) {
                r.timeline.add(RiskPoint.fromJson(tl.getJSONObject(i)));
            }
        }
        return r;
    }
}
