# Changelog

What changed in **Propwash: Just More Parts**, for players and server owners. Technical details per release are in
[`docs/RELEASE-NOTES.md`](docs/RELEASE-NOTES.md), developer notes per day in [`changelogs/`](changelogs/) (German).

## [1.1.0] — Source-available, made for Propwash 0.5.0

### Highlights
- **Source code on GitHub:** Just More Parts is now source-available under the PolyForm Shield License, like Propwash – read the code, build it yourself, put it in modpacks and send pull requests.
- **Ready for Propwash 0.5.0:** built and tested against Propwash 0.5.0, and the whoop frames follow its corrected whoop data.

### New
- The documentation is MIT licensed; contributions are welcome as pull requests with a short contributor agreement.
- Exit clause: if Just More Parts ever goes 12 months without a release or maintainer activity, the code automatically becomes available under the MIT License.

### Improved
- Whoop and cinewhoop ducts look like real ones: thinner walls, a slim rim and round rings – and with Propwash 0.5.0 you see them in your FPV view.
- The camera of the ducted whoops and cinewhoops sits above the ducts like on real ones, so the ducts show in the lower corners of your FPV view instead of covering both sides; an action cam on a cinewhoop sits on top of it.
- The 65, 75 and 85 mm whoop frames are no longer floaty: their air resistance from above counts the frame and the prop blades instead of treating the ducts like solid plates. They drop faster when you lower the throttle, keep their momentum and reach realistic top speeds (Whoop 75 preset 41 → 59 km/h, Whoop 85 preset 57 → 79 km/h).
- The 5″ Race Frame (Slim) carries its battery under the frame like real race quads: a lower center of gravity, a little snappier on roll, and the drone lands on the pack. With Propwash 0.5.0 you also see it hanging there, strap and all.
- The 3.5″ props are tuned to the new climb physics of Propwash 0.5.0: they draw a little more power, so the 3.5″ toothpick and cinewhoop presets stay within realistic flight times.

### Fixed
- The share-code window and the *Share code* tab fit large GUI scales and small windows: a long code and the drone's values scroll with the mouse wheel instead of being cut off, and long lines in the tab wrap instead of sliding back and forth.
- On the ducted whoops and cinewhoops the props no longer stick out above the ducts, and the motors sit on their mounts instead of floating above them.
- Motors and other small parts are no longer huge in your hand: they are held at a size that fits their real size, like Propwash's own parts – a 1103 motor is a small part, a 4214 a bigger one, frames and props from 5″ up look as before.

### Compatibility
- Minecraft 26.3 · Fabric (Loader 0.19.5+, Fabric API 0.161.0+26.3) or NeoForge (26.3.0.10-beta+) · Java 25 · install it on the server and on every client that plays there.
- Works with Propwash FPV 0.5.0 and 0.4.0 (Parts API 1.4 or newer); drones, share codes and worlds load unchanged.
- With Propwash 0.5.0 you can keep Just More Parts installed and still join servers that don't have it: it just stays inactive there. A server with Just More Parts still needs it on every player.
- The new license applies from 1.1.0 on; Just More Parts 1.0.0 stays All Rights Reserved.

## [1.0.0] — first release

### Highlights
- **143 real-world parts for Propwash FPV:** motors, props, batteries, stacks, video systems and frames with real data – Propwash's own flight model turns them into flight behaviour.
- **New frame shapes:** stretched and squashed X, deadcat, H-frame, ducted whoops and cinewhoops, long range 4–8″ and X-Class 10, 12 and 13″.
- **Share codes:** copy a whole build with paint, tune, camera angle and OSD layout as one short word and rebuild it anywhere.

### New
- 34 motors from 0702 to 4214 with several KV per stator, and new 31xx and 42xx mounts for X-Class.
- 38 props from 31 mm to 13″ with 2 to 5 blades in soft and hard PC, bi-blend, glass- and carbon-fiber nylon and full carbon, plus ducted props.
- 30 batteries from 1S 250 mAh to 8S 5000 mAh: LiHV, LiPo, Li-Ion and the new semi-solid LiHV (light, but overloaded at full throttle).
- 12 stacks and AIOs from the 1S whoop AIO to a 30×30 8S stack with 100 A; the workbench reports overvoltage and ESCs that are too weak.
- 7 analog and digital video systems with their own latency, range, picture quality, power draw and camera field of view.
- 22 frames and 22 painted, ready-to-fly presets from the Whoop 75 to the 13″ H-frame X-Class, including a deadcat 7″ on 8S.
- Own creative tab *Propwash: Just More Parts* right after the Propwash tab, presets first.
- Every part has its own model in Propwash's style with a paint variant, and a Shift help with key figures, pros, cons and use.
- Part names follow Propwash's naming scheme, e.g. *6S 2200 mAh Battery* or *1404 Motor, 3800 KV*.
- The workbench, the share code window and `/jmp code info` show Propwash's four flight times.
- Every frame has a camera mount with its own range; set the drone's camera angle in Propwash's service menu.
- How much prop you see depends on the frame: stretched race frames show the most, deadcats and H-frames none.
- Survival recipes for every part, scaled by size, unlocked once you own a Propwash part.
- Chest loot from village houses to end cities; X-Class and semi-solid parts are uncommon or rare and only in late structures.
- *Share code* button at the workbench and *Share code* tab in the configurator, with preview, parts check, flight values and a survival shopping list.
- Share codes are much shorter: a ready-made preset is about 18 characters, a tuned build with name about 55–75, even with the OSD layout about 115 – one word without dashes, so a double-click copies all of it.
- Copy just the tune, the paint or the OSD layout onto the drone you hold, or create a drone from a code in creative mode.
- Command `/jmp code export|import|info|give` (also `/justmoreparts …`).
- Flight School lessons *Frame shapes compared* and *Sharing builds*; the `M` key opens the matching lessons over every part.
- Drones, frames and props hang flat on the wall in item frames and tilt towards you on shelves, like Propwash's own.

### Compatibility
- Minecraft 26.3 · Fabric (Loader 0.19.5+, Fabric API 0.161.0+26.3) or NeoForge (26.3.0.10-beta+) · Java 25 · required on client and server.
- Requires Propwash FPV 0.4.0 or newer (Parts API 1.4) on client and server.
- Removing the add-on keeps your drones: its parts show as *Unknown part* until you install it again.
- Share codes from earlier test builds (`PW1-…`) still import and the website configurator reads both; parts from other add-ons the server does not have are reported as unknown.
