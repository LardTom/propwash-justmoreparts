from . import common, palette, paths

import kit
import paint
import video

WHOOP_ANTENNA_TILT = 25


def params(key, definition, overrides=None):
    width = float(definition.get("camera_width_mm", 19.0))
    digital = definition.get("link", "analog") == "digital"
    power = int(definition.get("vtx_power_mw", 25))
    mass = float(definition.get("mass_g", 10.0))
    bezel = palette.fit(palette.accent(definition, "#d6352c"), "camera")
    if width <= 14.0:
        kind = "digital_micro" if digital else "analog_whoop"
    elif digital:
        kind = "digital_hd" if mass >= 20.0 else "digital_lr" if power > 600 else "digital_lite"
    else:
        kind = "analog_lr" if power >= 1000 else "analog_race"
    p = dict(width=width, digital=digital, power=power, mass=mass, bezel=bezel, kind=kind,
             lens=18.0 if key.endswith("_pro") and kind == "digital_hd" else None, label="HD")
    if overrides:
        p.update(overrides)
    return p


def bezel_material(key, p):
    return common.material(f"cam_bezel_{key}", paint.alu(p["bezel"]), "video", "camera", p["bezel"])


def camera_for(m, p, bezel):
    w = p["width"]
    if w <= 14.0:
        return video.camera(m, w, w, 12.0, -6.0, 9.0, 4.5, bezel=bezel)
    lens = p["lens"] or (16.0 if p["kind"] == "digital_hd" else 12.0 if not p["digital"] else 11.0 if w < 19.0 else 12.5)
    depth = 18.0 if p["kind"] == "digital_hd" else 17.0 if w >= 19.0 else 13.0
    return video.camera(m, w, w, depth, -w * 0.45, lens, lens * 0.5, bezel=bezel)


def build_analog_whoop(key, p):
    m = kit.Model("video", key, density=10.0, gui=(20, 205, 0), gui_fill=14.0)
    camera_for(m, p, bezel_material(key, p))
    m.box(-6.0, -6.0, 6.0, 6.0, 6.0, 7.5, "pcb", name="cam_pcb")
    for i, mat in enumerate(("wire_yellow", "wire_red", "wire_black")):
        m.box(-2.4 + i * 1.6 - 0.5, -3.5, 7.5, -2.4 + i * 1.6 + 0.5, -2.5, 12.0, mat, name="cam_wire", skip=("north",))
    m.box(-6.5, -6.0, 12.0, 6.5, -4.4, 24.0, "pcb", name="vtx_board", decals={"up": video.vtx_label})
    m.box(-2.0, -4.4, 16.0, 2.0, -3.4, 20.0, "chip", name="vtx_chip")
    video.antenna(m, 0.0, -4.4, 22.5, (WHOOP_ANTENNA_TILT, 0, 0), 22.0, 0.8, stalk="coax", tip="antenna_white", tip_len=8.0,
                  tip_d=1.0)
    return m


def build_digital_micro(key, p):
    m = kit.Model("video", key, density=10.0, gui=(20, 205, 0), gui_fill=14.0)
    camera_for(m, p, bezel_material(key, p))
    m.box(-1.2, -2.0, 6.0, 1.2, 0.0, 9.0, "coax", name="ribbon")
    m.box(-10.0, -3.0, 9.0, 10.0, 1.0, 27.0, "vtx_lite", name="vtx", decals={"up": video.lite_label})
    m.box(-8.5, 1.0, 11.0, 8.5, 2.0, 25.0, "heatsink", name="vtx_heatsink")
    video.antenna(m, 0.0, 1.0, 25.5, (WHOOP_ANTENNA_TILT - 10, 0, 0), 16.0, 1.4, stalk="antenna_black",
                  tee=(12.0, 1.2, "antenna_white"))
    return m


