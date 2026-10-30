#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 AMS 校园助手的视觉语言移植到行程提醒（schedule-native）。
可重放：python3 skin/ams_skin.py
"""
import os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'res')
SRC = os.path.join(ROOT, 'src', 'com', 'moshang', 'schedule')

def w(path, text):
    p = path if os.path.isabs(path) else os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        f.write(text)
    print('  written', os.path.relpath(p, ROOT), len(text), 'bytes')

# ---------------------------------------------------------------- 浅色令牌
LIGHT = {
    'brand':            '#FF172E61',  # campus-blue
    'brand_dark':       '#FF254E9B',  # room-blue
    'window_bg':        '#FFF5F6F8',  # paper
    'surface':          '#FFFCFDFE',
    'card_bg':          '#FFFCFDFE',
    'text_primary':     '#FF17233A',  # ink
    'text_secondary':   '#FF69758A',  # pencil
    'text_tertiary':    '#FFA4AFBF',
    'weekday':          '#FF69758A',
    'weekend':          '#FF956174',
    'out_month':        '#FFC3CBD8',
    'divider':          '#FFE1E6ED',
    'grid_line':        '#FFF1F4F8',
    'selection':        '#14172E61',
    'chip_bg':          '#FFF5F7FA',
    'chip_bg_on':       '#FFEAF0FA',
    'chip_text_on':     '#FF172E61',
    'statusbar':        '#FFF5F6F8',
    'nav_divider':      '#FFE1E6ED',
    'scrim':            '#52000000',
    'today_ring':       '#FF172E61',
    'day_sel_bg':       '#FF172E61',
    'day_sel_text':     '#FFFFFFFF',
    'lunar_text':       '#FFA4AFBF',
    'lunar_hi':         '#FF254E9B',
    'segment_bg':       '#FFEDF0F5',
    'segment_sel':      '#FFEAF0FA',
    'segment_sel_text': '#FF172E61',
    'segment_text':     '#FF69758A',
    'infobar_bg':       '#FFEAF0FA',
    'infobar_text':     '#FF4B5B78',
    'circle_bg':        '#FFFCFDFE',
    'sheet_handle':     '#FFD7DEE8',
    'toggle_off':       '#FFD7DEE8',
    'danger':           '#FFB3505E',
    'sheet_line':       '#FFF1F4F8',
}

# ---------------------------------------------------------------- 深色令牌
DARK = {
    'brand':            '#FF8FA6D6',
    'brand_dark':       '#FF6E8AC4',
    'window_bg':        '#FF15191F',
    'surface':          '#FF202630',
    'card_bg':          '#FF202630',
    'text_primary':     '#FFE7ECF4',
    'text_secondary':   '#FFA4AFBF',
    'text_tertiary':    '#FF6B7688',
    'weekday':          '#FFA4AFBF',
    'weekend':          '#FFC08A9B',
    'out_month':        '#FF4A5464',
    'divider':          '#FF343D49',
    'grid_line':        '#FF2A313B',
    'selection':        '#33FFFFFF',
    'chip_bg':          '#FF191F27',
    'chip_bg_on':       '#FF1D283E',
    'chip_text_on':     '#FF9EB6E0',
    'statusbar':        '#FF15191F',
    'nav_divider':      '#FF343D49',
    'scrim':            '#66000000',
    'today_ring':       '#FF9EB6E0',
    'day_sel_bg':       '#FF254E9B',
    'day_sel_text':     '#FFFFFFFF',
    'lunar_text':       '#FF6B7688',
    'lunar_hi':         '#FF9EB6E0',
    'segment_bg':       '#FF1B222B',
    'segment_sel':      '#FF253C63',
    'segment_sel_text': '#FFD6E2F5',
    'segment_text':     '#FFA4AFBF',
    'infobar_bg':       '#FF1D283E',
    'infobar_text':     '#FF9EB6E0',
    'circle_bg':        '#FF202630',
    'sheet_handle':     '#FF3A4453',
    'toggle_off':       '#FF3A4453',
    'danger':           '#FFD08A94',
    'sheet_line':       '#FF2A313B',
}


def colors_xml(d):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    order = ['brand', 'brand_dark', '', 'window_bg', 'surface', 'card_bg', '',
             'text_primary', 'text_secondary', 'text_tertiary', '',
             'weekday', 'weekend', 'out_month', '', 'divider', 'grid_line', 'selection', '',
             'chip_bg', 'chip_bg_on', 'chip_text_on', '', 'statusbar', 'nav_divider', 'scrim',
             'today_ring', '', 'day_sel_bg', 'day_sel_text', 'lunar_text', 'lunar_hi', '',
             'segment_bg', 'segment_sel', 'segment_sel_text', 'segment_text', '',
             'infobar_bg', 'infobar_text', 'circle_bg', 'sheet_handle', 'toggle_off',
             'danger', 'sheet_line']
    for k in order:
        if k == '':
            lines.append('')
        else:
            lines.append('    <color name="%s">%s</color>' % (k, d[k]))
    lines.append('</resources>')
    lines.append('')
    return '\n'.join(lines)


DIMENS = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <dimen name="page_padding">20dp</dimen>
    <dimen name="header_height">58dp</dimen>
    <dimen name="month_cell_height">58dp</dimen>
    <dimen name="month_day_text">16sp</dimen>
    <dimen name="week_text">12sp</dimen>
    <dimen name="agenda_card_gap">8dp</dimen>
    <dimen name="agenda_bar_w">3dp</dimen>
    <dimen name="card_radius">13dp</dimen>
    <dimen name="tab_text">13sp</dimen>
</resources>
"""

