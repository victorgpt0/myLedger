#!/usr/bin/env python3
"""
Generates the myLedger app icon in every format the Android build and Play listing need.

Artwork: the hand-drawn, folded wad of banknotes with a paper band from the reference
`uglycash.webp`, with "KSh" on the front note. Geometry is traced from the 512px reference:

  - front note face (left): white paper, L-shaped inner frame, "KSh" where the reference has
    its top "100", "100" kept in the bottom corner, the edge of a printed head by the band
  - paper band around the fold
  - the top note curling up on the right, with the smiley portrait, cheek, shoulders and the
    wavy curl edge in ink
  - the notes underneath, peeking out along the left side and bottom

Style as in the reference: white paper with black ink outlines on a black background, the
paper showing as a white rim just beyond each ink outline.

Everything is drawn once on the 108x108 adaptive-icon grid, then centred and scaled to stay
inside the 66-unit safe zone (circle of radius 33 around (54, 54)). Outputs:

  app/src/main/res/drawable/ic_launcher_foreground.xml   adaptive foreground (vector)
  app/src/main/res/drawable/ic_launcher_background.xml   adaptive background (solid black)
  app/src/main/res/drawable/ic_launcher_monochrome.xml   Android 13+ themed layer: visible ink
                                                          lines only (themed icons use alpha only,
                                                          so filled paper would be a solid blob)
  app/src/main/res/mipmap-*/ic_launcher(.webp|_round.webp)  legacy icons for API 24-25
  app/src/main/ic_launcher-playstore.png                 512x512 Play Store icon
  docs/brand/app-icon.svg                                master artwork
  site/icon.png                                          website icon (GitHub Pages)

The hand-drawn wobble is seeded jitter, so output is reproducible. Edit and re-run from the
repo root:

    python3 tools/icon/generate_app_icon.py

Requires Pillow (for the raster outputs).
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"

SEED = 11
WHITE = (255, 255, 255)
BLACK = (0, 0, 0)
SAFE_RADIUS = 32.5  # keep everything inside this radius of (54, 54); adaptive safe zone is 33

rng = random.Random(SEED)


# --- geometry -----------------------------------------------------------------------------------

def lerp(a, b, t):
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)


def quad_map(quad, u, v):
    """Bilinear map of (u, v) in [0,1]^2 onto quad (TL, TR, BR, BL), so text follows the note's slant."""
    tl, tr, br, bl = quad
    return lerp(lerp(tl, tr, u), lerp(bl, br, u), v)


def densify(points, closed, step, jitter):
    """Split long segments and nudge points sideways so lines wobble like a quick pen stroke."""
    pts = points + [points[0]] if closed else points
    out = []
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        length = math.hypot(x1 - x0, y1 - y0)
        n = max(1, round(length / step))
        nx, ny = (-(y1 - y0) / length, (x1 - x0) / length) if length else (0, 0)
        for i in range(n):
            t = i / n
            j = rng.gauss(0, jitter) if i else rng.gauss(0, jitter * 0.5)
            out.append((x0 + (x1 - x0) * t + nx * j, y0 + (y1 - y0) * t + ny * j))
    if not closed:
        out.append(pts[-1])
    return out


