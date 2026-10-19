package com.example.project.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Circular gauge showing the user's overall safety score (0-100) from their walk history. */
public class ScoreRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint number = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint caption = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int score = -1; // -1 = no data yet

    public ScoreRingView(Context context) {
        this(context, null);
    }

    public ScoreRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(Color.parseColor("#3A2E52"));
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        number.setColor(Color.WHITE);
        number.setTextAlign(Paint.Align.CENTER);
        number.setFakeBoldText(true);
        caption.setColor(Color.parseColor("#B8AFC9"));
        caption.setTextAlign(Paint.Align.CENTER);
    }

    public void setScore(int score) {
        this.score = score;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.11f;
        track.setStrokeWidth(stroke);
        arc.setStrokeWidth(stroke);
        float inset = stroke / 2f + 2f;
        oval.set(inset, inset, size - inset, size - inset);
        canvas.drawArc(oval, 0, 360, false, track);

        if (score >= 0) {
            arc.setColor(score >= 70 ? Color.parseColor("#43A047")
                    : score >= 40 ? Color.parseColor("#F9A825") : Color.parseColor("#E53935"));
            canvas.drawArc(oval, -90, 360f * score / 100f, false, arc);
        }

        number.setTextSize(size * 0.30f);
        caption.setTextSize(size * 0.13f);
        float cx = size / 2f;
        float cy = size / 2f;
        canvas.drawText(score >= 0 ? String.valueOf(score) : "--", cx, cy + size * 0.06f, number);
        canvas.drawText("safety", cx, cy + size * 0.24f, caption);
    }
}
