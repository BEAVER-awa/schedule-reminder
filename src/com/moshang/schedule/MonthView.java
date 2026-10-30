package com.moshang.schedule;

import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/** 月视图（OriginOS 风格）：周日起始 + 农历/节气小字 + 事件圆点；左右滑动跟手翻月 */
public class MonthView extends View {

    public interface OnDayClick { void onDay(int y, int m, int d); }
    public interface OnMonthChange { void onMonth(int y, int m); }
    public interface DotSource { HashMap<Integer, List<Integer>> dotsFor(int year, int month); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pWash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();

    private int year, month;
    private int selY, selM, selD;
    private float gridShift;   // 整体上移量：收起时把选中那一行顶到 View 顶部
    private final int todayY, todayM, todayD;

    private HashMap<Integer, List<Integer>> dots = new HashMap<Integer, List<Integer>>();
    private DotSource dotSource;
    private final HashMap<String, HashMap<Integer, List<Integer>>> dotCache =
            new HashMap<String, HashMap<Integer, List<Integer>>>();

    private String[] labCur, labPrev, labNext;
    private boolean[] hiCur, hiPrev, hiNext;
    private boolean ready = false;

    private int headerColor, normalColor, outColor, brandColor, selBgColor, selTextColor, lunarColor, lunarHiColor, weekendColor, todayWashColor, todayRingColor;
    private static final String[] WB = {"日", "一", "二", "三", "四", "五", "六"};

    private float density, scaled, cellH, headerH;
    private final int rows = 6;

    private OnDayClick dayClick;
    private OnMonthChange monthChange;

    private float offX = 0f;
    private float downX, downY;
    private boolean dragging, horiz, moved;
    private ValueAnimator anim;
    private int stepDur = 250;
    private android.view.VelocityTracker vt;

    public MonthView(Context c) { this(c, null); }

    public MonthView(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        cellH = getResources().getDimension(R.dimen.month_cell_height);
        headerH = dp(28);
        headerColor = getResources().getColor(R.color.weekday);
        normalColor = getResources().getColor(R.color.text_primary);
        outColor = getResources().getColor(R.color.out_month);
        brandColor = Theme.brand(getContext());
        selBgColor = Theme.selBg(getContext());
        selTextColor = getResources().getColor(R.color.day_sel_text);
        todayWashColor = Theme.wash(getContext());
        todayRingColor = Theme.brand(getContext());
        lunarColor = getResources().getColor(R.color.lunar_text);
        lunarHiColor = getResources().getColor(R.color.lunar_hi);
        weekendColor = getResources().getColor(R.color.weekend);

        Calendar now = Calendar.getInstance();
        todayY = now.get(Calendar.YEAR);
        todayM = now.get(Calendar.MONTH);
        todayD = now.get(Calendar.DAY_OF_MONTH);
        year = todayY; month = todayM;
        selY = todayY; selM = todayM; selD = todayD;

        p.setTextAlign(Paint.Align.CENTER);
        pDot.setStyle(Paint.Style.FILL);
        pSel.setStyle(Paint.Style.FILL);
        pSel.setColor(selBgColor);
        pWash.setStyle(Paint.Style.FILL);
        pWash.setColor(todayWashColor);
        pRing.setStyle(Paint.Style.STROKE);
        pRing.setStrokeWidth(dp(1.5f));
        pRing.setColor(todayRingColor);
        p.setFontFeatureSettings("tnum");
        recompute();
    }

    private float dp(float v) { return v * density; }

    private float sp(float v) { return v * scaled; }

    public void setMonth(int y, int m) {
        if (y == year && m == month) { invalidate(); return; }
        year = y; month = m;
        recompute();
        invalidate();
        updateA11y();
    }

