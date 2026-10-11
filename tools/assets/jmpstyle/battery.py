import math

import numpy as np

from . import common, palette, paths

import batteries
import kit
import paint
from paint import hexc
from stacks import lead, xt_connector

LIPO_DENSITY = 2.1
WHOOP_DENSITY = 1.6
ASPECT = 2.2

CONNECTORS = {
    "ph": dict(w=4.6, h=3.4, l=6.0, wire=0.9, body="bt_white", lead=6.0),
    "bt": dict(w=6.4, h=4.5, l=5.0, wire=1.0, body="bt_white", lead=7.0),
    "bt3": dict(w=7.6, h=5.2, l=6.0, wire=1.2, body="bt_white", lead=7.0),
    "a30": dict(w=8.6, h=4.8, l=9.0, wire=1.6, body="xt_black", lead=10.0),
    "xt30": dict(w=10.2, h=5.6, l=11.0, wire=2.0, body="xt_yellow", lead=14.0),
    "xt60": dict(w=16.0, h=8.0, l=15.5, wire=3.0, body="xt_yellow", lead=14.0),
    "xt90": dict(w=21.0, h=10.4, l=20.5, wire=4.0, body="xt_yellow", lead=18.0),
    "as150": dict(w=25.0, h=12.0, l=27.0, wire=5.2, body="xt_yellow", lead=22.0),
}


def connector_of(definition, cells):
    raw = str(definition.get("connector", "")).lower().replace(".", "").replace(" ", "")
    table = {"ph20": "ph", "bt20": "bt", "bt30": "bt3", "a30": "a30", "xt30": "xt30", "xt60": "xt60", "xt90": "xt90",
             "as150": "as150"}
    if raw in table:
        return table[raw]
    return "bt" if cells == 1 else "xt60" if cells >= 5 else "xt30"


def chemistry_of(definition):
    value = str(definition.get("chemistry", "propwash:lipo")).split(":")[-1]
    if value == "lion":
        return "lion"
    if value.endswith("_ss"):
        return "semi_solid"
    if value == "lihv":
        return "lihv"
    return "lipo"


MAX_CELL_MM = 8.8


def cell_thickness(capacity):
    return min(4.2 + capacity / 800.0, MAX_CELL_MM)


def aspect(capacity):
    return common.interp(capacity, [1500.0, 2200.0, 5000.0], [ASPECT, 2.9, 3.1], extrapolate="hold")


def pouch_dims(definition):
    pack = definition.get("pack_mm")
    if isinstance(pack, list) and len(pack) == 3:
        return float(pack[1]), float(pack[2]), float(pack[0]), 1
    cells = int(definition["cells"])
    capacity = float(definition["capacity_mah"])
    mass = float(definition["mass_g"])
    if cells == 1:
        w = 11.0 + capacity / 200.0
        h = 5.0 + capacity / 120.0
        l = mass / WHOOP_DENSITY * 1000.0 / (w * h)
        return round(w, 2), round(h, 2), round(l, 2), 1
    volume = mass / LIPO_DENSITY * 1000.0
    h = cells * cell_thickness(capacity)
    w = math.sqrt(volume / h / aspect(capacity))
    l = volume / (w * h)
    return round(w, 2), round(h, 2), round(l, 2), 1


def lipo_params(key, definition, overrides=None):
    cells = int(definition["cells"])
    capacity = int(definition["capacity_mah"])
    chemistry = chemistry_of(definition)
    w, h, l, rows = pouch_dims(definition)
    accent = palette.accent(definition, palette.band(cells, capacity))
    if cells == 1:
        wrap = palette.fit(accent, "battery")
        band = "#f2f2f2"
        text = wrap
    else:
        wrap = palette.PACK_SEMI_SOLID if chemistry == "semi_solid" else palette.PACK_DARK
        band = accent
        text = palette.readable_on(band)
    p = dict(w=w, h=h, l=l, base=wrap, band=band, t1=f"{cells}S", t2=str(capacity), text=text,
             connector=connector_of(definition, cells), density=round(common.clamp(390.0 / max(w, h, l), 2.2, 8.0), 2),
             strap=cells >= 2 and (cells >= 5 or l >= 70.0), cells=cells, chemistry=chemistry, rows=rows,
             stripe=band if chemistry == "semi_solid" else None)
    if overrides:
        p.update(overrides)
    return p


