#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按 calendar-design 技能规范重做行程提醒 UI（可重放）。
规则来源：calendar-design/SKILL.md 二（网格）、五（无障碍）、六（视觉规范）、七（Android 自绘）。
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'com', 'moshang', 'schedule')


def patch(name, pairs):
    p = os.path.join(SRC, name)
    s = open(p, encoding='utf-8').read()
    for old, new in pairs:
        if s.count(old) == 1:
            s = s.replace(old, new)
        elif new in s:
            print('  already', name)
        else:
            raise SystemExit('[FAIL] %s: %d 次 -> %r' % (name, s.count(old), old[:60]))
    open(p, 'w', encoding='utf-8').write(s)
    print('  patched', name)


def patch_res(rel, pairs):
    p = os.path.join(ROOT, rel)
    s = open(p, encoding='utf-8').read()
    for old, new in pairs:
        if s.count(old) == 1:
            s = s.replace(old, new)
        elif new in s:
            print('  already', rel)
        else:
            raise SystemExit('[FAIL] %s: %d 次 -> %r' % (rel, s.count(old), old[:60]))
    open(p, 'w', encoding='utf-8').write(s)
    print('  patched', rel)


# ── 1) MonthView：今日改为描边环（与选中填充区分）、农历 10sp、等宽数字
patch('MonthView.java', [
    ('    private final Paint pWash = new Paint(Paint.ANTI_ALIAS_FLAG);',
     '    private final Paint pWash = new Paint(Paint.ANTI_ALIAS_FLAG);\n'
     '    private final Paint pRing = new Paint(Paint.ANTI_ALIAS_FLAG);'),
    ('    private int headerColor, normalColor, outColor, brandColor, selBgColor, selTextColor, lunarColor, lunarHiColor, weekendColor, todayWashColor;',
     '    private int headerColor, normalColor, outColor, brandColor, selBgColor, selTextColor, lunarColor, lunarHiColor, weekendColor, todayWashColor, todayRingColor;'),
    ('        todayWashColor = getResources().getColor(R.color.chip_bg_on);',
     '        todayWashColor = getResources().getColor(R.color.chip_bg_on);\n'
     '        todayRingColor = getResources().getColor(R.color.today_ring);'),
    ('        pWash.setColor(todayWashColor);',
     '        pWash.setColor(todayWashColor);\n'
     '        pRing.setStyle(Paint.Style.STROKE);\n'
     '        pRing.setStrokeWidth(dp(1.5f));\n'
     '        pRing.setColor(todayRingColor);\n'
     '        p.setFontFeatureSettings("tnum");'),
    ('''                } else if (isToday) {
                    rf.set(cx - halfW, numCy - dp(14), cx + halfW, numCy + dp(14));
                    cv.drawRoundRect(rf, dp(12), dp(12), pWash);
                }''',
     '''                } else if (isToday) {
                    rf.set(cx - halfW, numCy - dp(14), cx + halfW, numCy + dp(14));
                    cv.drawRoundRect(rf, dp(12), dp(12), pRing);
                }'''),
    ('                    p.setTextSize(sp(9));', '                    p.setTextSize(sp(10));'),
])

# ── 2) AgendaAdapter：时间用等宽数字
patch('AgendaAdapter.java', [
    ('        st.setTextSize(12);', '        st.setTextSize(12);\n        st.setFontFeatureSettings("tnum");'),
])

# ── 3) WeekTimelineView：事件条圆角 5dp + 等宽刻度
patch('WeekTimelineView.java', [
    ('        cv.drawRoundRect(rf, dp(4), dp(4), pEv);', '        cv.drawRoundRect(rf, dp(5), dp(5), pEv);'),
    ('        pTxt.setTextSize(sp(10));', '        pTxt.setTextSize(sp(10));\n        pTxt.setFontFeatureSettings("tnum");'),
])

