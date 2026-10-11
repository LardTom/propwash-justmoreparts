import numpy as np

from . import common, palette, paths

import kit
import paint
import stacks
from paint import hexc

BOARDS = {"whoop_aio": (24.0, 10.2), "s20": (27.0, 10.0), "s30": (36.0, 15.25)}


def pcb_sheet(x0, z0, x1, z1, pads=(), label=None, label_pos=None, holes=(), traces=True, silk_box=(), label_color="#e4e6ea"):
    def painter(region, ctx):
        h, w = region.shape[:2]
        mmpp = (x1 - x0) / w
        paint.fill(region, hexc(palette.PCB))
        paint.noise(region, ctx, 0.03)
        yy, xx = np.mgrid[0:h, 0:w]
        mx = x0 + (xx + 0.5) * mmpp
        mz = z0 + (yy + 0.5) * mmpp
        if traces:
            lines = ((xx * 3 + yy * 5) % 23 == 0) | ((xx * 7 + yy * 2) % 31 == 0)
            region[lines, :3] = hexc("#1d2126")
        paint.edges(region, hexc("#2a2e34"), hexc("#0c0d0f"))
        for hx, hz, r in holes:
            d2 = (mx - hx) ** 2 + (mz - hz) ** 2
            region[d2 <= (r + 0.9) ** 2, :3] = hexc("#d9a33a")
            region[d2 <= r * r, :3] = hexc("#08090a")
        for px0, pz0, px1, pz1, color in pads:
            m = (mx >= px0) & (mx <= px1) & (mz >= pz0) & (mz <= pz1)
            region[m, :3] = hexc(color)
        for bx0, bz0, bx1, bz1 in silk_box:
            m = (((np.abs(mx - bx0) < mmpp * 0.6) | (np.abs(mx - bx1) < mmpp * 0.6)) & (mz >= bz0) & (mz <= bz1)) | \
                (((np.abs(mz - bz0) < mmpp * 0.6) | (np.abs(mz - bz1) < mmpp * 0.6)) & (mx >= bx0) & (mx <= bx1))
            region[m, :3] = hexc("#6f757d")
        if label:
            lx, lz = label_pos if label_pos else (0.0, 0.0)
            tx = int((lx - x0) / mmpp - paint.text_width(label) / 2)
            ty = int((lz - z0) / mmpp - 2.5)
            paint.text(region, tx, ty, label, hexc(label_color))

    painter.__name__ = f"pcb_{label}_{label_color}_{x1 - x0:.1f}"
    return painter


def cap_sleeve(colour):
    base = hexc(colour)

    def painter(region, ctx):
        h, w = region.shape[:2]
        paint.fill(region, base)
        if ctx.side == "side":
            for x in range(w):
                region[:, x, :3] = base if x % 6 not in (0, 1) else hexc("#c9cfd8")
        paint.noise(region, ctx, 0.03)

    return painter


def mount_of(definition):
    mount = str(definition.get("mount", "propwash:s30")).split(":")[-1]
    return mount if mount in BOARDS else "s30"


def params(key, definition, overrides=None):
    mount = mount_of(definition)
    board, hole = BOARDS[mount]
    if definition.get("board_size_mm"):
        board = float(definition["board_size_mm"])
    cells = definition.get("cells", [3, 6])
    max_cells = int(cells[1]) if isinstance(cells, list) else int(cells)
    current = float(definition.get("esc_continuous_a", 30.0))
    aio = bool(definition.get("all_in_one", False)) or mount == "whoop_aio"
    accent = palette.accent(definition, "#1f2a44")
    whoop = mount == "whoop_aio" and max_cells <= 2
    toothpick = mount == "whoop_aio" and max_cells > 2
    if toothpick:
        board = 26.0
    tower20 = not aio and board < 30.0
    per_quadrant = 1 if current <= 35.0 else 2 if current <= 65.0 else 3
    fet = common.clamp(common.interp(current, [6.0, 35.0, 60.0, 100.0], [2.6, 3.4, 4.2, 5.0], extrapolate="hold"), 2.4, 5.2)
    if whoop:
        connector = "bt"
    elif mount == "whoop_aio" or (aio and max_cells <= 4):
        connector = "xt30"
    elif max_cells >= 8 and current >= 80.0:
        connector = "xt90"
    else:
        connector = "xt60"
    p = dict(mount=mount, board=board, hole=hole, aio=aio, whoop=whoop, toothpick=toothpick, cells=max_cells,
             current=current, per_quadrant=per_quadrant, fet=fet, connector=connector,
             cap_d=0.0 if whoop else 5.0 if toothpick else 6.3 if aio and max_cells <= 4 else 8.0 if aio or (tower20 and max_cells < 8)
             else 12.0 if max_cells >= 8 and not tower20 else 10.0,
             caps=0 if whoop else 1 if aio or tower20 else 2,
             sleeve=palette.mix(accent, "#101114", 0.35), led="led_red" if max_cells >= 6 else "led_blue",
             label_color=palette.mix(accent, "#ffffff", 0.35),
             fc_label=f"{max_cells}S" if toothpick else "F7" if not aio and board >= 30 else "F4")
    if overrides:
        p.update(overrides)
    return p


