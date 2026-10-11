import json
import pathlib
import re
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
NS = "justmoreparts"
RES = ROOT / "common/src/main/resources"
PARTS = RES / "data" / NS / "propwash_parts"
LANG = ROOT / "common/src/lang"
CATEGORIES = ["frame", "stack", "motor", "prop", "video", "battery", "accessory"]
ORDER = ["chemistry"] + CATEGORIES
ITEM_MODEL = {"model": {"type": "minecraft:special", "base": "propwash:item/part_base", "model": {"type": "propwash:part"}}}
ID_RULES = {
    "motor": r"^m\d{4,5}_\d{3,5}$",
    "prop": r"^p\d{2,3}_\d{2,3}_\db_(pc|soft|hard|bi|gfn|cfn|carbon)(_duct)?$",
    "battery": r"^b\d{1,2}s_\d{3,5}(_hv|_lion|_ss)?$",
    "stack": r"^(aio|stack)_[a-z0-9_]+$",
    "frame": r"^[a-z0-9_]+$",
    "video": r"^(analog|digital)_[a-z0-9_]+$",
    "accessory": r"^[a-z0-9_]+$",
    "chemistry": r"^[a-z0-9_]+$",
}
BAD_GERMAN = ["fuer", "ueber", "groess", "waerme", "hoeher", "laenger", "kuerzer", "schwaecher", "staerker", "leistungsfaehig",
              "moeglich", "natuerlich", "zuverlaessig", "aehnlich", "flaeche", "hoechst", "duenn", "spaeter", "gross "]


PROP_MATERIAL = {
    "pc": ("PC", "PC"), "soft": ("soft PC", "weiches PC"), "hard": ("hard PC", "hartes PC"),
    "bi": ("bi-blend", "Bi-Blend"), "gfn": ("glass-fiber nylon", "Glasfaser-Nylon"),
    "cfn": ("carbon-fiber nylon", "Carbonfaser-Nylon"), "carbon": ("carbon", "Carbon"),
}
CHEMISTRY_NAMES = {
    "propwash:lipo": (None, None),
    "propwash:lihv": ("LiHV", "LiHV"),
    "propwash:lion": ("Li-Ion", "Li-Ion"),
    f"{NS}:lihv_ss": ("semi-solid LiHV", "Semi-Solid-LiHV"),
}
STACK_FORMS = {"aio_whoop": "Whoop", "aio_tp": "Toothpick", "aio_20": "20×20", "stack_20": "20×20", "stack_30": "30×30"}
NAME_BANS = {"en_us": ["Li-ion", "fibre", '"'], "de_de": ['"']}
TEXT_BANS = {"en_us": ["Li-ion", "fibre"], "de_de": ["Schutzring"]}
ASCII_INCH = re.compile(r'\d"')
BLADE_WORDS = {"en_us": {2: "two-blade", 3: "tri-blade", 4: "quad-blade", 5: "five-blade"},
               "de_de": {2: "Zweiblatt", 3: "Dreiblatt", 4: "Vierblatt", 5: "Fünfblatt"}}


def number(value, decimals, language):
    text = f"{value:.{decimals}f}" if decimals is not None else f"{value:.1f}".removesuffix(".0")
    return text.replace(".", ",") if language == "de_de" else text


