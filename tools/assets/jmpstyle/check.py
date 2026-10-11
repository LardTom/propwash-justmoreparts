import hashlib
import json
from pathlib import Path

from PIL import Image

from . import display, paths

import paintable

CONTEXTS = ("gui", "ground", "fixed", "on_shelf", "thirdperson_righthand", "firstperson_righthand", "head")
TEXTURE_SIZES = (32, 64)


def split(resource_id):
    ns, path = resource_id.split(":", 1)
    return ns, path


def model_refs(node, out):
    if isinstance(node, dict):
        if node.get("type", "").endswith("model") and "model" in node and isinstance(node["model"], str):
            out.append(node["model"])
        if node.get("type", "").endswith("special") and isinstance(node.get("base"), str):
            out.append(node["base"])
        for value in node.values():
            model_refs(value, out)
    elif isinstance(node, list):
        for value in node:
            model_refs(value, out)


def check(assets_root, namespace=paths.NS, keys=None):
    root = Path(assets_root) / namespace
    problems = []
    models = sorted((root / "models").rglob("*.json")) if (root / "models").is_dir() else []
    for file in models:
        rel = file.relative_to(root / "models").with_suffix("").as_posix()
        if keys and not any(rel.split("/")[-1].startswith(k) for k in keys):
            continue
        data = json.loads(file.read_text(encoding="utf-8"))
        for el in data.get("elements", []):
            for v in el["from"] + el["to"]:
                if v < display.MODEL_MIN or v > display.MODEL_MAX:
                    problems.append(f"{rel}: Element {el.get('name')} außerhalb {display.MODEL_MIN:g}…{display.MODEL_MAX:g}")
                    break
        for key, sprite in data.get("textures", {}).items():
            if isinstance(sprite, dict):
                sprite = sprite["sprite"]
            if sprite.startswith("#"):
                continue
            ns, path = split(sprite)
            png = Path(assets_root) / ns / "textures" / f"{path}.png"
            if not png.exists():
                problems.append(f"{rel}: Textur {sprite} fehlt")
                continue
            with Image.open(png) as img:
                if img.size[0] != img.size[1] or img.size[0] not in TEXTURE_SIZES:
                    problems.append(f"{rel}: Textur {sprite} ist {img.size[0]}×{img.size[1]} (erlaubt 32² oder 64², Basis 64²)")
        if rel.startswith("part/") or rel.startswith(display.ICON_PREFIX + "/"):
            shown = data.get("display", {})
            for context in CONTEXTS:
                if context not in shown:
                    problems.append(f"{rel}: display.{context} fehlt")
                elif max(abs(s) for s in shown[context].get("scale", [1, 1, 1])) > 4.0:
                    problems.append(f"{rel}: display.{context}.scale über 4")
            if data.get("gui_light") != "side":
                problems.append(f"{rel}: gui_light ist nicht side")
    items = sorted((root / "items").glob("*.json")) if (root / "items").is_dir() else []
    for file in items:
        if keys and file.stem not in keys:
            continue
        data = json.loads(file.read_text(encoding="utf-8"))
        refs = []
        model_refs(data.get("model", {}), refs)
        for ref in refs:
            ns, path = split(ref)
            if ns == namespace and not (Path(assets_root) / ns / "models" / f"{path}.json").exists():
                problems.append(f"items/{file.stem}: Modell {ref} fehlt")
        if data.get("model", {}).get("type") == "minecraft:special" and data["model"].get("base") == "propwash:item/part_base":
            problems.append(f"items/{file.stem}: Sonder-Renderer mit part_base statt eigenem Modell (Anzeige weicht von der Basis ab)")
    paint_file = root / "drone_paint.json"
    if paint_file.exists():
        groups = json.loads(paint_file.read_text(encoding="utf-8")).get("models", {})
        as_tuples = {name: [(g["channel"], g["base"]) for g in entries] for name, entries in groups.items()}
        problems += paintable.check_references(as_tuples)
    return problems


def vendored_intact():
    problems = []
    sums = paths.STYLE / "SHA256SUMS"
    if not sums.exists():
        return ["propwash_style/SHA256SUMS fehlt"]
    for line in sums.read_text(encoding="utf-8").splitlines():
        digest, name = line.split(None, 1)
        file = paths.STYLE / name.strip()
        if not file.exists():
            problems.append(f"propwash_style/{name.strip()} fehlt")
        elif hashlib.sha256(file.read_bytes()).hexdigest() != digest:
            problems.append(f"propwash_style/{name.strip()} weicht von der Propwash-Quelle ab (Kopie muss unverändert bleiben)")
    return problems
