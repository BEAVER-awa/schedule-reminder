#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按 engineering 参数渲染「AMS 皮肤」设计预览图（浅色 / 深色并排）。
这不是真机截图，是按 colors.xml / dimens.xml / 各 View 的实际绘制参数 1:1 复算的示意图。
"""
import os, datetime
from PIL import Image, ImageDraw, ImageFont

W, H = 360, 720          # dp
SCALE = 2                # 2x
FS = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
FB = '/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc'

def f(sp, bold=False):
    return ImageFont.truetype(FB if bold else FS, int(round(sp * SCALE)), index=0)

LIGHT = dict(
    paper='#F5F6F8', surface='#FCFDFE', ink='#17233A', pencil='#69758A',
    tertiary='#A4AFBF', line='#E1E6ED', brand='#172E61', accent='#254E9B',
    wash='#EAF0FA', seg_bg='#EDF0F5', seg_sel='#FFFFFF', seg_sel_text='#172E61',
    seg_text='#69758A', weekend='#956174', out='#C3CBD8', info_bg='#EAF0FA',
    info_text='#4B5B78', sel_bg='#172E61', sel_text='#FFFFFF', circle='#FCFDFE',
    lunar='#A4AFBF',
)
DARK = dict(
    paper='#15191F', surface='#202630', ink='#E7ECF4', pencil='#A4AFBF',
    tertiary='#6B7688', line='#343D49', brand='#8FA6D6', accent='#9EB6E0',
    wash='#1D283E', seg_bg='#1B222B', seg_sel='#2A3444', seg_sel_text='#D6E2F5',
    seg_text='#A4AFBF', weekend='#C08A9B', out='#4A5464', info_bg='#1D283E',
    info_text='#9EB6E0', sel_bg='#254E9B', sel_text='#FFFFFF', circle='#202630',
    lunar='#6B7688',
)
EV_COLORS = ['#3F6BA8', '#3E8C7E', '#6C7A96', '#4E8F8B', '#7A8F76',
             '#7F7398', '#6E7A88', '#A87E8E', '#A89052']

LUNAR = ['初七', '初八', '初九', '初十', '十一', '十二', '十三', '十四', '十五',
         '十六', '十七', '十八', '十九', '二十', '廿一', '廿二', '廿三', '廿四',
         '廿五', '廿六', '廿七', '廿八', '廿九', '三十']


def px(v):
    return int(round(v * SCALE))


def draw_panel(T, accent_sel_month=True):
    img = Image.new('RGB', (px(W), px(H)), T['paper'])
    d = ImageDraw.Draw(img)

    # ---- 状态栏留白
    d.rectangle([0, 0, px(W), px(24)], fill=T['paper'])

    # ---- header 58dp
    top = 24
    ey = top + 8
    d.text((px(52), px(ey)), '行 程 提 醒', font=f(10.5, True), fill=T['pencil'])
    d.text((px(52), px(ey + 15)), '2026/', font=f(21, True), fill=T['ink'])
    tw = d.textlength('2026/', font=f(21, True))
    d.text((px(52) + tw, px(ey + 15)), '10', font=f(21, True), fill=T['accent'])
    tw2 = tw + d.textlength('10', font=f(21, True))
    # 下拉箭头
    ax, ay = px(52) + tw2 + px(9), px(ey + 26)
    d.polygon([(ax - px(4.6), ay - px(3.2)), (ax + px(4.6), ay - px(3.2)), (ax, ay + px(3.2))],
              fill=T['pencil'])
    # 右上：收起按钮
    cx, cy = px(W - 26), px(top + 13)
    d.rounded_rectangle([cx, cy, cx + px(18), cy + px(18)], radius=px(4), outline=T['pencil'], width=1)
    d.polygon([(cx + px(5), cy + px(11)), (cx + px(13), cy + px(11)), (cx + px(9), cy + px(6))],
              fill=T['pencil'])
    # 右侧功能图标（搜索/前后/更多）
    for i, gx in enumerate([W - 42 + 0]):
        pass
    ix = px(W - 62)
    d.ellipse([ix, cy + px(3), ix + px(11), cy + px(14)], outline=T['pencil'], width=1)
    d.line([ix + px(9), cy + px(12), ix + px(14), cy + px(17)], fill=T['pencil'], width=1)

    # ---- 分段条 40dp（外 padding 16/6/16/10）
    sy = top + 56 + 6
    d.rounded_rectangle([px(16), px(sy), px(W - 16), px(sy + 44)], radius=px(12), fill=T['seg_bg'])
    names = ['年', '月', '周', '日', '日程']
    cw = (W - 32) / 5.0
    sel = 1
    sx = 16 + sel * cw
    d.rounded_rectangle([px(sx + 3.5), px(sy + 3.5), px(sx + cw - 3.5), px(sy + 40.5)],
                        radius=px(11), fill=T['seg_sel'])
    for i, n in enumerate(names):
        w = d.textlength(n, font=f(13, i == sel))
        d.text((px(16 + i * cw + cw / 2) - w / 2, px(sy + 14)), n,
               font=f(13, i == sel), fill=T['seg_sel_text'] if i == sel else T['seg_text'])

    # ---- 月历
    my = sy + 44 + 8
    WB = ['日', '一', '二', '三', '四', '五', '六']
    colw = W / 7.0
    for i, wch in enumerate(WB):
        w = d.textlength(wch, font=f(11))
        d.text((px(i * colw + colw / 2) - w / 2, px(my)), wch, font=f(11), fill=T['pencil'])
    cell_h = 56
    grid_y = my + 24
    first = datetime.date(2026, 10, 1)
    lead = (first.weekday() + 1) % 7          # 周日起始
    sel_day, today = 12, 30
    months = [(9, 31 - lead + 1), (10, 31), (11, 1)]
    cells = []
    for i in range(42):
        idx = i - lead + 1
        if idx < 1:
            dd, mm = 30 + idx, 9
        elif idx > 31:
            dd, mm = idx - 31, 11
        else:
            dd, mm = idx, 10
        cells.append((dd, mm))
    for i, (dd, mm) in enumerate(cells):
        r, c = i // 7, i % 7
        cx = colw * c + colw / 2
        cy = grid_y + r * cell_h + cell_h * 0.32
        in_month = (mm == 10)
        is_sel = in_month and dd == sel_day
        is_today = in_month and dd == today
        halfw = min(colw, cell_h) * 0.33
        if is_sel:
            d.rounded_rectangle([px(cx - halfw), px(cy - 14), px(cx + halfw), px(cy + 14)],
                                radius=px(12), fill=T['sel_bg'])
        elif is_today:
            d.rounded_rectangle([px(cx - halfw), px(cy - 14), px(cx + halfw), px(cy + 14)],
                                radius=px(12), outline=T['brand'], width=px(1.5))
        if is_sel:
            tc = T['sel_text']
        elif is_today:
            tc = T['brand']
        elif not in_month:
            tc = T['out']
        elif c in (0, 6):
            tc = T['weekend']
        else:
            tc = T['ink']
        s = str(dd)
        w = d.textlength(s, font=f(16, True))
        d.text((px(cx) - w / 2, px(cy) - px(9)), s, font=f(16, True), fill=tc)
        if in_month:
            ln = LUNAR[(dd + 3) % len(LUNAR)]
            w2 = d.textlength(ln, font=f(9))
            d.text((px(cx) - w2 / 2, px(grid_y + r * cell_h + cell_h * 0.62)), ln,
                   font=f(9), fill=T['lunar'])
            # 事件点
            n_dots = [0, 1, 2, 1, 0, 3, 1][dd % 7]
            if n_dots:
                gap = px(6)
                tot = (n_dots - 1) * gap
                x0 = px(cx) - tot / 2
                yy = px(grid_y + r * cell_h + cell_h * 0.84)
                for k in range(n_dots):
                    col = EV_COLORS[(dd + k) % len(EV_COLORS)]
                    d.ellipse([x0 + k * gap - px(1.8), yy - px(1.8), x0 + k * gap + px(1.8), yy + px(1.8)],
                              fill=col)

    # ---- 农历信息条
    iy = grid_y + 6 * cell_h
    d.rectangle([0, px(iy), px(W), px(iy + 30)], fill=T['info_bg'])
    d.text((px(16), px(iy + 8)), '农历九月十一 · 霜降 · 距期末还有 62 天',
           font=f(12), fill=T['info_text'])

    # ---- 日程卡
    cy0 = iy + 38
    items = [('高等数学 · 第三章', '08:00 - 09:40', 'SB-201', 0),
             ('学术英语写作', '10:00 - 11:40', 'EB-105', 1),
             ('小组讨论：ADS 项目', '14:00 - 15:00', '图书馆 A 区', 3)]
    for title, t, place, ci in items:
        ch = 62
        d.rounded_rectangle([px(16), px(cy0), px(W - 16), px(cy0 + ch)],
                            radius=px(13), fill=T['surface'], outline=T['line'], width=1)
        d.rounded_rectangle([px(30), px(cy0 + 14), px(33), px(cy0 + ch - 14)],
                            radius=px(2), fill=EV_COLORS[ci])
        d.text((px(44), px(cy0 + 12)), title, font=f(14.5, True), fill=T['ink'])
        d.text((px(44), px(cy0 + 34)), t + '   ·   ' + place, font=f(11.5), fill=T['pencil'])
        cy0 += ch + 8

    # ---- 右下 今 + FAB
    bx, by = W - 20 - 56, H - 24 - 56
    d.rounded_rectangle([px(bx + 6), px(by - 56), px(bx + 6 + 44), px(by - 56 + 44)],
                        radius=px(20), outline=T['line'], width=1, fill=T['circle'])
    tj = '今'
    w = d.textlength(tj, font=f(15, True))
    d.text((px(bx + 6 + 22) - w / 2, px(by - 56 + 13)), tj, font=f(15, True), fill=T['brand'])
    d.rounded_rectangle([px(bx), px(by), px(bx + 56), px(by + 56)], radius=px(20), fill=T['brand'])
    cx2, cy2 = px(bx + 28), px(by + 28)
    d.rectangle([cx2 - px(9), cy2 - px(1), cx2 + px(9), cy2 + px(1)], fill='#FFFFFF')
    d.rectangle([cx2 - px(1), cy2 - px(9), cx2 + px(1), cy2 + px(9)], fill='#FFFFFF')
    return img


def main():
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'preview')
    os.makedirs(out, exist_ok=True)
    a = draw_panel(LIGHT)
    b = draw_panel(DARK)
    gap = px(16)
    canvas = Image.new('RGB', (a.width * 2 + gap, a.height + px(28)), '#0B0E13')
    canvas.paste(a, (0, px(28)))
    canvas.paste(b, (a.width + gap, px(28)))
    d = ImageDraw.Draw(canvas)
    lab = ImageFont.truetype(FB, px(13))
    d.text((px(12), px(5)), '浅色 AMS Paper', font=lab, fill='#9FB0C6')
    d.text((a.width + gap + px(12), px(5)), '深色 AMS Night', font=lab, fill='#9FB0C6')
    p1 = out + '/ams-skin-light.png'
    p2 = out + '/ams-skin-dark.png'
    a.save(p1)
    b.save(p2)
    canvas.save(out + '/ams-skin-both.png')
    print('ok', p1, p2)


if __name__ == '__main__':
    main()
