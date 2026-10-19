package com.example.project.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** CRUD for trusted-circle contacts, persisted locally as a JSON array in SharedPreferences. */
public class TrustedCircleStore {
    private static final String PREFS = "safewalk_trusted_circle";
    private static final String KEY_CONTACTS = "contacts_json";

    private final SharedPreferences prefs;

    public TrustedCircleStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<TrustedContact> getAll() {
        List<TrustedContact> result = new ArrayList<>();
        String json = prefs.getString(KEY_CONTACTS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                result.add(TrustedContact.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    public void add(TrustedContact contact) {
        List<TrustedContact> all = getAll();
        all.add(contact);
        saveAll(all);
    }

    public void update(TrustedContact contact) {
        List<TrustedContact> all = getAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(contact.id)) {
                all.set(i, contact);
                break;
            }
        }
        saveAll(all);
    }

    public void remove(String id) {
        List<TrustedContact> all = getAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id.equals(id)) {
                all.remove(i);
                break;
            }
        }
        saveAll(all);
    }

    public boolean isEmpty() {
        return getAll().isEmpty();
    }

    private void saveAll(List<TrustedContact> all) {
        JSONArray arr = new JSONArray();
        try {
            for (TrustedContact c : all) arr.put(c.toJson());
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(KEY_CONTACTS, arr.toString()).apply();
    }
}
