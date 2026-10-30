package com.moshang.schedule;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/** 「视图 ▼」小下拉：点按钮在下方弹出年/月/周/日/日程，再点收起 */
public class ViewMenu {

    public interface OnPick { void onPick(int index); }

    private final Context ctx;
    private final float d;
    private PopupWindow pw;

    public ViewMenu(Context c) {
        ctx = c;
        d = c.getResources().getDisplayMetrics().density;
    }

    private int dp(float v) { return Math.round(v * d); }

    public boolean isShowing() { return pw != null && pw.isShowing(); }

    public void dismiss() { if (pw != null && pw.isShowing()) pw.dismiss(); }

    public void toggle(View anchor, String[] names, int current, OnPick cb) {
        if (isShowing()) { dismiss(); return; }
        show(anchor, names, current, cb);
    }

    public void show(View anchor, String[] names, int current, final OnPick cb) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(6), dp(8), dp(6), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ctx.getResources().getColor(R.color.window_bg));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), ctx.getResources().getColor(R.color.divider));
        card.setBackground(bg);

        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            boolean on = i == current;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(11), dp(10), dp(11));
            row.setBackgroundResource(R.drawable.ripple_card);

            TextView t = new TextView(ctx);
            t.setText(names[i]);
            t.setTextSize(15);
            t.setTextColor(on ? Theme.brand(ctx) : ctx.getResources().getColor(R.color.text_primary));
            if (on) t.setTypeface(null, Typeface.BOLD);
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            if (on) {
                ImageView ck = new ImageView(ctx);
                ck.setImageResource(R.drawable.ic_check);
                LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(dp(16), dp(16));
                klp.leftMargin = dp(10);
                row.addView(ck, klp);
            }
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dismiss();
                    if (cb != null) cb.onPick(idx);
                }
            });
            card.addView(row);
        }

        pw = new PopupWindow(card, dp(136), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setElevation(dp(10));
        pw.showAsDropDown(anchor, 0, dp(6));
    }
}

