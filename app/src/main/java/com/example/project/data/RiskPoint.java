package com.example.project.data;

import org.json.JSONException;
import org.json.JSONObject;

/** One sample on a walk's risk timeline. */
public class RiskPoint {
    public long timestampMillis;
    public double score;
    /** GPS position when sampled; NaN for older records without a trail. */
    public double lat = Double.NaN;
    public double lng = Double.NaN;

    public boolean hasLocation() {
        return !Double.isNaN(lat) && !Double.isNaN(lng);
    }

    public RiskPoint() {
    }

    public RiskPoint(long timestampMillis, double score) {
        this.timestampMillis = timestampMillis;
        this.score = score;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("t", timestampMillis);
        o.put("s", score);
        if (hasLocation()) {
            o.put("la", lat);
            o.put("lo", lng);
        }
        return o;
    }

    public static RiskPoint fromJson(JSONObject o) throws JSONException {
        RiskPoint p = new RiskPoint(o.optLong("t"), o.optDouble("s"));
        p.lat = o.optDouble("la", Double.NaN);
        p.lng = o.optDouble("lo", Double.NaN);
        return p;
    }
}
