import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from PIL import Image, ImageDraw

import compare_render as cr
from jmpstyle import itemview as iv
from jmpstyle import motor, paths

BASE_MOTORS = ["m0802_22000", "m1404_4600", "m2004_3000", "m2207_1750", "m2306_1860", "m2807_1300"]
BG = (34, 36, 40, 255)
STRIP_BG = (38, 41, 46, 255)
BASE_TEXT = (255, 200, 80, 255)
JMP_TEXT = (220, 220, 220, 255)


def jmp_motors():
    out = []
    for file in (paths.PARTS / "motor").glob("*.json"):
        definition = json.loads(file.read_text(encoding="utf-8"))
        out.append((motor.stator_of(definition), definition.get("kv", 0), file.stem))
    return [key for _, _, key in sorted(out)]


def base_stator(key):
    return float(key[1:3]), float(key[3:5])


def jmp_stator(key):
    return motor.stator_of(json.loads((paths.PARTS / "motor" / f"{key}.json").read_text(encoding="utf-8")))


def stator_label(stator):
    diameter, height = stator
    rest = height - int(height)
    return f"{int(diameter):02d}{int(height):02d}" + (f"{rest:.1f}"[1:].replace(".", ",") if rest else "")


def gui_grid(base, jmp, keys, cell=150, cols=8):
    entries = [("Propwash", base, f"propwash:{k}", k) for k in BASE_MOTORS]
    entries += [("JMP", jmp, f"{paths.NS}:{k}", k) for k in keys]
    rows = (len(entries) + cols - 1) // cols
    img = Image.new("RGBA", (cols * cell, 30 + rows * (cell + 18)), BG)
    draw = ImageDraw.Draw(img)
    draw.text((8, 6), "Inventar (GUI) – gelb: Propwash, weiß: JMP", fill=BASE_TEXT, font=cr.font(14))
    for i, (label, store, item_id, key) in enumerate(entries):
        x, y = (i % cols) * cell, 30 + (i // cols) * (cell + 18)
        gui = iv.view_gui(store, item_id, size=cell - 10, gui_scale=8)
        img.alpha_composite(gui, (x + (cell - gui.width) // 2, y + 2))
        draw.text((x + 6, y + cell), key, fill=BASE_TEXT if label == "Propwash" else JMP_TEXT, font=cr.font(12))
    return img


def scale_line(base, jmp, keys, width, px_per_mm=4.0):
    first = {}
    for key in keys:
        first.setdefault(jmp_stator(key), key)
    line = [(base_stator(k), 0, "Propwash", base, k) for k in BASE_MOTORS]
    line += [(stator, 1, "JMP", jmp, k) for stator, k in first.items()]
    line.sort(key=lambda e: (e[0], e[1]))
    rows, row, x = [], [], 10
    for stator, _, label, store, key in line:
        ns = "propwash" if label == "Propwash" else paths.NS
        tile = iv.true_scale(store, f"{ns}:part/motor/{key}", px_per_mm)
        if row and x + tile.width > width - 10:
            rows.append(row)
            row, x = [], 10
        row.append((label, tile, stator, x))
        x += tile.width + 14
    rows.append(row)
    heights = [max(t.height for _, t, _, _ in r) + 30 for r in rows]
    img = Image.new("RGBA", (width, 44 + sum(heights)), STRIP_BG)
    draw = ImageDraw.Draw(img)
    draw.text((10, 6), "gemeinsamer Maßstab (Drohnen-Modelle, echte Größe), je Statorgröße ein Motor", fill=JMP_TEXT,
              font=cr.font(13))
    draw.line((10, 28, 10 + 50 * px_per_mm, 28), fill=BASE_TEXT, width=2)
    draw.text((14 + 50 * px_per_mm, 22), "50 mm", fill=BASE_TEXT, font=cr.font(12))
    y = 44
    for r, h in zip(rows, heights):
        for label, tile, stator, x in r:
            img.alpha_composite(tile, (x, y + h - 26 - tile.height))
            name = stator_label(stator)
            draw.text((x, y + h - 22), name, fill=BASE_TEXT if label == "Propwash" else JMP_TEXT, font=cr.font(12))
        y += h
    return img


def main(argv=None):
    parser = argparse.ArgumentParser(description="Übersicht aller JMP-Motoren neben den Propwash-Motoren: Inventar-Raster "
                                                 "und Aufreihung im gemeinsamen Maßstab.")
    parser.add_argument("--jmp-assets", default=str(paths.ASSETS_ROOT), help="assets-Ordner der JMP-Modelle")
    parser.add_argument("--propwash-assets", help="Propwash-Assets (Ordner oder Jar), Standard: propwash-common aus mavenLocal")
    parser.add_argument("--out", default=str(paths.BUILD / "style-compare" / "motor_overview.png"), help="Ziel-PNG")
    args = parser.parse_args(argv)
    pw = paths.propwash_assets(args.propwash_assets)
    base = iv.AssetStore([pw])
    jmp = iv.AssetStore([Path(args.jmp_assets), pw])
    keys = jmp_motors()
    grid = gui_grid(base, jmp, keys)
    line = scale_line(base, jmp, keys, grid.width)
    out = Image.new("RGBA", (grid.width, grid.height + line.height + 10), BG)
    out.alpha_composite(grid, (0, 0))
    out.alpha_composite(line, (0, grid.height + 10))
    target = Path(args.out)
    target.parent.mkdir(parents=True, exist_ok=True)
    out.save(target, optimize=True)
    print(f"{target} ({len(keys)} JMP-Motoren, {len(BASE_MOTORS)} Propwash-Motoren)")


if __name__ == "__main__":
    main()
