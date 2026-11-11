Deutsche Version: [README.de.md](README.de.md)

<p align="center"><img src=".github/listing/banner.png" alt="Propwash: Just More Parts"></p>

Version **1.2.0** · Author: lardtom · License: see [License](#license).

**146 more real-world drone parts for Propwash FPV.** From 65 mm whoops over toothpicks and cinewhoops, 5″ freestyle
and race quads and 6 to 8″ long range up to 13″ X-Class: motors, props, batteries, stacks, video systems and frames in
new shapes. On top come 23 ready-to-fly presets.

<p align="center"><img src=".github/listing/gif-presets-lineup.gif" alt="Just More Parts presets: from Race Whoop 65 to X-Class 13″"></p>

<p align="center"><a href="https://modrinth.com/mod/propwash-fpv"><img src=".github/listing/card-requires-propwash.png" alt="Requires Propwash FPV 0.6.0"></a></p>

<p align="center"><img src=".github/listing/header-server.png" alt="Public server"></p>

<p align="center"><img src=".github/listing/server-banner.png" alt="Public server mc.propwashfpv.com: join with Fabric or NeoForge"></p>

Play Propwash with others on our public server **`mc.propwashfpv.com`**. Since Propwash 0.6.0 you can join with
**Fabric or NeoForge**. Add the address to your server list; if the server needs a mod you don't have, the join screen
names it.

<p align="center"><img src=".github/listing/header-new.png" alt="New in 1.2.0"></p>

- **Three 1S race-whoop motors.** 0702 with 30000 and 35000 KV for 65 mm whoops, 0802 with 27200 KV for 75 mm whoops, each based on data sheets from at least three makers. Hold full throttle too long and they get hot and lose power.
- **New preset: Race Whoop 65 · 1S.** Built around the 35000 KV motor: very snappy, short flights, and it wants a battery with a high C rating.
- **Clean battery mounts.** With the battery in, all 22 frames show no plugs or cables.
- **Share codes from Propwash.** The add-on's own share window is gone, because Propwash's PW3 codes cover every part.
- **Motor heat.** Cooling is tuned to the new motor heat of Propwash 0.6.0. Every frame takes the accessories of its Propwash counterpart, with a front mount on 18 frames.

<p align="center"><img src=".github/listing/gif-fpv-race-whoop.gif" alt="New preset Race Whoop 65 · 1S in FPV through an indoor course"></p>

<p align="center"><img src=".github/listing/header-physics.png" alt="Honest parts, real physics"></p>

Just More Parts fakes nothing. Every part is a data sheet with real-world numbers (weight, stator size and KV, winding
resistance, heat limits, thrust and power coefficients, cell count, capacity, C rating, internal resistance, wheelbase,
arm layout, drag), and **Propwash's own flight model turns them into flight behaviour**. The trade-offs appear by
themselves:

- a two-blade prop flies longer, a five-blade prop pulls harder and draws more current
- a Li-Ion pack hovers for half an hour and has no punch; a semi-solid pack is light but overloaded at full throttle
- a 4S stack on 6S burns out when you arm
- a deadcat or H-frame keeps the props out of your camera view; a stretched X rolls faster and flips slower
- an X-Class quad is heavy and slow to turn, and it flies for a long time

Hold **Shift** over any part: the tooltip lists its key figures, pros, cons and what it is good for.

<p align="center"><img src=".github/listing/gif-workbench-build.gif" alt="An X-Class 13″ comes together at the Drone Workbench"></p>

<p align="center"><img src=".github/listing/header-inside.png" alt="What&#x27;s inside"></p>

| | Count | Range |
|---|---|---|
| Motors | 37 | 0702 to 4214, 35000 KV down to 330 KV, 1S race-whoop motors |
| Props | 38 | 31 mm to 13″, 2 to 5 blades, PC, bi-blend, nylon, carbon, ducted |
| Batteries | 30 | 1S 250 mAh to 8S 5000 mAh: LiHV, LiPo, Li-Ion, semi-solid LiHV |
| Cell chemistry | 1 | semi-solid LiHV (up to 4.45 V per cell) |
| Stacks / AIOs | 12 | 1S whoop AIO to 30×30 8S 100 A |
| Video systems | 7 | analog 25 mW to 1.6 W, digital micro to long range |
| Frames | 22 | whoops, toothpicks, cinewhoops, 5″ in five shapes, long range 4 to 8″, X-Class 10/12/13″ |
| Presets | 23 | painted, ready to fly, in their own creative tab |

<p align="center"><img src=".github/listing/header-frames.png" alt="New frame shapes"></p>

<p align="center"><img src=".github/listing/gif-size-classes.gif" alt="Size classes side by side: 75 mm whoop to 13″ X-Class"></p>

- **Five shapes, same parts.** True X, stretched X, squashed X, deadcat and H-frame, flown with a mixer for the real arm layout. The Flight School lesson *Frame shapes compared* shows the difference.
- **X-Class up to 13″.** Heavy, slow to turn and long-flying. X-Class parts are uncommon or rare and only appear in late structures.
- **Ducts like real ones.** Whoops and cinewhoops have thin walls and round rings, and you see them in the lower corners of your FPV view.

<p align="center"><img src=".github/listing/header-share.png" alt="Share codes"></p>

Share codes come from Propwash 0.6.0, and its **PW3** codes know every Just More Parts part. A code carries any mix of
parts, paint, flight tune, OSD layout and name.

- **One place for all codes:** the *Share code* button at the Drone Workbench, the *Share code* tab (`K`) or `/propwash code`.
- **Codes name their add-ons:** without Just More Parts, paint, tune and OSD still apply, and every part has a Propwash stand-in.
- **Click in chat:** hover a code another pilot posted to see the drone, click to open it in the import.
- **Old codes keep working:** PW1 and PW2 codes still load and convert to PW3 with one click.

<p align="center"><img src=".github/listing/header-survival.png" alt="Survival"></p>

- **Recipes and loot.** Every part has a recipe (bigger parts cost more) and turns up in chests from village houses to end cities.
- **Flight School.** The Flight School key (`M`) opens the matching lessons over every Just More Parts item, just like over Propwash's own parts.

<p align="center"><img src=".github/listing/header-get-started.png" alt="Get started"></p>

1. Put **Propwash FPV 0.6.0** and Just More Parts **1.2.0** into the same `mods` folder (Fabric or NeoForge), on the
   client and on the server.
2. Grab a preset from the add-on's creative tab, or craft the parts in survival.
3. Build at Propwash's **Drone Workbench**. The new parts sit next to Propwash's own, and the live analysis shows
   weight, thrust and flight time as you go.
4. Bind it, put on your goggles and fly. Hold **Shift** over a part for its figures, or **M** for its lesson.

<p align="center"><img src=".github/listing/header-compatibility.png" alt="Compatibility"></p>

- Minecraft 26.3 · Fabric (Fabric API required) or NeoForge · Java 25
- **Propwash FPV 0.6.0 or newer** ([download](https://modrinth.com/mod/propwash-fpv)), on the client **and** the server. Propwash 0.6.0 won't start with Just More Parts 1.1.x, so update both together.
- Install it on the server and on every client, in the same version. On a server without it, the add-on simply stays off.
- Your worlds, drones and settings stay as they are. Remove the add-on and drones keep its parts as *Unknown part* until it comes back.
- English and German

<p align="center"><img src=".github/listing/header-community.png" alt="Community"></p>

<p align="center">

[![Discord](.github/listing/button-discord.png)](https://discord.gg/bNhkSXYSgZ) [![Website](.github/listing/button-website.png)](https://propwashfpv.com) [![Support on Ko-fi](.github/listing/button-kofi.png)](https://ko-fi.com/propwashfpv)

</p>

Questions, builds, a part you miss? Join the Discord or read the docs on the website. Propwash and its add-ons are
free; if you enjoy them, you can support the project on [Ko-fi](https://ko-fi.com/propwashfpv). Thank you!

## Developing
1. In the Propwash repository, once: `./gradlew publishToMavenLocal` (the version in `propwash_version` in
   `gradle.properties`).
2. Here, once: `JAVA_HOME=<jdk25> ./gradlew writeClasspath` (for `tools/fastcheck.sh`), then `./gradlew build` (also
   runs `checkShareTable`, Propwash's check of the parts table against `common/sharecode-baseline.json`).
3. Quick checks without Gradle: `tools/fastcheck.sh all`, `tools/fastcheck.sh test` (all JUnit tests, including the
   test benches for every category).
4. Play: `./gradlew :fabric:runClient` or `:neoforge:runClient` (Propwash loads alongside it, sound muted).
5. Self-test: `./gradlew :fabric:runClientSelftest` (with `-PjmpScreenshots` it also writes screenshots to
   `docs/screenshots/`, including the Propwash-vs-JMP comparison images in `docs/screenshots/compare/`), smoke tests
   `runClientSmoke`, `runServerSmoke`; web export: `./gradlew webExport` writes `build/web-export/` (runs Propwash's
   `/propwash webexport all` on the dev server).
6. Change parts: JSON under `common/src/main/resources/data/justmoreparts/propwash_parts/`, then
   `python3 tools/parts_sync.py --write` and `--check`; language strings in `common/src/lang/<module>/`. A new part
   also needs a line in the parts table for share codes (`docs/design/share-table.md`).
7. Change models: templates in `tools/assets/jmpstyle/`, then `python3 tools/assets/style_generate.py --write`
   (needs numpy and Pillow; rules in `docs/design/asset-style.md`), compare with `tools/assets/compare_render.py`.
   After changing a stack model, `python3 tools/connector_models.py` writes its variants for a plugged-in battery
   (`--check` only checks; rules in `docs/battery-plug.md`).

## Documentation
- `CHANGELOG.md`: changes per version, short and for players (English)
- `docs/RELEASE-NOTES.md`: details per version: test-bench numbers, naming examples, notes
- `docs/ARCHITECTURE.md`: structure, lifecycle, API wishes for Propwash
- `docs/CONVENTIONS.md`: rules for code, data, language, Git
- `docs/design/parts-catalog.md`: every part with its figures, pros/cons and test-bench values
- `docs/design/asset-style.md`: style rules for the models (Propwash building blocks, textures, display)
- `docs/design/share-table.md`: JMP's parts table for Propwash's PW3 share codes
- `docs/design/sharecode-pw2.md`, `docs/design/sharecode.md`: the old JMP share code formats PW2 and PW1 (reference;
  Propwash still reads them)
- `docs/design/web-export.md`: the old web export format 1 (reference; the web export now comes from Propwash)
- `docs/battery-plug.md`: stack variants for a plugged-in battery
- `docs/modrinth/`: Modrinth description (DE/EN) and listing

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
