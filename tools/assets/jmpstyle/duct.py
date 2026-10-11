import json

from . import paths

SEGMENTS = 24
LIP_OVERLAP = 0.05
TYPICAL_MOTOR = {"whoop_65_lite": "m0802_19000", "whoop_75_duct": "m0802_19000", "whoop_85_duct": "m1102_13500",
                 "cw_25_duct": "m1404_3800", "cw_35_duct": "m2004_1700"}
SEAT_BY_MOUNT = {"m08": 7.0, "m14": 12.5, "m22": 18.0, "m28": 19.5, "m31": 28.65, "m42": 31.4}


def clearance(prop_mm):
    return 1.1 if prop_mm <= 50 else 1.5


def radius(prop_mm):
    return round(prop_mm / 2 + clearance(prop_mm), 2)


def wall(prop_mm):
    return round(min(3.0, max(0.8, 0.35 + 0.016 * prop_mm)), 2)


def lip(prop_mm):
    return round(max(0.45, 0.55 * wall(prop_mm)), 2), round(max(0.8, 0.02 * prop_mm), 2)


def blade_top(prop_mm):
    return 1.75 + 0.024 * prop_mm


def margin(prop_mm):
    return 1.0 if prop_mm <= 50 else 1.2


def top(hub_mm, prop_mm):
    return round(hub_mm + blade_top(prop_mm) + margin(prop_mm), 2)


def motor_height(d):
    arms = d.get("arms")
    if isinstance(arms, dict) and arms:
        ys = [float(v["pos"][1]) for v in arms.values() if isinstance(v, dict) and "pos" in v]
        if ys:
            return sum(ys) / len(ys)
    wb = float(d["wheelbase_mm"])
    return 3.0 if wb >= 200 else 2.5 if wb >= 100 else 1.5


def seat(key, mount):
    motor = TYPICAL_MOTOR.get(key)
    if motor:
        layout = paths.ASSETS_ROOT / paths.NS / "drone_layout.json"
        if layout.exists():
            seats = json.loads(layout.read_text(encoding="utf-8")).get("motor_seat", {})
            if motor in seats:
                return float(seats[motor])
    return SEAT_BY_MOUNT.get(mount, 12.5)


def ring(m, cx, cz, y0, y1, prop_mm, material, lip_material=None, lip_name="duct_lip", with_lip=True, wall_factor=1.0):
    r = radius(prop_mm)
    w = round(max(0.8, wall(prop_mm) * wall_factor), 2)
    m.ring(cx, cz, y0, y1, r, w, SEGMENTS, material, name="duct")
    if with_lip:
        lw, lh = lip(prop_mm)
        m.ring(cx, cz, y1 - lh, y1, r + w - LIP_OVERLAP, lw, SEGMENTS, lip_material or material, name=lip_name)
    return r, w
