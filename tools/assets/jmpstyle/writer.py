import json
from pathlib import Path

from . import common, display, paths

import addon
import kit
import paintable


CREDIT = "Propwash: Just More Parts - lardtom - tools/assets/style_generate.py"


class Output(addon.Output):
    def model(self, model, display):
        data = model.to_json(display)
        data["credit"] = CREDIT
        path = self.asset(model.model_id, "models", ".json")
        kit.dump_json(path, data)
        self.written.append(path)
        if self.bbmodel_root is not None:
            kit.dump_json(self.bbmodel_root / f"{model.category}/{model.key}.bbmodel", model.to_bbmodel(display))


class Writer:
    def __init__(self, assets_root, namespace=paths.NS, icons=True, oversize="scaled", bbmodel_root=None):
        common.setup(namespace)
        self.namespace = namespace
        self.root = Path(assets_root)
        self.out = Output(self.root, namespace, bbmodel_root)
        self.icons = icons
        self.oversize = oversize
        self.report = []

    def clean(self, category, key):
        base = self.root / self.namespace
        longer = [k for k in owned_keys(category) if k != key and k.startswith(key + "_")]
        patterns = [(base / "models/part" / category, f"{key}*.json"), (base / "models" / display.ICON_PREFIX / category, f"{key}*.json"),
                    (base / "textures/item/part" / category, f"{key}*.png"), (base / "items/drone_render" / category, f"{key}*.json")]
        for folder, pattern in patterns:
            if folder.is_dir():
                for file in folder.glob(pattern):
                    stem = file.stem
                    if any(stem == k or stem.startswith(k + "_") for k in longer):
                        continue
                    if stem == key or stem.startswith(key + "_"):
                        file.unlink()

    def simple(self, model, icon_source=None):
        model.pack()
        if display.fits(model):
            display_json, pre = display.compute(model)
            self.out.texture(*model.texture_map()["0"])
            self.out.model(model, display_json)
            self.out.render_item(model)
            addon.write_paint(self.out, model, display_json)
            if self.icons:
                self.out.icon(model.key, display.item_definition(model.model_id, pre))
            self.report.append((model.category, model.key, "modell"))
            return
        _, factor = display.icon_model(model)
        source = model
        if icon_source is not None:
            icon_source.key = model.key + "_icon"
            icon_source.pack()
            source = icon_source
        icon, icon_factor = display.icon_model(source)
        if icon is None:
            icon = display.scaled_copy(source, 1.0, f"{kit.NAMESPACE}:{display.ICON_PREFIX}/{source.category}/{source.key}")
        display_json, pre = display.compute(icon)
        self.out.texture(*source.texture_map()["0"])
        self.out.model(icon, display_json)
        if self.icons:
            self.out.icon(model.key, display.item_definition(icon.model_id, pre))
        if self.oversize == "scaled":
            if source is not model:
                self.out.texture(*model.texture_map()["0"])
            factor = display.render_factor(model)
            scaled = display.scaled_copy(model, factor, model.model_id)
            scaled_display, _ = display.compute(scaled)
            self.out.model(scaled, scaled_display)
            self.scaled_render_item(model.category, model.key, model.model_id, factor)
            variant = addon.write_paint(self.out, scaled, scaled_display)
            if variant is not None:
                self.scaled_render_item(model.category, variant.key, variant.model_id, factor,
                                        paintable.default_tints(variant.groups))
            self.report.append((model.category, model.key, f"skaliert {factor:.3f}"))
        else:
            self.report.append((model.category, model.key, f"nur Symbol (Faktor {factor:.3f}), an der Drohne Form aus der Definition"))

    def variant(self, model):
        model.pack()
        if not display.fits(model):
            self.report.append((model.category, model.key, "Variante passt nicht in die Modellgrenzen, an der Drohne das Grundmodell"))
            return
        display_json, _ = display.compute(model)
        self.out.texture(*model.texture_map()["0"])
        self.out.model(model, display_json)
        self.out.render_item(model)
        addon.write_paint(self.out, model, display_json)

    def scaled_render_item(self, category, key, model_id, factor, tints=None):
        path = self.root / self.namespace / "items/drone_render" / category / f"{key}.json"
        kit.dump_json(path, {"model": display.scaled_render_node(model_id, factor, tints)})
        if path not in self.out.written:
            self.out.written.append(path)

    def motor(self, model, seat):
        self.simple(model)
        self.out.motor_seats[model.key] = seat

    def prop(self, key, built):
        addon.write_props(self.out, key, built, "model" if self.icons else "none")
        self.report.append(("prop", key, "modell"))

    def finish(self, fragments=None):
        problems = paintable.check_references(self.out.paint_groups)
        if problems:
            raise RuntimeError("Lack-Referenzen zu hell: " + "; ".join(problems))
        base = self.root / self.namespace
        if self.out.paint_groups:
            merge_json(base / "drone_paint.json", paintable.summary_json(self.out.paint_groups), "models")
        if self.out.motor_seats:
            merge_json(base / "drone_layout.json", {"motor_seat": dict(sorted(self.out.motor_seats.items()))}, "motor_seat")
        if fragments:
            folder = Path(fragments)
            if self.out.paint_groups:
                kit.dump_json(folder / "drone_paint.json", {"models": paintable.summary_json(self.out.paint_groups)["models"]})
            if self.out.motor_seats:
                kit.dump_json(folder / "drone_layout.json", {"motor_seat": dict(sorted(self.out.motor_seats.items()))})
        return self.report


def owned_keys(category):
    folder = paths.PARTS / category
    return [file.stem for file in folder.glob("*.json")] if folder.is_dir() else []


def merge_json(path, fresh, section):
    data = {}
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
    for key, value in fresh.items():
        if key == section:
            merged = dict(data.get(key, {}))
            merged.update(value)
            data[key] = dict(sorted(merged.items()))
        elif key not in data:
            data[key] = value
    if section == "motor_seat":
        data.setdefault("frames", {})
    kit.dump_json(path, data)
