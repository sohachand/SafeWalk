package com.example.project.safety;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.TrustedCircleStore;
import com.example.project.data.TrustedContact;

import java.util.List;

/**
 * Decoy incoming-call screen to discreetly get you out of a situation. Purely local UI -
 * no telephony API is used and no real call is placed.
 */
public class FakeCallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        TextView callerName = findViewById(R.id.callerName);
        List<TrustedContact> contacts = new TrustedCircleStore(this).getAll();
        callerName.setText(contacts.isEmpty() ? "Mom" : contacts.get(0).name);

        findViewById(R.id.declineButton).setOnClickListener(v -> finish());
        findViewById(R.id.acceptButton).setOnClickListener(v -> finish());
    }
}
