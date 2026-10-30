package com.moshang.schedule;

import java.util.Calendar;
import java.util.TimeZone;

/** 农历 / 二十四节气 / 节日 计算引擎（1900~2100） */
public class Lunar {

    private static final int[] LUNAR_INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
            0x0d520
    };

    private static final String[] GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] ANIMAL = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] LMONTH = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
    private static final String[] LDAY = {
            "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };
    private static final String[] TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    private static final double[] TERM_INFO = {
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921, 173149, 195551, 218072, 240693,
            263343, 285989, 308563, 331033, 353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    };

    public int year, month, day;
    public boolean leap;

    // ---------- 基础量 ----------
    private static int leapMonth(int y) {
        return LUNAR_INFO[y - 1900] & 0xf;
    }

    private static int leapDays(int y) {
        if (leapMonth(y) == 0) return 0;
        return ((LUNAR_INFO[y - 1900] & 0x10000) != 0) ? 30 : 29;
    }

    private static int monthDays(int y, int m) {
        return ((LUNAR_INFO[y - 1900] & (0x10000 >> m)) != 0) ? 30 : 29;
    }

    private static int yearDays(int y) {
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) sum += ((LUNAR_INFO[y - 1900] & i) != 0) ? 1 : 0;
        return sum + leapDays(y);
    }

    private static long dayNumber(int y, int m, int d) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(y, m - 1, d, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() / 86400000L;
    }

    public static Lunar of(int y, int m, int d) {
        Lunar L = new Lunar();
        long offset = dayNumber(y, m, d) - dayNumber(1900, 1, 31);
        int i, temp = 0;
        for (i = 1900; i < 2101 && offset > 0; i++) {
            temp = yearDays(i);
            offset -= temp;
        }
        if (offset < 0) {
            offset += temp;
            i--;
        }
        L.year = i;
        int leap = leapMonth(i);
        boolean isLeap = false;
        int j;
        for (j = 1; j < 13 && offset > 0; j++) {
            if (leap > 0 && j == (leap + 1) && !isLeap) {
                --j;
                isLeap = true;
                temp = leapDays(i);
            } else {
                temp = monthDays(i, j);
            }
            if (isLeap && j == (leap + 1)) isLeap = false;
            offset -= temp;
        }
        if (offset == 0 && leap > 0 && j == leap + 1) {
            if (isLeap) isLeap = false;
            else {
                isLeap = true;
                --j;
            }
        }
        if (offset < 0) {
            offset += temp;
            --j;
        }
        L.month = j;
        L.day = (int) offset + 1;
        L.leap = isLeap;
        return L;
    }

    public String monthName() {
        return LMONTH[month - 1] + "月";
    }

    public String dayName() {
        return LDAY[day - 1];
    }

    // ---------- 干支 ----------
    private static int gzYearIndex(int y) {
        return (y - 1864) % 60;
    }

    public String gzYear() {
        int idx = gzYearIndex(year);
        return GAN[idx % 10] + ZHI[idx % 12];
    }

    public String animal() {
        return ANIMAL[gzYearIndex(year) % 12];
    }

    public static String gzDay(int y, int m, int d) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(y, m - 1, d, 0, 0, 0);
        long jdn = c.getTimeInMillis() / 86400000L + 2440588L;
        int idx = (int) ((jdn + 49) % 60);
        if (idx < 0) idx += 60;
        return GAN[idx % 10] + ZHI[idx % 12];
    }

    /** 该公历日是否某节气，是则返回节气名 */
    public static String termOf(int y, int m, int d) {
        int i0 = 2 * (m - 1);
        if (d == termDay(y, i0)) return TERMS[i0];
        if (d == termDay(y, i0 + 1)) return TERMS[i0 + 1];
        return null;
    }

    private static int termDay(int y, int n) {
        Calendar base = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        base.set(1900, 0, 6, 2, 5, 0);
        base.set(Calendar.MILLISECOND, 0);
        double ms = base.getTimeInMillis() + 31556925974.7 * (y - 1900) + TERM_INFO[n] * 60000.0;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTimeInMillis((long) ms);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    // ---------- 节日 ----------
    private static String solarFestival(int m, int d) {
        if (m == 1 && d == 1) return "元旦";
        if (m == 2 && d == 14) return "情人节";
        if (m == 3 && d == 8) return "妇女节";
        if (m == 3 && d == 12) return "植树节";
        if (m == 4 && d == 1) return "愚人节";
        if (m == 5 && d == 1) return "劳动节";
        if (m == 5 && d == 4) return "青年节";
        if (m == 6 && d == 1) return "儿童节";
        if (m == 7 && d == 1) return "建党节";
        if (m == 8 && d == 1) return "建军节";
        if (m == 9 && d == 10) return "教师节";
        if (m == 10 && d == 1) return "国庆节";
        if (m == 11 && d == 8) return "记者节";
        if (m == 12 && d == 24) return "平安夜";
        if (m == 12 && d == 25) return "圣诞节";
        return null;
    }

    private static String lunarFestival(Lunar L) {
        int m = L.month, d = L.day;
        if (!L.leap) {
            if (m == 1 && d == 1) return "春节";
            if (m == 1 && d == 15) return "元宵节";
            if (m == 2 && d == 2) return "龙抬头";
            if (m == 5 && d == 5) return "端午节";
            if (m == 7 && d == 7) return "七夕节";
            if (m == 7 && d == 15) return "中元节";
            if (m == 8 && d == 15) return "中秋节";
            if (m == 9 && d == 9) return "重阳节";
            if (m == 12 && d == 8) return "腊八节";
            if (m == 12 && d == 23) return "小年";
        }
        return null;
    }

    private static String nthWeekday(int y, int m, int d, int weekday, int n) {
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, d);
        int wd = c.get(Calendar.DAY_OF_WEEK);
        int dom = c.get(Calendar.DAY_OF_MONTH);
        if (wd == weekday && ((dom - 1) / 7 + 1) == n) return null;
        return null;
    }

    private static boolean isNthSunday(int y, int m, int d, int n) {
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, d);
        return c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && ((d - 1) / 7 + 1) == n;
    }

    private static boolean isNthThursday(int y, int m, int d, int n) {
        Calendar c = Calendar.getInstance();
        c.set(y, m - 1, d);
        return c.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY && ((d - 1) / 7 + 1) == n;
    }

    /** 单元格下方小字的标签；返回 [文字, 是否高亮(红)] ；空则 null */
    public static String[] cellLabel(int y, int m, int d) {
        String t = termOf(y, m, d);
        if (t != null) return new String[]{t, "0"};
        String f = solarFestival(m, d);
        if (f != null) return new String[]{f, "0"};
        if (m == 5 && isNthSunday(y, m, d, 2)) return new String[]{"母亲节", "0"};
        if (m == 6 && isNthSunday(y, m, d, 3)) return new String[]{"父亲节", "0"};
        if (m == 11 && isNthThursday(y, m, d, 4)) return new String[]{"感恩节", "0"};
        Lunar L = of(y, m, d);
        String lf = lunarFestival(L);
        if (lf != null) return new String[]{lf, "1"};
        if (L.day == 1) return new String[]{L.monthName(), "1"};
        return new String[]{L.dayName(), "0"};
    }

    /** 信息条文本：农历九月廿三 丙午年[马]戊戌月 己卯日 */
    public static String infoLine(int y, int m, int d) {
        Lunar L = of(y, m, d);
        StringBuilder sb = new StringBuilder();
        sb.append(L.monthName()).append(L.dayName());
        sb.append("  ").append(L.gzYear()).append("年[").append(L.animal()).append("]");
        sb.append(L.gzMonth(y, m, d)).append("月");
        sb.append("  ").append(gzDay(y, m, d)).append("日");
        return sb.toString();
    }

    /** 月柱干支（以节气分月，寅月起） */
    public String gzMonth(int y, int m, int d) {
        // 找到最近一个已过的“节”
        int[] branchOfMonth = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0}; // 1月≈丑/寅边界，用节气精判
        int branch;
        // 判断是否已过当月的“节”（月首节气）
        int jieIdx = 2 * (m - 1); // 该月第一个“节”
        boolean afterJie = d >= termDay(y, jieIdx);
        // 月支序：立春(2月节)→寅(2) ...
        // 由月份推算：以“节”为界，立春后为寅月
        branch = ((m - (afterJie ? 2 : 3)) % 12 + 12) % 12;
        // 更稳妥：使用 节气对应的地支
        int zhiIdx = (m + (afterJie ? 0 : -1));
        // 甲/己年 → 寅月干为丙
        int yg = gzYearIndex(year) % 10;
        int yinGan = (yg * 2 + 2) % 10;
        // 从寅月(0)到当前月
        int monOrder = ((m - 2) + 12) % 12; // 2月≈寅月(0)
        if (!afterJie) monOrder = ((m - 3) + 12) % 12;
        int gan = (yinGan + monOrder) % 10;
        int zhi = (2 + monOrder) % 12;
        return GAN[gan] + ZHI[zhi];
    }
}

