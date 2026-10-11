import math

import numpy as np

from . import common, palette, paths

import kit
import paint
import paintable
import props
from paint import hexc

DIAMETERS = [31.0, 76.0, 130.0, 178.0]
HUB_D = [5.1, 9.0, 13.0, 14.0]
HUB_H = [4.1, 6.0, 7.0, 8.0]
THICK = [0.85, 1.4, 2.0, 2.2]
DUCT_TIP = (0.96, 0.9)
FAMILY_PD = {"p31_4b": 1.2, "p31_3b": 1.2, "p3_3b": 0.9, "p51_3b": 0.85, "p7_2b": 0.6}
MATERIAL_BY_SUFFIX = {"pc": "polycarbonate", "gfn": "glass_fiber_nylon", "cfn": "carbon_fiber_nylon", "carbon": "carbon",
                      "bi": "bi_blend", "soft": "soft", "hard": "hard"}
FINISH = {
    "polycarbonate": dict(thick=1.0, colour=1.0, sat=1.0),
    "soft": dict(thick=0.88, colour=1.0, sat=0.95),
    "hard": dict(thick=1.12, colour=0.9, sat=1.12),
    "bi_blend": dict(thick=1.0, colour=1.0, sat=1.0),
    "glass_fiber_nylon": dict(thick=1.05, colour=1.0, sat=0.7),
    "carbon_fiber_nylon": dict(thick=1.05, colour=1.0, sat=0.6),
    "carbon": dict(thick=0.9, colour=1.0, sat=0.5),
}


def material_of(key, definition):
    tags = key.split("_")[3:]
    for tag in tags:
        if tag in MATERIAL_BY_SUFFIX:
            return MATERIAL_BY_SUFFIX[tag]
    value = str(definition.get("material", "polycarbonate")).split(":")[-1]
    return value if value in FINISH else "polycarbonate"


def family(diameter, blades):
    if diameter <= 45.0:
        return "p31_4b" if blades >= 4 else "p31_3b"
    if diameter < 100.0:
        return "p3_3b"
    if diameter < 160.0:
        return "p51_3b"
    return "p7_2b"


def params(key, definition, overrides=None):
    diameter = float(definition["diameter_mm"])
    blades = int(definition["blades"])
    pitch_in = float(definition.get("pitch_in", diameter / 25.4 * 0.8))
    material = material_of(key, definition)
    ducted = "duct" in key.split("_")
    fam = family(diameter, blades)
    template = props.PROPS[fam]
    radius = diameter / 2.0
    ratio = radius / template["radius"]
    chord_k = 1.0 if ducted else (template["blades"] / max(2, blades)) ** 0.3
    pd = pitch_in * 25.4 / diameter
    steep = common.clamp(pd / FAMILY_PD[fam], 0.7, 1.45)
    segments = []
    widest = max(c for _, _, c, _ in template["segments"]) * ratio * chord_k
    for i, (r0, r1, chord, angle) in enumerate(template["segments"]):
        c = chord * ratio * chord_k
        if ducted and i >= 2:
            c = max(c, widest * DUCT_TIP[i - 2])
        a = math.degrees(math.atan(math.tan(math.radians(angle)) * steep))
        segments.append((round(r0 * ratio, 3), round(r1 * ratio, 3), round(c, 3), round(a, 2)))
    finish = FINISH[material]
    colour = palette.accent(definition, palette.PROP_MATERIAL_COLOURS.get(material, "#e4e4ea"))
    colour = palette.saturate(colour, finish["sat"], finish["colour"])
    hub_d = common.interp(diameter, DIAMETERS, HUB_D, extrapolate="slope")
    hub_h = common.clamp(common.interp(diameter, DIAMETERS, HUB_H, extrapolate="slope"), 3.6, 10.0)
    thick = common.clamp(common.interp(diameter, DIAMETERS, THICK, extrapolate="slope"), 0.7, 3.0) * finish["thick"]
    nut = "nylock" if diameter >= 100.0 else "tmount" if diameter >= 60.0 else None
    p = dict(radius=radius, blades=blades, hub_d=round(hub_d, 2), hub_h=round(hub_h, 2), thick=round(thick, 3), color=colour,
             nut=nut, segments=segments, sweep=list(template["sweep"]), material=material, ducted=ducted,
             nut_d=8.6 if diameter < 250.0 else 10.4, tip=None)
    if material == "bi_blend":
        p["tip"] = palette.mix(colour, "#f4f1ea", 0.25)
    if overrides:
        p.update(overrides)
    return p


