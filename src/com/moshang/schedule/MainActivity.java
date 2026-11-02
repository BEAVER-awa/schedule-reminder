package com.moshang.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

/** 主界面（vivo/OriginOS 风格）：五段视图胶囊 + 月历(农历) + 信息条 + 右下「今 / ＋」 */
public class MainActivity extends Activity {

    private Store store;
    private float density;
    private static final String[] WK1 = {"日", "一", "二", "三", "四", "五", "六"};

    private TitleBar title;
    private TextView eyebrow;
    private ImageView prev, next;
    private SegmentBar segBar;
    private ImageView icon, search, more;
    private ImageView collapseBtn;
    private View headerWrap, segWrap;
    private int headH = -1, segH = -1;
    private boolean topCollapsed = false;
    private final String[] SEG_NAMES = {"年", "月", "周", "日", "日程"};

    private LinearLayout monthPage, weekPage, dayPage;
    private FrameLayout agendaWrap;
    private SwipePanel monthSwipe;
    private int monthRowTop;
    private LinearLayout monthRows;
    private ListView agendaList;
    private TextView monthEmpty;
    private AdapterView.OnItemClickListener clicker;
    private WeekTimelineView weekTimeline;
    private ScrollView weekScroll;
    private MonthView monthView;
    private YearMonthSheet ymSheet;
    private TextView infoBar;
    private LinearLayout weekStrip;
    private DayView dayView;
    private ScrollView dayScroll;
    private TextView dayHeader;
    private ScrollView yearScroll;
    private YearView yearView;

    private AgendaAdapter monthAdapter, agendaAdapter;

    private int tab = 1; // 0年 1月 2周 3日 4日程（默认月）
    private int year, month;
    private long selectedMillis;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = Store.get(this);
        density = getResources().getDisplayMetrics().density;
        Calendar now = Calendar.getInstance();
        year = now.get(Calendar.YEAR);
        month = now.get(Calendar.MONTH);
        selectedMillis = Store.dayStart(System.currentTimeMillis());

        createChannel();
        setContentView(buildUi());
        setTab(1);
        requestNotifPerm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private int dp(float v) { return Math.round(v * density); }

    /** 主题淡底（用于农历信息条等大面积色块） */
    private android.graphics.drawable.GradientDrawable washBg() {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(Theme.wash(this));
        return g;
    }

    private int c(int id) { return getResources().getColor(id); }