XT = {"xt30": (10.2, 5.6, 11.0, 2.0), "xt60": (16.0, 8.0, 15.5, 3.0), "xt90": (21.0, 10.4, 20.5, 4.0)}


def battery_lead(m, z0, y_lead, y_plug, kind, length):
    w, h, l, dia = XT[kind]
    stacks.lead(m, -dia * 0.8 - 0.6, y_lead, z0, z0 + length, dia, "wire_red")
    stacks.lead(m, dia * 0.8 + 0.6, y_lead, z0, z0 + length, dia, "wire_black")
    stacks.xt_connector(m, z0 + length, y_plug, w, h, l)


def build_whoop(key, p):
    m = kit.Model("stack", key, density=12.0, gui=(50, 225, 0))
    half = p["board"] / 2
    hole = p["hole"]
    for sx in (-1, 1):
        for sz in (-1, 1):
            stacks.grommet(m, sx * hole, sz * hole, 0.0, 1.4, 2.4)
    sheet = m.sheet("pcb", -half, -half, half, half,
                    pcb_sheet(-half, -half, half, half, pads=stacks.corner_pads(half, 1.1, n=2),
                              holes=[(sx * hole, sz * hole, 0.8) for sx in (-1, 1) for sz in (-1, 1)],
                              silk_box=[(-4.5, -4.5, 4.5, 4.5)], label=f"{p['cells']}S",
                              label_pos=(0.0, -8.5), label_color=p["label_color"]))
    m.box(-half, 1.4, -half, half, 2.2, half, "pcb", name="pcb", sheet_up=sheet)
    m.box(-2.6, 2.2, -2.6, 2.6, 2.9, 2.6, "chip", name="mcu", rot=(0, 45, 0), origin=(0, 2.2, 0))
    f = p["fet"] / 2
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.box(sx * 7.0 - f, 2.2, sz * 5.0 - f, sx * 7.0 + f, 2.8, sz * 5.0 + f, "chip", name="esc_fet")
            m.box(sx * 9.4 - 1.2, 2.2, sz * 9.4 - 0.9, sx * 9.4 + 1.2, 3.6, sz * 9.4 + 0.9, "bt_white", name="motor_plug",
                  rot=(0, 45 * sx * sz, 0))
    m.box(-2.2, 2.2, 6.0, 2.2, 3.0, 9.5, "chip", name="receiver")
    m.box(2.8, 2.2, 7.2, 3.8, 2.7, 8.2, p["led"], name="led")
    m.box(-11.0, 2.2, -2.0, -8.5, 3.6, 2.0, "steel", name="usb")
    m.box(-0.8, 2.3, 9.5, 0.8, 2.8, 16.0, "coax", name="antenna_lead")
    m.box(-0.5, 2.4, 16.0, 0.5, 2.9, 25.0, "antenna_white", name="antenna")
    stacks.lead(m, -1.1, 2.5, 12.0, 17.5, 1.0, "wire_red")
    stacks.lead(m, 1.1, 2.5, 12.0, 17.5, 1.0, "wire_black")
    wide = 1.0 if p["cells"] == 1 else 1.4
    m.box(-3.2 * wide, 1.0, 17.5, 3.2 * wide, 4.4, 22.0, "bt_white", name="bt_connector", decals={"south": stacks.xt_face})
    return m


