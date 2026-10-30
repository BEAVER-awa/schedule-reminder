#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""启动图标（方案 A「日历牌」）：深蓝圆角底 + 白色日历纸 + 深色分隔线 + 两个挂钩 + 事件条。
8x 超采样抗锯齿；导出 5 档 PNG + adaptive icon 前景（432px）。
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NAVY = (0x17, 0x2E, 0x61, 255)
WHITE = (255, 255, 255, 255)

SS = 8
SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}


def render(U, with_base=True):
    img = Image.new('RGBA', (U, U), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    if with_base:
        d.rounded_rectangle([0, 0, U - 1, U - 1], radius=int(0.225 * U), fill=NAVY)

    # 白色日历纸
    x0, y0, x1, y1 = 0.215 * U, 0.265 * U, 0.785 * U, 0.775 * U
    pr = int(0.085 * U)
    d.rounded_rectangle([x0, y0, x1, y1], radius=pr, fill=WHITE)

    # 深色分隔线（盖住纸的上部，形成日历表头）
    bar = 0.075 * U
    d.rectangle([x0, y0 + bar * 0.9, x1, y0 + bar * 2.0], fill=NAVY)

    # 两个装订挂钩
    for cx in (0.355, 0.645):
        hx = cx * U
        hr = 0.028 * U
        d.rounded_rectangle([hx - hr, 0.195 * U, hx + hr, 0.325 * U],
                            radius=int(hr), fill=WHITE)

    # 事件条（主）
    d.rounded_rectangle([0.300 * U, 0.530 * U, 0.600 * U, 0.575 * U],
                        radius=int(0.022 * U), fill=NAVY)
    # 事件条（次，半透明）
    d.rounded_rectangle([0.300 * U, 0.645 * U, 0.500 * U, 0.690 * U],
                        radius=int(0.022 * U), fill=(0x17, 0x2E, 0x61, 120))
    return img


def main():
    for folder, S in SIZES.items():
        out_dir = os.path.join(ROOT, 'res', folder)
        os.makedirs(out_dir, exist_ok=True)
        img = render(S * SS).resize((S, S), Image.LANCZOS)
        p = os.path.join(out_dir, 'ic_launcher.png')
        img.save(p)
        print('  %-16s %3dpx %5d B' % (folder, S, os.path.getsize(p)))

    # adaptive icon 前景：108dp(432px) 画布上只画图形本身，不要底色，
    # 也不额外缩放——图形已在 18..90 的安全区内，系统会自己套遮罩
    fore = render(432, with_base=False)
    out_dir = os.path.join(ROOT, 'res', 'drawable-xxxhdpi')
    os.makedirs(out_dir, exist_ok=True)
    p = os.path.join(out_dir, 'ic_launcher_foreground.png')
    fore.save(p)
    print('  %-16s 432px %5d B' % ('foreground', os.path.getsize(p)))


if __name__ == '__main__':
    main()
