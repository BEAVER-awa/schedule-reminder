package com.moshang.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 新增 / 编辑分类弹层：名称 + 调色板 + 保存/删除 */
public class CategorySheet extends Sheet {

    public interface OnDone { void onDone(); }

    private final String origName;
    private final OnDone done;
    private final EditText nameEt;
    private final LinearLayout palette;
    private int color;

    public CategorySheet(final Context c, String title, String name, int color0,
                         final boolean isNew, final OnDone done) {
        super(c, title);
        this.origName = name;
        this.done = done;
        this.color = color0;

        nameEt = new EditText(c);
        nameEt.setHint("分类名称");
        nameEt.setTextSize(16);
        nameEt.setSingleLine(true);
        nameEt.setInputType(InputType.TYPE_CLASS_TEXT);
        nameEt.setTextColor(c.getResources().getColor(R.color.text_primary));
        nameEt.setHintTextColor(c.getResources().getColor(R.color.text_tertiary));
        nameEt.setBackgroundResource(R.drawable.bg_field);
        nameEt.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (name != null) nameEt.setText(name);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.leftMargin = dp(16);
        nlp.rightMargin = dp(16);
        nameEt.setLayoutParams(nlp);
        body.addView(nameEt);

        TextView lab = new TextView(c);
        lab.setText("颜色");
        lab.setTextSize(12);
        lab.setTextColor(c.getResources().getColor(R.color.text_secondary));
        lab.setPadding(dp(18), dp(16), dp(18), dp(8));
        body.addView(lab);

        palette = new LinearLayout(c);
        palette.setOrientation(LinearLayout.HORIZONTAL);
        palette.setPadding(dp(16), 0, dp(16), dp(4));
        buildPalette();
        body.addView(palette);

        LinearLayout btns = new LinearLayout(c);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(dp(16), dp(14), dp(16), dp(18));

        if (!isNew) {
            TextView del = new TextView(c);
            del.setText("删除");
            del.setTextSize(15);
            del.setGravity(Gravity.CENTER);
            del.setTextColor(c.getResources().getColor(R.color.danger));
            del.setBackgroundResource(R.drawable.bg_field);
            del.setPadding(0, dp(13), 0, dp(13));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dlp.rightMargin = dp(12);
            del.setLayoutParams(dlp);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    closeAnimated(new Runnable() {
                        public void run() {
                            new ConfirmSheet(c, "删除分类",
                                    "删除「" + origName + "」后，该分类下的日程会移到第一个分类。",
                                    "删除", true, new ConfirmSheet.OnOk() {
                                public void onOk() {
                                    Store.get(c).deleteCat(origName);
                                    if (done != null) done.onDone();
                                }
                            }).showSheet();
                        }
                    });
                }
            });
            btns.addView(del);
        }

        TextView save = new TextView(c);
        save.setText("保存");
        save.setTextSize(15);
        save.setTypeface(null, Typeface.BOLD);
        save.setGravity(Gravity.CENTER);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundResource(R.drawable.bg_btn_brand);
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(ctx)));
        save.setPadding(0, dp(13), 0, dp(13));
        save.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String nm = nameEt.getText().toString().trim();
                if (nm.length() == 0) { nameEt.setError("请填写名称"); return; }
                Store st = Store.get(c);
                if (isNew) st.addCat(nm, color);
                else st.renameCat(origName, nm, color);
                closeAnimated(new Runnable() {
                    public void run() { if (done != null) done.onDone(); }
                });
            }
        });
        btns.addView(save);
        body.addView(btns);
    }

    private void buildPalette() {
        palette.removeAllViews();
        for (int i = 0; i < Store.PALETTE.length; i++) {
            final int col = Store.PALETTE[i];
            View sw = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
            lp.rightMargin = dp(8);
            sw.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(col);
            if (col == color) g.setStroke(dp(3), 0x66000000);
            sw.setBackground(g);
            sw.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    color = col;
                    buildPalette();
                }
            });
            palette.addView(sw);
        }
    }
}

