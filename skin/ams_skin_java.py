#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""AMS 皮肤 · Java 自绘组件补丁（可重放）"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src', 'com', 'moshang', 'schedule')


def patch(name, pairs, optional=()):
    p = os.path.join(SRC, name)
    s = open(p, encoding='utf-8').read()
    for old, new in pairs:
        if s.count(old) != 1:
            raise SystemExit('[FAIL] %s: 匹配 %d 次 -> %r' % (name, s.count(old), old[:70]))
        s = s.replace(old, new)
    for old, new in optional:
        if s.count(old) == 1:
            s = s.replace(old, new)
        else:
            print('  skip(optional) %s <- %r (%d)' % (name, old[:50], s.count(old)))
    open(p, 'w', encoding='utf-8').write(s)
    print('  patched', name)


# ---- SegmentBar：AMS 选中块（大圆角、无阴影）
patch('SegmentBar.java', [
    ('        radius = dp(9);', '        radius = dp(12);'),
    ('        pPill.setShadowLayer(dp(2), 0, dp(1), 0x1F000000);\n', ''),
    ('        float inset = dp(3);', '        float inset = dp(3.5f);'),
])

# ---- MonthView：选中块大圆角 + 今日 wash 底色块
patch('MonthView.java', [
    ('    private final Paint pSel = new Paint(Paint.ANTI_ALIAS_FLAG);',
     '    private final Paint pSel = new Paint(Paint.ANTI_ALIAS_FLAG);\n'
     '    private final Paint pWash = new Paint(Paint.ANTI_ALIAS_FLAG);'),
    ('    private int headerColor, normalColor, outColor, brandColor, selBgColor, selTextColor, lunarColor, lunarHiColor, weekendColor;',
     '    private int headerColor, normalColor, outColor, brandColor, selBgColor, selTextColor, lunarColor, lunarHiColor, weekendColor, todayWashColor;'),
    ('        selTextColor = getResources().getColor(R.color.day_sel_text);',
     '        selTextColor = getResources().getColor(R.color.day_sel_text);\n'
     '        todayWashColor = getResources().getColor(R.color.chip_bg_on);'),
    ('        pSel.setColor(selBgColor);',
     '        pSel.setColor(selBgColor);\n'
     '        pWash.setStyle(Paint.Style.FILL);\n'
     '        pWash.setColor(todayWashColor);'),
    ('''                if (isSel) {
                    rf.set(cx - halfW, numCy - dp(14), cx + halfW, numCy + dp(14));
                    cv.drawRoundRect(rf, dp(9), dp(9), pSel);
                }''',
     '''                if (isSel) {
                    rf.set(cx - halfW, numCy - dp(14), cx + halfW, numCy + dp(14));
                    cv.drawRoundRect(rf, dp(12), dp(12), pSel);
                } else if (isToday) {
                    rf.set(cx - halfW, numCy - dp(14), cx + halfW, numCy + dp(14));
                    cv.drawRoundRect(rf, dp(12), dp(12), pWash);
                }'''),
])

# ---- TitleBar：标题放大到 AMS h1 观感，收紧字距
patch('TitleBar.java', [
    ('        p.setTextSize(scaled * 19f);', '        p.setTextSize(scaled * 21f);\n        p.setLetterSpacing(-0.01f);'),
])

# ---- Sheet：AMS 浅遮罩
patch('Sheet.java', [
    ('    protected int scrimColor = 0x99000000;', '    protected int scrimColor = 0x52000000;'),
    ('        scrim.setBackgroundColor(0x99000000);', '        scrim.setBackgroundColor(scrimColor);'),
])

print('done')
