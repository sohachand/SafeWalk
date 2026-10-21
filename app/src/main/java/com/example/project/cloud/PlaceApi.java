package com.example.project.cloud;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Two free OpenStreetMap web APIs, no API key needed:
 *  - Overpass API: finds the nearest police stations, hospitals, pharmacies and petrol pumps.
 *  - Nominatim reverse geocoding: turns coordinates into a short address for the alert SMS.
 * Every call runs on a background thread and fails quietly, so the app works without them.
 */
public final class PlaceApi {
    private static final String USER_AGENT = "SafeWalk-College-Project/1.0 (Android)";
    private static final String[] OVERPASS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
    };
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Latest street address of the walker, refreshed during a walk; null if unknown. */
    public static volatile String lastAddress;
    private static volatile long lastLookupMs = 0L;

    private PlaceApi() {
    }

    public static class Place {
        public String name;
        public double lat;
        public double lng;
        public float distanceM;
    }

    public interface NearbyListener {
        void onResult(List<Place> places);
        void onError();
    }

    /** Best last-known location across providers, or null (no permission / no fix). */
    public static Location lastKnown(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- Overpass: nearby help ----------------

    /** osmTag example: "amenity=police". Searches within 3 km and returns up to 6 nearest. */
    public static void findNearby(final String osmTag, final double lat, final double lng,
                                  final NearbyListener listener) {
        new Thread(() -> {
            String query = String.format(Locale.US,
                    "[out:json][timeout:15];nwr[%s](around:3000,%.6f,%.6f);out center 40;", osmTag, lat, lng);
            List<Place> result = null;
            for (String endpoint : OVERPASS) {
                result = queryOverpass(endpoint, query, lat, lng);
                if (result != null) break;
            }
            final List<Place> finalResult = result;
            MAIN.post(() -> {
                if (finalResult == null) listener.onError();
                else listener.onResult(finalResult);
            });
        }).start();
    }

    private static List<Place> queryOverpass(String endpoint, String query, double lat, double lng) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(18000);
            conn.setDoOutput(true);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            byte[] body = ("data=" + URLEncoder.encode(query, "UTF-8")).getBytes("UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            if (conn.getResponseCode() != 200) return null;
            JSONArray elements = new JSONObject(readAll(conn)).getJSONArray("elements");
            List<Place> places = new ArrayList<>();
            for (int i = 0; i < elements.length(); i++) {
                JSONObject e = elements.getJSONObject(i);
                double plat;
                double plng;
                if (e.has("lat") && e.has("lon")) {
                    plat = e.getDouble("lat");
                    plng = e.getDouble("lon");
                } else if (e.has("center")) {
                    plat = e.getJSONObject("center").getDouble("lat");
                    plng = e.getJSONObject("center").getDouble("lon");
                } else {
                    continue;
                }
                Place p = new Place();
                JSONObject tags = e.optJSONObject("tags");
                p.name = tags == null ? "" : tags.optString("name", "");
                p.lat = plat;
                p.lng = plng;
                float[] d = new float[1];
                Location.distanceBetween(lat, lng, plat, plng, d);
                p.distanceM = d[0];
                places.add(p);
            }
            Collections.sort(places, new Comparator<Place>() {
                @Override
                public int compare(Place a, Place b) {
                    return Float.compare(a.distanceM, b.distanceM);
                }
            });
            return places.size() > 6 ? new ArrayList<>(places.subList(0, 6)) : places;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ---------------- Nominatim: coordinates to address ----------------

    /** Refreshes {@link #lastAddress} at most once a minute. Safe to call every risk tick. */
    public static void refreshAddress(final double lat, final double lng) {
        long now = System.currentTimeMillis();
        if (now - lastLookupMs < 60_000L) return;
        lastLookupMs = now;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String url = String.format(Locale.US,
                        "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&lat=%.6f&lon=%.6f",
                        lat, lng);
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept-Language", "en");
                if (conn.getResponseCode() != 200) return;
                JSONObject root = new JSONObject(readAll(conn));
                String text = shortAddress(root);
                if (!text.isEmpty()) lastAddress = text;
            } catch (Exception ignored) {
                // offline or rate limited: keep the previous address
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static String shortAddress(JSONObject root) {
        JSONObject a = root.optJSONObject("address");
        List<String> parts = new ArrayList<>();
        if (a != null) {
            addPart(parts, a.optString("road", ""));
            String area = a.optString("suburb", "");
            if (area.isEmpty()) area = a.optString("neighbourhood", "");
            addPart(parts, area);
            String city = a.optString("city", "");
            if (city.isEmpty()) city = a.optString("town", "");
            if (city.isEmpty()) city = a.optString("village", "");
            if (city.isEmpty()) city = a.optString("state_district", "");
            addPart(parts, city);
        }
        if (parts.isEmpty()) {
            String[] pieces = root.optString("display_name", "").split(",");
            for (int i = 0; i < pieces.length && i < 3; i++) addPart(parts, pieces[i].trim());
        }
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    private static void addPart(List<String> parts, String value) {
        if (value == null) return;
        String v = value.trim();
        if (v.isEmpty()) return;
        for (String p : parts) if (p.equalsIgnoreCase(v)) return;
        parts.add(v);
    }

    private static String readAll(HttpURLConnection conn) throws Exception {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}