    /** 读屏播报：月历整体状态（当前年月 + 已选日期 + 当天行程数） */
    private void updateA11y() {
        String[] WKN = {"日", "一", "二", "三", "四", "五", "六"};
        Calendar c = Calendar.getInstance();
        c.set(selY, selM, selD);
        int wd = c.get(Calendar.DAY_OF_WEEK) - 1;
        int cnt = 0;
        List<Integer> ds = dots.get(selD);
        if (ds != null) cnt = ds.size();
        setContentDescription(String.format(java.util.Locale.CHINA,
                "月历，%d年%d月，已选 %d月%d日 星期%s，当天 %d 个行程",
                selY, selM + 1, selM + 1, selD, WKN[wd], cnt));
    }

    public void setSelected(int y, int m, int d) {
        if (y == selY && m == selM && d == selD) return;
        selY = y; selM = m; selD = d;
        invalidate();
        updateA11y();
    }

    public void setDots(HashMap<Integer, List<Integer>> map) {
        dots = map;
        dotCache.clear();
        invalidate();
        updateA11y();
    }

    public void setDotSource(DotSource s) {
        dotSource = s;
        dotCache.clear();
        invalidate();
    }

    public void setDayClick(OnDayClick l) { dayClick = l; }

    public void setMonthChange(OnMonthChange l) { monthChange = l; }

    public int getYear() { return year; }

    public int getMonth() { return month; }

    private HashMap<Integer, List<Integer>> dotsFor(int y, int m) {
        if (dotSource == null) return new HashMap<Integer, List<Integer>>();
        String k = y + "-" + m;
        HashMap<Integer, List<Integer>> v = dotCache.get(k);
        if (v == null) {
            v = dotSource.dotsFor(y, m);
            if (v == null) v = new HashMap<Integer, List<Integer>>();
            dotCache.put(k, v);
        }
        return v;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = (int) (headerH + rows * cellH);
        setMeasuredDimension(w, h);
    }

    private Calendar firstCell(int y, int m) {
        Calendar c = Calendar.getInstance();
        c.set(y, m, 1);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DAY_OF_MONTH, -(dow - 1));
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static int[] shift(int y, int m, int d) {
        m += d;
        if (m < 0) { m = 11; y--; }
        if (m > 11) { m = 0; y++; }
        return new int[]{y, m};
    }

