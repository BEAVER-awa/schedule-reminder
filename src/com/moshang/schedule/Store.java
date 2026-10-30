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

    /** 分类色板：低饱和 9 色（calendar-design 技能要求「低饱和、9 色以内」） */
    public static final int[] PALETTE = {
            0xFF3F6BA8, 0xFF3E8C7E, 0xFF6C7A96, 0xFF4E8F8B, 0xFF7A8F76,
            0xFF7F7398, 0xFF6E7A88, 0xFFA87E8E, 0xFFA89052
    };

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
        cats.add(new Category("工作", PALETTE[0]));
        cats.add(new Category("生活", PALETTE[3]));
        cats.add(new Category("学习", PALETTE[4]));
        cats.add(new Category("重要", PALETTE[8]));
        cats.add(new Category("其他", PALETTE[1]));

        long today = dayStart(System.currentTimeMillis());
        events.add(mk("团队周会", today + 10 * H, today + 11 * H, "工作", PALETTE[0], "会议室 A"));
        events.add(mk("健身", today + 18 * H + 30 * M, today + 19 * H + 30 * M, "生活", PALETTE[3], "健身房"));
        events.add(mk("项目复盘", today + 15 * H, today + 16 * H, "工作", PALETTE[1], ""));
        events.add(mk("读书", today + DAY + 9 * H, today + DAY + 10 * H, "学习", PALETTE[4], "图书馆"));
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
        cats.add(new Category(name.trim(), color));
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
                    if (l.size() < 4) l.add(e.color);
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

    public void schedule(Event e) {
        long at = nextFireTime(e);
        if (at <= 0) return;
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = pi(e);
            if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
        } catch (Exception ignored) {
        }
    }

    public void cancel(String id) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, id.hashCode(),
                    new Intent(ctx, ReminderReceiver.class).setAction(id),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        } catch (Exception ignored) {
        }
    }

    public void rescheduleAll() {
        for (Event e : events) {
            cancel(e.id);
            schedule(e);
        }
    }

    private PendingIntent pi(Event e) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.setAction(e.id);
        i.putExtra("id", e.id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, e.id.hashCode(), i, flags);
    }
}

