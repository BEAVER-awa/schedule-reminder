package com.moshang.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** 新建 / 编辑日程（含重复规则） */
public class EventEditActivity extends Activity {

    private Store store;
    private float density;

    private Event editing;
    private long startMillis;
    private long endMillis;
    private boolean allDay = false;
    private int remind = -1;
    private String catName = "";
    private int color = Store.PALETTE[0];
    private String repeat = "none";
    private long until = 0L;

    private EditText titleEt, locEt, noteEt;
    private ToggleView allDaySw;
    private TextView startVal, endVal, untilVal;
    private LinearLayout untilRow;
    private TextView remindVal, repeatVal;
    private LinearLayout chipRow;

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("M月d日 EEEE", Locale.CHINA);
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());

    private final String[] REMIND_TEXT = {"不提醒", "准时提醒", "提前5分钟", "提前15分钟", "提前30分钟", "提前1小时"};
    private final int[] REMIND_VAL = {-1, 0, 5, 15, 30, 60};
    private final String[] REPEAT_TEXT = {"不重复", "每天", "每周", "每月"};
    private final String[] REPEAT_VAL = {"none", "daily", "weekly", "monthly"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        density = getResources().getDisplayMetrics().density;

        String id = getIntent().getStringExtra("id");
        if (id != null) editing = store.byId(id);
        if (editing != null) {
            startMillis = editing.start;
            endMillis = editing.end;
            allDay = editing.allDay;
            remind = editing.remind;
            catName = editing.category;
            color = editing.color;
            repeat = editing.repeat == null ? "none" : editing.repeat;
            until = editing.until;
        } else {
            long s = getIntent().getLongExtra("start", 0L);
            if (s == 0L) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                s = c.getTimeInMillis();
            }
            startMillis = s;
            endMillis = s + 3600000L;
            if (store.cats.size() > 0) {
                Category cc = store.cats.get(0);
                catName = cc.name;
                color = cc.color;
            }
        }

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
        TextView cancel = new TextView(this);
        cancel.setText(getString(R.string.cancel));
        cancel.setTextSize(15);
        cancel.setTextColor(Theme.brand(this));
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(dp(12), dp(8), dp(12), dp(8));
        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        Press.attach(cancel);
        TextView t = new TextView(this);
        t.setText(editing != null ? getString(R.string.edit_event) : getString(R.string.new_event));
        t.setTextSize(17);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(c(R.color.text_primary));
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView done = new TextView(this);
        done.setText(getString(R.string.save));
        done.setTextSize(15);
        done.setTypeface(null, Typeface.BOLD);
        done.setTextColor(Theme.brand(this));
        done.setGravity(Gravity.CENTER);
        done.setPadding(dp(12), dp(8), dp(12), dp(8));
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { doSave(); }
        });
        Press.attach(done);
        bar.addView(cancel);
        bar.addView(t);
        bar.addView(done);
        root.addView(bar);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(16), dp(4), dp(16), dp(24));

        titleEt = new EditText(this);
        titleEt.setHint(getString(R.string.title_hint));
        titleEt.setTextSize(16);
        titleEt.setTextColor(c(R.color.text_primary));
        titleEt.setHintTextColor(c(R.color.text_tertiary));
        titleEt.setBackgroundResource(R.drawable.bg_field);
        titleEt.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (editing != null) titleEt.setText(editing.title);
        form.addView(titleEt);

        TextView clab = small(getString(R.string.category));
        form.addView(clab);
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chipRow);
        form.addView(hsv);
        buildChips();

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(16);
        card.setLayoutParams(clp);

        LinearLayout rowAll = rowBase();
        rowAll.addView(label(getString(R.string.all_day)));
        rowAll.addView(spacer());
        allDaySw = new ToggleView(this);
        allDaySw.setOn(allDay);
        allDaySw.setOnChange(new ToggleView.OnChange() {
            public void onChange(boolean checked) {
                allDay = checked;
                syncTimeRows();
            }
        });
        rowAll.addView(allDaySw);
        card.addView(rowAll);
        card.addView(divider());

        LinearLayout rowS = rowBase();
        rowS.addView(label(getString(R.string.start)));
        rowS.addView(spacer());
        startVal = value();
        rowS.addView(startVal);
        rowS.addView(chevron());
        rowS.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pick(true); }
        });
        card.addView(rowS);
        card.addView(divider());

        LinearLayout rowE = rowBase();
        rowE.addView(label(getString(R.string.end)));
        rowE.addView(spacer());
        endVal = value();
        rowE.addView(endVal);
        rowE.addView(chevron());
        rowE.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pick(false); }
        });
        card.addView(rowE);
        form.addView(card);

        // 重复
        LinearLayout rowRep = rowBase();
        rowRep.setBackgroundResource(R.drawable.bg_field);
        LinearLayout.LayoutParams rlp0 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp0.topMargin = dp(12);
        rowRep.setLayoutParams(rlp0);
        rowRep.addView(label(getString(R.string.repeat)));
        rowRep.addView(spacer());
        repeatVal = value();
        rowRep.addView(repeatVal);
        rowRep.addView(chevron());
        rowRep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickRepeat(); }
        });
        form.addView(rowRep);

        untilRow = rowBase();
        untilRow.setBackgroundResource(R.drawable.bg_field);
        LinearLayout.LayoutParams ulp0 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ulp0.topMargin = dp(12);
        untilRow.setLayoutParams(ulp0);
        untilRow.addView(label(getString(R.string.until)));
        untilRow.addView(spacer());
        untilVal = value();
        untilRow.addView(untilVal);
        untilRow.addView(chevron());
        untilRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickUntil(); }
        });
        form.addView(untilRow);

        // 地点
        locEt = new EditText(this);
        locEt.setHint(getString(R.string.location_hint));
        locEt.setTextSize(15);
        locEt.setTextColor(c(R.color.text_primary));
        locEt.setHintTextColor(c(R.color.text_tertiary));
        locEt.setBackgroundResource(R.drawable.bg_field);
        locEt.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (editing != null) locEt.setText(editing.location);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = dp(12);
        locEt.setLayoutParams(llp);
        form.addView(locEt);

        // 提醒
        LinearLayout rowR = rowBase();
        rowR.setBackgroundResource(R.drawable.bg_field);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(12);
        rowR.setLayoutParams(rlp);
        rowR.addView(label(getString(R.string.remind)));
        rowR.addView(spacer());
        remindVal = value();
        rowR.addView(remindVal);
        rowR.addView(chevron());
        rowR.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickRemind(); }
        });
        form.addView(rowR);

        // 备注
        noteEt = new EditText(this);
        noteEt.setHint(getString(R.string.note_hint));
        noteEt.setTextSize(15);
        noteEt.setTextColor(c(R.color.text_primary));
        noteEt.setHintTextColor(c(R.color.text_tertiary));
        noteEt.setBackgroundResource(R.drawable.bg_field);
        noteEt.setPadding(dp(14), dp(12), dp(14), dp(12));
        noteEt.setMinLines(2);
        noteEt.setGravity(Gravity.TOP);
        if (editing != null) noteEt.setText(editing.note);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = dp(12);
        noteEt.setLayoutParams(nlp);
        form.addView(noteEt);

        if (editing != null) {
            LinearLayout delRow = new LinearLayout(this);
            delRow.setOrientation(LinearLayout.HORIZONTAL);
            delRow.setGravity(Gravity.CENTER);
            delRow.setBackgroundResource(R.drawable.bg_field);
            delRow.setPadding(0, dp(14), 0, dp(14));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = dp(24);
            delRow.setLayoutParams(dlp);
            TextView del = new TextView(this);
            del.setText(getString(R.string.delete));
            del.setTextSize(15);
            del.setTextColor(c(R.color.danger));
            delRow.addView(del);
            delRow.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { confirmDelete(); }
            });
            Press.attach(delRow);
            form.addView(delRow);
        }

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(form);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(sv);

        untilRow.setVisibility("none".equals(repeat) ? View.GONE : View.VISIBLE);
        syncTimeRows();
        return root;
    }

    private LinearLayout rowBase() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(12), dp(16), dp(12));
        return r;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTextColor(c(R.color.text_primary));
        return t;
    }

    private TextView value() {
        TextView t = new TextView(this);
        t.setTextSize(15);
        t.setTextColor(c(R.color.text_secondary));
        return t;
    }

    private ImageView chevron() {
        ImageView v = new ImageView(this);
        v.setImageResource(R.drawable.ic_next);
        v.setAlpha(0.28f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(13), dp(13));
        lp.leftMargin = dp(4);
        v.setLayoutParams(lp);
        return v;
    }

    private TextView small(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(c(R.color.text_secondary));
        t.setPadding(0, dp(16), 0, dp(8));
        return t;
    }

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(c(R.color.divider));
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return v;
    }

    private void buildChips() {
        chipRow.removeAllViews();
        for (int i = 0; i < store.cats.size(); i++) {
            final Category cat = store.cats.get(i);
            final boolean on = cat.name.equals(catName);

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(12), dp(8), dp(14), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);

            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(100));
            if (on) {
                g.setColor(withAlpha(cat.color, 0.20f));
                g.setStroke(dp(1), cat.color);
            } else {
                g.setColor(c(R.color.chip_bg));
            }
            chip.setBackground(g);

            View dot = new View(this);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dp(9), dp(9)));
            GradientDrawable dg = new GradientDrawable();
            dg.setShape(GradientDrawable.OVAL);
            dg.setColor(cat.color);
            dot.setBackground(dg);
            chip.addView(dot);

            TextView label = new TextView(this);
            label.setText(cat.name);
            label.setTextSize(13);
            label.setTextColor(on ? c(R.color.text_primary) : c(R.color.text_secondary));
            LinearLayout.LayoutParams llp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp2.leftMargin = dp(7);
            label.setLayoutParams(llp2);
            chip.addView(label);

            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    catName = cat.name;
                    color = cat.color;
                    buildChips();
                }
            });
            Press.attach(chip);
            chipRow.addView(chip);
        }

        TextView more = new TextView(this);
        more.setText("＋");
        more.setTextSize(15);
        more.setGravity(Gravity.CENTER);
        more.setTextColor(c(R.color.text_secondary));
        more.setBackgroundResource(R.drawable.bg_field);
        more.setPadding(dp(13), dp(6), dp(13), dp(6));
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(EventEditActivity.this, CategoryActivity.class));
            }
        });
        Press.attach(more);
        chipRow.addView(more);
    }

    private int withAlpha(int color, float a) {
        return (color & 0x00FFFFFF) | (((int) (255 * a)) << 24);
    }

    private void syncTimeRows() {
        if (allDay) {
            startVal.setText(dateFmt.format(new Date(startMillis)));
            endVal.setText(dateFmt.format(new Date(endMillis)));
        } else {
            startVal.setText(dateFmt.format(new Date(startMillis)) + "  " + timeFmt.format(new Date(startMillis)));
            endVal.setText(dateFmt.format(new Date(endMillis)) + "  " + timeFmt.format(new Date(endMillis)));
        }
        untilVal.setText(until > 0 ? fullFmt.format(new Date(until)) : getString(R.string.until_forever));
        untilVal.setTextColor(c(until > 0 ? R.color.text_primary : R.color.text_tertiary));
        if (remindVal != null) {
            remindVal.setTextColor(c(remind >= 0 ? R.color.text_primary : R.color.text_tertiary));
            remindVal.setText(remindText());
        }
        if (repeatVal != null) {
            repeatVal.setTextColor(c("none".equals(repeat) ? R.color.text_tertiary : R.color.text_primary));
            repeatVal.setText(repeatText());
        }
    }

    private String remindText() {
        for (int i = 0; i < REMIND_VAL.length; i++) if (REMIND_VAL[i] == remind) return REMIND_TEXT[i];
        return REMIND_TEXT[0];
    }

    private String repeatText() {
        for (int i = 0; i < REPEAT_VAL.length; i++) if (REPEAT_VAL[i].equals(repeat)) return REPEAT_TEXT[i];
        return REPEAT_TEXT[0];
    }

    private void pickRemind() {
        int sel = 0;
        for (int i = 0; i < REMIND_VAL.length; i++) if (REMIND_VAL[i] == remind) sel = i;
        new OptionSheet(this, getString(R.string.remind), REMIND_TEXT, sel, new OptionSheet.OnPick() {
            public void onPick(int idx) { remind = REMIND_VAL[idx]; syncTimeRows(); }
        }).showSheet();
    }

    private void pickRepeat() {
        int sel = 0;
        for (int i = 0; i < REPEAT_VAL.length; i++) if (REPEAT_VAL[i].equals(repeat)) sel = i;
        new OptionSheet(this, getString(R.string.repeat), REPEAT_TEXT, sel, new OptionSheet.OnPick() {
            public void onPick(int idx) {
                repeat = REPEAT_VAL[idx];
                if (untilRow != null) untilRow.setVisibility("none".equals(repeat) ? View.GONE : View.VISIBLE);
                syncTimeRows();
            }
        }).showSheet();
    }

    private void pick(final boolean isStart) {
        final long cur = isStart ? startMillis : endMillis;
        new DateSheet(this, cur, new DateSheet.OnDate() {
            public void onDate(final int y, final int m, final int d) {
                Calendar cc = Calendar.getInstance();
                cc.setTimeInMillis(cur);
                cc.set(y, m, d);
                if (allDay) {
                    cc.set(Calendar.HOUR_OF_DAY, 0);
                    cc.set(Calendar.MINUTE, 0);
                    apply(isStart, cc.getTimeInMillis());
                } else {
                    new TimeSheet(EventEditActivity.this,
                            cc.get(Calendar.HOUR_OF_DAY), cc.get(Calendar.MINUTE),
                            new TimeSheet.OnTime() {
                                public void onTime(int hh, int mm) {
                                    Calendar t = Calendar.getInstance();
                                    t.set(y, m, d, hh, mm, 0);
                                    t.set(Calendar.MILLISECOND, 0);
                                    apply(isStart, t.getTimeInMillis());
                                }
                            }).showSheet();
                }
            }
        }).showSheet();
    }

    private void apply(boolean isStart, long ms) {
        if (isStart) {
            long dur = endMillis - startMillis;
            startMillis = ms;
            if (endMillis <= startMillis) endMillis = startMillis + (dur > 0 ? dur : 3600000L);
        } else {
            endMillis = ms;
            if (endMillis < startMillis) endMillis = startMillis;
        }
        syncTimeRows();
    }

    private void pickUntil() {
        final String[] opts = {"永久（不限）", "选择截止日期"};
        new OptionSheet(this, getString(R.string.until), opts, until > 0 ? 1 : 0, new OptionSheet.OnPick() {
            public void onPick(int idx) {
                if (idx == 0) {
                    until = 0L;
                    syncTimeRows();
                } else {
                    new DateSheet(EventEditActivity.this, until > 0 ? until : startMillis, new DateSheet.OnDate() {
                        public void onDate(int y, int m, int d) {
                            Calendar cc = Calendar.getInstance();
                            cc.set(y, m, d);
                            until = Store.dayStart(cc.getTimeInMillis());
                            syncTimeRows();
                        }
                    }).showSheet();
                }
            }
        }).showSheet();
    }

    private void doSave() {
        String title = titleEt.getText().toString().trim();
        if (title.length() == 0) {
            titleEt.setError(getString(R.string.title_required));
            return;
        }
        Event e = editing != null ? editing.copy() : new Event();
        e.title = title;
        e.location = locEt.getText().toString().trim();
        e.note = noteEt.getText().toString().trim();
        e.category = catName;
        e.color = color;
        e.start = startMillis;
        e.end = endMillis;
        e.allDay = allDay;
        e.remind = remind;
        e.repeat = repeat;
        e.until = until;
        if (editing != null) store.update(e);
        else store.add(e);
        finish();
    }

    private void confirmDelete() {
        new ConfirmSheet(this, getString(R.string.delete), getString(R.string.confirm_delete),
                getString(R.string.delete), true, new ConfirmSheet.OnOk() {
            public void onOk() {
                if (editing != null) store.remove(editing.id);
                finish();
            }
        }).showSheet();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}

