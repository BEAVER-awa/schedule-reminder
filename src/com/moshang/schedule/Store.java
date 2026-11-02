package com.moshang.schedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/** 数据仓库：本地 JSON 持久化 + 重复规则 + 闹钟排程 + 导入导出 */
public class Store {

    /** 分类色板：12 色，每种都对应一类常见语义（见 SEMANTIC），
     *  中饱和、明度适中，浅色与深色主题下都清晰可辨 */
    public static final int[] PALETTE = {
            0xFF4A86D8, // 0 工作 · 蓝
            0xFF7C7EE8, // 1 学习 · 靛
            0xFF35BE7F, // 2 运动 · 绿
            0xFF2FB9BC, // 3 生活 · 青
            0xFFE85F5F, // 4 重要 · 红
            0xFFE76AA0, // 5 健康 · 玫
            0xFFF0922F, // 6 社交 · 橙
            0xFF47AFE3, // 7 出行 · 天蓝
            0xFFD9AB3C, // 8 财务 · 金
            0xFFC08A52, // 9 家庭 · 棕
            0xFF9E7AE0, // 10 娱乐 · 紫
            0xFF8C97A6  // 11 其他 · 灰
    };

    /** 分类名关键词 → PALETTE 下标。新建分类时按名称自动选一个语义相符的颜色。 */
    private static final String[][] SEMANTIC = {
            {"工作", "会议", "上班", "项目", "汇报", "客户", "加班", "出差", "面试", "述职"},
            {"学习", "课程", "上课", "课", "考试", "作业", "复习", "自习", "讲座", "论文", "实验"},
            {"运动", "健身", "跑步", "锻炼", "球", "游泳", "瑜伽", "骑行", "爬山"},
            {"生活", "日常", "家务", "购物", "做饭", "买菜", "打扫", "收纳"},
            {"重要", "紧急", "截止", "deadline", "DDL", "必须"},
            {"医疗", "看病", "医院", "体检", "牙", "拿药", "复诊", "挂号", "健康"},
            {"社交", "聚会", "聚餐", "约", "生日", "团建", "见面", "朋友"},
            {"旅行", "出行", "航班", "高铁", "酒店", "旅游", "机场", "火车"},
            {"财务", "账单", "缴费", "还款", "工资", "报销", "发票", "转账"},
            {"家庭", "家", "父母", "孩子", "爸妈", "亲戚", "接"},
            {"娱乐", "电影", "游戏", "休闲", "音乐", "追剧", "看剧", "放松"}
    };

    /** 按分类名推断一个默认颜色；没有关键词命中时用灰色（其他）。 */
    public static int colorForName(String name) {
        int c = colorOrZero(name);
        return c == 0 ? PALETTE[PALETTE.length - 1] : c;
    }

    /** 同上，但没有命中时返回 0，调用方可以据此「保持原色不变」。 */
    public static int colorOrZero(String name) {
        if (name != null) {
            String n = name.trim();
            for (int i = 0; i < SEMANTIC.length; i++) {
                for (int k = 0; k < SEMANTIC[i].length; k++) {
                    if (n.contains(SEMANTIC[i][k])) return PALETTE[i];
                }
            }
        }
        return 0;
    }

    public static final long DAY = 86400000L;
    private static final long H = 3600000L;
    private static final long M = 60000L;

    private static Store INSTANCE;

    public static synchronized Store get(Context c) {
        if (INSTANCE == null) INSTANCE = new Store(c.getApplicationContext());
        return INSTANCE;
    }

    private final Context ctx;
    private final SharedPreferences sp;
    public final List<Event> events = new ArrayList<Event>();
    public final List<Category> cats = new ArrayList<Category>();

    private Store(Context c) {
        ctx = c;
        sp = c.getSharedPreferences("schedule_store", Context.MODE_PRIVATE);
        load();
        migratePalette();
    }

