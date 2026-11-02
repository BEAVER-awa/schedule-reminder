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

/**
 * 闹钟回调，按 kind 分四种：
 *   remind  提前提醒（普通通知，可滑掉）
 *   begin   事件开始 → 弹「正在进行」富通知：进度条 + 倒计时 + 「结束提醒」按钮，常驻
 *   end     事件结束 → 收掉那条常驻通知
 *   stop    用户点「结束提醒」 → 收掉通知并撤销后续闹钟
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String CH_REMIND = "schedule_remind";
    private static final String CH_NOW = "schedule_now";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String id = intent.getStringExtra("id");
        String kind = intent.getStringExtra("kind");
        if (kind == null) kind = Store.K_REMIND;

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        ensureChannels(ctx, nm);

        int nid = id == null ? 1 : id.hashCode();
        Store s = Store.get(ctx);

        if (Store.K_END.equals(kind)) {
            nm.cancel(nid);
            return;
        }
        if (Store.K_STOP.equals(kind)) {
            nm.cancel(nid);
            // 用户主动结束：连后续的 begin / end 闹钟一起撤掉，今天不再弹
            if (id != null) s.cancel(id);
            return;
        }

        Event e = id == null ? null : s.byId(id);
        if (e == null) return;

        if (Store.K_BEGIN.equals(kind)) {
            notifyOngoing(ctx, nm, s, e, nid);
        } else {
            notifyRemind(ctx, nm, e, nid);
        }
    }

    private void ensureChannels(Context ctx, NotificationManager nm) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel a = new NotificationChannel(CH_REMIND, "日程提醒",
                NotificationManager.IMPORTANCE_HIGH);
        a.enableVibration(true);
        a.setDescription("日程开始前的提醒");
        nm.createNotificationChannel(a);

        NotificationChannel b = new NotificationChannel(CH_NOW, "进行中的日程",
                NotificationManager.IMPORTANCE_HIGH);
        b.enableVibration(false);
        b.setDescription("日程进行时在通知栏常驻，带进度与结束按钮");
        nm.createNotificationChannel(b);
    }

    private Notification.Builder base(Context ctx, String channel) {
        if (Build.VERSION.SDK_INT >= 26) return new Notification.Builder(ctx, channel);
        return new Notification.Builder(ctx);
    }

    private PendingIntent openApp(Context ctx) {
        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(ctx, 0, open, flags);
    }

    private PendingIntent stopIntent(Context ctx, String id) {
        Intent i = new Intent(ctx, ReminderReceiver.class)
                .setAction(Store.K_STOP + ":" + id)
                .putExtra("id", id)
                .putExtra("kind", Store.K_STOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, (id + "#stop").hashCode(), i, flags);
    }

    private String spanOf(Event e) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        sb.append(f.format(new Date(e.start)));
        if (e.end > e.start) sb.append(" - ").append(f.format(new Date(e.end)));
        return sb.toString();
    }

    /** 提前提醒：普通可清除通知 */
    private void notifyRemind(Context ctx, NotificationManager nm, Event e, int nid) {
        StringBuilder sb = new StringBuilder();
        if (e.allDay) sb.append("全天");
        else sb.append(spanOf(e));
        if (e.location != null && e.location.length() > 0) sb.append("  ·  ").append(e.location);

        Notification.Builder b = base(ctx, CH_REMIND)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(e.title)
                .setContentText(sb.toString())
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setContentIntent(openApp(ctx));
        b.setStyle(new Notification.BigTextStyle().bigText(sb.toString()));
        if (Build.VERSION.SDK_INT >= 26) b.setColor(e.color);
        nm.notify(nid, b.build());
    }

    /** 正在进行：进度条 + 倒计时 + 结束按钮，常驻不可滑掉 */
    private void notifyOngoing(Context ctx, NotificationManager nm, Store s, Event e, int nid) {
        long now = System.currentTimeMillis();
        long dur = Store.durationOf(e);
        int pct = 0;
        if (dur > 0) pct = (int) Math.max(0, Math.min(100, (now - e.start) * 100 / dur));
        long leftMin = Math.max(0, (e.end - now) / 60000L);
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.getDefault());

        String line1 = e.location != null && e.location.length() > 0 ? e.location + " · " : "";
        if (e.allDay) {
            line1 += "全天";
            leftMin = 0;
        } else if (e.end > e.start) {
            line1 += "剩约 " + leftMin + " 分钟 · " + f.format(new Date(e.end)) + " 结束";
        } else {
            line1 += f.format(new Date(e.start)) + " 开始";
        }

        Notification.Builder b = base(ctx, CH_NOW)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("正在进行 · " + e.title)
                .setContentText(line1)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentIntent(openApp(ctx))
                .addAction(0, "结束提醒", stopIntent(ctx, e.id));
        if (Build.VERSION.SDK_INT >= 26) b.setColor(e.color);

        if (!e.allDay && e.end > e.start) {
            b.setProgress(100, pct, false);
            if (Build.VERSION.SDK_INT >= 24) {
                // 通知右上角显示实时倒计时（随时间自己走，不用反复刷新通知）
                b.setUsesChronometer(true);
                b.setChronometerCountDown(true);
                b.setShowWhen(true);
                b.setWhen(e.end);
            } else {
                b.setShowWhen(true);
                b.setWhen(now);
            }
            b.setSubText(f.format(new Date(e.start)) + " - " + f.format(new Date(e.end)));
        } else {
            b.setShowWhen(true);
            b.setWhen(now);
        }
        nm.notify(nid, b.build());
    }
}
