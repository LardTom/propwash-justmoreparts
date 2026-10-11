# Propwash: Just More Parts

**143 more real-world drone parts for Propwash FPV.** From 65 mm whoops over 2–3.5″ toothpicks and cinewhoops, 5″
freestyle and race quads and 6–8″ long range up to 13″ X-Class: motors, props, batteries, stacks, video systems and
frames in new shapes – stretched X, squashed X, deadcat, H-frame, ducts and X-Class. Plus **share codes**: copy a whole
build with paint, flight tune and OSD layout as one short text and rebuild it anywhere.

> **Requires [Propwash FPV](https://modrinth.com/mod/propwash-fpv) 0.4.0 or newer** on client and server. This is an
> add-on: it adds content, Propwash does the flying.

---

## Honest parts, real physics

Just More Parts does not fake anything. Every part is a data sheet with real-world numbers – weight, stator size and KV,
winding resistance, heat limits, thrust and power coefficients, blade stiffness, cell count, capacity, C rating,
internal resistance, wheelbase, arm layout, drag – and **Propwash's own flight model turns them into flight behaviour**.
So the trade-offs appear by themselves:

- a two-blade prop flies longer, a five-blade prop pulls harder and draws more current
- a Li-Ion pack hovers for half an hour and has no punch; a semi-solid pack is light but overloaded at full throttle
- a 4S stack on 6S burns out when you arm
- a deadcat or H-frame keeps the props out of your camera view; a stretched X rolls faster and flips slower
- an X-Class quad is heavy and slow to turn – and flies for a long time

Hold **Shift** over any part: the tooltip lists its key figures, pros, cons and what it is good for. Every one of those
claims is checked against Propwash's analysis in an automated test bench.

## What's inside

| | Count | Range |
|---|---|---|
| Motors | 34 | 0702 to 4214, several KV per stator, new 31xx and 42xx mounts |
| Props | 38 | 31 mm to 13″, 2–5 blades, soft/hard PC, bi-blend, glass/carbon-fiber nylon, carbon, ducted props |
| Batteries | 30 | 1S 250 mAh to 8S 5000 mAh: LiHV, LiPo, Li-Ion (18650/21700, 6S2P), semi-solid LiHV |
| Cell chemistry | 1 | semi-solid LiHV (up to 4.45 V per cell) |
| Stacks / AIOs | 12 | 1S whoop AIO to 30×30 8S 100 A stack |
| Video systems | 7 | analog 25 mW to 1.6 W, digital from micro to long range, each with its camera's field of view (110–133°) |
| Frames | 22 | whoops, toothpicks, cinewhoops, 5″ true X / stretched / squashed / deadcat / H, long range 4–8″, X-Class 10/12/13″ |
| Presets | 22 | ready-made, painted drones for every class in the add-on's own *Propwash: Just More Parts* creative tab |

- **Survival:** every part has a recipe (bigger parts cost more) and turns up in chests from village houses to end
  cities; X-Class parts are uncommon or rare and only appear in late structures.
- **Own models in Propwash's style** with paint variants for every motor, prop (with damage stages), battery, stack,
  frame and video system – built like Propwash's own parts and just as big in your hand, the inventory, item frames and
  on the ground.
- **Flight School:** two new lessons, *Frame shapes compared* and *Sharing builds*, and the Flight School key (M)
  opens the matching lessons over every Just More Parts item, just like over Propwash's own parts.
- Fully translated into **English and German**.

## Share codes

A share code is one short word like `PW21RSaEPg8Rt` (a ready-made preset; with tune and name usually 50–75 characters) and carries any mix of **parts, paint, flight tune (with the drone's camera angle), OSD layout and name**. Older `PW1-…` codes still work.

- **At the Drone Workbench:** the *Share code* button opens one window: *Export* puts the finished build on your
  clipboard; *Import* shows the drone in 3D, checks every part, lists the flight figures and – in survival – a shopping
  list, then applies parts, paint, tune or OSD. In survival the workbench needs the parts and dyes; in creative they are
  created.
- **In the configurator** (`K`, tab *Share code*): share the drone in your hand, apply only the tune or only the paint,
  apply an OSD layout, or create the drone in creative mode.
- **By command:** `/jmp code export`, `/jmp code info <code>`, `/jmp code import <code>` and, for operators,
  `/jmp code give <code> [player]`.

Spaces, line breaks and lower case don't matter, typos are caught by a checksum, and parts from add-ons the server
doesn't have are reported as unknown instead of breaking the import.

## Compatibility

- Minecraft 26.3 · Fabric (Fabric API required) or NeoForge · Java 25
- Propwash FPV 0.4.0 or newer, on client **and** server
- Your Propwash worlds, drones and settings stay as they are. If you remove the add-on, drones keep its parts as
  *Unknown part* and are complete again when it comes back.
