package com.moshang.schedule;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 收到闹钟后弹提醒通知 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String id = intent.getStringExtra("id");
        if (id == null) id = intent.getAction();
        Store s = Store.get(ctx);
        Event e = id == null ? null : s.byId(id);

        String title = e != null ? e.title : "日程提醒";
        StringBuilder sb = new StringBuilder();
        if (e != null) {
            if (e.allDay) {
                sb.append("全天");
            } else {
                SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.getDefault());
                sb.append(f.format(new Date(e.start)));
                if (e.end > e.start) sb.append(" - ").append(f.format(new Date(e.end)));
            }
            if (e.location != null && e.location.length() > 0) sb.append("  ·  ").append(e.location);
        }

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(ctx, 0, open, piFlags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("schedule_remind", "日程提醒", NotificationManager.IMPORTANCE_HIGH);
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
            b = new Notification.Builder(ctx, "schedule_remind");
        } else {
            b = new Notification.Builder(ctx);
        }
        b.setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(sb.toString())
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(content);
        if (sb.length() > 0) b.setStyle(new Notification.BigTextStyle().bigText(sb.toString()));

        int nid = id == null ? 1 : id.hashCode();
        nm.notify(nid, b.build());
    }
}

