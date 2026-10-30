package com.moshang.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 时间选择弹层（自绘滚轮，替代原生 TimePickerDialog） */
public class TimeSheet extends Sheet {

    public interface OnTime { void onTime(int hour, int minute); }

    public TimeSheet(Context c, int hour, int minute, final OnTime cb) {
        super(c, c.getString(R.string.set_time));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(28), 0, dp(28), dp(4));

        final WheelView wh = new WheelView(c);
        String[] hs = new String[24];
        for (int i = 0; i < 24; i++) hs[i] = (i < 10 ? "0" : "") + i;
        wh.setItems(hs);
        wh.setSelected(hour, false);
        wh.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView colon = new TextView(c);
        colon.setText(":");
        colon.setTextSize(20);
        colon.setTypeface(null, Typeface.BOLD);
        colon.setTextColor(c.getResources().getColor(R.color.text_primary));
        colon.setPadding(dp(4), 0, dp(4), 0);

        final WheelView wm = new WheelView(c);
        String[] ms = new String[60];
        for (int i = 0; i < 60; i++) ms[i] = (i < 10 ? "0" : "") + i;
        wm.setItems(ms);
        wm.setSelected(minute, false);
        wm.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(wh);
        row.addView(colon);
        row.addView(wm);
        body.addView(row);

        body.addView(okRow(c, new View.OnClickListener() {
            public void onClick(View v) {
                final int hh = wh.getSelected();
                final int mm = wm.getSelected();
                closeAnimated(new Runnable() {
                    public void run() { if (cb != null) cb.onTime(hh, mm); }
                });
            }
        }));
    }

    static LinearLayout okRow(Context c, View.OnClickListener l) {
        float d = c.getResources().getDisplayMetrics().density;
        LinearLayout wrap = new LinearLayout(c);
        wrap.setPadding((int) (16 * d), (int) (8 * d), (int) (16 * d), (int) (18 * d));
        TextView ok = new TextView(c);
        ok.setText(c.getString(R.string.ok));
        ok.setTextSize(15);
        ok.setTypeface(null, Typeface.BOLD);
        ok.setGravity(Gravity.CENTER);
        ok.setTextColor(0xFFFFFFFF);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Theme.brand(c));
        g.setCornerRadius(12 * d);
        ok.setBackground(g);
        ok.setPadding(0, (int) (13 * d), 0, (int) (13 * d));
        ok.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        ok.setOnClickListener(l);
        wrap.addView(ok);
        return wrap;
    }
}

