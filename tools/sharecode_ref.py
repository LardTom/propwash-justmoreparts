import json
import math
import re
import struct
import sys
import zlib

PREFIX = "PW1-"
ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
NAMESPACES = ["", "propwash", "justmoreparts"]
PAINT_SLOTS = ["frame", "tpu", "motors", "prop_fl", "prop_fr", "prop_rl", "prop_rr", "stack", "battery", "camera", "antenna", "accessories"]
AXES = ["roll", "pitch", "yaw"]
RATE_FIELDS = ["rcRate", "superRate", "rcExpo", "centerRateDps", "maxRateDps", "actualExpo"]
TUNE_KEYS = (["rates.type"] + [f"rates.{a}.{f}" for a in AXES for f in RATE_FIELDS] + ["rates.rcDeadband", "rates.yawDeadband", "pid.source"]
             + [f"pid.{a}.{t}" for a in AXES for t in ["p", "i", "d", "ff"]]
             + ["pid.angleStrength", "pid.angleLimitDeg", "pid.horizonStrength", "pid.itermRelax", "pid.antiGravityGain", "pid.airmode",
                "pid.tpaMode", "pid.tpaRatePercent", "pid.tpaBreakpoint", "pid.motorIdlePercent", "pid.throttleMid", "pid.throttleExpo",
                "pid.throttleLimitType", "pid.throttleLimitPercent", "pid.thrustLinearPercent", "pid.vbatSagCompensationPercent",
                "filters.pidLoop", "filters.gyroLpfHz", "filters.dtermLpf1Hz", "filters.dtermLpf2Hz", "filters.rcSmoothingHz",
                "camera.uptiltDeg"])
LAYERS = ["parts", "paint", "tune", "osd", "name"]
SLOTS = ["top", "bottom"]
ID_CHARS = re.compile(r"^[a-z0-9_.-]+$")


def shared_group(key):
    if key.startswith("rates.") or key.startswith("filters.") or key.startswith("camera."):
        return True
    return key.startswith("pid.")


def f32(value):
    return struct.unpack(">f", struct.pack(">f", value))[0]


def varint(n):
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        if n:
            out.append(b | 0x80)
        else:
            out.append(b)
            return bytes(out)


def zigzag(n):
    return (n << 1) ^ (n >> 63) if n < 0 else n << 1


def unzigzag(n):
    return (n >> 1) ^ -(n & 1)


def text(s):
    data = s.encode("utf-8")
    return varint(len(data)) + data


def ref(identifier):
    ns, path = identifier.split(":", 1)
    if ns in NAMESPACES[1:]:
        return varint(NAMESPACES.index(ns)) + text(path)
    return varint(0) + text(ns) + text(path)


def encode(data, compress="auto"):
    blocks = []
    mask = 1
    build = data["build"]
    parts = b"".join(ref(build[k]) for k in ["frame", "stack", "motor", "prop", "video", "battery"])
    accessories = [(SLOTS.index(slot), build["accessories"][slot]) for slot in SLOTS if slot in build.get("accessories", {})]
    parts += bytes([len(accessories)]) + b"".join(bytes([slot]) + ref(identifier) for slot, identifier in accessories)
    blocks.append(parts)
    if data.get("paint"):
        mask |= 2
        bits = 0
        rgb = b""
        for i, slot in enumerate(PAINT_SLOTS):
            if slot in data["paint"]:
                bits |= 1 << i
                rgb += data["paint"][slot].to_bytes(3, "big")
        blocks.append(bits.to_bytes(2, "big") + rgb)
    if data.get("tune") is not None:
        mask |= 4
        entries = []
        for key, value in data["tune"].items():
            if not shared_group(key):
                continue
            index = TUNE_KEYS.index(key) + 1 if key in TUNE_KEYS else 0
            entries.append((index if index else 10 ** 9, key, index, f32(value)))
        entries.sort(key=lambda e: (e[0], e[1]))
        body = varint(len(entries))
        for _, key, index, value in entries:
            q = round(value * 1000.0) if math.isfinite(value * 1000.0) else 2 ** 31
            scaled = abs(q) < 2 ** 31 and struct.pack(">f", q / 1000.0) == struct.pack(">f", value)
            body += varint((index << 1) | (0 if scaled else 1))
            if index == 0:
                body += text(key)
            body += varint(zigzag(q)) if scaled else struct.pack(">f", value)
        blocks.append(body)
    if data.get("osd"):
        mask |= 8
        blocks.append(varint(data["osd"]["version"]) + text(data["osd"]["json"]))
    if data.get("name"):
        mask |= 16
        blocks.append(text(data["name"]))
    payload = varint(mask) + b"".join(varint(len(b)) + b for b in blocks)
    flags = 0
    stored = payload
    if compress in ("auto", "always"):
        c = zlib.compressobj(9, zlib.DEFLATED, -15)
        packed = c.compress(payload) + c.flush()
        if compress == "always" or len(packed) < len(payload):
            flags, stored = 1, packed
    frame = bytes([1, flags]) + stored
    frame += (zlib.crc32(frame) & 0xFFFFFFFF).to_bytes(4, "big")
    return PREFIX + group(base32(frame))


