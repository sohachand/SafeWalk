package com.example.project.risk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Draws the three membership functions of one fuzzy input and a marker at the current value. */
public class FuzzyCurvesView extends View {
    public static final int STATIONARY = 0, DEVIATION = 1, NIGHT = 2;

    private int kind = STATIONARY;
    private double scale = 1.0;
    private double value = 0.0;
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint curve = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] colors = {Color.parseColor("#66BB6A"), Color.parseColor("#FFCA28"), Color.parseColor("#EF5350")};

    public FuzzyCurvesView(Context c) { super(c); init(); }
    public FuzzyCurvesView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        axis.setColor(Color.parseColor("#6B6280"));
        axis.setStrokeWidth(2f);
        curve.setStyle(Paint.Style.STROKE);
        curve.setStrokeWidth(5f);
        text.setColor(Color.parseColor("#B8AFC9"));
        text.setTextSize(28f);
        marker.setColor(Color.WHITE);
        marker.setStrokeWidth(3f);
    }

    public void setConfig(int kind, double scale) {
        this.kind = kind;
        this.scale = scale;
        invalidate();
    }

    public void setValue(double value) {
        this.value = value;
        invalidate();
    }

    private double maxX() { return kind == STATIONARY ? 12 : kind == DEVIATION ? 250 : 12; }

    private String title() {
        return kind == STATIONARY ? "Stationary time (min)  -  Short / Moderate / Long"
                : kind == DEVIATION ? "Route deviation (m)  -  Small / Medium / Large"
                : "Hours after 6 pm  -  Early / Late / Very late";
    }

    private double degree(int set, double x) {
        double s = scale;
        if (kind == STATIONARY) {
            if (set == 0) return FuzzyRiskEngine.triangular(x, 0, 0, 3 * s);
            if (set == 1) return FuzzyRiskEngine.triangular(x, 1 * s, 4 * s, 7 * s);
            return FuzzyRiskEngine.rightShoulder(x, 5 * s, 8 * s);
        } else if (kind == DEVIATION) {
            if (set == 0) return FuzzyRiskEngine.triangular(x, 0, 0, 60 * s);
            if (set == 1) return FuzzyRiskEngine.triangular(x, 30 * s, 90 * s, 150 * s);
            return FuzzyRiskEngine.rightShoulder(x, 120 * s, 160 * s);
        } else {
            if (set == 0) return FuzzyRiskEngine.triangular(x, 0, 0, 3 * s);
            if (set == 1) return FuzzyRiskEngine.triangular(x, 2 * s, 5 * s, 8 * s);
            return FuzzyRiskEngine.rightShoulder(x, 6 * s, 9 * s);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float left = 20f, right = w - 20f, top = 44f, bottom = h - 16f;
        canvas.drawText(title(), left, 30f, text);
        canvas.drawLine(left, bottom, right, bottom, axis);
        canvas.drawLine(left, top, left, bottom, axis);
        double max = maxX();
        for (int set = 0; set < 3; set++) {
            curve.setColor(colors[set]);
            Path p = new Path();
            for (int i = 0; i <= 120; i++) {
                double x = max * i / 120.0;
                float px = left + (float) (x / max) * (right - left);
                float py = bottom - (float) degree(set, x) * (bottom - top);
                if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
            }
            canvas.drawPath(p, curve);
        }
        double v = Math.max(0, Math.min(max, value));
        float mx = left + (float) (v / max) * (right - left);
        canvas.drawLine(mx, top, mx, bottom, marker);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, (int) (getResources().getDisplayMetrics().density * 130));
    }
}
