package com.moshang.schedule;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** 顶部年月标题（vivo 风格）：点按打开年月选择，左右滑动切换月份 */
public class TitleBar extends View {

    public interface OnAction {
        void onTap();
        void onSwipe(int dir);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String head = "";
    private String accent = "";
    private int colText, colAccent;
    private float density, scaled;
    private float downX, downY;
    private boolean moved, horiz;
    private android.view.VelocityTracker vt;
    private OnAction listener;
    private android.animation.ValueAnimator arrowAnim;
    private float arrowAng = 0f;
    private boolean arrowVisible = false;
    private int colArrow;

    public TitleBar(Context c) { this(c, null); }

    public TitleBar(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        colText = getResources().getColor(R.color.text_primary);
        colAccent = Theme.brand(getContext());
        colArrow = getResources().getColor(R.color.text_secondary);
        p.setTextSize(scaled * 21f);
        p.setLetterSpacing(-0.01f);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setColor(colText);
        setClickable(true);
    }

    private float dp(float v) { return v * density; }

    public void setTitle(String head, String accent) {
        this.head = head == null ? "" : head;
        this.accent = accent == null ? "" : accent;
        invalidate();
    }

    public void setOnAction(OnAction l) { listener = l; }

    /** 是否显示标题后的下拉箭头 */
    public void setArrowVisible(boolean v) {
        if (arrowVisible != v) { arrowVisible = v; invalidate(); }
    }

    /** 展开/收起箭头旋转（0=朝下 可展开，180=朝上 可收回） */
    public void setExpanded(boolean ex) {
        final float to = ex ? 180f : 0f;
        if (Math.abs(arrowAng - to) < 0.5f) return;
        if (arrowAnim != null) arrowAnim.cancel();
        arrowAnim = android.animation.ValueAnimator.ofFloat(arrowAng, to);
        arrowAnim.setDuration(200);
        arrowAnim.setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                getContext(), R.interpolator.press_interp));
        arrowAnim.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                arrowAng = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        arrowAnim.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int wMode = MeasureSpec.getMode(wSpec);
        int w;
        if (wMode == MeasureSpec.EXACTLY) {
            w = MeasureSpec.getSize(wSpec);
        } else {
            float need = p.measureText(head)
                    + (accent.length() > 0 ? p.measureText(accent) : 0f) + dp(6);
            w = (int) Math.ceil(need);
            if (wMode == MeasureSpec.AT_MOST) w = Math.min(w, MeasureSpec.getSize(wSpec));
        }
        int h = MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(hSpec) : (int) dp(40);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float base = getHeight() / 2f - (p.ascent() + p.descent()) / 2f;
        p.setColor(colText);
        cv.drawText(head, 0, base, p);
        float tw = p.measureText(head);
        if (accent.length() > 0) {
            p.setColor(colAccent);
            cv.drawText(accent, tw, base, p);
            tw += p.measureText(accent);
        }
        if (arrowVisible) {
            float cx = tw + dp(9);
            float cy = getHeight() / 2f + dp(1f);
            float hw = dp(4.6f), hh = dp(3.2f);
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(cx - hw, cy - hh);
            path.lineTo(cx + hw, cy - hh);
            path.lineTo(cx, cy + hh);
            path.close();
            p.setColor(colArrow);
            int save = cv.save();
            cv.rotate(arrowAng, cx, cy);
            cv.drawPath(path, p);
            cv.restoreToCount(save);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                moved = false; horiz = false;
                if (vt == null) vt = android.view.VelocityTracker.obtain(); else vt.clear();
                vt.addMovement(ev);
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (vt != null) vt.addMovement(ev);
                float dx = ev.getX() - downX, dy = ev.getY() - downY;
                if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) moved = true;
                if (Math.abs(dx) > dp(8) && Math.abs(dx) > Math.abs(dy)) horiz = true;
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float vx = 0f;
                if (vt != null) {
                    vt.addMovement(ev);
                    vt.computeCurrentVelocity(1000);
                    vx = vt.getXVelocity();
                    vt.recycle();
                    vt = null;
                }
                if (horiz) {
                    float dx = ev.getX() - downX;
                    float th = Math.max(dp(18), getWidth() * 0.08f);
                    float vth = dp(250);
                    if (dx <= -th || vx <= -vth) {
                        if (listener != null) listener.onSwipe(1);
                    } else if (dx >= th || vx >= vth) {
                        if (listener != null) listener.onSwipe(-1);
                    }
                } else if (!moved) {
                    if (listener != null) listener.onTap();
                }
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }
}

