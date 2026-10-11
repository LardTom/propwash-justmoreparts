# Web-Export — Datenpaket für den Web-Konfigurator

| | |
|---|---|
| Format | `web-export` **1** (Feld `format` in `manifest.json`) |
| Besitzer | Implementierer 5 (SHARECODE + UI + BEFEHL + WEB-EXPORT) |
| Erzeugung | `./gradlew :fabric:runWebExport` (oder `:neoforge:runWebExport`): dedizierter Dev-Server mit Propwash + JMP, schreibt nach `build/web-export/`, beendet sich, Ergebnis `PASS …` in `fabric/runs/web-export/justmoreparts-selftest-result.txt` |
| Code | Quell-Set `devtest`, Paket `de.lardtom.justmoreparts.export` (`WebExport`, `PartData`, `RuleCheck`, `AssetIndex`, `LangTable`, `ExportJson`, `Problems`), Einstieg `dev.WebExportRun` (Property `justmoreparts.webexport=<zielordner>`); der Ordner wird vor dem Schreiben von allen Ausgaben aus Abschnitt 2 geleert |
| Quelle der Daten | nur öffentliche Propwash-API (`PropwashApi.parts()`, `builds()`, `partItems()`) plus Ressourcen-Dateien der Mods (Sprachdateien, Modelle, Texturen) über `Services.PLATFORM.readModResources` |

Der Web-Konfigurator (eigenes Projekt, nicht in diesem Repository) soll Drohnen aus **allen** registrierten Teilen —
eingebaute Propwash-Teile und JMP-Teile — zusammenstellen, Kompatibilität prüfen, Kennzahlen zeigen, lackieren und
Sharecodes (PW1, `docs/design/sharecode.md`) lesen und schreiben. Dieses Dokument legt das Datenpaket fest, das er
dafür bekommt.

---

## 1. Grundregeln

- UTF-8, JSON mit 2 Leerzeichen Einzug, Zeilenende `\n`, Schlüssel in `snake_case`.
- **Deterministisch**: gleiche Mods → byte-gleiche Dateien (feste Sortierung, kein Zeitstempel außer in
  `manifest.json#generated_at`).
- Sortierung der Teile: Kategorie in der Reihenfolge `frame, stack, motor, prop, video, battery, accessory`, dann
  `sort`, dann Id.
- Zahlen als JSON-Zahlen mit der Genauigkeit der Definition (keine Rundung); `NaN`/unendlich → `null`.
- Ids immer voll (`propwash:m2207_1750`, `justmoreparts:m2207_1950`).
- Texte immer in beiden Sprachen `de_de` und `en_us` als Objekt `{"de_de": "…", "en_us": "…"}`; fehlt ein Schlüssel,
  steht dort `null` und die Datei `problems.json` listet ihn.
- **Neue Felder** dürfen in `format` 1 dazukommen (der Web-Konfigurator ignoriert unbekannte Felder). Umbenennen oder
  Entfernen → `format` 2.

---

## 2. Dateien

```
build/web-export/
  manifest.json
  parts.json
  mounts.json
  chemistries.json
  rules.json
  paint.json
  tune.json
  presets.json
  analysis/<preset-namensraum>/<preset-pfad>.json
  analysis_labels.json               (ab 1.0.4, §2.9)
  sharecode/spec.json
  sharecode/vectors.json
  lang/de_de.json
  lang/en_us.json
  assets/<namensraum>/…               (Modelle, Render-Definitionen, Texturen, drone_layout.json, drone_paint.json)
  problems.json
```

### 2.1 `manifest.json`

```json
{
  "format": 1,
  "generated_at": "2026-09-29T21:00:00Z",
  "generator": "justmoreparts 0.1.0",
  "minecraft": "26.3",
  "mods": {"propwash": "0.4.0", "justmoreparts": "0.1.0"},
  "propwash_api": "1.0.0",
  "sharecode": {"prefix": "PW1-", "revision": "1.0"},
  "languages": ["de_de", "en_us"],
  "counts": {"frame": 27, "stack": 15, "motor": 40, "prop": 43, "video": 11, "battery": 35, "accessory": 3, "chemistry": 4, "preset": 26, "asset_files": 449, "vectors": 58},
  "fingerprint": "<PropwashApi.parts().fingerprint()>",
  "files": ["analysis/justmoreparts/deadcat5_6s.json", "chemistries.json", "…"]
}
```

