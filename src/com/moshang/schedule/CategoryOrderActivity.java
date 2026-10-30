package com.moshang.schedule;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** 分类排序页：按住右侧手柄上下拖动，实时换位，松手保存 */
public class CategoryOrderActivity extends Activity {

    private Store store;
    private float density;
    private LinearLayout list;
    private android.widget.ScrollView scrollParent;
    private int rowH;

    private View dragView = null;
    private int dragIndex = -1;
    private int targetIndex = -1;
    private float grabDy = 0f;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        density = getResources().getDisplayMetrics().density;
        rowH = dp(56);
        setContentView(build());
    }

    private int dp(float v) { return Math.round(v * density); }

    private int c(int id) { return getResources().getColor(id); }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(c(R.color.window_bg));

        // ---- 顶栏
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(16), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        TextView title = new TextView(this);
        title.setText("分类排序");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(6);
        title.setLayoutParams(tlp);

        bar.addView(back);
        bar.addView(title);
        root.addView(bar);

        // ---- 说明
        TextView tip = new TextView(this);
        tip.setText("按住右侧的 ≡ 手柄上下拖动，松手即保存。顺序会影响新建 / 编辑行程时的分类排列。");
        tip.setTextSize(12);
        tip.setTextColor(c(R.color.text_secondary));
        tip.setLineSpacing(dp(3), 1f);
        tip.setPadding(dp(18), dp(2), dp(18), dp(12));
        root.addView(tip);

        // ---- 列表
        scrollParent = new android.widget.ScrollView(this);
        android.widget.ScrollView sv = scrollParent;
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        sv.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), 0, dp(16), dp(16));
        list.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        list.setClipChildren(false);
        list.setClipToPadding(false);
        sv.setClipChildren(false);
        sv.setClipToPadding(false);
        sv.addView(list);
        root.addView(sv);
        root.setClipChildren(false);

        rebuild();
        return root;
    }

    private void rebuild() {
        list.removeAllViews();
        for (int i = 0; i < store.cats.size(); i++) {
            list.addView(buildRow(store.cats.get(i), i));
        }

    }

    private View buildRow(final Category cat, int index) {
        final LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH));
        row.setTag(cat.name);
        row.setContentDescription("第 " + (index + 1) + " 项，" + cat.name);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_field);
        card.setPadding(dp(14), 0, dp(6), 0);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, rowH - dp(8)));

        View dot = new View(this);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dp(12), dp(12)));
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(cat.color);
        dot.setBackground(g);

        TextView nm = new TextView(this);
        nm.setText(cat.name);
        nm.setTextSize(15);
        nm.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.leftMargin = dp(12);
        nm.setLayoutParams(nlp);

        TextView cnt = new TextView(this);
        cnt.setText(store.catCount(cat.name) + " 条");
        cnt.setTextSize(12);
        cnt.setTextColor(c(R.color.text_secondary));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = dp(10);
        cnt.setLayoutParams(clp);

        ImageView handle = new ImageView(this);
        handle.setImageResource(R.drawable.ic_drag);
        handle.setScaleType(ImageView.ScaleType.CENTER);
        handle.setLayoutParams(new LinearLayout.LayoutParams(dp(44), rowH - dp(8)));
        handle.setContentDescription("拖动 " + cat.name + " 调整顺序");

        card.addView(dot);
        card.addView(nm);
        card.addView(cnt);
        card.addView(handle);
        row.addView(card);

        handle.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                return onHandleTouch(row, e);
            }
        });
        return row;
    }

    /** 手柄拖动：按下即开始；只移动被拖行与其它行的位移，不删挂任何 View（避免触摸流被打断） */
    private boolean onHandleTouch(View row, MotionEvent e) {
        int[] loc = new int[2];
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                list.getLocationOnScreen(loc);
                dragIndex = list.indexOfChild(row);
                if (dragIndex < 0) return false;
                grabDy = e.getRawY() - (loc[1] + dragIndex * rowH);
                targetIndex = dragIndex;
                dragView = row;
                list.requestDisallowInterceptTouchEvent(true);
                row.setElevation(dp(10));
                row.setAlpha(0.96f);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (dragView != row) return false;
                list.getLocationOnScreen(loc);
                float top = (e.getRawY() - loc[1]) - grabDy;
                float center = top + rowH / 2f;
                int t = (int) (center / (float) rowH);
                int count = list.getChildCount();
                if (t < 0) t = 0;
                if (t > count - 1) t = count - 1;
                if (t != targetIndex) {
                    targetIndex = t;
                    layoutOthers();
                }
                row.setTranslationY(top - dragIndex * rowH);
                autoScroll(e.getRawY());
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (dragView != row) return false;
                finishDrag();
                return true;
            }
        }
        return false;
    }

    /** 手指贴近上下边缘时带动列表滚动，方便长列表长距离拖动 */
    private void autoScroll(float rawY) {
        if (scrollParent == null) return;
        int[] sl = new int[2];
        scrollParent.getLocationOnScreen(sl);
        float topEdge = sl[1] + dp(56);
        float bottomEdge = sl[1] + scrollParent.getHeight() - dp(56);
        if (rawY < topEdge) scrollParent.scrollBy(0, -dp(10));
        else if (rawY > bottomEdge) scrollParent.scrollBy(0, dp(10));
    }

    /** 其它行按「让位」规则上下平移，实时反映落点 */
    private void layoutOthers() {
        for (int i = 0; i < list.getChildCount(); i++) {
            View c = list.getChildAt(i);
            if (c == dragView) continue;
            float shift = 0f;
            if (dragIndex < targetIndex && i > dragIndex && i <= targetIndex) shift = -rowH;
            else if (dragIndex > targetIndex && i >= targetIndex && i < dragIndex) shift = rowH;
            c.animate().translationY(shift).setDuration(130).start();
        }
    }

    /** 松手：落位重排 + 清零所有位移 + 保存 */
    private void finishDrag() {
        View row = dragView;
        dragView = null;
        row.setElevation(0);
        row.setAlpha(1f);

        if (targetIndex != dragIndex) {
            list.removeView(row);
            int at = Math.min(targetIndex, list.getChildCount());
            list.addView(row, at);
        }
        for (int i = 0; i < list.getChildCount(); i++) {
            list.getChildAt(i).animate().cancel();
            list.getChildAt(i).setTranslationY(0f);
        }
        targetIndex = -1;
        dragIndex = -1;
        commit();
    }

    /** 把当前视觉顺序写回存储 */
    private void commit() {
        List<Category> ordered = new ArrayList<Category>();
        for (int i = 0; i < list.getChildCount(); i++) {
            View row = list.getChildAt(i);
            Object tag = row.getTag();
            if (tag == null) continue;
            Category cc = store.cat((String) tag);
            if (cc != null) ordered.add(cc);
        }
        if (ordered.size() != store.cats.size()) return;
        boolean changed = false;
        for (int i = 0; i < ordered.size(); i++) {
            if (store.cats.get(i) != ordered.get(i)) { changed = true; break; }
        }
        if (!changed) return;
        store.cats.clear();
        store.cats.addAll(ordered);
        store.save();
        updateA11yIndex();
        Toast.makeText(this, "分类顺序已保存", Toast.LENGTH_SHORT).show();
    }

    /** 拖动后同步每行的「第 N 项」播报，保证读屏顺序与视觉一致 */
    private void updateA11yIndex() {
        int total = list.getChildCount();
        for (int i = 0; i < total; i++) {
            View row = list.getChildAt(i);
            Object tag = row.getTag();
            if (tag == null) continue;
            row.setContentDescription("第 " + (i + 1) + " 项，" + tag);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}