def catmull_rom(points, closed):
    """Smooth curve through points as cubic Bézier segments [(p0, c1, c2, p1), ...]."""
    n = len(points)
    segs = []
    for i in range(n if closed else n - 1):
        p0 = points[(i - 1) % n] if closed else points[max(i - 1, 0)]
        p1 = points[i]
        p2 = points[(i + 1) % n]
        p3 = points[(i + 2) % n] if closed else points[min(i + 2, n - 1)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        segs.append((p1, c1, c2, p2))
    return segs


def bezier_points(segments, per_segment=10):
    out = []
    for p0, c1, c2, p1 in segments:
        for i in range(per_segment):
            t = i / per_segment
            mt = 1 - t
            out.append((
                mt ** 3 * p0[0] + 3 * mt * mt * t * c1[0] + 3 * mt * t * t * c2[0] + t ** 3 * p1[0],
                mt ** 3 * p0[1] + 3 * mt * mt * t * c1[1] + 3 * mt * t * t * c2[1] + t ** 3 * p1[1],
            ))
    out.append(segments[-1][3])
    return out


def path_data(segments, closed):
    f = lambda v: f"{v:.2f}".rstrip("0").rstrip(".")
    sx, sy = segments[0][0]
    parts = [f"M{f(sx)},{f(sy)}"]
    for _, c1, c2, p in segments:
        parts.append(f"C{f(c1[0])},{f(c1[1])} {f(c2[0])},{f(c2[1])} {f(p[0])},{f(p[1])}")
    if closed:
        parts.append("Z")
    return "".join(parts)


def inside(pt, polygon):
    """Even-odd point-in-polygon test."""
    x, y = pt
    hit = False
    for (x0, y0), (x1, y1) in zip(polygon, polygon[1:] + polygon[:1]):
        if (y0 > y) != (y1 > y) and x < x0 + (y - y0) * (x1 - x0) / (y1 - y0):
            hit = not hit
    return hit


class Shape:
    """A pen line, optionally closing a filled sheet of paper. Jittered once, at construction."""

    def __init__(self, points, width, color=BLACK, fill=None, rim=0.0, step=3.0, jitter=0.3):
        self.closed = fill is not None
        self.raw = densify(points, self.closed, step, jitter)
        self.width = width
        self.color = color
        self.fill = fill
        self.rim = rim  # white margin under the outline: the paper showing beyond the ink
        self.segments = None

    def finalize(self, transform, scale):
        self.segments = catmull_rom([transform(p) for p in self.raw], self.closed)
        self.width *= scale
        self.rim *= scale

    def path_data(self):
        return path_data(self.segments, self.closed)

    def sample(self, per_segment=10):
        return bezier_points(self.segments, per_segment)


# --- lettering ----------------------------------------------------------------------------------
# Glyphs are polylines in a 12-unit-tall design box; text() fits them into a sub-box of a quad.

GLYPHS = {
    "K": (6.0, [[(0, 0), (0, 12)], [(5.6, 0.2), (0.5, 6.6)], [(2.3, 4.9), (6.0, 12)]]),
    "S": (6.0, [[(5.6, 1.7), (4.4, 0.3), (2.7, 0), (1.0, 0.7), (0.4, 2.5), (1.3, 4.5), (3.1, 5.7),
                 (5.0, 7.0), (5.9, 8.9), (5.3, 11.0), (3.3, 12.0), (1.3, 11.6), (0.1, 10.2)]]),
    # Lowercase h: the arch springs from low on the stem so it can't be read as a capital H.
    "h": (5.8, [[(0, 0), (0, 12)], [(0, 9.4), (0.9, 7.4), (2.6, 6.4), (4.4, 6.6), (5.6, 8.2), (5.8, 12)]]),
    "1": (3.0, [[(0.2, 2.6), (2.4, 0), (2.4, 12)]]),
    "0": (5.0, [[(2.5 + 2.5 * math.cos(a), 6 + 6 * math.sin(a))
                 for a in (2 * math.pi * i / 14 - math.pi / 2 for i in range(15))]]),
}


def text(word, quad, box, gap=2.0):
    """Polylines for `word`, fitted into box=(u0, v0, u1, v1) of `quad`."""
    total = sum(GLYPHS[c][0] for c in word) + gap * (len(word) - 1)
    u0, v0, u1, v1 = box
    strokes = []
    x = 0.0
    for c in word:
        width, polylines = GLYPHS[c]
        for line in polylines:
            strokes.append([
                quad_map(quad, u0 + (u1 - u0) * (x + px) / total, v0 + (v1 - v0) * py / 12)
                for px, py in line
            ])
        x += width + gap
    return strokes


# --- artwork ------------------------------------------------------------------------------------
# Coordinates are traced from the 512px reference and converted to the 108 grid by P();
# fit_to_safe_zone() then centres and scales the drawing.

def P(x, y):
    return (x * 108 / 512, y * 108 / 512)


def Ps(*pts):
    return [P(x, y) for x, y in pts]


OUTLINE = 2.5   # paper edges (pre-scale units)
DETAIL = 2.2    # ink details
RIM = 1.2       # paper showing beyond the ink outline

FACE = Ps((172, 120), (282, 122), (201, 338), (104, 328))
BAND = Ps((282, 122), (306, 118), (225, 342), (201, 338))
CURL = Ps((306, 118), (318, 109), (356, 106), (392, 111), (420, 122), (441, 141), (436, 172), (430, 214),
          (416, 249), (396, 276), (378, 301), (364, 334), (350, 351), (300, 354), (225, 342))
WAD = FACE[:2] + CURL[:-1] + FACE[2:]  # whole front silhouette, used for the notes underneath


def paper(points):
    return Shape(points, width=OUTLINE, fill=WHITE, rim=RIM, step=3.2, jitter=0.32)


def ink(points, width=DETAIL, step=2.0, jitter=0.2):
    return Shape(points, width=width, step=step, jitter=jitter)


def build_shapes():
    s = []
    # Notes underneath: the front silhouette stepped left and down, so their edges peek out along
    # the left side and the bottom (the wad's thickness).
    for k in (2, 1):
        dx, dy = -15 * k * 108 / 512, 17 * k * 108 / 512
        s.append(paper([(x + dx, y + dy) for x, y in WAD]))

    # Front note face.
    s.append(paper(FACE))
    s.append(ink(Ps((168, 140), (122, 318), (192, 327)), step=3.0, jitter=0.3))     # inner frame
    for line in text("KSh", FACE, (0.2, 0.1, 0.84, 0.29), gap=2.6):
        s.append(ink(line, width=DETAIL * 0.85, step=1.4, jitter=0.08))
    for line in text("100", FACE, (0.32, 0.79, 0.74, 0.92), gap=1.6):
        s.append(ink(line, width=DETAIL * 0.85, step=1.4, jitter=0.1))
    s.append(ink(Ps((250, 226), (238, 221), (229, 229), (225, 245), (230, 259), (222, 286)), step=2.4))

    # Paper band around the fold.
    s.append(paper(BAND))

    # Top note curling over the fold, with the smiley portrait.
    s.append(paper(CURL))
    s.append(ink(Ps((362, 124), (340, 138), (322, 150), (306, 166), (292, 188), (281, 210),
                    (270, 238), (259, 266)), step=2.2, jitter=0.45))                       # curl edge
    s.append(ink(Ps((381, 182), (378, 207))))                                              # eyes
    s.append(ink(Ps((403, 179), (400, 202))))
    s.append(ink(Ps((365, 211), (371, 234), (384, 242), (397, 236), (405, 222)), step=1.6))  # smile
    s.append(ink(Ps((331, 224), (336, 249), (349, 264), (365, 273), (388, 277)), step=2.0))  # cheek
    s.append(ink(Ps((305, 245), (311, 275), (330, 299), (357, 307), (378, 304)), step=2.0))  # shoulders
    s.append(ink(Ps((306, 285), (316, 309), (340, 322), (366, 325)), step=2.0))
    return s


def fit_to_safe_zone(shapes):
    """Centre the drawing on (54, 54) and scale it so every point, plus pen and rim, fits SAFE_RADIUS."""
    pts = [p for sh in shapes for p in sh.raw]
    cx = (min(p[0] for p in pts) + max(p[0] for p in pts)) / 2
    cy = (min(p[1] for p in pts) + max(p[1] for p in pts)) / 2
    reach = max(
        math.hypot(x - cx, y - cy) + sh.width / 2 + sh.rim for sh in shapes for x, y in sh.raw
    )
    k = SAFE_RADIUS / reach
    transform = lambda p: (54 + (p[0] - cx) * k, 54 + (p[1] - cy) * k)
    for sh in shapes:
        sh.finalize(transform, k)
    return shapes


def visible_runs(shapes):
    """
    For the themed icon: each shape's line, minus the parts covered by paper drawn later.
    Returns [(points, width)] polylines.
    """
    papers = [(i, sh.sample(6)) for i, sh in enumerate(shapes) if sh.fill is not None]
    runs = []
    for i, sh in enumerate(shapes):
        pts = sh.sample(8)
        if sh.closed:
            pts = pts + pts[:1]
        covering = [poly for j, poly in papers if j > i]
        run = []
        for pt in pts:
            if any(inside(pt, poly) for poly in covering):
                if len(run) > 1:
                    runs.append((run, sh.width))
                run = []
            else:
                run.append(pt)
        if len(run) > 1:
            runs.append((run, sh.width))
    return runs


# --- writers ------------------------------------------------------------------------------------

def argb(rgb):
    return "#FF%02X%02X%02X" % rgb


def vector_xml(paths_xml, comment=""):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        f'<!-- Generated by tools/icon/generate_app_icon.py: edit the script, not this file.{comment} -->\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n'
        '    android:height="108dp"\n'
        '    android:viewportWidth="108"\n'
        '    android:viewportHeight="108">\n'
        f'{paths_xml}\n'
        '</vector>\n'
    )


