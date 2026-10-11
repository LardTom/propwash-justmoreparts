Deutsche Version: [README.de.md](README.de.md)

# Propwash: Just More Parts

An add-on for **Propwash FPV** (Minecraft 26.3, Fabric and NeoForge): **143 more realistic drone parts**, from the
65 mm whoop through 2–3.5″ toothpicks and cinewhoops, 5″ freestyle and race, 6–8″ long range, up to the 13″ X-Class,
**new frame shapes** (Stretched-X, Squashed-X, Deadcat, H-Frame, Ducts, X-Class) and **share codes** that let you
share a build, paint job, flight tune and OSD layout as a short piece of text.

JMP brings no flight physics of its own: every part is a data set with real figures (weight, stator size and KV,
winding resistance, heat limits, thrust and power coefficients, stiffness, cell count, capacity, C-rating, internal
resistance, wheelbase, arm geometry, drag …), and **Propwash computes the flight behavior from that**. Strengths and
weaknesses fall out on their own: a two-blade prop flies longer, a five-blade prop draws more current, a Li-Ion
battery lasts forever and has no punch, a deadcat has (almost) no props in the picture. Every part's Shift tooltip
names its strengths and weaknesses — and every one of those statements is backed by Propwash's own analysis on the
test bench.

Version **1.1.0** · Author: lardtom · License: see [License](#license).

## Requirements
- Minecraft 26.3, Java 25
- **Propwash FPV 0.4.0 or newer** (Parts API 1.4) — required on client **and** server
- Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 **or** NeoForge 26.3.0.10-beta

JMP is its own mod with its own mod ID (`justmoreparts`) and only uses Propwash's public API. Propwash's worlds,
drones and settings stay unchanged; without JMP, Propwash shows missing parts as placeholders.

## Contents

| Category | Count | Range |
|---|---|---|
| Motors | 34 | 0702 to 4214, several KV ratings per stator (27000 KV to 330 KV), new 31xx and 42xx mounts |
| Propellers | 38 | 31 mm to 13″, 2–5 blades, PC, soft/hard PC, bi-blend, glass-/carbon-fiber nylon, solid carbon, duct props |
| Batteries | 30 | 1S 250 mAh to 8S 5000 mAh: LiHV, LiPo, Li-Ion (18650/21700, also 6S2P), semi-solid |
| Cell chemistry | 1 | Semi-solid LiHV (up to 4.45 V per cell): light, but only 20–25C |
| Stacks/AIOs | 12 | 1S whoop AIO up to a 30×30 8S stack with 100 A |
| Video systems | 7 | analog 25 mW to 1.6 W, digital from micro to long range, with each camera's field of view (110–133°) |
| Frames | 22 | whoops 65–85 mm, toothpicks 2–3.5″, cinewhoops 2.5/3.5″, 5″ True-X/Squashed/Stretched/Deadcat/H-Frame, long range 4–8″, X-Class 10/12/13″ and 13″ H |
| Presets | 22 | ready-made, painted drones for every class, in their own creative tab |

Also:
- **Survival:** recipes for every part (cost grows with size) and chest loot from village houses to end cities;
  X-Class parts are uncommon or rare and only appear in late-game structures.
- **Its own models in Propwash's style** with paint variants for motors, propellers (with damage stages), batteries,
  stacks, frames and video systems, built from the same building blocks as Propwash's own parts and shown the same
  way in hand, in the inventory, on the frame and on the ground; the X-Class frames also get a true full-size model
  on the drone.
- **Flight School scenes:** "Frame shapes compared" (five 5″ shapes with the same parts) and "Sharing builds". The
  Flight School key (`M`) held over a JMP part opens its lessons just like Propwash's own parts: workbench and
  crafting for every part, plus, depending on the kind, frame shapes, damage and swapping props, batteries and the
  charger, the configurator or video systems; presets additionally get "Sharing builds".
- **German and English** for every name, tooltip and window; part names follow the same scheme as Propwash's own
  parts (`6S Battery 2200 mAh`, `1404 Motor, 3800 KV`, `40 mm Propeller (3 blades, 1.7″ pitch, PC)`, rules in
  [`docs/design/part-naming.md`](docs/design/part-naming.md)).

## Frame shapes in brief
Propwash works out the differences from each frame's arm geometry, weight, camera position and drag:
- **True-X:** all arms the same length — balanced in roll and pitch.
- **Stretched-X:** motors further forward/back than to the sides — more roll authority, slower pitch; slim and fast
  in a straight line (race). The front props pass close to the camera: more props in the picture than on a True-X.
- **Squashed-X:** motors further to the sides — more pitch authority, snappier flips, fewer props in the picture, but
  slower rolls.
- **Deadcat:** front arms splayed out — no props in the camera picture (the 7″ long range shows only the prop tips at
  the edges of the picture with 130° goggles, around 2%), but somewhat heavier, and roll and pitch respond
  differently.
- **H-Frame:** camera far forward — no props in the picture, snappier flips; heavy, lots of drag, slower rolls.
- **Ducts (whoop/cinewhoop):** guard rings around the props — they bounce off instead of breaking and can carry an
  action cam, but have a lot of drag.
- **X-Class 10–13″:** 460 to 820 mm wheelbase (the official race size starts at 800 mm), 3115 and 4214 motors, 6–8S —
  lots of mass and inertia, big props, long flights.

## Share codes: how it works
A share code is a short piece of text like `PW21RSaEPg8Rt` (a finished preset), usually 50–75 characters with a tune
and a name. It can hold any combination of a drone's **parts, paint job, flight tune, OSD layout and name**. Spaces
and line breaks don't matter when pasting it in, capitalization does, and the checksum catches corruption. Older
`PW1-…` codes from test versions keep working.

**At the workbench (parts, paint, tune, OSD)**
1. Build the drone at the Propwash workbench.
2. The **"Share Code"** button at the top of the workbench (shown as the `</>` icon in German so the title stays
   readable), **"Export"** tab: check the boxes for paint, tune, OSD and name, then **"Copy to clipboard"** — the
   code can go in chat, Discord or a forum post.
3. To rebuild it, pick the **"Import"** tab in the same window and paste the code: the window shows the drone in 3D,
   checks every part, gives flight figures and — in survival — a shopping list of the missing parts. Pick the layers
   (parts, paint, tune, OSD) and click **"Apply"**. In survival the workbench needs the parts (and dye for the paint
   job); in creative they appear on their own.

**In the configurator (key `K`, "Share Code" tab)**
- *Share the drone in hand:* pick the layers, copy the code.
- *Apply a code:* apply only the **tune** or only the **paint job** to the drone in hand, or apply the **OSD
  layout**; in creative, **"Spawn drone"**, in survival the shopping list.

**Via command** (also as `/justmoreparts …`)
- `/jmp code export [layers]` — the code for the drone in your main hand, as a clickable chat line (click to copy),
  e.g. `/jmp code export parts,paint,tune`.
- `/jmp code info <code>` — shows layers, parts, unknown parts and problems without changing anything.
- `/jmp code import <code>` — creative: a new drone into your inventory; survival: tune and paint onto the drone in
  hand.
- `/jmp code give <code> [player]` — operators only: spawns the code's drone into an inventory.

Codes stay valid: JMP's part IDs are frozen; parts from other add-ons that are missing get reported by the import as
"unknown" instead of failing. The format is documented in `docs/design/sharecode-pw2.md` (PW2, with measured
lengths) and `docs/design/sharecode.md` (PW1, still read for backward compatibility), both with test vectors.

## Developing
1. In the Propwash repository, once: `./gradlew publishToMavenLocal` (Propwash 0.5.0).
2. Here, once: `JAVA_HOME=<jdk25> ./gradlew writeClasspath` (for `tools/fastcheck.sh`), then `./gradlew build`.
3. Quick checks without Gradle: `tools/fastcheck.sh all`, `tools/fastcheck.sh test` (135 JUnit tests, including the
   test benches for every category).
4. Play: `./gradlew :fabric:runClient` or `:neoforge:runClient` (Propwash loads alongside it, sound muted).
5. Self-test: `./gradlew :fabric:runClientSelftest` (with `-PjmpScreenshots` it also writes screenshots to
   `docs/screenshots/`, including the Propwash-vs-JMP comparison images in `docs/screenshots/compare/`), smoke tests
   `runClientSmoke`, `runServerSmoke`; web export: `./gradlew webExport` → `build/web-export/`.
6. Change parts: JSON under `common/src/main/resources/data/justmoreparts/propwash_parts/`, then
   `python3 tools/parts_sync.py --write` and `--check`; language strings in `common/src/lang/<module>/`.
7. Change models: templates in `tools/assets/jmpstyle/`, then `python3 tools/assets/style_generate.py --write`
   (needs numpy and Pillow; rules in `docs/design/asset-style.md`), compare with `tools/assets/compare_render.py`.

## Documentation
- `CHANGELOG.md` — changes per version, short and for players (English)
- `docs/RELEASE-NOTES.md` — details per version: test-bench numbers, naming examples, notes
- `docs/ARCHITECTURE.md` — structure, lifecycle, API wishes for Propwash
- `docs/CONVENTIONS.md` — rules for code, data, language, Git
- `docs/design/parts-catalog.md` — every part with its figures, pros/cons and test-bench values
- `docs/design/asset-style.md` — style rules for the models (Propwash building blocks, textures, display)
- `docs/design/sharecode-pw2.md` — share code format PW2 (current)
- `docs/design/sharecode.md` — share code format PW1 (still read for compatibility)
- `docs/design/web-export.md` — data package for the web configurator
- `docs/modrinth/` — Modrinth description (DE/EN) and listing

## Contributing
Pull requests are welcome. Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) first; every pull request needs the
short contributor agreement [`CLA.md`](CLA.md) (a checkbox in the pull request template).

## License
From version 1.1.0 on, the source code of Propwash: Just More Parts is published under the
[PolyForm Shield License 1.0.0](https://polyformproject.org/licenses/shield/1.0.0), the same license Sodium and
Propwash FPV use. In plain words:

- **Allowed:** reading and inspecting the code, building it yourself, including JMP in modpacks, and contributing
  through pull requests (with a short contributor agreement).
- **Not allowed:** re-uploading JMP, publishing forks as alternative downloads, and building competing mods from the
  codebase.
- **MIT:** the documentation is under the MIT License; [`LICENSE-MIT`](LICENSE-MIT) lists the exact files.
- **Exit clause:** if there are no new releases and no maintainer activity for twelve months, the code automatically
  also becomes available under the MIT License.
- Version 1.0.0 and older stay "All Rights Reserved".

The license texts are binding: [`LICENSE`](LICENSE), [`LICENSE-MIT`](LICENSE-MIT) and [`NOTICE`](NOTICE).

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
