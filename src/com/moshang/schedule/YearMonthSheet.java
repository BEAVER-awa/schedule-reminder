package com.moshang.schedule;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

/** 年月选择弹层（两个滚轮），点顶部标题打开 */
public class YearMonthSheet extends Sheet {

    public interface OnPick { void onPick(int year, int month0); }

    public YearMonthSheet(Context c, int year, int month0, final OnPick cb) {
        super(c, null);
        fromTop = true;
        topOffset = Math.round(50 * c.getResources().getDisplayMetrics().density);
        scrimColor = 0x52000000;

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(28), 0, dp(28), dp(4));

        final WheelView wy = new WheelView(c);
        String[] ys = new String[201];
        for (int i = 0; i < 201; i++) ys[i] = (1900 + i) + "年";
        wy.setItems(ys);
        wy.setSelected(year - 1900, false);
        wy.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.35f));

        final WheelView wm = new WheelView(c);
        String[] ms = new String[12];
        for (int i = 0; i < 12; i++) ms[i] = (i + 1) + "月";
        wm.setItems(ms);
        wm.setSelected(month0, false);
        wm.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(wy);
        row.addView(wm);
        body.addView(row);

        body.addView(TimeSheet.okRow(c, new View.OnClickListener() {
            public void onClick(View v) {
                final int y = 1900 + wy.getSelected();
                final int m = wm.getSelected();
                closeAnimated(new Runnable() {
                    public void run() { if (cb != null) cb.onPick(y, m); }
                });
            }
        }));
    }
}

