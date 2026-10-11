import json
import pathlib
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
NS = "justmoreparts"
RES = ROOT / "common/src/main/resources"
PARTS = RES / "data" / NS / "propwash_parts" / "prop"
RECIPES = RES / "data" / NS / "recipe"
LOOT = RES / "data" / NS / "loot_table" / "inject" / "prop"

DYES = {
    31: "minecraft:pink_dye", 40: "minecraft:magenta_dye", 45: "minecraft:purple_dye", 51: "minecraft:light_blue_dye",
    65: "minecraft:cyan_dye", 76: "minecraft:blue_dye", 89: "minecraft:lime_dye", 102: "minecraft:green_dye",
    130: "minecraft:orange_dye", 152: "minecraft:yellow_dye", 178: "minecraft:red_dye", 203: "minecraft:brown_dye",
    254: "minecraft:gray_dye", 305: "minecraft:light_gray_dye", 330: "minecraft:black_dye",
}
MARKERS = {
    "pc": None,
    "soft": "minecraft:slime_ball",
    "hard": "minecraft:flint",
    "bi": "minecraft:honeycomb",
    "gfn": "minecraft:glass_pane",
    "cfn": "propwash:carbon_plate",
    "carbon": "minecraft:quartz",
}
BLADE_ROWS = {2: None, 3: " B ", 4: "B B", 5: "BBB"}

INJECT = {
    "village_house": (["chests/village/village_plains_house", "chests/village/village_desert_house",
                       "chests/village/village_savanna_house", "chests/village/village_snowy_house",
                       "chests/village/village_taiga_house"], 88,
                      [("p31_14_3b_bi", 4), ("p40_17_3b_pc", 3), ("p31_16_2b_pc", 2)]),
    "village_toolsmith": (["chests/village/village_toolsmith"], 70,
                          [("p40_20_2b_pc", 4), ("p45_19_3b_pc", 4), ("p51_20_3b_pc", 3), ("p65_25_3b_pc", 3),
                           ("p40_14_5b_soft", 2)]),
    "abandoned_mineshaft": (["chests/abandoned_mineshaft"], 82,
                            [("p76_25_3b_hard", 3), ("p130_41_3b_soft", 3), ("p51_18_4b_pc", 2), ("p89_30_3b_pc", 2)]),
    "shipwreck_supply": (["chests/shipwreck_supply"], 75,
                         [("p130_36_3b_bi", 4), ("p76_30_2b_pc", 3), ("p130_40_2b_pc", 3), ("p45_17_4b_bi", 2)]),
    "simple_dungeon": (["chests/simple_dungeon"], 78,
                       [("p130_41_3b_soft", 4), ("p130_35_4b_pc", 3), ("p65_22_4b_bi", 2), ("p76_24_5b_pc_duct", 2)]),
    "pillager_outpost": (["chests/pillager_outpost"], 72,
                         [("p130_46_3b_hard", 5), ("p130_50_3b_gfn", 3), ("p102_30_3b_gfn", 2)]),
    "temple": (["chests/desert_pyramid", "chests/jungle_temple"], 75,
               [("p152_33_3b_pc", 3), ("p178_35_3b_pc", 3), ("p89_25_5b_pc_duct", 2), ("p102_25_2b_pc", 2)]),
    "stronghold": (["chests/stronghold_corridor", "chests/stronghold_crossing"], 70,
                   [("p178_40_2b_cfn", 4), ("p152_40_2b_gfn", 3), ("p203_45_2b_cfn", 2), ("p203_40_3b_gfn", 2),
                    ("p178_45_3b_gfn", 2)]),
    "trial_chambers": (["chests/trial_chambers/supply", "chests/trial_chambers/intersection"], 80,
                       [("p31_12_4b_hard", 3), ("p40_15_4b_pc", 3), ("p65_25_3b_pc", 2)]),
    "ancient_city": (["chests/ancient_city"], 70,
                     [("p254_45_3b_cfn", 4), ("p305_60_2b_cfn", 3), ("p330_60_3b_cfn", 2), ("p254_50_2b_carbon", 2)]),
    "end_city": (["chests/end_city_treasure"], 65,
                 [("p330_70_2b_carbon", 4), ("p254_50_2b_carbon", 4), ("p305_60_2b_cfn", 3)]),
}