    private void computeInto(Calendar start, String[] lab, boolean[] h) {
        Calendar cur = (Calendar) start.clone();
        for (int i = 0; i < 42; i++) {
            int y = cur.get(Calendar.YEAR);
            int m = cur.get(Calendar.MONTH) + 1;
            int d = cur.get(Calendar.DAY_OF_MONTH);
            if (y >= 1900 && y <= 2100) {
                String[] r = Lunar.cellLabel(y, m, d);
                lab[i] = r[0];
                h[i] = "1".equals(r[1]);
            } else {
                lab[i] = "";
                h[i] = false;
            }
            cur.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void recompute() {
        labCur = new String[42]; hiCur = new boolean[42];
        labPrev = new String[42]; hiPrev = new boolean[42];
        labNext = new String[42]; hiNext = new boolean[42];
        computeInto(firstCell(year, month), labCur, hiCur);
        int[] pm = shift(year, month, -1);
        int[] nm = shift(year, month, 1);
        computeInto(firstCell(pm[0], pm[1]), labPrev, hiPrev);
        computeInto(firstCell(nm[0], nm[1]), labNext, hiNext);
        dots = dotsFor(year, month);
        ready = true;
    }

    /** 整体上移：收起月历时用它把选中那一行顶到 View 顶部，其余行移出上边界被裁掉 */
    public void setGridShift(float v) {
        if (v < 0) v = 0;
        if (Math.abs(v - gridShift) < 0.5f) return;
        gridShift = v;
        invalidate();
    }

    public float getGridShift() {
        return gridShift;
    }

    @Override
    protected void onDraw(Canvas cv) {
        if (!ready) recompute();
        int w = getWidth();
        if (w == 0) return;
        float colW = w / 7f;

        int save = cv.save();
        cv.translate(0, -gridShift);              // 表头与网格一起动
        cv.clipRect(0, (int) gridShift, w, getHeight() + (int) gridShift);

        p.setTextSize(sp(11));
        float hy = headerH / 2f - (p.ascent() + p.descent()) / 2f;
        p.setColor(headerColor);
        for (int i = 0; i < 7; i++) {
            cv.drawText(WB[i], colW * i + colW / 2f, hy, p);
        }

        int[] pm = shift(year, month, -1);
        int[] nm = shift(year, month, 1);
        drawGrid(cv, pm[0], pm[1], labPrev, hiPrev, dotsFor(pm[0], pm[1]), offX - w);
        drawGrid(cv, year, month, labCur, hiCur, dots, offX);
        drawGrid(cv, nm[0], nm[1], labNext, hiNext, dotsFor(nm[0], nm[1]), offX + w);
        cv.restoreToCount(save);
    }

    private void drawGrid(Canvas cv, int y, int m, String[] lab, boolean[] hi,
                          HashMap<Integer, List<Integer>> dm, float ox) {
        if (lab == null) return;
        float colW = getWidth() / 7f;
        Calendar cur = firstCell(y, m);
        float cy = headerH;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 7; c++) {
                int d = cur.get(Calendar.DAY_OF_MONTH);
                int mm = cur.get(Calendar.MONTH);
                int yy = cur.get(Calendar.YEAR);
                int idx = r * 7 + c;
                float cx = ox + colW * c + colW / 2f;
                boolean inMonth = (mm == m);
                boolean isToday = (yy == todayY && mm == todayM && d == todayD);
                boolean isSel = (yy == selY && mm == selM && d == selD);

                float numCy = cy + cellH * 0.34f;
                float numBase = numCy - (p.ascent() + p.descent()) / 2f;
                // 整格高亮：宽度铺满单元格、高度盖住公历 + 农历 + 事件点
                float halfW = colW / 2f - dp(3.5f);
                float top = cy + dp(4), bot = cy + cellH - dp(4);

                if (isSel) {
                    rf.set(cx - halfW, top, cx + halfW, bot);
                    cv.drawRoundRect(rf, dp(12), dp(12), pSel);
                } else if (isToday) {
                    rf.set(cx - halfW, top, cx + halfW, bot);
                    cv.drawRoundRect(rf, dp(12), dp(12), pRing);
                }

                int tc;
                if (!inMonth) tc = outColor;
                else if (isSel) tc = selTextColor;
                else if (isToday) tc = brandColor;
                else if (c == 0 || c == 6) tc = weekendColor;
                else tc = normalColor;
                p.setTextSize(sp(16));
                p.setColor(tc);
                cv.drawText(String.valueOf(d), cx, numBase, p);

                if (inMonth && lab[idx] != null && lab[idx].length() > 0) {
                    p.setTextSize(sp(10));
                    p.setColor(hi[idx] ? lunarHiColor : lunarColor);
                    cv.drawText(lab[idx], cx, cy + cellH * 0.66f, p);
                }

                if (inMonth && dm != null) {
                    List<Integer> ds = dm.get(d);
                    if (ds != null && !ds.isEmpty()) {
                        float dr = dp(1.8f);
                        float gap = dp(6f);
                        int n = Math.min(ds.size(), 3);
                        float totalW = (n - 1) * gap;
                        float dx0 = cx - totalW / 2f;
                        float dy = cy + cellH * 0.87f;
                        for (int k = 0; k < n; k++) {
                            pDot.setColor(ds.get(k));
                            cv.drawCircle(dx0 + k * gap, dy, dr, pDot);
                        }
                    }
                }
                cur.add(Calendar.DAY_OF_MONTH, 1);
            }
            cy += cellH;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                moved = false; dragging = false; horiz = false;
                if (vt == null) vt = android.view.VelocityTracker.obtain(); else vt.clear();
                vt.addMovement(ev);
                if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (vt != null) vt.addMovement(ev);
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                if (Math.abs(dx) > dp(12) || Math.abs(dy) > dp(12)) moved = true;
                if (!dragging && Math.abs(dx) > dp(8) && Math.abs(dx) > Math.abs(dy)) {
                    dragging = true; horiz = true;
                    // 横向翻月：不让外层上下滚动容器抢手势
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (dragging && horiz) {
                    float w = getWidth();
                    offX = Math.max(-w, Math.min(w, dx));
                    invalidate();
                }
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
                if (dragging && horiz) {
                    float w = getWidth();
                    float th = Math.max(dp(24), w * 0.10f);
                    float vth = dp(250);
                    if (offX <= -th || (vx <= -vth && offX < -dp(6))) settle(1, false);
                    else if (offX >= th || (vx >= vth && offX > dp(6))) settle(-1, false);
                    else startAnim(offX, 0f, 200, null);
                } else if (!moved && ev.getActionMasked() == MotionEvent.ACTION_UP) {
                    handleTap(ev.getX(), ev.getY());
                }
                dragging = false; horiz = false;
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    /** 供 ‹ › 箭头调用：与手势翻月同一条动画 */
    public void shiftMonthAnimated(int delta) {
        if (getWidth() == 0) {
            int[] nm = shift(year, month, delta);
            year = nm[0]; month = nm[1];
            recompute(); invalidate();
            if (monthChange != null) monthChange.onMonth(year, month);
            return;
        }
        stepDur = 200;
        settle(delta, false);
    }

    private static int[] moveBy(int y, int m, int k) {
        int t = y * 12 + m + k;
        return new int[]{t / 12, t % 12};
    }

    /** 平滑滑动到指定月份：「今」按钮用。
     *  用【一条】动画贯穿整段行程，逐帧推导当前页与页内偏移，
     *  避免逐页重启动画造成页与页之间的停顿。 */
    public void goToMonthAnimated(int ty, int tm) {
        int tgt = ty * 12 + tm, cur = year * 12 + month;
        if (tgt == cur) { invalidate(); return; }
        if (getWidth() == 0) {
            year = ty; month = tm;
            recompute(); invalidate();
            if (monthChange != null) monthChange.onMonth(ty, tm);
            return;
        }
        final int steps = Math.abs(tgt - cur);
        final int dir = tgt > cur ? 1 : -1;
        final int sY = year, sM = month;

        // 两段式：
        //   前段 = 前 (steps-1) 页匀速快翻（每页 flipMs）
        //   末段 = 最后一页固定 brakeMs，用强缓出在很短窗口内重减速 -> 死停
        int flipMs = 45;
        if (steps > 1 && (steps - 1) * flipMs > 620) flipMs = 620 / (steps - 1);
        final int brakeMs = 200;
        final int bounceMs = 360;
        final int totalMs = (steps - 1) * flipMs + brakeMs + bounceMs;
        final float f1 = steps <= 1 ? 0f : (steps - 1f) / steps;
        final float t1 = steps <= 1 ? 0f : ((steps - 1) * (float) flipMs) / totalMs;
        final float t2 = ((steps - 1) * (float) flipMs + brakeMs) / totalMs;

        // 三段式：① 匀速快翻 ② 最后一页重减速到位 ③ 撞停回弹（超冲后回弹归位）
        if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
        anim = ValueAnimator.ofFloat(0f, steps);
        anim.setDuration(totalMs);
        anim.setInterpolator(new android.animation.TimeInterpolator() {
            public float getInterpolation(float t) {
                if (t <= t1) return t1 <= 0f ? 0f : (t / t1) * f1;
                if (t <= t2) {
                    float u = (t - t1) / (t2 - t1);
                    return f1 + (1f - f1) * (1f - (float) Math.pow(1f - u, 2.2));
                }
                float v = (t - t2) / (1f - t2);
                // ③a 超冲(0~24%) -> ③b 停顿(24%~74%,停得更久) -> ③c 快速回弹(74%~100%)
                if (v <= 0.24f) {
                    float u = v / 0.24f;
                    return 1f + 0.040f * (1f - (1f - u) * (1f - u));
                }
                if (v <= 0.74f) return 1f + 0.040f;
                float u = (v - 0.74f) / 0.26f;
                return 1f + 0.040f * (float) Math.pow(1f - u, 1.8);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                float f = ((Float) a.getAnimatedValue()).floatValue();
                int k = (int) f;
                float frac = f - k;
                if (k >= steps) { k = steps; if (frac > 0.12f) frac = 0.12f; }
                int[] mm = moveBy(sY, sM, dir * k);
                boolean changed = (mm[0] != year || mm[1] != month);
                year = mm[0];
                month = mm[1];
                offX = -dir * frac * getWidth();
                if (changed) recompute();
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(android.animation.Animator a) {
                anim = null;
                offX = 0f;
                recompute();
                invalidate();
                if (monthChange != null) monthChange.onMonth(year, month);
            }
        });
        anim.start();
    }

    private void settle(final int delta, final boolean silent) {
        if (!silent) stepDur = 200;
        final float w = getWidth();
        float target = delta > 0 ? -w : w;
        startAnim(offX, target, stepDur, new Runnable() {
            public void run() {
                int[] nm = shift(year, month, delta);
                year = nm[0];
                month = nm[1];
                offX = 0f;
                recompute();
                invalidate();
                if (!silent && monthChange != null) monthChange.onMonth(year, month);
            }
        });
    }

    private void startAnim(float from, float to, int dur, final Runnable end) {
        if (anim != null) { anim.removeAllListeners(); anim.cancel(); }
        anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration(dur);
        anim.setInterpolator(AnimationUtils.loadInterpolator(getContext(), R.interpolator.vivo_enter));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                offX = ((Float) a.getAnimatedValue()).floatValue();
                invalidate();
            }
        });
        if (end != null) {
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(android.animation.Animator a) {
                    anim = null;
                    end.run();
                }
            });
        }
        anim.start();
    }