`files` nennt alle geschriebenen JSON-Dateien außer denen unter `assets/` (sortiert); `counts.asset_files` zählt die
kopierten Asset-Dateien, `counts.vectors` die Einträge in `sharecode/vectors.json`.

### 2.2 `parts.json`

Liste aller registrierten Teile (`PropwashApi.parts().all()`), sortiert nach Abschnitt 1 (Kategorie, `sort`, Id). Nicht exportiert
wird der Kreativ-Flugcontroller `propwash:creative_fc` aus Propwash 0.5.0 (`WebExport.CREATIVE_ONLY`): Er passt in jeden
Rahmen, was keine Formel aus `rules.json` abbilden kann, und gehört zum Drohnen-Editor, den der Web-Konfigurator nicht hat;
ein Sharecode mit ihm zeigt im Browser „unbekanntes Teil“. Die Zufallsbauten der Regelprüfung lassen ihn ebenso aus:

```json
{
  "id": "justmoreparts:m2207_1950",
  "category": "motor",
  "builtin": false,
  "name": {"de_de": "2207-Motor, 1950 KV", "en_us": "2207 Motor, 1950 KV"},
  "short_name": null,
  "help": {
    "de_de": {"summary": "2207-Motor, 1950 KV – 5″-Freestyle auf 6S", "what": ["2207 · 1950 KV · 32,8 g · 50 A", "+ viel Punch, sauberer Propwash", "− mehr Strom, Motoren wärmer"], "use": ["Passt zu: 5″-Rahmen, 6S 1100–1500"]},
    "en_us": {"summary": "…", "what": ["…"], "use": ["…"]}
  },
  "basics": {"mass_g": 32.8, "stack_size": 16, "sort": 116, "creative": true, "durability": 1.0, "recycle": {"item": "propwash:copper_coil", "count": 1}, "accent": "#e8b622", "render_model": null},
  "data": {"stator": "2207", "stator_mm": [22.0, 7.0], "kv": 1950, "mount": "propwash:m22", "max_current_a": 50.0, "resistance_ohm": 0.0443, "cells": [5, 6], "prop_mount": "propwash:universal",
           "thermal": {"heat_capacity_j_per_k": null, "cooling_w_per_k": null, "cooling_w_per_k_per_rad_s": null, "derate_start_c": 130.0, "max_temp_c": 150.0, "wear_per_s_at_max": 0.002}},
  "derived": {"no_load_rpm_per_cell_v": 1950, "max_power_w_at_nominal": 1110.0},
  "paint": {"slots": ["motors"], "paintable_model": false},
  "model": {"kind": "procedural", "item": "assets/justmoreparts/items/m2207_1950.json", "drone": null, "variants": [], "textures": []}
}
```

- `short_name`: nur Rahmen (`propwash.content.frame_short.<pfad>` bzw. `<ns>.frame_short.<pfad>`), sonst `null`; `help` je
  Sprache mit `summary` (`null`, wenn der Schlüssel fehlt) und den vorhandenen Zeilen `what.1…8`/`use.1…8`
  (eingebaute Teile: `propwash.help.<pfad>`, fremde: `<ns>.help.<pfad>`).
- `basics.recycle`: `{"item", "count"}`, `"none"` (kein Ertrag) oder `null` (Kategorie-Standard von Propwash);
  `basics.accent` `#rrggbb` oder `null`, `basics.render_model` Id oder `null`.
- `data`: **alle** Felder der Kategorie mit denselben Namen wie das JSON-Schema von Propwash (`parts-api-data.md`),
  aufgelöst (Standards eingesetzt; leere Optionen als `null`). Rahmen enthalten `arms` immer (bei True-X ohne eigene Arme
  die vier Positionen aus `MotorArm.trueX(radstand, 0)` und das Feld `arms_explicit: false`), `duct` als Objekt oder
  `null`, `battery_fit`, `accessory_anchors`, `camera` (Position `null`, wenn nicht gesetzt; `tilt_deg` Standard-Uptilt,
  `tilt_min_deg`/`tilt_max_deg` Bereich des Kamerahalters aus `PropwashApi.builds().uptiltRange(rahmen)`, API 1.4).
  Videoteile tragen `camera_fov_deg` (horizontal, das Sichtfeld des FPV-Bilds; `null` = Standard des Systems).
  Bis zum API-Wunsch W6 (öffentlicher JSON-Codec) schreibt der Export `data` selbst aus den Record-Zugriffen.
