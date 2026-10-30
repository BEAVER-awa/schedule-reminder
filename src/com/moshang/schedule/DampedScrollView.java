package com.moshang.schedule;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

/**
 * 行程提醒专用纵向滚动容器。
 *
 * 只有两个停靠位，不存在停在半路的状态：
 *   0        = 月历完全展开（顶部）
 *   maxY     = 只留表头 + 最后一行（底部）
 * 拖动过程按位移曲线跟手，松手时按「滑了多远 / 甩得多快」决定吸附到哪一端。
 *
 * 手势接管走 dispatchTouchEvent：子 View 调 requestDisallowInterceptTouchEvent
 * 也绕不过这里，保证阻尼一定生效；纵向没占主导时原样交给子 View。
 */
public class DampedScrollView extends ScrollView {

    private static final String BUILD_TAG = "damp-v4-snap";

    private static final float G0 = 0.06f;      // 起手瞬间的增益
    private static final float G1 = 0.94f;      // 曲线走满后的增益
    private static final float RAMP_DP = 110f;  // 曲线长度：越短中段越跟手
    private static final float RATIO_UP = 0.32f;    // 上滑（看日程）翻页门槛
    private static final float RATIO_DOWN = 0.50f;  // 下滑（回月历）翻页门槛
    private static final float FLING_UP = 700f;     // 上滑速度门槛 dp/s
    private static final float FLING_DOWN = 900f;   // 下滑速度门槛 dp/s
    private static final float EDGE_UP = 95f;       // 底部端头阻尼区
    private static final float EDGE_DOWN = 130f;    // 顶部端头阻尼区

    private VelocityTracker vt;
    private int touchSlop;
    private int maxFling;
    private float ramp, flingUp, flingDown, edgeUp, edgeDown;

    private int downX, downY;
    private int startScrollY;
    private boolean taking;
    private boolean snapping;

    public DampedScrollView(Context c) {
        super(c);
        init(c);
    }

    public DampedScrollView(Context c, AttributeSet a) {
        super(c, a);
        init(c);
    }

    private void init(Context c) {
        ViewConfiguration vc = ViewConfiguration.get(c);
        touchSlop = vc.getScaledTouchSlop();
        maxFling = vc.getScaledMaximumFlingVelocity();
        float d = c.getResources().getDisplayMetrics().density;
        ramp = RAMP_DP * d;
        flingUp = FLING_UP * d;
        flingDown = FLING_DOWN * d;
        edgeUp = EDGE_UP * d;
        edgeDown = EDGE_DOWN * d;
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setVerticalScrollBarEnabled(false);
    }

    public String buildTag() {
        return BUILD_TAG;
    }

    private int contentScrollMax() {
        if (getChildCount() == 0) return 0;
        int h = getChildAt(0).getHeight() + getPaddingTop() + getPaddingBottom();
        int m = h - getHeight();
        return m < 0 ? 0 : m;
    }

    /** 手指累计位移 -> 内容实际位移，增益 sqrt 型：前段重，后段轻。 */
    private int mapGain(int finger) {
        float a = Math.abs((float) finger);
        float out;
        if (a <= ramp) {
            float u = a / ramp;
            out = G0 * a + (G1 - G0) * (2f / 3f) * ramp * (float) Math.pow(u, 1.5);
        } else {
            out = G0 * ramp + (G1 - G0) * (2f / 3f) * ramp + (a - ramp) * G1;
        }
        return (int) (finger < 0 ? -out : out);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        final int act = e.getActionMasked();

        if (act == MotionEvent.ACTION_DOWN) {
            downX = (int) e.getX();
            downY = (int) e.getY();
            startScrollY = getScrollY();
            taking = false;
            snapping = false;
            if (vt == null) vt = VelocityTracker.obtain();
            vt.clear();
            vt.addMovement(e);
            super.dispatchTouchEvent(e);
            return true;
        }

        if (act == MotionEvent.ACTION_MOVE) {
            if (vt == null) vt = VelocityTracker.obtain();
            vt.addMovement(e);
            int y = (int) e.getY();
            int dy = y - downY;
            int dx = (int) e.getX() - downX;
            if (!taking) {
                if (Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                    taking = true;
                    snapping = false;
                    MotionEvent cancel = MotionEvent.obtain(e.getDownTime(), e.getEventTime(),
                            MotionEvent.ACTION_CANCEL, e.getX(), e.getY(), e.getMetaState());
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                } else {
                    return super.dispatchTouchEvent(e);
                }
            }
            int mx = contentScrollMax();
            int raw = -mapGain(dy);                    // 这一下想移动多少
            int target;
            int room = (raw > 0) ? (mx - startScrollY) : startScrollY;
            float edge = (raw > 0) ? edgeUp : edgeDown;
            if (room <= 0) {
                target = startScrollY;                 // 已经在端头，推不动
            } else if (room < edge) {
                // 靠近端点时改用渐近曲线：越推越远、但永远差最后一段，
                // 和线性结果按剩余空间比例混合，进出阻尼区不突变
                float r = Math.abs((float) raw);
                float lin = Math.min(r, room * 0.92f);
                float asym = room * (1f - (float) Math.exp(-r / room));
                float blend = 1f - room / edge;
                float mag = lin * (1f - blend) + asym * blend;
                target = startScrollY + (raw > 0 ? 1 : -1) * (int) mag;
            } else {
                target = startScrollY + raw;
            }
            if (target < 0) target = 0;
            if (target > mx) target = mx;
            if (target != getScrollY()) {
                if (snapping) snapping = false;
                scrollTo(0, target);
            }
            return true;
        }

        if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
            if (!taking) return super.dispatchTouchEvent(e);
            float vy = 0f;
            if (vt != null) {
                vt.addMovement(e);
                vt.computeCurrentVelocity(1000, maxFling);
                vy = vt.getYVelocity();
                vt.recycle();
                vt = null;
            }
            taking = false;
            resolveSnap((int) e.getY() - downY, vy);
            return true;
        }

        return super.dispatchTouchEvent(e);
    }

    /** 松手定去向：甩得够快按方向走；否则看已经滚过的路程有没有过半。 */
    private void resolveSnap(int finger, float vy) {
        int mx = contentScrollMax();
        if (mx <= 0) return;
        int sy = getScrollY();
        boolean upward = finger < 0 || (finger == 0 && vy < 0);
        float ratio = upward ? RATIO_UP : RATIO_DOWN;
        float vth = upward ? flingUp : flingDown;
        int snap;
        if (Math.abs(vy) >= vth) {
            snap = (vy < 0) ? mx : 0;
        } else if (sy > mx * ratio) {
            snap = mx;
        } else {
            snap = 0;
        }
        snapTo(snap);
    }

    private void snapTo(final int target) {
        final int from = getScrollY();
        if (from == target) return;
        snapping = true;
        final long t0 = System.currentTimeMillis();
        final int dur = Math.max(220, Math.min(420, 170 + Math.abs(target - from) / 2));
        postOnAnimation(new Runnable() {
            public void run() {
                float u = (System.currentTimeMillis() - t0) / (float) dur;
                if (u >= 1f) {
                    snapping = false;
                    scrollTo(0, target);
                    return;
                }
                float k = 1f - u;
                scrollTo(0, from + (int) ((target - from) * (1f - k * k * k)));
                if (snapping) postOnAnimation(this);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // 只消费不滚动：位置全部由 dispatchTouchEvent 里的映射与吸附决定
        return true;
    }
}
