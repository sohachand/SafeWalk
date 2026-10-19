package com.example.project.trustedcircle;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.TrustedCircleStore;
import com.example.project.data.TrustedContact;

public class AddEditContactActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_ID = "extra_contact_id";

    private TrustedCircleStore store;
    private TrustedContact editingContact;
    private EditText nameInput;
    private EditText phoneInput;
    private EditText relationshipInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        store = new TrustedCircleStore(this);
        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        relationshipInput = findViewById(R.id.relationshipInput);
        TextView formTitle = findViewById(R.id.formTitle);
        Button saveButton = findViewById(R.id.saveContactButton);
        Button deleteButton = findViewById(R.id.deleteContactButton);

        String contactId = getIntent().getStringExtra(EXTRA_CONTACT_ID);
        if (contactId != null) {
            for (TrustedContact c : store.getAll()) {
                if (c.id.equals(contactId)) {
                    editingContact = c;
                    break;
                }
            }
        }

        if (editingContact != null) {
            formTitle.setText("Edit Contact");
            nameInput.setText(editingContact.name);
            phoneInput.setText(editingContact.phone);
            relationshipInput.setText(editingContact.relationship);
            deleteButton.setVisibility(android.view.View.VISIBLE);
        }

        saveButton.setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> {
            if (editingContact != null) {
                store.remove(editingContact.id);
                finish();
            }
        });
    }

    private void save() {
        String name = nameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String relationship = relationshipInput.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name and phone number are required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (editingContact != null) {
            editingContact.name = name;
            editingContact.phone = phone;
            editingContact.relationship = relationship;
            store.update(editingContact);
        } else {
            store.add(new TrustedContact(name, phone, relationship));
        }
        finish();
    }
}
