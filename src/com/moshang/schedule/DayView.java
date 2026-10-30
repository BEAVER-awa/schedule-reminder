package com.moshang.schedule;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 日视图：24 小时时间轴 + 事件块 */
public class DayView extends View {

    public interface OnEventClick { void onClick(String id); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBlock = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private float density;
    private float scaled;
    private float hourH;
    private float labelW;
    private List<Event> events = new ArrayList<Event>();
    private int lineColor, labelColor, textColor;
    private OnEventClick click;

    public DayView(Context c) { this(c, null); }

    public DayView(Context c, AttributeSet a) {
        super(c, a);
        density = getResources().getDisplayMetrics().density;
        scaled = getResources().getDisplayMetrics().scaledDensity;
        hourH = dp(56);
        labelW = dp(48);
        lineColor = getResources().getColor(R.color.grid_line);
        labelColor = getResources().getColor(R.color.text_tertiary);
        textColor = getResources().getColor(R.color.text_primary);
        pLine.setColor(lineColor);
        pLine.setStrokeWidth(dp(1));
        p.setTextAlign(Paint.Align.LEFT);
        pBlock.setStyle(Paint.Style.FILL);
    }

    private float dp(float v) { return v * density; }

    private float sp(float v) { return v * scaled; }

    public void setEvents(List<Event> e) { events = e; invalidate(); }

    public void setOnEventClick(OnEventClick l) { click = l; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = (int) (24 * hourH) + (int) dp(8);
        setMeasuredDimension(w, h);
    }

    private float[] block(Event e) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(e.start);
        float top = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) / 60f * hourH + dp(4);
        c.setTimeInMillis(e.end);
        float bot = (c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) / 60f * hourH + dp(4);
        if (e.allDay) { top = dp(4); bot = dp(4) + hourH * 0.8f; }
        if (bot - top < dp(22)) bot = top + dp(22);
        return new float[]{top, bot};
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth();
        p.setTextSize(sp(10));
        for (int h = 0; h <= 23; h++) {
            float y = h * hourH + dp(4);
            cv.drawLine(labelW, y, w, y, pLine);
            p.setColor(labelColor);
            String t = (h < 10 ? "0" + h : "" + h) + ":00";
            cv.drawText(t, dp(8), y - dp(2), p);
        }
        float y24 = 24 * hourH + dp(4);
        cv.drawLine(labelW, y24, w, y24, pLine);

        for (Event e : events) {
            float[] b = block(e);
            float top = b[0], bot = b[1];
            float left = labelW + dp(6);
            float right = w - dp(10);

            pBlock.setColor((e.color & 0x00FFFFFF) | 0x33000000);
            rf.set(left, top, right, bot);
            cv.drawRoundRect(rf, dp(6), dp(6), pBlock);

            pBlock.setColor(e.color);
            rf.set(left, top, left + dp(3), bot);
            cv.drawRoundRect(rf, dp(1.5f), dp(1.5f), pBlock);

            p.setTextSize(sp(12));
            p.setColor(textColor);
            String ttl = e.title;
            if (!e.allDay) ttl = hm.format(new Date(e.start)) + "  " + ttl;
            cv.drawText(ttl, left + dp(10), top + dp(15), p);

            if (bot - top > dp(40)) {
                p.setTextSize(sp(10));
                p.setColor(labelColor);
                String sub = e.allDay ? "全天" : (hm.format(new Date(e.start)) + " - " + hm.format(new Date(e.end)));
                if (e.location != null && e.location.length() > 0) sub = sub + "   ·   " + e.location;
                cv.drawText(sub, left + dp(10), top + dp(31), p);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
            float y = ev.getY();
            for (Event e : events) {
                float[] b = block(e);
                if (y >= b[0] && y <= b[1]) {
                    if (click != null) click.onClick(e.id);
                    return true;
                }
            }
        }
        return true;
    }
}

