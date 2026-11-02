package com.moshang.schedule;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** 周视图时间轴：左侧小时刻度 + 7 天列，事件按起止时间定位（OriginOS 周视图观感） */
public class WeekTimelineView extends View {

    public interface OnEventClick { void onClick(String id); }

    public static final float ROW_DP = 50f;

    private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pEv = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pEvTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pNow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pCol = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();

    private final Store store;
    private float density, scaled, gutter, rowH;
    private long weekStart;
    private int selIndex = 0;

    private final List<RectF> rects = new ArrayList<RectF>();
    private final List<String> ids = new ArrayList<String>();

    private int colLine, colText, colGutter, colNow, colSel;
    private float downX, downY;
    private boolean moved;
    private OnEventClick listener;

    public WeekTimelineView(Context c) { this(c, null); }

    public WeekTimelineView(Context c, AttributeSet a) {
        super(c, a);
        store = Store.get(c);
        Resources r = getResources();
        density = r.getDisplayMetrics().density;
        scaled = r.getDisplayMetrics().scaledDensity;
        gutter = dp(36);
        rowH = dp(ROW_DP);
        colLine = r.getColor(R.color.grid_line);
        colText = r.getColor(R.color.text_tertiary);
        colGutter = r.getColor(R.color.text_secondary);
        colNow = Theme.brand(getContext());
        colSel = r.getColor(R.color.chip_bg_on);
        pLine.setStrokeWidth(dp(1));
        pLine.setColor(colLine);
        pTxt.setTextSize(sp(10));
        pTxt.setFontFeatureSettings("tnum");
        pTxt.setFontFeatureSettings("tnum");
        pTxt.setTextAlign(Paint.Align.RIGHT);
        pEvTxt.setTextSize(sp(10));
        pEvTxt.setColor(0xFFFFFFFF);
        pNow.setStrokeWidth(dp(1.5f));
        pNow.setColor(colNow);
        setDay(System.currentTimeMillis());
    }

    private float dp(float v) { return v * density; }

    private float sp(float v) { return v * scaled; }

    public void setOnEventClick(OnEventClick l) { listener = l; }

    public void setDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Store.dayStart(millis));
        int idx = c.get(Calendar.DAY_OF_WEEK) - 1; // 周日为 0
        selIndex = idx;
        c.add(Calendar.DAY_OF_MONTH, -idx);
        weekStart = Store.dayStart(c.getTimeInMillis());
        invalidate();
    }

    private String hm(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return (c.get(Calendar.HOUR_OF_DAY) < 10 ? "0" : "") + c.get(Calendar.HOUR_OF_DAY)
                + ":" + (c.get(Calendar.MINUTE) < 10 ? "0" : "") + c.get(Calendar.MINUTE);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, (int) (rowH * 24 + dp(2)));
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth();
        if (w == 0) return;
        float cw = (w - gutter) / 7f;
        float hgt = getHeight();

        pCol.setColor(colSel);
        cv.drawRect(gutter + selIndex * cw, 0, gutter + (selIndex + 1) * cw, hgt, pCol);

        for (int h = 0; h <= 24; h++) {
            float y = h * rowH;
            pLine.setColor(colLine);
            cv.drawLine(gutter, y, w, y, pLine);
            if (h < 24) {
                pTxt.setColor(colGutter);
                cv.drawText((h < 10 ? "0" : "") + h + ":00", gutter - dp(7), y + dp(11), pTxt);
            }
        }
        for (int i = 0; i <= 7; i++) {
            cv.drawLine(gutter + i * cw, 0, gutter + i * cw, hgt, pLine);
        }

        rects.clear();
        ids.clear();
        for (int i = 0; i < 7; i++) {
            long d = weekStart + i * Store.DAY;
            List<Event> es = store.onDay(d);
            for (int k = 0; k < es.size(); k++) drawEvent(cv, es.get(k), i, cw);
        }

        long today = Store.dayStart(System.currentTimeMillis());
        for (int i = 0; i < 7; i++) {
            if (weekStart + i * Store.DAY == today) {
                Calendar n = Calendar.getInstance();
                float m = n.get(Calendar.HOUR_OF_DAY) * 60 + n.get(Calendar.MINUTE);
                float y = m / 60f * rowH;
                pNow.setColor(colNow);
                cv.drawLine(gutter, y, w, y, pNow);
                cv.drawCircle(gutter, y, dp(3), pNow);
                break;
            }
        }
    }

    private void drawEvent(Canvas cv, Event e, int i, float cw) {
        float x0 = gutter + i * cw + dp(2);
        float x1 = gutter + (i + 1) * cw - dp(2);
        float y0, y1;
        if (e.allDay) {
            y0 = dp(0.5f);
            y1 = rowH - dp(2);
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(e.start);
            float m0 = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            c.setTimeInMillis(e.end);
            float m1 = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            if (m1 <= m0) m1 = m0 + 30;
            y0 = m0 / 60f * rowH + dp(1);
            y1 = m1 / 60f * rowH - dp(1);
        }
        if (y1 - y0 < dp(19)) y1 = y0 + dp(19);

        pEv.setColor(store != null ? store.colorOf(e) : e.color);
        pEv.setAlpha(235);
        rf.set(x0, y0, x1, y1);
        cv.drawRoundRect(rf, dp(5), dp(5), pEv);

        int sc = cv.save();
        cv.clipRect(x0 + dp(2), y0, x1, y1);
        pEvTxt.setColor(0xFFFFFFFF);
        pEvTxt.setTextSize(sp(10));
        cv.drawText(e.title == null ? "" : e.title, x0 + dp(4), y0 + dp(12), pEvTxt);
        if (y1 - y0 > dp(30)) {
            pEvTxt.setColor(0xCCFFFFFF);
            pEvTxt.setTextSize(sp(9));
            cv.drawText(e.allDay ? "全天" : hm(e.start), x0 + dp(4), y0 + dp(24), pEvTxt);
        }
        cv.restoreToCount(sc);

        rects.add(new RectF(x0, y0, x1, y1));
        ids.add(e.id);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                moved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(ev.getY() - downY) > dp(10) || Math.abs(ev.getX() - downX) > dp(10))
                    moved = true;
                return true;
            case MotionEvent.ACTION_UP:
                if (!moved) {
                    for (int i = 0; i < rects.size(); i++) {
                        if (rects.get(i).contains(ev.getX(), ev.getY())) {
                            if (listener != null) listener.onClick(ids.get(i));
                            break;
                        }
                    }
                }
                return true;
        }
        return super.onTouchEvent(ev);
    }
}

