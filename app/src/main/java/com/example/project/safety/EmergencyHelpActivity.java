package com.example.project.safety;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.R;
import com.example.project.data.TrustedCircleStore;
import com.example.project.data.TrustedContact;

import java.util.List;

/**
 * One-tap emergency screen: India's national numbers plus the user's own trusted contacts.
 * It opens the dialler (ACTION_DIAL) rather than calling directly, so no CALL_PHONE permission
 * is needed and a mis-tap can never place a call by itself.
 */
public class EmergencyHelpActivity extends AppCompatActivity {

    private static final String[][] HELPLINES = {
            {"Police", "100", "Report a crime or threat"},
            {"Ambulance", "102", "Medical emergency (108 in many states)"},
            {"Fire", "101", "Fire and rescue"},
            {"Women helpline", "1091", "Women in distress"},
            {"Women helpline (domestic abuse)", "181", "24x7 support and counselling"},
            {"Child helpline", "1098", "Children in need of help"},
            {"Cyber crime", "1930", "Online fraud and harassment"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_emergency_help);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.emergencyRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        findViewById(R.id.call112).setOnClickListener(v -> dial("112"));

        LinearLayout helplines = findViewById(R.id.helplinesList);
        for (String[] h : HELPLINES) {
            helplines.addView(row(h[0] + " - " + h[1], h[2], h[1]));
        }

        LinearLayout contacts = findViewById(R.id.contactsList);
        List<TrustedContact> list = new TrustedCircleStore(this).getAll();
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No trusted contacts yet - add some from Circle on the home screen.");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark_secondary));
            contacts.addView(empty);
        } else {
            for (TrustedContact c : list) {
                String rel = (c.relationship == null || c.relationship.isEmpty()) ? "" : c.relationship + " - ";
                contacts.addView(row(c.name, rel + c.phone, c.phone));
            }
        }
    }

    private View row(String title, String subtitle, String number) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(16);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(10);
        row.setLayoutParams(rowLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark));
        t.setTextSize(15f);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        texts.addView(t);

        TextView s = new TextView(this);
        s.setText(subtitle);
        s.setTextColor(ContextCompat.getColor(this, R.color.text_on_dark_secondary));
        s.setTextSize(12f);
        texts.addView(s);
        row.addView(texts);

        Button call = new Button(this);
        call.setText("Call");
        call.setTextColor(ContextCompat.getColor(this, R.color.white));
        call.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.accent_purple)));
        call.setOnClickListener(v -> dial(number));
        row.addView(call);
        return row;
    }

    private void dial(String number) {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