- `derived` (nur Anzeige): Motor `no_load_rpm_per_cell_v` (= KV), `max_power_w_at_nominal` (= `max_current_a` × höchste
  Zellzahl × 3,7 V); Prop `diameter_in` (mm / 25,4), `disc_area_m2` (π · (d/2000)²), `pitch_to_diameter`
  (`pitch_in` / `diameter_in`); Akku `energy_wh` (Zellen × Nennspannung der Chemie × Ah), `max_continuous_a`
  (`maxContinuousCurrent()`), `wh_per_kg` (`energy_wh` / kg); Stack `max_power_w` (= `esc_continuous_a` × 4 × höchste
  Zellzahl × 3,7 V); Rahmen `size_class_in` (`nominalPropInches()`), `ducted`, `shape_extent_mm` (größere Seite des
  Rechtecks um alle vier Prop-Kreise mit Radius `props_mm[1]` / 2 an den Armpositionen). Video und Zubehör: `{}`.
- `paint.slots`: Lack-Plätze (`PaintSpec.SLOTS`), die dieses Teil färbt: Rahmen `frame`, `tpu`; Motor `motors`; Prop
  `prop_fl`, `prop_fr`, `prop_rl`, `prop_rr`; Stack `stack`; Akku `battery`; Video `camera`, `antenna`; Zubehör
  `accessories`. `paintable_model`: gibt es eine `_paint`-Variante mit Eintrag in einer `drone_paint.json`.
- `model.item`: Inventar-Definition `assets/<ns>/items/<pfad>.json` (fehlt sie → `null` und Warnung), `model.drone`:
  Render-Definition `assets/<ns>/items/drone_render/<kategorie>/<pfad>.json` bzw. die aus `render_model`, `model.variants`:
  vorhandene Varianten derselben Render-Definition (`_ccw`, `_blur`, `_worn`, `_damaged`, `_broken`, `_under` für den
  Akku unter dem Rahmen, `_paint` und ihre Kombinationen), `model.textures`: alle über Modelle und Eltern erreichten Texturen (Pfade im Export).
- `model.kind`: `model` (eigene Dateien, alle unter `assets/` mitkopiert) oder `procedural` (Propwash zeichnet aus
  `data`; der Web-Konfigurator braucht dann eine eigene prozedurale Darstellung nach `docs/ASSETS.md` „Prozedurale
  Formen“ im Propwash-Repository).
- Render-Definitionen sind Vanilla-Item-Definitionen und dürfen `transformation` tragen (`translation`, `left_rotation`,
  `scale`, `right_rotation`, angewendet im Modellraum 0…1 vor dem Zentrieren um −0,5). JMP nutzt das seit der
  Stil-Review 1.0.1 für die X-Class-Rahmen: das Modell ist um den Anbaupunkt verkleinert, damit es in −16…32 Einheiten
  passt, `scale` k und `translation` 0,5 − 0,5·k bringen es auf echte Größe (Rahmen und `_paint`). Der
  Web-Konfigurator muss die Transformation anwenden, sonst erscheint der Rahmen um den Faktor 1/k zu klein; die
  X-Class-Rahmen haben damit `model.kind` `model` statt `procedural`.

### 2.3 `mounts.json`, `chemistries.json`

```json
{"motor": [{"id": "propwash:m22", "name": {"de_de": "…", "en_us": "…"}, "frames": ["propwash:freestyle_5", "…"], "parts": ["propwash:m2207_1750", "…"]}],
 "stack": [ … ], "prop": [ … ]}
```

Die Prop-Aufnahmen stehen unter `prop` mit `motors` (Motoren mit dieser `prop_mount`) statt `frames`; die Universal-Aufnahme
`propwash:universal` bekommt, weil Propwash keinen Text dafür hat, den JMP-Text `justmoreparts.share.mount.prop.universal`.

`chemistries.json`: je Chemie alle Felder (`full_v`, `nominal_v`, `empty_v`, `cutoff_v`, `storage_v`, `density_g_cm3`,
`ocv_shape`, `soc_grid` = `CellChemistry.SOC_GRID`, ab 1.0.4 `usable_fraction` = wirksamer nutzbarer Anteil und
`landing_v` = Landespannung unter Last je Zelle), Name de/en, benutzt von (Akku-Ids).

