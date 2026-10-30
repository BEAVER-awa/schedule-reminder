package com.moshang.schedule;

import java.util.ArrayList;
import java.util.List;

/** 一条日程 */
public class Event {
    public String id = "";
    public String title = "";
    public String location = "";
    public String note = "";
    public String category = "";
    public int color = 0xFF0F77FF;
    public long start = 0L;
    public long end = 0L;
    public boolean allDay = false;
    public int remind = -1; // 提前分钟数，-1 表示不提醒
    public String repeat = "none"; // none/daily/weekly/monthly
    public long until = 0L;        // 重复截止(当天0点)，0=永久
    public List<String> dates = new ArrayList<String>(); // repeat=dates 时的具体日期 yyyy-MM-dd

    public Event() {}

    public Event copy() {
        Event e = new Event();
        e.id = id; e.title = title; e.location = location; e.note = note;
        e.category = category; e.color = color; e.start = start; e.end = end;
        e.allDay = allDay; e.remind = remind; e.repeat = repeat; e.until = until;
        e.dates = new ArrayList<String>(dates);
        return e;
    }
}

