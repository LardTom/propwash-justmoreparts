import argparse
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from PIL import Image, ImageDraw

from compare_render import font
from jmpstyle import itemview as iv, paths

BASE = {"p31_4b": 31.0, "p31_3b": 31.0, "p3_3b": 76.0, "p51_3b": 130.0, "p7_2b": 178.0}
VARIANTS = ("", "_ccw", "_blur", "_worn", "_damaged", "_broken")
CELL_W, CELL_H, LABEL_W = 150, 130, 190
BG = (30, 32, 36, 255)
HEAD = (230, 232, 236, 255)
SUB = (150, 156, 166, 255)
WARN = (255, 96, 80, 255)


def diameter(key):
    data = json.loads((paths.PARTS / "prop" / f"{key}.json").read_text(encoding="utf-8"))
    return float(data["diameter_mm"])


def cell(store, model_id, dia):
    img = iv.true_scale(store, model_id, (CELL_W - 32) / dia, rotation=(35, 225, 0))
    if img is None:
        return None
    if img.width > CELL_W - 4 or img.height > CELL_H - 18:
        f = min((CELL_W - 4) / img.width, (CELL_H - 18) / img.height)
        img = img.resize((max(1, int(img.width * f)), max(1, int(img.height * f))), Image.LANCZOS)
    return img


def main(argv=None):
    parser = argparse.ArgumentParser(description="Übersicht aller JMP-Props mit Varianten (GUI, cw, ccw, Scheibe, drei "
                                                 "Schadensstufen) unter den fünf Propwash-Props, je Zeile auf den Durchmesser skaliert.")
    parser.add_argument("--assets", default=str(paths.ASSETS_ROOT), help="assets-Ordner der JMP-Props (Standard: Mod-Ressourcen)")
    parser.add_argument("--propwash-assets", help="Propwash-Assets (Ordner oder Jar), Standard: propwash-common aus mavenLocal")
    parser.add_argument("--out", default=str(paths.BUILD / "style-compare" / "prop_variants.png"), help="Ziel-PNG")
    parser.add_argument("--only", action="append", help="nur diese Prop-Id (mehrfach möglich)")
    args = parser.parse_args(argv)
    store = iv.AssetStore([Path(args.assets), paths.propwash_assets(args.propwash_assets)])
    keys = sorted((p.stem for p in (paths.PARTS / "prop").glob("*.json")), key=lambda k: (diameter(k), k))
    if args.only:
        keys = [k for k in keys if k in args.only]
    rows = [("propwash", k, d) for k, d in BASE.items()] + [(paths.NS, k, diameter(k)) for k in keys]
    sheet = Image.new("RGBA", (LABEL_W + CELL_W * (1 + len(VARIANTS)), 24 + CELL_H * len(rows)), BG)
    draw = ImageDraw.Draw(sheet)
    draw.text((6, 6), "Props: Propwash oben, JMP darunter – GUI, cw, ccw, Scheibe, abgenutzt, beschädigt, gebrochen", fill=HEAD, font=font(13))
    for r, (ns, key, dia) in enumerate(rows):
        y = 24 + r * CELL_H
        draw.text((6, y + 6), ns, fill=SUB, font=font(11))
        draw.text((6, y + 20), key, fill=HEAD, font=font(12))
        draw.text((6, y + 36), f"{dia:.0f} mm", fill=SUB, font=font(11))
        sheet.alpha_composite(iv.view_gui(store, f"{ns}:{key}", size=CELL_H - 4, gui_scale=7), (LABEL_W, y + 2))
        for i, variant in enumerate(VARIANTS):
            x = LABEL_W + CELL_W * (i + 1)
            img = cell(store, f"{ns}:part/prop/{key}{variant}", dia)
            if img is None:
                draw.text((x + 6, y + 50), f"fehlt: {variant}", fill=WARN, font=font(11))
                continue
            sheet.alpha_composite(img, (x + (CELL_W - img.width) // 2, y + 14 + (CELL_H - 14 - img.height) // 2))
            draw.text((x + 4, y + 2), variant.lstrip("_") or "cw", fill=SUB, font=font(11))
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    print(f"{out} ({len(keys)} JMP-Props, {len(BASE)} Propwash-Props)")


if __name__ == "__main__":
    main()
