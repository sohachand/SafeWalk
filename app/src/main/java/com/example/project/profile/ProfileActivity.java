package com.example.project.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.SessionStore;
import com.example.project.onboarding.SignUpLoginActivity;
import com.example.project.settings.GeneralSettingsActivity;
import com.example.project.trustedcircle.TrustedCircleListActivity;

public class ProfileActivity extends AppCompatActivity {

    private SessionStore session;
    private EditText nameInput;
    private EditText emailInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = new SessionStore(this);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        TextView phoneText = findViewById(R.id.phoneText);

        String phone = session.getPhone();
        phoneText.setText(phone.isEmpty() ? "No phone on file" : phone);
        nameInput.setText(session.getName());
        emailInput.setText(session.getEmail());

        findViewById(R.id.saveProfileButton).setOnClickListener(v -> {
            session.setName(nameInput.getText().toString().trim());
            session.setEmail(emailInput.getText().toString().trim());
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.trustedCircleRow).setOnClickListener(v ->
                startActivity(new Intent(this, TrustedCircleListActivity.class)));
        findViewById(R.id.settingsRow).setOnClickListener(v ->
                startActivity(new Intent(this, GeneralSettingsActivity.class)));
        findViewById(R.id.helpRow).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Help & Support")
                        .setMessage("SafeWalk is a student project demo. For real emergencies, "
                                + "always contact local emergency services directly.")
                        .setPositiveButton("OK", null)
                        .show());
        findViewById(R.id.logoutRow).setOnClickListener(v -> {
            session.logout();
            com.example.project.cloud.FirebaseGate.signOut(this);
            Intent intent = new Intent(this, SignUpLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