### 2.4 `rules.json` — Kompatibilität

Die Regeln von Propwash 0.4.0 (`BuildSpec` → `BuildCheck`), als Daten und als Formel, damit der Web-Konfigurator sie ohne
Spiel auswerten kann. Jede Regel: `key` (wie `BuildCheck`), `kind` (`problem` = nicht baubar, `warning`), `text` de/en
(aus `propwash.build.issue.<key>`), `formula` (Textform unten, verbindlich), `order` (Anzeige-Reihenfolge wie Propwash).

| key | kind | Formel (Felder aus `parts.json#data`) |
|---|---|---|
| `missing_part` | problem | eine Id ist nicht in `parts.json` |
| `motor_mount` | problem | `motor.mount ∉ frame.motor_mounts` |
| `prop_too_large` | problem | `prop.diameter_mm > frame.props_mm[1]` |
| `prop_too_small` | problem | `prop.diameter_mm < frame.props_mm[0]` |
| `prop_mount` | problem | nicht (`motor.prop_mount = propwash:universal` oder `prop.mount = propwash:universal` oder `motor.prop_mount = prop.mount`) |
| `stack_mount` | problem | `stack.mount ∉ frame.stack_mounts` |
| `battery_frame` | problem | `battery.cells < fit.cells[0]` oder `> fit.cells[1]` oder `battery.mass_g > fit.max_mass_g` |
| `accessory_mount` | problem | ein Zubehör-Platz ∉ `frame.accessory_mounts` (und Zubehör nur im eigenen `slot`) |
| `motor_overvoltage` | warning | `battery.cells > motor.cells[1]` |
| `motor_undervoltage` | warning | `battery.cells < motor.cells[0]` |
| `stack_voltage` | warning | `battery.cells ∉ [stack.cells[0], stack.cells[1]]` |
| `esc_undersized` | warning | `motor.max_current_a > stack.esc_continuous_a × 1,15` |
| `battery_weak` | warning | `4 × motor.max_current_a > battery.c_rating × battery.capacity_mah / 1000 × 1,3` |
| `battery_undersized` | warning | Akku erlaubt und (`battery.cells < fit.preferred_min_cells` oder `battery.capacity_mah < fit.preferred_min_capacity_mah`) |

Aufbau: `{"rules": [...], "gates": [...], "constants": {...}, "analysis_warnings": [...], "verified": {...}}`.
`gates` sind die Vorbedingungen, die Propwash vor allen Regeln prüft: Eine Id fehlt oder gehört zu einer anderen Kategorie
als ihr Feld → nur `missing_part` (Propwash selbst meldet sie in `BuildCheck#unknownParts` und prüft sonst nichts);
Zubehör steckt in einem fremden Platz → nur das Problem `accessory_mount`, keine Warnung. `constants`: `esc_margin` 1,15,
`battery_margin` 1,3, `motor_count` 4, `universal_prop_mount`. `verified`: Anzahl, Startwert und Abweichungen der
Zufallsprüfung. Die Reihenfolge von `problems`/`warnings` ist die von `order` (so liefert sie auch Propwash).

Zusätzlich `analysis_warnings`: die Schlüssel aus `BuildStats#warnings` (`cannot_hover`, `underpowered`,
`sluggish_motors`, `heavy_sag`, `esc_current_limited`, `battery_overload`, `motor_thermal`, `props_in_view`,
`esc_overvoltage`) mit Text de/en (`BuildStats.warningTranslationKey`) — sie entstehen nur in der Analyse und stehen deshalb
nur in vorberechneten Ergebnissen (2.8, 2.9).

**Prüfung beim Export (Pflicht):** Der Export baut 5000 zufällige Baupläne (fester Startwert 1, jede Kategorie gleich
verteilt, 0–2 Zubehörteile) und vergleicht für jeden die Formeln mit `PropwashApi.builds().check(spec)`. Jede Abweichung
→ Eintrag in `problems.json` und Ergebnis `FAIL`. So bleibt `rules.json` bei jeder Propwash-Version wahr.

### 2.5 `paint.json`

