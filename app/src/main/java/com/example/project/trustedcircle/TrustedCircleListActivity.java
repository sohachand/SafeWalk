package com.example.project.trustedcircle;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.TrustedCircleStore;
import com.example.project.data.TrustedContact;

public class TrustedCircleListActivity extends AppCompatActivity {

    private LinearLayout contactsList;
    private TrustedCircleStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_circle_list);

        store = new TrustedCircleStore(this);
        contactsList = findViewById(R.id.contactsList);
        findViewById(R.id.addContactButton).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditContactActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            row.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditContactActivity.class);
                intent.putExtra(AddEditContactActivity.EXTRA_CONTACT_ID, contact.id);
                startActivity(intent);
            });
            contactsList.addView(row);
        }
    }
}