def expected_name(category, path, data):
    if category == "motor":
        stator, kv = data["stator"], data["kv"]
        return {"en_us": f"{stator} Motor, {kv} KV", "de_de": f"{stator.replace('.', ',')}-Motor, {kv} KV"}
    if category == "prop":
        mm, pitch, blades = int(data["diameter_mm"]), float(data["pitch_in"]), int(data["blades"])
        material = PROP_MATERIAL[path.split("_")[3]]
        duct = path.endswith("_duct")
        inch = round(mm / 25.4, 1)
        en_size = f"{mm} mm" if mm < 50 else f"{number(inch, None, 'en_us')}″"
        de_size = f"{mm}-mm" if mm < 50 else f"{number(inch, None, 'de_de')}-Zoll"
        en = [f"{blades}-blade", f"{number(pitch, 1, 'en_us')}″ pitch", material[0]] + (["for ducts"] if duct else [])
        de = [f"{blades} Blätter", f"{number(pitch, 1, 'de_de')}″ Steigung", material[1]] + (["für Ducts"] if duct else [])
        return {"en_us": f"{en_size} Propeller ({', '.join(en)})", "de_de": f"{de_size}-Propeller ({', '.join(de)})"}
    if category == "battery":
        en, de = CHEMISTRY_NAMES[data.get("chemistry", "propwash:lipo")]
        cells, mah = data["cells"], data["capacity_mah"]
        return {"en_us": f"{cells}S {mah} mAh Battery" + (f" ({en})" if en else ""),
                "de_de": f"{cells}S-Akku {mah} mAh" + (f" ({de})" if de else "")}
    if category == "stack":
        form = next((v for k, v in STACK_FORMS.items() if path.startswith(k + "_")), None)
        if form is None:
            return None
        low, high = data["cells"]
        amps = data["esc_continuous_a"]
        attrs = f"({low}S, {amps:g} A)" if low == high else f"({low}–{high}S, {amps:g} A)"
        aio = data.get("all_in_one", False)
        return {"en_us": f"{form} {'AIO Board' if aio else 'FC/ESC Stack'} {attrs}",
                "de_de": f"{form}-{'AIO-Board' if aio else 'FC/ESC-Stack'} {attrs}"}
    return None


SUMMARY_DASH = " – "
FRAME_CLASSES = {"whoop": ("whoop", "Whoop"), "toothpick": ("toothpick", "Toothpick"), "cinewhoop": ("cinewhoop", "Cinewhoop"),
                 "freestyle": ("freestyle", "Freestyle"), "race": ("race", "Race"), "long_range": ("long-range", "Long-Range"),
                 "x_class": ("X-Class", "X-Class")}
FRAME_FORMS = {"en_us": {"true X", "stretched X", "squashed X", "deadcat", "H-frame"},
               "de_de": {"True-X", "Stretched-X", "Squashed-X", "Deadcat", "H-Frame"}}
HINTS = {"en_us": {"example"}, "de_de": {"Beispiel"}}
DATA_MATERIALS = {"polycarbonate": "pc", "glass_fiber_nylon": "gfn", "carbon_fiber_nylon": "cfn", "carbon": "carbon"}
STACK_MOUNTS = {"propwash:whoop_aio": "Whoop", "propwash:s20": "20×20", "propwash:s30": "30×30"}
CHEMISTRY_SHORT = {
    "propwash:lipo": ("LiPo", "LiPo"),
    "propwash:lihv": ("LiHV", "LiHV"),
    "propwash:lion": ("Li-Ion", "Li-Ion"),
    f"{NS}:lihv_ss": ("semi-solid LiHV", "Semi-Solid-LiHV"),
}
PARALLEL = {f"{NS}:b6s_8400_lion": 2}
FRAME_NAME = {"en_us": re.compile(r"^(?P<size>\d+ mm|\d+(?:\.\d)?″) .+ Frame(?: \((?P<extra>[^()]+)\))?$"),
              "de_de": re.compile(r"^(?P<size>\d+-mm|\d+(?:,\d)?-Zoll)-.+-Rahmen(?: \((?P<extra>[^()]+)\))?$")}
VIDEO_NAME = {"en_us": re.compile(r"^(?P<link>Analog|Digital)(?: (?P<cls>.+?))? (?:Camera \+ VTX|Video System)(?: \((?P<extra>[^()]+)\))?$"),
              "de_de": re.compile(r"^(?:(?P<analog>Analoge) (?P<acls>.+)-Kamera mit Sender|(?P<digital>Digitales) (?:(?P<dcls>.+)-)?Videosystem)"
                                  r"(?: \((?P<extra>[^()]+)\))?$")}
DEVICE_NAME = re.compile(r"^(?P<device>[^()]+?)(?: \((?P<extra>[^()]+)\))?$")
TITLE_WORD = re.compile(r"^[A-Z][a-z]")


def extras(text, language):
    return [item for item in (text or "").split(", ") if item and item not in HINTS[language]]


def lower_words(text):
    words = text.split(" ")
    return " ".join(words[:1] + [w.lower() if TITLE_WORD.match(w) and w != "X-Class" else w for w in words[1:]])


