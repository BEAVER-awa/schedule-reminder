package com.moshang.schedule;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 桌面小组件：显示今天日期与今日日程 */
public class ScheduleWidget extends AppWidgetProvider {

    private static final String[] WK = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    /** 数据变更后刷新所有小组件 */
    public static void refresh(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName cn = new ComponentName(ctx, ScheduleWidget.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    private static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        Store s = Store.get(ctx);
        long today = Store.dayStart(System.currentTimeMillis());
        List<Event> evs = s.onDay(today);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(today);
        v.setTextViewText(R.id.w_date, (c.get(Calendar.MONTH) + 1) + "月" + c.get(Calendar.DAY_OF_MONTH) + "日 " + WK[c.get(Calendar.DAY_OF_WEEK) - 1]);

        int[] rows = {R.id.w_row1, R.id.w_row2, R.id.w_row3, R.id.w_row4};
        int[] dots = {R.id.w_dot1, R.id.w_dot2, R.id.w_dot3, R.id.w_dot4};
        int[] texts = {R.id.w_t1, R.id.w_t2, R.id.w_t3, R.id.w_t4};
        for (int i = 0; i < 4; i++) {
            if (i < evs.size()) {
                Event e = evs.get(i);
                v.setViewVisibility(rows[i], View.VISIBLE);
                v.setInt(dots[i], "setColorFilter", e.color);
                v.setTextViewText(texts[i], line(e));
            } else {
                v.setViewVisibility(rows[i], View.GONE);
            }
        }
        v.setViewVisibility(R.id.w_empty, evs.isEmpty() ? View.VISIBLE : View.GONE);

        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_root, pi);

        mgr.updateAppWidget(id, v);
    }

    private static String line(Event e) {
        if (e.allDay) return "全天  " + e.title;
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return f.format(new Date(e.start)) + "  " + e.title;
    }
}