def carbon_blade(colour):
    base = hexc(colour)

    def painter(region, ctx):
        h, w = region.shape[:2]
        if ctx.side == "side":
            paint.fill(region, paint.shade(base, 0.8))
            if h >= 2:
                region[0, :, :3] = paint.shade(base, 1.5)
            return
        yy, xx = np.mgrid[0:h, 0:w]
        ramp = 0.88 + 0.24 * xx / max(1, w - 1)
        weave = ((xx // 2 + yy // 2) % 2) == 0
        sheen = weave & (((xx + yy) % 7) == 0)
        k = ramp * np.where(sheen, 1.3, np.where(weave, 1.13, 1.0))
        region[..., :3] = np.clip(base[None, None, :] * k[..., None], 0, 255)
        region[..., 3] = 255
        paint.noise(region, ctx, 0.02)
        if h >= 3:
            region[0, :, :3] = paint.shade(base, 1.6)
            region[-1, :, :3] = paint.shade(base, 0.7)
        if ctx.side == "bottom":
            region[..., :3] = paint.shade(region[..., :3], 0.85)

    return painter


def blade_materials(key, p):
    if p["material"] == "carbon":
        blade = common.material(f"prop_{key}", carbon_blade(p["color"]), "prop", "prop", p["color"])
    else:
        blade = common.material(f"prop_{key}", paint.prop_material(p["color"]), "prop", "prop", p["color"])
    hub = common.material(f"prop_hub_{key}", paint.plastic(p["color"]), "prop", "prop", p["color"])
    tip = None
    if p.get("tip"):
        tip = common.material(f"prop_tip_{key}", paint.prop_material(p["tip"]), "prop", "prop", p["tip"])
    return blade, hub, tip


def build_hub(m, p, hub_mat):
    hd, hh = p["hub_d"], p["hub_h"]
    sheet = m.sheet("hub_top", -hd / 2, -hd / 2, hd / 2, hd / 2, props.hub_top(p["color"]))
    m.cylinder(0, 0, 0.0, hh, hd, hub_mat, name="hub", sheet_up=sheet, bevel=max(0.4, hd * 0.06))
    if p["nut"] == "nylock":
        nd = p.get("nut_d", 8.6)
        k = nd / 8.6
        nut_sheet = m.sheet("nut_top", -nd / 2, -nd / 2, nd / 2, nd / 2, props.nut_top)
        m.cylinder(0, 0, hh, hh + 3.2 * k, nd, "steel", name="nut", bevel=0.4, no_bottom=True)
        m.cylinder(0, 0, hh + 3.2 * k, hh + 5.6 * k, nd - 0.6 * k, "alu_nut", name="nut_cap", sheet_up=nut_sheet, bevel=0.5,
                   no_bottom=True)
    elif p["nut"] == "tmount":
        for s in (-1, 1):
            m.box(s * 2.6 - 0.9, hh, -0.9, s * 2.6 + 0.9, hh + 0.8, 0.9, "dark_steel", name="screw")


def build_prop(key, ccw=False, damage=None):
    p = props.PROPS[key]
    suffix = ("_" + damage if damage else "") + ("_ccw" if ccw else "")
    density = 16.0 if p["radius"] < 20 else 6.0 if p["radius"] < 40 else 3.6
    m = kit.Model("prop", key + suffix, density=density, gui=(58, 225, 0), gui_fill=15.5)
    blade_mat, hub_mat, tip_mat = blade_materials(key, p)
    build_hub(m, p, hub_mat)
    y_mid = p["hub_h"] * 0.5
    spin = -1 if ccw else 1
    t = p["thick"]
    hub = p["hub_d"] * 0.5
    tip_from = p["radius"] * 0.62
    for b in range(p["blades"]):
        base_yaw = 360.0 * b / p["blades"] + 15.0
        spec = dict(props.blade_damage(damage, b))
        if "bend_at" in spec:
            spec["bend_radius"] = hub + (p["radius"] - hub) * spec["bend_at"]
        pieces, marks = props.blade_pieces(p, spec, spin)
        for pc in pieces:
            mat = tip_mat if tip_mat and pc["r0"] >= tip_from - 1e-6 else blade_mat
            if tip_mat and pc["r0"] < tip_from < pc["r1"]:
                props.emit_piece(m, dict(pc, r1=tip_from), blade_mat, "blade", spin, base_yaw + spin * pc["sweep"], spec, y_mid, t)
                props.emit_piece(m, dict(pc, r0=tip_from), tip_mat, "blade", spin, base_yaw + spin * pc["sweep"], spec, y_mid, t)
                continue
            props.emit_piece(m, pc, mat, "blade", spin, base_yaw + spin * pc["sweep"], spec, y_mid, t)
        for mk in marks:
            props.emit_piece(m, mk, "prop_break", "break", spin, base_yaw + spin * mk["sweep"], spec, y_mid, t, pad=0.02)
    return m


def build(key, definition, overrides=None):
    common.setup()
    p = params(key, definition, overrides)
    props.PROPS[key] = p
    paintable.MATERIALS[("prop", f"prop_{key}")] = ("prop", p["color"])
    paintable.MATERIALS[("prop", f"prop_hub_{key}")] = ("prop", p["color"])
    cw = build_prop(key)
    ccw = build_prop(key, ccw=True)
    blur = props.build_blur(key, cw)
    damaged = [(build_prop(key, damage=d), build_prop(key, ccw=True, damage=d)) for d in props.DAMAGE]
    return cw, ccw, blur, damaged
