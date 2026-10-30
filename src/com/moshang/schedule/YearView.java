package com.moshang.schedule;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;
import java.util.HashSet;

/** 年视图：12 个迷你月历（3 列 × 4 行），点击进入月视图 */
public class YearView extends View {

    public interface OnMonthPick { void onPick(int y, int m); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pSep = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float density;
    private float scaled;
    private int year;
    private HashSet<Long> marked = new HashSet<Long>();
    private int titleColor, normalColor, outColor, weekendColor, brand, sepColor;
    private OnMonthPick pick;
    private float titleH, cellW, cellH;

    public YearView(Context c) { this(c, null); }

    public YearView(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        titleH = dp(40);
        titleColor = getResources().getColor(R.color.text_primary);
        normalColor = getResources().getColor(R.color.text_primary);
        outColor = getResources().getColor(R.color.out_month);
        weekendColor = getResources().getColor(R.color.weekend);
        brand = Theme.brand(getContext());
        sepColor = getResources().getColor(R.color.grid_line);
        p.setTextAlign(Paint.Align.CENTER);
        pSep.setColor(sepColor);
        pSep.setStrokeWidth(dp(1));
    }

    private float dp(float v) { return v * density; }

    private float sp(float v) { return v * scaled; }

    public void setYear(int y) { year = y; invalidate(); }

    public void setMarked(HashSet<Long> m) { marked = m; invalidate(); }

    public void setOnMonthPick(OnMonthPick l) { pick = l; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        cellW = w / 3f;
        cellH = cellW * 0.98f;
        int h = (int) (titleH + 4 * cellH);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth();
        // 年份标题
        p.setTextSize(sp(20));
        p.setFakeBoldText(true);
        p.setColor(titleColor);
        cv.drawText(year + "年", w / 2f, titleH * 0.7f, p);
        p.setFakeBoldText(false);

        Calendar now = Calendar.getInstance();
        int todayY = now.get(Calendar.YEAR);
        int todayM = now.get(Calendar.MONTH);
        int todayD = now.get(Calendar.DAY_OF_MONTH);

        for (int m = 0; m < 12; m++) {
            int col = m % 3;
            int row = m / 3;
            float ox = col * cellW;
            float oy = titleH + row * cellH;
            drawMiniMonth(cv, ox, oy, m, todayY, todayM, todayD);
        }
    }

    private void drawMiniMonth(Canvas cv, float ox, float oy, int m, int todayY, int todayM, int todayD) {
        float pad = dp(8);
        // 标题
        p.setTextSize(sp(12));
        p.setColor(m == todayM && year == todayY ? brand : titleColor);
        p.setFakeBoldText(m == todayM && year == todayY);
        p.setTextAlign(Paint.Align.LEFT);
        cv.drawText((m + 1) + "月", ox + pad, oy + dp(16), p);
        p.setFakeBoldText(false);
        p.setTextAlign(Paint.Align.CENTER);

        float gridTop = oy + dp(22);
        float gridW = cellW - pad * 2;
        float colW = gridW / 7f;
        float avail = cellH - dp(24);
        float rowH = avail / 6f;

        Calendar c = Calendar.getInstance();
        c.set(year, m, 1);
        int idx = c.get(Calendar.DAY_OF_WEEK) - 1; // 周日为 0
        c.add(Calendar.DAY_OF_MONTH, -idx);

        p.setTextSize(sp(8.5f));
        for (int r = 0; r < 6; r++) {
            for (int col = 0; col < 7; col++) {
                int d = c.get(Calendar.DAY_OF_MONTH);
                int cm = c.get(Calendar.MONTH);
                int cy = c.get(Calendar.YEAR);
                boolean in = (cm == m);
                float cx = ox + pad + colW * col + colW / 2f;
                float baseY = gridTop + rowH * r + rowH * 0.62f;
                float numY = baseY;
                if (in) {
                    boolean isToday = (cy == todayY && cm == todayM && d == todayD);
                    if (isToday) {
                        p.setColor(brand);
                        cv.drawCircle(cx, baseY - dp(3.2f), dp(6.5f), p);
                        p.setColor(0xFFFFFFFF);
                    } else {
                        p.setColor((col == 0 || col == 6) ? weekendColor : normalColor);
                    }
                    cv.drawText(String.valueOf(d), cx, numY, p);

                    long day = Store.dayStart(c.getTimeInMillis());
                    if (marked.contains(Long.valueOf(day)) && !isToday) {
                        pDot.setColor(brand);
                        cv.drawCircle(cx, baseY + dp(4.5f), dp(1.4f), pDot);
                    }
                }
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        pSep.setColor(sepColor);
        cv.drawLine(ox + pad, oy + cellH - dp(1), ox + cellW - pad, oy + cellH - dp(1), pSep);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
            float x = ev.getX();
            float y = ev.getY();
            if (y > titleH) {
                int col = (int) Math.min(2, x / cellW);
                int row = (int) Math.min(3, (y - titleH) / cellH);
                int m = row * 3 + col;
                if (m >= 0 && m < 12) {
                    if (pick != null) pick.onPick(year, m);
                }
            }
        }
        return true;
    }
}

