package com.example.project;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Soft radar-style rings that expand from the centre and fade out (splash screen). */
public class PulseView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    private ValueAnimator animator;

    public PulseView(Context c) { super(c); init(); }
    public PulseView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float max = Math.min(getWidth(), getHeight()) / 2f;
        float min = max * 0.38f;
        for (int i = 0; i < 3; i++) {
            float p = (phase + i / 3f) % 1f;
            paint.setAlpha((int) (140 * (1f - p)));
            canvas.drawCircle(cx, cy, min + (max - min) * p, paint);
        }
    }
}
