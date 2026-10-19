package com.example.project.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Past (and in-progress) walk records, persisted locally as a JSON array. */
public class WalkHistoryStore {
    private static final String PREFS = "safewalk_walk_history";
    private static final String KEY_WALKS = "walks_json";

    private final SharedPreferences prefs;

    public WalkHistoryStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<WalkRecord> getAll() {
        List<WalkRecord> result = new ArrayList<>();
        String json = prefs.getString(KEY_WALKS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(WalkRecord.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        Collections.sort(result, (a, b) -> Long.compare(b.startTimeMillis, a.startTimeMillis));
        return result;
    }

    public WalkRecord getById(String id) {
        for (WalkRecord r : getAll()) {
            if (r.id.equals(id)) return r;
        }
        return null;
    }

    public void save(WalkRecord record) {
        List<WalkRecord> all = getAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(record.id)) {
                all.set(i, record);
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(record);
        saveAll(all);
    }

    private void saveAll(List<WalkRecord> all) {
        JSONArray arr = new JSONArray();
        try {
            for (WalkRecord r : all) arr.put(r.toJson());
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(KEY_WALKS, arr.toString()).apply();
    }
}
