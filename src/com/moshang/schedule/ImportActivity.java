package com.moshang.schedule;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 内置导入界面：JSON 精确导入 + 正则文本导入（自绘，不用系统风格选择界面） */
public class ImportActivity extends Activity {

    private static final Pattern P_FULL = Pattern.compile("([12][0-9]{3})[-/年.]([0-9]{1,2})[-/月.]([0-9]{1,2})日?");
    private static final Pattern P_SHORT = Pattern.compile("(?<![0-9/])([0-9]{1,2})[/月]([0-9]{1,2})日?(?![0-9])");
    private static final Pattern P_RANGE = Pattern.compile("([0-9]{1,2})[:：]([0-9]{2})[ 　]*[-~—–至到>]+[ 　]*([0-9]{1,2})[:：]([0-9]{2})");
    private static final Pattern P_TIME = Pattern.compile("([0-9]{1,2})[:：]([0-9]{2})");

    private Store store;
    private float density;
    private EditText inputEt;
    private TextView preview;
    private int jsonEvents = -1;
    private List<Event> textEvents;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(buildUi());
    }

    private int dp(float v) { return Math.round(v * density); }

    private int c(int id) { return getResources().getColor(id); }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(c(R.color.window_bg));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(16), 0);
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        ImageView back = new ImageView(this);
        back.setImageResource(R.drawable.ic_back);
        back.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        TextView t = new TextView(this);
        t.setText("导入日程");
        t.setTextSize(18);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(c(R.color.text_primary));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(6);
        t.setLayoutParams(tlp);
        bar.addView(back);
        bar.addView(t);
        root.addView(bar);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(4), dp(16), dp(24));

        TextView tip = new TextView(this);
        tip.setText("支持两种内容：\n· JSON —— 「行程提醒」导出的备份；\n· 文本 —— 每行一条，自动识别日期与时间，例：\n  高数 2026-09-14 10:00-10:50 @SIP-FB346\n  体育 9/15 15:00 操场");
        tip.setTextSize(13);
        tip.setTextColor(c(R.color.text_secondary));
        tip.setLineSpacing(dp(3), 1f);
        form.addView(tip);

        LinearLayout src = new LinearLayout(this);
        src.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(14);
        src.setLayoutParams(slp);
        src.addView(srcBtn(R.drawable.ic_clipboard, "从剪贴板", new View.OnClickListener() {
            public void onClick(View v) { fromClipboard(); }
        }));
        src.addView(srcBtn(R.drawable.ic_file, "选择文件", new View.OnClickListener() {
            public void onClick(View v) { fromFile(); }
        }));
        form.addView(src);

        TextView lab = new TextView(this);
        lab.setText("待导入内容");
        lab.setTextSize(12);
        lab.setTextColor(c(R.color.text_secondary));
        lab.setPadding(0, dp(18), 0, dp(8));
        form.addView(lab);

        inputEt = new EditText(this);
        inputEt.setHint("在此粘贴 JSON 或日程文本…");
        inputEt.setTextSize(13);
        inputEt.setTextColor(c(R.color.text_primary));
        inputEt.setHintTextColor(c(R.color.text_tertiary));
        inputEt.setBackgroundResource(R.drawable.bg_field);
        inputEt.setPadding(dp(14), dp(12), dp(14), dp(12));
        inputEt.setGravity(Gravity.TOP);
        inputEt.setMinLines(6);
        form.addView(inputEt);

        inputEt.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int cc) {}
            public void onTextChanged(CharSequence s, int a, int b, int cc) {}
            public void afterTextChanged(android.text.Editable s) { refreshPreview(); }
        });

        preview = new TextView(this);
        preview.setTextSize(13);
        preview.setTextColor(c(R.color.text_secondary));
        preview.setPadding(dp(2), dp(10), dp(2), 0);
        form.addView(preview);

        TextView go = new TextView(this);
        go.setText("开始导入");
        go.setTextSize(15);
        go.setTypeface(null, Typeface.BOLD);
        go.setGravity(Gravity.CENTER);
        go.setTextColor(0xFFFFFFFF);
        go.setBackgroundResource(R.drawable.bg_btn_brand);
        go.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(this)));
        go.setPadding(0, dp(14), 0, dp(14));
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        glp.topMargin = dp(18);
        go.setLayoutParams(glp);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doImport(); }
        });
        Press.attach(go);
        form.addView(go);

        sv.addView(form);
        root.addView(sv);
        return root;
    }

    private View srcBtn(int icon, String label, View.OnClickListener l) {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundResource(R.drawable.bg_field);
        v.setPadding(dp(8), dp(14), dp(8), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(8);
        v.setLayoutParams(lp);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        v.addView(iv);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(12);
        t.setTextColor(c(R.color.text_primary));
        t.setPadding(0, dp(6), 0, 0);
        v.addView(t);
        v.setOnClickListener(l);
        Press.attach(v);
        return v;
    }

    // ---------------- 预览 / 识别 ----------------
    private void refreshPreview() {
        String s = inputEt.getText().toString().trim();
        jsonEvents = -1;
        textEvents = null;
        if (s.length() == 0) { preview.setText(""); return; }
        if (s.startsWith("{")) {
            try {
                JSONObject o = new JSONObject(s);
                JSONArray ev = o.optJSONArray("events");
                JSONArray ca = o.optJSONArray("cats");
                if (ca == null) ca = o.optJSONArray("categories");
                int n = ev == null ? 0 : ev.length();
                int m = ca == null ? 0 : ca.length();
                jsonEvents = n;
                preview.setTextColor(c(R.color.text_secondary));
                preview.setText("JSON · 识别到 " + n + " 条日程 · " + m + " 个分类");
            } catch (Exception e) {
                jsonEvents = -1;
                preview.setTextColor(c(R.color.danger));
                preview.setText("JSON 格式有误，无法解析");
            }
            return;
        }
        textEvents = parseText(s);
        if (textEvents.isEmpty()) {
            preview.setTextColor(c(R.color.danger));
            preview.setText("没识别出日程：每行一条，需含日期或时间");
        } else {
            int dates = 0;
            for (Event e : textEvents) dates += Math.max(1, e.dates.size());
            preview.setTextColor(c(R.color.text_secondary));
            preview.setText("文本 · 解析到 " + textEvents.size() + " 条日程 · " + dates + " 个日期");
        }
    }

    private List<Event> parseText(String text) {
        List<Event> out = new ArrayList<Event>();
        Calendar now = Calendar.getInstance();
        int defY = now.get(Calendar.YEAR);
        String[] lines = text.split("[\n\r]+");
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li].trim();
            if (line.length() == 0) continue;
            if (line.startsWith("{") || line.startsWith("[")) continue;

            List<int[]> dates = new ArrayList<int[]>();
            Matcher mf = P_FULL.matcher(line);
            while (mf.find()) {
                dates.add(new int[]{Integer.parseInt(mf.group(1)),
                        Integer.parseInt(mf.group(2)) - 1, Integer.parseInt(mf.group(3))});
            }
            if (dates.isEmpty()) {
                Matcher ms = P_SHORT.matcher(line);
                while (ms.find()) {
                    dates.add(new int[]{defY,
                            Integer.parseInt(ms.group(1)) - 1, Integer.parseInt(ms.group(2))});
                }
            }

            int sh = -1, sm = 0, eh = -1, em = 0;
            Matcher mr = P_RANGE.matcher(line);
            if (mr.find()) {
                sh = Integer.parseInt(mr.group(1));
                sm = Integer.parseInt(mr.group(2));
                eh = Integer.parseInt(mr.group(3));
                em = Integer.parseInt(mr.group(4));
            } else {
                Matcher mt = P_TIME.matcher(line);
                if (mt.find()) {
                    sh = Integer.parseInt(mt.group(1));
                    sm = Integer.parseInt(mt.group(2));
                }
            }

            String name = line;
            name = P_FULL.matcher(name).replaceAll(" ");
            name = P_SHORT.matcher(name).replaceAll(" ");
            name = P_RANGE.matcher(name).replaceAll(" ");
            name = P_TIME.matcher(name).replaceAll(" ");
            name = name.replaceAll("[,;、]", " ").trim();
            String loc = "";
            int at = name.indexOf('@');
            if (at < 0) at = name.indexOf('｜');
            if (at < 0) at = name.indexOf('|');
            if (at >= 0) {
                loc = name.substring(at + 1).trim();
                name = name.substring(0, at).trim();
            }
            name = name.replaceAll("[ ]{2,}", " ").trim();
            if (name.length() == 0) name = "未命名日程";

            if (dates.isEmpty()) dates.add(new int[]{now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)});

            Event e = new Event();
            e.title = name;
            e.location = loc;
            e.allDay = (sh < 0);
            if (store.cats.size() > 0) {
                e.category = store.cats.get(0).name;
                e.color = store.cats.get(0).color;
            }
            e.remind = -1;
            e.repeat = "none";

            int[] d0 = dates.get(0);
            Calendar c0 = Calendar.getInstance();
            c0.set(d0[0], d0[1], d0[2], sh < 0 ? 9 : sh, sm, 0);
            c0.set(Calendar.MILLISECOND, 0);
            e.start = c0.getTimeInMillis();
            if (sh < 0) {
                e.end = e.start + 3600000L;
            } else {
                Calendar c1 = Calendar.getInstance();
                c1.set(d0[0], d0[1], d0[2], eh, em, 0);
                c1.set(Calendar.MILLISECOND, 0);
                if (c1.getTimeInMillis() <= e.start) c1.add(Calendar.DAY_OF_MONTH, 1);
                e.end = c1.getTimeInMillis();
            }
            if (dates.size() > 1) {
                e.repeat = "dates";
                for (int i = 0; i < dates.size(); i++) {
                    int[] dd = dates.get(i);
                    e.dates.add(String.format("%04d-%02d-%02d", dd[0], dd[1] + 1, dd[2]));
                }
            }
            out.add(e);
        }
        return out;
    }

    // ---------------- 导入 ----------------
    private void doImport() {
        String s = inputEt.getText().toString().trim();
        if (s.length() == 0) { toast("请先载入内容"); return; }

        if (s.startsWith("{")) {
            if (jsonEvents < 0) refreshPreview();
            if (jsonEvents < 0) { toast("导入失败：JSON 格式有误"); return; }
            try {
                int n = store.importJson(s);
                if (n > 0) { toast("导入成功 +" + n); finish(); }
                else toast("没有新增记录（可能已导入过）");
            } catch (Exception e) {
                toast("导入失败：内容格式有误");
            }
            return;
        }

        List<Event> es = (textEvents != null) ? textEvents : parseText(s);
        if (es.isEmpty()) { toast("没有可导入的日程"); return; }
        for (int i = 0; i < es.size(); i++) store.add(es.get(i));
        toast("导入成功 +" + es.size());
        finish();
    }

    private void fromClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = cm == null ? null : cm.getPrimaryClip();
            if (cd == null || cd.getItemCount() == 0) { toast("剪贴板是空的"); return; }
            CharSequence cs = cd.getItemAt(0).coerceToText(this);
            inputEt.setText(cs == null ? "" : cs.toString());
            toast("已从剪贴板读取");
        } catch (Exception e) {
            toast("读取剪贴板失败");
        }
    }

    private void fromFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, 1002);
        } catch (Exception e) {
            toast("无法打开文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != 1002 || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri uri = data.getData();
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            br.close();
            inputEt.setText(sb.toString());
            toast("已读取文件");
        } catch (Exception e) {
            toast("读取文件失败");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}

