package com.example.project.walk;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StartWalkSetupActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private EditText destinationInput;
    private LinearLayout suggestionList;
    private boolean suppressSearch = false;
    private int searchId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_walk_setup);

        destinationInput = findViewById(R.id.destinationInput);
        suggestionList = findViewById(R.id.suggestionList);
        TextView chipHome = findViewById(R.id.chipHome);
        TextView chipLibrary = findViewById(R.id.chipLibrary);
        TextView chipHostel = findViewById(R.id.chipHostel);
        Button startWalkButton = findViewById(R.id.startWalkButton);

        chipHome.setOnClickListener(v -> setDestination("Home"));
        chipLibrary.setOnClickListener(v -> setDestination("Library"));
        chipHostel.setOnClickListener(v -> setDestination("Hostel"));

        // Place search: after the user stops typing for 500 ms, look the name up and
        // show every matching place (different cities, areas) as a tappable list.
        destinationInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (suppressSearch) return;
                if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
                final String query = s.toString().trim();
                if (query.length() < 3) {
                    suggestionList.setVisibility(View.GONE);
                    return;
                }
                pendingSearch = () -> searchPlaces(query);
                handler.postDelayed(pendingSearch, 500);
            }
        });

        startWalkButton.setOnClickListener(v -> {
            String destination = destinationInput.getText().toString().trim();
            if (destination.isEmpty()) {
                Toast.makeText(this, "Enter or pick a destination", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ActiveWalkActivity.class);
            intent.putExtra(ActiveWalkActivity.EXTRA_FROM, "Current Location");
            intent.putExtra(ActiveWalkActivity.EXTRA_TO, destination);
            startActivity(intent);
            finish();
        });
    }

    private void setDestination(String text) {
        suppressSearch = true;
        destinationInput.setText(text);
        destinationInput.setSelection(text.length());
        suppressSearch = false;
        suggestionList.setVisibility(View.GONE);
    }

    @SuppressWarnings("deprecation")
    private void searchPlaces(String query) {
        final int myId = ++searchId;
        if (!Geocoder.isPresent()) {
            showMessage("Place search is not available on this device. Type the destination name.");
            return;
        }
        showMessage("Searching places...");
        new Thread(() -> {
            List<String> labels = new ArrayList<>();
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 6);
                if (results != null) {
                    for (Address a : results) {
                        String label = describe(a);
                        if (!label.isEmpty() && !labels.contains(label)) labels.add(label);
                    }
                }
            } catch (Exception e) {
                // no network or geocoder backend: fall through with an empty list
            }
            runOnUiThread(() -> {
                if (isFinishing() || myId != searchId) return;
                if (labels.isEmpty()) {
                    showMessage("No places found. You can still start with the name you typed.");
                } else {
                    showSuggestions(labels);
                }
            });
        }).start();
    }

    /** "Name, locality, state, country" without repeating parts. */
    private String describe(Address a) {
        List<String> parts = new ArrayList<>();
        add(parts, a.getFeatureName());
        add(parts, a.getSubLocality());
        add(parts, a.getLocality());
        add(parts, a.getAdminArea());
        add(parts, a.getCountryName());
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.toString();
    }

    private void add(List<String> parts, String value) {
        if (value == null || value.trim().isEmpty()) return;
        for (String p : parts) if (p.equalsIgnoreCase(value.trim())) return;
        parts.add(value.trim());
    }

    private void showSuggestions(List<String> labels) {
        suggestionList.removeAllViews();
        for (String label : labels) {
            TextView row = makeRow(label, true);
            row.setOnClickListener(v -> setDestination(label));
            suggestionList.addView(row);
        }
        suggestionList.setVisibility(View.VISIBLE);
    }

    private void showMessage(String text) {
        suggestionList.removeAllViews();
        suggestionList.addView(makeRow(text, false));
        suggestionList.setVisibility(View.VISIBLE);
    }

    private TextView makeRow(String text, boolean clickable) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColor(clickable ? R.color.text_on_dark : R.color.text_on_dark_secondary));
        tv.setTextSize(14);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        if (clickable) tv.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_mylocation, 0, 0, 0);
        tv.setCompoundDrawablePadding(pad);
        return tv;
    }
}
