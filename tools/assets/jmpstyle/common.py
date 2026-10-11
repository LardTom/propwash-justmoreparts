from . import paths

import addon
import kit
import paint
import paintable

_ready = {"done": False}


def setup(namespace=paths.NS):
    kit.NAMESPACE = namespace
    if not _ready["done"]:
        addon.register_materials()
        _ready["done"] = True


def clamp(value, low, high):
    return max(low, min(high, value))


def interp(x, xs, ys, extrapolate="scale"):
    if x <= xs[0]:
        if extrapolate == "scale" and xs[0] > 0:
            return ys[0] * x / xs[0]
        return ys[0]
    if x >= xs[-1]:
        if extrapolate == "scale":
            return ys[-1] * x / xs[-1]
        if extrapolate == "slope":
            return ys[-1] + (ys[-1] - ys[-2]) / (xs[-1] - xs[-2]) * (x - xs[-1])
        return ys[-1]
    for i in range(1, len(xs)):
        if x <= xs[i]:
            t = (x - xs[i - 1]) / (xs[i] - xs[i - 1])
            return ys[i - 1] + (ys[i] - ys[i - 1]) * t
    return ys[-1]


def material(name, painter, category=None, channel=None, base=None):
    paint.register(name, painter)
    if category and channel and base:
        paintable.MATERIALS[(category, name)] = (channel, base)
    return name


def density_for(size_mm, xs, ys, low, high):
    return clamp(interp(size_mm, xs, ys, extrapolate="hold"), low, high)
