package com.moshang.schedule;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 和 AI 对话添加日程：说人话 → 解析成日程 → 预览 → 一键写入 */
public class AiChatActivity extends Activity {

    private Store store;
    private AiConfig cfg;
    private float density;

    private LinearLayout bubbles;
    private ScrollView scroll;
    private EditText input;
    private TextView send;

    private final List<AiClient.Msg> history = new ArrayList<AiClient.Msg>();
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        cfg = AiConfig.load(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(build());
        greet();
    }

    private int dp(float v) { return Math.round(v * density); }

    private int c(int id) { return getResources().getColor(id); }

    // ---------------- UI ----------------
    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(c(R.color.window_bg));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(12), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setContentDescription("返回");
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        TextView title = new TextView(this);
        title.setText("AI 添加日程");
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(6);
        title.setLayoutParams(tlp);

        TextView setting = new TextView(this);
        setting.setText("设置");
        setting.setTextSize(13);
        setting.setTextColor(Theme.brand(this));
        setting.setPadding(dp(12), dp(10), dp(12), dp(10));
        setting.setContentDescription("AI 接口设置");
        setting.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new android.content.Intent(AiChatActivity.this, AiSettingsActivity.class));
                overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out);
            }
        });
        Press.attach(setting);

        bar.addView(back);
        bar.addView(title);
        bar.addView(setting);
        root.addView(bar);

        scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        bubbles = new LinearLayout(this);
        bubbles.setOrientation(LinearLayout.VERTICAL);
        bubbles.setPadding(dp(16), dp(8), dp(16), dp(16));
        scroll.addView(bubbles);
        root.addView(scroll);

        // 底部输入
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.BOTTOM);
        bottom.setPadding(dp(12), dp(8), dp(12), dp(12));
        bottom.setBackgroundColor(c(R.color.window_bg));

        input = new EditText(this);
        input.setHint("例：明天上午十点在三教 302 上课，提前 15 分钟提醒");
        input.setHintTextColor(c(R.color.text_tertiary));
        input.setTextColor(c(R.color.text_primary));
        input.setTextSize(14);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setBackgroundResource(R.drawable.bg_field);
        input.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        input.setLayoutParams(ilp);

        send = new TextView(this);
        send.setText("发送");
        send.setTextSize(14);
        send.setTypeface(Typeface.DEFAULT_BOLD);
        send.setGravity(Gravity.CENTER);
        send.setTextColor(0xFFFFFFFF);
        send.setBackgroundResource(R.drawable.bg_btn_brand);
        send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(this)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(64), dp(44));
        slp.leftMargin = dp(8);
        send.setLayoutParams(slp);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSend(); }
        });
        Press.attach(send);

        bottom.addView(input);
        bottom.addView(send);
        root.addView(bottom);
        return root;
    }

    private void greet() {
        if (!cfg.ready()) {
            bubbleSys("还没配置 AI 接口。点右上角「设置」填接口地址、API Key 和模型名，然后就能用自然语言加日程了。");
        } else {
            bubbleSys("试试说：「明天下午三点和组员在图书馆讨论项目，两小时」「每周一三五早上八点跑步」");
        }
    }

    // ---------------- 气泡 ----------------
    private void bubbleUser(String text) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setLayoutParams(margins(0, dp(6), 0, 0));
        wrap.setGravity(Gravity.END);
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(0xFFFFFFFF);
        t.setBackground(round(Theme.brand(this), dp(14)));
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        t.setTextIsSelectable(true);
        wrap.addView(t);
        bubbles.addView(wrap);
        scrollBottom();
    }

    private TextView aiBubble(String text) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setLayoutParams(margins(0, dp(8), 0, 0));
        wrap.setGravity(Gravity.START);
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(c(R.color.text_primary));
        GradientDrawable g = new GradientDrawable();
        g.setColor(c(R.color.surface));
        g.setCornerRadius(dp(14));
        g.setStroke(dp(1), c(R.color.divider));
        t.setBackground(g);
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        t.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.82f));
        t.setTextIsSelectable(true);
        wrap.addView(t);
        bubbles.addView(wrap);
        scrollBottom();
        return t;
    }

    private void bubbleSys(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(c(R.color.text_secondary));
        t.setLineSpacing(dp(3), 1f);
        t.setPadding(dp(4), dp(6), dp(4), dp(6));
        bubbles.addView(t);
        scrollBottom();
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = l;
        lp.topMargin = t;
        lp.rightMargin = r;
        lp.bottomMargin = b;
        return lp;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private void scrollBottom() {
        scroll.post(new Runnable() {
            public void run() { scroll.fullScroll(View.FOCUS_DOWN); }
        });
    }

    // ---------------- 发送 ----------------
    private void doSend() {
        if (busy) return;
        final String text = input.getText().toString().trim();
        if (text.length() == 0) return;
        if (!cfg.ready()) {
            bubbleSys("先点右上角「设置」配置 AI 接口。");
            return;
        }
        input.setText("");
        bubbleUser(text);
        history.add(new AiClient.Msg("user", text));
        trimHistory();

        busy = true;
        send.setAlpha(0.5f);
        final TextView thinking = aiBubble("正在思考…");

        new Thread(new Runnable() {
            public void run() {
                String reply;
                String err = null;
                try {
                    reply = AiClient.chat(cfg, systemPrompt(), history);
                } catch (Exception ex) {
                    reply = null;
                    err = ex.getMessage();
                }
                final String fReply = reply;
                final String fErr = err;
                runOnUiThread(new Runnable() {
                    public void run() {
                        busy = false;
                        send.setAlpha(1f);
                        bubbles.removeView((View) thinking.getParent());
                        if (fErr != null) {
                            bubbleSys("调用失败：" + fErr);
                            return;
                        }
                        history.add(new AiClient.Msg("assistant", fReply));
                        trimHistory();
                        AiParser.Result r = AiParser.parse(fReply, store);
                        String say = r.reply == null || r.reply.length() == 0 ? "我已理解你的安排。" : r.reply;
                        aiBubble(say);
                        if (r.error.length() > 0) {
                            bubbleSys(r.error);
                        }
                        if (!r.events.isEmpty()) {
                            showPreview(r.events);
                        }
                    }
                });
            }
        }).start();
    }

    private void trimHistory() {
        while (history.size() > 10) history.remove(0);
    }

    private void showPreview(final List<Event> events) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setLayoutParams(margins(0, dp(10), 0, dp(4)));

        TextView head = new TextView(this);
        head.setText("识别到 " + events.size() + " 条日程");
        head.setTextSize(13);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setTextColor(c(R.color.text_primary));
        head.setPadding(0, 0, 0, dp(8));
        card.addView(head);

        for (Event e : events) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));

            View dot = new View(this);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dp(8), dp(8)));
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.OVAL);
            gd.setColor(e.color);
            dot.setBackground(gd);

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            clp.leftMargin = dp(10);
            col.setLayoutParams(clp);

            TextView t1 = new TextView(this);
            t1.setText(e.title);
            t1.setTextSize(14);
            t1.setTextColor(c(R.color.text_primary));

            TextView t2 = new TextView(this);
            t2.setText(fmtWhen(e));
            t2.setTextSize(12);
            t2.setTextColor(c(R.color.text_secondary));
            t2.setFontFeatureSettings("tnum");

            col.addView(t1);
            col.addView(t2);

            row.addView(dot);
            row.addView(col);
            card.addView(row);
        }

        TextView add = new TextView(this);
        add.setText("添加到日程");
        add.setTextSize(14);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setGravity(Gravity.CENTER);
        add.setTextColor(0xFFFFFFFF);
        add.setBackgroundResource(R.drawable.bg_btn_brand);
        add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(this)));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        alp.topMargin = dp(10);
        add.setLayoutParams(alp);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Event e : events) store.add(e);
                Toast.makeText(AiChatActivity.this, "已添加 " + events.size() + " 条日程", Toast.LENGTH_SHORT).show();
                v.setEnabled(false);
                ((TextView) v).setText("已添加");
                ((TextView) v).setBackground(round(c(R.color.segment_bg), dp(13)));
                ((TextView) v).setTextColor(c(R.color.text_secondary));
            }
        });
        Press.attach(add);
        card.addView(add);

        bubbles.addView(card);
        scrollBottom();
    }

    private String fmtWhen(Event e) {
        SimpleDateFormat md = new SimpleDateFormat("M月d日", Locale.getDefault());
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (e.allDay) return md.format(new Date(e.start)) + " · 全天";
        String s = md.format(new Date(e.start)) + "  " + hm.format(new Date(e.start)) + "-" + hm.format(new Date(e.end));
        if (e.remind >= 0) s += e.remind == 0 ? " · 准时提醒" : " · 提前" + e.remind + "分钟";
        if ("weekly".equals(e.repeat)) s += " · 每周";
        else if ("daily".equals(e.repeat)) s += " · 每天";
        else if ("monthly".equals(e.repeat)) s += " · 每月";
        else if ("dates".equals(e.repeat)) s += " · 共 " + e.dates.size() + " 天";
        return s;
    }

    // ---------------- 提示词 ----------------
    private String systemPrompt() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat wf = new SimpleDateFormat("EEEE", Locale.CHINA);
        StringBuilder cats = new StringBuilder();
        for (int i = 0; i < store.cats.size(); i++) {
            if (i > 0) cats.append("、");
            cats.append(store.cats.get(i).name);
        }
        return "你是手机日程助手的解析器。把用户的话变成日程，只输出一个 JSON 对象，不要解释、不要代码块标记。\n"
                + "今天：" + df.format(now.getTime()) + " " + wf.format(now.getTime())
                + "，当前时间 " + new SimpleDateFormat("HH:mm", Locale.US).format(now.getTime()) + "。\n"
                + "输出结构：\n"
                + "{\"reply\":\"一句话自然回应\",\"events\":[{\"title\":\"\",\"date\":\"YYYY-MM-DD\",\"start\":\"HH:MM\","
                + "\"end\":\"HH:MM\",\"allDay\":false,\"location\":\"\",\"note\":\"\",\"repeat\":\"none\","
                + "\"dates\":[],\"category\":\"\",\"remind\":-1}]}\n"
                + "规则：\n"
                + "1. 相对日期必须换算成具体日期（明天、下周三、这周五都能算）。\n"
                + "2. 没给结束时间默认 1 小时；说「全天」「一整天」时 allDay=true，start/end 留空字符串。\n"
                + "3. repeat 只能是 none/daily/weekly/monthly/dates；说出多个具体日期时用 dates 并填 dates 数组（YYYY-MM-DD）。\n"
                + "4. category 从这些里挑最合适的一个：" + cats + "；都不合适就留空。\n"
                + "5. 用户只是在聊天、没有新增安排时，events 返回空数组 []。\n"
                + "6. 用户要求修改刚才的日程时，输出修改后的完整 events。\n"
                + "7. remind 是提前多少分钟提醒：说「提前15分钟」就填 15，说「准时提醒」填 0，没提就不填或填 -1。";
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}