BG_CARD = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/card_bg" />
    <corners android:radius="@dimen/card_radius" />
    <stroke android:width="1dp" android:color="@color/divider" />
</shape>
"""

RIPPLE_CARD = """<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#14172E61">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/card_bg" />
            <corners android:radius="@dimen/card_radius" />
            <stroke android:width="1dp" android:color="@color/divider" />
        </shape>
    </item>
</ripple>
"""

BG_FAB = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/brand" />
    <corners android:radius="20dp" />
</shape>
"""

BG_CIRCLE = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/circle_bg" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="@color/divider" />
</shape>
"""

BG_CHIP = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/chip_bg" />
    <corners android:radius="11dp" />
</shape>
"""

BG_SEGMENT = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/segment_bg" />
    <corners android:radius="14dp" />
</shape>
"""

BG_SEGMENT_ON = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/segment_sel" />
    <corners android:radius="11dp" />
</shape>
"""

BG_FIELD = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/chip_bg" />
    <corners android:radius="12dp" />
    <stroke android:width="1dp" android:color="@color/divider" />
</shape>
"""

BG_BTN_BRAND = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/brand" />
    <corners android:radius="13dp" />
</shape>
"""

BG_INFOBAR = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/infobar_bg" />
</shape>
"""

BG_SHEET = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/surface" />
    <corners android:topLeftRadius="22dp" android:topRightRadius="22dp" />
    <stroke android:width="1dp" android:color="@color/divider" />
</shape>
"""

BG_OPTION = """<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#1F172E61">
    <item android:id="@android:id/mask">
        <shape>
            <solid android:color="#FFFFFFFF" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</ripple>
"""

def main():
    print('[1/4] colors.xml')
    w('res/values/colors.xml', colors_xml(LIGHT))
    w('res/values-night/colors.xml', colors_xml(DARK))

    print('[2/4] dimens')
    w('res/values/dimens.xml', DIMENS)

    print('[3/4] drawable')
    w('res/drawable/bg_card.xml', BG_CARD)
    w('res/drawable/ripple_card.xml', RIPPLE_CARD)
    w('res/drawable/bg_fab.xml', BG_FAB)
    w('res/drawable/bg_circle.xml', BG_CIRCLE)
    w('res/drawable/bg_chip.xml', BG_CHIP)
    w('res/drawable/bg_segment.xml', BG_SEGMENT)
    w('res/drawable/bg_segment_on.xml', BG_SEGMENT_ON)
    w('res/drawable/bg_field.xml', BG_FIELD)
    w('res/drawable/bg_btn_brand.xml', BG_BTN_BRAND)
    w('res/drawable/bg_infobar.xml', BG_INFOBAR)
    w('res/drawable/bg_sheet.xml', BG_SHEET)
    w('res/drawable/bg_option.xml', BG_OPTION)

    print('[4/4] drawable/base（新增 AMS 版）')
    w('res/drawable/base_ams.xml', BASE_AMS)


BASE_AMS = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface" />
    <corners android:radius="@dimen/card_radius" />
</shape>
"""

if __name__ == '__main__':
    main()
