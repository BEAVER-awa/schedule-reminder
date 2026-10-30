package com.moshang.schedule;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 单选弹层：标题 + 选项列表（右侧打勾），替代原生 Spinner / AlertDialog.setItems */
public class OptionSheet extends Sheet {

    public interface OnPick { void onPick(int index); }

    private final String[] items;
    private final int selected;
    private final OnPick cb;

    public OptionSheet(Context c, String title, String[] items, int selected, OnPick cb) {
        super(c, title);
        this.items = items;
        this.selected = selected;
        this.cb = cb;
        build();
    }

    private void build() {
        for (int i = 0; i < items.length; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(20), dp(15), dp(20), dp(15));
            row.setBackgroundResource(R.drawable.bg_option);

            TextView t = new TextView(ctx);
            t.setText(items[i]);
            t.setTextSize(15);
            t.setTextColor(ctx.getResources().getColor(R.color.text_primary));
            row.addView(t);

            View sp = new View(ctx);
            sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            row.addView(sp);

            if (idx == selected) {
                ImageView ck = new ImageView(ctx);
                ck.setImageResource(R.drawable.ic_check);
                ck.setLayoutParams(new LinearLayout.LayoutParams(dp(19), dp(19)));
                row.addView(ck);
            }

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    closeAnimated(new Runnable() {
                        public void run() { if (cb != null) cb.onPick(idx); }
                    });
                }
            });
            body.addView(row);
        }
        View pad = new View(ctx);
        pad.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10)));
        body.addView(pad);
    }
}

