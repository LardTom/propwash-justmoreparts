import io
import json
import math
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

from . import paths

import kit
import render

PART_YAW = {"frame": -20.0, "video": -20.0}
GUI_PITCH = 45.0
GUI_YAW = 225.0
CATEGORIES = ("frame", "stack", "motor", "prop", "video", "battery", "accessory")
CONTEXTS = ("gui", "firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand",
            "ground", "fixed", "head")
LEFT = {"firstperson_lefthand": "firstperson_righthand", "thirdperson_lefthand": "thirdperson_righthand"}


class AssetStore:
    def __init__(self, roots):
        self.roots = []
        for root in roots:
            root = Path(root)
            if root.is_file():
                self.roots.append(zipfile.ZipFile(root))
            elif root.is_dir():
                self.roots.append(root)
        self._json = {}
        self._tex = {}

    def read(self, ns, rel):
        for root in self.roots:
            if isinstance(root, zipfile.ZipFile):
                name = f"assets/{ns}/{rel}"
                try:
                    return root.read(name)
                except KeyError:
                    continue
            file = root / ns / rel
            if file.exists():
                return file.read_bytes()
        return None

    def exists(self, resource_id, folder, suffix=".json"):
        ns, path = split_id(resource_id)
        return self.read(ns, f"{folder}/{path}{suffix}") is not None

    def json(self, resource_id, folder):
        key = (resource_id, folder)
        if key not in self._json:
            ns, path = split_id(resource_id)
            data = self.read(ns, f"{folder}/{path}.json")
            self._json[key] = json.loads(data.decode("utf-8")) if data is not None else None
        return self._json[key]

    def texture(self, sprite):
        if isinstance(sprite, dict):
            sprite = sprite["sprite"]
        if sprite not in self._tex:
            ns, path = split_id(sprite)
            data = self.read(ns, f"textures/{path}.png")
            if data is None:
                img = np.zeros((2, 2, 4))
                img[0, 0] = img[1, 1] = (255, 0, 255, 255)
                img[0, 1] = img[1, 0] = (0, 0, 0, 255)
            else:
                img = np.asarray(Image.open(io.BytesIO(data)).convert("RGBA"), dtype=np.float64)
            self._tex[sprite] = img
        return self._tex[sprite]

    def items(self, ns):
        out = set()
        for root in self.roots:
            if isinstance(root, zipfile.ZipFile):
                prefix = f"assets/{ns}/items/"
                for name in root.namelist():
                    if name.startswith(prefix) and name.endswith(".json") and "/" not in name[len(prefix):]:
                        out.add(name[len(prefix):-5])
            else:
                folder = root / ns / "items"
                if folder.is_dir():
                    out.update(p.stem for p in folder.glob("*.json"))
        return sorted(out)


def split_id(resource_id):
    if ":" in resource_id:
        return tuple(resource_id.split(":", 1))
    return "minecraft", resource_id