def base32(data):
    bits = "".join(f"{b:08b}" for b in data)
    bits += "0" * ((5 - len(bits) % 5) % 5)
    return "".join(ALPHABET[int(bits[i:i + 5], 2)] for i in range(0, len(bits), 5))


def group(s):
    return "-".join(s[i:i + 5] for i in range(0, len(s), 5))


def unbase32(s):
    bits = ""
    for ch in s:
        ch = {"O": "0", "I": "1", "L": "1"}.get(ch, ch)
        if ch not in ALPHABET:
            raise ValueError("BAD_CHARACTER")
        bits += f"{ALPHABET.index(ch):05b}"
    usable = len(bits) // 8 * 8
    if "1" in bits[usable:]:
        raise ValueError("CORRUPT")
    return bytes(int(bits[i:i + 8], 2) for i in range(0, usable, 8))


class Reader:
    def __init__(self, data):
        self.data, self.pos = data, 0

    def byte(self):
        if self.pos >= len(self.data):
            raise ValueError("TRUNCATED")
        self.pos += 1
        return self.data[self.pos - 1]

    def take(self, n):
        if self.pos + n > len(self.data):
            raise ValueError("TRUNCATED")
        self.pos += n
        return self.data[self.pos - n:self.pos]

    def varint(self):
        value = 0
        for i in range(5):
            b = self.byte()
            value |= (b & 0x7F) << (7 * i)
            if not b & 0x80:
                if value > 0xFFFFFFFF:
                    raise ValueError("CORRUPT")
                return value
        raise ValueError("CORRUPT")

    def text(self):
        try:
            return self.take(self.varint()).decode("utf-8")
        except UnicodeDecodeError:
            raise ValueError("CORRUPT")

    def ref(self):
        ns = self.varint()
        namespace = self.text() if ns == 0 else NAMESPACES[ns] if ns < len(NAMESPACES) else None
        if ns == 0 and not (valid_id_part(namespace, 64) and namespace != ".."):
            raise ValueError("CORRUPT")
        path = self.text()
        if not valid_id_part(path, 128):
            raise ValueError("CORRUPT")
        return (f"{namespace}:{path}" if namespace else f"?{ns}:{path}"), ns, namespace is not None


def valid_id_part(text, limit):
    return 1 <= len(text) <= limit and ID_CHARS.match(text) is not None


def skip_once(out, marker):
    if marker not in out["skipped"]:
        out["skipped"].append(marker)


def decode(code):
    s = "".join(code.split()).upper()
    if not s:
        raise ValueError("EMPTY")
    if len(s) > 4000:
        raise ValueError("TOO_LONG")
    if not s.startswith("PW1"):
        raise ValueError("INVALID_PREFIX")
    frame = unbase32(s[3:].replace("-", ""))
    if len(frame) > 2500:
        raise ValueError("TOO_LARGE")
    if len(frame) < 7:
        raise ValueError("TRUNCATED")
    if (zlib.crc32(frame[:-4]) & 0xFFFFFFFF) != int.from_bytes(frame[-4:], "big"):
        raise ValueError("CHECKSUM")
    if frame[0] != 1 or frame[1] & 0xFE:
        raise ValueError("UNSUPPORTED_VERSION")
    payload = frame[2:-4]
    if frame[1] & 1:
        d = zlib.decompressobj(-15)
        try:
            payload = d.decompress(payload, 16384)
        except zlib.error:
            raise ValueError("CORRUPT")
        if d.unconsumed_tail:
            raise ValueError("TOO_LARGE")
        if not d.eof or d.unused_data:
            raise ValueError("CORRUPT")
    r = Reader(payload)
    mask = r.varint()
    if not mask & 1:
        raise ValueError("CORRUPT")
    out = {"layers": [], "skipped": []}
    bit = 0
    while mask >> bit:
        if mask >> bit & 1:
            block = Reader(r.take(r.varint()))
            name = LAYERS[bit] if bit < len(LAYERS) else None
            if name is None:
                out["skipped"].append(f"layer:{bit}")
            else:
                out["layers"].append(name)
                read_block(name, block, out)
                if block.pos != len(block.data):
                    raise ValueError("CORRUPT")
        bit += 1
    if r.pos != len(r.data):
        raise ValueError("CORRUPT")
    return out


