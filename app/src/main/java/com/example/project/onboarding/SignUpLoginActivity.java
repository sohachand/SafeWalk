package com.example.project.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.cloud.CloudSync;
import com.example.project.cloud.FirebaseGate;
import com.example.project.data.SessionStore;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Email/password sign-in backed by Firebase Authentication. If Firebase isn't configured
 * (no google-services.json) the account buttons explain that and "Continue without an account"
 * keeps the whole app usable in local-only mode.
 */
public class SignUpLoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private TextView authStatus;
    private Button signInButton;
    private Button createAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        authStatus = findViewById(R.id.authStatus);
        signInButton = findViewById(R.id.signInButton);
        createAccountButton = findViewById(R.id.createAccountButton);
        Button offlineButton = findViewById(R.id.offlineButton);

        boolean cloud = FirebaseGate.isReady(this);
        authStatus.setText(cloud ? "" : "Cloud features are not set up in this build.");

        signInButton.setOnClickListener(v -> submit(false));
        createAccountButton.setOnClickListener(v -> submit(true));

        offlineButton.setOnClickListener(v -> {
            new SessionStore(this).setLoggedIn("guest");
            goNext();
        });
    }

    private void submit(boolean create) {
        if (!FirebaseGate.isReady(this)) {
            Toast.makeText(this, "Cloud sign-in isn't set up - use \"Continue without an account\"",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("At least 6 characters");
            return;
        }

        setBusy(true);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> task =
                create ? auth.createUserWithEmailAndPassword(email, password)
                        : auth.signInWithEmailAndPassword(email, password);
        task.addOnSuccessListener(result -> {
            SessionStore session = new SessionStore(this);
            session.setLoggedIn(email);
            session.setEmail(email);
            CloudSync.pullWalks(this, null);
            goNext();
        }).addOnFailureListener(e -> {
            setBusy(false);
            String msg = e.getMessage() == null ? "Authentication failed" : e.getMessage();
            authStatus.setText(msg);
        });
    }

    private void setBusy(boolean busy) {
        signInButton.setEnabled(!busy);
        createAccountButton.setEnabled(!busy);
        if (busy) authStatus.setText("Please wait...");
    }

    private void goNext() {
        startActivity(new Intent(this, PermissionsSetupActivity.class));
        finish();
    }
}
