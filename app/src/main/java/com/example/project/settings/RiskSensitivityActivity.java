package com.example.project.settings;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project.R;
import com.example.project.data.SessionStore;
import com.example.project.risk.FuzzyRiskEngine;

/**
 * This is the one Settings screen that is NOT cosmetic: the choice made here is read directly
 * by ActiveWalkActivity via SessionStore.getSensitivity() and passed straight into
 * FuzzyRiskEngine.computeRisk(...), changing the fuzzy set boundaries for every subsequent walk.
 */
public class RiskSensitivityActivity extends AppCompatActivity {

    private SessionStore session;
    private RadioButton radioRelaxed, radioStandard, radioCautious;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_sensitivity);

        session = new SessionStore(this);
        RadioGroup group = findViewById(R.id.sensitivityGroup);
        radioRelaxed = findViewById(R.id.radioRelaxed);
        radioStandard = findViewById(R.id.radioStandard);
        radioCautious = findViewById(R.id.radioCautious);

        FuzzyRiskEngine.Sensitivity current = session.getSensitivity();
        if (current == FuzzyRiskEngine.Sensitivity.RELAXED) radioRelaxed.setChecked(true);
        else if (current == FuzzyRiskEngine.Sensitivity.CAUTIOUS) radioCautious.setChecked(true);
        else radioStandard.setChecked(true);
        updateHighlights();

        group.setOnCheckedChangeListener((RadioGroup g, int checkedId) -> {
            if (checkedId == R.id.radioRelaxed) {
                session.setSensitivity(FuzzyRiskEngine.Sensitivity.RELAXED);
            } else if (checkedId == R.id.radioCautious) {
                session.setSensitivity(FuzzyRiskEngine.Sensitivity.CAUTIOUS);
            } else {
                session.setSensitivity(FuzzyRiskEngine.Sensitivity.STANDARD);
            }
            updateHighlights();
        });
    }

    private void updateHighlights() {
        setRowBackground(radioRelaxed, radioRelaxed.isChecked());
        setRowBackground(radioStandard, radioStandard.isChecked());
        setRowBackground(radioCautious, radioCautious.isChecked());
    }

    private void setRowBackground(RadioButton button, boolean selected) {
        button.setBackgroundColor(ContextCompat.getColor(this,
                selected ? R.color.accent_purple : R.color.surface_dark));
    }
}