def read_block(name, r, out):
    if name == "parts":
        build = {}
        for key in ["frame", "stack", "motor", "prop", "video", "battery"]:
            identifier, ns, known = r.ref()
            build[key] = identifier
            if not known:
                skip_once(out, f"namespace:{ns}")
        build["accessories"] = {}
        for _ in range(r.byte()):
            slot = r.byte()
            identifier, ns, known = r.ref()
            if not known:
                skip_once(out, f"namespace:{ns}")
            if slot < len(SLOTS):
                if SLOTS[slot] in build["accessories"]:
                    out["skipped"].append(f"accessory_slot_duplicate:{slot}")
                build["accessories"][SLOTS[slot]] = identifier
            else:
                out["skipped"].append(f"accessory_slot:{slot}")
        out["build"] = build
    elif name == "paint":
        bits = int.from_bytes(r.take(2), "big")
        paint = {}
        for i in range(16):
            if bits >> i & 1:
                rgb = int.from_bytes(r.take(3), "big")
                if i < len(PAINT_SLOTS):
                    paint[PAINT_SLOTS[i]] = rgb
                else:
                    out["skipped"].append(f"paint_slot:{i}")
        out["paint"] = paint
    elif name == "tune":
        tune = {}
        count = r.varint()
        if count > 256:
            raise ValueError("CORRUPT")
        for _ in range(count):
            h = r.varint()
            index, raw = h >> 1, h & 1
            key = r.text() if index == 0 else TUNE_KEYS[index - 1] if index <= len(TUNE_KEYS) else None
            if index == 0 and not 1 <= len(key) <= 64:
                raise ValueError("CORRUPT")
            value = struct.unpack(">f", r.take(4))[0] if raw else f32(unzigzag(r.varint()) / 1000.0)
            if not math.isfinite(value):
                raise ValueError("CORRUPT")
            if key is None:
                out["skipped"].append(f"tune_index:{index}")
            elif not shared_group(key):
                out["skipped"].append(f"tune_key:{key}")
            else:
                if key in tune:
                    out["skipped"].append(f"tune_duplicate:{key}")
                tune[key] = value
        out["tune"] = tune
    elif name == "osd":
        version, text_value = r.varint(), r.text()
        if version < 1 or version > 2 ** 31 - 1 or len(text_value) > 8192:
            raise ValueError("CORRUPT")
        out["osd"] = {"version": version, "json": text_value}
    elif name == "name":
        value = r.text().strip()
        if not value or len(value) > 48:
            raise ValueError("CORRUPT")
        out["name"] = value


