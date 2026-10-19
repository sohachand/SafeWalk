package com.example.project.history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.project.data.RiskPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal hand-drawn line chart of a walk's risk score over time - no charting library needed.
 */
public class RiskTimelineView extends View {

    private final List<RiskPoint> points = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RiskTimelineView(Context context) {
        super(context);
        init();
    }

    public RiskTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setColor(Color.parseColor("#8E4DFF"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);

        axisPaint.setColor(Color.parseColor("#3A2E52"));
        axisPaint.setStrokeWidth(2f);

        textPaint.setColor(Color.parseColor("#B8AFC9"));
        textPaint.setTextSize(24f);
    }

    public void setPoints(List<RiskPoint> newPoints) {
        points.clear();
        if (newPoints != null) points.addAll(newPoints);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float padding = 40f;

        // Axes
        canvas.drawLine(padding, h - padding, w - padding, h - padding, axisPaint); // x-axis
        canvas.drawLine(padding, padding, padding, h - padding, axisPaint); // y-axis
        canvas.drawText("100", 4, padding + 10, textPaint);
        canvas.drawText("0", 4, h - padding, textPaint);

        if (points.size() < 2) {
            canvas.drawText("Not enough data yet", padding + 20, h / 2f, textPaint);
            return;
        }

        long minTime = points.get(0).timestampMillis;
        long maxTime = points.get(points.size() - 1).timestampMillis;
        long span = Math.max(1, maxTime - minTime);

        float usableWidth = w - 2 * padding;
        float usableHeight = h - 2 * padding;

        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            RiskPoint p = points.get(i);
            float x = padding + usableWidth * ((p.timestampMillis - minTime) / (float) span);
            float y = (h - padding) - usableHeight * ((float) (p.score / 100.0));
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);
    }
}
