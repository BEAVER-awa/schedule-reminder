package com.moshang.schedule;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 自定义颜色弹层：色相 / 饱和度 / 明度 三条滑条 + 实时预览。
 *  默认色板里没有想要的颜色时用这个，取任意 RGB。 */
public class ColorPickerSheet extends Sheet {

    public interface OnPick { void onPick(int color); }

    private static final int[] HUE_STOPS = {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF,
            0xFF0000FF, 0xFFFF00FF, 0xFFFF0000
    };

    private float h, s, v;          // 0..360, 0..1, 0..1
    private final OnPick pick;

    private final View preview;
    private final TextView hexLabel;
    private final Bar hueBar, satBar, valBar;

    public ColorPickerSheet(Context c, int initColor, final OnPick pick) {
        super(c, "自定义颜色");
        this.pick = pick;

        float[] hsv = new float[3];
        Color.colorToHSV(initColor, hsv);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];

        preview = new View(c);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        plp.leftMargin = dp(16);
        plp.rightMargin = dp(16);
        preview.setLayoutParams(plp);
        body.addView(preview);

        hexLabel = new TextView(c);
        hexLabel.setTextSize(13);
        hexLabel.setGravity(android.view.Gravity.CENTER);
        hexLabel.setTextColor(c.getResources().getColor(R.color.text_secondary));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = dp(8);
        hexLabel.setLayoutParams(hlp);
        body.addView(hexLabel);

        hueBar = new Bar(c, Bar.HUE);
        satBar = new Bar(c, Bar.SAT);
        valBar = new Bar(c, Bar.VAL);
        addBar(hueBar);
        addBar(satBar);
        addBar(valBar);

        TextView ok = new TextView(c);
        ok.setText("确定");
        ok.setTextSize(16);
        ok.setGravity(android.view.Gravity.CENTER);
        ok.setTextColor(0xFFFFFFFF);
        ok.setBackgroundResource(R.drawable.bg_btn_brand);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        olp.leftMargin = dp(16);
        olp.rightMargin = dp(16);
        olp.topMargin = dp(18);
        olp.bottomMargin = dp(18);
        ok.setLayoutParams(olp);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                final int col = current();
                closeAnimated(new Runnable() {
                    public void run() { if (ColorPickerSheet.this.pick != null) ColorPickerSheet.this.pick.onPick(col); }
                });
            }
        });
        body.addView(ok);
        sync();
    }

    private void addBar(Bar b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
        lp.leftMargin = dp(16);
        lp.rightMargin = dp(16);
        lp.topMargin = dp(14);
        b.setLayoutParams(lp);
        body.addView(b);
    }

    private int current() {
        return Color.HSVToColor(new float[]{h, s, v});
    }

    private void sync() {
        int col = current();
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        g.setCornerRadius(dp(12));
        g.setColor(col);
        preview.setBackground(g);
        hexLabel.setText(String.format("#%06X", 0xFFFFFF & col));
        hueBar.invalidate();
        satBar.invalidate();
        valBar.invalidate();
    }

    /** 一条可拖动的颜色滑条 */
    private class Bar extends View {
        static final int HUE = 0, SAT = 1, VAL = 2;
        final int kind;
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();

        Bar(Context c, int kind) {
            super(c);
            this.kind = kind;
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth(), hh = getHeight();
            float r = Math.min(dp(8), hh / 2f);
            rf.set(0, 0, w, hh);
            int[] cols;
            if (kind == HUE) {
                cols = HUE_STOPS;
            } else if (kind == SAT) {
                int base = Color.HSVToColor(new float[]{h, 1f, v});
                cols = new int[]{0xFFFFFFFF, base};
            } else {
                int base = Color.HSVToColor(new float[]{h, s, 1f});
                cols = new int[]{0xFF000000, base};
            }
            pt.setShader(new LinearGradient(0, 0, w, 0, cols, null, Shader.TileMode.CLAMP));
            cv.drawRoundRect(rf, r, r, pt);
            pt.setShader(null);

            float t = kind == HUE ? hh : 0;
            float pos;
            if (kind == HUE) pos = (h / 360f) * w;
            else if (kind == SAT) pos = s * w;
            else pos = v * w;
            if (pos < dp(9)) pos = dp(9);
            if (pos > w - dp(9)) pos = w - dp(9);
            float cy = hh / 2f;
            pt.setStyle(Paint.Style.FILL);
            pt.setColor(0xFFFFFFFF);
            cv.drawCircle(pos, cy, dp(9), pt);
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeWidth(dp(2));
            pt.setColor(0x40000000);
            cv.drawCircle(pos, cy, dp(9), pt);
            pt.setStyle(Paint.Style.FILL);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int act = e.getActionMasked();
            if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_MOVE) {
                float w = Math.max(1, getWidth());
                float t = e.getX() / w;
                if (t < 0) t = 0;
                if (t > 1) t = 1;
                if (kind == HUE) h = t * 360f;
                else if (kind == SAT) s = t;
                else v = Math.max(0.08f, t);
                sync();
                // 父容器不要抢走横向拖动
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
            return true;
        }
    }
}