def path_xml(data, fill=None, stroke=None, width=0.0):
    attrs = [f'android:pathData="{data}"']
    if fill is not None:
        attrs.append(f'android:fillColor="{argb(fill)}"')
    if stroke is not None and width > 0:
        attrs += [
            f'android:strokeColor="{argb(stroke)}"',
            f'android:strokeWidth="{width:.2f}"',
            'android:strokeLineCap="round"',
            'android:strokeLineJoin="round"',
        ]
    return "    <path\n        " + "\n        ".join(attrs) + " />"


def draw_list(shapes):
    """Paint order: every paper rim first (so a rim never covers ink drawn earlier), then each shape."""
    ops = [("rim", sh) for sh in shapes if sh.rim > 0]
    ops += [("main", sh) for sh in shapes]
    return ops


def write_vectors(shapes):
    fg = []
    for op, sh in draw_list(shapes):
        if op == "rim":
            fg.append(path_xml(sh.path_data(), fill=WHITE, stroke=WHITE, width=sh.width + 2 * sh.rim))
        else:
            fg.append(path_xml(sh.path_data(), fill=sh.fill, stroke=sh.color, width=sh.width))
    (RES / "drawable/ic_launcher_foreground.xml").write_text(vector_xml("\n".join(fg)))

    mono = []
    for pts, width in visible_runs(shapes):
        dense = pts[::3] + ([pts[-1]] if (len(pts) - 1) % 3 else [])
        mono.append(path_xml(path_data(catmull_rom(dense, False), False), stroke=WHITE, width=width))
    (RES / "drawable/ic_launcher_monochrome.xml").write_text(vector_xml(
        "\n".join(mono),
        "\n     Themed-icon layer (Android 13+): visible ink lines only; themed icons use alpha alone."))

    bg = path_xml("M0,0h108v108h-108z", fill=BLACK)
    (RES / "drawable/ic_launcher_background.xml").write_text(vector_xml(bg))