VECTORS = {
    "A_parts_only": ({"build": {"frame": "propwash:freestyle_5", "stack": "propwash:stack_30", "motor": "propwash:m2207_1750",
                                "prop": "propwash:p51_3b", "video": "propwash:analog_race", "battery": "propwash:b6s_1300"}}, "never"),
    "B_parts_paint_tune_name": ({"build": {"frame": "justmoreparts:deadcat_5", "stack": "justmoreparts:stack_30_6s_60a",
                                           "motor": "justmoreparts:m22075_1850", "prop": "justmoreparts:p130_36_3b_bi",
                                           "video": "justmoreparts:digital_hd_pro", "battery": "propwash:b6s_1300",
                                           "accessories": {"top": "propwash:action_cam_light"}},
                                 "paint": {"frame": 0x1B1D22, "motors": 0xE8B622, "prop_fl": 0xFF7A1A, "prop_fr": 0xFF7A1A,
                                           "prop_rl": 0xFF7A1A, "prop_rr": 0xFF7A1A},
                                 "tune": {"rates.roll.rcRate": 1.2, "rates.pitch.rcRate": 1.2, "rates.yaw.rcRate": 1.05,
                                          "pid.roll.p": 48.0, "pid.throttleMid": 0.4567891, "pid.motorIdlePercent": 5.5,
                                          "filters.gyroLpfHz": 250.0},
                                 "name": "Deadcat Test"}, "never"),
    "C_all_layers_compressed": ({"build": {"frame": "justmoreparts:xclass_13", "stack": "justmoreparts:stack_30_8s_100a",
                                           "motor": "justmoreparts:m4214_400", "prop": "justmoreparts:p330_70_2b_carbon",
                                           "video": "justmoreparts:digital_lr", "battery": "justmoreparts:b8s_5000"},
                                 "paint": {"frame": 0x202020, "battery": 0x2F6FD6},
                                 "tune": {"rates.roll.centerRateDps": 120.0, "rates.roll.maxRateDps": 420.0, "pid.tpaRatePercent": 40.0},
                                 "osd": {"version": 1, "json": "{\"v\":1,\"preset\":\"race\",\"elements\":{\"speed\":{\"on\":true,\"col\":1,\"row\":12}}}"},
                                 "name": "X-Class 13"}, "always"),
    "D_foreign_namespace": ({"build": {"frame": "otheraddon:frame_x", "stack": "propwash:stack_30", "motor": "propwash:m2207_1750",
                                       "prop": "propwash:p51_3b", "video": "propwash:analog_race", "battery": "propwash:b6s_1300"},
                             "tune": {"rates.yaw.rcRate": 1.0, "pid.newFlightKey": 3.0}}, "never"),
}


EXTRA_VECTORS = {
    "G_camera_angle": ({"build": {"frame": "propwash:race_5", "stack": "propwash:stack_30", "motor": "propwash:m2306_1860",
                                  "prop": "propwash:p51_3b", "video": "propwash:analog_race", "battery": "propwash:b6s_1100"},
                        "tune": {"rates.roll.rcRate": 1.1, "camera.uptiltDeg": 52.0}}, "never"),
}


def crafted_skips():
    build = VECTORS["A_parts_only"][0]["build"]
    parts = b"".join(ref(build[k]) for k in ["frame", "stack", "motor", "prop", "video", "battery"]) + bytes([1, 5]) + ref("propwash:led_strip")
    paint = ((1 << 0) | (1 << 13)).to_bytes(2, "big") + (0x112233).to_bytes(3, "big") + (0x445566).to_bytes(3, "big")
    tune = varint(3) + varint(200 << 1) + varint(zigzag(7000)) + varint(0) + text("vtx.channel") + varint(zigzag(5000)) \
        + varint(TUNE_KEYS.index("pid.airmode") + 1 << 1) + varint(zigzag(1000))
    future = b"\x01\x02\x03"
    mask = 1 | 2 | 4 | 64
    payload = varint(mask) + b"".join(varint(len(b)) + b for b in [parts, paint, tune, future])
    frame = bytes([1, 0]) + payload
    frame += (zlib.crc32(frame) & 0xFFFFFFFF).to_bytes(4, "big")
    return PREFIX + group(base32(frame))


def main():
    if len(sys.argv) > 1:
        print(json.dumps(decode(sys.argv[1]), indent=2))
        return
    codes = {name: encode(data, mode) for name, (data, mode) in VECTORS.items()}
    codes["E_tolerant_skips"] = crafted_skips()
    a = codes["A_parts_only"]
    flipped = a[:10] + ("1" if a[10] != "1" else "2") + a[11:]
    for name, code in codes.items():
        print(name)
        print(code)
        print(json.dumps(decode(code), sort_keys=True))
        print()
    print("F_checksum_error")
    print(flipped)
    try:
        decode(flipped)
    except ValueError as e:
        print(str(e))
    for name, (data, mode) in EXTRA_VECTORS.items():
        code = encode(data, mode)
        print()
        print(name)
        print(code)
        print(json.dumps(decode(code), sort_keys=True))


if __name__ == "__main__":
    main()
