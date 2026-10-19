package com.example.project.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/** A single trusted-circle contact. */
public class TrustedContact {
    public String id;
    public String name;
    public String phone;
    public String relationship;

    public TrustedContact() {
        this.id = UUID.randomUUID().toString();
    }

    public TrustedContact(String name, String phone, String relationship) {
        this();
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("phone", phone);
        o.put("relationship", relationship == null ? "" : relationship);
        return o;
    }

    public static TrustedContact fromJson(JSONObject o) throws JSONException {
        TrustedContact c = new TrustedContact();
        c.id = o.optString("id", UUID.randomUUID().toString());
        c.name = o.optString("name", "");
        c.phone = o.optString("phone", "");
        c.relationship = o.optString("relationship", "");
        return c;
    }
}
