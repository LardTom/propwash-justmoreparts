import json
from pathlib import Path

from . import battery, display, frame, motor, paths, prop, stack, video

CATEGORIES = ("motor", "prop", "battery", "stack", "frame", "video")
BUILDERS = {"battery": battery.build, "stack": stack.build, "frame": frame.build, "video": video.build}
OVERRIDES = {
    "digital_hd_pro": {"lens": 18.0, "label": "PRO"},
}


def load(parts_dir=paths.PARTS, categories=CATEGORIES, only=None):
    out = []
    for category in categories:
        folder = Path(parts_dir) / category
        if not folder.is_dir():
            continue
        for file in sorted(folder.glob("*.json")):
            if only and file.stem not in only:
                continue
            out.append((category, file.stem, json.loads(file.read_text(encoding="utf-8"))))
    return out


def build_into(writer, category, key, definition, overrides=None):
    if overrides is None:
        overrides = OVERRIDES.get(key)
    if category == "motor":
        model, seat = motor.build(key, definition, overrides)
        writer.motor(model, seat)
    elif category == "prop":
        writer.prop(key, prop.build(key, definition, overrides))
    else:
        model = BUILDERS[category](key, definition, overrides)
        icon_source = None
        if category == "frame" and not display.fits(model):
            icon_source = frame.build(key, definition, dict(overrides or {}, icon=True))
        writer.simple(model, icon_source)
        if category == "battery":
            under = battery.build_under(key, definition, overrides)
            if under is not None:
                writer.variant(under)