```json
{"slots": [{"key": "frame", "bit": 0, "name": {"de_de": "Rahmen", "en_us": "Frame"}, "hint": {…}, "categories": ["frame"]}, …],
 "default_colors": {"<teil-id>": {"<slot>": "#rrggbb"}}}
```

`bit` = Bit in der PAINT-Maske von PW1. `default_colors`: Originalfarben aus `drone_paint.json` (`base`, erste Ebene je
Kanal; Kanäle `frame`, `tpu`, `motor` → `motors`, `prop` → alle vier Prop-Plätze, `stack`, `battery`, `camera`, `antenna`,
`accessory` → `accessories`) bzw. der Akzentfarbe des Teils, falls prozedural; Teile ohne beides fehlen.

### 2.6 `tune.json`

```json
{"groups": {"flight": ["rates", "pid", "feedforward", "tpa", "filters", "throttle"],
            "shared": ["rates", "pid", "feedforward", "tpa", "filters", "throttle", "camera"], "never": ["hold", "led", "vtx", "other"]},
 "pw1_keys": [{"index": 1, "key": "rates.type", "group": "rates"}, …],
 "defaults": {"propwash:freestyle": {"rates.roll.rcRate": 1.0, …}, "justmoreparts:xclass13_8s": { … }}}
```

`group_names`: Text de/en je Gruppe (`TuneGroup#translationKey`). `flight` = Gruppen des Tuning-Editors
(`TuneGroup.FLIGHT`), `shared` = was ein Sharecode trägt (Revision 1.2: dazu `camera`, der Kamerawinkel der Drohne),
`never` = alles andere. `pw1_keys` = Tabelle aus `sharecode.md` §5.3 (mit `TuneGroup.of`; Index 56 `camera.uptiltDeg`). Tuning-Werte sind float32 und werden mit ihrer kürzesten Dezimalform geschrieben (1,2 statt 1,2000000476…). `defaults` = `PropwashApi.builds().defaultTune(spec)`
je Preset (vollständige Standardwerte). Wertebereiche und Schrittweiten gehören nicht zur API; der Web-Konfigurator nimmt
sie aus `sharecode.md` bzw. zeigt Rohwerte (API-Wunsch, falls nötig).

### 2.7 `presets.json`

Alle Presets (`PropwashApi.builds().presets()`): `id`, Name de/en (`DronePreset#translationKey` oder `DroneData#name`),
`creative`, `build` (Ids je Kategorie + Zubehör je Platz), `paint` (Platz → `#rrggbb`), `tune` (gespeicherte Werte),
`sharecode` (seit dem 06.10.2026 PW2, `sharecode-pw2.md`; Ebenen Teile+Lack+Tuning – Presets haben keinen eigenen
Namen, die Ebene Name fehlt deshalb), `check` (`problems`, `warnings`), `analysis`
(alle Felder von `BuildStats` außer `warnings`, dazu `warnings`) und seit JMP 1.0.2 `camera_view` (Abschnitt 2.8.1).

### 2.8 `analysis/<preset>.json` — Ein-Teil-Varianten

Für jedes Preset: für jede Kategorie jedes **baubare** Teil (alle anderen Teile wie im Preset; Zubehör in seinem eigenen
Platz) ein Eintrag `{"category", "id", "check": {…}, "analysis": {…}, "camera_view": {…}}`, Datei
`{"preset": "<id>", "camera_view": {…}, "variants": [...]}` (`camera_view` oben für das Preset selbst). Das sind rund 26 Presets × ~180 Teile, auf dem Server in Sekunden
berechnet. Damit zeigt der Web-Konfigurator echte Propwash-Zahlen für den häufigsten Fall „ein Teil tauschen“; für
andere Kombinationen zeigt er die Regeln aus `rules.json` und die Kennzahlen aus `parts.json` (eine Physik im Browser
ist nicht Teil dieses Formats).

### 2.8.1 `camera_view` — FPV-Kamera für Vorschau und „Props im Bild“ (JMP 1.0.2, Propwash-API 1.1)

Aus `PropwashApi.builds().cameraView(spec)`: genau der Aufbau, mit dem Propwash das FPV-Bild zeichnet und
`props_in_view_percent` rechnet (Propwash 0.4.1, Flugmodell-Spezifikation 17.7). Koordinaten in Rahmen-Millimetern wie
`parts.json#frame.camera/arms`: x rechts, y oben, **−z vorn**.

