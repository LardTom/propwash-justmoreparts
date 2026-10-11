import math

from . import common, duct, palette, paths

import frames
import kit
import paint

PAD_BY_MOUNT = {"m08": 11.5, "m14": 16.0, "m22": 25.0, "m28": 30.0, "m31": 36.0, "m42": 46.0}
BODY_HALF_BY_STACK = {"whoop_aio": 14.0, "s20": 17.0, "s30": 21.0}
DENSITY_WB = [65.0, 142.0, 225.0, 295.0, 820.0]
DENSITY = [9.0, 4.2, 3.6, 3.0, 1.4]
ROOT_K = {"freestyle": 0.071, "race": 0.0465, "long_range": 0.054, "cinewhoop": 0.099, "toothpick": 0.05, "x_class": 0.036}
WHOOP_LENS = (9.7, -17.8)


def motors_of(d):
    arms = d.get("arms")
    order = ("front_left", "front_right", "rear_left", "rear_right")
    if isinstance(arms, dict) and all(k in arms for k in order):
        return [[float(v) for v in arms[k]["pos"]] for k in order]
    s = float(d["wheelbase_mm"]) / (2.0 * math.sqrt(2.0))
    return [[-s, 0.0, -s], [s, 0.0, -s], [-s, 0.0, s], [s, 0.0, s]]


def body_span(wb):
    return wb if wb <= 300 else 300 + 0.3 * (wb - 300)


def max_prop(d):
    if "props_mm" in d:
        return float(d["props_mm"][1])
    return float(d.get("max_prop_mm", 0.55 * float(d["wheelbase_mm"])))


def mount_key(value):
    return str(value).split(":")[-1]


def params(key, d, overrides=None):
    wb = float(d["wheelbase_mm"])
    role = d.get("role", "freestyle")
    shape = d.get("shape", "true_x")
    span = body_span(wb)
    prop = max_prop(d)
    ducted = "duct" in d
    accent = palette.fit(palette.accent(d, "#d6333a"), "tpu")
    stacks = [mount_key(s) for s in d.get("stack_mounts", ["s30"])]
    stack = "s30" if "s30" in stacks else "s20" if "s20" in stacks else "whoop_aio"
    motor_mount = mount_key((d.get("motor_mounts") or ["m22"])[-1])
    battery = d.get("battery") or {}
    camera = d.get("camera") or {}
    cam_z = float(camera["pos"][2]) if "pos" in camera else -0.21 * wb
    cam_y = float(camera["pos"][1]) if "pos" in camera else None
    cam_tilt = float(camera.get("tilt_deg", 0.0))
    plate = float(battery.get("plate_mm", 0.13 * wb))
    root_k = ROOT_K.get(role, 0.06)
    root = common.clamp(root_k * wb, 4.0, 36.0)
    if "bash" in key:
        root *= 1.12
    thick = {"freestyle": 6.0, "race": 5.0, "cinewhoop": 3.0, "toothpick": 2.5 if wb <= 120 else 3.0,
             "long_range": 3.0 if wb <= 200 else 4.5 if wb <= 280 else 5.0, "x_class": 5.0 if wb <= 500 else 6.0}.get(role, 5.0)
    if role == "freestyle" and shape in ("deadcat", "squashed_x"):
        thick = 5.5
    if "bash" in key:
        thick = 6.5
    half = BODY_HALF_BY_STACK[stack]
    if role == "race":
        half = 16.0
    if role == "x_class":
        half = common.clamp(0.065 * span, 24.0, 34.0)
    if role == "toothpick":
        half = 12.5 if wb <= 130 else 14.0
    if role == "cinewhoop":
        half = common.clamp(0.13 * wb, 15.0, 22.0)
    p = dict(wb=wb, role=role, shape=shape, span=span, prop=prop, ducted=ducted, accent=accent, stack=stack,
             motor_y=duct.motor_height(d), seat=duct.seat(key, motor_mount),
             motors=motors_of(d), root=round(root, 2), tip=round(root * 0.78, 2), thick=thick, split=0.5,
             pad=PAD_BY_MOUNT.get(motor_mount, common.clamp(0.11 * wb, 12.0, 60.0)), half=half, plate=plate,
             top_plate=plate if bool(battery.get("on_top", wb >= 100)) else round(0.1 * wb),
             on_top=bool(battery.get("on_top", wb >= 100)), cam_z=cam_z, cam_y=cam_y, cam_tilt=cam_tilt, camera_set="pos" in camera,
             cam_inner=7.6 if role in ("toothpick", "whoop") else 10.8 if role in ("freestyle", "long_range", "x_class") else 10.6,
             density=round(common.interp(wb, DENSITY_WB, DENSITY, extrapolate="hold"), 2),
             gps=role in ("long_range", "x_class") and wb >= 250, text=plate_text(role, prop, key))
    if overrides:
        p.update(overrides)
    if p.get("icon"):
        p["root"] = round(max(p["root"], 0.07 * wb), 2)
        p["tip"] = round(p["root"] * 0.78, 2)
        p["pad"] = round(max(p["pad"], 0.08 * wb), 2)
        p["thick"] = round(max(p["thick"], 0.012 * wb), 2)
    return p