def load_props():
    out = {}
    for path in sorted(PARTS.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        out[path.stem] = dict(diameter=int(data["diameter_mm"]), blades=int(data["blades"]), material=path.stem.split("_")[3])
    return out


def hub(diameter):
    if diameter <= 89:
        return "minecraft:iron_nugget"
    if diameter <= 203:
        return "minecraft:iron_ingot"
    return "minecraft:gold_ingot"


def count(diameter):
    return 2 if diameter >= 254 else 4


def recipe(key, p):
    marker = MARKERS[p["material"]]
    blade = "propwash:carbon_plate" if p["material"] == "carbon" else "minecraft:dried_kelp"
    keys = {"B": blade, "C": DYES[p["diameter"]], "N": hub(p["diameter"])}
    if marker is not None:
        keys["M"] = marker
    pattern = [("M" if marker else " ") + "C ", "BNB"]
    if BLADE_ROWS[p["blades"]]:
        pattern.append(BLADE_ROWS[p["blades"]])
    return {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "group": "propwash_prop",
        "key": dict(sorted(keys.items())),
        "pattern": pattern,
        "result": {"id": f"{NS}:{key}", "count": count(p["diameter"])},
    }


def loot_table(entries, empty):
    items = [{"type": "minecraft:empty", "weight": empty}]
    for key, weight in entries:
        items.append({"type": "minecraft:item", "name": f"{NS}:{key}", "weight": weight,
                      "modifier": {"type": "minecraft:set_count", "count": 4 if not key.startswith(("p254", "p305", "p330")) else 2}})
    return {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": items}]}


def dumps(data):
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def generate(props):
    files = {}
    for key, p in props.items():
        files[RECIPES / f"{key}.json"] = dumps(recipe(key, p))
    for table, (targets, empty, entries) in INJECT.items():
        files[LOOT / f"{table}.json"] = dumps(loot_table(entries, empty))
    return files


def shrink(pattern):
    rows = [r for r in pattern]
    while rows and not rows[0].strip():
        rows.pop(0)
    while rows and not rows[-1].strip():
        rows.pop()
    if not rows:
        return rows
    width = max(len(r) for r in rows)
    rows = [r.ljust(width) for r in rows]
    left = min(len(r) - len(r.lstrip()) for r in rows if r.strip())
    right = max(len(r.rstrip()) for r in rows)
    return [r[left:right] for r in rows]


def grid(recipe_data):
    key = recipe_data.get("key", {})
    rows = shrink(recipe_data.get("pattern", []))
    return tuple(tuple(json.dumps(key.get(c), sort_keys=True) if c != " " else "_" for c in row) for row in rows)


def signatures(recipe_data):
    kind = recipe_data.get("type", "")
    if kind != "minecraft:crafting_shaped":
        return {(kind, json.dumps(recipe_data, sort_keys=True))}
    g = grid(recipe_data)
    mirrored = tuple(tuple(reversed(row)) for row in g)
    return {(kind, g), (kind, mirrored)}


def propwash_recipes():
    props = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            props[k.strip()] = v.strip()
    version = props.get("propwash_version", "0.4.0")
    mc = props.get("minecraft_version", "26.3")
    jar = pathlib.Path.home() / f".m2/repository/de/lardtom/propwash-common-{mc}/{version}/propwash-common-{mc}-{version}.jar"
    out = {}
    if jar.exists():
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith("data/propwash/recipe/") and name.endswith(".json"):
                    out[name] = json.loads(z.read(name))
    return out, jar.exists()


def check(props, files):
    problems = []
    for path, content in files.items():
        if not path.exists():
            problems.append(f"fehlt: {path.relative_to(ROOT)}")
        elif path.read_text(encoding="utf-8") != content:
            problems.append(f"veraltet: {path.relative_to(ROOT)} (python3 tools/props/prop_data.py --write)")
    for folder in (LOOT,):
        if folder.is_dir():
            for path in folder.iterdir():
                if path not in files:
                    problems.append(f"überzählig: {path.relative_to(ROOT)}")
    stale = ROOT / "neoforge/src/main/resources/data" / NS / "loot_modifiers"
    if stale.exists():
        problems.append(f"überzählig: {stale.relative_to(ROOT)} (Beute hängt Propwash über PropwashApi.loot() ein)")
    seen = {}
    external, found = propwash_recipes()
    if not found:
        problems.append("Propwash-Jar in mavenLocal nicht gefunden: Kollisionen mit Propwash ungeprüft")
    for name, data in external.items():
        for sig in signatures(data):
            seen[sig] = name
    for path in sorted(RECIPES.glob("*.json")) if RECIPES.is_dir() else []:
        data = json.loads(path.read_text(encoding="utf-8"))
        own = signatures(data)
        for sig in own:
            if sig in seen and seen[sig] != path.name:
                problems.append(f"Rezept {path.name} gleicht {seen[sig]} (auch gespiegelt geprüft)")
        for sig in own:
            seen.setdefault(sig, path.name)
    looted = set()
    for table, (targets, empty, entries) in INJECT.items():
        for key, weight in entries:
            if key not in props:
                problems.append(f"Beute {table}: unbekanntes Teil {key}")
            looted.add(key)
    for key in props:
        if key not in looted:
            problems.append(f"{key}: in keiner Truhen-Beute")
    targets = [t for (ts, _, _) in INJECT.values() for t in ts]
    if len(targets) != len(set(targets)):
        problems.append("eine Vanilla-Truhe hat mehrere Prop-Tabellen")
    for problem in problems:
        print(problem)
    print(f"prop_data --check: {len(props)} Props, {len(files)} Dateien, {len(problems)} Probleme")
    return 0 if not problems else 1


def write(files):
    changed = 0
    for path, content in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists() or path.read_text(encoding="utf-8") != content:
            path.write_text(content, encoding="utf-8")
            changed += 1
    removed = 0
    for folder in (LOOT,):
        if folder.is_dir():
            for path in folder.iterdir():
                if path not in files:
                    path.unlink()
                    removed += 1
    print(f"prop_data: {changed} Dateien geschrieben, {removed} entfernt")


def main(argv):
    mode = argv[1] if len(argv) > 1 else "--check"
    props = load_props()
    files = generate(props)
    if mode == "--write":
        write(files)
        return check(props, files)
    if mode == "--check":
        return check(props, files)
    print("Aufruf: python3 tools/props/prop_data.py [--write|--check]")
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