```json
"camera_view": {
  "lens_mm": [0.0, 16.0, -78.0],
  "uptilt_deg": 22.0,
  "uptilt_min_deg": 0.0,
  "uptilt_max_deg": 40.0,
  "horizontal_fov_deg": 130.0,
  "vertical_fov_deg": 100.68,
  "aspect": 1.7777777777777777,
  "projection": "rectilinear",
  "props": [{"center_mm": [-135.0, 22.25, -72.0], "radius_mm": 89.0, "hub_radius_mm": 8.01}, …],
  "props_in_view_percent": 2.016540184661153
}
```

- `lens_mm`: Objektiv (Rahmen-`camera.pos`, ohne Angabe aus dem Schwerpunkt wie im Spiel), `uptilt_deg`: Standard-Uptilt
  des Rahmens (0–80°); `uptilt_min_deg`/`uptilt_max_deg` (ab JMP 1.0.0 mit Propwash-API 1.4): Bereich des Kamerahalters,
  in dem der Winkel der Drohne liegen darf (`CameraView#uptiltRange()`).
- `horizontal_fov_deg`: Sichtfeld der **Kamera im Videomodul** (`camera_fov_deg`, horizontal; Propwash 0.4.0,
  `FovMath.cameraHorizontalDegrees`), ohne Angabe analog 120°, digital 130°. So zeichnet das Spiel das Bild mit analoger
  und digitaler Brille.
- `props`: vier Kreisringe in der Ebene y = `center_mm[1]` (Nabe auf dem Motor laut `motor_seat` + Höhe der
  Unschärfe-Scheibe), von `hub_radius_mm` bis `radius_mm` (Blattspitze).
- Bild: geradlinige Projektion, Kamera schaut nach −z und ist um `uptilt_deg` um die x-Achse nach oben gekippt; ein Punkt
  p wird zu d = p − `lens_mm`, Kamera-Koordinaten (x, y·cos t + z·sin t, −y·sin t + z·cos t), Bildpunkt
  u = x / Tiefe, v = y' / Tiefe mit Tiefe = −z' > 0,25 mm; sichtbar bei |u| ≤ tan(hFOV/2), |v| ≤ tan(hFOV/2)/`aspect`.
- `props_in_view_percent`: Anteil der Bildfläche (16:9), den die Kreisringe bedecken – derselbe Wert wie
  `analysis.props_in_view_percent`. Im Spiel gegengeprüft (Propwash-Selbsttest: gerenderte Pixel gegen Analyse, alle
  JMP-Presets innerhalb 0,25 Prozentpunkten).

Damit kann der Web-Konfigurator eine Kamera-Vorschau zeichnen (Horizont, Uptilt, Propscheiben am Bildrand) und seine
eigene Rechnung prüfen.

### 2.9 `sharecode/spec.json`, `sharecode/vectors.json`

`spec.json`: maschinenlesbare Tabellen aus `sharecode.md` (Alphabet, Präfix, Namensraum-Tabelle, Zubehör-Plätze,
Lack-Bits, Tuning-Schlüssel, Grenzen, Statuswerte, Alias-Tabelle) und unter `pw2` die Tabellen aus `sharecode-pw2.md`
(`format`, `prefix`, `revision`, `alphabet`, `text_alphabet`, `checksum`, `unknown_part`, `params` mit den
Exp-Golomb-Parametern, `parts` je Art, `colors` als `#rrggbb`, `tune_keys` ab Index 1, `osd_dictionary` als Text).
`manifest.json` → `sharecode` nennt dazu `default_format` (`PW2`) und `pw2_revision`.

`vectors.json`: Liste von Testvektoren mit **erwarteten Ergebnissen**:

```json
{"name": "preset:justmoreparts:deadcat5_6s",
 "format": "PW1",
 "code": "PW1-…",
 "compression": "auto",
 "status": "OK",
 "layers": ["parts", "paint", "tune"],
 "decoded": {"build": {…}, "paint": {…}, "tune": {…}, "osd": null, "name": null},
 "skipped": [], "unknown_parts": [], "wrong_kind": [], "renamed": [],
 "check": {"problems": [], "warnings": []},
 "analysis": {"mass_grams": 534.3, "thrust_to_weight": 11.1, "hover_throttle_percent": 23.6, "hover_flight_time_min": 15.0,
              "cruise_flight_time_min": 7.0, "top_speed_kmh": 132.0, "full_throttle_current_a": 101.4, "esc_load_percent": 42.0,
              "battery_load_percent": 65.0, "motor_response_ms": 13.9, "crash_speed_ms": 8.5, "sustained_motor_temp_c": 85.2,
              "props_in_view_percent": 0.3, "warnings": [], "mixed_flight_time_min": 5.6, "aggressive_flight_time_min": 4.0,
              "cruise_speed_kmh": 55.0, "hover_current_a": 5.2, "cruise_current_a": 8.1, "mixed_current_a": 10.6,
              "landing_cell_voltage": 3.61, "avionics_power_w": 9.6, "motor_temp_minute_c": 91.0, "mixed_motor_temp_c": 70.0}}
```

Inhalt: die Vektoren A–F aus `sharecode.md` §13 (unverändert, mit `check`/`analysis` wo alle Teile bekannt sind; D und E
enthalten unbekannte/übersprungene Einträge), dazu je Preset ein Vektor `preset:<id>` (Kompression automatisch) und je
Preset ein Vektor `preset_parts:<id>` nur mit Teilen (Kompression „nie“, damit Web-Encoder bytegleich prüfen können);
seit dem 06.10.2026 außerdem die PW2-Vektoren `PW2_A_…` bis `PW2_I_…` aus `sharecode-pw2.md` und je Preset
`pw2_preset:<id>` und `pw2_preset_parts:<id>`. `format` (`PW1` oder `PW2`) sagt, mit welchem Kodierer ein Vektor
bytegleich nachzubauen ist; bei PW2 steht `compression` immer auf `auto` (das OSD-Layout wird gepackt, wenn es kürzer ist).
Ids aus einer neueren PW2-Teiletabelle erscheinen als `?<art>.<nummer>`.
Analysewerte ungerundet. `compression`: `never`, `always` oder `auto`; `status` wie `sharecode.md` §7.1 in Großbuchstaben;
`decoded` ist `null`, wenn der Status nicht `OK` ist; Ids aus unbekannten Namensraum-Nummern erscheinen als `?<ns>:<pfad>`;
`check`/`analysis` sind `null`, wenn ein Teil unbekannt ist.
Der Export dekodiert jeden Vektor mit `share.ShareCodec` und vergleicht mit dem erwarteten Inhalt (Rundreise), sonst `FAIL`.

### 2.10 `lang/*.json`, `assets/`

- `lang/<sprache>.json`: alle Schlüssel, die der Export benutzt (Teilnamen, Kurznamen, Hilfen, Halterungen, Chemien,
  Presets, Probleme/Warnungen, Lack-Plätze, Tuning-Gruppen, Import-Status), flach, sortiert.
- `assets/<namensraum>/…`: Render-Definitionen `items/drone_render/<kategorie>/…`, Modelle `models/part/<kategorie>/…`
  samt Varianten (`_ccw`, `_blur`, `_worn`, `_damaged`, `_broken`, `_under`, `_paint`), alle referenzierten Texturen,
  `drone_layout.json`, `drone_paint.json`, Inventar-Definitionen `items/<pfad>.json` — nur Dateien, die ein exportiertes
  Teil wirklich benutzt. Pfade wie im Mod.

### 2.11 `problems.json`

Liste von Meldungen `{"level": "error"|"warning", "where": "…", "message": "…"}`: fehlende Texte, fehlende Modelle
(Warnung), Regel-Abweichungen und Vektor-Fehler (Fehler). Mit Fehlern schreibt der Run `FAIL …`, sonst
`PASS <anzahl teile> Teile, <anzahl vektoren> Vektoren`.

---

### 2.12 Prüfung ohne Spiel

Die Java-Seite des Exports braucht nur die öffentliche API und den Plattformdienst. Der Web-Export wurde während der
Entwicklung außerhalb von Minecraft gegen die Teile-Entwürfe (`docs/design/catalog-draft/`) und das Propwash-Jar gerechnet:
174 Teile, 5000 Zufallsbaupläne ohne Abweichung von `check`, alle Vektoren vom Python-Referenzdecoder
(`tools/sharecode_ref.py`) gleich gelesen, die unkomprimierten Vektoren bytegleich neu kodiert.

## 3. Stabilität und Versionierung