def build_aio(key, p):
    half = p["board"] / 2
    hole = p["hole"]
    tp = p["toothpick"]
    m = kit.Model("stack", key, density=8.0 if tp else 6.0, gui=(50, 225, 0))
    sleeve = common.material(f"cap_sleeve_{key}", cap_sleeve(p["sleeve"]))
    gh = 2.0 if tp else 3.0
    for sx in (-1, 1):
        for sz in (-1, 1):
            stacks.grommet(m, sx * hole, sz * hole, 0.0, gh, 3.0 if tp else 3.6)
    sheet = m.sheet("pcb", -half, -half, half, half,
                    pcb_sheet(-half, -half, half, half, pads=stacks.corner_pads(half, 1.5 if tp else 1.8),
                              holes=[(sx * hole, sz * hole, 1.0 if tp else 1.1) for sx in (-1, 1) for sz in (-1, 1)],
                              silk_box=[(-5.5, -5.5, 5.5, 5.5)], label=p["fc_label"], label_pos=(0.0, -half + 4.5),
                              label_color=p["label_color"]))
    top = gh + 1.6
    m.box(-half, gh, -half, half, top, half, "pcb", name="pcb", sheet_up=sheet)
    m.box(-3.4, top, -3.4, 3.4, top + 0.9, 3.4, "chip", name="mcu", rot=(0, 45, 0), origin=(0, top, 0))
    f = p["fet"]
    for sx in (-1, 1):
        for sz in (-1, 1):
            for i in range(p["per_quadrant"]):
                cz = sz * (5.2 + i * (f + 0.6)) if p["per_quadrant"] > 1 else sz * 5.2
                if abs(cz) + f * 0.5 > half - 1.0:
                    continue
                m.box(sx * (half * 0.6) - f * 0.4, top, cz - f / 2, sx * (half * 0.6) + f * 0.4, top + 0.9, cz + f / 2, "chip",
                      name="esc_fet")
    if not tp:
        for sx, sz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            stacks.grommet(m, sx * hole, sz * hole, top, top + 1.4, 3.4, color="dark_grey")
    m.box(half * 0.3, top, half * 0.52, half * 0.3 + 1.4, top + 0.7, half * 0.52 + 1.4, p["led"], name="led")
    m.box(-2.0, top, half * 0.44, 2.0, top + 1.0, half * 0.44 + 3.0, "chip", name="receiver")
    m.box(-half - 1.0, gh + 0.4, -3.0, -half + 2.5, gh + 3.4, 3.0, "steel", name="usb")
    kind = p["connector"]
    lead_len = 11.5 if tp or kind == "xt30" else 9.0
    y_lead = top + 1.4
    if p["caps"]:
        cd = p["cap_d"]
        stacks.cap(m, -half * 0.4, y_lead + cd / 2 - 1.5, half + 2.5 + cd * 0.6, cd, cd * 1.3)
        for el in m.elements:
            if el.name == "capacitor":
                for fs in el.faces.values():
                    if fs.material == "cap_body":
                        fs.material = sleeve
    w, h, l, dia = XT[kind]
    stacks.lead(m, -dia * 0.8 - 0.4, y_lead, half, half + lead_len, dia, "wire_red")
    stacks.lead(m, dia * 0.8 + 0.4, y_lead, half, half + lead_len, dia, "wire_black")
    stacks.xt_connector(m, half + lead_len, gh + 0.2, w, h, l)
    m.box(-0.6, top + 0.4, -half, 0.6, top + 1.2, -half - 4.5, "coax", name="antenna_lead")
    return m