def build_analog(key, p, long_range):
    m = kit.Model("video", key, density=6.5, gui=(20, 205, 0), gui_fill=15.0)
    camera_for(m, p, bezel_material(key, p))
    w = p["width"]
    z0 = w * 0.47
    length = 24.0 if long_range else 21.0
    m.box(-10.0, -8.0, z0, 10.0, -2.0, z0 + length, "vtx_wrap", name="vtx", decals={"up": video.vtx_label})
    fins = 4 if long_range else 2 if p["power"] >= 500 else 0
    if fins:
        for i in range(fins):
            x = -7.5 + i * 5.0 if fins == 4 else -2.5 + i * 5.0
            m.box(x - 0.9, -2.0, z0 + 3.0, x + 0.9, 0.6, z0 + length - 5.0, "heatsink", name="fin")
    az = z0 + length - 4.0
    m.box(-2.2, -2.0, az - 2.0, 2.2, 1.0, az + 2.0, "brass", name="mmcx")
    stalk = 30.0 if long_range else 26.0
    video.antenna(m, 0.0, 1.0, az, (42, 0, 0), stalk, 2.4, stalk="antenna_black")
    cap = 6.5 if long_range else 5.5
    m.box(-cap, 1.0 + stalk, az - cap, cap, 1.0 + stalk + 7.0, az + cap, "antenna_red", name="lollipop", rot=(42, 0, 0),
          origin=(0.0, 1.0, az))
    m.box(-cap * 0.73, stalk, az - cap * 0.73, cap * 0.73, 1.0 + stalk + 8.2, az + cap * 0.73, "antenna_red", name="lollipop_cap",
          rot=(42, 45, 0), origin=(0.0, 1.0, az))
    for i, mat in enumerate(("wire_yellow", "wire_red", "wire_black")):
        m.box(-2.0 + i * 1.6 - 0.5, -5.0, z0 - 1.5, -2.0 + i * 1.6 + 0.5, -4.0, z0, mat, name="cam_wire")
    return m


def build_digital(key, p):
    kind = p["kind"]
    if kind == "digital_lite":
        m = kit.Model("video", key, density=6.0, gui=(20, 205, 0), gui_fill=15.0)
        camera_for(m, p, bezel_material(key, p))
        w = p["width"]
        z0 = w * 0.47
        m.box(-10.5, -5.0, z0, 10.5, 1.0, z0 + 22.0, "vtx_lite", name="vtx", decals={"up": video.lite_label})
        m.box(-9.5, 1.0, z0 + 3.0, 9.5, 2.4, z0 + 19.0, "heatsink", name="vtx_heatsink")
        m.box(-1.5, -2.0, z0 - 0.5, 1.5, 0.0, z0, "coax", name="ribbon")
        for s in (-1, 1):
            video.antenna(m, s * 7.5, 1.0, z0 + 20.0, (58, s * 32, 0), 20.0, 1.6, stalk="antenna_black",
                          tee=(14.0, 1.4, "antenna_white"))
        return m
    m = kit.Model("video", key, density=5.5, gui=(20, 205, 0), gui_fill=15.0)
    camera_for(m, p, bezel_material(key, p))
    w = p["width"]
    big = kind == "digital_hd"
    uw, uh, ul = (30.0, 10.5, 34.0) if big else (26.0, 9.0, 30.0)
    z0 = w * 0.45 + 3.0
    m.box(-1.8, 2.0, z0 - 3.0, 1.8, 4.0, z0 + 1.0, "coax", name="ribbon")
    side = side_label(p["label"])
    m.box(-uw / 2, 2.0, z0, uw / 2, 2.0 + uh, z0 + ul, "airunit", name="air_unit", decals={"east": side, "west": side})
    fins = 6 if big else 5
    for i in range(fins):
        x = -uw / 2 + 2.5 + i * (uw - 5.0) / (fins - 1)
        m.box(x - 1.0, 2.0 + uh, z0 + 3.0, x + 1.0, 5.0 + uh, z0 + ul - 3.0, "heatsink", name="fin")
    m.box(-9.0, 2.5, z0 + ul, 9.0, 2.0 + uh - 2.0, z0 + ul + 1.0, "airunit_port", name="port_panel")
    length = 34.0 if big else 44.0
    for s in (-1, 1):
        ax = s * (uw / 2 - 4.0)
        m.box(ax - 2.0, 2.0 + uh - 4.5, z0 + ul - 2.0, ax + 2.0, 2.0 + uh - 1.5, z0 + ul + 2.5, "brass", name="antenna_socket")
        video.antenna(m, ax, 2.0 + uh - 1.5, z0 + ul + 0.5, (64, s * 28, 0), length, 3.4, stalk="antenna_black", tip="antenna_red",
                      tip_len=8.0 if big else 12.0, tip_d=4.2 if big else 5.4)
    return m


def side_label(text):
    if text == "HD":
        return video.hd_side

    def painter(region, ctx):
        h, w = region.shape[:2]
        if w >= 16 and h >= 7:
            paint.text_centered(region, text, paint.hexc("#e4e6ea"))
            paint.rect(region, 1, h - 2, w - 1, h - 1, paint.hexc("#d6352c"))

    return painter


def build(key, definition, overrides=None):
    common.setup()
    p = params(key, definition, overrides)
    kind = p["kind"]
    if kind == "analog_whoop":
        return build_analog_whoop(key, p)
    if kind == "digital_micro":
        return build_digital_micro(key, p)
    if kind in ("analog_race", "analog_lr"):
        return build_analog(key, p, kind == "analog_lr")
    return build_digital(key, p)