- `format` 1 bleibt, solange nur Felder dazukommen; der Web-Konfigurator prüft `format` und lehnt größere Hauptwerte ab.
- `manifest.json#fingerprint` ändert sich mit jeder Teile-Änderung; der Web-Konfigurator zeigt „Datenstand“ und warnt
  bei Sharecodes mit Teilen, die im Datenstand fehlen.
- Neue Propwash-Felder (Parts-API 1.x) erscheinen in `data` unter ihrem JSON-Namen, sobald der Export sie kennt.


### 2.9 Flugzeiten und Energie (ab 1.0.4, Propwash 0.4.2)

Neue Felder im `analysis`-Objekt (Presets, Varianten, Sharecode-Vektoren), Definitionen in Propwashs
Flugmodell-Spezifikation Kap. 18 und `docs/design/realism/calibration.md`:

| Feld | Bedeutung | Beschriftung (Propwash-Sprachschlüssel) |
|---|---|---|
| `hover_flight_time_min` | Schweben bis zur Landespannung bzw. nutzbaren Kapazität (vorher 80 % ohne Landespannung) | `propwash.content.workbench.details.hover_time` |
| `cruise_flight_time_min` | Geradeausflug mit klassentypischer Reisegeschwindigkeit (vorher halbe Höchstgeschwindigkeit) | `propwash.content.workbench.details.cruise_time` |
| `cruise_speed_kmh` | diese Reisegeschwindigkeit | (im Text von `cruise_time`) |
| `mixed_flight_time_min` | typisches Fliegen der Klasse – **Hauptwert** | `propwash.content.workbench.stat.flight_time`, `…details.mixed_time` |
| `aggressive_flight_time_min` | Rennen bzw. hartes Freestyle | `propwash.content.workbench.details.aggressive_time` |
| `hover_current_a`, `cruise_current_a`, `mixed_current_a` | mittlerer Akkustrom | `propwash.content.workbench.details.currents` |
| `landing_cell_voltage` | Zellspannung unter der mittleren Last des gemischten Flugs bei der Landung (gelandet wird bei Landespannung im ruhigen Flug oder Abschaltspannung unter mittlerer Last) | `propwash.content.workbench.details.landing_voltage` |
| `avionics_power_w` | Grundlast Flugcontroller, Video, Zubehör | `propwash.content.workbench.details.avionics_power` |
| `motor_temp_minute_c` | Motor nach 60 s Vollgas im Stand | `propwash.content.workbench.details.motor_temp` |
| `mixed_motor_temp_c` | Motor am Ende eines gemischten Flugs (mit Fahrtwind-Kühlung) | `propwash.content.workbench.details.motor_temp_flight` |
| `sustained_motor_temp_c` | wie bisher: Vollgas im Stand bis der Akku leer ist (Grundlage der Warnung `motor_thermal`) | – |

Die Felder sind seit der ersten Fassung von 1.0.4 gleich; die Nachkalibrierung nach dem Realismus-Review (ebenfalls
1.0.4, Dateien ersetzt) hat nur ihre Werte geändert (Teile-Katalog §12). 1.0.5 (gegen Propwash 0.4.3) ändert ebenfalls nur Werte: die korrigierten
Teile (Teile-Katalog §13) und die Energiebilanz von 0.4.3; Sharecode-Tabellen und Testvektoren bleiben gleich.
Öffentlich erscheinen alle Felder mit Just More Parts 1.0.0 gegen Propwash 0.4.0 (`manifest.json`: `mods`); gegenüber
dem internen Stand 1.0.5 ändern sich nur Werte der in Teile-Katalog §13.2 nachgestimmten Teile, die Felder und die
Sharecode-Tabellen bleiben gleich.

`parts.json` enthält für Videoteile `power_w` (Leistungsaufnahme in W). Der Web-Konfigurator zeigt als Flugzeit den Wert
`mixed_flight_time_min` („Flugzeit gemischt“ / „Flight time (mixed)“) und in den Details alle vier Flugzeiten. Die
Beschriftungen liefert `analysis_labels.json`: je Kennzahl `{"key": <Propwash-Sprachschlüssel>, "text": {"de_de", "en_us"}}`
genau wie in der Werkbank (Platzhalter `%s` bzw. `%1$s`/`%2$s` für Wert und Reisegeschwindigkeit); dieselben Schlüssel
stehen auch in `lang/de_de.json` und `lang/en_us.json`.
