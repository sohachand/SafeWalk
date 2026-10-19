package com.example.project.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.TrustedCircleStore;
import com.example.project.data.TrustedContact;
import com.example.project.home.HomeDashboardActivity;

public class TrustedCircleSetupActivity extends AppCompatActivity {

    private TrustedCircleStore store;
    private LinearLayout contactsList;
    private EditText nameInput;
    private EditText phoneInput;
    private EditText relationshipInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_circle_setup);

        store = new TrustedCircleStore(this);
        contactsList = findViewById(R.id.contactsList);
        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        relationshipInput = findViewById(R.id.relationshipInput);
        Button addContactButton = findViewById(R.id.addContactButton);
        Button doneButton = findViewById(R.id.doneButton);

        addContactButton.setOnClickListener(v -> addContact());
        doneButton.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeDashboardActivity.class));
            finish();
        });

        renderContacts();
    }

    private void addContact() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String relationship = relationshipInput.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Enter at least a name and phone number", Toast.LENGTH_SHORT).show();
            return;
        }
        store.add(new TrustedContact(name, phone, relationship));
        nameInput.setText("");
        phoneInput.setText("");
        relationshipInput.setText("");
        renderContacts();
    }

    private void renderContacts() {
        contactsList.removeAllViews();
        for (TrustedContact contact : store.getAll()) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_trusted_contact, contactsList, false);
            ((TextView) row.findViewById(R.id.contactName)).setText(contact.name);
            ((TextView) row.findViewById(R.id.contactPhone)).setText(contact.phone);
            row.findViewById(R.id.contactRemove).setOnClickListener(v -> {
                store.remove(contact.id);
                renderContacts();
            });
            contactsList.addView(row);
        }
    }
}