def m_translate(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def m_scale(x, y=None, z=None):
    y = x if y is None else y
    z = x if z is None else z
    return np.diag([x, y, z, 1.0])


def m_rot(axis, degrees):
    a = math.radians(degrees)
    c, s = math.cos(a), math.sin(a)
    m = np.eye(4)
    if axis == "x":
        m[1:3, 1:3] = [[c, -s], [s, c]]
    elif axis == "y":
        m[0, 0], m[0, 2], m[2, 0], m[2, 2] = c, s, -s, c
    else:
        m[0:2, 0:2] = [[c, -s], [s, c]]
    return m


def m_quat(q):
    x, y, z, w = (float(v) for v in q)
    m = np.eye(4)
    m[:3, :3] = [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                 [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                 [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]
    return m


def chain(*ms):
    out = np.eye(4)
    for m in ms:
        out = out @ m
    return out


def item_transform(display, left=False):
    if not display:
        return m_translate(-0.5, -0.5, -0.5)
    rx, ry, rz = display.get("rotation", [0, 0, 0])
    tx, ty, tz = (max(-80.0, min(80.0, v)) / 16.0 for v in display.get("translation", [0, 0, 0]))
    sx, sy, sz = (max(-4.0, min(4.0, v)) for v in display.get("scale", [1, 1, 1]))
    if left:
        tx, ry, rz = -tx, -ry, -rz
    return chain(m_translate(tx, ty, tz), m_rot("x", rx), m_rot("y", ry), m_rot("z", rz), m_scale(sx, sy, sz),
                 m_translate(-0.5, -0.5, -0.5))


def transformation(node):
    t = node.get("transformation")
    if not t:
        return np.eye(4)
    if isinstance(t, list):
        return np.array(t, dtype=float).reshape(4, 4).T
    return chain(m_translate(*t.get("translation", [0, 0, 0])), m_quat(t.get("left_rotation", [0, 0, 0, 1])),
                 m_scale(*t.get("scale", [1, 1, 1])), m_quat(t.get("right_rotation", [0, 0, 0, 1])))


def model_display(store, model_id, context):
    ref = model_id
    for _ in range(8):
        data = store.json(ref, "models") if ":" in ref and not ref.startswith("minecraft:") else None
        if not data:
            return {}
        display = data.get("display", {})
        if context in display:
            return display[context]
        if context in LEFT and LEFT[context] in display:
            return display[LEFT[context]]
        ref = data.get("parent")
        if not ref:
            return {}
    return {}


def model_quads(store, model_id):
    data = store.json(model_id, "models")
    if data is None:
        return [], False
    textures = dict(data.get("textures", {}))
    translucent = any(isinstance(v, dict) and v.get("force_translucent") for v in textures.values())
    quads = []
    for el in data.get("elements", []):
        f = np.array(el["from"], dtype=float)
        t = np.array(el["to"], dtype=float)
        rot = el.get("rotation")
        m = np.eye(3)
        origin = np.zeros(3)
        if rot:
            origin = np.array(rot["origin"], dtype=float)
            if "axis" in rot:
                ang = {"x": (rot["angle"], 0, 0), "y": (0, rot["angle"], 0), "z": (0, 0, rot["angle"])}[rot["axis"]]
            else:
                ang = (rot.get("x", 0), rot.get("y", 0), rot.get("z", 0))
            m = kit.element_matrix(*ang)
        for d, face in el["faces"].items():
            p0, u, v = render.face_frame(f, t, d)
            ref = face["texture"]
            seen = 0
            while isinstance(ref, str) and ref.startswith("#") and seen < 8:
                ref = textures.get(ref[1:], "minecraft:missingno")
                seen += 1
            corners = np.array([p0, p0 + u, p0 + v, p0 + u + v])
            corners = (corners - origin) @ m.T + origin
            uv = face.get("uv") or [0, 0, 16, 16]
            quads.append((corners / 16.0, uv, store.texture(ref), face.get("rotation", 0)))
    return quads, translucent


class Layer:
    def __init__(self, model_id, local, display_model, fit=None):
        self.model_id = model_id
        self.local = local
        self.display_model = display_model
        self.fit = fit


def when_matches(when, context):
    if isinstance(when, list):
        return context in when or any(context == w.split(":")[-1] for w in when)
    return when == context or when.split(":")[-1] == context


def walk(store, node, context, local, item_id, category):
    kind = node.get("type", "minecraft:model").split(":")[-1]
    here = local @ transformation(node)
    if kind == "model":
        return [Layer(node["model"], here, node["model"])]
    if kind == "select":
        if node.get("property", "").split(":")[-1] == "display_context":
            for case in node.get("cases", []):
                if when_matches(case["when"], context):
                    return walk(store, case["model"], context, here, item_id, category)
        if "fallback" in node:
            return walk(store, node["fallback"], context, here, item_id, category)
        cases = node.get("cases", [])
        return walk(store, cases[0]["model"], context, here, item_id, category) if cases else []
    if kind == "range_dispatch":
        if "fallback" in node:
            return walk(store, node["fallback"], context, here, item_id, category)
        entries = node.get("entries", [])
        return walk(store, entries[0]["model"], context, here, item_id, category) if entries else []
    if kind == "condition":
        return walk(store, node["on_false"], context, here, item_id, category)
    if kind == "composite":
        out = []
        for child in node.get("models", []):
            out += walk(store, child, context, here, item_id, category)
        return out
    if kind == "special":
        special = node.get("model", {})
        if special.get("type") == "propwash:part":
            part = special.get("part", item_id)
            layers = part_layers(store, part, category)
            out = []
            for layer in layers:
                out.append(Layer(layer.model_id, here, node.get("base"), fit=(layer.local, PART_YAW.get(category, 0.0))))
            return out
        return [Layer(node.get("base"), here, node.get("base"))]
    return []


def part_layers(store, part_id, category):
    ns, path = split_id(part_id)
    cats = [category] if category else list(CATEGORIES)
    for cat in cats:
        render_id = f"{ns}:drone_render/{cat}/{path}"
        data = store.json(render_id, "items")
        if data is not None:
            return walk(store, data["model"], "none", np.eye(4), part_id, cat)
    return []


def category_of(store, item_id):
    ns, path = split_id(item_id)
    for cat in CATEGORIES:
        if store.exists(f"{ns}:drone_render/{cat}/{path}", "items"):
            return cat
    return None


def item_layers(store, item_id, context, category=None):
    data = store.json(item_id, "items")
    if data is None:
        return []
    category = category or category_of(store, item_id)
    return walk(store, data["model"], context, np.eye(4), item_id, category)


def hull(points):
    return points.min(axis=0), points.max(axis=0)


def apply(m, pts):
    return pts @ m[:3, :3].T + m[:3, 3]


def fit_matrix(quads, inner_local, yaw):
    pts = np.concatenate([apply(inner_local, q[0]) for q in quads]) - 0.5
    lo, hi = hull(pts)
    center = (lo + hi) / 2.0
    rot = chain(m_rot("x", GUI_PITCH), m_rot("y", GUI_YAW + yaw))
    corners = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]) - center
    p = apply(rot, corners)
    extent = max(p[:, 0].max() - p[:, 0].min(), p[:, 1].max() - p[:, 1].min())
    s = 1.0 / extent if extent > 1e-4 else 1.0
    return chain(m_translate(0.5, 0.5, 0.5), m_rot("y", yaw), m_scale(s), m_translate(*(-center)),
                 m_translate(-0.5, -0.5, -0.5), inner_local)


def item_geometry(store, item_id, context, category=None):
    left = context.endswith("lefthand")
    out = []
    translucent_any = False
    for layer in item_layers(store, item_id, context, category):
        quads, translucent = model_quads(store, layer.model_id)
        if not quads:
            continue
        display = model_display(store, layer.display_model, context) if layer.display_model else {}
        inner = np.eye(4)
        if layer.fit is not None:
            inner = fit_matrix(quads, layer.fit[0], layer.fit[1])
        m = chain(item_transform(display, left), layer.local, inner)
        out.append((m, quads, translucent))
        translucent_any |= translucent
    return out


class Projection:
    def __init__(self, width, height, ortho=None, fov=None, center=None):
        self.w, self.h = width, height
        self.ortho = ortho
        self.focal = None if fov is None else (height / 2.0) / math.tan(math.radians(fov) / 2.0)
        self.center = center or (0.0, 0.0)

    def world(self, pts):
        return pts

    def __call__(self, pts):
        out = np.empty_like(pts)
        if self.focal is None:
            out[:, 0] = self.w / 2 + (pts[:, 0] - self.center[0]) * self.ortho
            out[:, 1] = self.h / 2 - (pts[:, 1] - self.center[1]) * self.ortho
        else:
            depth = np.maximum(-pts[:, 2], 1e-3)
            out[:, 0] = self.w / 2 + self.focal * pts[:, 0] / depth
            out[:, 1] = self.h / 2 - self.focal * pts[:, 1] / depth
        out[:, 2] = pts[:, 2]
        return out


def transform_quads(quads, m):
    return [(apply(m, q[0]), q[1], q[2], q[3]) for q in quads]


def solid(colour):
    img = np.zeros((1, 1, 4))
    img[0, 0] = (*colour, 255)
    return img


def box_quads(lo, hi, colour, shade=True):
    out = []
    lo = np.array(lo, dtype=float)
    hi = np.array(hi, dtype=float)
    tone = {"up": 1.0, "down": 0.55, "north": 0.8, "south": 0.8, "east": 0.68, "west": 0.68}
    for d in ("north", "south", "east", "west", "up", "down"):
        p0, u, v = render.face_frame(lo, hi, d)
        c = tuple(int(ch * (tone[d] if shade else 1.0)) for ch in colour)
        out.append((np.array([p0, p0 + u, p0 + v, p0 + u + v]), [0, 0, 16, 16], solid(c), 0))
    return out


def draw(canvas, placed, projection, extra=()):
    for quads in extra:
        canvas.draw(quads, projection)
    for m, quads, translucent in placed:
        canvas.draw(transform_quads(quads, m), projection, translucent=translucent)
    canvas.flush()


def view_gui(store, item_id, size=112, gui_scale=6, category=None):
    canvas = render.Canvas(size, size, background=(0, 0, 0, 0))
    slot = 16 * gui_scale
    placed = item_geometry(store, item_id, "gui", category)
    draw(canvas, placed, Projection(size, size, ortho=slot))
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    bg = Image.new("RGBA", (slot + 2 * gui_scale, slot + 2 * gui_scale), (55, 55, 55, 255))
    inner = Image.new("RGBA", (slot, slot), (139, 139, 139, 255))
    bg.paste(inner, (gui_scale, gui_scale))
    img.alpha_composite(bg, ((size - bg.width) // 2, (size - bg.height) // 2))
    img.alpha_composite(canvas.image())
    return img


def sky(width, height):
    arr = np.zeros((height, width, 4), dtype=np.uint8)
    horizon = int(height * 0.55)
    for y in range(height):
        if y < horizon:
            t = y / max(1, horizon)
            arr[y] = (int(118 + 40 * t), int(160 + 35 * t), int(228 - 12 * t), 255)
        else:
            t = (y - horizon) / max(1, height - horizon)
            arr[y] = (int(86 - 26 * t), int(136 - 40 * t), int(58 - 16 * t), 255)
    return arr.astype(np.float64)


def view_first_person(store, item_id, width=224, height=156, left=False, category=None):
    context = "firstperson_lefthand" if left else "firstperson_righthand"
    side = -1 if left else 1
    canvas = render.Canvas(width, height)
    canvas.color[...] = sky(width, height)
    arm = m_translate(side * 0.56, -0.52, -0.72)
    placed = [(chain(arm, m), q, t) for m, q, t in item_geometry(store, item_id, context, category)]
    draw(canvas, placed, Projection(width, height, fov=70.0))
    img = canvas.image().copy()
    px = img.load()
    cx, cy = width // 2, height // 2
    for d in range(-4, 5):
        px[cx + d, cy] = (235, 235, 235, 255)
        px[cx, cy + d] = (235, 235, 235, 255)
    return img


SKIN = (196, 150, 118)
SHIRT = (60, 160, 170)
PANTS = (60, 60, 150)
HAIR = (92, 62, 40)


def player_parts(left_item):
    px = 1.0 / 16.0
    body = []
    body += box_quads((-4, -8, -4), (4, 0, 4), SKIN)
    body += box_quads((-4.05, -8.05, -4.05), (4.05, -6.2, 4.05), HAIR)
    body += box_quads((-4, 0, -2), (4, 12, 2), SHIRT)
    body += box_quads((-3.95, 12, -2), (0, 24, 2), PANTS)
    body += box_quads((0, 12, -2), (3.95, 24, 2), PANTS)
    arms = {}
    for right in (True, False):
        lo, hi = ((-3, 1) if right else (-1, 3))
        quads = box_quads((lo, -2, -2), (hi, 6, 2), SHIRT) + box_quads((lo, 6, -2), (hi, 10, 2), SKIN)
        holding = (not right) == left_item
        pitch = -18.0 if holding else 0.0
        pose = chain(m_translate((-5 if right else 5) * px, 2 * px, 0), m_rot("x", pitch))
        arms[right] = (pose, quads)
    return body, arms


def view_third_person(store, item_id, width=224, height=156, left=False, category=None):
    context = "thirdperson_lefthand" if left else "thirdperson_righthand"
    px = 1.0 / 16.0
    to_world = chain(m_translate(0, 1.501 * 0.9375, 0), m_scale(-0.9375, -0.9375, 0.9375))
    body, arms = player_parts(left)
    extra = [transform_quads(body, chain(to_world, m_scale(px)))]
    placed = []
    for right, (pose, quads) in arms.items():
        extra.append(transform_quads(quads, chain(to_world, pose, m_scale(px))))
    hand_pose = arms[not left][0]
    hand = chain(to_world, hand_pose, m_rot("x", -90), m_rot("y", 180), m_translate((-1 if left else 1) * px, 2 * px, -10 * px))
    yaw = 150.0 if not left else 210.0
    view = chain(m_rot("x", 12.0), m_rot("y", -yaw), m_translate(0, -1.05, 0))
    for m, q, t in item_geometry(store, item_id, context, category):
        placed.append((chain(view, hand, m), q, t))
    extra = [transform_quads(e, view) for e in extra]
    item_pts = np.concatenate([apply(m, q[0]) for m, quads, _ in placed for q in quads]) if placed else np.zeros((1, 3))
    cx = (item_pts[:, 0].min() + item_pts[:, 0].max()) / 4
    cy = 0.12
    canvas = render.Canvas(width, height, background=(44, 47, 53, 255))
    draw(canvas, placed, Projection(width, height, ortho=height / 1.6, center=(cx, cy)), extra=extra)
    return canvas.image()


def ground_quads():
    out = []
    for ix in range(-2, 3):
        for iz in range(-2, 3):
            tone = (95, 142, 64) if (ix + iz) % 2 == 0 else (86, 132, 58)
            out += [q for q in box_quads((ix - 0.5, -1.0, iz - 0.5), (ix + 0.5, 0.0, iz + 0.5), tone)]
    return out


def view_ground(store, item_id, width=224, height=156, category=None, spin=30.0, bob=0.1):
    geo = item_geometry(store, item_id, "ground", category)
    pts = np.concatenate([apply(m, q[0]) for m, quads, _ in geo for q in quads]) if geo else np.zeros((1, 3))
    lift = -pts[:, 1].min() + 1.0 / 16.0 + bob
    place = chain(m_translate(0, lift, 0), m_rot("y", spin))
    cam = chain(m_translate(0.0, 0.0, -1.7), m_rot("x", 38.0), m_rot("y", -20.0), m_translate(0.0, -0.12, 0.0))
    placed = [(chain(cam, place, m), q, t) for m, q, t in geo]
    canvas = render.Canvas(width, height)
    canvas.color[...] = sky(width, height)
    draw(canvas, placed, Projection(width, height, fov=70.0), extra=[transform_quads(ground_quads(), cam)])
    return canvas.image()


def frame_quads():
    px = 1.0 / 16.0
    out = []
    out += box_quads((-1.5, -1.5, -1.0), (1.5, 1.5, 0.0), (125, 125, 125))
    out += box_quads((-6 * px, -6 * px, 0.0), (6 * px, 6 * px, 0.5 * px), (148, 96, 58))
    out += box_quads((-5 * px, -5 * px, 0.5 * px), (5 * px, 5 * px, 0.6 * px), (122, 80, 52))
    for lo, hi in (((-6, -6), (6, -5)), ((-6, 5), (6, 6)), ((-6, -5), (-5, 5)), ((5, -5), (6, 5))):
        out += box_quads((lo[0] * px, lo[1] * px, 0.5 * px), (hi[0] * px, hi[1] * px, 1.0 * px), (160, 110, 66))
    return out


def view_item_frame(store, item_id, width=224, height=156, category=None, rotation=0):
    px = 1.0 / 16.0
    geo = item_geometry(store, item_id, "fixed", category)
    place = chain(m_translate(0, 0, 1.0 * px), m_rot("y", 180.0), m_rot("z", rotation * 45.0), m_scale(0.5))
    cam = chain(m_translate(0.0, 0.0, -0.95), m_rot("x", 12.0), m_rot("y", -18.0))
    placed = [(chain(cam, place, m), q, t) for m, q, t in geo]
    canvas = render.Canvas(width, height, background=(44, 47, 53, 255))
    draw(canvas, placed, Projection(width, height, fov=60.0), extra=[transform_quads(frame_quads(), cam)])
    return canvas.image()


def render_layer(store, item_id, category):
    ns, path = split_id(item_id)
    data = store.json(f"{ns}:drone_render/{category}/{path}", "items")
    if data is None:
        return None
    layers = walk(store, data["model"], "none", np.eye(4), item_id, category)
    return layers[0] if layers else None


def placed_points(store, model_id, local=None):
    quads, _ = model_quads(store, model_id)
    if not quads:
        return None
    m = local if local is not None else np.eye(4)
    return np.concatenate([apply(m, q[0]) for q in quads])


def true_scale(store, model_id, px_per_mm, rotation=(30, 225, 0), size=None, local=None):
    quads, translucent = model_quads(store, model_id)
    if not quads:
        return None
    rot = chain(m_rot("x", rotation[0]), m_rot("y", rotation[1]), m_rot("z", rotation[2]))
    m = chain(rot, m_scale(16.0 * kit.UNIT_MM * px_per_mm), m_translate(-0.5, -0.5, -0.5))
    if local is not None:
        m = chain(m, local)
    pts = np.concatenate([apply(m, q[0]) for q in quads])
    lo, hi = hull(pts)
    w = int(math.ceil(hi[0] - lo[0])) + 8
    h = int(math.ceil(hi[1] - lo[1])) + 8
    if size:
        w, h = max(w, size[0]), max(h, size[1])
    canvas = render.Canvas(w, h)
    center = ((lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2)
    canvas.draw(transform_quads(quads, m), Projection(w, h, ortho=1.0, center=center), translucent=translucent)
    canvas.flush()
    return canvas.image()
