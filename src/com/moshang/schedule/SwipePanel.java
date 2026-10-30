package com.moshang.schedule;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * 月页容器：上面月历固定占位，下面是可上移的面板。
 *
 * 面板分两段移动：
 *   第一段 0 ~ (月历高 − 一行)：面板整体上移，同时月历内部按行裁切，
 *          停住时屏幕上只剩选中日期那一行；
 *   第二段 超出部分：面板顶钉在行底下不动，只让面板内部内容继续上移，
 *          用来滚完当天的全部日程 —— 月历始终只露一行。
 *
 * 位移带 sqrt 型阻尼，松手按「过半 / 甩得快」吸附到展开或收起两端。
 */
public class SwipePanel extends FrameLayout {

    private static final float G0 = 0.06f;
    private static final float G1 = 0.93f;
    private static final float RAMP_DP = 110f;
    private static final float RATIO_UP = 0.30f;    // 上滑看日程：过三成就翻
    private static final float RATIO_DOWN = 0.32f;  // 下滑看日期：过三成即回
    private static final float FLING_UP_DP = 550f;
    private static final float FLING_DOWN_DP = 600f;
    private static final float UP_GAIN = 1.25f;     // 上滑位移放大
    private static final float DOWN_GAIN = 1.05f;   // 下滑时面板更沉
    private static final float EDGE_DP = 90f;       // 最后这段范围内阻尼递增

    private final LinearLayout panel;   // 整个面板（高度 = 容器高）
    private final LinearLayout head;    // 面板顶部固定区（农历条）
    private final LinearLayout body;    // 面板内可滚动区（日程卡片）
    private final java.util.ArrayList<View> fixed = new java.util.ArrayList<View>();
    private int fixedH;

    private VelocityTracker vt;
    private int touchSlop;
    private int maxFling;
    private float ramp, flingUp, flingDown, edge;

    private int downX, downY;
    private float offset, startOffset;
    private boolean taking, snapping;
    private float keepTop = 1 << 20;
    private float calShift, calMax = 1 << 20;
    private OnMove onMove;

    public interface OnMove {
        void onMove(float offset, float calShift);
    }

