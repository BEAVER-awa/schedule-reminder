package com.moshang.schedule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** 把模型返回的 JSON 解析成 Event 列表（容忍 ```json 围栏与前后废话） */
public class AiParser {

    public static class Result {
        public String reply = "";
        public final List<Event> events = new ArrayList<Event>();
        public String error = "";
    }

    public static Result parse(String content, Store store) {
        Result r = new Result();
        String json = extractJson(content);
        if (json == null) {
            r.error = "没能从回复里找到 JSON";
            r.reply = content == null ? "" : content.trim();
            return r;
        }
        try {
            JSONObject o = new JSONObject(json);
            r.reply = o.optString("reply", "");
            JSONArray arr = o.optJSONArray("events");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.optJSONObject(i);
                    if (e == null) continue;
                    Event ev = toEvent(e, store);
                    if (ev != null) r.events.add(ev);
                }
            }
        } catch (Exception ex) {
            r.error = "JSON 解析失败：" + ex.getMessage();
        }
        return r;
    }

    private static String extractJson(String s) {
        if (s == null) return null;
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a < 0 || b <= a) return null;
        return s.substring(a, b + 1);
    }

    private static Event toEvent(JSONObject o, Store store) {
        String title = o.optString("title", "").trim();
        if (title.length() == 0) return null;

        String date = normDate(o.optString("date", ""));
        boolean allDay = o.optBoolean("allDay", false);
        String startS = o.optString("start", "");
        String endS = o.optString("end", "");

        long dayStart = 0L;
        Calendar c = Calendar.getInstance();
        if (date.length() > 0) {
            String[] p = date.split("-");
            if (p.length == 3) {
                c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
                c.set(Calendar.MILLISECOND, 0);
                dayStart = c.getTimeInMillis();
            }
        }
        if (dayStart == 0L) dayStart = Store.dayStart(System.currentTimeMillis());

        Event ev = new Event();
        ev.title = title;
        ev.location = o.optString("location", "");
        ev.note = o.optString("note", "");
        ev.allDay = allDay;

        if (allDay || startS.length() == 0) {
            ev.allDay = true;
            ev.start = dayStart;
            ev.end = dayStart + 24L * 3600000L - 60000L;
        } else {
            ev.start = atTime(dayStart, startS, 9 * 60);
            long e = endS.length() > 0 ? atTime(dayStart, endS, 10 * 60) : ev.start + 3600000L;
            if (e <= ev.start) e = ev.start + 3600000L;
            ev.end = e;
        }

        String rep = o.optString("repeat", "none").toLowerCase(Locale.US);
        if (rep.equals("daily") || rep.equals("weekly") || rep.equals("monthly") || rep.equals("dates")) {
            ev.repeat = rep;
        } else {
            ev.repeat = "none";
        }
        JSONArray ds = o.optJSONArray("dates");
        if (ds != null) {
            for (int i = 0; i < ds.length(); i++) {
                String d = normDate(ds.optString(i, ""));
                if (d.length() > 0) ev.dates.add(d);
            }
        }
        if (ev.repeat.equals("dates") && ev.dates.isEmpty()) ev.repeat = "none";

        String catName = o.optString("category", "").trim();
        Category cat = store == null ? null : store.cat(catName);
        if (cat != null) {
            ev.category = cat.name;
            ev.color = cat.color;
        } else {
            ev.category = catName;
            ev.color = Store.PALETTE[0];
        }
        ev.remind = o.optInt("remind", -1);
        if (ev.remind > 1440) ev.remind = 1440;
        return ev;
    }

    /** 把 "09:00" / "9:00" / "09:00:00" 落到某天的具体毫秒 */
    private static long atTime(long dayStart, String hhmm, int defMinutes) {
        int h = defMinutes / 60, m = defMinutes % 60;
        try {
            String s = hhmm.trim();
            String[] p = s.split(":");
            if (p.length >= 2) {
                h = Integer.parseInt(p[0].trim());
                m = Integer.parseInt(p[1].trim());
            }
        } catch (Exception ignore) {
        }
        if (h < 0) h = 0;
        if (h > 23) h = 23;
        if (m < 0) m = 0;
        if (m > 59) m = 59;
        return dayStart + h * 3600000L + m * 60000L;
    }

    /** 容错：2026-10-30 / 2026/10/30 / 2026.10.30 / 2026-11-2（未补零也能认） */
    private static String normDate(String s) {
        if (s == null) return "";
        s = s.trim().replace('/', '-').replace('.', '-');
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(s);
        if (!m.find()) {
            // 只有 月-日 时补当年
            java.util.regex.Matcher m2 = java.util.regex.Pattern
                    .compile("(?:^|[^0-9])(\\d{1,2})-(\\d{1,2})(?:[^0-9]|$)").matcher(s);
            if (!m2.find()) return "";
            int mo2 = Integer.parseInt(m2.group(1));
            int d2 = Integer.parseInt(m2.group(2));
            if (mo2 < 1 || mo2 > 12 || d2 < 1 || d2 > 31) return "";
            int y2 = Calendar.getInstance().get(Calendar.YEAR);
            return String.format(Locale.US, "%04d-%02d-%02d", y2, mo2, d2);
        }
        int y = Integer.parseInt(m.group(1));
        int mo = Integer.parseInt(m.group(2));
        int d = Integer.parseInt(m.group(3));
        if (mo < 1 || mo > 12 || d < 1 || d > 31) return "";
        return String.format(Locale.US, "%04d-%02d-%02d", y, mo, d);
    }
}