    // ---------------- UI ----------------
    private View buildUi() {
        FrameLayout top = new FrameLayout(this);
        top.setBackgroundColor(c(R.color.window_bg));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        headerWrap = buildHeader();
        root.addView(headerWrap);
        segWrap = buildSegment();
        root.addView(segWrap);

        FrameLayout content = new FrameLayout(this);
        content.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 年
        yearScroll = new ScrollView(this);
        yearView = new YearView(this);
        yearView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        yearView.setOnMonthPick(new YearView.OnMonthPick() {
            public void onPick(int y, int m) {
                year = y;
                month = m;
                setTab(1);
            }
        });
        yearScroll.addView(yearView);

        // 月
        monthPage = new LinearLayout(this);
        monthPage.setOrientation(LinearLayout.VERTICAL);
        monthView = new MonthView(this);
        monthView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        monthView.setDayClick(new MonthView.OnDayClick() {
            public void onDay(int y, int m, int d) { selectDay(y, m, d); }
        });
        monthView.setMonthChange(new MonthView.OnMonthChange() {
            public void onMonth(int y, int m) { onMonthChanged(y, m); }
        });
        monthView.setDotSource(new MonthView.DotSource() {
            public java.util.HashMap<Integer, java.util.List<Integer>> dotsFor(int y, int m) {
                return store.monthDots(y, m);
            }
        });
        infoBar = new TextView(this);
        infoBar.setTextSize(12);
        infoBar.setTextColor(c(R.color.infobar_text));
        infoBar.setBackground(washBg());
        infoBar.setPadding(dp(16), dp(9), dp(16), dp(9));
        infoBar.setSingleLine(true);
        infoBar.setEllipsize(android.text.TextUtils.TruncateAt.END);


        // 月历与农历条固定不动；日程面板贴在其下方，手指在面板上上滑时整块上移盖住月历。
        monthSwipe = new SwipePanel(this);
        monthSwipe.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        monthSwipe.addFixed(monthView);
        // 农历条属于「跟着卡片走」的那一层：固定在顶层会被面板顶上来压住卡片
        monthSwipe.addHead(infoBar);

        monthRows = new LinearLayout(this);
        monthRows.setOrientation(LinearLayout.VERTICAL);
        monthRows.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        monthSwipe.getPanel().addView(monthRows);
        // 面板每移动一点，月历内部就同步上移一点；到选中行顶部为止，
        // 于是滑到底时屏幕上只剩选中那一行
        monthSwipe.setOnMove(new SwipePanel.OnMove() {
            public void onMove(float off, float calShift) {
                monthView.setGridShift(calShift);
            }
        });

        monthEmpty = new TextView(this);
        monthEmpty.setText(getString(R.string.no_events));
        monthEmpty.setTextSize(15);
        monthEmpty.setTextColor(c(R.color.text_tertiary));
        monthEmpty.setGravity(Gravity.CENTER);
        monthEmpty.setPadding(0, dp(64), 0, dp(64));
        monthEmpty.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        monthEmpty.setVisibility(View.GONE);
        monthSwipe.getPanel().addView(monthEmpty);

        monthPage.addView(monthSwipe);

        // 周
        weekPage = new LinearLayout(this);
        weekPage.setOrientation(LinearLayout.VERTICAL);
        weekStrip = new LinearLayout(this);
        weekStrip.setOrientation(LinearLayout.HORIZONTAL);
        weekStrip.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        weekStrip.setPadding(dp(8), dp(8), dp(8), dp(8));
        weekPage.addView(weekStrip);
        weekTimeline = new WeekTimelineView(this);
        weekTimeline.setOnEventClick(new WeekTimelineView.OnEventClick() {
            public void onClick(String id) { openEdit(id); }
        });
        weekScroll = new ScrollView(this);
        weekScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        weekScroll.setFillViewport(true);
        weekScroll.addView(weekTimeline);
        weekPage.addView(weekScroll);

        // 日
        dayPage = new LinearLayout(this);
        dayPage.setOrientation(LinearLayout.VERTICAL);
        dayHeader = new TextView(this);
        dayHeader.setTextSize(13);
        dayHeader.setTextColor(c(R.color.text_secondary));
        dayHeader.setPadding(dp(20), dp(8), dp(20), dp(4));
        dayPage.addView(dayHeader);
        dayView = new DayView(this);
        dayView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        dayView.setOnEventClick(new DayView.OnEventClick() {
            public void onClick(String id) { openEdit(id); }
        });
        dayScroll = new ScrollView(this);
        dayScroll.addView(dayView);
        dayScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        dayPage.addView(dayScroll);

        // 日程
        agendaWrap = new FrameLayout(this);
        agendaList = new ListView(this);
        agendaList.setDivider(null);
        agendaWrap.addView(agendaList, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView ae = emptyView();
        agendaWrap.addView(ae, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        agendaList.setEmptyView(ae);

        content.addView(yearScroll, full());
        content.addView(monthPage, full());
        content.addView(weekPage, full());
        content.addView(dayPage, full());
        content.addView(agendaWrap, full());

        root.addView(content);
        top.addView(root);

        collapseBtn = new ImageView(this);
        collapseBtn.setImageResource(R.drawable.ic_collapse);
        collapseBtn.setContentDescription("收起或展开视图栏");
        collapseBtn.setScaleType(ImageView.ScaleType.CENTER);
        FrameLayout.LayoutParams cbp = new FrameLayout.LayoutParams(dp(32), dp(32));
        cbp.gravity = Gravity.TOP | Gravity.END;
        cbp.topMargin = dp(15);
        cbp.rightMargin = dp(10);
        top.addView(collapseBtn, cbp);
        collapseBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleTop(); }
        });
        Press.attach(collapseBtn);