def build_tower(key, p):
    half = p["board"] / 2
    hole = p["hole"]
    big = p["board"] >= 30
    m = kit.Model("stack", key, density=5.5 if big else 7.0, gui=(48, 225, 0))
    sleeve = common.material(f"cap_sleeve_{key}", cap_sleeve(p["sleeve"]))
    holes = [(sx * hole, sz * hole, 1.5 if big else 1.2) for sx in (-1, 1) for sz in (-1, 1)]
    for sx in (-1, 1):
        for sz in (-1, 1):
            stacks.grommet(m, sx * hole, sz * hole, 0.0, 3.0, 4.2 if big else 3.6)
    pad = 2.4 if big else 1.9
    esc_sheet = m.sheet("esc", -half, -half, half, half,
                        pcb_sheet(-half, -half, half, half,
                                  pads=stacks.corner_pads(half, pad) + [(-6.0, half - 4.5, -2.0, half - 1.0, "#d9a33a"),
                                                                        (2.0, half - 4.5, 6.0, half - 1.0, "#d9a33a")],
                                  holes=holes, label=f"{int(p['current'])}A", label_pos=(0.0, 0.0), label_color=p["label_color"]))
    m.box(-half, 3.0, -half, half, 4.6, half, "pcb", name="esc_pcb", sheet_up=esc_sheet)
    f = p["fet"]
    for sx in (-1, 1):
        for sz in (-1, 1):
            for i in range(p["per_quadrant"]):
                cx = sx * half * 0.58
                cz = sz * (half * 0.3 + i * (f * 0.9 + 0.5))
                if abs(cz) + f * 0.45 > half - pad * 1.2:
                    continue
                m.box(cx - f / 2, 4.6, cz - f * 0.42, cx + f / 2, 5.7, cz + f * 0.42, "chip", name="fet")
    fc_y = 11.0 if big else 9.5
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.box(sx * hole - 2.2, 4.6, sz * hole - 2.2, sx * hole + 2.2, fc_y, sz * hole + 2.2, "black", name="nylon_standoff",
                  skip=("up", "down"))
    fc_sheet = m.sheet("fc", -half, -half, half, half,
                       pcb_sheet(-half, -half, half, half, pads=stacks.corner_pads(half, pad * 0.66, color="#e7e9ec", n=2),
                                 holes=holes, silk_box=[(-half * 0.36, -half * 0.36, half * 0.36, half * 0.36)], label=p["fc_label"],
                                 label_pos=(0.0, -half + 6.5)))
    top = fc_y + 1.6
    m.box(-half, fc_y, -half, half, top, half, "pcb", name="fc_pcb", sheet_up=fc_sheet)
    m.box(-4.0, top, -4.0, 4.0, top + 1.0, 4.0, "chip", name="mcu", rot=(0, 45, 0), origin=(0, top, 0))
    m.box(-half * 0.67, top, half * 0.5, -half * 0.5, top + 0.8, half * 0.67, "chip", name="gyro")
    if big:
        m.box(5.0, top, 8.0, 10.0, top + 4.2, 13.0, "black", name="buzzer", decals={"up": stacks.buzzer_top})
    m.box(half * 0.5, top, -half * 0.67, half * 0.61, top + 0.7, -half * 0.56, p["led"], name="led")
    m.box(-half - 1.0, fc_y + 0.4, -3.2, -half + 2.5, fc_y + 3.4, 3.2, "steel", name="usb")
    for sx in (-1, 1):
        for sz in (-1, 1):
            m.box(sx * hole - 2.6, top, sz * hole - 2.6, sx * hole + 2.6, top + 1.8, sz * hole + 2.6, "dark_grey", name="nut",
                  rot=(0, 45, 0))
    for sx in (-1, 1):
        m.box(sx * half * 0.72 - 2.5, top, -2.0, sx * half * 0.72 + 2.5, top + 1.6, 3.0, "bt_white", name="sh_connector")
    cd = p["cap_d"]
    cap_len = 18.0 if cd >= 12.0 else 16.0 if cd >= 10.0 else 12.0
    kind = p["connector"]
    w, h, l, dia = XT[kind]
    if p["caps"] > 1:
        cap_x = (-7.0 * cd / 10.0, 7.0 * cd / 10.0)
    else:
        cap_x = (-(dia * 1.5 + cd / 2 + 0.5),)
    for cx in cap_x:
        stacks.cap(m, cx, 3.0 + cd / 2, half + 1.0 + cap_len / 2, cd, cap_len)
        m.box(cx - 0.4, 4.6, half - 0.5, cx + 0.4, 5.4, half + 1.2, "steel", name="cap_leg")
    for el in m.elements:
        if el.name == "capacitor":
            for fs in el.faces.values():
                if fs.material == "cap_body":
                    fs.material = sleeve
    lead_len = 20.0 if big else 16.0
    stacks.lead(m, -dia * 0.93, 6.2, half - 1.0, half + lead_len, dia, "wire_red")
    stacks.lead(m, dia * 0.93, 6.2, half - 1.0, half + lead_len, dia, "wire_black")
    stacks.xt_connector(m, half + lead_len, 2.2, w, h, l)
    return m


def build(key, definition, overrides=None):
    common.setup()
    p = params(key, definition, overrides)
    if p["whoop"]:
        return build_whoop(key, p)
    if p["aio"]:
        return build_aio(key, p)
    return build_tower(key, p)