def prop_material(path, data):
    if re.match(ID_RULES["prop"], path):
        return path.split("_")[3]
    return DATA_MATERIALS.get(data.get("material", "polycarbonate"), "pc")


def prop_summary_heads(path, data, language):
    mm, blades = int(data["diameter_mm"]), int(data["blades"])
    material = PROP_MATERIAL[prop_material(path, data)][0 if language == "en_us" else 1]
    word = BLADE_WORDS[language][blades]
    inch = number(round(mm / 25.4, 1), None, language)
    if language == "en_us":
        base = f"{mm} mm {word}" if mm < 50 else f"{inch}″ {word}"
        duct = " for ducts"
    else:
        base = f"{mm}-mm-{word}" if mm < 50 else f"{inch}″-{word}"
        duct = " für Ducts"
    if path.endswith("_duct"):
        return [base + duct]
    return [f"{base}, {material}"]


def summary_heads(source, category, path, data, name, language):
    german = language == "de_de"
    if category == "motor":
        stator, kv = data["stator"], data["kv"]
        return [f"{stator.replace('.', ',')}-Motor, {kv} KV" if german else f"{stator} motor, {kv} KV"]
    if category == "prop":
        return prop_summary_heads(path, data, language)
    if category == "battery":
        chemistry = data.get("chemistry", "propwash:lipo")
        short = CHEMISTRY_SHORT.get(chemistry)
        if short is None:
            return None
        cells = f"{data['cells']}S" + (f"{PARALLEL[source.ns + ':' + path]}P" if source.ns + ":" + path in PARALLEL else "")
        return [f"{cells}{'-' if german else ' '}{short[1 if german else 0]}, {data['capacity_mah']} mAh"]
    if category == "stack":
        form = next((v for k, v in STACK_FORMS.items() if path.startswith(k + "_")), None) if source.ns == NS else None
        form = form or STACK_MOUNTS.get(data.get("mount", ""))
        if form is None:
            return None
        low, high = data["cells"]
        cells = f"{low}S" if low == high else f"{low}–{high}S"
        aio = data.get("all_in_one", False)
        device = ("-AIO" if aio else "-Stack") if german else (" AIO" if aio else " stack")
        return [f"{form}{device} ({cells}, {data['esc_continuous_a']:g} A)"]
    if category == "frame":
        match = FRAME_NAME[language].match(name)
        role = FRAME_CLASSES.get(data.get("role", ""))
        if not match or not role:
            return None
        size = match.group("size")
        if german and size.endswith("-Zoll"):
            size = size.removesuffix("-Zoll") + "″"
        variants = [e for e in extras(match.group("extra"), language) if e not in FRAME_FORMS[language]]
        suffix = f" ({', '.join(variants)})" if variants else ""
        return [f"{size}-{role[1]}-Rahmen{suffix}" if german else f"{size} {role[0]} frame{suffix}"]
    if category == "video":
        match = VIDEO_NAME[language].match(name)
        if not match:
            return None
        model = ", ".join(extras(match.group("extra"), language))
        if german:
            short = f"Analoge {match.group('acls')}-Kamera" if match.group("analog") else \
                f"Digitales {match.group('dcls') + '-' if match.group('dcls') else ''}Video"
        else:
            cls = lower_words("x " + match.group("cls")).removeprefix("x ").replace("long range", "long-range") if match.group("cls") else ""
            short = f"Analog {cls} camera" if match.group("link") == "Analog" else f"Digital {cls + ' ' if cls else ''}video"
        heads = [short + (f" ({model})" if model else "")]
        return heads + [model] if model else heads
    if category == "accessory":
        match = DEVICE_NAME.match(name)
        if not match:
            return None
        model = ", ".join(extras(match.group("extra"), language))
        device = match.group("device") if german else lower_words(match.group("device"))
        return [device + (f" ({model})" if model else "")]
    return None


