package com.moshang.schedule;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 分类管理页（全自绘） */
public class CategoryActivity extends Activity {

    private Store store;
    private float density;
    private LinearLayout list;

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
        bar.setPadding(dp(8), 0, dp(16), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        TextView t = new TextView(this);
        t.setText("分类管理");
        t.setTextSize(18);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(6);
        t.setLayoutParams(tlp);
        View sp = new View(this);
        sp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        TextView sortBtn = new TextView(this);
        sortBtn.setText("排序");
        sortBtn.setTextSize(13);
        sortBtn.setGravity(Gravity.CENTER);
        sortBtn.setTextColor(Theme.brand(this));
        sortBtn.setPadding(dp(10), dp(8), dp(10), dp(8));
        sortBtn.setContentDescription("调整分类顺序");
        sortBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(CategoryActivity.this, CategoryOrderActivity.class));
                overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out);
            }
        });
        Press.attach(sortBtn);

        TextView add = new TextView(this);
        add.setText("＋");
        add.setTextSize(22);
        add.setGravity(Gravity.CENTER);
        add.setTextColor(Theme.brand(this));
        add.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openSheet(null, Store.PALETTE[0], true); }
        });
        Press.attach(add);
        bar.addView(back);
        bar.addView(t);
        bar.addView(sp);
        bar.addView(sortBtn);
        bar.addView(add);
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(8), dp(16), dp(24));
        sv.addView(list);
        root.addView(sv);
        rebuild();
        return root;
    }

    private void rebuild() {
        list.removeAllViews();
        TextView tip = new TextView(this);
        tip.setText("点分类可改名/换色/删除；右上「排序」可拖动调整顺序。删除分类后，原日程会移到第一个分类。");
        tip.setTextSize(12);
        tip.setTextColor(c(R.color.text_secondary));
        tip.setPadding(dp(2), dp(2), dp(2), dp(12));
        list.addView(tip);

        for (int i = 0; i < store.cats.size(); i++) {
            final Category cat = store.cats.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_field);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rlp.bottomMargin = dp(10);
            row.setLayoutParams(rlp);

            View dot = new View(this);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(14), dp(14));
            dot.setLayoutParams(dlp);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(cat.color);
            dot.setBackground(g);
            row.addView(dot);

            TextView nm = new TextView(this);
            nm.setText(cat.name);
            nm.setTextSize(15);
            nm.setTextColor(c(R.color.text_primary));
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nlp.leftMargin = dp(12);
            nm.setLayoutParams(nlp);
            row.addView(nm);

            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
            row.addView(spacer);

            TextView cnt = new TextView(this);
            cnt.setText(store.catCount(cat.name) + " 条");
            cnt.setTextSize(12);
            cnt.setTextColor(c(R.color.text_secondary));
            row.addView(cnt);

            ImageView chev = new ImageView(this);
            chev.setImageResource(R.drawable.ic_next);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(16), dp(16));
            clp.leftMargin = dp(8);
            chev.setLayoutParams(clp);
            chev.setAlpha(0.3f);
            row.addView(chev);

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openSheet(cat.name, cat.color, false); }
            });
            Press.attach(row);
            list.addView(row);
        }
    }

    private void openSheet(String name, int color, boolean isNew) {
        new CategorySheet(this, isNew ? "新建分类" : "编辑分类", name, color, isNew,
                new CategorySheet.OnDone() {
                    public void onDone() { rebuild(); }
                }).showSheet();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}