def inch_class(prop):
    halves = int(round(prop / 25.4 * 2.0))
    return str(halves // 2) if halves % 2 == 0 else f"{halves // 2}.5"


def plate_text(role, prop, key):
    inch = inch_class(prop)
    if role == "race":
        return "RACE"
    if role == "x_class":
        return inch
    if role == "toothpick":
        return f"TP{inch}"
    return f"{inch}IN" if "." not in inch else inch


def materials(key, p):
    guard = common.material(f"tpu_{key}", paint.plastic(p["accent"], 0.05), "frame", "tpu", p["accent"])
    alu = common.material(f"alu_{key}", paint.alu(p["accent"]), "frame", "tpu", p["accent"])
    return guard, alu


def arms(m, p, guard, top=-0.3, material="carbon", guards=True, pads=True):
    y0 = top - p["thick"]
    for i, (cx, _, cz) in enumerate(p["motors"]):
        reach = math.hypot(cx, cz)
        yaw = round(math.degrees(math.atan2(-cz, cx)), 3)
        split = reach * p["split"]
        start = p["root"] / 2 + 0.2
        m.bar(0, 0, yaw, split - start, p["root"], y0, top, material, start=start, name="arm", skip=("west",))
        m.bar(0, 0, yaw, reach - split, p["tip"], y0, top, material, start=split, name="arm", skip=("west",))
        if guards:
            length = p["pad"] * 0.8
            m.bar(0, 0, yaw, length, p["tip"] + 1.2, y0 - 0.4, top + 0.4, guard, start=reach - p["pad"] / 2 - length,
                  name="arm_guard")
        if pads:
            pad = p["pad"]
            sheet = m.sheet(f"pad{i}", cx - pad / 2, cz - pad / 2, cx + pad / 2, cz + pad / 2, frames.pad_top(p["accent"]))
            m.cylinder(cx, cz, y0 - 0.3, top + 0.3, pad, "carbon", name="motor_pad", sheet_up=sheet)


SEGMENT_PX = 40.0
PLATE_PX = 64.0


def pieces(length, density):
    return max(1, math.ceil(length / kit.UNIT_MM * density / SEGMENT_PX))


def joint_skip(i, n, first, last):
    return tuple(d for d, inner in ((first, i > 0), (last, i < n - 1)) if inner)


def rails(m, p, guard, top=-0.3):
    y0 = top - p["thick"]
    motors = p["motors"]
    for left in (True, False):
        front = motors[0 if left else 1]
        rear = motors[2 if left else 3]
        dx, dz = rear[0] - front[0], rear[2] - front[2]
        length = math.hypot(dx, dz)
        yaw = round(math.degrees(math.atan2(-dz, dx)), 3)
        n = pieces(length, p["density"])
        seg = length / n
        if abs(dx) < 1e-6:
            z_start = min(front[2], rear[2])
            for i in range(n):
                m.box(front[0] - p["root"] / 2, y0, z_start + i * seg, front[0] + p["root"] / 2, top, z_start + (i + 1) * seg,
                      "carbon", name="rail", skip=joint_skip(i, n, "north", "south"))
        else:
            mid_x, mid_z = (front[0] + rear[0]) / 2, (front[2] + rear[2]) / 2
            for i in range(n):
                m.bar(mid_x, mid_z, yaw, seg, p["root"], y0, top, "carbon", start=-length / 2 + i * seg, name="rail",
                      skip=joint_skip(i, n, "west", "east"))
        for end, other in ((front, rear), (rear, front)):
            ex, ez = other[0] - end[0], other[2] - end[2]
            m.bar(end[0], end[2], round(math.degrees(math.atan2(-ez, ex)), 3), p["pad"] * 0.8, p["tip"] + 1.2, y0 - 0.4, top + 0.4,
                  guard, start=p["pad"] / 2, name="arm_guard")
    rail_x = abs(motors[1][0] + motors[3][0]) / 2
    n = pieces(2.0 * rail_x, p["density"])
    seg = 2.0 * rail_x / n
    for zc in (-0.12 * p["span"], 0.14 * p["span"]):
        for i in range(n):
            m.box(-rail_x + i * seg, y0, zc - p["root"] * 0.45, -rail_x + (i + 1) * seg, top, zc + p["root"] * 0.45, "carbon",
                  name="cross_brace", skip=joint_skip(i, n, "west", "east"))
    for i, (cx, _, cz) in enumerate(motors):
        pad = p["pad"]
        sheet = m.sheet(f"pad{i}", cx - pad / 2, cz - pad / 2, cx + pad / 2, cz + pad / 2, frames.pad_top(p["accent"]))
        m.cylinder(cx, cz, y0 - 0.3, top + 0.3, pad, "carbon", name="motor_pad", sheet_up=sheet)


def body(m, p, guard, alu):
    span = p["span"]
    half = p["half"]
    x_class = p["role"] == "x_class"
    race = p["role"] == "race"
    z_front = min(-0.27 * span, p["cam_z"] - 16.0)
    z_rear = 0.25 * span
    bottom = 4.0 if x_class else 2.0 if race else 2.5 if p["role"] == "long_range" else 3.0
    hole = 15.25 if p["stack"] == "s30" else 10.0 if p["stack"] == "s20" else 10.2
    z_main = z_front
    if (z_rear - z_front) / kit.UNIT_MM * p["density"] > PLATE_PX:
        z_main = max(z_front, min(-0.27 * span, z_rear - PLATE_PX * 0.9 * kit.UNIT_MM / p["density"]))
        frames.carbon_plate(m, -half, z_front, half, z_main, -bottom, 0.0, "bottom_plate_nose")
    under_battery = not p["on_top"] and p["wb"] >= 100
    strap_slots = []
    if under_battery:
        sz0, sz1 = -0.17 * span, 0.15 * span
        strap_slots = [(-half + 3.0, sz0, -half + 7.0, sz1), (half - 7.0, sz0, half - 3.0, sz1)]
    holes = [(sx * hole, sz * hole, 1.6 if hole > 12 else 1.2) for sx in (-1, 1) for sz in (-1, 1)]
    slots = [(-half * 0.48, z_rear * 0.52, half * 0.48, z_rear * 0.62)] + strap_slots
    sheet = m.sheet("bottom_plate", -half, z_main, half, z_rear, frames.plate_sheet(-half, z_main, half, z_rear, holes, slots))
    m.box(-half, -bottom, z_main, half, 0.0, z_rear, "carbon", name="bottom_plate", sheet_up=sheet,
          sheet_down=sheet if under_battery else None)
    if under_battery:
        pad_x = half - 8.0
        m.box(-pad_x, p["plate"], -0.13 * span, pad_x, -bottom, 0.15 * span, "rubber", name="battery_pad", skip=("up",))
    elif not p["on_top"]:
        return
    plate = p["top_plate"]
    top_t = 3.0 if x_class else 2.0 if race or p["wb"] < 170 else 2.5
    under = plate - top_t
    so_d = 6.0 if x_class else 4.5 if race else 4.0 if p["wb"] < 170 else 5.0
    zs = (-0.13, 0.18) if race or p["role"] == "cinewhoop" else (-0.16, 0.02, 0.2)
    so_x = half - (2.5 if race else 4.0)
    for sx in (-1, 1):
        for sz in zs:
            frames.standoff(m, sx * so_x, sz * span, 0.0, under, so_d, alu)
    tz0 = max(z_front + 8.0, -0.17 * span)
    tz1 = 0.22 * span
    tx = half - 1.0
    freestyle = p["role"] == "freestyle"
    frames.carbon_plate(m, -tx, tz0, tx, tz1, under, plate, "top_plate",
                        slots=[] if under_battery else [(-tx + 3.0, tz0 + 0.2 * (tz1 - tz0), -tx + 7.0, tz1 - 0.25 * (tz1 - tz0)),
                                                        (tx - 7.0, tz0 + 0.2 * (tz1 - tz0), tx - 3.0, tz1 - 0.25 * (tz1 - tz0))],
                        holes=[(0.0, tz0 + 8.0, 2.0), (0.0, tz1 - 8.0, 2.0)] if freestyle or under_battery else (),
                        text=p["text"])
    if not race:
        m.box(-half * 0.55, plate, tz0 + 0.25 * (tz1 - tz0), half * 0.55, plate + 0.8, tz1 - 0.2 * (tz1 - tz0), "rubber",
              name="battery_pad", skip=("down",))
    cz = p["cam_z"]
    frames.camera_cage(m, p["cam_inner"], 2.0 if race or p["wb"] < 170 else 2.5, 0.0, under, cz - 15.0, cz + 14.0)
    if p["gps"]:
        gz = tz1 - 8.0
        mast = 2.0 if not x_class else 3.0
        m.box(-mast, plate, gz - mast, mast, plate + 25.0, gz + mast, "dark_steel", name="gps_mast")
        m.box(-11.0, plate + 25.0, gz - 11.0, 11.0, plate + 31.0, gz + 11.0, "dark_grey", name="gps_puck",
              decals={"up": frames.gps_decal})
    if race:
        m.box(-9.0, 0.0, z_rear - 6.0, 9.0, 7.0, z_rear, "tpu_black", name="rear_mount")
        return
    rear_w = 13.0 if not x_class else 18.0
    m.box(-rear_w, 0.0, z_rear - 8.0, rear_w, 10.0, z_rear, guard, name="rear_mount")
    if freestyle:
        m.box(-rear_w + 4.0, 10.0, z_rear - 6.0, rear_w - 4.0, 14.0, z_rear, guard, name="rear_mount_top")


def build_x(key, p):
    gui = (52, 200, 0)
    m = kit.Model("frame", key, density=p["density"], gui=gui)
    guard, alu = materials(key, p)
    if p["shape"] == "h_frame":
        rails(m, p, guard)
    else:
        arms(m, p, guard)
    body(m, p, guard, alu)
    return m


def build_cinewhoop(key, p):
    m = kit.Model("frame", key, density=p["density"], gui=(45, 205, 0))
    guard, alu = materials(key, p)
    p = dict(p, split=0.45)
    my = p["motor_y"]
    arms(m, p, guard, top=my - 0.3, guards=False)
    prop = p["prop"]
    bottom = my - 0.3
    top = duct.top(my + p["seat"], prop)
    duct_r = duct.radius(prop)
    for cx, _, cz in p["motors"]:
        duct.ring(m, cx, cz, bottom, top, prop, "tpu_black", lip_material=guard, lip_name="duct_rim")
        yaw_out = math.degrees(math.atan2(-cz, cx))
        for off in (90, 270):
            m.bar(cx, cz, yaw_out + off, duct_r - 8.0 + 0.4, 4.0, my - 3.1, bottom, "carbon", start=8.0, name="duct_brace")
    half = p["half"]
    wb = p["wb"]
    zf, zr = -0.21 * wb, 0.24 * wb
    frames.carbon_plate(m, -half, zf, half, zr, -3.0, 0.0, "bottom_plate",
                        holes=[(sx * 10.0, sz * 10.0, 1.2) for sx in (-1, 1) for sz in (-1, 1)],
                        slots=[(-half * 0.6, zr * 0.65, half * 0.6, zr * 0.76)])
    plate = p["plate"]
    under = plate - 2.0
    for sx in (-1, 1):
        for sz in (-0.13, 0.18):
            frames.standoff(m, sx * (half - 5.0), sz * wb, 0.0, under, 4.0, alu)
    tx = half - 2.0
    tz0, tz1 = -0.14 * wb, 0.225 * wb
    frames.carbon_plate(m, -tx, tz0, tx, tz1, under, plate, "top_plate",
                        slots=[(-tx + 4.0, tz0 + 12.0, -tx + 8.0, tz1 - 16.0), (tx - 8.0, tz0 + 12.0, tx - 4.0, tz1 - 16.0)],
                        text=p["text"])
    cy, cz = p["cam_y"], p["cam_z"]
    inner = p["cam_inner"]
    floor = round(top + 0.4, 2)
    frames.camera_cage(m, inner, 2.0, floor, cy + 6.0, cz - 2.0, cz + 18.0)
    m.box(-inner - 2.0, floor, cz - 2.0, inner + 2.0, floor + 1.2, cz + 18.0, "carbon", name="cam_floor")
    m.box(-3.0, plate, cz + 2.0, 3.0, floor, cz + 14.0, guard, name="cam_riser")
    m.box(-3.0, under, cz + 2.0, 3.0, plate, tz0 + 3.0, "carbon", name="cam_tongue")
    return m


def build_whoop(key, p):
    m = kit.Model("frame", key, density=p["density"], gui=(45, 205, 0))
    guard, _ = materials(key, p)
    frame_mat = "black_matte"
    wb = p["wb"]
    h = wb / 2.0 / math.sqrt(2.0)
    prop = p["prop"]
    my = p["motor_y"]
    lite = "lite" in key
    bottom = round(my - 1.9, 2)
    top = duct.top(my + p["seat"], prop)
    duct_r = duct.radius(prop)
    cup = 11.5 if p["pad"] >= 11.0 else 10.5
    for sx in (-1, 1):
        for sz in (-1, 1):
            cx, cz = sx * h, sz * h
            duct.ring(m, cx, cz, bottom, top, prop, frame_mat, with_lip=not lite, wall_factor=0.9 if lite else 1.0)
            m.cylinder(cx, cz, bottom, my, cup, frame_mat, name="motor_cup")
            yaw_out = math.degrees(math.atan2(-sz, sx))
            for off in (90, 180, 270):
                m.bar(cx, cz, yaw_out + off, duct_r - 5.0, 1.4 if lite else 1.6, bottom, bottom + 1.2, frame_mat, start=5.4, name="spoke")
            yaw_in = math.degrees(math.atan2(sz, -sx))
            m.bar(cx, cz, yaw_in, math.hypot(h, h) - 12.0 - 5.5, 4.4 if lite else 5.0, my - 1.7, my + 0.5, frame_mat, start=5.5, name="arm")
    bridge = 6.5 * min(1.0, wb / 65.0) + (wb - 65.0) * 0.08
    for sz in (-1, 1):
        m.box(-bridge, bottom, sz * (h + 0.5) - 1.6, bridge, my + 4.5, sz * (h + 0.5) + 1.6, frame_mat, name="bridge")
    for sx in (-1, 1):
        m.box(sx * (h + 0.5) - 1.6, bottom, -bridge, sx * (h + 0.5) + 1.6, my + 4.5, bridge, frame_mat, name="bridge")
    sheet = m.sheet("floor", -12.5, -12.5, 12.5, 12.5, frames.floor_sheet)
    m.box(-12.5, -1.6, -12.5, 12.5, 0.0, 12.5, frame_mat, name="floor", sheet_up=sheet)
    for sx in (-1, 1):
        m.box(sx * 7.6, -4.2, -9.0, sx * 6.4, -1.6, 9.0, frame_mat, name="battery_rail")
        m.box(sx * 7.6, -4.9, -9.0, sx * 5.4, -4.2, 9.0, frame_mat, name="battery_clip")
    dy = dz = lift = 0.0
    if p.get("cam_y") is not None:
        dy = round(p["cam_y"] - WHOOP_LENS[0], 2)
        dz = round(p["cam_z"] - WHOOP_LENS[1], 2)
        tilt = math.radians(p.get("cam_tilt", 0.0))
        lift = 0.0 if lite else max(dy, round(p["cam_y"] + 7.0 * math.cos(tilt) - 6.0 * math.sin(tilt) + 0.2 - 14.0, 2))
    if not lite:
        for sx in (-1, 1):
            m.box(sx * 12.1, 0.0, -7.5, sx * 13.3, 14.0 + lift, 8.0, guard, name="canopy_side")
        m.box(-13.3, 0.0, 8.0, 13.3, 12.6 + lift, 9.2, guard, name="canopy_back")
        m.box(-13.3, 14.0 + lift, -2.0 + dz, 13.3, 15.2 + lift, 9.2, guard, name="canopy_top")
        m.box(-13.3, 12.6 + lift, 9.2, 13.3, 14.0 + lift, 7.4, guard, name="canopy_back_top", skip=("down",))
        m.box(-12.1, 12.8 + lift, -2.0 + dz, 12.1, 14.0 + lift, -1.0 + dz, guard, name="canopy_brow")
    low = 1.0 if dz >= 0.0 else round(top + 0.3, 2)
    for sx in (-1, 1):
        m.box(sx * 7.7, low, -13.0 + dz, sx * 9.0, 13.0 + dy, -2.0 + dz, guard, name="cam_cheek")
        m.box(sx * 9.0, low, -9.0 + dz, sx * 12.1, low + 4.0, max(-3.0 + dz, -7.0), guard, name="cam_cheek_base")
    m.box(-7.7, 7.0 + dy, -3.0 + dz, 7.7, 8.2 + dy, -2.0 + dz, "dark_grey", name="cam_axle_left", skip=("up", "down"))
    return m


def build_toothpick(key, p):
    m = kit.Model("frame", key, density=p["density"], gui=(52, 200, 0))
    guard, _ = materials(key, p)
    p = dict(p, split=0.5)
    arms(m, p, guard, guards=False)
    half = p["half"]
    wb = p["wb"]
    zf, zr = -0.2 * wb - 6.0, 0.18 * wb + 6.0
    frames.carbon_plate(m, -half, zf, half, zr, -p["thick"] - 0.3, -0.3, "bottom_plate",
                        holes=[(sx * 10.2, sz * 10.2, 0.9) for sx in (-1, 1) for sz in (-1, 1)])
    plate = p["plate"]
    cz = p["cam_z"] if p["cam_z"] > zf else zf + 4.0
    if p.get("camera_set") and p["cam_z"] < zf:
        cz = p["cam_z"]
        frames.carbon_plate(m, -half * 0.75, cz - 7.0, half * 0.75, zf, -p["thick"] - 0.3, -0.3, "bottom_plate_nose")
    side = half - 1.2
    for sx in (-1, 1):
        m.box(sx * side, 0.0, cz + 2.0, sx * (side + 1.2), plate - 1.2, zr - 4.0, guard, name="canopy_side")
    m.box(-side - 1.2, plate - 1.2, cz + 6.0, side + 1.2, plate, zr - 4.0, guard, name="canopy_top")
    m.box(-side - 1.2, 0.0, zr - 5.2, side + 1.2, plate - 1.2, zr - 4.0, guard, name="canopy_back")
    m.box(-half * 0.55, plate, cz + 10.0, half * 0.55, plate + 0.8, zr - 8.0, "rubber", name="battery_pad", skip=("down",))
    for sx in (-1, 1):
        m.box(sx * 7.7, 0.0, cz - 5.0, sx * 9.0, min(plate, 12.0), cz + 6.0, guard, name="cam_cheek")
    m.box(-7.7, min(plate, 12.0) * 0.5, cz + 1.0, 7.7, min(plate, 12.0) * 0.5 + 1.2, cz + 2.0, "dark_grey", name="cam_axle",
          skip=("up", "down"))
    return m


def build(key, definition, overrides=None):
    common.setup()
    p = params(key, definition, overrides)
    if p["role"] == "whoop":
        return build_whoop(key, p)
    if p["ducted"]:
        return build_cinewhoop(key, p)
    if p["role"] == "toothpick":
        return build_toothpick(key, p)
    return build_x(key, p)
