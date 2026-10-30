package com.moshang.schedule;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

/** 自绘滚轮选择器（替代原生 NumberPicker / TimePicker） */
public class WheelView extends View {

    public interface OnChange { void onChange(int index); }

    private String[] items = new String[0];
    private float pos = 0f;
    private float itemH, density, scaled;
    private final int visible = 5;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lastY;
    private ValueAnimator anim;
    private OnChange listener;
    private int colText, colSel, colLine;

    public WheelView(Context c) { this(c, null); }

    public WheelView(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        itemH = dp(42);
        colText = getResources().getColor(R.color.text_tertiary);
        colSel = getResources().getColor(R.color.text_primary);
        colLine = getResources().getColor(R.color.sheet_line);
        p.setTextAlign(Paint.Align.CENTER);
        pLine.setStrokeWidth(dp(1));
        pLine.setColor(colLine);
    }

    private float dp(float v) { return v * density; }

    private float sp(float v) { return v * scaled; }

    public void setItems(String[] s) {
        items = s;
        if (pos > s.length - 1) pos = Math.max(0, s.length - 1);
        invalidate();
    }

    public void setSelected(int i, boolean animate) {
        if (items.length == 0) { pos = 0; return; }
        i = Math.max(0, Math.min(items.length - 1, i));
        if (animate) animateTo(i);
        else { if (anim != null) anim.cancel(); pos = i; invalidate(); }
    }

    public int getSelected() { return Math.round(pos); }

    public void setOnChange(OnChange l) { listener = l; }

    private void animateTo(float target) {
        if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
        anim = ValueAnimator.ofFloat(pos, target);
        anim.setDuration(220);
        anim.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.activity_interp));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                pos = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension(MeasureSpec.getSize(wSpec), (int) (itemH * visible));
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float cy = h / 2f;
        for (int i = 0; i < items.length; i++) {
            float y = cy + (i - pos) * itemH;
            if (y < -itemH || y > h + itemH) continue;
            float d = Math.abs(i - pos);
            if (d > 2.9f) continue;
            float a = 1f - d / 2.9f;
            p.setTextSize(sp(17) * (1f - 0.16f * Math.min(1f, d)));
            p.setTypeface(d < 0.5f ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            p.setColor(lerp(colText, colSel, a));
            p.setAlpha((int) (255 * Math.max(0.18f, a)));
            float yy = y - (p.ascent() + p.descent()) / 2f;
            cv.drawText(items[i], w / 2f, yy, p);
            p.setAlpha(255);
        }
        pLine.setColor(colLine);
        cv.drawLine(dp(16), cy - itemH / 2f, w - dp(16), cy - itemH / 2f, pLine);
        cv.drawLine(dp(16), cy + itemH / 2f, w - dp(16), cy + itemH / 2f, pLine);
    }

    private int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
        int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastY = e.getY();
                if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dy = e.getY() - lastY;
                lastY = e.getY();
                pos -= dy / itemH;
                float max = items.length - 1;
                if (pos < 0) pos = pos * 0.35f;
                if (pos > max) pos = max + (pos - max) * 0.35f;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                int target = Math.max(0, Math.min(items.length - 1, Math.round(pos)));
                animateTo(target);
                if (listener != null) listener.onChange(target);
                return true;
            }
        }
        return super.onTouchEvent(e);
    }
}

