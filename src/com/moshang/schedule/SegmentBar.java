package com.moshang.schedule;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

/** 五段视图胶囊（OriginOS 观感）：白色药丸指示器，点击平滑滑动、按住可跟手拖动，文字颜色随指示器渐显 */
public class SegmentBar extends View {

    public interface OnSegment { void onSeg(int i); }

    private String[] names = new String[0];
    private int sel = 0;
    private float pos = 0f;
    private float downX, downY;
    private boolean dragging, horiz, decided;
    private int downCell = -1;

    private final Paint pBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();

    private int colBarBg, colPill, colTextOn, colTextOff;
    private float density, scaled, radius;
    private ValueAnimator anim;
    private OnSegment listener;

    public SegmentBar(Context c) { this(c, null); }

    public SegmentBar(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        colBarBg = getResources().getColor(R.color.segment_bg);
        colPill = Theme.wash(getContext());
        colTextOn = Theme.brand(getContext());
        colTextOff = getResources().getColor(R.color.segment_text);
        radius = dp(12);
        pText.setTextAlign(Paint.Align.CENTER);
        pBg.setStyle(Paint.Style.FILL);
        pPill.setStyle(Paint.Style.FILL);
    }

    private float dp(float v) { return v * density; }

    private float sp(float v) { return v * scaled; }

    public void setNames(String[] n) { names = n; invalidate(); }

    public void setOnSegment(OnSegment l) { listener = l; }

    public int getSelected() { return sel; }

    public void setSelected(int i, boolean animate) {
        if (i < 0 || i >= names.length) return;
        sel = i;
        if (animate) animateTo(i);
        else { if (anim != null) anim.cancel(); pos = i; invalidate(); }
    }

    private void animateTo(float target) {
        if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
        anim = ValueAnimator.ofFloat(pos, target);
        anim.setDuration(230);
        anim.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.press_interp));
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
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, (int) dp(40));
    }

    private float cellW() {
        if (names.length == 0 || getWidth() == 0) return 1f;
        return getWidth() / (float) names.length;
    }

    private float px0(int i) { return i * cellW(); }

    @Override
    protected void onDraw(Canvas cv) {
        if (names.length == 0 || getWidth() == 0) return;
        float w = getWidth(), h = getHeight();
        float cw = cellW();

        rf.set(0, 0, w, h);
        pBg.setColor(colBarBg);
        cv.drawRoundRect(rf, radius, radius, pBg);

        float inset = dp(3.5f);
        float px = pos * cw;
        pPill.setColor(colPill);
        cv.drawRoundRect(px + inset, inset, px + cw - inset, h - inset, radius * 0.82f, radius * 0.82f, pPill);

        for (int i = 0; i < names.length; i++) {
            float t = 1f - Math.min(1f, Math.abs(i - pos));
            pText.setColor(lerp(colTextOff, colTextOn, t));
            pText.setTextSize(sp(13));
            pText.setTypeface(t > 0.5f ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            float cy = h / 2f - (pText.ascent() + pText.descent()) / 2f;
            cv.drawText(names[i], px0(i) + cw / 2f, cy, pText);
        }
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
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX(); downY = ev.getY();
                dragging = false; horiz = false; decided = false;
                downCell = cellAt(ev.getX());
                if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = ev.getX() - downX, dy = ev.getY() - downY;
                if (!decided && (Math.abs(dx) > dp(6) || Math.abs(dy) > dp(6)))
                    decided = Math.abs(dx) > Math.abs(dy);
                if (decided && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true; horiz = true;
                    float q = ev.getX() / cellW() - 0.5f;
                    pos = Math.max(0f, Math.min(names.length - 1, q));
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (dragging && horiz) {
                    int target = Math.round(pos);
                    pos = target;
                    invalidate();
                    if (target != sel) { sel = target; if (listener != null) listener.onSeg(target); }
                } else {
                    int cell = cellAt(ev.getX());
                    if (cell >= 0 && cell == downCell) {
                        if (cell != sel) { sel = cell; if (listener != null) listener.onSeg(cell); }
                        animateTo(cell);
                    }
                }
                dragging = false; horiz = false;
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    private int cellAt(float x) {
        if (names.length == 0) return -1;
        int i = (int) (x / cellW());
        if (i < 0 || i >= names.length) return -1;
        return i;
    }
}

