package com.example.project.walk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;

public class StartWalkSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_walk_setup);

        EditText destinationInput = findViewById(R.id.destinationInput);
        TextView chipHome = findViewById(R.id.chipHome);
        TextView chipLibrary = findViewById(R.id.chipLibrary);
        TextView chipHostel = findViewById(R.id.chipHostel);
        Button startWalkButton = findViewById(R.id.startWalkButton);

        chipHome.setOnClickListener(v -> destinationInput.setText("Home"));
        chipLibrary.setOnClickListener(v -> destinationInput.setText("Library"));
        chipHostel.setOnClickListener(v -> destinationInput.setText("Hostel"));

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
}