    public SwipePanel(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        ramp = RAMP_DP * d;
        flingUp = FLING_UP_DP * d;
        flingDown = FLING_DOWN_DP * d;
        edge = EDGE_DP * d;
        ViewConfiguration vc = ViewConfiguration.get(c);
        touchSlop = vc.getScaledTouchSlop();
        maxFling = vc.getScaledMaximumFlingVelocity();

        panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(c.getResources().getColor(R.color.window_bg));
        head = new LinearLayout(c);
        head.setOrientation(LinearLayout.VERTICAL);
        body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        panel.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.setClipChildren(true);
        panel.setClipToPadding(true);
        super.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /** 面板内可滚动区，日程卡片往这里塞 */
    public LinearLayout getPanel() {
        return body;
    }

    /** 面板顶部固定区（农历条之类，不随内容滚动） */
    public void addHead(View v) {
        head.addView(v, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /** 加一块固定在月历区的 View（只放月历） */
    public void addFixed(View v) {
        fixed.add(v);
        super.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setOnMove(OnMove l) {
        onMove = l;
    }

    public void refreshPanel() {
        requestLayout();
        post(new Runnable() {
            public void run() {
                float mx = maxOffset();
                if (offset > mx) setOffset(mx);
            }
        });
    }

    public void resetOffset() {
        setOffset(0);
    }

    /** 月历收起量的上限 = 选中日期所在行的顶部 */
    public void setCalMax(float px) {
        calMax = px;
    }

    /** 面板顶部能升到的最高位置（px，相对本容器顶部）；通常传一行的行高 */
    public void setKeepTop(float px) {
        keepTop = px;
        float mx = maxOffset();
        if (offset > mx) setOffset(mx);
    }

    private float firstLeg() {
        float v = fixedH - keepTop;
        return v < 0 ? 0 : v;
    }

    private float maxOffset() {
        float base = firstLeg();
        float sh = getHeight();
        float bodyH = body.getMeasuredHeight();
        if (sh <= 0 || bodyH <= 0) return base;
        float visible = sh - keepTop - head.getMeasuredHeight();
        if (visible < 0) visible = 0;
        float extra = bodyH - visible;
        return base + (extra > 0 ? extra : 0);
    }

    private void setOffset(float v) {
        offset = v;
        float base = firstLeg();
        float shift = Math.min(offset, base);
        float inner = Math.max(0, offset - base);
        panel.setTranslationY(-shift);
        body.setTranslationY(-inner);

        // 月历收起量按面板位移等比例映射：走到行程尽头时，选中行刚好贴到顶部。
        // 选中行越靠上、calMax 越小，收敛得就越慢，不会「滑一点点就已经贴顶」。
        if (base <= 0) {
            calShift = 0;
        } else {
            float p = Math.min(offset, base) / base;
            calShift = calMax * p;
        }
        if (calShift < 0) calShift = 0;
        if (calShift > calMax) calShift = calMax;
        if (onMove != null) onMove.onMove(offset, calShift);
    }

    private float mapAbs(float a) {
        if (a <= ramp) {
            return G0 * a + (G1 - G0) * (2f / 3f) * ramp * (float) Math.pow(a / ramp, 1.5);
        }
        return G0 * ramp + (G1 - G0) * (2f / 3f) * ramp + (a - ramp) * G1;
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        int w = MeasureSpec.getSize(ws);
        int h = MeasureSpec.getSize(hs);
        int cw = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        for (int i = 0; i < fixed.size(); i++) {
            View c = fixed.get(i);
            c.measure(cw, getChildMeasureSpec(hs, 0, c.getLayoutParams().height));
        }
        // 面板高度固定成容器高：内容超出部分靠 body 上移露出，不用撑高面板
        panel.measure(cw, MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 0 && getChildAt(getChildCount() - 1) != panel) {
            panel.bringToFront();
        }
        int w = r - l;
        int top = 0;
        for (int i = 0; i < fixed.size(); i++) {
            View c = fixed.get(i);
            int hh = c.getMeasuredHeight();
            c.layout(0, top, w, top + hh);
            top += hh;
        }
        fixedH = top;
        panel.layout(0, top, w, top + panel.getMeasuredHeight());
        setOffset(offset);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        final int act = e.getActionMasked();

        if (act == MotionEvent.ACTION_DOWN) {
            downX = (int) e.getX();
            downY = (int) e.getY();
            startOffset = offset;
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
                boolean fromPanel = downY >= fixedH - offset;
                if (fromPanel && Math.abs(dy) > touchSlop && Math.abs(dy) > Math.abs(dx)) {
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
            float f = -dy;
            float mag = mapAbs(Math.abs((float) dy));
            mag *= (f > 0) ? UP_GAIN : DOWN_GAIN;   // 两个方向都比手指位移推得更远
            float mx = maxOffset();
            float target;
            float room = (f > 0) ? (mx - startOffset) : startOffset;
            if (room <= 0) {
                target = startOffset;
            } else if (room < edge) {
                // 三次方衰减：remaining 越小越陡，最后 45dp 内几乎推不动
                float k = room / edge;
                float damp = k * k * k;             // 三次方：范围铺开，末端仍很陡
                float m2 = mag * damp;
                if (m2 > room * 0.97f) m2 = room * 0.97f;
                target = startOffset + (f > 0 ? m2 : -m2);
            } else {
                target = startOffset + (f > 0 ? mag : -mag);
            }
            if (target < 0) target = 0;
            if (target > mx) target = mx;
            setOffset(target);
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

    private void resolveSnap(int finger, float vy) {
        float mx = maxOffset();
        if (mx <= 0) return;
        boolean upward = finger < 0 || (finger == 0 && vy < 0);
        float vth = upward ? flingUp : flingDown;
        int snap;
        if (Math.abs(vy) >= vth) {
            snap = (vy < 0) ? (int) mx : 0;
        } else if (upward) {
            snap = (offset > mx * RATIO_UP) ? (int) mx : 0;
        } else {
            // 下滑看的是月历展开到什么程度，不是面板退了多少
            snap = (calShift < calMax * 0.5f) ? 0 : (int) mx;
        }
        settleTo(snap);
    }

    private void settleTo(final float target) {
        final float from = offset;
        if (Math.abs(from - target) < 0.5f) return;
        snapping = true;
        final long t0 = System.currentTimeMillis();
        // 归位要利落：短距离 150ms 起，长距离封顶 280ms
        final int dur = Math.max(150, Math.min(280, 120 + (int) (Math.abs(target - from) / 4)));
        postOnAnimation(new Runnable() {
            public void run() {
                float u = (System.currentTimeMillis() - t0) / (float) dur;
                if (u >= 1f) {
                    snapping = false;
                    setOffset(target);
                    return;
                }
                float k = 1f - u;
                setOffset(from + (target - from) * (1f - k * k * k));
                if (snapping) postOnAnimation(this);
            }
        });
    }
}
