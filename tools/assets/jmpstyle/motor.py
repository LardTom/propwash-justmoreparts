from . import common, palette, paths

import layout
import motors
import paintable

STATOR_REF = [8.0, 14.0, 20.0, 22.0, 23.0, 28.0]
BASE_D = [9.0, 15.0, 17.0, 21.0, 21.0, 26.0]
BASE_H = [1.2, 2.2, 2.4, 3.2, 3.0, 3.4]
LUG_REACH = [6.5, 9.5, 11.0, 13.8, 13.8, 17.0]
LUG_W = [2.4, 3.4, 3.8, 5.0, 5.0, 5.5]
RING_D = [8.6, 14.0, 18.0, 23.0, 23.0, 28.0]
GAP = [0.8, 1.2, 1.4, 2.0, 1.8, 2.2]
BELL_EXTRA_D = [2.5, 4.0, 2.5, 5.9, 5.6, 6.0]
SPOKES_REF = [(8.0, 4), (14.0, 5), (20.0, 6), (22.0, 5), (23.0, 6), (28.0, 5)]
BELL_EXTRA_H_REF = [8.0, 14.0, 20.0, 22.0, 28.0]
BELL_EXTRA_H = [3.0, 5.1, 5.7, 5.8, 6.9]

MOUNT_BOLT_RADIUS = {
    "propwash:m08": 0.0,
    "propwash:m14": 6.36,
    "propwash:m22": 11.31,
    "propwash:m28": 13.43,
    "justmoreparts:m31": 17.68,
    "justmoreparts:m42": 21.21,
}


def stator_of(definition):
    size = definition.get("stator_mm")
    if isinstance(size, list) and len(size) == 2:
        return float(size[0]), float(size[1])
    label = str(definition.get("stator", "2207"))
    diameter = float(label[:2])
    rest = label[2:]
    if "." not in rest and len(rest) > 2:
        rest = rest[:2] + "." + rest[2:]
    return diameter, float(rest)


def bell_extra_h(stator_d):
    return common.clamp(common.interp(stator_d, BELL_EXTRA_H_REF, BELL_EXTRA_H, extrapolate="slope"), 3.0, 9.0)


def spokes_for(stator_d):
    if stator_d >= 30.0:
        return 6
    return min(SPOKES_REF, key=lambda r: (abs(r[0] - stator_d), -r[0]))[1]


def shaft_for(stator_d):
    if stator_d >= 40.0:
        return 6.0, common.clamp(0.33 * stator_d, 10.0, 15.0)
    if stator_d >= 22.0:
        return 5.0, common.interp(stator_d, [22.0, 28.0], [9.5, 10.5], extrapolate="slope")
    if stator_d >= 11.0:
        return 1.5, common.interp(stator_d, [11.0, 14.0, 20.0], [2.2, 2.5, 2.8], extrapolate="hold")
    return 1.0, common.interp(stator_d, [7.0, 8.0], [1.4, 1.6], extrapolate="hold")


def params(definition, overrides=None):
    stator_d, stator_h = stator_of(definition)
    bell_colour = palette.fit(palette.accent(definition, "#b9bec7"), "motor")
    base_h = common.interp(stator_d, STATOR_REF, BASE_H)
    gap = common.interp(stator_d, STATOR_REF, GAP)
    base_d = common.interp(stator_d, STATOR_REF, BASE_D)
    bell_d = stator_d + common.interp(stator_d, STATOR_REF, BELL_EXTRA_D, extrapolate="scale")
    lug = None
    if stator_d >= 11.0:
        reach = common.interp(stator_d, STATOR_REF, LUG_REACH)
        width = common.interp(stator_d, STATOR_REF, LUG_W, extrapolate="slope")
        bolt = MOUNT_BOLT_RADIUS.get(definition.get("mount", ""))
        if bolt:
            reach = max(reach, bolt + width * 0.55)
        lug = (round(reach, 2), round(width, 2))
    shaft_d, shaft_len = shaft_for(stator_d)
    large = stator_d >= 20.0
    p = dict(base_d=round(base_d, 2), base_h=round(base_h, 2), base_color=palette.MOTOR_BASE_LARGE if stator_d >= 22.0 else palette.MOTOR_BASE,
             lug=lug, stator_d=round(common.interp(stator_d, STATOR_REF, RING_D), 2), gap=round(gap, 2),
             bell_d=round(bell_d, 2), bell_color=bell_colour, shaft_d=shaft_d, shaft_len=round(shaft_len, 2),
             spokes=spokes_for(stator_d), lug_w=lug[1] if lug else 0, vent="#c8733a" if stator_d > 10.0 else "#9a5a2a")
    if large:
        p["top_color"] = palette.MOTOR_CAP_DARK
    p["seat"] = round(base_h + gap + stator_h + bell_extra_h(stator_d), 2)
    p["stator_real"] = (stator_d, stator_h)
    if overrides:
        p.update(overrides)
    return p


def build(key, definition, overrides=None):
    common.setup()
    p = params(definition, overrides)
    seat = p.pop("seat")
    p.pop("stator_real")
    motors.MOTORS[key] = p
    layout.MOTOR_SEAT_MM[key] = seat
    paintable.MATERIALS[("motor", f"motor_bell_{key}")] = ("motor", p["bell_color"])
    paintable.MATERIALS[("motor", f"motor_bell_plain_{key}")] = ("motor", p["bell_color"])
    model = motors.build_motor(key)
    if p["bell_d"] >= 40.0:
        model.density = 3.0
    return model, seat