    /** 0.0.1 用过的那 9 个灰调色。升级到 0.0.2 时，凡是还停在旧色板上的分类，
     *  按名称重新匹配成语义色；用户手动选过的颜色不在旧色板里，不会被覆盖。 */
    private static final int[] OLD_PALETTE = {
            // 0.0.1 的 9 色灰调板
            0xFF3F6BA8, 0xFF3E8C7E, 0xFF6C7A96, 0xFF4E8F8B, 0xFF7A8F76,
            0xFF7F7398, 0xFF6E7A88, 0xFFA87E8E, 0xFFA89052,
            // 0.0.2 初版的 12 色（偏暗，已提亮替换）
            0xFF3B6FB6, 0xFF5B5FC7, 0xFF2E9E6B, 0xFF2A9D9F, 0xFFD05353,
            0xFFC8588C, 0xFFD9802F, 0xFF3F92C4, 0xFFB99135, 0xFFA4713F,
            0xFF8A63C4, 0xFF77808F
    };

    private static boolean inOldPalette(int c) {
        for (int i = 0; i < OLD_PALETTE.length; i++) {
            if (OLD_PALETTE[i] == c) return true;
        }
        return false;
    }

    private void migratePalette() {
        if (sp.getBoolean("cat_palette_v3", false)) return;
        boolean changed = false;
        for (int i = 0; i < cats.size(); i++) {
            Category c = cats.get(i);
            if (inOldPalette(c.color)) {
                int nc = colorForName(c.name);
                if (nc != c.color) {
                    c.color = nc;
                    changed = true;
                }
            }
        }
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            Category c = cat(e.category);
            if (c != null && inOldPalette(e.color) && e.color != c.color) {
                e.color = c.color;
                changed = true;
            }
        }
        sp.edit().putBoolean("cat_palette_v3", true).apply();
        if (changed) save();
    }

    // ---------- 持久化 ----------
    private void load() {
        String s = sp.getString("data", "");
        if (s == null || s.length() == 0) {
            seed();
            save();
            return;
        }
        try {
            fill(new JSONObject(s), false);
        } catch (Exception ignored) {
        }
        if (cats.isEmpty()) seed();
    }

    private void fill(JSONObject o, boolean merge) throws Exception {
        if (!merge) { cats.clear(); events.clear(); }
        JSONArray ca = o.optJSONArray("cats");
        if (ca == null) ca = o.optJSONArray("categories");
        if (ca != null) {
            for (int i = 0; i < ca.length(); i++) {
                JSONObject j = ca.getJSONObject(i);
                String name = j.optString("name");
                if (name != null && name.length() > 0 && cat(name) == null)
                    cats.add(new Category(name, parseColor(j.opt("color"), PALETTE[0])));
            }
        }
        JSONArray ea = o.optJSONArray("events");
        if (ea != null) {
            for (int i = 0; i < ea.length(); i++) {
                JSONObject j = ea.getJSONObject(i);
                Event e = new Event();
                e.id = j.optString("id");
                if (merge && byId(e.id) != null) continue;
                e.title = j.optString("title");
                e.location = j.optString("location");
                if (e.location.length() == 0) e.location = j.optString("place");
                e.note = j.optString("note");
                e.category = j.optString("category");
                e.color = parseColor(j.opt("color"), PALETTE[0]);
                Object sv = j.opt("start");
                e.start = (sv instanceof Number) ? ((Number) sv).longValue() : parseIso(sv == null ? "" : sv.toString());
                Object ev = j.opt("end");
                if (ev instanceof Number) {
                    e.end = ((Number) ev).longValue();
                } else {
                    int cm = parseClock(ev == null ? "" : ev.toString());
                    e.end = cm >= 0 ? dayStart(e.start) + cm * M : e.start + H;
                }
                e.allDay = j.optBoolean("allDay", false);
                e.remind = j.optInt("remind", -1);
                e.repeat = j.optString("repeat", "none");
                Object uv = j.opt("until");
                e.until = (uv instanceof Number) ? ((Number) uv).longValue() : (uv == null ? 0L : parseIso(uv.toString()));
                JSONArray da = j.optJSONArray("dates");
                if (da != null) for (int k = 0; k < da.length(); k++) e.dates.add(da.optString(k));
                events.add(e);
            }
        }
    }

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            JSONArray ca = new JSONArray();
            for (Category c : cats) {
                JSONObject j = new JSONObject();
                j.put("name", c.name);
                j.put("color", c.color);
                ca.put(j);
            }
            JSONArray ea = new JSONArray();
            for (Event e : events) {
                JSONObject j = new JSONObject();
                j.put("id", e.id);
                j.put("title", e.title);
                j.put("location", e.location);
                j.put("note", e.note);
                j.put("category", e.category);
                j.put("color", e.color);
                j.put("start", e.start);
                j.put("end", e.end);
                j.put("allDay", e.allDay);
                j.put("remind", e.remind);
                j.put("repeat", e.repeat);
                j.put("until", e.until);
                if (e.dates.size() > 0) {
                    JSONArray da = new JSONArray();
                    for (int k = 0; k < e.dates.size(); k++) da.put(e.dates.get(k));
                    j.put("dates", da);
                }
                ea.put(j);
            }
            o.put("cats", ca);
            o.put("events", ea);
            return o.toString();
        } catch (Exception ex) {
            return "{}";
        }
    }

    public void save() {
        sp.edit().putString("data", toJson()).apply();
    }

    public int importJson(String s) throws Exception {
        JSONObject o = new JSONObject(s);
        int before = events.size();
        fill(o, true);
        save();
        rescheduleAll();
        return events.size() - before;
    }

    private void seed() {
        cats.add(new Category("工作", colorForName("工作")));
        cats.add(new Category("学习", colorForName("学习")));
        cats.add(new Category("生活", colorForName("生活")));
        cats.add(new Category("运动", colorForName("运动")));
        cats.add(new Category("其他", colorForName("其他")));

        long today = dayStart(System.currentTimeMillis());
        events.add(mk("团队周会", today + 10 * H, today + 11 * H, "工作", colorForName("工作"), "会议室 A"));
        events.add(mk("健身", today + 18 * H + 30 * M, today + 19 * H + 30 * M, "运动", colorForName("运动"), "健身房"));
        events.add(mk("项目复盘", today + 15 * H, today + 16 * H, "工作", colorForName("工作"), ""));
        events.add(mk("读书", today + DAY + 9 * H, today + DAY + 10 * H, "学习", colorForName("学习"), "图书馆"));
    }

    private Event mk(String title, long s, long e, String cat, int color, String loc) {
        Event ev = new Event();
        ev.id = UUID.randomUUID().toString();
        ev.title = title;
        ev.start = s;
        ev.end = e;
        ev.category = cat;
        ev.color = color;
        ev.location = loc;
        return ev;
    }

    // ---------- 增删改 ----------
    public void add(Event e) {
        if (e.id == null || e.id.length() == 0) e.id = UUID.randomUUID().toString();
        events.add(e);
        save();
        schedule(e);
    }

    public void update(Event e) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id.equals(e.id)) {
                events.set(i, e);
                break;
            }
        }
        save();
        cancel(e.id);
        schedule(e);
    }

    public void remove(String id) {
        cancel(id);
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id.equals(id)) {
                events.remove(i);
                break;
            }
        }
        save();
    }

    public Event byId(String id) {
        for (Event e : events) if (e.id.equals(id)) return e;
        return null;
    }

    /** 渲染用色：始终以分类当前颜色为准，避免事件里的副本过期（改分类颜色后旧日程不变色） */
    public int colorOf(Event e) {
        Category c = cat(e.category);
        return c != null ? c.color : e.color;
    }

    /** 按名称重置所有分类颜色，并同步到全部日程（分类管理页的一键修复入口） */
    public void resetCatColors() {
        for (int i = 0; i < cats.size(); i++) {
            Category c = cats.get(i);
            c.color = colorForName(c.name);
        }
        for (int i = 0; i < events.size(); i++) {
            Event e = events.get(i);
            Category c = cat(e.category);
            if (c != null) e.color = c.color;
        }
        save();
    }

    public Category cat(String name) {
        for (Category c : cats) if (c.name.equals(name)) return c;
        return null;
    }

    public int catCount(String name) {
        int n = 0;
        for (Event e : events) if (name != null && name.equals(e.category)) n++;
        return n;
    }

    public void addCat(String name, int color) {
        if (name == null || name.trim().length() == 0) return;
        if (cat(name) != null) return;
        // color 传 0 表示「没指定」，按分类名推断语义色
        int col = color == 0 ? colorForName(name) : color;
        cats.add(new Category(name.trim(), col));
        save();
    }

    public void renameCat(String oldName, String newName, int color) {
        Category c = cat(oldName);
        if (c == null) return;
        String nm = (newName == null || newName.trim().length() == 0) ? oldName : newName.trim();
        for (Event e : events) {
            if (oldName != null && oldName.equals(e.category)) {
                e.category = nm;
                e.color = color;
            }
        }
        c.name = nm;
        c.color = color;
        save();
    }

    public void deleteCat(String name) {
        Category c = cat(name);
        if (c == null) return;
        cats.remove(c);
        String fb = cats.size() > 0 ? cats.get(0).name : "";
        int fc = cats.size() > 0 ? cats.get(0).color : PALETTE[0];
        for (Event e : events) {
            if (name != null && name.equals(e.category)) {
                e.category = fb;
                e.color = fc;
            }
        }
        save();
    }

    // ---------- 导入字段解析 ----------
    private static long parseIso(String s) {
        if (s == null || s.length() < 10) return 0L;
        String[] pats = {"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm"};
        for (int i = 0; i < pats.length; i++) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(pats[i], Locale.US);
                if (s.endsWith("Z")) f.setTimeZone(TimeZone.getTimeZone("UTC"));
                return f.parse(s.trim()).getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private static int parseClock(String s) { // "HH:MM" -> 当天分钟数，非法返回 -1
        if (s == null) return -1;
        int p = s.indexOf(':');
        if (p <= 0) return -1;
        try {
            int h = Integer.parseInt(s.substring(0, p).trim());
            int m = Integer.parseInt(s.substring(p + 1).trim());
            return h * 60 + m;
        } catch (Exception e) { return -1; }
    }

    private static int parseColor(Object v, int def) { // "#rrggbb" 或 int
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        String s = v.toString().trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            long c = Long.parseLong(s, 16);
            if (s.length() <= 6) c |= 0xFF000000L;
            return (int) c;
        } catch (Exception e) { return def; }
    }

    public static String ymd(long ms) { // 本地日期 yyyy-MM-dd
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    // ---------- 重复规则 ----------
    public static boolean occursOn(Event e, long day) {
        long d = dayStart(day);
        String r = e.repeat == null ? "none" : e.repeat;
        if ("dates".equals(r)) {
            String ds = ymd(d);
            for (int i = 0; i < e.dates.size(); i++) if (e.dates.get(i).equals(ds)) return true;
            return false;
        }
        long base = dayStart(e.start);
        if (d < base) return false;
        if (e.until > 0 && d > dayStart(e.until)) return false;
        if ("daily".equals(r)) return true;
        if ("weekly".equals(r)) {
            Calendar a = Calendar.getInstance();
            a.setTimeInMillis(base);
            Calendar b = Calendar.getInstance();
            b.setTimeInMillis(d);
            return a.get(Calendar.DAY_OF_WEEK) == b.get(Calendar.DAY_OF_WEEK);
        }
        if ("monthly".equals(r)) {
            Calendar a = Calendar.getInstance();
            a.setTimeInMillis(base);
            Calendar b = Calendar.getInstance();
            b.setTimeInMillis(d);
            int da = a.get(Calendar.DAY_OF_MONTH);
            int db = b.get(Calendar.DAY_OF_MONTH);
            if (da == db) return true;
            if (da > b.getActualMaximum(Calendar.DAY_OF_MONTH)) return db == b.getActualMaximum(Calendar.DAY_OF_MONTH);
            return false;
        }
        return d == base;
    }

    /** 把事件在指定日期上实例化（保持当日时间） */
    private Event instanceOn(Event e, long day) {
        if (dayStart(e.start) == dayStart(day)) return e;
        Event c = e.copy();
        long timeOfDay = e.start - dayStart(e.start);
        long dur = e.end - e.start;
        c.start = dayStart(day) + timeOfDay;
        c.end = c.start + dur;
        return c;
    }

    public List<Event> onDay(long dayMillis) {
        long d0 = dayStart(dayMillis);
        List<Event> out = new ArrayList<Event>();
        for (Event e : events) {
            if (occursOn(e, d0)) out.add(instanceOn(e, d0));
        }
        sort(out);
        return out;
    }

    public List<Event> upcoming() {
        long start = dayStart(System.currentTimeMillis());
        List<Event> out = new ArrayList<Event>();
        for (Event e : events) {
            if ("none".equals(e.repeat) || e.repeat == null) {
                if (dayStart(e.start) >= start) out.add(e);
            } else {
                for (int i = 0; i < 200; i++) {
                    long d = start + i * DAY;
                    if (e.until > 0 && d > dayStart(e.until)) break;
                    if (occursOn(e, d)) out.add(instanceOn(e, d));
                }
            }
        }
        sort(out);
        return out;
    }

    private void sort(List<Event> list) {
        Collections.sort(list, new Comparator<Event>() {
            public int compare(Event a, Event b) {
                if (a.allDay != b.allDay) return a.allDay ? -1 : 1;
                return Long.compare(a.start, b.start);
            }
        });
    }

    /** 某月每天的事件颜色点：key = 日(1..31) */
    public HashMap<Integer, List<Integer>> monthDots(int year, int month) {
        HashMap<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>();
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1);
        int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= days; d++) {
            c.set(year, month, d);
            long day = dayStart(c.getTimeInMillis());
            for (Event e : events) {
                if (occursOn(e, day)) {
                    List<Integer> l = map.get(d);
                    if (l == null) {
                        l = new ArrayList<Integer>();
                        map.put(d, l);
                    }
                    if (l.size() < 4) l.add(colorOf(e));
                }
            }
        }
        return map;
    }

    // ---------- 时间工具 ----------
    public static long dayStart(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static boolean sameDay(long a, long b) {
        return dayStart(a) == dayStart(b);
    }

    // ---------- 提醒 ----------
    public long nextFireTime(Event e) {
        if (e.remind < 0) return 0;
        long now = System.currentTimeMillis();
        long day = dayStart(now);
        for (int i = 0; i < 400; i++) {
            long d = day + i * DAY;
            if (e.until > 0 && d > dayStart(e.until)) break;
            if (occursOn(e, d)) {
                long timeOfDay = e.start - dayStart(e.start);
                long at = d + timeOfDay - e.remind * M;
                if (at > now) return at;
            }
        }
        return 0;
    }

    /** 事件时长（毫秒），异常数据兜底 1 分钟 */
    public static long durationOf(Event e) {
        long d = e.end - e.start;
        return d > 0 ? d : 60 * M;
    }

    /** 下一次（或正在进行中的）开始时刻；返回过去时刻表示「已经开始了」 */
    public long nextStart(Event e) {
        long now = System.currentTimeMillis();
        long day = dayStart(now);
        long dur = durationOf(e);
        for (int i = -1; i < 400; i++) {
            long d = day + i * DAY;
            if (e.until > 0 && d > dayStart(e.until)) break;
            if (occursOn(e, d)) {
                long at = d + (e.start - dayStart(e.start));
                if (at > now) return at;              // 还没开始
                if (at + dur > now) return at;        // 正在进行
            }
        }
        return 0;
    }

    public void schedule(Event e) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            long at = nextFireTime(e);
            if (at > 0) setAlarm(am, at, piFor(e, K_REMIND));
            long st = nextStart(e);
            if (st > 0) {
                setAlarm(am, st, piFor(e, K_BEGIN));
                setAlarm(am, st + durationOf(e), piFor(e, K_END));
            }
        } catch (Exception ignored) {
        }
    }

    public void cancel(String id) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                String[] kinds = {K_REMIND, K_BEGIN, K_END};
                for (int i = 0; i < kinds.length; i++) {
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
                    PendingIntent pi = PendingIntent.getBroadcast(ctx, (id + "#" + kinds[i]).hashCode(),
                            new Intent(ctx, ReminderReceiver.class)
                                    .setAction(kinds[i] + ":" + id)
                                    .putExtra("id", id).putExtra("kind", kinds[i]),
                            flags);
                    am.cancel(pi);
                }
            }
            android.app.NotificationManager nm = (android.app.NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(id.hashCode());
        } catch (Exception ignored) {
        }
    }

    public void rescheduleAll() {
        for (Event e : events) {
            cancel(e.id);
            schedule(e);
        }
    }

    /** 闹钟种类：提前提醒 / 开始（弹进行中通知）/ 结束（收掉通知）/ 手动停止 */
    public static final String K_REMIND = "remind";
    public static final String K_BEGIN = "begin";
    public static final String K_END = "end";
    public static final String K_STOP = "stop";

    private PendingIntent piFor(Event e, String kind) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.setAction(kind + ":" + e.id);
        i.putExtra("id", e.id);
        i.putExtra("kind", kind);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, (e.id + "#" + kind).hashCode(), i, flags);
    }

    private void setAlarm(AlarmManager am, long at, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
    }
}