def summary_problems(source, category, path, data, table, language):
    name = table.get(f"part.{source.ns}.{path}")
    summary = table.get(f"{source.ns}.help.{path}.summary")
    where = f"{source.label}{category}/{path}"
    if summary is None:
        return [f"{where}: {source.ns}.help.{path}.summary fehlt in {language}"]
    if name is None:
        return []
    heads = summary_heads(source, category, path, data, name, language)
    if heads is None:
        return [f"{where}: {language}-Name „{name}“ passt zu keinem Muster der Kurzkennung (docs/design/part-naming.md §6)"]
    head = next((h for h in heads if summary.startswith(h + SUMMARY_DASH)), None)
    if head is None:
        wanted = " oder ".join(f"„{h} – “" for h in heads)
        return [f"{where}: {language}-Kurzbeschreibung „{summary}“ beginnt nicht mit {wanted} (docs/design/part-naming.md §6)"]
    use = summary[len(head) + len(SUMMARY_DASH):]
    out = []
    if not use.strip() or SUMMARY_DASH in use or use.endswith("."):
        out.append(f"{where}: {language}-Kurzbeschreibung „{summary}“: nach der Kurzkennung genau ein Einsatz ohne Punkt (§6)")
    if language == "en_us" and TITLE_WORD.match(use):
        out.append(f"{where}: en_us-Kurzbeschreibung „{summary}“: Einsatz klein beginnen (§6)")
    return out


class Source:
    def __init__(self, label, ns, entries=None, tables=None):
        self.label, self.ns, self.entries, self.tables = label, ns, entries or [], tables or {}


JMP = Source("", NS)


def ordered(entries):
    return sorted(entries, key=lambda e: (ORDER.index(e[0]), e[1]))


def folder_source(label, ns, data_dir, lang_dirs):
    entries = [(c, f.stem, load_json(f)) for c in ORDER if (data_dir / c).is_dir() for f in sorted((data_dir / c).glob("*.json"))]
    tables = {}
    for language in ("de_de", "en_us"):
        merged = {}
        for folder in lang_dirs:
            if (folder / f"{language}.json").exists():
                merged.update(load_json(folder / f"{language}.json"))
        tables[language] = merged
    return Source(label, ns, ordered(entries), tables)


def propwash_sources(location):
    path = pathlib.Path(location)
    if path.is_dir():
        common = path / "common/src"
        lang_dirs = sorted(p for p in (common / "lang").iterdir() if p.is_dir()) if (common / "lang").is_dir() else []
        out = [folder_source("Propwash ", "propwash", common / "main/resources/data/propwash/propwash_parts", lang_dirs)]
        example = common / "devtest/resources"
        if (example / "data/propwash_example/propwash_parts").is_dir():
            out.append(folder_source("Propwash-Beispiel-Addon ", "propwash_example", example / "data/propwash_example/propwash_parts",
                                     [example / "assets/propwash_example/lang"]))
        return out
    if path.is_file():
        entries, tables = [], {}
        with zipfile.ZipFile(path) as z:
            for name in z.namelist():
                m = re.match(r"^data/propwash/propwash_parts/([a-z]+)/([a-z0-9_]+)\.json$", name)
                if m and m.group(1) in ORDER:
                    entries.append((m.group(1), m.group(2), json.loads(z.read(name))))
            for language in ("de_de", "en_us"):
                tables[language] = json.loads(z.read(f"assets/propwash/lang/{language}.json"))
        return [Source("Propwash ", "propwash", ordered(entries), tables)]
    return None


def audit(source):
    problems, count = [], 0
    for category, path, data in source.entries:
        if category == "chemistry":
            continue
        for language in ("de_de", "en_us"):
            table = source.tables.get(language, {})
            if f"part.{source.ns}.{path}" not in table:
                problems.append(f"{source.label}{category}/{path}: part.{source.ns}.{path} fehlt in {language}")
                continue
            problems.extend(summary_problems(source, category, path, data, table, language))
            summary = table.get(f"{source.ns}.help.{path}.summary", "")
            if ASCII_INCH.search(summary):
                problems.append(f"{source.label}{category}/{path}: {language} Zoll als ″ schreiben: „{summary}“")
            for banned in TEXT_BANS[language]:
                if banned in summary:
                    problems.append(f"{source.label}{category}/{path}: „{banned}“ in „{summary}“ (docs/design/part-naming.md)")
            count += 1
    return problems, count


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if not path.exists() or path.read_text(encoding="utf-8") != text:
        path.write_text(text, encoding="utf-8")
        return True
    return False


