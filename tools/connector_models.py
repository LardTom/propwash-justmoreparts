import argparse
import copy
import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NS = "justmoreparts"
RES = ROOT / "common/src/main/resources"
PARTS = RES / "data" / NS / "propwash_parts"
ASSETS = RES / "assets" / NS

MOUNTED = "_mounted"
PAINT = "_paint"
PLUG_ELEMENTS = ("connector", "connector_grip", "bt_connector")
SOCKET_BODY = ("connector", "bt_connector")
POWER_LEAD = "wire"
PIGTAIL = "pigtail"
CREDIT = "Propwash: Just More Parts - lardtom - tools/connector_models.py"
YELLOW = ("XT30", "XT60", "XT90", "A30", "AS150")
WHITE = ("BT2.0", "BT3.0", "PH2.0")
MM_PER_UNIT = 8.0
ANCHOR = 8.0


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def text(data):
    return json.dumps(data, indent=2, ensure_ascii=False) + "\n"


def names(model):
    return [el.get("name", "") for el in model.get("elements", [])]


def has_plug(model):
    return any(name in PLUG_ELEMENTS for name in names(model))


def stack_ids(parts=PARTS):
    folder = parts / "stack"
    return sorted(file.stem for file in folder.glob("*.json")) if folder.is_dir() else []


def battery_ids(parts=PARTS):
    folder = parts / "battery"
    return sorted(file.stem for file in folder.glob("*.json")) if folder.is_dir() else []


def model_id(key):
    return f"{NS}:part/stack/{key}"


def mounted_key(key):
    if key.endswith(PAINT):
        return key[:-len(PAINT)] + MOUNTED + PAINT
    return key + MOUNTED


def mounted_model(model, key):
    leads = names(model).count(POWER_LEAD)
    if leads != 2:
        raise ValueError(f"stack/{key}: {leads} Elemente {POWER_LEAD}, erwartet 2")
    out = {}
    for field, value in model.items():
        if field == "elements":
            kept = []
            for el in value:
                if el.get("name", "") in PLUG_ELEMENTS:
                    continue
                el = copy.deepcopy(el)
                if el.get("name") == POWER_LEAD:
                    el["name"] = PIGTAIL
                kept.append(el)
            out[field] = kept
        elif field == "credit":
            out[field] = CREDIT
        else:
            out[field] = copy.deepcopy(value)
    return out


def swap_model(node, old, new):
    if isinstance(node, dict):
        return {k: (new if k == "model" and v == old else swap_model(v, old, new)) for k, v in node.items()}
    if isinstance(node, list):
        return [swap_model(v, old, new) for v in node]
    return node


def rotation(rot):
    if not isinstance(rot, dict):
        return None
    if "axis" in rot:
        angles = {"x": 0.0, "y": 0.0, "z": 0.0}
        if rot["axis"] not in angles:
            return None
        angles[rot["axis"]] = float(rot.get("angle", 0.0))
        x, y, z = angles["x"], angles["y"], angles["z"]
    else:
        x, y, z = (float(rot.get(a, 0.0)) for a in ("x", "y", "z"))
    if x == 0.0 and y == 0.0 and z == 0.0:
        return None
    return [math.radians(z), math.radians(y), math.radians(x)], rot.get("origin", [8, 8, 8])


def turn(point, angles, origin):
    rz, ry, rx = angles
    px, py, pz = (point[i] - origin[i] for i in range(3))
    py, pz = py * math.cos(rx) - pz * math.sin(rx), py * math.sin(rx) + pz * math.cos(rx)
    px, pz = px * math.cos(ry) + pz * math.sin(ry), -px * math.sin(ry) + pz * math.cos(ry)
    px, py = px * math.cos(rz) - py * math.sin(rz), px * math.sin(rz) + py * math.cos(rz)
    return [px + origin[0], py + origin[1], pz + origin[2]]


