import argparse
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from jmpstyle import itemview as iv
from jmpstyle import paths

CONTEXTS = [
    ("gui", "GUI"),
    ("fp_right", "1. Person rechts"),
    ("fp_left", "1. Person links"),
    ("tp_right", "3. Person rechts"),
    ("tp_left", "3. Person links"),
    ("ground", "Boden"),
    ("frame", "Rahmen"),
]

PAIRS = {
    "motor": [("m0802_22000", "m0802_19000"), ("m1404_4600", "m1404_3800"), ("m2207_1750", "m2207_1950"),
              ("m2807_1300", "m2807_1500"), ("m2807_1300", "m4214_400")],
    "prop": [("p31_3b", "p31_14_3b_bi"), ("p3_3b", "p76_25_3b_hard"), ("p51_3b", "p130_46_3b_hard"),
             ("p7_2b", "p178_40_2b_cfn"), ("p7_2b", "p330_70_2b_carbon")],
    "battery": [("b1s_300_hv", "b1s_450_hv"), ("b4s_650", "b4s_850"), ("b6s_1300", "b6s_2200"),
                ("b6s_4000_lion", "b6s_4500_lion"), ("b6s_1300", "b8s_5000")],
    "stack": [("aio_whoop", "aio_whoop_1s_6a"), ("aio_20", "aio_tp_4s_20a"), ("aio_20", "aio_20_6s_45a"),
              ("stack_30", "stack_30_6s_60a"), ("stack_30", "stack_30_8s_100a")],
    "frame": [("whoop_65", "whoop_75_duct"), ("cinewhoop_3", "cw_35_duct"), ("freestyle_5", "tp_3_x"),
              ("freestyle_5", "deadcat_5"), ("race_5", "race_5_stretched"), ("freestyle_5", "hframe_5"),
              ("long_range_7", "lr_7_deadcat"), ("long_range_7", "xclass_13")],
    "video": [("analog_nano", "analog_whoop_25mw"), ("analog_nano", "digital_micro"), ("analog_race", "analog_race_800"),
              ("analog_race", "analog_lr_1600"), ("digital_lite", "digital_low_latency"), ("digital_hd", "digital_hd_pro"),
              ("digital_hd", "digital_lr")],
}

BG = (30, 32, 36, 255)
HEAD = (230, 232, 236, 255)
SUB = (150, 156, 166, 255)
ACCENT = (255, 187, 0, 255)
CELL_W, CELL_H = 224, 156
LABEL_W = 176
GAP = 6


