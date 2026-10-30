package com.moshang.schedule;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** OriginOS 风格底部弹层：全屏遮罩 + 圆角面板上滑出现，顶部抓手可下拉关闭 */
public class Sheet extends Dialog {

    protected final Context ctx;
    protected FrameLayout root;
    protected LinearLayout panel;
    protected LinearLayout body;
    private static final float OFFSCREEN = 100000f;
    private View scrim;
    private float startY;
    private boolean closing;
    protected boolean fromTop = false;
    protected int topOffset = 0;
    protected int scrimColor = 0x52000000;

    public Sheet(Context c, String title) {
        super(c, R.style.SheetDialog);
        ctx = c;

        root = new FrameLayout(c);
        scrim = new View(c);
        scrim.setBackgroundColor(scrimColor);
        scrim.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closeAnimated(); }
        });
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundResource(R.drawable.bg_sheet);

        FrameLayout handleWrap = new FrameLayout(c);
        handleWrap.setPadding(0, dp(10), 0, dp(8));
        View handle = new View(c);
        handle.setBackgroundResource(R.drawable.bg_sheet_handle);
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(dp(38), dp(4));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        handle.setLayoutParams(hlp);
        handleWrap.addView(handle);
        panel.addView(handleWrap);

        if (title != null && title.length() > 0) {
            TextView tv = new TextView(c);
            tv.setText(title);
            tv.setTextSize(16);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(c.getResources().getColor(R.color.text_primary));
            tv.setPadding(dp(22), dp(4), dp(22), dp(12));
            panel.addView(tv);
        }

        body = new LinearLayout(c);
        body.setOrientation(LinearLayout.VERTICAL);
        panel.addView(body);

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.gravity = Gravity.BOTTOM;
        root.addView(panel, plp);

        View.OnTouchListener drag = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = e.getRawY();
                        panel.animate().cancel();
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dy = e.getRawY() - startY;
                        float mv = fromTop ? Math.min(dy, 0f) : Math.max(dy, 0f);
                        panel.setTranslationY(mv);
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        float dy = e.getRawY() - startY;
                        if (fromTop ? (dy < -dp(72)) : (dy > dp(72))) closeAnimated();
                        else panel.animate().translationY(0).setDuration(200)
                                .setInterpolator(AnimationUtils.loadInterpolator(ctx, R.interpolator.press_interp))
                                .start();
                        return true;
                    }
                }
                return false;
            }
        };
        handleWrap.setOnTouchListener(drag);

        setContentView(root);
    }

    protected int dp(float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Window win = getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            win.setDimAmount(0f);
        }
        setCanceledOnTouchOutside(false);
    }

    /** 先摆到屏幕外再显示，避免出现一帧“面板已在最终位置”的闪帧 */
    public void showSheet() {
        FrameLayout.LayoutParams slp = (FrameLayout.LayoutParams) scrim.getLayoutParams();
        slp.topMargin = topOffset;
        scrim.setLayoutParams(slp);
        scrim.setBackgroundColor(scrimColor);
        FrameLayout.LayoutParams plp = (FrameLayout.LayoutParams) panel.getLayoutParams();
        plp.gravity = fromTop ? Gravity.TOP : Gravity.BOTTOM;
        plp.topMargin = topOffset;
        panel.setLayoutParams(plp);
        scrim.setAlpha(0f);
        panel.setTranslationY(fromTop ? -OFFSCREEN : OFFSCREEN);
        show();
        panel.post(new Runnable() {
            public void run() {
                float h = panel.getHeight();
                if (h <= 0) h = panel.getMeasuredHeight();
                if (h <= 0) h = dp(420);
                panel.animate().cancel();
                panel.setTranslationY(fromTop ? -(h + topOffset) : h);
                panel.animate().translationY(0).setDuration(300)
                        .setInterpolator(AnimationUtils.loadInterpolator(ctx, R.interpolator.vivo_enter))
                        .withLayer()
                        .start();
                scrim.animate().alpha(1f).setDuration(200).start();
            }
        });
    }

    protected void closeAnimated() { closeAnimated(null); }

    /** 关闭动画结束后再执行 after（用于串联下一个弹层，避免两层重叠闪动） */
    protected void closeAnimated(final Runnable after) {
        if (closing) return;
        closing = true;
        float ch = Math.max(panel.getHeight(), dp(300));
        scrim.animate().alpha(0f).setDuration(180).start();
        panel.animate().translationY(fromTop ? -(ch + topOffset) : ch).setDuration(250)
                .setInterpolator(AnimationUtils.loadInterpolator(ctx, R.interpolator.vivo_exit))
                .withEndAction(new Runnable() {
                    public void run() {
                        dismiss();
                        if (after != null) after.run();
                    }
                }).start();
    }

    @Override
    public void onBackPressed() {
        closeAnimated();
    }
}