# ── 4) MainActivity：触控区 34→40dp、段条 40→44dp、header 58→56dp、今 44→48dp、图标无障碍描述
def main_activity():
    p = os.path.join(SRC, 'MainActivity.java')
    s = open(p, encoding='utf-8').read()

    n = s.count('dp(34)')
    assert n == 8, n
    s = s.replace('dp(34)', 'dp(40)')

    assert s.count('h.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));') == 1
    s = s.replace('h.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));',
                  'h.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));')

    assert s.count('segBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));') == 1
    s = s.replace('segBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));',
                  'segBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));')

    assert s.count('LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(44), dp(44));') == 1
    s = s.replace('LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(44), dp(44));',
                  'LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(dp(48), dp(48));')

    # 图标无障碍描述
    s = s.replace("        search.setImageResource(R.drawable.ic_search);",
                  "        search.setImageResource(R.drawable.ic_search);\n        search.setContentDescription(\"搜索行程\");")
    s = s.replace("        more.setImageResource(R.drawable.ic_more);",
                  "        more.setImageResource(R.drawable.ic_more);\n        more.setContentDescription(\"更多操作\");")
    s = s.replace("        prev.setImageResource(R.drawable.ic_back);",
                  "        prev.setImageResource(R.drawable.ic_back);\n        prev.setContentDescription(\"上一页\");")
    s = s.replace("        next.setImageResource(R.drawable.ic_next);",
                  "        next.setImageResource(R.drawable.ic_next);\n        next.setContentDescription(\"下一页\");")
    s = s.replace("        collapseBtn.setImageResource(R.drawable.ic_collapse);",
                  "        collapseBtn.setImageResource(R.drawable.ic_collapse);\n        collapseBtn.setContentDescription(\"收起或展开视图栏\");")
    s = s.replace("        fab.setImageResource(R.drawable.ic_add);",
                  "        fab.setImageResource(R.drawable.ic_add);\n        fab.setContentDescription(\"新建行程\");")
    open(p, 'w', encoding='utf-8').write(s)
    print('  patched MainActivity.java')


main_activity()

# ── 5) Press：尊重系统「减少动态效果」
patch('Press.java', [
    ('''    public static void attach(final View v) {''',
     '''    /** 尊重系统动画缩放：关闭动画时按压反馈直接跳过 */
    static long dur(View v, long d) {
        try {
            float s = android.provider.Settings.Global.getFloat(
                    v.getContext().getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
            return (long) (d * Math.max(0f, s));
        } catch (Throwable t) {
            return d;
        }
    }

    public static void attach(final View v) {'''),
    ('                                .setDuration(200).setInterpolator(interp(view)).start();',
     '                                .setDuration(dur(view, 200)).setInterpolator(interp(view)).start();'),
    ('                                .setDuration(250).setInterpolator(interp(view)).start();',
     '                                .setDuration(dur(view, 250)).setInterpolator(interp(view)).start();'),
])

# ── 6) 资源：4dp 基线、圆角收敛、危险色与周末色分离、分类色板降饱和
patch_res('res/values/dimens.xml', [
    ('<dimen name="header_height">58dp</dimen>', '<dimen name="header_height">56dp</dimen>'),
    ('<dimen name="month_cell_height">58dp</dimen>', '<dimen name="month_cell_height">56dp</dimen>'),
])
patch_res('res/values/colors.xml', [
    ('<color name="danger">#FFB3505E</color>', '<color name="danger">#FFC0453F</color>'),
])
patch_res('res/values-night/colors.xml', [
    ('<color name="danger">#FFD08A94</color>', '<color name="danger">#FFE08578</color>'),
])
patch_res('res/drawable/bg_sheet.xml', [
    ('android:topLeftRadius="22dp" android:topRightRadius="22dp"',
     'android:topLeftRadius="16dp" android:topRightRadius="16dp"'),
])
patch_res('src/com/moshang/schedule/Store.java', [
    ('''    public static final int[] PALETTE = {
            0xFF0F77FF, 0xFF7979D9, 0xFF33C2CC, 0xFF5BCC77, 0xFFF6CF40, 0xFFB0673C,
            0xFFFF9D58, 0xFFEEA2E4, 0xFFF1657C, 0xFF4DB6AC, 0xFFFF8A65, 0xFF9575CD
    };''',
     '''    /** 分类色板：低饱和 9 色（calendar-design 技能要求「低饱和、9 色以内」） */
    public static final int[] PALETTE = {
            0xFF3F6BA8, 0xFF3E8C7E, 0xFF6C7A96, 0xFF4E8F8B, 0xFF7A8F76,
            0xFF7F7398, 0xFF6E7A88, 0xFFA87E8E, 0xFFA89052
    };'''),
])

print('done')
