package com.moshang.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

/** 日期选择弹层（复用自绘月历，替代原生 DatePickerDialog） */
public class DateSheet extends Sheet {

    public interface OnDate { void onDate(int y, int m, int d); }

    private int y, m, d;
    private final TextView label;
    private final MonthView mv;

    public DateSheet(Context c, long millis, final OnDate cb) {
        super(c, c.getString(R.string.set_date));
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        y = cal.get(Calendar.YEAR);
        m = cal.get(Calendar.MONTH);
        d = cal.get(Calendar.DAY_OF_MONTH);

        label = new TextView(c);
        label.setTextSize(14);
        label.setTextColor(c.getResources().getColor(R.color.text_secondary));
        label.setPadding(dp(22), 0, dp(22), dp(6));
        body.addView(label);

        mv = new MonthView(c);
        mv.setMonth(y, m);
        mv.setSelected(y, m, d);
        mv.setDayClick(new MonthView.OnDayClick() {
            public void onDay(int yy, int mm, int dd) {
                y = yy; m = mm; d = dd;
                mv.setSelected(yy, mm, dd);
                updateLabel();
            }
        });
        body.addView(mv);

        updateLabel();

        LinearLayout btns = new LinearLayout(c);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(dp(16), dp(10), dp(16), dp(18));

        TextView today = new TextView(c);
        today.setText(c.getString(R.string.today));
        today.setTextSize(15);
        today.setGravity(Gravity.CENTER);
        today.setTextColor(c.getResources().getColor(R.color.text_primary));
        today.setBackgroundResource(R.drawable.bg_field);
        today.setPadding(0, dp(13), 0, dp(13));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.rightMargin = dp(12);
        today.setLayoutParams(tlp);
        today.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Calendar n = Calendar.getInstance();
                y = n.get(Calendar.YEAR);
                m = n.get(Calendar.MONTH);
                d = n.get(Calendar.DAY_OF_MONTH);
                mv.setMonth(y, m);
                mv.setSelected(y, m, d);
                updateLabel();
            }
        });
        btns.addView(today);

        TextView ok = new TextView(c);
        ok.setText(c.getString(R.string.ok));
        ok.setTextSize(15);
        ok.setTypeface(null, Typeface.BOLD);
        ok.setGravity(Gravity.CENTER);
        ok.setTextColor(0xFFFFFFFF);
        ok.setBackgroundResource(R.drawable.bg_btn_brand);
        ok.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(ctx)));
        ok.setPadding(0, dp(13), 0, dp(13));
        ok.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeAnimated(new Runnable() {
                    public void run() { if (cb != null) cb.onDate(y, m, d); }
                });
            }
        });
        btns.addView(ok);

        body.addView(btns);
    }

    private void updateLabel() {
        label.setText(y + "\u5e74" + (m + 1) + "\u6708" + d + "\u65e5");
    }
}

