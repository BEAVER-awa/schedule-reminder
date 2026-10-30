package com.moshang.schedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 开机后重排全部提醒 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Store.get(ctx).rescheduleAll();
        }
    }
}