def font(size):
    for name in ("DejaVuSans.ttf", "Arial.ttf", "Helvetica.ttc"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def render_cell(store, item_id, context):
    if context == "gui":
        cell = Image.new("RGBA", (CELL_W, CELL_H), (44, 47, 53, 255))
        gui = iv.view_gui(store, item_id, size=CELL_H - 12, gui_scale=8)
        cell.alpha_composite(gui, ((CELL_W - gui.width) // 2, 6))
        return cell
    if context in ("fp_right", "fp_left"):
        return iv.view_first_person(store, item_id, CELL_W, CELL_H, left=context == "fp_left")
    if context in ("tp_right", "tp_left"):
        return iv.view_third_person(store, item_id, CELL_W, CELL_H, left=context == "tp_left")
    if context == "ground":
        return iv.view_ground(store, item_id, CELL_W, CELL_H)
    return iv.view_item_frame(store, item_id, CELL_W, CELL_H)


def scale_strip(entries, category, width):
    models = []
    for label, store, item_id in entries:
        layer = iv.render_layer(store, item_id, category)
        models.append((label, store, layer.model_id if layer else None, layer.local if layer else None))
    extents = []
    for _, store, model_id, local in models:
        pts = iv.placed_points(store, model_id, local) if model_id else None
        if pts is None:
            extents.append(0.0)
            continue
        pts = pts * 16.0 * 8.0
        extents.append(float(np.max(pts.max(axis=0) - pts.min(axis=0))))
    biggest = max(extents + [1.0])
    px_per_mm = min(4.0, (CELL_H - 44) / biggest * 1.2)
    strip = Image.new("RGBA", (width, CELL_H), (38, 41, 46, 255))
    draw = ImageDraw.Draw(strip)
    x = 10
    for (label, store, model_id, local), extent in zip(models, extents):
        if model_id is None:
            img = Image.new("RGBA", (170, CELL_H - 30), (0, 0, 0, 0))
            ImageDraw.Draw(img).text((4, 40), "kein Drohnen-Modell:\nPropwash zeichnet die\nForm aus der Definition", fill=SUB, font=font(11))
        else:
            img = iv.true_scale(store, model_id, px_per_mm, local=local)
            if img.height > CELL_H - 24:
                img = img.resize((max(1, int(img.width * (CELL_H - 24) / img.height)), CELL_H - 24), Image.NEAREST)
        if x + img.width > width - 10:
            break
        strip.alpha_composite(img, (x, CELL_H - 18 - img.height))
        caption = f"{label} ({extent:.0f} mm)" if extent else label
        draw.text((x, CELL_H - 16), caption, fill=SUB, font=font(11))
        x += max(img.width, int(font(11).getlength(caption)) if hasattr(font(11), "getlength") else 180) + 24
    ruler = 50.0 * px_per_mm
    draw.line((width - 20 - ruler, 12, width - 20, 12), fill=ACCENT, width=2)
    draw.text((width - 20 - ruler, 16), "50 mm", fill=ACCENT, font=font(11))
    draw.text((10, 6), "gemeinsamer Maßstab (Drohnen-Modelle, echte Größe)", fill=SUB, font=font(12))
    return strip


def sheet(category, pairs, base, sources, contexts):
    width = LABEL_W + len(contexts) * (CELL_W + GAP) + GAP
    rows_per_pair = 1 + len(sources)
    height = 44 + len(pairs) * (rows_per_pair * (CELL_H + GAP) + CELL_H + 3 * GAP)
    out = Image.new("RGBA", (width, height), BG)
    draw = ImageDraw.Draw(out)
    draw.text((GAP, 8), f"Propwash vs. JMP – {category}", fill=HEAD, font=font(18))
    for i, (_, name) in enumerate(contexts):
        draw.text((LABEL_W + i * (CELL_W + GAP) + 4, 28), name, fill=SUB, font=font(12))
    y = 44
    for base_key, jmp_key in pairs:
        rows = [("Propwash", base, f"propwash:{base_key}")]
        rows += [(label, store, f"{paths.NS}:{jmp_key}") for label, store in sources]
        for label, store, item_id in rows:
            draw.text((GAP, y + 6), label, fill=HEAD, font=font(13))
            draw.text((GAP, y + 24), item_id.split(":", 1)[1], fill=SUB, font=font(12))
            if store.json(item_id, "items") is None:
                draw.text((LABEL_W, y + 40), "Item fehlt", fill=ACCENT, font=font(12))
            else:
                drawn = bool(iv.item_geometry(store, item_id, "gui"))
                for i, (context, _) in enumerate(contexts):
                    out.alpha_composite(render_cell(store, item_id, context), (LABEL_W + i * (CELL_W + GAP), y))
                if not drawn:
                    draw.text((GAP, y + 44), "Sonder-Renderer ohne\nModell: prozedurale\nForm, hier nicht\nnachgebildet", fill=ACCENT,
                              font=font(11))
            y += CELL_H + GAP
        entries = [(f"{lab}: {iid.split(':', 1)[1]}", st, iid) for lab, st, iid in (rows[0], rows[-1])]
        strip = scale_strip(entries, category, width - LABEL_W - GAP)
        out.alpha_composite(strip, (LABEL_W, y))
        y += CELL_H + 3 * GAP
        draw.line((GAP, y - GAP - 2, width - GAP, y - GAP - 2), fill=(60, 64, 72, 255), width=1)
    return out


def size_of(store, item_id, category):
    layer = iv.render_layer(store, item_id, category)
    if layer is None:
        return None
    pts = iv.placed_points(store, layer.model_id, layer.local)
    if pts is None:
        return None
    return float(np.linalg.norm(pts.max(axis=0) - pts.min(axis=0)))


def auto_pairs(category, base, store):
    jmp_items = [k for k in store.items(paths.NS) if (paths.PARTS / category / f"{k}.json").exists()]
    base_items = [k for k in base.items("propwash") if iv.category_of(base, f"propwash:{k}") == category]
    sizes = {k: size_of(base, f"propwash:{k}", category) for k in base_items}
    sizes = {k: v for k, v in sizes.items() if v}
    out = []
    for key in jmp_items:
        size = size_of(store, f"{paths.NS}:{key}", category) or 1.0
        best = min(sizes, key=lambda k: abs(math.log(size / sizes[k]))) if sizes else None
        if best:
            out.append((best, key))
    return out


def main(argv=None):
    parser = argparse.ArgumentParser(description="Vergleichsbilder Propwash gegen JMP je Kategorie: GUI, erste und dritte "
                                                 "Person (beide Hände), Boden, Rahmen, dazu ein Streifen im gemeinsamen Maßstab.")
    parser.add_argument("--category", action="append", choices=sorted(PAIRS), help="Kategorie (mehrfach), Standard: alle")
    parser.add_argument("--pair", action="append", help="eigenes Paar basis=jmp, z. B. m2207_1750=m2207_1950 (mit --category)")
    parser.add_argument("--all", action="store_true", help="jedes JMP-Teil der Kategorie gegen das nächstgroße Basisteil")
    parser.add_argument("--jmp", action="append", help="JMP-Quelle LABEL=ORDNER (assets-Ordner), mehrfach; Standard: "
                                                       "„JMP-Ressourcen“ = Mod-Ressourcen und „Stil-API-Vorschau“ = build/style-preview/assets")
    parser.add_argument("--propwash-assets", help="Propwash-Assets (Ordner oder Jar), Standard: propwash-common aus mavenLocal")
    parser.add_argument("--out", default=str(paths.BUILD / "style-compare"), help="Zielordner der PNGs")
    parser.add_argument("--contexts", help="Auswahl, z. B. gui,fp_right,ground")
    args = parser.parse_args(argv)
    pw = paths.propwash_assets(args.propwash_assets)
    base = iv.AssetStore([pw])
    sources = []
    if args.jmp:
        for spec in args.jmp:
            label, folder = spec.split("=", 1)
            sources.append((label, iv.AssetStore([Path(folder), pw])))
    else:
        sources.append(("JMP-Ressourcen", iv.AssetStore([paths.ASSETS_ROOT, pw])))
        if (paths.PREVIEW_ASSETS / paths.NS).is_dir():
            sources.append(("Stil-API-Vorschau", iv.AssetStore([paths.PREVIEW_ASSETS, paths.ASSETS_ROOT, pw])))
    contexts = CONTEXTS
    if args.contexts:
        wanted = args.contexts.split(",")
        contexts = [c for c in CONTEXTS if c[0] in wanted]
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    categories = args.category or sorted(PAIRS)
    for category in categories:
        if args.pair:
            pairs = [tuple(p.split("=", 1)) for p in args.pair]
        elif args.all:
            pairs = auto_pairs(category, base, sources[-1][1])
        else:
            pairs = PAIRS[category]
        img = sheet(category, pairs, base, sources, contexts)
        file = out_dir / f"compare_{category}.png"
        img.save(file, optimize=True)
        print(f"{file} ({len(pairs)} Paare, {len(sources)} JMP-Quellen)")


if __name__ == "__main__":
    main()