def parts():
    out = []
    for category in ORDER:
        folder = PARTS / category
        if not folder.is_dir():
            continue
        for file in sorted(folder.glob("*.json")):
            out.append((category, file.stem, load_json(file)))
    return out


def lang(language):
    merged = {}
    for module in sorted(p for p in LANG.iterdir() if p.is_dir()):
        file = module / f"{language}.json"
        if file.exists():
            merged.update(load_json(file))
    return merged


def propwash_jar():
    props = {}
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    version = props.get("propwash_version", "0.4.0")
    mc = props.get("minecraft_version", "26.3")
    jar = pathlib.Path.home() / f".m2/repository/de/lardtom/propwash-common-{mc}/{version}/propwash-common-{mc}-{version}.jar"
    return jar if jar.exists() else None


def recipe_signature(recipe):
    kind = recipe.get("type", "")
    if kind == "minecraft:crafting_shaped":
        key = recipe.get("key", {})
        rows = [row for row in recipe.get("pattern", [])]
        grid = tuple("".join(json.dumps(key.get(c, ""), sort_keys=True) if c != " " else "_" for c in row) for row in rows)
        return (kind, grid)
    if kind == "minecraft:crafting_shapeless":
        return (kind, tuple(sorted(json.dumps(i, sort_keys=True) for i in recipe.get("ingredients", []))))
    if kind == "minecraft:stonecutting":
        return (kind, json.dumps(recipe.get("ingredient"), sort_keys=True), json.dumps(recipe.get("result"), sort_keys=True))
    return (kind, json.dumps(recipe, sort_keys=True))


def advancement(category, path):
    return {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_propwash_part": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "#propwash:parts"}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": f"{NS}:{path}"}},
        },
        "requirements": [["has_propwash_part", "has_the_recipe"]],
        "rewards": {"recipes": [f"{NS}:{path}"]},
    }


def write():
    changed = 0
    by_category = {}
    for category, path, _ in parts():
        if category == "chemistry":
            continue
        by_category.setdefault(category, []).append(f"{NS}:{path}")
        item = RES / "assets" / NS / "items" / f"{path}.json"
        if not item.exists():
            changed += write_json(item, ITEM_MODEL)
        recipe = RES / "data" / NS / "recipe" / f"{path}.json"
        if recipe.exists():
            changed += write_json(RES / "data" / NS / "advancement" / "recipes" / category / f"{path}.json", advancement(category, path))
    for category in CATEGORIES:
        tag = RES / "data" / "propwash" / "tags" / "item" / "parts" / f"{category}.json"
        ids = sorted(by_category.get(category, []))
        if ids:
            changed += write_json(tag, {"replace": False, "values": ids})
        elif tag.exists():
            tag.unlink()
            changed += 1
    print(f"parts_sync --write: {changed} Dateien geschrieben")


