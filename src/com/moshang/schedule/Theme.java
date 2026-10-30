package com.moshang.schedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

/** 运行时主题色：9 套主色，跟随深浅色自动调整明度 */
public class Theme {

    public static final String[] NAMES = {
            "校园蓝", "松柏绿", "暮云蓝", "青瓷", "鼠尾草", "雾紫", "石墨", "玫瑰", "麦田"
    };

    private static final int[] PRIMARY = {
            0xFF172E61, 0xFF205C52, 0xFF405D7A, 0xFF356B68, 0xFF526B58,
            0xFF655F7C, 0xFF555E6A, 0xFF956174, 0xFF6F5B1E
    };

    private static final int[] ACCENT = {
            0xFF254E9B, 0xFF327E70, 0xFF405D7A, 0xFF356B68, 0xFF526B58,
            0xFF655F7C, 0xFF555E6A, 0xFF805064, 0xFF5F4C16
    };

    private static final int[] WASH = {
            0xFFEAF0FA, 0xFFE8F3EF, 0xFFEAF0F6, 0xFFE9F2F0, 0xFFEDF2ED,
            0xFFF0EDF5, 0xFFEDF0F3, 0xFFF5EBEF, 0xFFF5F0DF
    };

    private static final String PREF = "schedule_theme";
    private static int cachedKey = 0;
    private static boolean cachedDark = false;
    private static boolean inited = false;

    private static void sync(Context c) {
        if (c == null) return;
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        int k = sp.getInt("key", 0);
        if (k < 0 || k >= NAMES.length) k = 0;
        boolean d = (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        cachedKey = k;
        cachedDark = d;
        inited = true;
    }

    public static int key(Context c) {
        sync(c);
        return cachedKey;
    }

    public static String name(Context c) {
        sync(c);
        return NAMES[cachedKey];
    }

    public static void setKey(Context c, int k) {
        if (k < 0 || k >= NAMES.length) k = 0;
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt("key", k).apply();
        cachedKey = k;
        inited = true;
    }

    /** 主色：浅色用原值，深色提亮到可读 */
    public static int brand(Context c) {
        sync(c);
        int p = PRIMARY[cachedKey];
        return cachedDark ? mix(p, 0xFFFFFFFF, 0.45f) : p;
    }

    /** 强调色（偏亮一档，用于次级文字/箭头） */
    public static int accent(Context c) {
        sync(c);
        int a = ACCENT[cachedKey];
        return cachedDark ? mix(a, 0xFFFFFFFF, 0.52f) : a;
    }

    /** 淡底：浅色用主题 wash，深色用卡面与主色混合 */
    public static int wash(Context c) {
        sync(c);
        if (!cachedDark) return WASH[cachedKey];
        return mix(0xFF2A3444, PRIMARY[cachedKey], 0.45f);
    }

    /** 选中块底色（沿用主色） */
    public static int selBg(Context c) {
        return brand(c);
    }

    static int mix(int a, int b, float f) {
        int ar = (a >> 16) & 0xff, ag = (a >> 8) & 0xff, ab = a & 0xff;
        int br = (b >> 16) & 0xff, bg = (b >> 8) & 0xff, bb = b & 0xff;
        int r = Math.round(ar + (br - ar) * f);
        int g = Math.round(ag + (bg - ag) * f);
        int bl = Math.round(ab + (bb - ab) * f);
        return 0xff000000 | (r << 16) | (g << 8) | bl;
    }
}