def plug(m, kind, zr, y_top):
    c = CONNECTORS[kind]
    if kind in ("ph", "bt", "bt3"):
        lead(m, -c["wire"] * 1.2, y_top - 2.0, zr, zr + c["lead"], c["wire"], "wire_red")
        lead(m, c["wire"] * 1.2, y_top - 2.0, zr, zr + c["lead"], c["wire"], "wire_black")
        m.box(-c["w"] / 2, y_top - 0.5 - c["h"], zr + c["lead"], c["w"] / 2, y_top - 0.5, zr + c["lead"] + c["l"], c["body"],
              name="bt_connector")
        return
    dia = c["wire"]
    y = y_top - max(4.0, dia + 1.0)
    lead(m, -dia * 0.8, y, zr, zr + c["lead"], dia, "wire_red")
    lead(m, dia * 0.8, y, zr, zr + c["lead"], dia, "wire_black")
    xt_connector(m, zr + c["lead"], y - c["h"] / 2, c["w"], c["h"], c["l"], body=c["body"])


def build_lipo(key, p, under=False):
    density = p["density"]
    suffix = batteries.UNDER_SUFFIX if under else ""
    m = kit.Model("battery", key + suffix, density=density, gui=(30, 225, 0))
    mat = common.material(f"wrap_{key}{suffix}",
                          batteries.wrap(p["base"], p["band"], p["t1"], p["t2"], p["text"], p.get("stripe"), under=under),
                          "battery", "battery", p["base"])
    w, h, l = p["w"], p["h"], p["l"]
    y0 = -h if under else 0.0
    top = y0 + h
    m.box(-w / 2, y0, -l / 2, w / 2, top, l / 2, mat, name="pack",
          faces={"north": "pack_end", "south": "pack_end"},
          decals={"east": batteries.label_decal(p["band"], p["t1"], p["t2"], p["text"], True),
                  "west": batteries.label_decal(p["band"], p["t1"], p["t2"], p["text"], False)})
    zr = l / 2
    kind = p["connector"]
    plug(m, kind, zr, top)
    if p["cells"] > 1:
        batteries.balance_lead(m, w / 2 - min(5.0, w * 0.2), top, zr, p["cells"])
    if p["strap"]:
        strap_w = common.clamp(l * 0.2, 12.0, 24.0)
        band = batteries.strap_band_under if under else batteries.strap_band
        band(m, w, h, l / 2 - strap_w - 1.0, strap_w)
    return m