    /** 当前选中日期所在那一行的顶部 y（相对本 View），用于上滑时把这一行留在屏幕上 */
    public int selectedRowTop() {
        Calendar a = firstCell(year, month);
        Calendar b = Calendar.getInstance();
        b.set(selY, selM, selD, 12, 0, 0);
        a.set(Calendar.HOUR_OF_DAY, 12);
        long diff = Math.round((Store.dayStart(b.getTimeInMillis())
                - Store.dayStart(a.getTimeInMillis())) / (double) Store.DAY);
        int idx = (int) diff;
        if (idx < 0) idx = 0;
        int row = idx / 7;
        if (row > rows - 1) row = rows - 1;
        return (int) (headerH + row * cellH);
    }

    private void handleTap(float x, float y) {
        y += gridShift;
        if (y < headerH) return;
        int col = (int) (x / (getWidth() / 7f));
        int row = (int) ((y - headerH) / cellH);
        if (col < 0 || col > 6 || row < 0 || row > 5) return;
        Calendar cur = firstCell(year, month);
        cur.add(Calendar.DAY_OF_MONTH, row * 7 + col);
        int y2 = cur.get(Calendar.YEAR);
        int m2 = cur.get(Calendar.MONTH);
        int d2 = cur.get(Calendar.DAY_OF_MONTH);
        selY = y2; selM = m2; selD = d2;
        if (m2 != month || y2 != year) {
            year = y2;
            month = m2;
            recompute();
            if (monthChange != null) monthChange.onMonth(y2, m2);
        }
        invalidate();
        if (dayClick != null) dayClick.onDay(y2, m2, d2);
    }
}

