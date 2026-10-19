package com.example.project.safety;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.project.R;

/**
 * Manual SOS. Two ways in:
 *  - Press-and-hold the circle for 5 seconds (the original design) - a safeguard against
 *    accidental taps, since releasing early cancels it.
 *  - Launched with EXTRA_AUTO_TRIGGERED=true (from a detected shake gesture, see
 *    ActiveWalkActivity's ShakeDetector) - the countdown starts immediately with no hold
 *    required, since shaking the phone hard is already a deliberate, unambiguous signal.
 *    A tap on the circle still cancels it, in case the shake was accidental (dropped phone,
 *    bumpy bus ride, etc).
 */
public class ManualSosActivity extends AppCompatActivity {

    public static final String EXTRA_AUTO_TRIGGERED = "extra_auto_triggered";

    private TextView countdownView;
    private TextView hintView;
    private CountDownTimer timer;
    private boolean autoTriggered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_sos);

        autoTriggered = getIntent().getBooleanExtra(EXTRA_AUTO_TRIGGERED, false);

        countdownView = findViewById(R.id.sosCountdown);
        hintView = findViewById(R.id.sosHint);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(ContextCompat.getColor(this, R.color.risk_high));
        countdownView.setBackground(circle);

        if (autoTriggered) {
            hintView.setText("Shake detected. Sending SOS automatically — tap the circle to cancel.");
            startCountdown();
        }

        countdownView.setOnTouchListener((v, event) -> {
            if (autoTriggered) {
                // Auto-triggered mode: any tap cancels, no hold semantics.
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    cancelCountdown();
                    hintView.setText("Cancelled. Tap below to send manually instead.");
                }
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startCountdown();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                cancelCountdown();
                return true;
            }
            return false;
        });
    }

    private void startCountdown() {
        cancelCountdown();
        timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownView.setText(String.valueOf((millisUntilFinished / 1000) + 1));
            }

            @Override
            public void onFinish() {
                startActivity(new Intent(ManualSosActivity.this, AlertSentActivity.class));
                finish();
            }
        }.start();
    }

    private void cancelCountdown() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        countdownView.setText("5");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountdown();
    }
}