def lion_label(text_top, text_bottom, teal, turn=False):
    accent = hexc(teal)

    def painter(region, ctx):
        paint.fill(region, hexc("#17191d"))
        paint.noise(region, ctx, 0.02)
        if turn and ctx.side == "top":
            region = np.rot90(region, -1)
        h, w = region.shape[:2]
        if ctx.side == "top" and turn and h >= 9:
            paint.rect(region, 0, 1, w, 2, accent)
            paint.rect(region, 0, h - 2, w, h - 1, accent)
        elif ctx.side == "top" and h >= 13:
            paint.rect(region, 0, 1, w, 2, accent)
            paint.rect(region, 0, h - 2, w, h - 1, accent)
            paint.text_centered(region, text_top, hexc("#ffffff"), y=h // 2 - 6)
            if paint.text_width(text_bottom) <= w - 2:
                paint.text_centered(region, text_bottom, accent, y=h // 2 + 1)
        elif ctx.side == "side":
            paint.rect(region, 0, 1, w, 3, accent)
            if h >= 12 and w >= 8:
                short = text_top.split(" ")[0] if " " in text_top else text_bottom.split(" ")[0]
                text = text_top if turn and paint.text_width(text_top) <= w - 1 else short
                paint.text_centered(region, text, hexc("#ffffff"), y=h // 2 - 2)

    painter.__name__ = f"lion_label_{text_top}_{text_bottom}_{teal}_{turn}"
    return painter


def cell_cap(colour):
    ring = hexc(colour)

    def painter(region, ctx):
        h, w = region.shape[:2]
        cx, cy = w / 2, h / 2
        r = min(w, h) / 2
        yy, xx = np.mgrid[0:h, 0:w]
        d = np.hypot(xx + 0.5 - cx, yy + 0.5 - cy)
        region[..., 3] = 0
        m = d <= r
        region[m, :3] = ring
        region[m, 3] = 255
        region[d <= r * 0.8, :3] = hexc("#d9cfae")
        region[d <= r * 0.55, :3] = hexc("#b8bec6")
        region[d <= r * 0.3, :3] = hexc("#e1e5ea")

    return painter


def lion_params(key, definition, overrides=None):
    cells = int(definition["cells"])
    capacity = int(definition["capacity_mah"])
    pack = definition.get("pack_mm")
    parallel = 1 if capacity <= 5000 else 2
    total = cells * parallel
    fmt = "21700" if capacity / parallel >= 3600 else "18650"
    d, length = (21.0, 70.0) if fmt == "21700" else (18.3, 65.0)
    rows = 2 if total <= 8 else 3 if total <= 15 else 4
    cols = math.ceil(total / rows)
    across = False
    if isinstance(pack, list) and len(pack) == 3:
        plen, pw, ph = (float(v) for v in pack)
        best = None
        for r in range(1, total + 1):
            c = math.ceil(total / r)
            for along in (1, 2):
                if c % along:
                    continue
                cc = c // along
                for turned in (False, True):
                    if turned:
                        err = abs(cc * d - plen) + abs(r * d - ph) + abs(along * length - pw)
                    else:
                        err = abs(cc * d - pw) + abs(r * d - ph) + abs(along * length - plen)
                    if best is None or err < best[0] - 1e-6:
                        best = (err, r, cc, along, turned)
        _, rows, cols, along, across = best
    else:
        along = 1
    colour = palette.fit(palette.accent(definition, "#1a8f8b"), "battery")
    p = dict(cells=cells, capacity=capacity, parallel=parallel, format=fmt, d=d, length=length, rows=rows, cols=cols,
             along=along, across=across, colour=colour, connector=connector_of(definition, cells))
    if overrides:
        p.update(overrides)
    return p


def build_lion(key, p):
    d, l, rows, cols, along = p["d"], p["length"], p["rows"], p["cols"], p["along"]
    total_l = l * along
    m = kit.Model("battery", key, density=lion_density(d, max(cols * d, total_l)), gui=(30, 225, 0))
    wrap = common.material(f"cell_wrap_{key}", paint.shrink(p["colour"]), "battery", "battery", p["colour"])
    cap = common.material(f"cell_cap_{key}", cell_cap(p["colour"]))
    band = common.material(f"lion_band_{key}", lion_label(f"{p['cells']}S{p['parallel']}P" if p["parallel"] > 1 else "LI-ION",
                                                         f"{p['cells']}S {p['capacity']}", p["colour"]))
    x0 = -(cols - 1) * d / 2
    for seg in range(along):
        zc = -total_l / 2 + l * (seg + 0.5)
        for ix in range(cols):
            for iy in range(rows):
                m.cylinder_x(x0 + ix * d, d / 2 + iy * d, zc, l, d, wrap, end="cell_end", along="z", name="cell", bevel=1.2)
    half_w = cols * d / 2
    top = rows * d
    label_len = common.clamp(total_l * 0.37, 18.0, 40.0)
    m.box(-half_w - 0.4, -0.4, -label_len / 2, half_w + 0.4, top + 0.4, label_len / 2, band, name="label_wrap",
          faces={"north": None, "south": None})
    for ix in range(cols):
        for iy in range(rows):
            zc = -total_l / 2 - 0.15
            m.box(x0 + ix * d - d / 2, iy * d, zc - 0.05, x0 + ix * d + d / 2, iy * d + d, zc + 0.05, cap, name="cell_cap",
                  only=("north",))
    for zc in (-total_l / 2 + 5.0, total_l / 2 - 5.0):
        m.box(-half_w - 0.3, -0.3, zc - 3.0, half_w + 0.3, top + 0.3, zc + 3.0, "shrink_clear", name="shrink",
              faces={"north": None, "south": None})
    zr = total_l / 2
    m.box(-half_w, 2.0, zr, half_w, top - 2.0, zr + 0.8, "nickel", name="nickel_strip")
    kind = p["connector"]
    c = CONNECTORS[kind]
    y = top - max(5.0, c["wire"] + 1.0)
    lead(m, -c["wire"] * 0.8, y, zr + 0.8, zr + c["lead"], c["wire"], "wire_red")
    lead(m, c["wire"] * 0.8, y, zr + 0.8, zr + c["lead"], c["wire"], "wire_black")
    xt_connector(m, zr + c["lead"], y - c["h"] / 2, c["w"], c["h"], c["l"], body=c["body"])
    batteries.balance_lead(m, half_w - d / 2, top, zr + 0.8, p["cells"])
    return m


CAP_PX = 9.0
SHRINK_INSET = 14.0


LION_DENSITY_MAX = 3.6


def lion_density(d, longest):
    cap = (CAP_PX - 0.5) / (d / kit.UNIT_MM) + 0.02
    return max(cap, min(common.clamp(280.0 / longest, 2.0, 4.0), LION_DENSITY_MAX))


def build_lion_across(key, p):
    d, l, rows, cols, along = p["d"], p["length"], p["rows"], p["cols"], p["along"]
    total_w = l * along
    total_l = cols * d
    m = kit.Model("battery", key, density=lion_density(d, max(total_w, total_l)), gui=(30, 225, 0))
    wrap = common.material(f"cell_wrap_{key}", paint.shrink(p["colour"]), "battery", "battery", p["colour"])
    cap = common.material(f"cell_cap_{key}", cell_cap(p["colour"]))
    band = common.material(f"lion_band_{key}", lion_label(f"{p['cells']}S{p['parallel']}P" if p["parallel"] > 1 else "LI-ION",
                                                         f"{p['cells']}S {p['capacity']}", p["colour"], turn=True))
    z0 = -(cols - 1) * d / 2
    half_w = total_w / 2
    half_l = total_l / 2
    top = rows * d
    for seg in range(along):
        xc = -half_w + l * (seg + 0.5)
        for iz in range(cols):
            for iy in range(rows):
                m.cylinder_x(xc, d / 2 + iy * d, z0 + iz * d, l, d, wrap, end="cell_end", along="x", name="cell", bevel=1.2)
    label_len = common.clamp(total_w * 0.58, 18.0, 48.0)
    m.box(-label_len / 2, -0.4, -half_l - 0.4, label_len / 2, top + 0.4, half_l + 0.4, band, name="label_wrap",
          faces={"east": None, "west": None})
    for sx, face in ((-1, "west"), (1, "east")):
        xc = sx * (half_w + 0.15)
        for iz in range(cols):
            for iy in range(rows):
                m.box(xc - 0.05, iy * d, z0 + iz * d - d / 2, xc + 0.05, iy * d + d, z0 + iz * d + d / 2, cap, name="cell_cap",
                      only=(face,))
    for xc in (-half_w + SHRINK_INSET, half_w - SHRINK_INSET):
        for zs, face in ((-1, "south"), (1, "north")):
            z_a, z_b = sorted((0.0, zs * (half_l + 0.3)))
            m.box(xc - 3.0, -0.3, z_a, xc + 3.0, top + 0.3, z_b, "shrink_clear", name="shrink",
                  faces={"east": None, "west": None, face: None})
    zr = half_l + 0.3
    kind = p["connector"]
    c = CONNECTORS[kind]
    y = top - max(5.0, c["wire"] + 1.0)
    m.box(-half_w + 2.0, y - 4.0, zr, half_w - 2.0, top - 1.5, zr + 0.8, "nickel", name="nickel_strip")
    lead(m, -c["wire"] * 0.8, y, zr + 0.8, zr + c["lead"], c["wire"], "wire_red")
    lead(m, c["wire"] * 0.8, y, zr + 0.8, zr + c["lead"], c["wire"], "wire_black")
    xt_connector(m, zr + c["lead"], y - c["h"] / 2, c["w"], c["h"], c["l"], body=c["body"])
    batteries.balance_lead(m, half_w - l / 4, top, zr + 0.8, p["cells"])
    return m


def build_under(key, definition, overrides=None):
    common.setup()
    if chemistry_of(definition) == "lion":
        return None
    p = lipo_params(key, definition, overrides)
    if not p["strap"]:
        return None
    return build_lipo(key, p, under=True)


def build(key, definition, overrides=None):
    common.setup()
    if chemistry_of(definition) == "lion":
        p = lion_params(key, definition, overrides)
        return build_lion_across(key, p) if p["across"] else build_lion(key, p)
    p = lipo_params(key, definition, overrides)
    batteries.BATTERIES[key] = p
    return build_lipo(key, p)