def box_mm(el):
    frm, to = el["from"], el["to"]
    rot = rotation(el.get("rotation"))
    corners = []
    for i in range(8):
        corner = [frm[0] if i & 1 == 0 else to[0], frm[1] if i & 2 == 0 else to[1], frm[2] if i & 4 == 0 else to[2]]
        corners.append(turn(corner, *rot) if rot else corner)
    lo = [min(c[a] for c in corners) for a in range(3)]
    hi = [max(c[a] for c in corners) for a in range(3)]
    return [(v - ANCHOR) * MM_PER_UNIT for v in lo], [(v - ANCHOR) * MM_PER_UNIT for v in hi]


def socket(model):
    body = None
    bt = False
    for el in model.get("elements", []):
        if el.get("name") in SOCKET_BODY:
            lo, hi = box_mm(el)
            body = (lo, hi) if body is None else ([min(a, b) for a, b in zip(body[0], lo)], [max(a, b) for a, b in zip(body[1], hi)])
            bt |= el.get("name") == "bt_connector"
    if body is None or any(hi - lo <= 0.0 for lo, hi in zip(*body)):
        return None
    return body, bt


def pigtail(model):
    if has_plug(model):
        return None
    leads = []
    for el in model.get("elements", []):
        if el.get("name") != PIGTAIL:
            continue
        lo, hi = box_mm(el)
        diameter = min(hi[0] - lo[0], hi[1] - lo[1])
        if diameter <= 0.0:
            return None
        leads.append(((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, hi[2], diameter))
    return sorted(leads) if len(leads) == 2 else None


def normalize(connector):
    return (connector or "").replace(" ", "").replace("-", "").upper()


def find(node, key):
    if isinstance(node, dict):
        for k, v in node.items():
            if k == key:
                return v
            got = find(v, key)
            if got is not None:
                return got
    return None


def plan(assets=ASSETS, parts=PARTS):
    files = {}
    paint_entries = {}
    plugged = []
    problems = []
    models = assets / "models/part/stack"
    items = assets / "items/drone_render/stack"
    paint = load(assets / "drone_paint.json").get("models", {}) if (assets / "drone_paint.json").exists() else {}
    for key in stack_ids(parts):
        base_file = models / f"{key}.json"
        if not base_file.exists():
            problems.append(f"stack/{key}: Grundmodell fehlt")
            continue
        if not has_plug(load(base_file)):
            continue
        plugged.append(key)
        for variant in (key, key + PAINT):
            source = models / f"{variant}.json"
            item = items / f"{variant}.json"
            if not source.exists() or not item.exists():
                if variant == key:
                    problems.append(f"stack/{variant}: Modell oder Render-Definition fehlt")
                continue
            target = mounted_key(variant)
            try:
                files[models / f"{target}.json"] = text(mounted_model(load(source), variant))
            except ValueError as e:
                problems.append(str(e))
                continue
            files[items / f"{target}.json"] = text(swap_model(load(item), model_id(variant), model_id(target)))
        group = f"stack/{key}"
        if group in paint:
            paint_entries[f"stack/{key}{MOUNTED}"] = copy.deepcopy(paint[group])
    return files, paint_entries, plugged, problems


def stale(assets, files):
    out = []
    for folder in (assets / "models/part/stack", assets / "items/drone_render/stack"):
        for file in sorted(folder.glob(f"*{MOUNTED}*.json")) if folder.is_dir() else []:
            if file not in files:
                out.append(file)
    for file in sorted((assets / "items").glob(f"*{MOUNTED}*.json")) if (assets / "items").is_dir() else []:
        out.append(file)
    return out


def paint_text(assets, entries):
    file = assets / "drone_paint.json"
    data = load(file) if file.exists() else {}
    models = {k: v for k, v in data.get("models", {}).items() if not (k.startswith("stack/") and MOUNTED in k)}
    models.update(entries)
    if "models" in data or models:
        data["models"] = dict(sorted(models.items()))
    return text(data)


def check_batteries(assets=ASSETS, parts=PARTS):
    problems = []
    count = 0
    for file in sorted((assets / "models/part/battery").glob("*.json")):
        count += 1
        key = file.stem
        found = socket(load(file))
        if found is None:
            problems.append(f"battery/{key}: keine Buchse (Element connector oder bt_connector)")
            continue
        base = key
        for suffix in (PAINT, "_under"):
            if base.endswith(suffix):
                base = base[:-len(suffix)]
        definition = parts / "battery" / f"{base}.json"
        connector = normalize(find(load(definition), "connector")) if definition.exists() else ""
        if not connector:
            continue
        if connector not in YELLOW and connector not in WHITE:
            problems.append(f"battery/{key}: Steckerart {connector} ohne Farbe im Vertrag")
        elif (connector in WHITE) != found[1]:
            problems.append(f"battery/{key}: Steckerart {connector} passt nicht zum Element {'bt_connector' if found[1] else 'connector'}")
    return problems, count


def check_mounted(assets, files, plugged):
    problems = []
    models = assets / "models/part/stack"
    for key in plugged:
        for variant in (key, key + PAINT):
            target = models / f"{mounted_key(variant)}.json"
            if target not in files:
                continue
            model = json.loads(files[target])
            got = names(model)
            if any(name in PLUG_ELEMENTS for name in got):
                problems.append(f"stack/{target.stem}: Stecker-Element vorhanden")
            if got.count(PIGTAIL) != 2 or POWER_LEAD in got:
                problems.append(f"stack/{target.stem}: {got.count(PIGTAIL)} Elemente {PIGTAIL} statt 2")
            if pigtail(model) is None:
                problems.append(f"stack/{target.stem}: Pigtail-Enden nicht lesbar")
            if model.get("textures") != load(models / f"{variant}.json").get("textures"):
                problems.append(f"stack/{target.stem}: andere Textur als das Grundmodell")
    return problems


def main(argv=None):
    parser = argparse.ArgumentParser(description="Erzeugt die gesteckten Stack-Varianten _mounted (+ _mounted_paint) aus den "
                                                 "JMP-Stack-Modellen und prüft Akku-Buchsen nach dem Vertrag „Akku gesteckt“.")
    parser.add_argument("--check", action="store_true", help="nichts schreiben, nur prüfen (Rückgabe 1 bei Befunden)")
    parser.add_argument("--assets", default=str(ASSETS), help="Ordner assets/justmoreparts")
    parser.add_argument("--parts", default=str(PARTS), help="Ordner data/justmoreparts/propwash_parts")
    args = parser.parse_args(argv)
    assets, parts = Path(args.assets), Path(args.parts)
    files, entries, plugged, problems = plan(assets, parts)
    paint_file = assets / "drone_paint.json"
    wanted_paint = paint_text(assets, entries)
    old = stale(assets, files)
    changed = [path for path, content in files.items() if not path.exists() or path.read_text(encoding="utf-8") != content]
    paint_changed = not paint_file.exists() or paint_file.read_text(encoding="utf-8") != wanted_paint
    battery_problems, batteries = check_batteries(assets, parts)
    problems += check_mounted(assets, files, plugged) + battery_problems
    if args.check:
        problems += [f"{path.relative_to(assets).as_posix()}: fehlt oder veraltet (python3 tools/connector_models.py)" for path in changed]
        problems += [f"{path.relative_to(assets).as_posix()}: gehört zu keinem Stack mit Stecker" for path in old]
        if paint_changed:
            problems.append("drone_paint.json: Lack-Gruppen stack/<id>_mounted fehlen oder veraltet (python3 tools/connector_models.py)")
    else:
        for path in changed:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(files[path], encoding="utf-8")
        for path in old:
            path.unlink()
        if paint_changed:
            paint_file.write_text(wanted_paint, encoding="utf-8")
    for line in problems:
        print(line)
    action = "geprüft" if args.check else f"{len(changed)} geschrieben, {len(old)} entfernt{', drone_paint.json aktualisiert' if paint_changed else ''}"
    print(f"connector_models: {len(plugged)} Stacks mit Stecker, {len(files)} Dateien _mounted ({action}), "
          f"{len(entries)} Lack-Gruppen, {batteries} Akku-Modelle mit Buchse geprüft – {'OK' if not problems else f'{len(problems)} Befunde'}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