        // 右下：今 + ＋
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.gravity = Gravity.BOTTOM | Gravity.END;
        blp.rightMargin = dp(20);
        blp.bottomMargin = dp(24);
        bottom.setLayoutParams(blp);

        TextView todayBtn = new TextView(this);
        todayBtn.setText("今");
        todayBtn.setTextSize(15);
        todayBtn.setTypeface(null, Typeface.BOLD);
        todayBtn.setTextColor(Theme.brand(this));
        todayBtn.setGravity(Gravity.CENTER);
        todayBtn.setBackgroundResource(R.drawable.bg_circle);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(48), dp(48));
        tlp.bottomMargin = dp(12);
        todayBtn.setLayoutParams(tlp);
        todayBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { goToday(); }
        });
        Press.attach(todayBtn);
        bottom.addView(todayBtn);

        ImageView fab = new ImageView(this);
        fab.setImageResource(R.drawable.ic_add);
        fab.setContentDescription("新建行程");
        fab.setBackgroundResource(R.drawable.bg_fab);
        fab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.brand(this)));
        fab.setScaleType(ImageView.ScaleType.CENTER);
        fab.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        fab.setElevation(dp(6));
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openNew(); }
        });
        Press.attach(fab);
        bottom.addView(fab);
        top.addView(bottom);

        monthAdapter = new AgendaAdapter(this);
        agendaAdapter = new AgendaAdapter(this);
        agendaList.setAdapter(agendaAdapter);

        clicker = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = p.getAdapter().getItem(pos);
                if (o instanceof AgendaAdapter.Row) {
                    AgendaAdapter.Row r = (AgendaAdapter.Row) o;
                    if (r.type == 1) openEdit(r.ev.id);
                }
            }
        };
        agendaList.setOnItemClickListener(clicker);

        return top;
    }

    private FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private TextView emptyView() {
        TextView t = new TextView(this);
        t.setText(getString(R.string.no_events));
        t.setTextColor(c(R.color.text_tertiary));
        t.setTextSize(15);
        t.setGravity(Gravity.CENTER);
        t.setVisibility(View.GONE);
        return t;
    }

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(dp(16), dp(6), dp(42), 0);
        h.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_cal);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));

        eyebrow = new TextView(this);
        eyebrow.setText("行程提醒");
        eyebrow.setTextSize(10.5f);
        eyebrow.setLetterSpacing(0.14f);
        eyebrow.setTypeface(null, Typeface.BOLD);
        eyebrow.setTextColor(Theme.accent(this));
        eyebrow.setIncludeFontPadding(false);

        title = new TitleBar(this);
        title.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.leftMargin = dp(8);
        titleCol.setLayoutParams(colLp);
        titleCol.addView(eyebrow);
        titleCol.addView(title);
        title.setOnAction(new TitleBar.OnAction() {
            public void onTap() { }
            public void onSwipe(int dir) { step(dir); }
        });

        search = new ImageView(this);
        search.setImageResource(R.drawable.ic_search);
        search.setContentDescription("搜索行程");
        search.setScaleType(ImageView.ScaleType.CENTER);
        search.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openSearch(); }
        });
        Press.attach(search);

        prev = new ImageView(this);
        prev.setImageResource(R.drawable.ic_back);
        prev.setContentDescription("上一页");
        prev.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        prev.setScaleType(ImageView.ScaleType.CENTER);
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { step(-1); }
        });

        next = new ImageView(this);
        next.setImageResource(R.drawable.ic_next);
        next.setContentDescription("下一页");
        next.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        next.setScaleType(ImageView.ScaleType.CENTER);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { step(1); }
        });

        more = new ImageView(this);
        more.setImageResource(R.drawable.ic_more);
        more.setContentDescription("更多操作");
        more.setScaleType(ImageView.ScaleType.CENTER);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(dp(40), dp(40));
        mlp.leftMargin = dp(4);
        more.setLayoutParams(mlp);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showMore(v); }
        });
        Press.attach(prev);
        Press.attach(next);
        Press.attach(more);

        h.addView(icon);
        h.addView(titleCol);
        h.addView(search);
        h.addView(prev);
        h.addView(next);
        h.addView(more);
        return h;
    }

    private View buildSegment() {
        LinearLayout outer = new LinearLayout(this);
        outer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        outer.setPadding(dp(16), dp(6), dp(16), dp(10));

        segBar = new SegmentBar(this);
        segBar.setNames(SEG_NAMES);
        segBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        segBar.setOnSegment(new SegmentBar.OnSegment() {
            public void onSeg(int i) { setTab(i); }
        });
        outer.addView(segBar);
        return outer;
    }

    /** 收起/展开：只把「年 月 周 日 日程」视图标签栏往上收走，
     *  最顶部的年月标题行与右上角按钮一动不动 */
    private void toggleTop() {
        topCollapsed = !topCollapsed;
        collapseBtn.animate().rotation(topCollapsed ? 180f : 0f).setDuration(200)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                        this, R.interpolator.press_interp)).start();
        if (segWrap == null) return;
        if (topCollapsed) {
            segH = segWrap.getHeight();
            animHeight(segWrap, segH, 0);
        } else {
            animHeight(segWrap, 0, segH > 0 ? segH : dp(56));
        }
    }

    /** 高度动画（收起/展开用）；展开结束后恢复 wrap_content 自适应 */
    private void animHeight(final View v, int from, int to) {
        final boolean expanding = to > from;
        android.animation.ValueAnimator a = android.animation.ValueAnimator.ofInt(from, to);
        a.setDuration(expanding ? 200 : 300);
        a.setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(
                this, expanding ? R.interpolator.vivo_enter : R.interpolator.vivo_exit));
        a.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(android.animation.ValueAnimator an) {
                int hv = ((Integer) an.getAnimatedValue()).intValue();
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = hv;
                v.setLayoutParams(lp);
            }
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            public void onAnimationEnd(android.animation.Animator an) {
                if (!topCollapsed) {
                    ViewGroup.LayoutParams lp = v.getLayoutParams();
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    v.setLayoutParams(lp);
                }
            }
        });
        a.start();
    }

    // ---------------- 行为 ----------------
    private void setTab(int i) {
        int oldTab = tab;
        tab = i;
        segBar.setSelected(i, true);
        yearScroll.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        monthPage.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        weekPage.setVisibility(i == 2 ? View.VISIBLE : View.GONE);
        dayPage.setVisibility(i == 3 ? View.VISIBLE : View.GONE);
        agendaWrap.setVisibility(i == 4 ? View.VISIBLE : View.GONE);
        boolean nav = (i != 4);
        prev.setVisibility(nav ? View.VISIBLE : View.INVISIBLE);
        next.setVisibility(nav ? View.VISIBLE : View.INVISIBLE);
        updateTitle();
        refreshAll();
        animatePage(oldTab, i);
    }

    private void animatePage(int from, int to) {
        android.view.View page = to == 0 ? (android.view.View) yearScroll : to == 1 ? monthPage : to == 2 ? weekPage : to == 3 ? dayPage : agendaWrap;
        page.setTranslationX(0f);
        page.setAlpha(0f);
        page.animate().alpha(1f).setDuration(150)
                .setInterpolator(android.view.animation.AnimationUtils.loadInterpolator(this, R.interpolator.vivo_enter))
                .start();
    }

    private void step(int dir) {
        if (tab == 0) {
            year += dir;
            refreshAll();
        } else if (tab == 2) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedMillis);
            c.add(Calendar.DAY_OF_MONTH, dir * 7);
            selectedMillis = Store.dayStart(c.getTimeInMillis());
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            refreshAll();
        } else if (tab == 3) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedMillis);
            c.add(Calendar.DAY_OF_MONTH, dir);
            selectedMillis = Store.dayStart(c.getTimeInMillis());
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            refreshAll();
        } else {
            monthView.shiftMonthAnimated(dir);
        }
        updateTitle();
    }

    private void onMonthChanged(int y, int m) {
        year = y;
        month = m;
        // 翻月只切换视图，不改动选中日期：
        // 之前这里会把选中日「搬」到新月份的同一天号，导致选中高亮跟着手指跑
        refreshAll();
        updateTitle();
    }

    private void selectDay(int y, int m, int d) {
        Calendar c = Calendar.getInstance();
        c.set(y, m, d);
        selectedMillis = Store.dayStart(c.getTimeInMillis());
        year = y;
        month = m;
        updateTitle();
        refreshAll();
    }

    private void goToday() {
        Calendar now = Calendar.getInstance();
        final int ty = now.get(Calendar.YEAR);
        final int tm = now.get(Calendar.MONTH);
        final int td = now.get(Calendar.DAY_OF_MONTH);
        selectedMillis = Store.dayStart(System.currentTimeMillis());
        // 当前已经是今天所在月时，goToMonthAnimated 不会回调 onMonthChanged，
        // 必须自己补一次刷新，否则下方行程仍停在旧日期
        final boolean sameMonth = (year == ty && month == tm);
        year = ty;
        month = tm;
        if (tab == 0) { setTab(1); return; }   // setTab 内部已 refreshAll
        if (tab == 1) {
            monthView.setSelected(ty, tm, td);
            monthView.goToMonthAnimated(ty, tm);
            if (sameMonth) {
                refreshAll();
                updateTitle();
            }
        } else {
            refreshAll();
            updateTitle();
        }
    }

    private void showMore(View anchor) {
        final String[] opts = {"AI 添加日程", getString(R.string.menu_export),
                getString(R.string.menu_import), "分类管理", "主题颜色", getString(R.string.menu_about)};
        new OptionSheet(this, null, opts, -1, new OptionSheet.OnPick() {
            public void onPick(int idx) {
                if (idx == 0) openPage(AiChatActivity.class);
                else if (idx == 1) doExport();
                else if (idx == 2) startActivity(new Intent(MainActivity.this, ImportActivity.class));
                else if (idx == 3) openPage(CategoryActivity.class);
                else if (idx == 4) pickTheme();
                else about();
            }
        }).showSheet();
    }

    /** 主题色选择：9 套主色，跟随深浅色自动调明度 */
    private void pickTheme() {
        new OptionSheet(this, "主题颜色", Theme.NAMES, Theme.key(this), new OptionSheet.OnPick() {
            public void onPick(int idx) {
                if (idx == Theme.key(MainActivity.this)) return;
                Theme.setKey(MainActivity.this, idx);
                recreate();
            }
        }).showSheet();
    }

    /** 二级页面统一转场 */
    private void openPage(Class<?> cls) {
        startActivity(new Intent(this, cls));
        overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out);
    }

    private void about() {
        String ver = "";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
        String body = "版本 " + ver + "\n"
                + "日程数据仅保存在本机，不会上传服务器\n"
                + "AI 添加日程使用你自己填写的接口，聊天内容直接发送给该服务商\n"
                + "支持：年 / 月 / 周 / 日视图、重复日程、分类管理、导入导出、桌面小组件\n"
                + "开源许可：GNU General Public License v3.0";
        new ConfirmSheet(this, getString(R.string.app_name), body,
                getString(R.string.ok), false, null).showSheet();
    }

    private void openNew() {
        Intent i = new Intent(this, EventEditActivity.class);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedMillis);
        Calendar now = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, 0);
        i.putExtra("start", c.getTimeInMillis());
        startActivity(i);
        overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out);
    }

    private void openEdit(String id) {
        Intent i = new Intent(this, EventEditActivity.class);
        i.putExtra("id", id);
        startActivity(i);
        overridePendingTransition(R.anim.activity_right_in, R.anim.activity_right_out);
    }

    // ---------------- 刷新 ----------------
    private String mdShort(long ms) {
        Calendar c2 = Calendar.getInstance();
        c2.setTimeInMillis(ms);
        return (c2.get(Calendar.MONTH) + 1) + "/" + c2.get(Calendar.DAY_OF_MONTH);
    }

    /** 顶部年月按钮：点一次弹出，再点一次收回 */
    private void toggleYearMonth() {
        if (ymSheet != null && ymSheet.isShowing()) {
            ymSheet.closeAnimated();
            return;
        }
        title.setExpanded(true);
        ymSheet = new YearMonthSheet(this, year, month, new YearMonthSheet.OnPick() {
            public void onPick(int y, int m) { jumpTo(y, m); }
        });
        ymSheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) { title.setExpanded(false); }
        });
        ymSheet.showSheet();
    }

    private void jumpTo(int y, int m) {
        if (tab != 1) setTab(1);
        year = y;
        month = m;
        if (monthView.getYear() == y && monthView.getMonth() == m) {
            refreshAll();
            updateTitle();
        } else {
            monthView.goToMonthAnimated(y, m);
        }
    }

    private void openSearch() {
        startActivity(new Intent(this, SearchActivity.class));
    }

    private void updateTitle() {
        if (tab == 0) {
            title.setTitle(year + "年", "");
        } else if (tab == 2) {
            long[] wk = weekRange();
            title.setTitle(mdShort(wk[0]) + " - " + mdShort(wk[1]), "");
        } else if (tab == 3) {
            title.setTitle(shortDay(selectedMillis), "");
        } else if (tab == 4) {
            title.setTitle("日程", "");
        } else {
            title.setTitle(year + "/", (month + 1 < 10 ? "0" : "") + (month + 1));
        }
    }

    private long[] weekRange() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedMillis);
        int idx = c.get(Calendar.DAY_OF_WEEK) - 1; // 周日为 0
        c.add(Calendar.DAY_OF_MONTH, -idx);
        long a = c.getTimeInMillis();
        c.add(Calendar.DAY_OF_MONTH, 6);
        return new long[]{a, c.getTimeInMillis()};
    }

    private String shortDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return (c.get(Calendar.MONTH) + 1) + "月" + c.get(Calendar.DAY_OF_MONTH) + "日";
    }

    /** 把当天日程渲染进可滚动的容器（复用 AgendaAdapter 的行样式） */
    private void renderMonthAgenda() {
        if (monthAdapter == null || monthRows == null) return;
        monthRows.removeAllViews();
        int n = monthAdapter.getCount();
        if (monthEmpty != null) {
            monthEmpty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        }
        for (int i = 0; i < n; i++) {
            View v = monthAdapter.getView(i, null, monthRows);
            Object o = monthAdapter.getItem(i);
            if (o instanceof AgendaAdapter.Row && ((AgendaAdapter.Row) o).type == 1) {
                final String id = ((AgendaAdapter.Row) o).ev.id;
                v.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View x) { openEdit(id); }
                });
            }
            monthRows.addView(v);
        }
        monthSwipe.refreshPanel();
    }

    private void refreshAll() {
        if (tab == 0) {
            yearView.setYear(year);
            yearView.setMarked(yearMarked(year));
        } else if (tab == 1) {
            monthView.setMonth(year, month);
            Calendar sc = Calendar.getInstance();
            sc.setTimeInMillis(selectedMillis);
            monthView.setSelected(sc.get(Calendar.YEAR), sc.get(Calendar.MONTH), sc.get(Calendar.DAY_OF_MONTH));
            monthView.setDots(store.monthDots(year, month));
            monthAdapter.setRows(dayRows(store.onDay(selectedMillis)));
            renderMonthAgenda();
            infoBar.setText(infoText(sc));
            // 上滑到底时只留选中日期那一行：月历内部按行裁切，面板顶停在行底
            monthRowTop = monthView.selectedRowTop();
            monthSwipe.setKeepTop(getResources().getDimension(R.dimen.month_cell_height));
            monthSwipe.setCalMax(monthRowTop);
        } else if (tab == 2) {
            buildWeekStrip();
            weekTimeline.setDay(selectedMillis);
            scrollWeekToNow();
        } else if (tab == 3) {
            dayHeader.setText(dateday(selectedMillis));
            dayView.setEvents(store.onDay(selectedMillis));
            final int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            dayScroll.post(new Runnable() {
                public void run() {
                    dayScroll.scrollTo(0, Math.max(0, (hr - 1) * dp(56)));
                }
            });
        } else {
            agendaAdapter.setRows(agendaRows());
        }
    }

    private String infoText(Calendar sc) {
        int y = sc.get(Calendar.YEAR);
        int m = sc.get(Calendar.MONTH) + 1;
        int d = sc.get(Calendar.DAY_OF_MONTH);
        if (y < 1900 || y > 2100) return "";
        long diff = (Store.dayStart(sc.getTimeInMillis()) - Store.dayStart(System.currentTimeMillis())) / Store.DAY;
        String pre = "";
        if (diff > 0) pre = diff + "天后  ";
        else if (diff < 0) pre = (-diff) + "天前  ";
        return pre + Lunar.infoLine(y, m, d) + "    ›";
    }

    private String dateday(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return (c.get(Calendar.MONTH) + 1) + "月" + c.get(Calendar.DAY_OF_MONTH) + "日  " + WK1[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private List<AgendaAdapter.Row> dayRows(List<Event> evs) {
        List<AgendaAdapter.Row> rows = new ArrayList<AgendaAdapter.Row>();
        for (Event e : evs) rows.add(AgendaAdapter.Row.ev(e));
        return rows;
    }

    private static long weekStartOf(long day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Store.dayStart(day));
        int idx = c.get(Calendar.DAY_OF_WEEK) - 1; // 周日为 0
        c.add(Calendar.DAY_OF_MONTH, -idx);
        return Store.dayStart(c.getTimeInMillis());
    }

    private List<AgendaAdapter.Row> agendaRows() {
        List<AgendaAdapter.Row> rows = new ArrayList<AgendaAdapter.Row>();
        long lastDay = -1, lastWeek = -1;
        long thisWeek = weekStartOf(System.currentTimeMillis());
        for (Event e : store.upcoming()) {
            long d = Store.dayStart(e.start);
            long wk = weekStartOf(d);
            if (wk != lastWeek) {
                rows.add(AgendaAdapter.Row.week(agendaAdapter.weekHeader(wk, wk == thisWeek)));
                lastWeek = wk;
                lastDay = -1;
            }
            if (d != lastDay) {
                rows.add(AgendaAdapter.Row.head(agendaAdapter.dayHeader(d)));
                lastDay = d;
            }
            rows.add(AgendaAdapter.Row.ev(e));
        }
        return rows;
    }

    private HashSet<Long> yearMarked(int y) {
        HashSet<Long> set = new HashSet<Long>();
        Calendar c = Calendar.getInstance();
        c.set(y, 0, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i < 400; i++) {
            if (c.get(Calendar.YEAR) != y) break;
            long d = c.getTimeInMillis();
            for (Event e : store.events) {
                if (Store.occursOn(e, d)) {
                    set.add(Long.valueOf(Store.dayStart(d)));
                    break;
                }
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return set;
    }

    private void scrollWeekToNow() {
        if (weekScroll == null) return;
        Calendar n = Calendar.getInstance();
        final int y = (int) ((n.get(Calendar.HOUR_OF_DAY) - 1) * dp(WeekTimelineView.ROW_DP));
        weekScroll.post(new Runnable() {
            public void run() { weekScroll.scrollTo(0, Math.max(0, y)); }
        });
    }

    private void buildWeekStrip() {
        weekStrip.removeAllViews();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedMillis);
        int idx = c.get(Calendar.DAY_OF_WEEK) - 1; // 周日为 0
        c.add(Calendar.DAY_OF_MONTH, -idx);
        for (int i = 0; i < 7; i++) {
            final long dayMs = Store.dayStart(c.getTimeInMillis());
            final int yy = c.get(Calendar.YEAR);
            final int mm = c.get(Calendar.MONTH);
            final int dd = c.get(Calendar.DAY_OF_MONTH);

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

            boolean sel = Store.sameDay(dayMs, selectedMillis);
            boolean isToday = Store.sameDay(dayMs, System.currentTimeMillis());

            TextView wd = new TextView(this);
            wd.setText(WK1[i]);
            wd.setTextSize(11);
            wd.setTextColor(sel ? Theme.brand(this) : c(R.color.text_secondary));
            cell.addView(wd);

            TextView dn = new TextView(this);
            dn.setText(String.valueOf(dd));
            dn.setTextSize(15);
            dn.setGravity(Gravity.CENTER);
            dn.setWidth(dp(30));
            dn.setHeight(dp(30));
            if (sel) {
                GradientDrawable g = new GradientDrawable();
                g.setShape(GradientDrawable.OVAL);
                g.setColor(Theme.brand(this));
                dn.setBackground(g);
                dn.setTextColor(0xFFFFFFFF);
            } else {
                dn.setTextColor(isToday ? Theme.brand(this) : c(R.color.text_primary));
            }
            cell.addView(dn);

            LinearLayout dotRow = new LinearLayout(this);
            dotRow.setGravity(Gravity.CENTER);
            dotRow.setMinimumHeight(dp(8));
            List<Event> es = store.onDay(dayMs);
            int n = Math.min(es.size(), 3);
            for (int k = 0; k < n; k++) {
                View dot = new View(this);
                LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(4), dp(4));
                dlp.leftMargin = dp(2);
                dlp.rightMargin = dp(2);
                dlp.topMargin = dp(3);
                dot.setLayoutParams(dlp);
                GradientDrawable g2 = new GradientDrawable();
                g2.setShape(GradientDrawable.OVAL);
                g2.setColor(es.get(k).color);
                dot.setBackground(g2);
                dotRow.addView(dot);
            }
            cell.addView(dotRow);

            cell.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { selectDay(yy, mm, dd); }
            });
            weekStrip.addView(cell);
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    // ---------------- 导入 / 导出 ----------------
    private void doExport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "行程提醒备份.json");
        try {
            startActivityForResult(i, 1001);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.import_fail), Toast.LENGTH_SHORT).show();
        }
    }

    private void doImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, 1002);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.import_fail), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (req == 1001) {
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(store.toJson().getBytes("UTF-8"));
                os.flush();
                os.close();
                Toast.makeText(this, getString(R.string.export_done), Toast.LENGTH_SHORT).show();
            } else if (req == 1002) {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String text = sb.toString();
                if (text.trim().length() == 0) {
                    Toast.makeText(this, getString(R.string.import_empty), Toast.LENGTH_LONG).show();
                } else {
                    try {
                        int n = store.importJson(text);
                        refreshAll();
                        if (n > 0)
                            Toast.makeText(this, getString(R.string.import_ok) + " +" + n, Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(this, getString(R.string.import_none), Toast.LENGTH_LONG).show();
                    } catch (org.json.JSONException je) {
                        Toast.makeText(this, getString(R.string.import_bad), Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.import_fail), Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------- 通知 ----------------
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel("schedule_remind") == null) {
                NotificationChannel ch = new NotificationChannel("schedule_remind", "日程提醒", NotificationManager.IMPORTANCE_HIGH);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private void requestNotifPerm() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 10);
            }
        }
    }
}

