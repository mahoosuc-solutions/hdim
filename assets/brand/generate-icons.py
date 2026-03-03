#!/usr/bin/env python3
"""
Generate HDIM brand icon (B - Hex Network) at all required sizes.
Outputs: PNG files + favicon.ico + site.webmanifest

Requires: pillow (pip install pillow)
Run from project root: python3 assets/brand/generate-icons.py
"""

import math
import os
from PIL import Image, ImageDraw

# Brand colors
NAVY_DARK   = (30, 58, 95)     # #1e3a5f
NAVY_LIGHT  = (45, 90, 135)    # #2d5a87
BLUE        = (33, 150, 243)   # #2196f3
TEAL        = (33, 203, 243)   # #21cbf3

SIZES = [16, 32, 48, 64, 180, 512, 1024]
OUT_DIR = os.path.join(os.path.dirname(__file__), "icon-b-hex-network")


def lerp_color(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


def hex_vertices(cx, cy, r, flat_top=True):
    """Return 6 (x, y) vertices of a regular hexagon."""
    verts = []
    for i in range(6):
        angle = math.radians(60 * i + (0 if flat_top else 30))
        verts.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    return verts


def draw_icon(size):
    scale = size / 100.0
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Background with gradient (simulated via two-pass fill) ───────────────
    # Pillow doesn't do gradients natively; we fill top-half darker, bottom lighter
    # then composite. For small sizes this is imperceptible anyway.
    radius = int(18 * scale)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius,
                           fill=NAVY_DARK)
    # Subtle gradient: draw lighter triangle in bottom-right
    gradient_pts = [(size * 0.4, size), (size, size * 0.4), (size, size)]
    draw.polygon(gradient_pts, fill=NAVY_LIGHT)
    # Re-clip to rounded rect by masking
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg.paste(img, mask=mask)
    img = bg
    draw = ImageDraw.Draw(img)

    cx, cy = size / 2, size / 2

    # ── Hexagon outline ───────────────────────────────────────────────────────
    hex_r = 38 * scale
    verts = hex_vertices(cx, cy, hex_r, flat_top=True)
    hex_stroke = max(2, int(3 * scale))
    # Draw outline as filled polygon then inner filled polygon (manual stroke)
    draw.polygon(verts, outline=BLUE, fill=None)
    if size >= 32:
        # Thicker outline for larger sizes
        for offset in range(hex_stroke):
            r_off = hex_r - offset
            v_off = hex_vertices(cx, cy, r_off, flat_top=True)
            draw.polygon(v_off, outline=BLUE, fill=None)

    # ── Internal nodes ────────────────────────────────────────────────────────
    # Top-center, bottom-left, bottom-right (in the 100x100 coordinate space)
    nodes_100 = [(50, 34), (32, 65), (68, 65)]
    nodes = [(x * scale, y * scale) for x, y in nodes_100]

    if size >= 32:
        # Connecting lines
        line_w = max(1, int(1.8 * scale))
        teal_dim = (*TEAL, 178)  # 70% opacity
        for i in range(len(nodes)):
            for j in range(i + 1, len(nodes)):
                draw.line([nodes[i], nodes[j]], fill=TEAL, width=line_w)

    # Node dots
    node_r = max(2, int(5 * scale))
    for idx, (nx, ny) in enumerate(nodes):
        fill = TEAL if idx == 0 else BLUE
        draw.ellipse([nx - node_r, ny - node_r, nx + node_r, ny + node_r],
                     fill=fill)

    # ── Pulse arc (top-right hex edge) ───────────────────────────────────────
    # Only meaningful at 32px and above
    if size >= 32:
        arc_pts_100 = [(59, 16), (68, 10), (77, 20)]
        arc_pts = [(x * scale, y * scale) for x, y in arc_pts_100]
        arc_w = max(1, int(2.5 * scale))
        draw.line(arc_pts, fill=TEAL, width=arc_w)

    return img


def generate_all():
    os.makedirs(OUT_DIR, exist_ok=True)
    generated = []

    for size in SIZES:
        img = draw_icon(size)
        filename = f"hdim-icon-{size}.png"
        path = os.path.join(OUT_DIR, filename)
        img.save(path, "PNG", optimize=True)
        generated.append((size, path))
        print(f"  ✓ {filename}  ({size}×{size})")

    # favicon.ico — embed 16, 32, 48
    ico_sizes = [s for s in [16, 32, 48] if s in SIZES]
    ico_images = [draw_icon(s) for s in ico_sizes]
    ico_path = os.path.join(OUT_DIR, "favicon.ico")
    ico_images[0].save(
        ico_path,
        format="ICO",
        sizes=[(s, s) for s in ico_sizes],
        append_images=ico_images[1:],
    )
    print(f"  ✓ favicon.ico  (16, 32, 48)")

    print(f"\nAll assets written to: {OUT_DIR}/")
    return ico_path, generated


if __name__ == "__main__":
    print("\nGenerating HDIM Icon B — Hex Network\n")
    generate_all()
    print("\nDone.\n")