def write_svg(shapes, path):
    hexc = lambda c: "#%02x%02x%02x" % c
    els = []
    for op, sh in draw_list(shapes):
        if op == "rim":
            els.append(f'  <path d="{sh.path_data()}" fill="#fff" stroke="#fff" stroke-width="{sh.width + 2 * sh.rim:.2f}"/>')
        else:
            fill = hexc(sh.fill) if sh.fill else "none"
            els.append(f'  <path d="{sh.path_data()}" fill="{fill}" stroke="{hexc(sh.color)}" stroke-width="{sh.width:.2f}"/>')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="1080" height="1080"'
        ' stroke-linecap="round" stroke-linejoin="round">\n'
        '  <!-- Generated by tools/icon/generate_app_icon.py. 108-unit adaptive grid; safe zone r=33 at (54,54). -->\n'
        '  <rect width="108" height="108" fill="#000"/>\n'
        + "\n".join(els) + "\n</svg>\n"
    )


def stroke_poly(draw, pts, width, color, closed):
    r = width / 2
    draw.line(pts + ([pts[0]] if closed else []), fill=color, width=max(1, round(width)), joint="curve")
    for x, y in pts:  # round caps/joins
        draw.ellipse((x - r, y - r, x + r, y + r), fill=color)


def render(shapes, size, crop, shape, supersample=4):
    """Rasterise [crop[0], crop[1]] of the 108 grid to size x size in paint order, masked to shape."""
    big = size * supersample
    lo, hi = crop
    scale = big / (hi - lo)
    art = Image.new("RGB", (big, big), BLACK)
    draw = ImageDraw.Draw(art)
    for op, sh in draw_list(shapes):
        pts = [((x - lo) * scale, (y - lo) * scale) for x, y in sh.sample()]
        if op == "rim":
            draw.polygon(pts, fill=WHITE)
            stroke_poly(draw, pts, (sh.width + 2 * sh.rim) * scale, WHITE, True)
            continue
        if sh.fill is not None:
            draw.polygon(pts, fill=sh.fill)
        stroke_poly(draw, pts, sh.width * scale, sh.color, sh.closed)

    mask = Image.new("L", (big, big), 0)
    mdraw = ImageDraw.Draw(mask)
    if shape == "circle":
        mdraw.ellipse((0, 0, big - 1, big - 1), fill=255)
    elif shape == "rounded":
        mdraw.rounded_rectangle((0, 0, big - 1, big - 1), radius=round(big * 0.18), fill=255)
    else:
        mdraw.rectangle((0, 0, big, big), fill=255)

    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    out.paste(art, mask=mask)
    return out.resize((size, size), Image.LANCZOS)


LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def main():
    shapes = fit_to_safe_zone(build_shapes())

    write_vectors(shapes)
    write_svg(shapes, ROOT / "docs/brand/app-icon.svg")

    for density, px in LEGACY_SIZES.items():
        folder = RES / f"mipmap-{density}"
        render(shapes, px, (16, 92), "rounded").save(folder / "ic_launcher.webp", "WEBP", lossless=True)
        render(shapes, px, (14, 94), "circle").save(folder / "ic_launcher_round.webp", "WEBP", lossless=True)

    # Play Store: 512x512 full-bleed square; Play applies its own corner mask.
    render(shapes, 512, (12, 96), "square").convert("RGB").save(ROOT / "app/src/main/ic_launcher-playstore.png")

    # Website icon (GitHub Pages privacy policy / landing page).
    render(shapes, 192, (16, 92), "rounded").save(ROOT / "site/icon.png")

    print(f"Generated {len(shapes)} shapes → vectors (fg/bg/monochrome), "
          f"{2 * len(LEGACY_SIZES)} legacy webp, Play Store png, SVG.")


if __name__ == "__main__":
    main()
