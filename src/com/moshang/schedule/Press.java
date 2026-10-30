package com.moshang.schedule;

import android.animation.TimeInterpolator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

/** 按压反馈动效（曲线取自 vivo OriginOS：pathInterpolator 0.25,0.45,0.3,1） */
public class Press {

    private static TimeInterpolator interp;

    private static TimeInterpolator interp(View v) {
        if (interp == null) {
            interp = AnimationUtils.loadInterpolator(v.getContext(), R.interpolator.press_interp);
        }
        return interp;
    }

    /** 尊重系统动画缩放：关闭动画时按压反馈直接跳过 */
    static long dur(View v, long d) {
        try {
            float s = android.provider.Settings.Global.getFloat(
                    v.getContext().getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return (long) (d * Math.max(0f, s));
        } catch (Throwable t) {
            return d;
        }
    }

    public static void attach(final View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().cancel();
                        view.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.82f)
                                .setDuration(dur(view, 200)).setInterpolator(interp(view)).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().cancel();
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                                .setDuration(dur(view, 250)).setInterpolator(interp(view)).start();
                        break;
                }
                return false;
            }
        });
    }
}

