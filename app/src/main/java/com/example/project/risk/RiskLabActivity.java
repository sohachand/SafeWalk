package com.example.project.risk;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.SessionStore;
import com.example.project.home.ScoreRingView;

import java.util.Locale;

/** Interactive playground: moves the three fuzzy inputs by hand and shows the engine's output live. */
public class RiskLabActivity extends AppCompatActivity {

    private SeekBar st, dev, night;
    private TextView stLabel, devLabel, nightLabel, tierText, whyText;
    private ScoreRingView ring;
    private FuzzyRiskEngine.Sensitivity sensitivity = FuzzyRiskEngine.Sensitivity.STANDARD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_risk_lab);

        st = findViewById(R.id.labSt);
        dev = findViewById(R.id.labDev);
        night = findViewById(R.id.labNight);
        stLabel = findViewById(R.id.labStLabel);
        devLabel = findViewById(R.id.labDevLabel);
        nightLabel = findViewById(R.id.labNightLabel);
        tierText = findViewById(R.id.labTier);
        whyText = findViewById(R.id.labWhy);
        ring = findViewById(R.id.labRing);

        sensitivity = new SessionStore(this).getSensitivity();

        SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) { recompute(); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        };
        st.setOnSeekBarChangeListener(l);
        dev.setOnSeekBarChangeListener(l);
        night.setOnSeekBarChangeListener(l);

        findViewById(R.id.presetSafe).setOnClickListener(v -> preset(0, 0, 1));
        findViewById(R.id.presetLost).setOnClickListener(v -> preset(4, 90, 5));
        findViewById(R.id.presetDanger).setOnClickListener(v -> preset(10, 200, 10));
        findViewById(R.id.sensRelaxed).setOnClickListener(v -> setSens(FuzzyRiskEngine.Sensitivity.RELAXED));
        findViewById(R.id.sensStandard).setOnClickListener(v -> setSens(FuzzyRiskEngine.Sensitivity.STANDARD));
        findViewById(R.id.sensCautious).setOnClickListener(v -> setSens(FuzzyRiskEngine.Sensitivity.CAUTIOUS));

        recompute();
    }

    private void preset(int stationaryMin, int deviationM, int hoursAfter6) {
        st.setProgress(stationaryMin * 10);       // 0.1 min steps
        dev.setProgress(deviationM);
        night.setProgress(hoursAfter6 * 2);       // 0.5 h steps
        recompute();
    }

    private void setSens(FuzzyRiskEngine.Sensitivity s) {
        sensitivity = s;
        recompute();
    }

    private void recompute() {
        double minutes = st.getProgress() / 10.0;
        double meters = dev.getProgress();
        double hours = night.getProgress() / 2.0;

        stLabel.setText(String.format(Locale.getDefault(), "Stationary time: %.1f min", minutes));
        devLabel.setText(String.format(Locale.getDefault(), "Route deviation: %.0f m", meters));
        nightLabel.setText(String.format(Locale.getDefault(), "Time of night: %.1f h after 6 pm", hours));

        FuzzyCurvesView c1 = findViewById(R.id.curveSt);
        FuzzyCurvesView c2 = findViewById(R.id.curveDev);
        FuzzyCurvesView c3 = findViewById(R.id.curveNight);
        c1.setConfig(FuzzyCurvesView.STATIONARY, sensitivity.scale); c1.setValue(minutes);
        c2.setConfig(FuzzyCurvesView.DEVIATION, sensitivity.scale); c2.setValue(meters);
        c3.setConfig(FuzzyCurvesView.NIGHT, sensitivity.scale); c3.setValue(hours);

        FuzzyRiskEngine.RiskExplanation ex =
                FuzzyRiskEngine.computeRiskExplained(minutes, meters, hours, sensitivity);
        ring.setScore((int) Math.round(100 - ex.risk)); // ring shows SAFETY (100 = safest)

        FuzzyRiskEngine.RiskTier tier = FuzzyRiskEngine.tierFor(ex.risk);
        int color = tier == FuzzyRiskEngine.RiskTier.HIGH ? Color.parseColor("#E53935")
                : tier == FuzzyRiskEngine.RiskTier.MEDIUM ? Color.parseColor("#F9A825")
                : Color.parseColor("#43A047");
        tierText.setText(String.format(Locale.getDefault(), "%s  (%s, %.0f)",
                tier.name(), sensitivity.name(), ex.risk));
        tierText.setTextColor(color);
        whyText.setText(ex.summary);
    }
}
