package com.example.project.history;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.project.data.RiskPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a walk's GPS path (no map tiles or API key needed). Each segment is coloured by the
 * risk score at that moment - green / amber / red using the same 30 and 65 thresholds as the
 * fuzzy engine's tiers - so you can see exactly where on the route the risk went up.
 */
public class TrailView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<RiskPoint> points = new ArrayList<>();

    public TrailView(Context context) {
        this(context, null);
    }

    public TrailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(10f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(36f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setPoints(List<RiskPoint> timeline) {
        points = new ArrayList<>();
        for (RiskPoint p : timeline) {
            if (p.hasLocation()) points.add(p);
        }
        invalidate();
    }

    private static int colorFor(double score) {
        if (score < 30) return Color.parseColor("#43A047");
        if (score < 65) return Color.parseColor("#F9A825");
        return Color.parseColor("#E53935");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (points.size() < 2) {
            canvas.drawText("No GPS trail recorded for this walk", w / 2f, h / 2f, textPaint);
            return;
        }

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;
        for (RiskPoint p : points) {
            minLat = Math.min(minLat, p.lat);
            maxLat = Math.max(maxLat, p.lat);
            minLng = Math.min(minLng, p.lng);
            maxLng = Math.max(maxLng, p.lng);
        }
        // Longitude degrees shrink with latitude; keep the true aspect ratio.
        double midLat = (minLat + maxLat) / 2.0;
        double lngScale = Math.cos(Math.toRadians(midLat));
        double spanX = Math.max((maxLng - minLng) * lngScale, 1e-6);
        double spanY = Math.max(maxLat - minLat, 1e-6);
        float pad = 48f;
        double scale = Math.min((w - 2 * pad) / spanX, (h - 2 * pad) / spanY);
        double offX = (w - spanX * scale) / 2.0;
        double offY = (h - spanY * scale) / 2.0;

        float prevX = 0, prevY = 0;
        for (int i = 0; i < points.size(); i++) {
            RiskPoint p = points.get(i);
            float x = (float) (offX + (p.lng - minLng) * lngScale * scale);
            float y = (float) (h - (offY + (p.lat - minLat) * scale));
            if (i > 0) {
                linePaint.setColor(colorFor(p.score));
                canvas.drawLine(prevX, prevY, x, y, linePaint);
            }
            if (i == 0) {
                dotPaint.setColor(Color.WHITE);
                canvas.drawCircle(x, y, 16f, dotPaint);
            }
            if (i == points.size() - 1) {
                dotPaint.setColor(Color.parseColor("#E91E8C"));
                canvas.drawCircle(x, y, 16f, dotPaint);
            }
            prevX = x;
            prevY = y;
        }
        textPaint.setTextSize(28f);
        canvas.drawText("white = start  -  pink = end", w / 2f, h - 14f, textPaint);
    }
}
