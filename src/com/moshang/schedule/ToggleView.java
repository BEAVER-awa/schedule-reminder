package com.moshang.schedule;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

/** 自绘开关（替代原生 Switch） */
public class ToggleView extends View {

    public interface OnChange { void onChange(boolean on); }

    private boolean on;
    private float t;
    private ValueAnimator anim;
    private final Paint pTrack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pKnob = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private int colOff, colOn;
    private float density;
    private OnChange listener;

    public ToggleView(Context c) { this(c, null); }

    public ToggleView(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        colOff = getResources().getColor(R.color.toggle_off);
        colOn = Theme.brand(getContext());
        pKnob.setColor(0xFFFFFFFF);
        t = on ? 1f : 0f;
    }

    private float dp(float v) { return v * density; }

    public void setOn(boolean v) { setOn(v, false); }

    public void setOn(boolean v, boolean animate) {
        boolean changed = (v != on);
        on = v;
        if (animate) {
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(t, on ? 1f : 0f);
            anim.setDuration(200);
            anim.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.press_interp));
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator a) {
                    t = ((Float) a.getAnimatedValue()).floatValue();
                    invalidate();
                }
            });
            anim.start();
        } else {
            t = on ? 1f : 0f;
            invalidate();
        }
        if (changed && listener != null) listener.onChange(on);
    }

    public boolean isOn() { return on; }

    public void setOnChange(OnChange l) { listener = l; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        setMeasuredDimension((int) dp(46), (int) dp(27));
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float r = h / 2f;
        pTrack.setColor(lerp(colOff, colOn, t));
        rf.set(0, 0, w, h);
        cv.drawRoundRect(rf, r, r, pTrack);

        float kr = r - dp(2.5f);
        float cx = dp(2.5f) + kr + t * (w - 2 * (dp(2.5f) + kr));
        pKnob.setShadowLayer(dp(1.5f), 0, dp(1), 0x33000000);
        cv.drawCircle(cx, r, kr, pKnob);
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
                return true;
            case MotionEvent.ACTION_UP:
                setOn(!on, true);
                return true;
        }
        return super.onTouchEvent(e);
    }
}

