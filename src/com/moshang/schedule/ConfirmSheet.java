package com.moshang.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 确认弹层（替代原生 AlertDialog） */
public class ConfirmSheet extends Sheet {

    public interface OnOk { void onOk(); }

    public ConfirmSheet(Context c, String title, String message, String okText,
                        boolean danger, final OnOk ok) {
        super(c, title);

        TextView msg = new TextView(c);
        msg.setText(message);
        msg.setTextSize(14);
        msg.setTextColor(c.getResources().getColor(R.color.text_secondary));
        msg.setPadding(dp(22), dp(2), dp(22), dp(20));
        body.addView(msg);

        LinearLayout btns = new LinearLayout(c);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(dp(16), 0, dp(16), dp(18));

        TextView cancel = new TextView(c);
        cancel.setText(c.getString(R.string.cancel));
        cancel.setTextSize(15);
        cancel.setGravity(Gravity.CENTER);
        cancel.setTextColor(c.getResources().getColor(R.color.text_primary));
        cancel.setBackgroundResource(R.drawable.bg_field);
        cancel.setPadding(0, dp(13), 0, dp(13));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.rightMargin = dp(12);
        cancel.setLayoutParams(clp);
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closeAnimated(); }
        });
        btns.addView(cancel);

        TextView confirm = new TextView(c);
        confirm.setText(okText);
        confirm.setTextSize(15);
        confirm.setTypeface(null, Typeface.BOLD);
        confirm.setGravity(Gravity.CENTER);
        confirm.setTextColor(0xFFFFFFFF);
        GradientDrawable g = new GradientDrawable();
        g.setColor(danger ? c.getResources().getColor(R.color.danger)
                : Theme.brand(c));
        g.setCornerRadius(dp(12));
        confirm.setBackground(g);
        confirm.setPadding(0, dp(13), 0, dp(13));
        confirm.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        confirm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeAnimated(new Runnable() {
                    public void run() { if (ok != null) ok.onOk(); }
                });
            }
        });
        btns.addView(confirm);

        body.addView(btns);
    }
}