def check(propwash=None):
    problems = []
    de, en = lang("de_de"), lang("en_us")
    all_parts = parts()
    seen = {}
    motor_mounts, stack_mounts = set(), set()
    for category, path, data in all_parts:
        if path in seen:
            problems.append(f"{category}/{path}: Id schon in {seen[path]} vergeben")
        seen[path] = category
        if not re.match(ID_RULES[category], path):
            problems.append(f"{category}/{path}: Id folgt nicht der Namensregel {ID_RULES[category]}")
        if category == "motor":
            motor_mounts.add(data.get("mount", ""))
        if category == "stack":
            stack_mounts.add(data.get("mount", ""))
        if category == "chemistry":
            if f"{NS}.chemistry.{path}" not in de or f"{NS}.chemistry.{path}" not in en:
                problems.append(f"chemistry/{path}: Name {NS}.chemistry.{path} fehlt in de_de oder en_us")
            continue
        if not (RES / "assets" / NS / "items" / f"{path}.json").exists():
            problems.append(f"{category}/{path}: Item-Symbol assets/{NS}/items/{path}.json fehlt (tools/parts_sync.py --write)")
        expected = expected_name(category, path, data)
        for language, table in (("de_de", de), ("en_us", en)):
            if f"part.{NS}.{path}" not in table:
                problems.append(f"{category}/{path}: part.{NS}.{path} fehlt in {language}")
            else:
                name = table[f"part.{NS}.{path}"]
                if expected and name != expected[language]:
                    problems.append(f"{category}/{path}: {language}-Name „{name}“ statt „{expected[language]}“ (docs/design/part-naming.md)")
                for banned in NAME_BANS[language]:
                    if banned in name:
                        problems.append(f"{category}/{path}: {language}-Name „{name}“ enthält „{banned}“ (docs/design/part-naming.md)")
            problems.extend(summary_problems(JMP, category, path, data, table, language))
            for key, value in table.items():
                if key.startswith(f"{NS}.help.{path}.what.") or key.startswith(f"{NS}.help.{path}.use."):
                    if len(value) > 48:
                        problems.append(f"{language} {key}: {len(value)} Zeichen (höchstens 48)")
        if not (RES / "data" / NS / "recipe" / f"{path}.json").exists() and data.get("creative", True):
            problems.append(f"{category}/{path}: Rezept data/{NS}/recipe/{path}.json fehlt")
    for category, path, data in all_parts:
        if category == "frame":
            for mount in data.get("motor_mounts", []):
                full = mount if ":" in mount else f"propwash:{mount}"
                if full.startswith(NS + ":") and full not in motor_mounts:
                    problems.append(f"frame/{path}: Motorhalterung {full} hat keinen Motor")
    for language, table in (("de_de", de), ("en_us", en)):
        for key, value in table.items():
            if ASCII_INCH.search(value):
                problems.append(f"{language} {key}: Zoll als ″ schreiben: „{value}“ (docs/design/part-naming.md §1.3)")
            for banned in TEXT_BANS[language]:
                if banned in value:
                    problems.append(f"{language} {key}: „{banned}“ in „{value}“ (docs/design/part-naming.md §1.1)")
    for key, value in de.items():
        low = value.lower()
        for bad in BAD_GERMAN:
            if bad in low:
                problems.append(f"de_de {key}: Umschreibung '{bad.strip()}' statt Umlaut/ß")
    for category in CATEGORIES:
        tag = RES / "data" / "propwash" / "tags" / "item" / "parts" / f"{category}.json"
        expected = sorted(f"{NS}:{p}" for c, p, _ in all_parts if c == category)
        actual = sorted(load_json(tag)["values"]) if tag.exists() else []
        if expected != actual:
            problems.append(f"Tag #propwash:parts/{category} passt nicht zu den Teile-Dateien (tools/parts_sync.py --write)")
    signatures = {}
    jar = propwash_jar()
    if jar:
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith("data/propwash/recipe/") and name.endswith(".json"):
                    signatures[recipe_signature(json.loads(z.read(name)))] = name
    else:
        problems.append("Propwash-Jar in mavenLocal nicht gefunden: Rezept-Kollisionen mit Propwash ungeprüft")
    for file in sorted((RES / "data" / NS / "recipe").glob("*.json")) if (RES / "data" / NS / "recipe").is_dir() else []:
        signature = recipe_signature(load_json(file))
        if signature in signatures:
            problems.append(f"Rezept {file.name} kollidiert mit {signatures[signature]}")
        signatures[signature] = file.name
    audited = []
    if propwash:
        sources = propwash_sources(propwash)
        if sources is None:
            problems.append(f"--propwash {propwash}: weder Propwash-Ordner noch Propwash-Jar")
        for source in sources or []:
            found, count = audit(source)
            problems.extend(found)
            audited.append(f"{source.label.strip()}: {len(source.entries)} Dateien, {count} Kurzbeschreibungen, {len(found)} Probleme")
    for problem in problems:
        print(problem)
    for line in audited:
        print(f"parts_sync --check --propwash: {line}")
    print(f"parts_sync --check: {len(all_parts)} Dateien, {len(problems)} Probleme")
    return 0 if not problems else 1


if __name__ == "__main__":
    args = sys.argv[1:]
    mode = args[0] if args else "--check"
    propwash = None
    if len(args) == 3 and args[1] == "--propwash":
        propwash = args[2]
    elif len(args) > 1:
        mode = ""
    if mode == "--write":
        write()
        sys.exit(check(propwash))
    if mode == "--check":
        sys.exit(check(propwash))
    print("Aufruf: python3 tools/parts_sync.py [--write|--check] [--propwash <Propwash-Ordner oder -Jar>]")
    sys.exit(2)
