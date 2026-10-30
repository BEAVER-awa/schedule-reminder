package com.moshang.schedule;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 搜索日程（全自绘） */
public class SearchActivity extends Activity {

    private Store store;
    private float density;
    private EditText q;
    private LinearLayout results;
    private final SimpleDateFormat full = new SimpleDateFormat("yyyy年M月d日 EEEE HH:mm", Locale.CHINA);
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(build());
    }

    private int dp(float v) { return Math.round(v * density); }

    private int c(int id) { return getResources().getColor(id); }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(c(R.color.window_bg));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(12), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        bar.addView(back);

        q = new EditText(this);
        q.setHint("搜索日程 / 地点 / 备注 / 分类");
        q.setTextSize(15);
        q.setSingleLine(true);
        q.setTextColor(c(R.color.text_primary));
        q.setHintTextColor(c(R.color.text_tertiary));
        q.setBackgroundResource(R.drawable.bg_field);
        q.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        qlp.leftMargin = dp(4);
        q.setLayoutParams(qlp);
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int cc) {}
            public void onTextChanged(CharSequence s, int a, int b, int cc) {}
            public void afterTextChanged(Editable s) { search(); }
        });
        bar.addView(q);

        TextView clear = new TextView(this);
        clear.setText("取消");
        clear.setTextSize(14);
        clear.setTextColor(Theme.brand(this));
        clear.setPadding(dp(10), dp(8), dp(4), dp(8));
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { q.setText(""); }
        });
        Press.attach(clear);
        bar.addView(clear);
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(dp(16), dp(6), dp(16), dp(24));
        sv.addView(results);
        root.addView(sv);

        search();
        q.requestFocus();
        return root;
    }

    private String subOf(Event e) {
        StringBuilder sb = new StringBuilder();
        Calendar c1 = Calendar.getInstance();
        c1.setTimeInMillis(e.start);
        if (e.allDay) sb.append(full.format(new Date(e.start)));
        else {
            sb.append(full.format(new Date(e.start))).append(" - ").append(hm.format(new Date(e.end)));
        }
        if (e.repeat != null && !"none".equals(e.repeat)) {
            if ("dates".equals(e.repeat)) sb.append("   · 指定日期 ").append(e.dates.size()).append(" 次");
            else if ("daily".equals(e.repeat)) sb.append("   · 每天");
            else if ("weekly".equals(e.repeat)) sb.append("   · 每周");
            else if ("monthly".equals(e.repeat)) sb.append("   · 每月");
        }
        if (e.location != null && e.location.length() > 0) sb.append("   · ").append(e.location);
        return sb.toString();
    }

    private void search() {
        results.removeAllViews();
        String key = q.getText().toString().trim().toLowerCase(Locale.getDefault());

        TextView tip = new TextView(this);
        tip.setTextSize(12);
        tip.setTextColor(c(R.color.text_secondary));
        tip.setPadding(dp(2), dp(6), dp(2), dp(10));
        results.addView(tip);

        if (key.length() == 0) {
            tip.setText("输入关键词开始搜索（共 " + store.events.size() + " 条日程）");
            return;
        }

        int n = 0;
        for (int i = 0; i < store.events.size(); i++) {
            final Event e = store.events.get(i);
            String hay = (e.title + " " + e.location + " " + e.note + " " + e.category).toLowerCase(Locale.getDefault());
            if (!hay.contains(key)) continue;
            n++;
            results.addView(row(e));
        }
        tip.setText(n == 0 ? "没有找到匹配的日程" : ("找到 " + n + " 条"));
    }

    private View row(final Event e) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.HORIZONTAL);
        outer.setBackgroundResource(R.drawable.ripple_card);
        outer.setPadding(dp(14), dp(14), dp(14), dp(14));
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        olp.bottomMargin = dp(10);
        outer.setLayoutParams(olp);

        View bar = new View(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(3), LinearLayout.LayoutParams.MATCH_PARENT);
        bar.setLayoutParams(blp);
        GradientDrawable g = new GradientDrawable();
        g.setColor(e.color);
        g.setCornerRadius(dp(2));
        bar.setBackground(g);
        outer.addView(bar);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = dp(12);
        col.setLayoutParams(clp);

        TextView t = new TextView(this);
        t.setText(e.title);
        t.setTextSize(15);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(c(R.color.text_primary));
        t.setMaxLines(2);
        col.addView(t);

        TextView s = new TextView(this);
        s.setText(subOf(e));
        s.setTextSize(12);
        s.setTextColor(c(R.color.text_secondary));
        s.setPadding(0, dp(4), 0, 0);
        col.addView(s);

        outer.addView(col);

        outer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(SearchActivity.this, EventEditActivity.class);
                i.putExtra("id", e.id);
                startActivity(i);
                overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out);
            }
        });
        Press.attach(outer);
        return outer;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}

