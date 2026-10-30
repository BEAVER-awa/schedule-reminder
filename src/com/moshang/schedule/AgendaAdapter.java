package com.moshang.schedule;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 日程列表适配器：支持“日期分隔头 + 事件卡片”两种行 */
public class AgendaAdapter extends BaseAdapter {

    public static class Row {
        public int type;      // 0=日期头, 1=事件, 2=周分隔
        public String header;
        public Event ev;
        public static Row head(String h) { Row r = new Row(); r.type = 0; r.header = h; return r; }
        public static Row ev(Event e) { Row r = new Row(); r.type = 1; r.ev = e; return r; }
        public static Row week(String h) { Row r = new Row(); r.type = 2; r.header = h; return r; }
    }

    private final Context ctx;
    private final List<Row> rows = new ArrayList<Row>();
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat md = new SimpleDateFormat("M月d日", Locale.getDefault());
    private final String[] WK = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

    public AgendaAdapter(Context c) { ctx = c; }

    public void setRows(List<Row> r) {
        rows.clear();
        if (r != null) rows.addAll(r);
        notifyDataSetChanged();
    }

    public int getCount() { return rows.size(); }

    public Object getItem(int i) { return rows.get(i); }

    public long getItemId(int i) { return i; }

    private float dp(float v) { return v * ctx.getResources().getDisplayMetrics().density; }

    private int c(int id) { return ctx.getResources().getColor(id); }

    public String dayHeader(long dayMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(dayMillis);
        String s = md.format(new Date(dayMillis)) + "  " + WK[c.get(Calendar.DAY_OF_WEEK) - 1];
        long diff = (Store.dayStart(dayMillis) - Store.dayStart(System.currentTimeMillis())) / Store.DAY;
        if (diff == 0) s += "  ·  今天";
        else if (diff == 1) s += "  ·  明天";
        else if (diff == 2) s += "  ·  后天";
        return s;
    }

    public String weekHeader(long weekStart, boolean current) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(weekStart);
        String s = md.format(new Date(weekStart));
        c.add(Calendar.DAY_OF_MONTH, 6);
        s += " – " + md.format(new Date(c.getTimeInMillis()));
        if (current) s += "   ·  本周";
        return s;
    }

    public String timeText(Event e) {
        if (e.allDay) return "全天";
        return hm.format(new Date(e.start)) + " - " + hm.format(new Date(e.end));
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        Row row = rows.get(i);
        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.VERTICAL);

        if (row.type == 2) {
            outer.setPadding((int) dp(18), (int) dp(20), (int) dp(18), (int) dp(2));
            TextView wt = new TextView(ctx);
            wt.setText(row.header);
            wt.setTextColor(c(R.color.text_secondary));
            wt.setTextSize(12);
            wt.setTypeface(null, Typeface.BOLD);
            outer.addView(wt);
            return outer;
        }

        if (row.type == 0) {
            outer.setPadding((int) dp(20), (int) dp(12), (int) dp(20), (int) dp(4));
            TextView t = new TextView(ctx);
            t.setText(row.header);
            t.setTextColor(c(R.color.text_secondary));
            t.setTextSize(13);
            outer.addView(t);
            return outer;
        }

        outer.setPadding((int) dp(16), (int) dp(5), (int) dp(16), (int) dp(5));

        Event e = row.ev;
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.ripple_card);
        int pad = (int) dp(14);
        card.setPadding(pad, pad, pad, pad);

        View bar = new View(ctx);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams((int) dp(3), LinearLayout.LayoutParams.MATCH_PARENT);
        bar.setLayoutParams(blp);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(e.color);
        gd.setCornerRadius(dp(2));
        bar.setBackground(gd);
        card.addView(bar);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = (int) dp(12);
        col.setLayoutParams(clp);

        TextView title = new TextView(ctx);
        title.setText(e.title);
        title.setTextColor(c(R.color.text_primary));
        title.setTextSize(15);
        title.setTypeface(null, Typeface.BOLD);
        title.setMaxLines(2);
        col.addView(title);

        String sub = timeText(e);
        if (e.location != null && e.location.length() > 0) sub = sub + "   ·   " + e.location;
        TextView st = new TextView(ctx);
        st.setText(sub);
        st.setTextColor(c(R.color.text_secondary));
        st.setTextSize(12);
        st.setFontFeatureSettings("tnum");
        st.setPadding(0, (int) dp(4), 0, 0);
        col.addView(st);

        card.addView(col);
        outer.addView(card);

        outer.setTag(R.id.tag_event_id, e.id);
        outer.setGravity(Gravity.NO_GRAVITY);
        return outer;
    }
}

