# Teile-Katalog — Propwash: Just More Parts 1.0

Status: **öffentliche Version 1.0.0 (2026-10-01, Nachtrag 2026-10-02)**. Sie fasst die internen Stände 1.0.0–1.0.5 zusammen;
die Abschnitte nennen die internen Nummern, in denen eine Änderung entstand (`docs/RELEASING.md`), §13.2 den Stand der
Veröffentlichung, §13.3 die Nachträge vom 02.10. (89-mm-Dreiblatt, Motorwärme, offene Punkte).
Frühere Statuszeile: Stand 1.0.1 (Modelle im Propwash-Stil, 2026-09-30), Teile und Werte wie 1.0.0; Abschnitt 8.3 enthält die Messwerte nach der Integration, Abschnitt 8.4
die Korrekturen und Messwerte nach dem Realismus-Review. Ursprünglich die
**verbindliche Liste** für die Implementierer 1–4 (Ids, Namen, Kennzahlen, Pro/Contra, Einsatz). Zahlen sind
**Entwurfswerte des Architekten**, im Prüfstand gegen Propwash 0.4.0 gemessen (Abschnitt 8.1). Implementierer dürfen
Werte innerhalb der Regeln aus Abschnitt 1 anpassen, **Ids und Namen nicht** (sie sind ab der ersten Veröffentlichung
eingefroren, siehe `docs/design/sharecode.md` §11). Alles, was hier fehlt, aber im Spiel gebraucht wird (Rezepte,
Hilfetexte), legt der Besitzer der Kategorie nach den Regeln in `docs/CONVENTIONS.md` an.

Umfang: 34 Motoren, 38 Propeller, 30 Akkus, 1 Zellchemie, 12 Stacks/AIOs, 7 Videosysteme, 22 Rahmen, 2 neue
Motorhalterungen, 22 Presets.

---

## 1. Grundsätze

### 1.1 Realismus entsteht in Propwash
JMP rechnet nichts selbst. Jede Eigenschaft wirkt über die Physik von Propwash 0.4.0 (`docs/design/flight-model-spec.md`
§17 im Propwash-Repository): Masse und Massenverteilung (Trägheit), Arm-Geometrie (Mixer, Hebel, Schwerpunkt),
Stirn-/Seiten-/Draufsicht (Luftwiderstand), Ducts, Motor-KV/Widerstand/Strom/Thermik (Drehmoment, Erwärmung,
Drosselung), Prop-Beiwerte/Steifigkeit/Haltbarkeit (Schub, Leistung, Propwash-Rauschen, Bruch), Akku-Chemie/
Innenwiderstand/C (Spannungseinbruch, Flugzeit), ESC-Strom/Spannung (Strombegrenzung, Durchbrennen bei Überspannung),
Kamera-Lage, Uptilt und Kamera-Sichtfeld (Props im Bild und FPV-Bild; seit dem Nachtrag vom 02.10.2026 kommt das Sichtfeld aus `camera_fov_deg` des Videosystems, nicht mehr von der Brille). Deshalb braucht **jedes Teil glaubwürdige Zahlen**, und jedes Pro/Contra im
Tooltip muss im Prüfstand sichtbar sein (Abschnitt 1.6). Ein Vorteil ohne messbare Wirkung wird nicht behauptet.

### 1.2 Ids und Namen
- Namensraum `justmoreparts`, Pfad nur `[a-z0-9_]` (Punkte vermeiden), eindeutig über alle Kategorien.
- Regeln je Kategorie (geprüft von `tools/parts_sync.py --check`):
  Motor `m<stator>_<kv>`, Prop `p<mm>_<pitch×10>_<n>b_<material>[_duct]`, Akku `b<S>s_<mAh>[_hv|_lion|_ss]`,
  Stack `aio_…`/`stack_…`, Video `analog_…`/`digital_…`, Rahmen frei (`<klasse>_<größe>_<form>`).
- Namen: Deutsch mit Umlauten und ß, Englisch; **ein Namensschema für Propwash und Just More Parts**, festgelegt in
  [`part-naming.md`](part-naming.md) (Kopie des Propwash-Dokuments `docs/design/part-naming.md`): Kern aus Kenngröße
  und Gerätewort wie bei den eingebauten Teilen (`2207-Motor, 1750 KV`, `6S-Akku 1300 mAh`), Zusätze in Klammern
  (`5,1-Zoll-Propeller (3 Blätter, 3,6″ Steigung, Bi-Blend)`, en `5.1″ Propeller (3-blade, 3.6″ pitch, bi-blend)`;
  `6S-Akku 3000 mAh (Li-Ion)`, en `6S 3000 mAh Battery (Li-Ion)`; `30×30-FC/ESC-Stack (3–6S, 60 A)`).
  Videosysteme tragen produktartige Eigennamen **ohne echte Marken** (Firefly, Racecore, Longshot, Pixel, Swift,
  Clarity, Horizon) als Modellnamen in Klammern (`Digitales HD-Videosystem (Clarity HD Pro)`). Geprüft von
  `tools/parts_sync.py --check` (Motor, Prop, Akku, Stack aus den Teile-Dateien berechnet), `share/ShareNamesTest`
  und `power/PowerDataTest`.

### 1.3 Motoren: Ableitung
- **Masse** und **Spitzenstrom** aus der Datenblatt-Klasse des Stators (gängige Motoren gleicher Baugröße).
- **Widerstand**: Propwash kalibriert seine Motoren über die Kennzahl `KV·√R` (niedriger = besserer Motor). Aus den
  eingebauten Motoren: 0802 ≈ 9840, 1404 ≈ 1720, 2004 ≈ 950, 2207 ≈ 410, 2306,5 ≈ 456, 2807 ≈ 344. Neue Motoren liegen
  auf der log-log-Geraden über dem Statorvolumen d²·h zwischen diesen Ankern; so passen sie zur Kalibrierung der
  eingebauten Teile. Ausnahmen: 0702/0802-19000 (etwas besserer Wert gegen `sluggish_motors`), 31xx (`KV·√R` = 170)
  und 42xx (`KV·√R` = 89) aus Datenblättern der X-Class-Klasse und passend zum Charakterisierungstest von Propwash.
- **Zellbereich** so, dass die Leerlaufdrehzahl (KV × Nennspannung) im üblichen Fenster der Klasse liegt
  (Whoops 80–100 k/min, 2–3″ 45–60 k, 5″ 38–45 k, 7″ 27–30 k, X-Class 9–13 k).
- **Thermik**: Standard, außer bei hitzefesten Magneten (Drosselung 140–145 °C) und großen Motoren mit offener
  Glocke (eigene Kühlung in W/K). Der 0702/27000 bekommt niedrigere Grenzen (winzige Wärmekapazität).

### 1.4 Propeller: Beiwerte
Propwash rechnet mit statischen Beiwerten `T = ct·ρ·n²·D⁴`, `P = cp·ρ·n³·D⁵`. Anker sind die eingebauten Props
(31 mm 3B: 0,090/0,072; 31 mm 4B: 0,100/0,085; 3″ 3B: 0,090/0,085; 5,1×4,6×3: 0,120/0,060; 7×3,5×2: 0,090/0,036).
Zwischen den Ankern wird log-linear über den Durchmesser interpoliert (3-Blatt-Äquivalent), dann skaliert:
`ct = ct₀·(B/B₀)^0,45·(P/D ÷ P/D₀)^0,5`, `cp = cp₀·(B/B₀)^0,75·(P/D ÷ P/D₀)^1,3`. Das bildet die bekannten
Zusammenhänge ab: mehr Blätter → mehr Schub und deutlich mehr Leistung (Grip gegen Effizienz), mehr Steigung →
mehr Topspeed und Strom, kleine Props arbeiten bei niedriger Reynoldszahl schlechter. Material wirkt nicht über die
Beiwerte, sondern über **Steifigkeit** (Propwash-Rauschen `÷√s`, Schubverlust unter Last bei s < 1) und
**Haltbarkeit** (Teiler der Crash-Dosis).

### 1.5 Akkus: Ableitung
- Masse = Nennenergie ÷ Energiedichte der Klasse: 1S-Whoop-HV 150 Wh/kg, kleine LiPo (≤ 650 mAh) 125, mittlere
  (850–1500) 132–138, große (≥ 1800) 140–150, Li-Ion 18650/21700-Hochstrom 215–218, Semi-Solid 220–245 (Pack-Ebene; der kleine
  6S 3000 mit mehr Gehäuse- und Schutzanteil 222, der 8S 5000 240). Das deckt sich mit
  den eingebauten Akkus (6S 1300: 137 Wh/kg, Li-Ion 6S 4000: 206 Wh/kg).
- Innenwiderstand je Zelle ≈ 7 mΩ·Ah (LiPo, Werte der eingebauten Akkus), Whoop-Zellen und kleine Packs schlechter,
  Li-Ion ≈ 70 mΩ·Ah, Semi-Solid ≈ 21 mΩ·Ah.
- C-Rate wie auf Etiketten üblich (Propwash ist darauf kalibriert: `BATTERY_WEAK`, wenn 4 × Motor-Spitzenstrom >
  1,3 × C × Ah). Li-Ion 10–12 C (Zellgrenze 45 A bei 4,5 Ah), Semi-Solid 20–25 C.

### 1.6 Prüfstand (Pflicht für jeden Implementierer)
`common/src/test/java/de/lardtom/justmoreparts/bench/` (Architekt): `PartFiles` liest alle JSON-Teile aus
`data/justmoreparts/propwash_parts/` (bis zur Integration: Kategorien ohne Ordner dort aus den Entwürfen) und meldet sie über
die **öffentliche** API an (in JUnit ohne Spielstart; der
Test `PartFilesTest#mapperReadsPropwashBuiltinFilesExactlyLikePropwash` beweist, dass der Leser die eingebauten Dateien
von Propwash bitgleich zu Propwash selbst liest). `Bench.row(label, Bench.spec(rahmen, stack, motor, prop, video, akku))`
liefert eine Tabellenzeile aus `builds().check` und `builds().analyze`. Aufruf:
`JMP_TAIL=200 tools/fastcheck.sh test --select-class <Klasse>`.

Jeder Implementierer schreibt einen eigenen Test in seinem Paket (`motor/MotorBenchTest`, `prop/PropBenchTest`,
`power/PowerBenchTest`, `frame/FrameBenchTest`), der für **jedes** seiner Teile mindestens einen Referenzaufbau misst
und die Zielkorridore aus 1.7 sowie die behaupteten Pro/Contra-Richtungen als Assertions prüft (z. B. „2-Blatt fliegt
länger als 3-Blatt gleicher Größe“, „4S-Stack meldet auf 6S `stack_voltage`“, „Deadcat hat < 1 % Props im Bild“).
In JUnit nur Teile im Namensraum `justmoreparts` über `PartFiles.registerAll()` anmelden; Versuchsteile im Namensraum
`jmp_test`, nie `propwash`.

Vergleichsaussagen („mehr Schub“, „länger in der Luft“, „schneller“, „kühler“ …) gelten erst ab **1 % Abstand** im
Prüfstand (`PropBenchTest.MARGIN`, `gt`/`lt` in `MotorBenchTest`, `PowerClaimsTest`, `FrameBenchTest`); knappere
Unterschiede sind im Flug nicht spürbar und stehen nicht in der Shift-Hilfe (Realismus-Review 2026-09-30).

### 1.7 Zielkorridore (real übliche Werte, gemessen mit Propwash-Analyse)

| Klasse | Masse | T/W | Schwebegas | Schweben / Reise | Topspeed | Motor °C (Dauervollgas Stand) | Props im Bild | Warnungen im Preset |
|---|---|---|---|---|---|---|---|---|
| Whoop 65–85 mm 1–2S | 20–60 g | 3,5–7 | 25–50 % | 7–13 / 6–10 min | 35–65 km/h | < 60 | ≤ 20 % | keine |
| Toothpick 2–3,5″ | 50–230 g | 5–8 | 25–35 % | 10–14 / 6–8 | 70–110 | < Drosselgrenze | ≤ 15 % | keine |
| Cinewhoop 2,5–3,5″ | 190–420 g | 3,5–5 | 40–50 % | 8–11 / 7–8 | 50–60 | < 100 | ≤ 25 % | keine |
| 5″ Freestyle/Race | 400–560 g | 10–15 | 16–25 % | 13–17 / 5–7,5 | 125–190 | < Drosselgrenze | ≤ 15 % (Deadcat/H 0 %) | höchstens `heavy_sag` beim 6S-Race |
| Long Range 4–8″ | 0,44–1,4 kg | 3,5–11 | 20–50 % | 19–45 / 10–35 | 90–130 | < Drosselgrenze | ≤ 20 % | Li-Ion: `battery_weak`, `heavy_sag`, `battery_overload` erlaubt (wie `propwash:long_range`) |
| Long Range 7″ 8S LiPo (Preset `lr7_8s`) | 0,8–0,95 kg | 9–13 | 18–25 % | 19–30 / 8–14 | 130–170 | < Drosselgrenze | ≤ 10 % | keine |
| X-Class 10–13″ | 1,6–3,4 kg | 5–9 | 25–40 % | 16–25 / 11–15 | 100–140 | < Drosselgrenze | ≤ 20 % | keine |

„Props im Bild“ seit JMP 1.0.2 mit Propwash 0.4.1 gemessen: der Wert entspricht jetzt dem FPV-Bild im Spiel (Nabe auf
dem Motor laut `motor_seat`, Unschärfe-Scheibe bis zur Blattspitze, seit 02.10.2026 Sichtfeld der Kamera im Videosystem
(vorher Brillen-Sichtfeld analog 120°/digital 130°); im
Spiel gegen gerenderte Pixel geprüft). Die alte Rechnung lag bei True-X-Rahmen mit Kamera zwischen den Vorderprops zu
niedrig (eingebauter 5″ 1,9 statt 11,7 %, eingebauter 7″ 11,2 statt 19,5 %, Cinewhoop 17 statt 21 %); die Korridore
für 5″ (15 %), Long Range (20 %), Cinewhoop (25 %) und X-Class (20 %) folgen den eingebauten Referenzen. Deadcats und
H-Frames zeigen 0 % – `deadcat_5` hat dafür seit 1.0.2 die Kamera 50 mm vor den Vorderarmen (vorher 7 mm: 2,6 % Props
im Bild), der Stretched-Toothpick 18 mm vor den Vorderarmen (vorher mitten zwischen den Props: 33 %, jetzt 7,6 %).
`lr_7_deadcat` hat seit 1.0.3 wieder die Kamera aus 1.0.0/1.0.1 (0, 16, −78), 6 mm vor den Vorderarmen: mit dem
130°-Bild der Digitalbrille ragen die Spitzen der 7″-Props als schmale Sicheln in den linken und rechten Bildrand – so,
wie es der Besitzer im Flug gesehen hat. „Props im Bild“ nennt dafür 2,02 % (`lr7_8s`) bzw. 1,84 % (`lr7_deadcat_lion`),
im Spiel an gerenderten Pixeln geprüft (1.0.2 hatte die Kamera auf −125 mm gesetzt und das Bild damit leer gemacht).
Seit 1.0.0 öffentlich (Architektur §10.15) haben alle Rahmen einen Kamerahalter mit dem Objektiv wenige Millimeter
unter der Propebene; vorher lag das Objektiv der Rahmen ohne `camera.pos` 9–28 mm darüber (Propwashs Altformel), und
der Race-Stretch zeigte 0–2 %. Die Korridore bleiben; die Presets liegen jetzt bei 2–10 %, Deadcats und H-Frames 0 %.

Anpassungen nach dem Realismus-Review (2026-09-30), jeweils gegen reale Klassenwerte: X-Class-Topspeed 80–115 →
100–140 km/h (real 120–150; die alte Stirnfläche ließ den 13″ langsamer fliegen als einen 5″), Cinewhoop-Masse bis 420 g
(3,5″ auf 6S mit vollwertigem HD-System wiegt real 330–420 g), 5″-Reiseflug ab 5 min (real 3–5 min Freestyle),
Long-Range-Reiseflug bis 35 min (8″ mit 6S2P-Li-Ion fliegt real über 30 min) und eine eigene Zeile für den 7″ auf 8S-LiPo
(real 800–950 g, T/W 9–13, 140–170 km/h), der sonst die Long-Range-Grenzen für T/W und Topspeed sprengt.

---

## 2. Neue Halterungen und Zellchemie

| Id | Art | Name (de) | Name (en) | Besitzer | Benutzt von |
|---|---|---|---|---|---|
| `justmoreparts:m31` | Motorhalterung | 31xx | 31xx | 1 (Sprachschlüssel `justmoreparts.mount.motor.m31`) | Motoren 3115; Rahmen `xclass_10`, `xclass_12` |
| `justmoreparts:m42` | Motorhalterung | 42xx | 42xx | 1 (`justmoreparts.mount.motor.m42`) | Motoren 4214; Rahmen `xclass_12`, `xclass_13`, `xclass_13_h` |
| `justmoreparts:lihv_ss` | Zellchemie | Semi-Solid-LiHV | Semi-solid LiHV | 3 (`justmoreparts.chemistry.lihv_ss`) | Akkus `…_ss` |

Vorhandene Propwash-Halterungen werden weiterbenutzt: Motor `propwash:m08` (Whoop, 3×M1,4 auf 6,6 mm — 0702 bis
1102), `propwash:m14` (9×9/12×12 M2 — 1103 bis 2004), `propwash:m22` (16×16 M3 — 22xx/23xx), `propwash:m28` (16×16/19×19
M3 — 25xx/28xx); Stack `propwash:whoop_aio` (25,5 mm), `propwash:s20`, `propwash:s30`. Prop-Aufnahmen bleiben
`propwash:universal` (Welle 1 mm, 1,5 mm, T-Mount und M5 stehen nur im Tooltip; eine eigene Aufnahme je Welle würde
jede 3″-Größe verdoppeln, Nutzen gering).

Zellchemie `lihv_ss` (JSON `chemistry/lihv_ss.json`): `full_v` 4,45, `nominal_v` 3,85, `empty_v` 3,5, `cutoff_v` 3,3,
`storage_v` 3,9, `density_g_cm3` 2,0, `ocv_shape` wie LiPo (0, 0,13, 0,20, 0,29, 0,34, 0,39, 0,43, 0,49, 0,57, 0,69, 0,83, 1).
Ehrlich: mehr Energie je Gramm, aber niedrige C-Rate und höherer Innenwiderstand als LiPo.

### 2.1 Tooltip (Shift) — für alle Kategorien
Propwash zeigt `justmoreparts.help.<id>.summary` und die Zeilen `.what.1…8` („Was ist das?“) und `.use.1…8`
(„Wofür?“), je Zeile **höchstens 48 Zeichen**. Pflichtaufbau:
- `summary`: `<Kurzkennung> – <Einsatz in drei bis sechs Wörtern>` wie bei den eingebauten Teilen, Kurzkennung im
  Satzbau (klein, außer Nomen im Deutschen): Motor `2207-Motor, 1750 KV`, Prop `5,1″-Dreiblatt, Bi-Blend`, Akku
  `6S-LiPo, 1300 mAh` (en `6S LiPo, 1300 mAh`), Stack `30×30-Stack (3–6S, 60 A)` (en `30×30 stack (3–6S, 60 A)`),
  Rahmen `5″-Race-Rahmen (Slim)` bzw. `5″-Freestyle-Rahmen` (Form vorn im Einsatz: `– Deadcat, …`), Video `Firefly Nano`;
  Regeln in [`part-naming.md`](part-naming.md) §6, geprüft für jedes Teil von `tools/parts_sync.py --check`.
- `what.1`: Kennzahlen kurz (`2207 · 1950 KV · 32,8 g · 50 A`).
- `what.2…`: je Vorteil eine Zeile mit `+ `, je Nachteil eine Zeile mit `− ` (echtes Minuszeichen U+2212), aus den Spalten
  „Pro“/„Contra“ dieses Katalogs, ausformuliert; nur Aussagen, die der Prüfstand zeigt.
- `use.1…`: Einsatz und passende Partner (`Passt zu: 5″-Rahmen, 6S 1100–1500`), Warnungen (`Nicht auf 6S: ESC brennt`).
- Englisch sinngemäß, gleiche Zeilenzahl.
Propwash ergänzt selbst die Datenzeilen (Masse, Halterung, KV, Zellen, Hitzegrenzen, Material, Stecker, FOV …).

## 3. Motoren (Implementierer 1 — MOTORS)

Id-Regel: `m<Stator>_<KV>`; der Stator steht mit vier Ziffern (`2207`), Halbmillimeter-Höhen mit fünf (`22075` = 2207,5, `12025` = 1202,5). Name de/en nach dem Muster der eingebauten Motoren. **R** ist der Wicklungswiderstand (Phase–Phase), abgeleitet aus der Kennzahl KV·√R der eingebauten Motoren (Abschnitt 1.3), bei X-Class-Motoren aus Datenblättern der Klasse. **I** = `max_current_a` (Spitzenstrom laut Datenblatt-Klasse). Thermik leer = Propwash-Standard (Drosselung 130 °C, Maximum 150 °C, Kühlung aus der Masse).

Stand nach der Umsetzung (Branch `feature/motors`): Werte, Pro und Contra sind genau die der Teile-Dateien und der
Shift-Hilfen; jede Aussage ist im Prüfstand belegt (`motor/MotorBenchTest#claimedProsAndConsAreVisibleOnTheBench`, 94
Aussagen). Thermik leer = Propwash-Standard; „Fahrtwind“ ist `thermal.cooling_w_per_k_per_rad_s`.

| Id | Name (de) | Name (en) | Stator | KV | g | I [A] | R [Ω] | Zellen | Halterung | Thermik | Einsatz | Pro | Contra |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `m0702_23000` | 0702-Motor, 23000 KV | 0702 Motor, 23000 KV | 0702 | 23000 | 1,8 | 3,0 | 0,2100 | 1S | `propwash:m08` |  | 65-mm-Whoop auf 1S, ruhiges Fliegen | leichtester Motor im Katalog (1,8 g); längere Flugzeit als 27000 KV; wenig Strom, bleibt kühl | weniger Schub (T/W 3,5 statt 4,3) |
| `m0702_27000` | 0702-Motor, 27000 KV | 0702 Motor, 27000 KV | 0702 | 27000 | 1,8 | 3,4 | 0,1600 | 1S | `propwash:m08` | 115/135 °C | 65-mm-Whoop auf 1S, Rennen | spritzig: mehr Schub als 23000 KV; mehr Topspeed (57 statt 51 km/h) | kürzere Flugzeit als 23000 KV; drosselt schon ab 115 °C (kleiner Motor) |
| `m0802_15500` | 0802-Motor, 15500 KV | 0802 Motor, 15500 KV | 0802 | 15500 | 2,3 | 3,6 | 0,3223 | 1–2S | `propwash:m08` |  | 65–75-mm-Whoop auf 2S | auf 2S kräftig (T/W 6) und effizient; auf 2S länger in der Luft als 1102 | auf 1S kraftlos; mit 45-mm-4-Blatt-Props träge |
| `m0802_19000` | 0802-Motor, 19000 KV | 0802 Motor, 19000 KV | 0802 | 19000 | 2,3 | 4,2 | 0,2100 | 1S | `propwash:m08` |  | 75-mm-Whoop auf 1S | 75 mm auf 1S: T/W 4, 7 min Schweben; auf 1S länger in der Luft als 1102 | 0,5 g schwerer als 0702; mit 31-mm-Props schwächer als 0702 |
| `m1102_13500` | 1102-Motor, 13500 KV | 1102 Motor, 13500 KV | 1102 | 13500 | 3,4 | 6,0 | 0,1560 | 1–2S | `propwash:m08` |  | 85-mm-Whoop oder 2″-Toothpick auf 2S | viel Drehmoment: dreht 45-mm-4-Blatt | auf 1S zu schwach (65 mm hebt nicht ab); 1,1 g schwerer als ein 0802 |
| `m1102_18000` | 1102-Motor, 18000 KV | 1102 Motor, 18000 KV | 1102 | 18000 | 3,4 | 7,5 | 0,0877 | 1S | `propwash:m08` |  | 75-mm-Whoop auf 1S, Grip und Punch | mehr Standschub als 0802 auf 1S; reagiert schneller (34 statt 46 ms, 1.0.5) | hoher Strom, kürzere Flugzeit; 6-A-AIO ist zu klein |
| `m1103_8000` | 1103-Motor, 8000 KV | 1103 Motor, 8000 KV | 1103 | 8000 | 4,0 | 7,0 | 0,2036 | 2–3S | `propwash:m14` | Fahrtwind 5,5·10⁻⁵ W/(K·rad/s) | 2,5″-Toothpick auf 3S | leichter 3S-Antrieb, sehr kräftig; schwebt länger als 1202,5 auf 4S | auf 2S lahm |
| `m1103_11000` | 1103-Motor, 11000 KV | 1103 Motor, 11000 KV | 1103 | 11000 | 4,0 | 9,0 | 0,1077 | 1–2S | `propwash:m14` | Fahrtwind 2,2·10⁻⁵ W/(K·rad/s) (1.0.5) | 2″-Toothpick auf 2S | leichter und ausdauernder als 1202,5 | wenig Reserve mit 2,5″-Props |
| `m12025_5500` | 1202,5-Motor, 5500 KV | 1202.5 Motor, 5500 KV | 1202,5 | 5500 | 4,7 | 9,0 | 0,3500 | 3–4S | `propwash:m14` | Fahrtwind 8·10⁻⁵ (2 × Standard, §13.3) | 2,5″-Toothpick auf 4S | leicht für 4S: schwebt länger als 1404; mehr Schub als 1404 mit 2,5″; auf 2,5″ 4S unter 80 °C nach 1 min | mit 3″-Props überfordert: über 100 °C |
| `m12025_11500` | 1202,5-Motor, 11500 KV | 1202.5 Motor, 11500 KV | 1202,5 | 11500 | 4,7 | 13,0 | 0,0800 | 1–2S | `propwash:m14` |  | 2–2,5″-Toothpick auf 2S, Speed | reagiert schneller als 1103 (30 statt 32 ms); mehr Topspeed als 1103 mit 2,5″-Props | schwerer, kürzer in der Luft als 1103; hoher Strom: Akku braucht hohe C-Rate |
| `m1404_2750` | 1404-Motor, 2750 KV | 1404 Motor, 2750 KV | 1404 | 2750 | 9,0 | 13,0 | 0,3918 | 5–6S | `propwash:m14` |  | 3″-Toothpick oder Cinewhoop auf 6S | wenig Strom, lange Flugzeit | 6S-Akku macht die Drohne 20 g schwerer; auf 4S zu wenig Spannung |
| `m1404_3800` | 1404-Motor, 3800 KV | 1404 Motor, 3800 KV | 1404 | 3800 | 9,0 | 17,0 | 0,2052 | 3–4S | `propwash:m14` |  | 3″-Toothpick oder 2,5″-Cinewhoop auf 4S | weniger Strom und kühler als 4600 KV; längere Flugzeit als 4600 KV | weniger Punch als 4600 KV |
| `m1505_2500` | 1505-Motor, 2500 KV | 1505 Motor, 2500 KV | 1505 | 2500 | 12,8 | 20,0 | 0,2594 | 5–6S | `propwash:m14` | Fahrtwind 1,6·10⁻⁴ (2 × Standard; 1.0.4: 1,2·10⁻⁴, §13.3) | 3,5″-Toothpick auf 6S | viel Drehmoment, auch für Ducts; im Duct etwas mehr Schub als 2004 (1.0.5) | im Cinewhoop zu heiß (drosselt, 1.0.5); schwerer als ein 1404 |
| `m1505_3600` | 1505-Motor, 3600 KV | 1505 Motor, 3600 KV | 1505 | 3600 | 12,8 | 25,0 | 0,1251 | 3–4S | `propwash:m14` |  | 3–3,5″-Toothpick auf 4S | kräftig auf 4S (T/W um 8) | hoher Strom: doppelt so viel wie 2500 KV; ein 20-A-AIO ist zu klein |
| `m2004_1700` | 2004-Motor, 1700 KV | 2004 Motor, 1700 KV | 2004 | 1700 | 16,5 | 22,0 | 0,221 | 5–6S | `propwash:m14` | Fahrtwind 2,36·10⁻⁴ W/(K·rad/s) (1.0.0, §13.2) | 3,5″-Cinewhoop oder 4″-Long-Range auf 6S | im Duct ohne Drosselung (83 °C nach 1 min); im Duct ein Drittel weniger Strom als 1505 | zu schwer für Toothpicks (16,5 g) |
| `m2207_1500` | 2207-Motor, 1500 KV | 2207 Motor, 1500 KV | 2207 | 1500 | 32,8 | 40,0 | 0,0749 | 6S | `propwash:m22` |  | 5″-Cruiser oder 6″-Long-Range auf 6S | längere Flugzeit als 1950 KV; wenig Strom (71 statt 125 A); mit 6″ länger in der Luft als ein 2507 | weniger Punch und Topspeed |
| `m2207_1950` | 2207-Motor, 1950 KV | 2207 Motor, 1950 KV | 2207 | 1950 | 32,8 | 50,0 | 0,0443 | 5–6S | `propwash:m22` |  | 5″-Freestyle auf 6S | viel Punch (T/W über 11) | mehr Strom als 1500 KV; wärmer als 1500 KV |
| `m2207_2550` | 2207-Motor, 2550 KV | 2207 Motor, 2550 KV | 2207 | 2550 | 32,8 | 55,0 | 0,0259 | 4S | `propwash:m22` |  | 5″-Freestyle oder Race auf 4S | auf 4S fast so stark wie 1950 KV auf 6S | auf 6S überdreht: Akku bricht ein; auf 6S über 50 % mehr Strom |
| `m22075_1850` | 2207,5-Motor, 1850 KV | 2207.5 Motor, 1850 KV | 2207,5 | 1850 | 35,5 | 50,0 | 0,0468 | 6S | `propwash:m22` | 145/165 °C | 5″-Freestyle mit Action-Cam, Deadcat | hitzefest: drosselt erst ab 145 °C; kühler als ein 2207 mit 1950 KV; viel Drehmoment: reagiert schneller | 2,7 g schwerer, etwas weniger Schub |
| `m22075_2700` | 2207,5-Motor, 2700 KV | 2207.5 Motor, 2700 KV | 2207,5 | 2700 | 35,5 | 58,0 | 0,0220 | 4S | `propwash:m22` |  | 5″-Freestyle auf 4S | maximaler Punch und Topspeed auf 4S | hoher Strom: 1500 mAh brechen ein; auf 6S überdreht |
| `m23065_2020` | 2306,5-Motor, 2020 KV | 2306.5 Motor, 2020 KV | 2306,5 | 2020 | 31,5 | 46,0 | 0,0408 | 5–6S | `propwash:m22` |  | 5″-Race auf 6S | mehr Topspeed als 2207 mit 1950 KV; 1,3 g leichter als ein 2207 | mehr Strom als 2207 mit 1950 KV; mit 5,0″-Steigung bricht der Akku ein |
| `m23065_2450` | 2306,5-Motor, 2450 KV | 2306.5 Motor, 2450 KV | 2306,5 | 2450 | 31,5 | 48,0 | 0,0278 | 4S | `propwash:m22` |  | 5″-Race auf 4S | leicht: über 150 km/h auf 4S; weniger Strom als 2207 mit 2550 KV | auf 6S überdreht |
| `m2507_1500` | 2507-Motor, 1500 KV | 2507 Motor, 1500 KV | 2507 | 1500 | 41,5 | 48,0 | 0,0621 | 6S | `propwash:m28` |  | 6″-Long-Range auf 6S | mit 6″-Props kühler als ein 2207 | 8,7 g schwerer als 2207: kürzer in der Luft |
| `m2507_1800` | 2507-Motor, 1800 KV | 2507 Motor, 1800 KV | 2507 | 1800 | 41,5 | 55,0 | 0,0431 | 5–6S | `propwash:m28` | 140/160 °C | 6″-Freestyle auf 6S | sehr viel Schub (T/W über 12); hitzefest: drosselt erst ab 140 °C | hoher Strom, kürzere Flugzeit |
| `m28065_1050` | 2806,5-Motor, 1050 KV | 2806.5 Motor, 1050 KV | 2806,5 | 1050 | 45,5 | 38,0 | 0,1133 | 6–8S | `propwash:m28` | Fahrtwind 2,7·10⁻⁴ W/(K·rad/s) | 8″-Long-Range auf 8S | auf 8S ausdauernd (8″: fast 40 min); 2 g leichter als ein 2807 | auf 6S schwach |
| `m28065_1300` | 2806,5-Motor, 1300 KV | 2806.5 Motor, 1300 KV | 2806,5 | 1300 | 45,5 | 42,0 | 0,0739 | 6S | `propwash:m28` | Fahrtwind 2,7·10⁻⁴ W/(K·rad/s) | 7″-Long-Range auf 6S | effizienter 7″-Standard: 33 min Schweben; leichter und kühler als 2807/1300 | wenig Punch; mit 8″-Props überfordert |
| `m2807_1100` | 2807-Motor, 1100 KV | 2807 Motor, 1100 KV | 2807 | 1100 | 47,5 | 40,0 | 0,0978 | 6–8S | `propwash:m28` | Kühlung 0,14 W/K, Fahrtwind 2,8·10⁻⁴ W/(K·rad/s), 140/160 °C | 8″-Long-Range auf 6–8S | viel Drehmoment für 8″-Props; offene Glocke kühlt: drosselt nicht | schwer (47,5 g) |
| `m2807_1500` | 2807-Motor, 1500 KV | 2807 Motor, 1500 KV | 2807 | 1500 | 53,0 | 50,0 | 0,0526 | 5–6S | `propwash:m28` |  | 7″-Freestyle auf 6S | kräftig mit 7″-3-Blatt (T/W über 12) | hoher Strom: Akku bricht ein; kürzere Flugzeit als 1100 KV; mit 3-Blatt warm (über 110 °C) |
| `m3115_640` | 3115-Motor, 640 KV | 3115 Motor, 640 KV | 3115 | 640 | 90,0 | 55,0 | 0,0706 | 6–8S | `justmoreparts:m31` | Kühlung 0,26 W/K, Fahrtwind 4,3·10⁻⁴ W/(K·rad/s), 140/160 °C | 10–11″ X-Class oder Long Range auf 8S | effizient: längste Flugzeit der 3115 | auf 6S träge |
| `m3115_900` | 3115-Motor, 900 KV | 3115 Motor, 900 KV | 3115 | 900 | 90,0 | 65,0 | 0,0357 | 6S | `justmoreparts:m31` | Kühlung 0,26 W/K, Fahrtwind 4,3·10⁻⁴ W/(K·rad/s), 140/160 °C | 10″ X-Class auf 6S | ausgewogen: T/W über 8 mit 10″ | mit 12″-Props überfordert |
| `m3115_1050` | 3115-Motor, 1050 KV | 3115 Motor, 1050 KV | 3115 | 1050 | 90,0 | 70,0 | 0,0262 | 6S | `justmoreparts:m31` | Kühlung 0,26 W/K, Fahrtwind 4,3·10⁻⁴ W/(K·rad/s), 140/160 °C | 10″ X-Class-Race auf 6S | sehr spritzig: mehr Schub als 900 KV | hoher Strom: Akku bricht ein; kürzere Flugzeit als 900 KV |
| `m4214_330` | 4214-Motor, 330 KV | 4214 Motor, 330 KV | 4214 | 330 | 128,0 | 55,0 | 0,0727 | 8–12S | `justmoreparts:m42` | Kühlung 0,34 W/K, 145/165 °C | 13″ X-Class zum Cruisen auf 8S | wenig Strom, längste Flugzeit | auf 8S wenig Punch (T/W unter 5) |
| `m4214_400` | 4214-Motor, 400 KV | 4214 Motor, 400 KV | 4214 | 400 | 128,0 | 65,0 | 0,0495 | 6–12S | `justmoreparts:m42` | Kühlung 0,34 W/K, 145/165 °C | 12–13″ X-Class auf 8S | Standard für 13″: ausgewogen | schwer (128 g je Motor) |
| `m4214_530` | 4214-Motor, 530 KV | 4214 Motor, 530 KV | 4214 | 530 | 128,0 | 80,0 | 0,0282 | 6–8S | `justmoreparts:m42` | Kühlung 0,34 W/K, 145/165 °C | 13″ X-Class-Race auf 8S | enormer Punch (T/W über 7) | sehr hoher Strom: Akku bricht ein; kürzeste Reiseflugzeit der 4214 |

Halterungen (Sprachschlüssel `justmoreparts.mount.motor.m31|m42`): „31xx“ und „42xx“ wie die eingebauten
„22xx/23xx“ und „28xx“ (Lochbilder 19×19/25×25 mit M3 bzw. 30×30 mit M4). Seltenheit der Items (`MotorModule`, vor `registerItems`): Stator ab 30 mm „ungewöhnlich“
(3115), ab 40 mm „selten“ (4214), wie die X-Class-Rahmen. Kreativ-Tab: alle Motoren über `creativeListed`, `sort` 100–133
nach Stator (Durchmesser, Höhe) und KV.

### 3.1 Kalibrierung gegenüber dem Entwurf

| Motor | Änderung | Grund und Wirkung im Prüfstand |
|---|---|---|
| `m0802_15500` | R 0,4029 → 0,3223 Ω (−20 %) | KV·√R 8800 wie der 19000er derselben Reihe; vorher `sluggish_motors` im eigenen Einsatz (75 mm, 2S) |
| `m1103_8000`, `m1103_11000` | Fahrtwind 5,5·10⁻⁵ W/(K·rad/s) (1,5 × Standard) | 2,5″ auf 3S vorher 136 °C (`motor_thermal`), jetzt 104 °C; Preset `toothpick2_2s` 44 → 38 °C |
| `m28065_1050`, `m28065_1300` | Fahrtwind 2,7·10⁻⁴ (1,45 × Standard) | 1050 KV auf 8S mit 8″ vorher 141 °C über der Drosselung, jetzt 116 °C; `lr7_deadcat_lion` 97 → 81 °C |
| `m2807_1100` | Fahrtwind 2,8·10⁻⁴ (1,46 × Standard) zusätzlich zu 0,14 W/K | offener Punkt aus 8.1 erledigt: `lr8_6s_lion` 153 °C (`motor_thermal`) → 126 °C, Drosselung 140 °C |
| `m3115_640/900/1050` | Fahrtwind 4,3·10⁻⁴ (1,47 × Standard) | 640 KV auf 8S vorher 153 °C (`motor_thermal`), jetzt 130 °C; `xclass10_6s` 126 → 111 °C |
| `m12025_5500`, `m1505_2500` | Fahrtwind 8·10⁻⁵ bzw. 1,6·10⁻⁴ (je 2 × Standard, 1.0.0 vom 02.10.) | 60-s-Werte nach den eigenen Prüfstandszeilen (F1203, F1408): 2,5″ 4S 121 → 79 °C, `toothpick35_6s` mit dem korrigierten 3,5″-Dreiblatt 78 statt 93 °C (§13.3) |
| alle | Akzentfarbe je Motor, `sort` neu | Glockenfarbe (Modell und prozedurale Form), keine Farbe gleich einem eingebauten Motor |

KV·√R bleibt innerhalb eines Stators gleich (Streuung < 4 %) und fällt mit der Statorgröße (JUnit
`MotorDataTest`). Kühlung nur im Rahmen von 1.3 (Fahrtwind bei der Umsetzung höchstens 1,5 × Standard, seit 1.0.4 nach der Datenregel von
`MotorDataTest` höchstens 2,5 × Standard; die Standkühlung der X-Class-Motoren
bleibt beim Entwurf, höchstens 2 × Standard).

### 3.2 Referenzaufbauten (Prüfstand)

Jeder Motor hat einen Referenzaufbau in seiner Klasse (`MotorBenchTest`), gemessen mit den übrigen Kategorien aus
`catalog-draft`. Korridore wie 1.7 mit ±5 % Toleranz (Rundung der Tabelle; auch das Preset `race5_6s` liegt mit
17,2 min Schweben knapp darüber); zusätzlich ein Korridor „Freestyle 6–7″“ (550–900 g, T/W 9–14, Schwebegas 15–27 %,
Schweben 14–22 min, Reise 6–9 min, 120–160 km/h, Motor unter der Drosselgrenze, ≤ 10 % Props im Bild), weil 1.7 keinen hat.
Letzte Spalte: Reaktionszeit der Motoren.

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
m0702_23000 65 mm 1S 250              22.7   3.8  48.6    8.4    6.9     37    7.7    31    77   29.6  18.6    45.4ms
m0702_27000 65 mm 1S 380              26.0   4.3  44.1   11.1    8.6     41   11.2    45    66   31.0  16.9    48.5ms
m0802_15500 75 mm 2S 300              38.3   6.0  28.8   12.1    7.4     52   17.4    36   129   46.1  13.8    43.3ms
m0802_19000 75 mm 1S 450 (Preset whoop75_1s)    32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  19.9    54.9ms
m1102_13500 85 mm 2S 450 (Preset whoop85_2s)    56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0    44.7ms
m1102_18000 75 mm 1S 750, 4 Blatt     46.1   3.6  50.7   12.7   10.5     43   15.4    31    27   32.5  15.8    38.8ms
m1103_8000 2,5" 3S 650                98.2   7.3  23.3   13.1    6.9     93   55.0    68    94  104.1   9.2    48.7ms
m1103_11000 2" 2S 450 (Preset toothpick2_2s)    57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   38.2  10.7    31.6ms
m12025_5500 2,5" 4S 450               97.4   6.7  26.3   11.1    6.5     89   35.0    44    97  119.7   9.6    51.7ms
m12025_11500 2" 2S 450                60.7   6.1  30.2    9.5    6.4     75   31.2    65    92   43.9  10.5   W[battery_weak][] 36.6ms
m1404_2750 3" 6S 550                 163.7   6.3  32.7   11.6    7.6     95   30.1    38    68   84.3   6.7    23.4ms
m1404_3800 3" 4S 650 (Preset toothpick3_4s)   142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4    24.0ms
m1505_2500 3,5" 6S 750 (Preset toothpick35_6s)   222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5    25.2ms
m1505_3600 3,5" 4S 850               190.0   8.0  26.6   12.9    7.2    103   59.5    42    70   88.9   7.5    25.8ms
m2004_1700 Cinewhoop 3,5" 6S (Preset cinewhoop35_6s)   339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6    22.5ms
m2207_1500 6" LR 6S 1800             603.0  11.2  23.4   21.8   10.4    129  104.1    43    58  116.4   8.3    18.5ms
m2207_1950 5" Bash 6S 1300 (Preset bash5_6s)   530.3  11.7  21.2   14.9    6.2    140  124.7    52    80   92.3   5.8    12.8ms
m2207_2550 5" Race 4S 1500           416.2  11.3  21.7   15.7    7.3    154  123.9    56    69   75.2   0.0    13.9ms
m22075_1850 5" Deadcat 6S 1300 (Preset deadcat5_6s)   534.3  11.1  23.6   15.0    7.0    132  101.4    42    65   85.2   0.3    12.1ms
m22075_2700 5" Race 4S 1500          426.2  11.8  21.3   15.9    7.4    155  125.6    57    70   69.5   0.0    13.6ms
m23065_2020 5" Race 6S 1300          461.2  14.1  17.8   17.7    6.1    184  145.1    60    93   93.9   0.0    12.2ms
m23065_2450 5" Race 4S 1500 (Preset race5_4s)   407.1  11.2  21.7   16.4    7.3    155  124.7    57    69   75.9   0.0    13.0ms
m2507_1500 6" LR 6S 1800 (Preset lr6_6s)   637.8  10.8  24.2   19.3    9.7    130  107.0    44    59  101.7   8.7    17.3ms
m2507_1800 6" Freestyle 6S 1800      637.8  13.2  19.2   19.6    7.9    143  155.7    65    87  103.8   8.7    17.8ms
m28065_1050 8" LR 8S Li-Ion         1072.3   5.0  21.5   39.5   28.4    103   86.2    33   192  115.7  10.0   W[battery_weak][heavy_sag, battery_overload] 34.2ms
m28065_1300 7" Deadcat 6S Li-Ion (Preset lr7_deadcat_lion)   881.7   4.8  28.0   33.5   25.3    101   75.9    31   169   80.5   0.1   W[battery_weak][heavy_sag, battery_overload] 24.2ms
m2807_1100 8" LR 6S2P Li-Ion (Preset lr8_6s_lion)  1329.7   4.9  33.0   42.1   31.7    109  108.3    42   129  125.6   7.9   W[battery_weak][] 32.6ms
m2807_1500 7" Freestyle 6S 1800      728.4  12.8  17.3   18.7    7.5    131  190.0    79   106  126.2   0.0   W[][heavy_sag] 25.9ms
m3115_640 10" 8S 5000               1945.7   7.7  29.7   26.2   16.1    117  162.3    41    54  130.3   8.2    20.3ms
m3115_900 10" 6S 5000 (Preset xclass10_6s)  1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  110.5  14.7    21.5ms
m3115_1050 10" 6S 5000              1692.5   9.8  21.1   24.9   12.4    122  292.2    73    97  111.7  14.7   W[][heavy_sag] 22.3ms
m4214_330 13" 3 Blatt 8S 5000       3155.7   4.7  42.4   17.4   13.8     80  118.2    29    39  125.3   3.0    22.9ms
m4214_400 13" 8S 5000 (Preset xclass13_8s)  3123.7   5.8  35.9   16.8   12.1     91  169.4    42    56  128.9   3.0    18.8ms
m4214_530 13" 8S 5000               3123.7   7.7  25.7   17.1   10.1    103  293.9    73    98  128.1   3.0   W[][heavy_sag] 19.9ms
```

Bewusste Abweichungen vom Korridor (im Test einzeln benannt, im Tooltip als Contra):
- `m1103_8000`: Schwebegas 23 % — sehr kräftig auf 3S.
- `m12025_11500`: 9,5 min Schweben, `battery_weak` mit 2S 450 mAh 75C — hoher Spitzenstrom (13 A).
- `m2207_1500` wird als 6″-Long-Range gemessen; im 5″-Bash (T/W 8,5, 121 km/h) liegt er bewusst unter dem 5″-Korridor.
- `m2807_1500`: `heavy_sag` (190 A aus 1800 mAh).
- `m3115_640`: 26 / 16 min über dem X-Class-Korridor — effizient.
- `m3115_1050`: T/W 9,8, Schwebegas 21 %, 122 km/h, `heavy_sag` — Race-Motor.
- `m4214_330`: T/W 4,7, Schwebegas 42 % — Cruiser auf 8S, wenig Punch (Datenblatt bis 12S, aber weder JMP noch Propwash haben 10–12S-Akkus oder -ESCs; die Hilfe nennt deshalb nur 8S).
- `m4214_530`: 10,1 min Reise, `heavy_sag` — Race-Motor.

Weitere Nachweise (JUnit): alle 22 Presets aus 8 ohne `sluggish_motors`/`motor_thermal` und unter der Drosselgrenze;
`m2207_2550`, `m22075_2700`, `m23065_2450` melden auf 6S `motor_overvoltage` (auf 4S nicht), werden schneller, ziehen
mehr Strom und brechen ein (`heavy_sag`/`esc_current_limited`); 15 KV-Reihen (auch mit den eingebauten 1404/4600,
2207/1750, 2807/1300) zeigen mit steigender KV mehr Topspeed und mehr Strom; die Selbsttest-Aufbauten mit eingebauten
Teilen bleiben ohne Probleme.

### 3.3 Rezepte

`minecraft:crafting_shaped`, Gruppe `propwash_motor`, Reiter `equipment`; alle Muster spiegelgleich (keine
Spiegel-Kollisionen), eindeutig auch gegen Propwash (`parts_sync.py --check`, JUnit `MotorDataTest`). Zutaten:
`N` Eisennugget, `I` Eisenbarren, `L` `propwash:copper_coil`, `C` Kupferbarren, `Q` Quarz, `O` Goldbarren (Lager),
`B` Kupferblock (Wicklung). Das KV-Kennzeichen `K` steigt mit der KV eines Stators: Amethystsplitter (niedrig,
effizient) → Redstone (Standard) → Glowstone-Staub (hohe KV).

| Stator | Muster | Ausbeute |
|---|---|---|
| 0702 / 0802 / 1102 | `NLN`/` K ` · `NKN`/` L ` · `NLN`/`NKN` | 2 |
| 1103 / 1202,5 / 1404 / 1505 / 2004 | `LKL` + ` N ` · `N N` · `NNN` · `NIN` · `INI` | 2 |
| 2207 / 2207,5 / 2306,5 | `ILI`/`LKL` + ` I ` · `I I` · ` Q ` | 2 |
| 2507 / 2806,5 / 2807 | `ILI`/`LKL` + `ICI` · `CIC` · `CCC` | 2 |
| 3115 | `ILI`/`LKL`/`IOI` | 1 |
| 4214 | `IBI`/`LKL`/`IOI` | 1 |

### 3.4 Truhen-Beute

Eigene Tabellen `justmoreparts:inject/motor/<name>` (ein Pool, ein Wurf, 10–40 % Chance auf einen Motor; kleine Motoren
paarweise, X-Class einzeln und nur in späten Strukturen). Liste der Einspeisungen: `motor.MotorLoot`; seit Parts-API 1.4
meldet `core.JmpLoot` alle Einspeisungen über `PropwashApi.loot()` an, Propwash hängt sie auf Fabric und NeoForge gleich
ein (keine Loader-Modifier mehr).

**Menge je Truhe (02.10. nachgerechnet aus den Tabellen):** Jede Kategorie hängt einen Pool mit großem Leer-Eintrag an;
erwartete JMP-Einträge je Truhe: Motoren 0,10–0,18, Akkus/Stacks 0,06–0,25, Props 0,09–0,19, Rahmen 0,24–0,30 (nur in
den Strukturen der Rahmen-Stufen). Zusammen meist 0,3–0,6 Einträge je Truhe, höchstens etwa 0,9 (antike Stadt,
Endsiedlung); Props kommen zu viert bzw. bei 10–13″ zu zweit. Gegenüber 5–8 Vanilla-Stapeln je Truhe ist das vertretbar,
deshalb unverändert.

| Tabelle | Vanilla-Truhen | Motoren |
|---|---|---|
| `village_house` | Dorfhäuser (Ebene, Wüste, Savanne, Schnee, Taiga) | 0702/23000, 0802/19000, 0802/15500, 1102/13500 |
| `village_toolsmith` | Werkzeugschmied | 1103, 1202,5/5500, 1404/3800, 1102/18000, 0702/27000 |
| `abandoned_mineshaft` | Mine | 1404/2750, 1505, 2004, 1202,5/11500, 2207/1500 |
| `simple_dungeon` | Verlies | 2207/1950, 2207/2550, 2306,5/2450, 1505/3600, 2004 |
| `pillager_outpost` | Außenposten | 2207/1950, 2207,5, 2306,5/2020, 2507/1800 |
| `temple` | Wüsten- und Dschungeltempel | 2306,5, 2207,5/2700, 2207/2550 |
| `shipwreck_supply` | Schiffswrack (Vorräte) | 2507/1500, 2806,5, 2207/1500 |
| `stronghold` | Festung (Gang, Kreuzung) | 2807, 2806,5/1050, 2507/1800, 3115/640 |
| `trial_chambers` | Prüfungskammer (Vorrat, Kreuzung) | 1404/3800, 1505/2500, 2207/1950, 2306,5/2450, 1103/11000 |
| `vault` | Tresor | 3115/900, 3115/640, 2207,5/1850, 2807/1500 |
| `vault_ominous` | Unheilvoller Tresor | 3115/1050, 4214 |
| `ancient_city` | Antike Stadt | 3115/900, 4214/400, 4214/330, 2807/1100 |
| `end_city` | Endsiedlung | 4214/400, 4214/530, 3115/1050, 3115/640 |

### 3.5 Modelle

Seit 1.0.1 entstehen alle Motor-Modelle mit der Stil-API (`tools/assets/style_generate.py --category motor`, Regeln in
`docs/design/asset-style.md` §4.1): `jmpstyle.motor` rechnet aus Stator, Halterung und Akzentfarbe die Werte der
Propwash-Motoren und baut mit `motors.build_motor` der Basis — achteckige Glocke mit Kupfer-Schlitzen, Deckel mit
Speichenfenstern und silberner Nabe (dunkel ab 20 mm Stator), sichtbarer Stator-Ring, Fuß mit vier Laschen ab 11 mm Stator,
Welle 1 mm (Whoop), 1,5 mm (T-Mount), M5 ab 22 mm und 6 mm ab 40 mm Stator. Glockendurchmesser, Fuß, Spalt und
Glockenhöhe über dem Stator laufen durch die Werte der sechs Propwash-Motoren; gleiche Statorgröße ergibt bis auf Zehntelmillimeter
dieselben Maße wie die Basis (1404, 2004, 2207, 2807), KV-Stufen unterscheiden sich nur in der Glockenfarbe. Textur 64 × 64 mit der
Texeldichte der Basis (8/5/4, X-Class-Glocken ab 40 mm 3), Lack-Variante `_paint` im Kanal `motor` wie die Basis (Glocke,
ab 20 mm Stator zusätzlich der Deckel), Item-Definition mit `display_context` und `display`-Block aus
`kit.compute_display` wie die Propwash-Motoren (kein `part_base`-Sonder-Renderer mehr). Die Sitzhöhe (Fuß + Spalt +
Glocke, `motor_seat` in `drone_layout.json`) reicht von 6,75 mm (0702) über 12,5 mm (1404) und 18 mm (2207) bis 31,4 mm
(4214). `--fragments build/fragments/motor` schreibt die Einträge für `drone_paint.json` und `drone_layout.json` zusätzlich
als Fragmente; Vergleichsbilder mit `tools/assets/compare_render.py --category motor` (`--all` für alle 34 Motoren).

Weil gleiche Statorgröße gleiche Geometrie bedeutet, darf ein JMP-Motor nicht die Glockenfarbe des Propwash-Motors
derselben Größe tragen. Nach der Stil-Review 1.0.1 geändert: `m0802_19000` Orange `#f28a26` (vorher Lila wie
`propwash:m0802_22000`), `m2207_1950` Gelb `#e8c22a` (vorher Rot wie `propwash:m2207_1750`), `m28065_1300` Pink `#d84a8c`
(vorher Bernstein wie `propwash:m2807_1300`).

### 3.6 Selbsttests (`motor.client.MotorSelfTests`)

- „motor: 34 Motor-Items mit Namen, Shift-Hilfe und Seltenheit“ (Items, Texte, Seltenheit, Tooltip mit KV-Zeile).
- „motor: je Größenklasse ein Aufbau baubar, als Drohne erzeugt und wieder gelesen“ (Whoop, Cinewhoop, 5″ Freestyle,
  5″ Race, 7″ Long Range mit eingebauten Teilen; X-Class 13″ mit JMP-Teilen, also erst nach der Integration).
- „motor: 4S-Motor auf 6S meldet Überspannung“.
- „motor: Rezepte, Beutetabellen und Truhen-Einspeisung auf dem Server“ (alle Rezepte, alle eigenen Tabellen würfeln nur
  Motoren, jede Ziel-Truhe liefert Motoren; auf Fabric erst nach der Verdrahtung grün).

## 4. Propeller (Implementierer 2 — PROPS)

Id-Regel: `p<Durchmesser mm>_<Steigung×10>_<Blätter>b_<Material>[_duct]`, Material `pc|soft|hard|bi|gfn|cfn|carbon`. ct/cp waren
Startwerte aus der Formel in Abschnitt 1.4; die Tabelle zeigt die **kalibrierten** Werte aus `resources` (Abweichung vom Entwurf höchstens
±15 %, Spalte „Kalibrierung“, Begründung in 4.3). Pro/Contra sind die Shift-Hilfezeilen (`justmoreparts.help.<id>.what.N`); jede Zeile ist
im Prüfstand belegt (4.4). Akzentfarbe = Farbe von Blatt, Nabe, Symbol und FPV-Scheibe; Grundton je Durchmesser, verschoben nach Blattzahl
und Material (weich heller, hart dunkler, Glasfaser grau, Carbon dunkel), innerhalb eines Durchmessers deutlich verschieden.

| Id | Name (de) | Name (en) | Ø mm | Steigung ″ | Blätter | Material | g | ct | cp | Akzent | Einsatz | Pro | Contra | Kalibrierung |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `p31_12_4b_hard` | 31-mm-Propeller (4 Blätter, 1,2″ Steigung, hartes PC) | 31 mm Propeller (4-blade, 1.2″ pitch, hard PC) | 31 | 1,2 | 4 | hartes PC | 0,34 | 0,1 | 0,085 | #b844a3 | 65-mm-Whoop, Race, enge Gates | viel Grip: mehr Schub als 2 und 3 Blätter; hartes PC: steif und präzise | bricht früher als weiches PC; mehr Strom, kürzeste Flugzeit | — |
| `p31_14_3b_bi` | 31-mm-Propeller (3 Blätter, 1,4″ Steigung, Bi-Blend) | 31 mm Propeller (3-blade, 1.4″ pitch, bi-blend) | 31 | 1,4 | 3 | Bi-Blend | 0,32 | 0,09 | 0,072 | #f2609e | 65-mm-Whoop, Allround und Racing | Bi-Blend: steif und zäh zugleich; mehr Grip als 2, länger als 4 Blätter | etwas schwerer als der Standard-Dreiblatt | — |
| `p31_16_2b_pc` | 31-mm-Propeller (2 Blätter, 1,6″ Steigung, PC) | 31 mm Propeller (2-blade, 1.6″ pitch, PC) | 31 | 1,6 | 2 | PC | 0,22 | 0,0802 | 0,0569 | #ff5f6d | 65-mm-Whoop, ruhige lange Flüge | längste Flugzeit der 31-mm-Props; wenig Strom, der Motor bleibt kühl; leichtestes Blatt: schnellste Reaktion | weniger Schub als 3 und 4 Blätter | cp 0,0632 → 0,0569 (-10 %) |
| `p40_14_5b_soft` | 40-mm-Propeller (5 Blätter, 1,4″ Steigung, weiches PC) | 40 mm Propeller (5-blade, 1.4″ pitch, soft PC) | 40 | 1,4 | 5 | weiches PC | 0,6 | 0,1787 | 0,1955 | #df5fa0 | Indoor-Whoop, Bando, viele Crashs | mehr Schub als 2 und 4 Blätter; weiches PC: übersteht viele Einschläge | weich: wackelt, verliert unter Last Schub; viel Strom: am 1S-AIO strombegrenzt | 1.0.0 wie die Familie ct × 1,7, cp × 1,7^1,5 (§13.2) |
| `p40_15_4b_pc` | 40-mm-Propeller (4 Blätter, 1,5″ Steigung, PC) | 40 mm Propeller (4-blade, 1.5″ pitch, PC) | 40 | 1,5 | 4 | PC | 0,52 | 0,1673 | 0,1809 | #a65ce0 | 75–85-mm-Whoop, Grip in Kurven | mehr Schub als der Zweiblatt | kürzere Flugzeit als 2 und 3 Blätter; mehr Strom als der Zweiblatt; auf 1S kaum mehr Schub als 3 Blätter | 1.0.0 wie die Familie ct × 1,7, cp × 1,7^1,5 (§13.2) |
| `p40_17_3b_pc` | 40-mm-Propeller (3 Blätter, 1,7″ Steigung, PC) | 40 mm Propeller (3-blade, 1.7″ pitch, PC) | 40 | 1,7 | 3 | PC | 0,42 | 0,1555 | 0,1625 | #d65ce0 | 75-mm-Whoop 1S, Allround | länger in der Luft als 4 und 5 Blätter; mehr Schub als der Zweiblatt | kürzere Flugzeit als 2 Blätter | ct 0,0889 → 0,0915 (+3 %, Realismus-Review); 1.0.5 Referenz-Beiwerte ct × 1,7, cp × 1,7^1,5 (§13) |
| `p40_20_2b_pc` | 40-mm-Propeller (2 Blätter, 2,0″ Steigung, PC) | 40 mm Propeller (2-blade, 2.0″ pitch, PC) | 40 | 2 | 2 | PC | 0,3 | 0,1367 | 0,1332 | #e04f9a | 75-mm-Whoop, lange Flüge draußen | längste Flugzeit der 40-mm-Props; wenig Strom, schnelle Motorreaktion | am wenigsten Schub der 40-mm-Props | cp 0,0668 → 0,0601 (-10 %); 1.0.0 wie die Familie ct × 1,7, cp × 1,7^1,5 (§13.2) |
| `p45_17_4b_bi` | 45-mm-Propeller (4 Blätter, 1,7″ Steigung, Bi-Blend) | 45 mm Propeller (4-blade, 1.7″ pitch, bi-blend) | 45 | 1,7 | 4 | Bi-Blend | 0,68 | 0,0988 | 0,0824 | #8a7af0 | 85-mm-Whoop 2S, draußen bei Wind | mehr Schub, längere Flugzeit als 3 Bl.; Bi-Blend: steif und zäh zugleich | mehr Strom, Motoren etwas wärmer; schwerer: Motoren reagieren träger | — |
| `p45_19_3b_pc` | 45-mm-Propeller (3 Blätter, 1,9″ Steigung, PC) | 45 mm Propeller (3-blade, 1.9″ pitch, PC) | 45 | 1,9 | 3 | PC | 0,55 | 0,0894 | 0,0761 | #9b59f0 | 85-mm-Whoop 2S, leichte Toothpicks | reagiert schneller als der Vierblatt; weniger Strom als 4 Blätter | weniger Schub und Flugzeit als 4 Blätter | — |
| `p51_18_4b_pc` | 2-Zoll-Propeller (4 Blätter, 1,8″ Steigung, PC) | 2″ Propeller (4-blade, 1.8″ pitch, PC) | 51 | 1,8 | 4 | PC | 1,05 | 0,0939 | 0,0784 | #3feed9 | 2″-Toothpick, enge Linien | mehr Schub als der Dreiblatt; schwebt etwas länger (flachere Steigung) | mehr Strom, Motoren wärmer; schwerer: Motoren reagieren träger | — |
| `p51_20_3b_pc` | 2-Zoll-Propeller (3 Blätter, 2,0″ Steigung, PC) | 2″ Propeller (3-blade, 2.0″ pitch, PC) | 51 | 2 | 3 | PC | 0,85 | 0,087 | 0,0724 | #3fc3ee | 2″-Toothpick 2S | reagiert schneller als der Vierblatt; weniger Strom, Motoren kühler | etwas weniger Schub als 4 Blätter | — |
| `p65_22_4b_bi` | 2,6-Zoll-Propeller (4 Blätter, 2,2″ Steigung, Bi-Blend) | 2.6″ Propeller (4-blade, 2.2″ pitch, bi-blend) | 65 | 2,2 | 4 | Bi-Blend | 1,55 | 0,1874 | 0,1961 | #57d399 | 2,5″-Cinewhoop 4S, drinnen filmen | im Duct ein Viertel mehr Schub als 3 Bl.; Bi-Blend: steif und zäh zugleich | mehr Strom, Motoren wärmer; überlastet 1202,5er (Hitze, Einbruch) | 1.0.5 Referenz-Beiwerte ct × 2,2, cp × 2,2^1,5 (§13); 1.0.0 ct × 2,0, Gütezahl 0,33 (§13.2) |
| `p65_25_3b_pc` | 2,6-Zoll-Propeller (3 Blätter, 2,5″ Steigung, PC) | 2.6″ Propeller (3-blade, 2.5″ pitch, PC) | 65 | 2,5 | 3 | PC | 1,3 | 0,114 | 0,0877 | #1fc4b4 | 2,5″-Toothpick 3–4S | leichter, längere Flugzeit als 4 Bl.; Motoren kühler als mit dem Vierblatt | im Duct weniger Schub als 4 Blätter | 1.0.0 ct × 1,3, Gütezahl 0,35 (§13.2) |
| `p76_24_5b_pc_duct` | 3-Zoll-Propeller (5 Blätter, 2,4″ Steigung, PC, für Ducts) | 3″ Propeller (5-blade, 2.4″ pitch, PC, for ducts) | 76 | 2,4 | 5 | PC | 2,6 | 0,1013 | 0,0933 | #a593ff | 3″-Cinewhoop mit Ducts | am meisten Schub der 3″-Props; im Duct länger in der Luft als Standard | hoher Strom, Motoren am heißesten; schwer: Motoren reagieren träge | — |
| `p76_25_3b_hard` | 3-Zoll-Propeller (3 Blätter, 2,5″ Steigung, hartes PC) | 3″ Propeller (3-blade, 2.5″ pitch, hard PC) | 76 | 2,5 | 3 | hartes PC | 1,5 | 0,0822 | 0,0671 | #2a59b8 | 3″-Toothpick 4S, Freestyle leicht | länger in der Luft als der Standard-3″; hartes PC: steif und präzise | bricht früher als weiches PC | — |
| `p76_30_2b_pc` | 3-Zoll-Propeller (2 Blätter, 3,0″ Steigung, PC) | 3″ Propeller (2-blade, 3.0″ pitch, PC) | 76 | 3 | 2 | PC | 0,8 | 0,075 | 0,0564 | #413aff | 3″-Toothpick, lange Flüge | längste Flugzeit der 3″-Props; wenig Strom, Motoren bleiben kühl | weniger Schub als 3 und 5 Blätter | cp 0,0627 → 0,0564 (-10 %); 1.0.5 Masse 1,2 → 0,8 g (Gemfan 3018) |
| `p89_25_5b_pc_duct` | 3,5-Zoll-Propeller (5 Blätter, 2,5″ Steigung, PC, für Ducts) | 3.5″ Propeller (5-blade, 2.5″ pitch, PC, for ducts) | 89 | 2,5 | 5 | PC | 3,3 | 0,2662 | 0,3048 | #96ef8a | 3,5″-Cinewhoop 6S mit Action-Cam | im Duct gut 40 % mehr Schub als 3 Bl. | doppelt so viel Strom wie 3 Bl., wärmer; schwer: Motoren reagieren träger; kürzer in der Luft als der Dreiblatt | 1.0.5 Referenz-Beiwerte ct × 2,5, cp × 2,5^1,5 (§13); 1.1.0 cp × 1,012 (§13.4) |
| `p89_30_3b_pc` | 3,5-Zoll-Propeller (3 Blätter, 3,0″ Steigung, PC) | 3.5″ Propeller (3-blade, 3.0″ pitch, PC) | 89 | 3 | 3 | PC | 2,3 | 0,1205 | 0,0855 | #9be22b | 3,5″-Toothpick 4–6S | im 3,5″ auf 6S: T/W fast 9, 115 km/h; leichter, reagiert schneller als 5 Bl.; im Duct halber Strom, länger in der Luft | im Duct 30 % weniger Schub als 5 Blätter | 1.0.0 (02.10.): ct × 1,3, Gütezahl 0,395 (§13.3); 1.1.0 cp × 1,012, Gütezahl 0,390 (§13.4) |
| `p102_25_2b_pc` | 4-Zoll-Propeller (2 Blätter, 2,5″ Steigung, PC) | 4″ Propeller (2-blade, 2.5″ pitch, PC) | 102 | 2,5 | 2 | PC | 2 | 0,13 | 0,0763 | #4fbf83 | 4″-Long-Range unter 250 g | länger in der Luft als 3 Blätter; ein Viertel weniger Strom, kühler | weniger Schub und Topspeed als 3 Bl. | ct 0,072 → 0,0763 (+6 %); 1.0.0 Referenz Gemfan 4024-2 (§13.2) |
| `p102_30_3b_gfn` | 4-Zoll-Propeller (3 Blätter, 3,0″ Steigung, Glasfaser-Nylon) | 4″ Propeller (3-blade, 3.0″ pitch, glass-fiber nylon) | 102 | 3 | 3 | Glasfaser-Nylon | 2,9 | 0,1703 | 0,1268 | #5c8a6a | 4″-Long-Range, leichter Freestyle | mehr Schub und Topspeed als 2 Blätter; Glasfaser-Nylon: steif | splittert statt zu biegen; mehr Strom, kürzere Flugzeit | 1.0.5 Referenz-Beiwerte ct × 1,8, cp × 1,8^1,5 (§13) |
| `p130_35_4b_pc` | 5,1-Zoll-Propeller (4 Blätter, 3,5″ Steigung, PC) | 5.1″ Propeller (4-blade, 3.5″ pitch, PC) | 130 | 3,5 | 4 | PC | 5,2 | 0,1191 | 0,0522 | #ff381f | 5″-Cinematic mit Action-Cam | am meisten Schub der 5,1″-Props; länger in der Luft als 5,1×4,6 | weniger Topspeed als 5,1×4,6; schwerster 5,1″: Motoren reagieren träger | — |
| `p130_36_3b_bi` | 5,1-Zoll-Propeller (3 Blätter, 3,6″ Steigung, Bi-Blend) | 5.1″ Propeller (3-blade, 3.6″ pitch, bi-blend) | 130 | 3,6 | 3 | Bi-Blend | 4,2 | 0,1062 | 0,0436 | #f0ad55 | 5″-Freestyle, flüssige Linien | länger in der Luft, weniger Strom als 4,6″; Bi-Blend: steif und zäh, Motoren kühl | weniger Topspeed als 5,1×4,6 | — |
| `p130_40_2b_pc` | 5,1-Zoll-Propeller (2 Blätter, 4,0″ Steigung, PC) | 5.1″ Propeller (2-blade, 4.0″ pitch, PC) | 130 | 4 | 2 | PC | 3,2 | 0,0932 | 0,0332 | #ffdc1f | 5″-Long-Range, ruhiges Cruisen | längste Flugzeit der 5,1″-Props; ein Drittel weniger Strom als 4,6″; leicht: schnellste Motorreaktion | wenig Grip: kleinster Schub der 5,1″ | cp 0,0369 → 0,0332 (-10 %) |
| `p130_41_3b_soft` | 5,1-Zoll-Propeller (3 Blätter, 4,1″ Steigung, weiches PC) | 5.1″ Propeller (3-blade, 4.1″ pitch, soft PC) | 130 | 4,1 | 3 | weiches PC | 4,4 | 0,1133 | 0,0517 | #f08c35 | 5″-Bando, Freestyle mit Crashs | weiches PC: übersteht viele Einschläge; länger in der Luft als 5,1×4,6 | weich: wackelt im Propwash; verliert unter Last Schub | — |
| `p130_46_3b_hard` | 5,1-Zoll-Propeller (3 Blätter, 4,6″ Steigung, hartes PC) | 5.1″ Propeller (3-blade, 4.6″ pitch, hard PC) | 130 | 4,6 | 3 | hartes PC | 4,1 | 0,12 | 0,06 | #b86316 | 5″-Race, schnelles Freestyle | hartes PC: präzise, leichter als Standard; mehr Topspeed als 3,6″ und 4,1″ | bricht früher als weiches PC; mehr Strom als flachere Props | — |
| `p130_50_3b_gfn` | 5,1-Zoll-Propeller (3 Blätter, 5,0″ Steigung, Glasfaser-Nylon) | 5.1″ Propeller (3-blade, 5.0″ pitch, glass-fiber nylon) | 130 | 5 | 3 | Glasfaser-Nylon | 4,6 | 0,1251 | 0,0669 | #9f7653 | 5″-Race 6S, lange Geraden | höchster Topspeed der 5,1″-Props; Glasfaser-Nylon: sehr steif | höchster Strom, Motoren am heißesten; kürzeste Flugzeit, splittert | — |
| `p152_33_3b_pc` | 6-Zoll-Propeller (3 Blätter, 3,3″ Steigung, PC) | 6″ Propeller (3-blade, 3.3″ pitch, PC) | 152 | 3,3 | 3 | PC | 5,6 | 0,1012 | 0,0399 | #ffd23a | 6″-Long-Range 6S | PC hält Crashs besser aus als Glasfaser; auf 6S-LiPo mehr Schub als der Zweiblatt | kürzer in der Luft, mehr Strom als 2 Bl. | — |
| `p152_40_2b_gfn` | 6-Zoll-Propeller (2 Blätter, 4,0″ Steigung, Glasfaser-Nylon) | 6″ Propeller (2-blade, 4.0″ pitch, glass-fiber nylon) | 152 | 4 | 2 | Glasfaser-Nylon | 4,4 | 0,0928 | 0,0325 | #95a35d | 6″-Long-Range, Reiseflug | länger in der Luft, weniger Strom; schneller geradeaus als der Dreiblatt; Glasfaser-Nylon: steif | splittert statt zu biegen | cp 0,0378 → 0,0325 (-14 %) |
| `p178_35_3b_pc` | 7-Zoll-Propeller (3 Blätter, 3,5″ Steigung, PC) | 7″ Propeller (3-blade, 3.5″ pitch, PC) | 178 | 3,5 | 3 | PC | 8 | 0,108 | 0,0488 | #ff4a3d | 7″-Long-Range, Cinematic | PC hält Crashs besser aus als Faser-Nylon | mehr Strom, kürzer in der Luft als 2 Bl.; schwerer: reagiert träger als 2 Blätter | — |
| `p178_40_2b_cfn` | 7-Zoll-Propeller (2 Blätter, 4,0″ Steigung, Carbonfaser-Nylon) | 7″ Propeller (2-blade, 4.0″ pitch, carbon-fiber nylon) | 178 | 4 | 2 | Carbonfaser-Nylon | 7,2 | 0,0962 | 0,0385 | #826451 | 7″-Long-Range, Reiseflug | längste Schwebezeit der 7″-Props (Li-Ion); schneller geradeaus als 3 Blätter; Carbonfaser-Nylon: sehr steif | bricht eher als PC (Haltbarkeit 0,9) | cp 0,0428 → 0,0385 (-10 %) |
| `p178_45_3b_gfn` | 7-Zoll-Propeller (3 Blätter, 4,5″ Steigung, Glasfaser-Nylon) | 7″ Propeller (3-blade, 4.5″ pitch, glass-fiber nylon) | 178 | 4,5 | 3 | Glasfaser-Nylon | 8,8 | 0,1225 | 0,0576 | #9f5e5e | 7″-Freestyle auf 6S-LiPo | auf 6S-LiPo am meisten Schub und Tempo; Glasfaser-Nylon: steif | höchster Strom, Motoren überhitzen; kürzer in der Luft als der Zweiblatt | cp 0,0677 → 0,0576 (-15 %) |
| `p203_40_3b_gfn` | 8-Zoll-Propeller (3 Blätter, 4,0″ Steigung, Glasfaser-Nylon) | 8″ Propeller (3-blade, 4.0″ pitch, glass-fiber nylon) | 203 | 4 | 3 | Glasfaser-Nylon | 11 | 0,1081 | 0,0470 | #8a7664 | 8″-Long-Range, lange Schwebeflüge | mehr Schub, schwebt länger als 2 Blätter; Glasfaser-Nylon: steif | langsamer geradeaus, mehr Strom als 2 Bl.; schwerer, Motoren heißer als mit 2 Bl. | cp 0,0489 → 0,0509 → 0,0470 (Realismus-Review: eigene Nische statt dominiert) |
| `p203_45_2b_cfn` | 8-Zoll-Propeller (2 Blätter, 4,5″ Steigung, Carbonfaser-Nylon) | 8″ Propeller (2-blade, 4.5″ pitch, carbon-fiber nylon) | 203 | 4,5 | 2 | Carbonfaser-Nylon | 9,2 | 0,0956 | 0,0417 | #828062 | 8″-Long-Range 6S2P Li-Ion | schneller geradeaus, kühler, weniger Strom; leichter: Motoren reagieren schneller; Carbonfaser-Nylon: sehr steif | weniger Schub, schwebt kürzer als 3 Bl. | cp 0,0421 → 0,0417 (-1 %) |
| `p254_45_3b_cfn` | 10-Zoll-Propeller (3 Blätter, 4,5″ Steigung, Carbonfaser-Nylon) | 10″ Propeller (3-blade, 4.5″ pitch, carbon-fiber nylon) | 254 | 4,5 | 3 | Carbonfaser-Nylon | 18 | 0,1025 | 0,0452 | #6f7682 | 10″ X-Class 6S | günstiger als Vollcarbon: ohne Quarz; Carbonfaser-Nylon: sehr steif | mehr Strom, Motoren heißer als 2 Bl.; schwerer: Motoren reagieren träger | cp 0,0426 → 0,0452 (+6 %) |
| `p254_50_2b_carbon` | 10-Zoll-Propeller (2 Blätter, 5,0″ Steigung, Carbon) | 10″ Propeller (2-blade, 5.0″ pitch, carbon) | 254 | 5 | 2 | Vollcarbon | 14 | 0,0901 | 0,0361 | #6f7482 | 10″ X-Class, Race und Reise | Vollcarbon: extrem steif und zäh; länger in der Luft, weniger Strom; schneller, Motoren kühler als 3 Bl. | teuer: Quarz und Gold im Rezept | — |
| `p305_60_2b_cfn` | 12-Zoll-Propeller (2 Blätter, 6,0″ Steigung, Carbonfaser-Nylon) | 12″ Propeller (2-blade, 6.0″ pitch, carbon-fiber nylon) | 305 | 6 | 2 | Carbonfaser-Nylon | 22 | 0,09 | 0,036 | #737882 | 12″ X-Class 8S | Carbonfaser-Nylon: sehr steif; leichter als 13″: reagiert schneller | weniger Schub als 13″-Props | — |
| `p330_60_3b_cfn` | 13-Zoll-Propeller (3 Blätter, 6,0″ Steigung, Carbonfaser-Nylon) | 13″ Propeller (3-blade, 6.0″ pitch, carbon-fiber nylon) | 330 | 6 | 3 | Carbonfaser-Nylon | 36 | 0,1039 | 0,0441 | #6f7582 | 13″ X-Class H-Frame, schwere Last | am meisten Schub der X-Class-Props; Carbonfaser-Nylon: sehr steif | schwerster Prop: Motoren reagieren träge; mehr Strom, Motoren heißer | — |
| `p330_70_2b_carbon` | 13-Zoll-Propeller (2 Blätter, 7,0″ Steigung, Carbon) | 13″ Propeller (2-blade, 7.0″ pitch, carbon) | 330 | 7 | 2 | Vollcarbon | 28 | 0,0935 | 0,0365 | #6e7482 | 13″ X-Class 8S, Race | Vollcarbon: extrem steif und zäh; länger in der Luft, schneller als 3 Bl. | teuer: Quarz und Gold im Rezept; weniger Schub als der Dreiblatt | cp 0,0397 → 0,0365 (-8 %) |

### 4.1 Materialien (soft/hard/bi-blend)

| Kürzel | JSON `material` | Steifigkeit | Haltbarkeit | Wirkung im Flugmodell | Tooltip |
|---|---|---|---|---|---|
| `pc` | `polycarbonate` | 1,0 (Standard) | 1,0 | Referenz | „Polycarbonat“ |
| `soft` | `polycarbonate` + `stiffness` 0,85, `durability` 1,35 | 0,85 | 1,35 | mehr Propwash-Rauschen (÷√s), Schubverlust unter hoher Last (T/W sinkt gegenüber einem steifen Zwilling), übersteht Einschläge | „+ übersteht viele Einschläge / − wackelt, verliert unter Last Schub“ |
| `hard` | `polycarbonate` + 1,12 / 0,8 | 1,12 | 0,8 | präziser, bricht früher | „+ steif und präzise / − bricht früher als weiches PC“ |
| `bi` | `polycarbonate` + 1,1 / 1,15 | 1,1 | 1,15 | Bi-Blend: steife Blätter, zähe Nabe (Textur zweifarbig) | „+ steif und zäh zugleich“ (Contra: etwas schwerer) |
| `gfn` | `glass_fiber_nylon` | 1,2 | 0,8 | steif, splittert | „+ steif / − splittert statt zu biegen“ |
| `cfn` | `carbon_fiber_nylon` | 1,4 | 0,9 | sehr steif, großer Prop | „+ sehr steif / − Carbon im Rezept, bricht eher als PC“ |
| `carbon` | `carbon` | 2,2 | 1,4 | extrem steif, kaum Flattern | „+ extrem steif und zäh / − teuer: Quarz und Gold im Rezept“ |

Die Materialwerte `soft`/`hard`/`bi` setzen `stiffness` und `durability` im JSON ausdrücklich (Material bleibt
`polycarbonate`, Propwash kennt nur vier Materialien). Lautstärke/Klang je Material ist in API 1.0 nicht einstellbar
(API-Wunsch W4). JUnit `PropBenchTest#materialsFollowTheCatalogTable` prüft jede Datei gegen diese Tabelle (und Ø, Steigung,
Blattzahl gegen die Id).

### 4.2 Duct-Props
`_duct`-Props (3″ 5-Blatt, 3,5″ 5-Blatt) sind für Cinewhoop-Rahmen gedacht: viele Blätter, wenig Steigung, hoher Standschub, im
Duct längere Schwebezeit als ihr Gegenstück. Propwash kann nicht erzwingen, dass sie nur in Ducts fliegen; offen geflogen zeigt der
Prüfstand den Nachteil (höchster Strom, heißeste Motoren, träge Motorreaktion), aber **keinen** Topspeed-Verlust gegenüber flacheren
Props — das Contra „wenig Topspeed“ aus dem Entwurf wurde deshalb gestrichen. Den Duct-Gewinn liefert der Rahmen (`duct`), nicht der
Prop (API-Wunsch W2). Für den 3″-Duct-Prop gibt es in JMP keinen 3″-Cinewhoop-Rahmen; seine Duct-Aussagen misst der Prüfstand im
eingebauten `propwash:cinewhoop_3` gegen `propwash:p3_3b`, den Korridor im offenen `tp_3_x`.

### 4.3 Kalibrierung (Prüfstand, Propwash 0.4.0)
Das Beiwert-Modell (1.4) gibt 2-Blatt-Props gleicher Größe **keinen** Wirkungsgradvorteil (ct^1,5/cp ∝ B^−0,075), die höhere Steigung
der Zweiblätter kostet im Stand sogar Wirkungsgrad (P/D^−0,55). Mit den Entwurfswerten flogen deshalb 9 der 10 Zweiblätter mit Dreiblatt-Gegenstück **kürzer**
als der Dreiblatt gleicher Größe. Kalibriert wurde paarweise und minimal:
- **Zweiblatt-Bonus** (Blattinterferenz, in Windkanalreihen ≈ 5–10 % Wirkungsgrad): cp −8 bis −14 % bei `p31_16`, `p40_20`, `p76_30`,
  `p130_40`, `p152_40`, `p178_40`, `p330_70`; `p203_45` nur −1 % (sonst verlässt das Preset `lr8_6s_lion` mit 32,8 min den
  Reise-Korridor); `p254_50` unverändert (Korridor X-Class: Schweben ≤ 25 min).
- **Dreiblatt-Malus** statt Bonus, wo der Zweiblatt am Korridor anliegt: `p254_45` cp +6 %, `p203_40` cp +4 %.
- `p102_25_2b_pc` ct +6 %: sonst T/W 3,2 im 4″-Long-Range (Korridor ≥ 3,5).
- `p178_45_3b_gfn` cp −15 %: mit Entwurfswerten war der 7″-Freestyle-Dreiblatt überall schlechter als alle anderen 7″-Props (weniger Schub,
  heißer, kürzer); jetzt hat er auf 6S-LiPo den meisten Schub und Topspeed der 7″-Props — bei höchstem Strom und überhitzten Motoren.
- Damals offener Punkt für Implementierer 1, **erledigt** (§3.1): `lr8_6s_lion` lag mit `m2807_1100` über der Drosselgrenze
  (152 °C mit `p203_45_2b_cfn`, 140 °C erlaubt); die Fahrtwind-Kühlung des `m2807_1100` wurde angehoben, heute 96 °C bei
  Dauervollgas im Stand ohne `motor_thermal`.

### 4.4 Prüfstand (JUnit `prop/PropBenchTest`, `prop/PropDataTest`)
- Jeder Prop hat genau einen Referenzaufbau und liegt dort im Zielkorridor 1.7 (Masse, T/W, Schwebegas, Schwebe-/Reisezeit, Topspeed,
  Motor unter der Drosselgrenze bzw. Klassengrenze, Props im Bild, nur erlaubte Warnungen). Ausnahmen, je begründet: 4″ mit
  Semi-Solid-Akku 427–430 g (Korridor-Untergrenze 440 g gilt für Li-Ion, mit dem der 2-Blatt nur T/W 3,4 hätte); 8″ im Preset-Aufbau
  ohne Motortemperatur (4.3); `p254_45_3b_cfn` Schwebegas 24 % (mehr Schub als der Zweiblatt, Korridor ab 25 %).
- Jede `+`/`−`-Zeile jeder Shift-Hilfe hat genau eine Prüfung (`everyProAndConLineIsBackedByTheBench` zählt die Zeilen in `de_de` und
  vergleicht sie mit den Prüfungen; Zeile ohne Prüfung oder Prüfung ohne Zeile = Fehler). Gemessen wird immer im selben Aufbau gegen
  die genannten Vergleichs-Props, eingebaute Propwash-Props (`propwash:p31_4b`, `p3_3b`, `p51_3b`, `p7_2b`) eingeschlossen.
- Nachweise aus der Architektur: 2-Blatt fliegt länger (Schweben und Reise) und braucht weniger Strom als der 3-Blatt gleicher Größe
  (12 Paare); 4/5-Blatt haben mehr Schub und mehr Strom als der 3-Blatt (12 Paare); höhere Steigung gibt mehr Topspeed und mehr Strom
  (5,1″-Reihe 3,6 → 4,1 → 4,6 → 5,0″ im JMP-5″ und im eingebauten Racer); weiches PC hat Haltbarkeit > 1,3, Steifigkeit < 1 (mehr
  Propwash-Rauschen) und verliert unter Last Schub gegenüber einem steifen Zwilling im Namensraum `jmp_test`.
- `PropDataTest`: Rezepte (Blattzahl im Muster, Farbstoff je Größe, Nabe je Klasse, Menge, Eindeutigkeit auch gespiegelt),
  Truhen-Beute (jede Einspeisung von `PropLoot` hat eine Tabelle, jede Tabelle mit großem Leer-Eintrag, jeder Prop mindestens einmal), Sprachdateien
  (Schlüssel, 48 Zeichen, „−“, „″“, Umlaute), Modelle/Texturen/Symbole aller Varianten und Lack-Standardfarbe = Akzentfarbe, Seltenheit.

Referenzaufbauten (Ausgabe von `everyPropHasAReferenceBuildInsideItsCorridor`, Spalten wie `Bench.header()`):

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
p31_16_2b_pc @ 65 mm 1S               25.7   4.1  46.1   11.7    9.3     41    9.7    39    57   29.8  16.7   
p31_14_3b_bi @ 65 mm 1S               26.0   4.3  44.1   11.1    8.6     41   11.2    45    66   31.0  16.9   
p31_12_4b_hard @ 65 mm 1S             26.2   4.4  42.1   10.9    8.4     42   12.4    50    72   32.0  17.2   
p40_20_2b_pc @ 75 mm 1S               32.4   4.0  45.8   11.9    9.5     39   11.3    46    42   34.3  19.6   
p40_17_3b_pc @ 75 mm 1S               32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  19.9   
p40_15_4b_pc @ 85 mm 2S               56.1   5.5  35.6   11.4    7.8     52   20.8    43    62   37.9   6.3   
p40_14_5b_soft @ 85 mm 2S             56.4   5.7  34.4   11.5    7.8     52   21.7    45    64   38.8   6.5   
p45_19_3b_pc @ 85 mm 2S               56.2   6.4  29.3   11.8    7.3     56   27.4    57    81   45.4   9.4   
p45_17_4b_bi @ 85 mm 2S               56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0   
p51_20_3b_pc @ 2″ 2S                  57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   43.6  10.7   
p51_18_4b_pc @ 2″ 2S                  57.9   6.4  29.3   11.1    7.2     74   29.8    62    88   44.8  10.7   
p65_25_3b_pc @ 2,5″ 4S                94.8   6.9  25.8   11.8    6.7     89   35.0    44    97  119.8  12.7   
p65_22_4b_bi @ CW 2,5″ 4S            193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2   
p76_30_2b_pc @ 3″ 4S                 141.6   6.4  34.3   11.3    7.2     93   34.5    43    56   71.9   8.4   
p76_25_3b_hard @ 3″ 4S               142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4   
p76_24_5b_pc_duct @ 3″ 4S            147.2   6.9  30.3   10.6    6.4     93   46.6    58    76   96.8   8.5   
p89_30_3b_pc @ 3,5″ 6S               222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5   
p89_25_5b_pc_duct @ CW 3,5″ 6S       339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6   
p102_25_2b_pc @ LR 4″ 6S SS          426.6   4.4  48.7   37.6   31.2     98   23.9    13    32   80.1   7.9   
p102_30_3b_gfn @ LR 4″ 6S SS         430.2   4.8  44.4   32.6   25.4    103   35.0    19    47  103.0   8.1   
p130_36_3b_bi @ 5″ 6S                516.5  12.1  21.7   15.8    6.8    142  112.5    47    72   88.6   8.2   
p130_41_3b_soft @ 5″ 6S              517.3  12.0  20.9   15.3    6.3    145  124.7    52    80   92.3   8.2   
p130_46_3b_hard @ 5″ 6S              516.1  12.2  20.2   14.8    5.8    148  136.0    57    87   95.9   8.2   
p130_50_3b_gfn @ 5″ 6S               518.1  12.1  19.8   14.4    5.6    150  144.6    60    93   98.7   8.2   
p130_35_4b_pc @ 5″ 6S                520.5  12.6  20.3   15.9    6.5    143  125.4    52    80   92.5   8.3   
p130_40_2b_pc @ 5″ 6S                512.5  11.7  23.3   16.3    7.2    145   94.4    39    61   83.6   8.1   
p152_33_3b_pc @ LR 6″ Li-Ion         657.8   5.5  27.3   29.0   21.3    101   59.1    24   164   81.8   8.6   W[battery_weak][heavy_sag, battery_overload]
p152_40_2b_gfn @ LR 6″ Li-Ion        653.0   5.8  28.4   30.2   21.7    107   55.4    23   154   80.0   8.5   W[battery_weak][heavy_sag, battery_overload]
p178_35_3b_pc @ LR 7″ Li-Ion         884.9   4.8  26.2   34.8   26.0     98   78.6    33   175  100.4   0.1   W[battery_weak][heavy_sag, battery_overload]
p178_40_2b_cfn @ LR 7″ Li-Ion        881.7   5.2  27.7   36.4   26.7    104   73.7    31   164   95.0   0.1   W[battery_weak][heavy_sag, battery_overload]
p178_45_3b_gfn @ LR 7″ Li-Ion        888.1   4.9  24.5   35.6   26.2    101   81.7    34   182  104.4   0.1   W[battery_weak][heavy_sag, battery_overload]
p203_40_3b_gfn @ LR 8″ 6S1P          938.9   5.2  24.6   38.5   27.9     93   78.6    30   175  109.6  12.0   W[battery_weak][heavy_sag, battery_overload]
p203_45_2b_cfn @ LR 8″ 6S1P          931.7   5.5  26.0   39.9   28.5     98   74.1    28   165  103.2  12.0   W[battery_weak][heavy_sag, battery_overload]
p254_45_3b_cfn @ X-Class 10″        1708.5   8.7  24.0   23.8   13.3    111  249.1    62    83  138.3  14.7   
p254_50_2b_carbon @ X-Class 10″     1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  126.4  14.7   
p305_60_2b_cfn @ X-Class 12″        2629.7   5.5  39.2   19.7   14.8     91  121.5    30    40  114.5   5.4   
p330_70_2b_carbon @ X-Class 13″     3123.7   5.9  35.7   18.0   12.8     92  160.4    40    53  125.9   3.0   
p330_60_3b_cfn @ X-Class 13″        3155.7   6.1  34.0   17.5   12.3     89  181.0    45    60  132.9   3.0   
```

Presets aus Abschnitt 8 mit den kalibrierten Props (übrige Kategorien: Entwürfe; nur `lr7_deadcat_lion`, `lr8_6s_lion`, `xclass13_8s`
ändern sich gegenüber 8.1):

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
whoop75_1s                            32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  19.9   
whoop85_2s                            56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0   
toothpick2_2s                         57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   43.6  10.7   
toothpick3_4s                        142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4   
toothpick35_6s                       222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5   
cinewhoop25_4s                       193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2   
cinewhoop35_6s                       339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6   
bash5_6s                             530.3  11.7  21.2   14.9    6.2    140  124.7    52    80   92.3   5.8   
squashed5_6s                         515.5  12.2  21.6   15.8    6.7    139  112.5    47    72   88.6   4.3   
deadcat5_6s                          534.3  11.1  23.6   15.0    7.0    132  101.4    42    65   85.2   0.3   
race5_4s                             407.1  11.2  21.7   16.4    7.3    155  124.7    57    69   75.9   0.0   
race5_6s                             462.9  14.1  17.4   17.2    5.8    187  154.1    64    99   96.9   0.0   W[][heavy_sag]
hframe5_6s                           540.5  12.1  20.8   15.3    6.3    127  125.4    52    80   92.5   0.0   
lr4_6s_lion                          442.8   3.6  51.0   28.3   24.8     91   27.6    15    77   87.7   8.3   W[battery_weak][]
lr6_6s                               637.8  10.8  24.2   19.3    9.7    130  107.0    44    59  101.7   8.7   
lr7_deadcat_lion                     881.7   5.2  27.7   36.4   26.7    104   73.7    31   164   95.0   0.1   W[battery_weak][heavy_sag, battery_overload]
lr8_6s_lion                         1329.7   5.0  32.8   44.0   32.8    111  106.4    41   127  149.0   7.9   W[battery_weak][motor_thermal]
xclass10_6s                         1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  126.4  14.7   
xclass12_8s                         2629.7   5.5  39.2   19.7   14.8     91  121.5    30    40  114.5   5.4   
xclass13_8s                         3123.7   5.9  35.7   18.0   12.8     92  160.4    40    53  125.9   3.0   
xclass13h_8s                        3355.7   5.7  35.4   16.1   11.6     83  181.0    45    60  132.9   0.0   
```

### 4.5 Rezepte, Seltenheit, Truhen-Beute, Modelle
- **Rezept** (`tools/props/prop_data.py`, geformt, Gruppe `propwash_prop`, Reiter `equipment`): oben Kennzeichen-Material (links) und
  Größen-Farbstoff (Mitte), Mitte Blatt–Nabe–Blatt, unten die übrigen Blätter (3 Bl. ` B `, 4 Bl. `B B`, 5 Bl. `BBB`) — die Zahl der
  `B` ist die Blattzahl. Blatt: getrockneter Seetang (Polymer), bei Vollcarbon Carbonplatte. Kennzeichen: weich Schleimball, hart
  Feuerstein, Bi-Blend Honigwabe, Glasfaser Glasscheibe, Carbonfaser Carbonplatte, Vollcarbon Quarz. Nabe nach Stufe: bis 3,5″
  Eisennugget (Einstieg), 4–8″ Eisenbarren, X-Class Goldbarren (Mittel-/Oberstufe). Farbstoff je Durchmesser (31 mm rosa, 40 mm magenta,
  45 mm lila, 2″ hellblau, 2,6″ türkis, 3″ blau, 3,5″ hellgrün, 4″ grün, 5,1″ orange, 6″ gelb, 7″ rot, 8″ braun, 10″ grau, 12″ hellgrau,
  13″ schwarz). Ergebnis 4 Stück, X-Class 2 Stück.
- **Seltenheit** (`prop.PropModule`, vor `registerItems`): X-Class-Props (ab 254 mm) „ungewöhnlich“, Vollcarbon „selten“.
- **Truhen-Beute** (`prop.PropLoot`, Tabellen `justmoreparts:inject/prop/<name>`, angemeldet über `PropwashApi.loot()`): Dorfhäuser/Werkzeugschmied Whoop- und Toothpick-Props, Mine/Schiffswrack/Verlies 3–5″,
  Außenposten Race-Props, Tempel 3,5–7″, Festung 6–8″ Long-Range, Prüfungskammern Whoop/2,6″, antike Stadt und Endsiedlung X-Class.
- **Modelle** (ab 1.0.1 Stil-API `jmpstyle.prop`, `python3 tools/assets/style_generate.py --category prop --write`, Regeln in
  `docs/design/asset-style.md` §4.2): Blätter, Nabe, Mutter, Scheibe und Schäden mit den Bausteinen der Propwash-Props
  (`props.blade_pieces`/`emit_piece`, `hub_top`, `nut_top`, `build_blur`), Blattform nach Durchmesser aus den fünf
  Propwash-Props skaliert, Blattwinkel aus P/D, Ducted mit breiten Paddeln, Material über Dicke, Farbe und Maler (PC wie die
  Basis, weich heller, hart satter, Bi-Blend zweifarbig, GFN/CFN entsättigt, Vollcarbon mit Köperbindung und Glanzkante).
  Texturen 64×64 wie die Basis, Anzeige aus `kit.compute_display`, Varianten `_ccw`, `_blur`, `_worn`, `_damaged`,
  `_broken` (auch `_ccw`) mit `_paint`-Varianten; Lack-Einträge führt der Writer in `drone_paint.json` zusammen
  (Fragment mit `--fragments`). Der Generator `tools/props/prop_assets.py` von 1.0.0 ist entfernt.

## 5. Akkus (Implementierer 3 — POWER)

Id-Regel: `b<Zellen>s_<mAh>[_hv|_lion|_ss]`. Masse aus Energie ÷ Energiedichte der Klasse (Abschnitt 1.5), gerundet auf 0,1 g. **IR** = Innenwiderstand je Zelle. **I dauer** = C × Ah (so rechnet Propwash `BATTERY_WEAK`). Stecker ist in API 1.0 nur Anzeige (API-Wunsch W3).

| Id | Name (de) | Name (en) | S | mAh | Chemie | C | IR [mΩ] | g | Wh | I dauer [A] | Stecker | Einsatz | Pro | Contra |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `b1s_250_hv` | 1S-Akku 250 mAh (LiHV) | 1S 250 mAh Battery (LiHV) | 1 | 250 | `propwash:lihv` | 50 | 50,0 | 6,3 | 0,9 | 12,5 | PH2.0 | 65-mm-Whoop, Race | leichtester Akku (1,2 g unter 300 mAh) | kürzeste Flugzeit der 1S-Reihe, 12,5 A zu wenig für 0802/19000 KV |
| `b1s_380_hv` | 1S-Akku 380 mAh (LiHV) | 1S 380 mAh Battery (LiHV) | 1 | 380 | `propwash:lihv` | 45 | 38,0 | 9,6 | 1,4 | 17 | BT2.0 | 65/75-mm-Whoop | gut 30 % mehr Flugzeit als 250 mAh, passt noch in 65 mm | weniger agil als 250/300 mAh |
| `b1s_450_hv` | 1S-Akku 450 mAh (LiHV) | 1S 450 mAh Battery (LiHV) | 1 | 450 | `propwash:lihv` | 60 | 32,0 | 11,4 | 1,7 | 27 | BT2.0 | 75-mm-Whoop | über 7 min Schweben, 60C auch für 1102 | zu schwer für 65 mm |
| `b1s_550_hv` | 1S-Akku 550 mAh (LiHV) | 1S 550 mAh Battery (LiHV) | 1 | 550 | `propwash:lihv` | 60 | 28,0 | 13,9 | 2,1 | 33 | BT2.0 | 75–85-mm-Whoop | gut 10 % mehr Flugzeit als 450 | etwas träger als 450 |
| `b1s_750_hv` | 1S-Akku 750 mAh (LiHV) | 1S 750 mAh Battery (LiHV) | 1 | 750 | `propwash:lihv` | 75 | 22,0 | 18,8 | 2,9 | 56 | BT3.0 | 75-mm-Whoop mit 1102-Motoren | längste 1S-Flugzeit, 75C ohne Einbruch | schwer; im Whoop 85 will der Rahmen 2S |
| `b2s_300_hv` | 2S-Akku 300 mAh (LiHV) | 2S 300 mAh Battery (LiHV) | 2 | 300 | `propwash:lihv` | 45 | 45,0 | 16,3 | 2,3 | 14 | A30 | 75-mm-Whoop 2S, ruhig geflogen (für Vollgas 2S 450 HV) | leicht, 2S-Punch | 13,5 A: jeder 2S-Whoop überlastet ihn (`battery_overload`, `heavy_sag` mit allen 40-mm-Props), starker Spannungseinbruch |
| `b2s_450_hv` | 2S-Akku 450 mAh (LiHV) | 2S 450 mAh Battery (LiHV) | 2 | 450 | `propwash:lihv` | 75 | 30,0 | 24,4 | 3,4 | 34 | XT30 | 85-mm-Whoop, 2″-Toothpick | 75C: 1102 ohne Einbruch, über 12 min | 8 g schwerer als 2S 300 |
| `b2s_650` | 2S-Akku 650 mAh | 2S 650 mAh Battery | 2 | 650 | `propwash:lipo` | 80 | 18,0 | 35,6 | 4,8 | 52 | XT30 | 2,5″ 2S | längste Flugzeit auf 2S, 80C | zu schwer für Whoops und 2″; 2,5″-Rahmen bevorzugt 3S |
| `b3s_450` | 3S-Akku 450 mAh | 3S 450 mAh Battery | 3 | 450 | `propwash:lipo` | 75 | 20,0 | 40,0 | 5,0 | 34 | XT30 | 2,5″ 3S | leicht, mehr Schub/Gewicht als 3S 650 | kürzere Flugzeit, Einbruch mit 1103/8000 |
| `b3s_650` | 3S-Akku 650 mAh | 3S 650 mAh Battery | 3 | 650 | `propwash:lipo` | 90 | 14,0 | 57,7 | 7,2 | 58 | XT30 | 2,5″ 3S | längere Flugzeit, 90C ohne Einbruch | 2,5″ wird träger |
| `b4s_450` | 4S-Akku 450 mAh | 4S 450 mAh Battery | 4 | 450 | `propwash:lipo` | 80 | 20,0 | 53,3 | 6,7 | 36 | XT30 | 2,5–3″-Toothpick 4S | leichtester 4S-Akku, 4S im 2,5″ | 36 A: für 1404 zu schwach, kürzer als 4S 650 |
| `b4s_850` | 4S-Akku 850 mAh | 4S 850 mAh Battery | 4 | 850 | `propwash:lipo` | 100 | 11,0 | 95,3 | 12,6 | 85 | XT60 | 3,5″ 4S, Cinewhoop 2,5″ | 100C für 1505, passt in 2,5″-Cinewhoop | im 3″ träger als 4S 650 |
| `b4s_1100` | 4S-Akku 1100 mAh | 4S 1100 mAh Battery | 4 | 1100 | `propwash:lipo` | 100 | 9,0 | 120,6 | 16,3 | 110 | XT60 | 3,5–4″ 4S | über 14 min im 3,5″, 40 g leichter als 1500 | im 5″ zu wenig Strom (Einbruch) |
| `b4s_1500` | 4S-Akku 1500 mAh | 4S 1500 mAh Battery | 4 | 1500 | `propwash:lipo` | 120 | 5,5 | 160,9 | 22,2 | 180 | XT60 | 5″-Race 4S | 120C, 5,5 mΩ: kein Einbruch, über 11 min | 40 g schwerer als 1100 |
| `b4s_1800` | 4S-Akku 1800 mAh | 4S 1800 mAh Battery | 4 | 1800 | `propwash:lipo` | 100 | 5,0 | 190,3 | 26,6 | 180 | XT60 | 5–6″ 4S | längste Flugzeit auf 4S (über 18 min) | schwerer; Freestyle-Rahmen wollen 6S |
| `b6s_550` | 6S-Akku 550 mAh | 6S 550 mAh Battery | 6 | 550 | `propwash:lipo` | 80 | 18,0 | 97,7 | 12,2 | 44 | XT30 | 3″ 6S | leichtester 6S-Akku, passt in 3″, über 90 km/h | kürzer als 750; 44 A zu wenig für 1505 |
| `b6s_750` | 6S-Akku 750 mAh | 6S 750 mAh Battery | 6 | 750 | `propwash:lipo` | 90 | 13,0 | 130,1 | 16,7 | 68 | XT30 | 3–3,5″ 6S | 67,5 A reicht für 1505, über 13 min | zu schwer für 3″-Rahmen |
| `b6s_850` | 6S-Akku 850 mAh | 6S 850 mAh Battery | 6 | 850 | `propwash:lipo` | 100 | 11,0 | 145,2 | 18,9 | 85 | XT60 | 3,5″-Cinewhoop 6S | 85 A für Ducts mit 2004, über 10 min | 15 g schwerer als 750 |
| `b6s_1500` | 6S-Akku 1500 mAh | 6S 1500 mAh Battery | 6 | 1500 | `propwash:lipo` | 120 | 5,0 | 237,9 | 33,3 | 180 | XT60 | 5″ 6S lang | gut 1 min länger als 6S 1300 | schwerer, zu schwer für Standard-5″ (230 g) |
| `b6s_1800` | 6S-Akku 1800 mAh | 6S 1800 mAh Battery | 6 | 1800 | `propwash:lipo` | 100 | 4,5 | 281,4 | 40,0 | 180 | XT60 | 6″ 6S | über 14 min im 6″, 100C ohne Einbruch | passt in keinen 5″-Rahmen |
| `b6s_2200` | 6S-Akku 2200 mAh | 6S 2200 mAh Battery | 6 | 2200 | `propwash:lipo` | 75 | 4,0 | 336,8 | 48,8 | 165 | XT60 | 6–7″ 6S | über 16 min im 6″, fast doppeltes Schub/Gewicht wie Li-Ion | 55 g schwerer als 1800; Li-Ion 3000 fliegt länger |
| `b6s_3000_lion` | 6S-Akku 3000 mAh (Li-Ion) | 6S 3000 mAh Battery (Li-Ion) | 6 | 3000 | `propwash:lion` | 12 | 20,0 | 301,4 | 64,8 | 36 | XT60 | 4–6″-Long-Range | rund 24 min im 6″, Stahlbecher-Zellen robuster | 36 A: Einbruch bei Vollgas, kaum halbes Schub/Gewicht |
| `b6s_4500_lion` | 6S-Akku 4500 mAh (Li-Ion) | 6S 4500 mAh Battery (Li-Ion) | 6 | 4500 | `propwash:lion` | 10 | 16,0 | 445,9 | 97,2 | 45 | XT60 | 7″-Long-Range | über 31 min im 7″-Deadcat, robuste Zellen | nur Reiseflug, Schub/Gewicht unter 5 |
| `b6s_8400_lion` | 6S-Akku 8400 mAh (Li-Ion) | 6S 8400 mAh Battery (Li-Ion) | 6 | 8400 | `propwash:lion` | 10 | 9,0 | 843,9 | 181,4 | 84 | XT60 | 8–10″-Long-Range (6S2P) | über 40 min im 8″, 2P ohne starken Einbruch | 844 g: nur 8″-Rahmen |
| `b6s_5000` | 6S-Akku 5000 mAh | 6S 5000 mAh Battery | 6 | 5000 | `propwash:lipo` | 60 | 3,0 | 740,0 | 111,0 | 300 | XT90 | 10″ X-Class 6S | 300 A, über 24 min im 10″ | 740 g; 12–13″-Rahmen wollen 8S |
| `b6s_3000_ss` | 6S-Akku 3000 mAh (Semi-Solid-LiHV) | 6S 3000 mAh Battery (semi-solid LiHV) | 6 | 3000 | `justmoreparts:lihv_ss` | 25 | 7,0 | 312,0 | 69,3 | 75 | XT60 | 6–7″-Long-Range | leichter als 6S 2200 LiPo bei 36 % mehr mAh, fast doppelter Li-Ion-Punch, schwebt länger als Li-Ion | 25C: bei Vollgas überlastet; schwerer als Li-Ion 3000, kürzere Reise |
| `b8s_2200` | 8S-Akku 2200 mAh | 8S 2200 mAh Battery | 8 | 2200 | `propwash:lipo` | 100 | 4,0 | 449,1 | 65,1 | 220 | XT90 | X-Class 10–12″ leicht (Race) | halb so schwer wie 8S 5000: mehr Punch | unter 12 min im 12″, Rahmen meldet „Akku schwach“ |
| `b8s_4500_lion` | 8S-Akku 4500 mAh (Li-Ion) | 8S 4500 mAh Battery (Li-Ion) | 8 | 4500 | `propwash:lion` | 10 | 16,0 | 594,5 | 129,6 | 45 | XT90 | 8″-Long-Range 8S | über 36 min im 8″, 250 g leichter als 6S2P | kaum Punch, Einbruch; braucht 8S-Stack |
| `b8s_5000` | 8S-Akku 5000 mAh | 8S 5000 mAh Battery | 8 | 5000 | `propwash:lipo` | 60 | 3,0 | 986,7 | 148,0 | 300 | AS150 | 13″ X-Class | 300 A für 4214, fast 17 min im 13″ | fast 1 kg; mit 4214/530 Einbruch |
| `b8s_5000_ss` | 8S-Akku 5000 mAh (Semi-Solid-LiHV) | 8S 5000 mAh Battery (semi-solid LiHV) | 8 | 5000 | `justmoreparts:lihv_ss` | 20 | 6,0 | 641,7 | 154,0 | 100 | AS150 | 13″ X-Class, Reise | ein Drittel leichter, 20 statt 17 min im 13″ | 100 A: bei Vollgas überlastet, weniger Topspeed |

### 5.1 Hinweise zu Akkus
- Li-Ion-Packs (`_lion`) sind 18650-/21700-Hochstromzellen (1P bzw. 2P); sie lösen bei Vollgas `battery_weak`,
  `heavy_sag` und `battery_overload` aus — das ist ihr ehrliches Contra und in Long-Range-Presets erlaubt (Propwash
  macht es beim eingebauten Long-Range-Preset genauso).
- `b6s_1500` (238 g) passt **nicht** auf die eingebauten 5″-Rahmen (Grenze 230 g), wohl aber auf `fs_5_bash` (260 g):
  gewollt, realistische Packmasse. Im Tooltip `use` erwähnen.
- Semi-Solid (`_ss`): leicht, aber 20–25 C → auf 13″ bei Vollgas Einbruch; gut für Reiseflug.
- `pack_mm` nur für Li-Ion setzen (zylindrische Zellen, Maße aus der Zellanordnung, Länge × Breite × Höhe, siehe 5.4);
  LiPo-Maße rechnet Propwash aus Masse und Dichte.
- Akzentfarbe (`visual.accent`) je Kapazitätsklasse, damit man Akkus im Inventar unterscheidet (frei wählbar, im
  Bericht nennen).

### 5.2 Stecker
| Stecker | typischer Dauerstrom | benutzt für |
|---|---|---|
| PH2.0 | ≈ 3–5 A | 1S bis 300 mAh |
| BT2.0 / BT3.0 | ≈ 9 A / 12 A | 1S Whoops |
| A30 | ≈ 15 A | 2S 300 |
| XT30 | ≈ 30 A | 2–6S bis 750 mAh |
| XT60 | ≈ 60 A | 4–6S |
| XT90 | ≈ 90 A | 6–8S groß |
| AS150 | ≈ 150 A | 8S X-Class |

In API 1.0 ist `connector` nur Anzeige; ein zu schwacher Stecker kann nicht begrenzen (API-Wunsch W3). Deshalb wählt der
Katalog Stecker immer passend zum Dauerstrom des Packs.


### 5.3 Stacks und AIOs (Implementierer 3 — POWER)

Id-Regel: `aio_<klasse>_<maxS>s_<A>a` bzw. `stack_<raster>_<maxS>s_<A>a`. **A** = Dauerstrom je Motor (`esc_continuous_a`), Spitze = `esc_burst_a`. Widerstand: Propwash-Standard (0,003 + 0,15/A Ω) außer wo angegeben.

| Id | Name (de) | Name (en) | Halterung | A dauer / Spitze | Zellen | AIO | g | Brownout | Einsatz | Pro | Contra |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `aio_whoop_1s_6a` | Whoop-AIO-Board (1S, 6 A) | Whoop AIO Board (1S, 6 A) | `propwash:whoop_aio` | 6 / 8 | 1S | ja | 2,6 | 2,9 V | 65/75-mm-Whoop 1S | 0,6 g leichter als das Propwash-AIO, Brownout 2,9 V | nur 1S, 6 A zu wenig für 1102 |
| `aio_whoop_2s_12a` | Whoop-AIO-Board (1–2S, 12 A) | Whoop AIO Board (1–2S, 12 A) | `propwash:whoop_aio` | 12 / 16 | 1–2S | ja | 3,6 | 2,9 V | 75–85-mm-Whoop, 2″ | 1–2S, 12 A für 1102/1103 | 1 g schwerer als 1S-AIO, ab 3S durchgebrannt |
| `aio_tp_4s_20a` | Toothpick-AIO-Board (2–4S, 20 A) | Toothpick AIO Board (2–4S, 20 A) | `propwash:whoop_aio` | 20 / 25 | 2–4S | ja | 5,0 | Standard | 2,5–3″-Toothpick 4S | nur 5 g, 20 A für 1404/3800 | nur bis 4S, zu schwach für 1505/3600 |
| `aio_tp_6s_20a` | Toothpick-AIO-Board (2–6S, 20 A) | Toothpick AIO Board (2–6S, 20 A) | `propwash:whoop_aio` | 20 / 25 | 2–6S | ja | 6,2 | Standard | 3″ 6S | 6S auf 25,5 mm, 2–6S | für 1505/3600 zu schwach |
| `aio_20_4s_35a` | 20×20-AIO-Board (2–4S, 35 A) | 20×20 AIO Board (2–4S, 35 A) | `propwash:s20` | 35 / 45 | 2–4S | ja | 8,5 | Standard | 3–4″ 4S | 35 A für 1505/3600, ESC-Last um 40 % | nur bis 4S, zu schwach für 5″-Race |
| `aio_20_6s_45a` | 20×20-AIO-Board (3–6S, 45 A) | 20×20 AIO Board (3–6S, 45 A) | `propwash:s20` | 45 / 55 | 3–6S | ja | 11,0 | Standard | 3,5″-Cinewhoop, 5″ leicht | stärkstes 20×20-Board, 3 g leichter als Stack | eine Platine: empfindlicher bei Crashs (Haltbarkeit 1,0) |
| `stack_20_6s_40a` | 20×20-FC/ESC-Stack (3–6S, 40 A) | 20×20 FC/ESC Stack (3–6S, 40 A) | `propwash:s20` | 40 / 50 | 3–6S | nein | 14,0 | Standard | 3,5–4″ 6S | getrennte Platinen: robuster (Haltbarkeit 1,25) | 3 g schwerer als AIO, zu schwach für 2207 |
| `stack_30_4s_55a` | 30×30-FC/ESC-Stack (2–4S, 55 A) | 30×30 FC/ESC Stack (2–4S, 55 A) | `propwash:s30` | 55 / 70 | 2–4S | nein | 16,0 | Standard | 5″-Race 4S | 5 g leichter als 60 A, 55 A auch für 2207,5/2700 | brennt auf 6S durch |
| `stack_30_6s_60a` | 30×30-FC/ESC-Stack (3–6S, 60 A) | 30×30 FC/ESC Stack (3–6S, 60 A) | `propwash:s30` | 60 / 75 | 3–6S | nein | 21,0 | Standard | 5″-Freestyle 6S | alle 22xx/23xx ohne Warnung | auf 8S durchgebrannt |
| `stack_30_6s_80a` | 30×30-FC/ESC-Stack (3–6S, 80 A) | 30×30 FC/ESC Stack (3–6S, 80 A) | `propwash:s30` | 80 / 100 | 3–6S | nein | 27,0 | Standard | 5–6″ schwer | ESC-Last unter 40 %, 2507/1800 ohne Grenze | 6 g schwerer als 60 A |
| `stack_30_8s_65a` | 30×30-FC/ESC-Stack (3–8S, 65 A) | 30×30 FC/ESC Stack (3–8S, 65 A) | `propwash:s30` | 65 / 80 | 3–8S | nein | 25,0 | Standard | 7–10″ 8S | bis 8S, auch 6S | 4 g schwerer, zu schwach für 4214/530 |
| `stack_30_8s_100a` | 30×30-FC/ESC-Stack (4–8S, 100 A) | 30×30 FC/ESC Stack (4–8S, 100 A) | `propwash:s30` | 100 / 130 | 4–8S | nein | 44,0 | Standard | 12–13″ X-Class | 100 A auch für 4214/530 | 44 g |

### 5.4 Umsetzung POWER (Stand 2026-09-30, Branch `feature/power`)

**Abweichungen von den Entwurfswerten** (alle innerhalb der Regeln aus 1.5):
- `b1s_250_hv`: C-Rate 40 → **50** (Dauerstrom 12,5 A). Mit 40C meldete schon der eingebaute `propwash:m0802_22000`
  `battery_weak`; 250-mAh-LiHV-Zellen tragen real 45–60C. Mit 0802/19000 KV bleibt die Warnung (ehrliches Contra).
- Li-Ion bekommen `pack_mm` aus der Zellanordnung (18650: Ø 18,6 × 65,2 mm, 21700: Ø 21,4 × 70,2 mm, plus Wickel/Nickel):
  `b6s_3000_lion` 6S1P, 3 × 2 Zellen längs: **69 × 59 × 40**; `b6s_4500_lion` 6S1P längs: **74 × 67 × 46**;
  `b6s_8400_lion` 6S2P, 6 × 2 Zellen quer: **133 × 74 × 46**; `b8s_4500_lion` 8S1P, 4 × 2 längs: **74 × 89 × 46**.
- Haltbarkeit (`durability`, Teiler der Crash-Dosis): Li-Ion **1,5** (Stahlbecher-Zellen statt Pouch), Semi-Solid **1,2**,
  getrennte Stacks (`stack_*`) **1,25** (FC und ESC getrennt, Softmount), AIOs Standard 1,0.
- Recycling: Akkus liefern `lipo_cell` je nach Größe (1–2S: 1, 3–4S: 2, 6S: 3, 8S: 4, 6S2P: 6), Stacks
  `circuit_board` (AIO 1, Stack 2, 8S-100-A-Stack 3).
- Standardwerte entfernt (Konvention): `chemistry: propwash:lipo`, `all_in_one: false`, `esc_burst_a` = 1,3 × Dauerstrom
  (`stack_30_8s_100a`). `brownout_v` 2,9 V bleibt bei den 1S-AIOs ausgeschrieben (Nachweis).

**Akzentfarben** (`visual.accent`, je Kapazitätsklasse): 1S Lila/Magenta (`#c9a8f5` 250 … `#b8307e` 750), 2S Cyan/Blau
(`#3fc2d9`, `#2a9fd6`, `#1f6fb8`), 3S Grün (`#8ad13f`, `#3fa34d`), 4S Gelb→Rost (`#f5d03f` 450 … `#b8452a` 1800), 6S klein
Rosa/Rot (`#ff8fa3`, `#f25c78`, `#d93a5c`), 6S groß Blau/Indigo (`#3f8fe0`, `#2750b8`, `#5a4fd6`), 6S 5000 Graphit
(`#4a5163`), 8S 2200 Gold (`#d6b82a`), 8S 5000 Stahl (`#8a8f99`), Li-Ion Kupfer (`#c25e1f`, 6S 3000) und Petrol (`#0f5f58`, `#1f7a9a`),
6S 4500 Li-Ion Violett (`#6a4bc4`, damit er nicht wie der Propwash-`b6s_4000_lion` aussieht), Semi-Solid Champagner (`#c8b88a`, `#a8986a`). Stacks: Farbe der Softmount-Gummis und des Aufdrucks.

**Seltenheit** (`PowerModule`, `partItems().registerItem`): X-Class-Klasse `b6s_5000`, `b6s_8400_lion`, `b8s_2200`,
`b8s_4500_lion`, `b8s_5000`, `stack_30_8s_65a`, `stack_30_8s_100a` = ungewöhnlich; Semi-Solid `b6s_3000_ss`,
`b8s_5000_ss` = selten.

**Rezepte** (Gruppe `propwash_battery`/`propwash_stack`, Reiter `equipment`; Zahl der `propwash:lipo_cell` = Zellzahl,
6S2P = zwei `b6s_4500_lion`): 1S formlos aus Zelle + Glowstone + Kupfer/Faden; 2S/3S/4S Zellen oben, darunter
Chemie-Kennzeichen (Glowstone = LiHV, Redstone = LiPo) und Strom-/Kapazitätsstufe (Kupfernugget → Kupferbarren →
Eisenbarren → Goldbarren → Diamant); Li-Ion mit Eisen (Stahlbecher) und Amethyst/Diamant, 8S Li-Ion mit Eisenblock;
Semi-Solid mit Schleim (Gel-Elektrolyt) und Diamant bzw. Schleimblock. Stacks aus `circuit_board` (AIO 1, 20×20 2,
30×30 4), Spannungskennzeichen Redstone (4S), Quarz (6S), Amethyst (8S), Stromstufe über Kupfer/Eisen/Gold/Diamant.
JUnit `PowerDataTest` prüft Eindeutigkeit (auch gespiegelt und gegen Propwash), Zellzahl und steigende Kosten je Stufe.

**Truhen-Beute** (`data/justmoreparts/loot_table/inject/power/*.json`, ein Wurf, großer Leer-Eintrag, Fundchance
6–25 %): Dorfhäuser 1S-Whoop-Akkus; Werkzeugschmied 1S/2S und Whoop-AIOs; Minen 2S/3S und Toothpick-AIO; Verliese
2–4S und 20×20-AIO; Tempel 6S klein, 20×20-Boards; Außenposten 5″-Akkus und 30×30-Stacks; Festung 6″/7″-LiPo, Li-Ion
3000, 80-A-Stack; Schiffswrack 2–4S und Li-Ion; Prüfungskammer 20×20-AIOs, 4S/6S; Tresor 6S groß, Semi-Solid, 8S-Stack;
unheilvoller Tresor Li-Ion 4500, Semi-Solid, 8S 2200/Li-Ion, 6S 5000; antike Stadt und Endstadt X-Class-Akkus und
100-A-Stack. X-Class- und Semi-Solid-Teile nur ab Festung/Tresor. Einspeisung: `power.PowerLoot` liefert die Paare,
`core.JmpLoot` meldet sie über `PropwashApi.loot()` an (Parts-API 1.4, beide Loader gleich, keine Loot-Modifier mehr;
Selbsttests beider Loader grün).

**Modelle und Texturen** (ab 1.0.1): `python3 tools/assets/style_generate.py --category battery --category stack --write`
erzeugt mit der Stil-API (`tools/assets/jmpstyle/battery.py`, `stack.py`, Regeln in `docs/design/asset-style.md` §4.3/§4.4)
für jedes Teil Modell, 64×64-Textur, Render-Definition, Lack-Variante (`_paint`, Kanal `battery` bzw. `stack`, Eintrag in
`assets/justmoreparts/drone_paint.json`) und `items/<id>.json` mit den Anzeige-Transformationen der Propwash-Items.
LiPo/LiHV/Semi-Solid wie die Propwash-Akkus (Stirnseiten, Hülle, Etikett, Band, Balancer, Gurt ab 2S), Zellen einreihig
gestapelt; Li-Ion aus Achteck-Rundzellen längs oder quer (6S2P 21700 quer wie `pack_mm`); Whoop-, Zahnstocher- und
20×20-AIOs sowie Türme wie die Propwash-Boards, 20×20-Turm mit einem Elko. Der alte Generator `power_assets.py` samt
`power_paint.json` ist entfernt. Vergleich Propwash / 1.0.0 / 1.0.1 in `docs/asset-sheets/power_compare_battery.png` und
`power_compare_stack.png` (`tools/assets/compare_render.py --category battery --jmp "JMP 1.0.0=<assets aus 1.0.0>"
--jmp "JMP 1.0.1=common/src/main/resources/assets" --pair b6s_1300=b6s_2200 …`, alle Teile mit `--all`).

**Prüfstand** (`power/PowerBenchTest`, `PowerClaimsTest`; Referenzaufbauten je Teil, Zielkorridore 1.7, jede Pro/Contra-
Aussage des Tooltips als Assertion):

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
Whoop 65 Lite 1S 250                  22.7   4.6  40.8    8.4    6.4     40   10.7    43    86   30.4  18.6
Whoop 65 1S 380                       26.5   3.9  47.4   10.5    8.5     40   10.4    42    61   30.3  17.0
Whoop 75 1S 450                       32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  19.9
Whoop 75 1S 550                       35.4   3.8  46.5   12.7   10.2     40   12.8    52    39   36.7  17.7
Whoop 75 1S 750                       46.4   3.7  49.2   12.9   10.5     43   16.2    33    29   32.9  16.3
Whoop 75 2S 300                       38.3   5.7  29.8   11.8    7.5     51   16.6    34   123   49.3  13.8
Whoop 85 2S 450                       56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0
Toothpick 2,5" 2S 650                 73.5   5.6  31.6   13.2    8.8     71   32.0    40    62   79.1  15.4   W[battery_undersized][]
Toothpick 2,5" 3S 450                 84.1   5.4  33.7   10.0    7.0     75   24.6    31    73   86.7  11.3
Toothpick 2,5" 3S 650                102.8   4.9  36.9   11.7    8.5     78   26.6    33    46   98.2   9.1
Toothpick 2,5" 4S 450                 97.4   6.7  26.3   11.1    6.5     89   35.0    44    97  119.7   9.6
Toothpick 3" 4S 650 (Propwash)       142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4
Toothpick 3,5" 4S 850                190.0   8.0  26.6   12.9    7.2    103   59.5    42    70   88.9   7.5
Toothpick 3,5" 4S 1100               215.3   7.4  28.8   14.4    8.4    106   62.2    44    57   94.6   7.5
Cinewhoop 2,5" 4S 850                193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2
Race 5" 4S 1500                      407.1  11.2  21.7   16.4    7.3    155  124.7    57    69   75.9   0.0
Race 5" 4S 1800                      436.5  10.8  22.7   18.2    8.4    157  127.7    58    71   79.8   0.0
Toothpick 3" 6S 550                  163.7   6.3  32.7   11.6    7.6     95   30.1    38    68   84.3   6.7
Toothpick 3,5" 6S 750                222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5
Cinewhoop 3,5" 6S 850                339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6
Cinewhoop 3,5" 6S 850 Stack          342.9   4.2  47.0   10.1    7.9     55   24.8    15    29   79.7  15.6
Bash 5" 6S 1500                      558.2  11.4  21.9   16.2    6.9    142  127.7    53    71   96.6   4.8
Bash 5" 6S 1300 80 A                 536.3  11.6  21.4   14.7    6.2    140  124.9    39    80   92.3   5.9
Long Range 6" 6S 1800                637.8  10.8  24.2   19.3    9.7    130  107.0    44    59  101.7   8.7
Long Range 6" 6S 2200                693.2  10.1  25.5   21.6   11.3    132  109.3    45    66  107.1   7.3
Long Range 6" 6S 3000 Li-Ion         657.8   5.5  27.3   29.0   21.3    101   59.1    24   164   81.8   8.1   W[battery_weak][heavy_sag, battery_overload]
Long Range 6" 6S 3000 Semi-Solid     645.2  10.1  23.6   32.8   17.0    128  101.5    42   135  105.4   8.6   W[battery_weak][battery_overload]
Deadcat LR 7" 6S 4500 Li-Ion         881.7   4.8  28.0   33.5   25.3    101   75.9    31   169   97.3   0.1   W[battery_weak][heavy_sag, battery_overload]
Long Range 8" 6S2P 8400 Li-Ion      1329.7   4.9  33.0   42.1   31.7    109  108.3    42   129  153.0   8.2   W[battery_weak][motor_thermal]
Long Range 8" 8S 4500 Li-Ion        1080.3   4.9  20.2   39.2   28.4    103   89.3    34   198  122.0  10.1   W[battery_weak][heavy_sag, battery_overload]
X-Class 10" 6S 5000                 1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  126.4  14.7
X-Class 12" 8S 2200                 2092.1   6.5  34.3   11.4    7.9     88  116.0    29    53   87.1   3.2   W[battery_undersized][]
X-Class 13" 8S 5000                 3123.7   5.8  35.9   16.8   12.1     91  169.4    42    56  128.9   3.0
X-Class 13" 8S 5000 Semi-Solid      2778.7   5.5  32.8   20.0   14.4     85  144.7    36   145  115.1   2.4   W[battery_weak][battery_overload]
```

Begründete Ausnahmen vom Korridor stehen im Test (`PowerReferences`): größere Akkus fliegen länger als der Preset-Korridor
(Flugzeit ist ihr Zweck), Li-Ion/Semi-Solid dürfen `battery_weak`/`heavy_sag`/`battery_overload` melden, 2S 650 und 8S 2200
melden „Akku schwach“ (Rahmen-Vorgabe 3S bzw. ≥ 3000 mAh; bewusst so, §13.3), der `lr8`-Aufbau meldete damals `motor_thermal`
(erledigt über die Kühlung des `m2807_1100`, §3.1; die Ausnahme im Test ist entfernt).

**Nachweise der Abnahme**: Li-Ion längste Schwebezeit und schwächster Punch aller 6S-Akkus im 7″-Deadcat
(`b6s_4500_lion` 33,5 min, T/W 4,8); Semi-Solid leichter als LiPo gleicher Kapazität (8S 5000: 642 statt 987 g) und
leichter als 6S 2200 LiPo mit 3000 mAh; 4S-Stacks (`stack_30_4s_55a`, `aio_20_4s_35a`, `aio_tp_4s_20a`) melden auf 6S
`stack_voltage` und `esc_overvoltage` (Durchbrennen beim Scharfschalten mit Server-Regel `partWear` macht Propwash; der
Selbsttest kann es mit der API 1.0 nur über die Analyse prüfen, Wunsch P1); 1S-AIOs mit Brownout 2,9 V und ohne
`heavy_sag` im 75-mm-Whoop.

**Wünsche aus POWER** (Vorschläge für Parts-API 1.1, rein ergänzend; die Architektur-Tabelle §9 pflegt der Architekt):

| Nr. | Wunsch | Signatur | Wirkung |
|---|---|---|---|
| P1 | ESC-Durchbrennen für Addons beobachtbar | `PropwashEvents.ESC_BURNED: PropwashEvent<EscBurned>`, `interface EscBurned { void onEscBurned(ServerPlayer player, ItemStack drone, DroneData data, int cells); }`; `BuildAccess#partCondition(ItemStack drone, PartKind kind): OptionalDouble` (0…1) | Selbsttest „4S-Stack auf 6S brennt beim Scharfschalten durch“ ohne Propwash-Interna; Statistik im Addon |
| P2 | Ladestand in Beute und Items | `PartItems#stack(Identifier partId, float charge): ItemStack`; Komponente `propwash:battery_charge` (Float 0…1) als stabile Daten-Schnittstelle dokumentieren | gefundene JMP-Akkus mit Lagerladung 40 % wie die eingebauten |
| P3 | Truhen-Beute ohne Loader-Code | `PropwashApi.loot(): LootAccess`, `LootAccess#inject(ResourceKey<LootTable> target, ResourceKey<LootTable> table)` (Propwash verdrahtet Fabric-Event und NeoForge-Modifier) | JMP-Beute auf beiden Loadern ohne Änderung der Loader-Module |
| P4 | Zellspannung unter Vollgas in der Analyse | `BuildStats#fullThrottleCellVoltage(): double` (neue Komponente hinten, alter Konstruktor bleibt als Überladung) | Brownout-Reserve der 1S-AIOs (2,9 V) und Einbruch der Li-Ion/Semi-Solid direkt prüfbar |
| (W3) | Stecker mit Stromgrenze | siehe Architektur §9 | PH2.0/BT2.0 an 1S-Whoops würden bei Vollgas messbar begrenzen |

## 6. Video (Implementierer 4 — FRAMES + VIDEO)

Produktartige Eigennamen ohne echte Marken. FOV ist das **horizontale** Sichtfeld der Kamera. Seit dem Nachtrag vom
02.10.2026 (Propwash 0.4.0, Parts-API 1.4) bestimmt es das FPV-Bild mit analoger und digitaler Brille und „Props im
Bild“ (vorher das Brillen-Sichtfeld analog 120°, digital 130°); die Sendeleistung steht weiter nur im Tooltip. Die Werte
sind aus echten Kameras recherchiert (Propwash `docs/design/realism/power-video.md` §4.4): Datenblätter nennen meist das
diagonale Sichtfeld, horizontal sind es bei 4:3 rund 0,8 ×, bei 16:9 0,85 × davon.

| Id | Name (de) | Name (en) | System | Latenz ms | Reichweite Blöcke | Bild | Kamera mm | FOV° | VTX mW | g | Einsatz | Pro | Contra |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `analog_whoop_25mw` | Analoge Whoop-Kamera mit Sender (Firefly Nano) | Analog Whoop Camera + VTX (Firefly Nano) | analog | 12 | 200 | 0,50 | 14 | 126 | 25 | 2,2 | 1S-Whoops | leichteste Kamera | kurze Reichweite, rauschig |
| `analog_race_800` | Analoge Race-Kamera mit Sender (Racecore 800) | Analog Race Camera + VTX (Racecore 800) | analog | 11 | 520 | 0,68 | 19 | 125 | 800 | 9,5 | 5″-Race | kaum Latenz, viel Reichweite | analoges Rauschen |
| `analog_lr_1600` | Analoge Long-Range-Kamera mit Sender (Longshot 1600) | Analog Long Range Camera + VTX (Longshot 1600) | analog | 12 | 820 | 0,62 | 19 | 110 | 1600 | 16,0 | Long-Range analog | größte analoge Reichweite | schwer, Kühlkörper |
| `digital_micro` | Digitales Micro-Videosystem (Pixel Micro) | Digital Micro Video System (Pixel Micro) | digital | 24 | 220 | 0,78 | 14 | 126 | 200 | 4,8 | Whoops, Toothpicks | klares Bild in 5 g | kurze Reichweite |
| `digital_low_latency` | Digitales Race-Videosystem (Swift Link) | Digital Race Video System (Swift Link) | digital | 14 | 380 | 0,82 | 19 | 133 | 500 | 9,8 | digitales Racing | Latenz fast wie analog | Bild weicher als HD |
| `digital_hd_pro` | Digitales HD-Videosystem (Clarity HD Pro) | Digital HD Video System (Clarity HD Pro) | digital | 24 | 650 | 1,00 | 20 | 130 | 1000 | 24,0 | Freestyle, Cinematic | bestes Bild, weniger Latenz und mehr Reichweite als Standard-HD | empfindlich (Haltbarkeit 0,8) |
| `digital_lr` | Digitales Long-Range-Videosystem (Horizon LR) | Digital Long Range Video System (Horizon LR) | digital | 32 | 900 | 0,90 | 19 | 130 | 1200 | 19,0 | Long-Range, X-Class | größte Reichweite | mehr Latenz, schwer |

`digital_hd_pro` trägt `durability: 0.8` (empfindliche Kamera). Referenz eingebaut: `analog_nano` 4 g/12 ms/250, `analog_race` 8 g/12 ms/380, `digital_lite` 8,5 g/26 ms/300, `digital_hd` 23 g/30 ms/520.

**Umsetzung (Implementierer 4, 2026-09-30).** Akzentfarben (`visual.accent`, Blende der Kamera): `analog_whoop_25mw` #f0883e,
`analog_race_800` #bf3989 (mit zwei Kühlrippen), `analog_lr_1600` #9e6a03, `digital_micro` #6e7781, `digital_low_latency` #1f6feb,
`digital_hd_pro` #b08d57 (18-mm-Objektiv), `digital_lr` #238636. Latenz, Reichweite, Bild und Sendeleistung wirken in API 1.0 nicht auf die
Analyse; die Hilfe nennt sie als Kennzahlen (Propwash zeigt sie selbst als Datenzeilen). Auch das Kamera-Sichtfeld ändert
das Bild im Spiel bis zum Nachtrag vom 02.10.2026 nicht: Propwash zeichnete das FPV-Bild mit dem Sichtfeld der Brille,
und seit 0.4.1 rechnete auch „Props im Bild“ damit (vorher mit `camera_fov_deg`, deshalb standen in 1.0.0/1.0.1 „weites
Sichtfeld zeigt mehr Props“ bei Firefly Nano, Pixel Micro und Clarity HD Pro – in 1.0.2 gestrichen). Seit dem Nachtrag
gilt wieder das Sichtfeld der Kamera, jetzt recherchiert: Firefly Nano 126° (Whoop-Kamera 1,8–2,1 mm), Racecore 800 125°
(Micro-Kamera 1,8 mm), Longshot 1600 110° (Long-Range-Optik mit natürlichem Bild), Pixel Micro 126° (digitale
Nano-Kamera), Swift Link 133° (digitale Micro-Race-Kamera), Clarity HD Pro und Horizon LR 130° (HD-Einheiten der
DJI-O3/O4-Pro-Klasse); die Hilfe nennt das Sichtfeld bei Longshot, Pixel Micro und Swift Link als Vorteil. Im Referenz-5″ bleibt der Wert mit jeder Kamera derselben
Verbindung gleich (± Schwerpunkt-Verschiebung durch die Masse, unter 1,5 Prozentpunkten). Deadcat 5″ und H-Frame zeigen
0 % Props, auch mit Standard-Brille digital 130°; `lr_7_deadcat` zeigt damit nur die Propspitzen an beiden Bildrändern
(rund 2 %).

## 7. Rahmen und Rahmenformen (Implementierer 4 — FRAMES + VIDEO)

Rahmenformen (API `FrameShape`, Wirkung über `arms`):

| Form | Geometrie | Wirkung in Propwash (Prüfstand: gleiche FREESTYLE-Teile, Abschnitt 8.1) | Ehrlich |
|---|---|---|---|
| True-X | alle Motoren auf den Diagonalen, gleicher Abstand | Referenz | ausgewogen |
| Stretched-X | Motoren weiter vorn/hinten als seitlich (x < z) | mehr Roll-, weniger Nick-Autorität; mit schlankem Race-Rumpf (Stirnfläche 0,0085 m², Cd 0,72) 171 statt 140 km/h | Props eher im Bild bei flachem Uptilt |
| Squashed-X | Motoren weiter seitlich als vorn/hinten (x > z) | mehr Nick-, weniger Roll-Autorität (Flips schneller als Rolls) | Rolls träger |
| Deadcat | vordere Motoren weit gespreizt und nach hinten gesetzt, hintere enger | Props aus dem Kamerabild (0–0,3 % statt 1,9 %), Schwerpunkt hinten, Mixer gleicht aus | asymmetrisch, schwerer, etwas langsamer |
| H-Frame | zwei Längsholme, Motoren an den Ecken, Kamera weit vorn | Props aus dem Bild, ruhig; mehr Luftwiderstand (Topspeed 124 statt 140 km/h) | schwer, träge |
| Duct (Cinewhoop/Whoop) | Ringe um die Props (`duct`) | Schubgewinn bei wenig Fahrt, Rückprall statt Bruch | langsam, Luftwiderstand, Ducts im Bild |
| X-Class | 10–13″, Radstand bis 828 mm | große Trägheit, lange Motor-Zeitkonstante, hohe Reichweite | schwer, bricht bei harten Crashs |

**Armdicke und Steifigkeit** sind in API 1.0 keine eigenen Felder (API-Wunsch W1). Bis dahin bilden sie ab:
Masse und Massenaufteilung (dickere Arme = schwerer, mehr Trägheit), `impact_tolerance` (dicke Arme brechen erst bei
höherer Aufprallgeschwindigkeit), `durability` (Teiler der Crash-Dosis), Tooltip (Armdicke in mm). Die Spalte
„Arme mm“ ist deshalb Tooltip-Information und Begründung der übrigen Werte.

Spaltenbedeutung: Radstand = Diagonale Motor–Motor in mm; Akku-Regel = `battery_fit` (Zellen, Höchstmasse,
bevorzugte Mindestzellen/-kapazität); Kamera = `camera.pos` (falls gesetzt) und `camera.tilt_deg`; Aero =
`aero.frontal_area_m2`, `aero.drag_coefficient`, „oben“ = `aero.top_area_m2` (seit 1.1.0 für alle Whoops nach Rumpf und Blattfläche statt nach dem ganzen Umriss,
Herleitung in Propwash `docs/design/realism/flight-feel-gravity.md` §10); Zubehör = `accessory_mounts`. Die vollständigen Werte stehen im
JSON-Entwurf (Abschnitt 10).


| Id | Name (de) | Name (en) | Kurzname | Rolle | Form | Radstand | Props mm | g | Arme mm | Motor / Stack | Akku oben · Platte | Akku-Regel (S, g, bev.) | Kamera | Zubehör | Aero (m², Cd) | Duct | Aufprall m/s | Haltb. |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `whoop_65_lite` | 65-mm-Whoop-Rahmen (Lite) | 65 mm Whoop Frame (Lite) | Whoop 65 Lite | whoop | true_x | 65 | 28–31 | 3,2 | 1,2 | m08 / whoop_aio | nein · -3,0 | 1–1S, ≤ 10 g, bev. 1S | 14° | bottom | 0,0034, 1,10, oben 0,0039 | clearance_mm 1.5 | 10,0 | 0,7 |
| `whoop_75_duct` | 75-mm-Whoop-Rahmen | 75 mm Whoop Frame | Whoop 75 | whoop | true_x | 75 | 35–40 | 5,8 | 1,5 | m08 / whoop_aio | nein · -3,5 | 1–2S, ≤ 25 g, bev. 1S | (0, 8, -26), 18° | bottom | 0,0045, 1,10, oben 0,0046 | Standard | 13,0 | 1,0 |
| `whoop_85_duct` | 85-mm-Whoop-Rahmen | 85 mm Whoop Frame | Whoop 85 | whoop | true_x | 85 | 40–45 | 7,6 | 1,8 | m08 / whoop_aio | nein · -4,0 | 1–2S, ≤ 35 g, bev. 2S | 18° | bottom | 0,0056, 1,10, oben 0,0065 | Standard | 12,0 | 1,0 |
| `tp_2_x` | 2-Zoll-Toothpick-Rahmen | 2″ Toothpick Frame | Toothpick 2″ | toothpick | true_x | 90 | 45–51 | 7,5 | 2,5 | m08, m14 / whoop_aio | ja · 7,0 | 1–2S, ≤ 30 g, bev. 2S | 25° | — | 0,0035, 0,90 | — | 9,0 | 1,0 |
| `tp_25_x` | 2,5-Zoll-Toothpick-Rahmen | 2.5″ Toothpick Frame | Toothpick 2.5″ | toothpick | true_x | 110 | 55–65 | 9,5 | 2,5 | m14 / whoop_aio | ja · 9,0 | 2–4S, ≤ 60 g, bev. 3S | 22° | — | 0,0045, 0,90 | — | 8,5 | 1,0 |
| `tp_3_x` | 3-Zoll-Toothpick-Rahmen | 3″ Toothpick Frame | Toothpick 3″ | toothpick | true_x | 125 | 70–76 | 13,0 | 3,0 | m14 / whoop_aio, s20 | ja · 12,0 | 3–6S, ≤ 110 g, bev. 4S | 25° | top | 0,0058, 0,90 | — | 8,0 | 1,0 |
| `tp_35_stretched` | 3,5-Zoll-Toothpick-Rahmen (Stretched-X) | 3.5″ Toothpick Frame (stretched X) | Toothpick 3.5″ | toothpick | stretched_x | 159 | 80–89 | 21,0 | 3,0 | m14 / whoop_aio, s20 | ja · 14,0 | 3–6S, ≤ 170 g, bev. 4S | (0, 12, -80), 26° | top | 0,0072, 0,85 | — | 8,0 | 1,0 |
| `cw_25_duct` | 2,5-Zoll-Cinewhoop-Rahmen | 2.5″ Cinewhoop Frame | Cinewhoop 2.5″ | cinewhoop | true_x | 112 | 60–65 | 46,0 | 2,0 | m14 / whoop_aio, s20 | ja · 16,0 | 3–4S, ≤ 110 g, bev. 4S | 15° | top, bottom | 0,0090, 1,00, Draufsicht 0,015 (1.0.5) | Standard | 12,0 | 1,0 |
| `cw_35_duct` | 3,5-Zoll-Cinewhoop-Rahmen | 3.5″ Cinewhoop Frame | Cinewhoop 3.5″ | cinewhoop | true_x | 158 | 80–90 | 92,0 | 2,5 | m14 / s20 | ja · 20,0 | 4–6S, ≤ 200 g, bev. 6S | 15° | top, bottom | 0,0137, 1,00, Draufsicht 0,0228 (1.0.5) | Standard | 11,0 | 1,0 |
| `fs_5_bash` | 5-Zoll-Freestyle-Rahmen (Bash) | 5″ Freestyle Frame (Bash) | Bash 5″ | freestyle | true_x | 226 | 115–130 | 138,0 | 6,0 | m22 / s30, s20 | ja · 32,0 | 4–6S, ≤ 260 g, bev. 6S | 30° | top, bottom | 0,0125, 0,95 | — | 10,0 | 1,4 |
| `fs_5_squashed` | 5-Zoll-Freestyle-Rahmen (Squashed-X) | 5″ Freestyle Frame (squashed X) | Squashed 5″ | freestyle | squashed_x | 239 | 115–130 | 124,0 | 5,0 | m22 / s30, s20 | ja · 30,0 | 4–6S, ≤ 250 g, bev. 6S | 30° | top, bottom | 0,0125, 0,92 | — | 8,5 | 1,0 |
| `race_5_stretched` | 5-Zoll-Race-Rahmen (Slim) | 5″ Race Frame (Slim) | Race 5″ Slim | race | stretched_x | 236 | 115–130 | 84,0 | 4,0 | m22 / s30, s20 | nein (unten, seit 1.1.0) · −2,8 | 4–6S, ≤ 230 g, bev. 4S | 40° | top, bottom | 0,0085, 0,72 | — | 6,5 | 0,8 |
| `deadcat_5` | 5-Zoll-Freestyle-Rahmen (Deadcat) | 5″ Freestyle Frame (deadcat) | Deadcat 5″ | freestyle | deadcat | 223 | 115–130 | 132,0 | 5,0 | m22 / s30, s20 | ja · 30,0 | 4–6S, ≤ 250 g, bev. 6S | (0, 14, -105), 30° | top, bottom | 0,0130, 0,95 | — | 8,5 | 1,0 |
| `hframe_5` | 5-Zoll-Freestyle-Rahmen (H-Frame) | 5″ Freestyle Frame (H-frame) | H-Frame 5″ | freestyle | h_frame | 252 | 115–130 | 145,0 | 5,0 | m22 / s30, s20 | ja · 28,0 | 4–6S, ≤ 260 g, bev. 6S | (0, 18, -92), 25° | top, bottom | 0,0150, 1,00 | — | 9,0 | 1,0 |
| `lr_4_x` | 4-Zoll-Long-Range-Rahmen | 4″ Long Range Frame | Long Range 4″ | long_range | true_x | 175 | 90–102 | 48,0 | 4,0 | m14 / s20, whoop_aio | ja · 22,0 | 4–6S, ≤ 320 g, bev. 6S | 20° | top | 0,0090, 0,90 | — | 8,0 | 1,0 |
| `lr_6_x` | 6-Zoll-Long-Range-Rahmen | 6″ Long Range Frame | Long Range 6″ | long_range | true_x | 255 | 140–152 | 128,0 | 5,0 | m28, m22 / s30, s20 | ja · 30,0 | 4–6S, ≤ 450 g, bev. 6S | 22° | top, bottom | 0,0155, 0,90 | — | 8,0 | 1,0 |
| `lr_7_deadcat` | 7-Zoll-Long-Range-Rahmen (Deadcat) | 7″ Long Range Frame (deadcat) | Deadcat 7″ | long_range | deadcat | 302 | 165–180 | 185,0 | 5,5 | m28, m22 / s30, s20 | ja · 32,0 | 6–8S, ≤ 650 g, bev. 6S/1300 mAh | (0, 16, -78), 22° | top, bottom | 0,0170, 0,90 | — | 8,0 | 1,0 |
| `lr_8_x` | 8-Zoll-Long-Range-Rahmen | 8″ Long Range Frame | Long Range 8″ | long_range | true_x | 360 | 190–205 | 215,0 | 6,0 | m28 / s30 | ja · 34,0 | 6–8S, ≤ 900 g, bev. 6S/2000 mAh | 20° | top, bottom | 0,0210, 0,90 | — | 7,5 | 1,0 |
| `xclass_10` | 10-Zoll-X-Class-Rahmen | 10″ X-Class Frame | X-Class 10″ | x_class | true_x | 460 | 230–254 | 480,0 | 8,0 | justmoreparts:m31 / s30 | ja · 45,0 | 6–8S, ≤ 1100 g, bev. 6S/3000 mAh | 25° | top, bottom | 0,0275, 1,00 | — | 7,0 | 1,0 |
| `xclass_12` | 12-Zoll-X-Class-Rahmen | 12″ X-Class Frame | X-Class 12″ | x_class | true_x | 700 | 280–305 | 980,0 | 10,0 | justmoreparts:m31, justmoreparts:m42 / s30 | ja · 55,0 | 6–8S, ≤ 1800 g, bev. 8S/3000 mAh | 28° | top, bottom | 0,0320, 1,00 | — | 6,5 | 1,0 |
| `xclass_13` | 13-Zoll-X-Class-Rahmen | 13″ X-Class Frame | X-Class 13″ | x_class | true_x | 820 | 300–330 | 1450,0 | 12,0 | justmoreparts:m42 / s30 | ja · 60,0 | 6–8S, ≤ 2500 g, bev. 8S/4000 mAh | 30° | top, bottom | 0,0400, 1,00 | — | 6,0 | 1,0 |
| `xclass_13_h` | 13-Zoll-X-Class-Rahmen (H-Frame) | 13″ X-Class Frame (H-frame) | H-Frame 13″ | x_class | h_frame | 828 | 300–330 | 1650,0 | 12,0 | justmoreparts:m42 / s30 | ja · 60,0 | 6–8S, ≤ 2500 g, bev. 8S/4000 mAh | (0, 30, -300), 25° | top, bottom | 0,0460, 1,05 | — | 7,0 | 1,3 |

Kamerahalter seit 1.0.0 (Architektur §10.15; die Spalte „Kamera“ oben nennt für die folgenden Rahmen nur den Uptilt):
`camera.pos` (x, y, z) in mm – `whoop_65_lite` (0, 11,7, −17,5), `whoop_85_duct` (0, 13,4, −17,5), `tp_2_x`
(0, 13,2, −28,9), `tp_25_x` (0, 14,1, −33,1), `tp_3_x` (0, 16,9, −36,3), `cw_25_duct` (0, 16,4, −33,5), `cw_35_duct`
(0, 16,5, −43,2), `fs_5_bash` (0, 20,6, −57,5), `fs_5_squashed` (0, 16,9, −60,2), `race_5_stretched` (0, 18,0, −59,6),
`lr_4_x` (0, 16,9, −46,8), `lr_6_x` (0, 21,0, −63,6), `lr_8_x` (0, 20,2, −85,6), `xclass_10` (0, 27,0, −106,6),
`xclass_12` (0, 26,0, −157,0), `xclass_13` (0, 26,0, −182,2). Das Objektiv sitzt damit wenige Millimeter unter der
Propebene (Whoops knapp über den Ducts), wie bei echten Rahmen mit der Kamera im Käfig; Herleitung und Quellen in
Propwash `docs/design/realism/props-in-view.md`. Seit 1.1.0 sitzt das Objektiv der Rahmen mit Ducts über der
Duct-Oberkante: `whoop_65_lite` (0, 18,5, −17,5), `whoop_75_duct` (0, 19, −24), `whoop_85_duct` (0, 21, −27), `cw_25_duct`
(0, 29,5, −48), `cw_35_duct` (0, 34,8, −52) – Regeln und Anteil der Ducts im FPV-Bild in `asset-style.md`
(Kamerahalter der Rahmen mit Ducts).

### 7.1 Arm-Geometrie der geformten Rahmen

Koordinaten in mm (Rahmen-System: +X rechts, +Y oben, −Z vorn), Drehrichtung „props in“ (VL cw, VR ccw, HL ccw, HR cw). Rahmen ohne Eintrag hier sind True-X aus dem Radstand (keine `arms`).

| Id | vorne links | vorne rechts | hinten links | hinten rechts | Schwerpunkt | Massenaufteilung | Einsatz | Pro | Contra |
|---|---|---|---|---|---|---|---|---|---|
| `tp_35_stretched` | (-50, 0, -62) | (50, 0, -62) | (-50, 0, 62) | (50, 0, 62) | 0 | Standard | 3,5″-Toothpick 4–6S | mehr Roll-Autorität, 3,5″-Props bei 21 g Rahmen | Nick träger |
| `fs_5_squashed` | (-97, 0, -70) | (97, 0, -70) | (-97, 0, 70) | (97, 0, 70) | 0 | Standard | 5″-Freestyle, Flips | schnelle Flips (Nick) | Rolls träger |
| `race_5_stretched` | (-70, 0, -95) | (70, 0, -95) | (-70, 0, 95) | (70, 0, 95) | 0 | Standard | 5″-Race | schlank (171 statt 140 km/h), leicht, mehr Roll-Autorität | Nick träger, 4-mm-Arme brechen früher |
| `deadcat_5` | (-100, 0, -55) | (100, 0, -55) | (-78, 0, 80) | (78, 0, 80) | (0, 0, 6) | Standard | 5″-Cinematic/Freestyle | keine Props im Bild | 7 g schwerer, etwas langsamer, Roll und Nick reagieren unterschiedlich |
| `hframe_5` | (-105, 0, -70) | (105, 0, -70) | (-105, 0, 70) | (105, 0, 70) | 0 | Arme 0,45 / Platte 0,55 | 5″-Cinematic | Kamera weit vorn, Props aus dem Bild, Flips spritziger | schwer, viel Luftwiderstand (124 km/h), Rolls träger |
| `lr_7_deadcat` | (-135, 0, -72) | (135, 0, -72) | (-105, 0, 112) | (105, 0, 112) | (0, 0, 8) | Standard | 7″-Long-Range, Cinematic | kaum Props im Bild (2 statt 8 %, nur Propspitzen an den Bildrändern) | schwerer als True-X |
| `xclass_13_h` | (-330, 0, -250) | (330, 0, -250) | (-330, 0, 250) | (330, 0, 250) | 0 | Arme 0,40 / Platte 0,60 | 13″ X-Class H-Frame | steif, Kamera vorn ohne Props | 200 g schwerer, mehr Luftwiderstand |

Einsatz, Pro und Contra der True-X-Rahmen:

| Id | Einsatz | Pro | Contra |
|---|---|---|---|
| `whoop_65_lite` | 65-mm-Whoop, Race | 0,8 g leichter als der Standard-Whoop, etwas längere Flugzeit | bricht früher (10 statt 14 m/s), Haltbarkeit 0,7 |
| `whoop_75_duct` | 75-mm-Whoop 1–2S | mehr Schub als 65 mm, trägt 2S | weniger wendig |
| `whoop_85_duct` | 85-mm-Whoop 2S, draußen | trägt 2S und digitale Kamera, schneller | drinnen groß, schwerer |
| `tp_2_x` | 2″-Toothpick 1–2S | extrem leicht, offene Props | Props im Bild, zerbrechlich |
| `tp_25_x` | 2,5″-Toothpick 2–4S | leicht und schnell (rund 100 km/h auf 4S, seit 1.0.0) | wenig Platz, kein Zubehör |
| `tp_3_x` | 3″-Toothpick 4–6S | Freestyle in 100 g | Props im Bild bei wenig Uptilt |
| `cw_25_duct` | 2,5″-Cinewhoop 4S | sicher drinnen, trägt Mini-Cam | Ducts bremsen (rund 85 km/h), wenig Flugzeit |
| `cw_35_duct` | 3,5″-Cinewhoop 6S | trägt eine volle Action-Cam | Ducts bremsen (rund 90 km/h) |
| `fs_5_bash` | 5″-Freestyle, Bando | 6-mm-Arme, übersteht fast alles | 13 g schwerer als der Standard-5″ |
| `lr_4_x` | 4″-Long-Range 6S | unter 250 g ohne Akku | wenig Reserve bei Wind |
| `lr_6_x` | 6″-Long-Range | effizienter als 5″ (über 16 min Schweben), wendiger als 7″ | weniger Reichweite als 7″ |
| `lr_8_x` | 8″-Long-Range 6–8S | trägt 6S2P-Li-Ion, über 40 min Schweben | träge (lange Motor-Reaktionszeit) |
| `xclass_10` | 10″ X-Class Einstieg | ein Drittel der Masse des 13″, spritzig auf 6S | kein Renn-X-Class (Regel ≥ 800 mm) |
| `xclass_12` | 12″ X-Class | leichter als 13″ | weniger Schub als 13″ |
| `xclass_13` | 13″ X-Class Race | Regelmaß 820 mm, maximaler Schub | 1,45 kg Rahmen, bricht bei harten Crashs |

Hinweise für Implementierer 4:
- Whoop-Rahmen haben `recycle: "none"` wie `propwash:whoop_65`.
- `whoop_65_lite`: Duct mit `clearance_mm` 1,5 (dünnere Ringe), `durability` 0,7 — Contra messbar über Crash-Verschleiß.
- `race_5_stretched`: bevorzugt 4S (Race-Klassen fliegen 4S und 6S), sonst würde 4S `battery_undersized` melden.
- `race_5_stretched` (seit 1.1.0): Akku unter dem Rahmen wie bei echten Race-Drohnen (`on_top: false`, `plate_mm` −2,8,
  wie `propwash:race_5` ab Propwash 0.5.0); Schwerpunkt `race5_4s` 18,7 → −3,6 mm, `race5_6s` 21,4 → −5,5 mm. Nur
  `role: race` zählt als Race-Rahmen; Toothpicks und X-Class behalten den Akku oben.
- Zubehör-Anker (`accessory_anchors`) für alle Rahmen mit `top`/`bottom` festlegen (Action-Cam oben vorn, LED unten);
  ohne Anker rechnet Propwash eine Ersatzformel, die bei Deadcat/H-Frame falsch aussieht.
- Der Flugschul-Szene „Rahmenformen“ (`justmoreparts:frames`, Besitzer 4, Client) dienen die fünf 5″-Rahmen als
  Akteure (True-X, Stretched, Squashed, Deadcat, H) mit denselben FREESTYLE-Teilen.
- X-Class-Rahmen: `role: x_class` (Tricks „Automatisch“ aus), Item-Seltenheit über
  `PropwashApi.partItems().registerItem(id, p -> p.rarity(Rarity.RARE))` **vor** `registerItems` (in `FrameModule.init()`).


### 7.2 Umsetzung (Implementierer 4, 2026-09-30)

**Änderungen gegenüber dem Entwurf** (alle im Prüfstand begründet, `frame/FrameBenchTest`):

| Teil | Entwurf | jetzt | Grund |
|---|---|---|---|
| `race_5_stretched` | 78 g | 84 g | `race5_6s` schwebte 17,2 min (Korridor ≤ 17); 84 g liegt im Bereich echter gestreckter Race-Rahmen (75–90 g) |
| `whoop_75_duct` | Kamera 15°, Standardlage | `camera.pos` (0, 8, −26), 18°; seit 1.1.0 (0, 19, −24) über den Ducts | Kamera vorn auf der Haube: 10 % statt 19,9 % Props/Ducts im Bild (Propwash warnt ab 20 %); 1.1.0: mit den Ducts im FPV-Bild lag das Objektiv zwischen den Ducts, jetzt darüber (`asset-style.md`, Kamerahalter) |
| `lr_6_x` | Aero 0,0140 · 0,90 | 0,0155 · 0,90 | GPS-Mast und breiter Rumpf; mit dem 2200-mAh-Preset sonst 132 km/h (Korridor ≤ 130) |

**Akzentfarben** (`visual.accent`, TPU-Teile der Modelle und der prozeduralen Form): `whoop_65_lite` #e84a8a, `whoop_75_duct`
#3fb950, `whoop_85_duct` #2f81f7, `tp_2_x` #f5c518, `tp_25_x` #ff7b29, `tp_3_x` #a371f7, `tp_35_stretched` #19b3a6,
`cw_25_duct` #e5534b, `cw_35_duct` #d29922, `fs_5_bash` #cf222e, `fs_5_squashed` #8250df, `race_5_stretched` #0969da,
`deadcat_5` #1a7f37, `hframe_5` #bf8700, `lr_4_x` #54aeff, `lr_6_x` #fb8f44, `lr_7_deadcat` #2da44e, `lr_8_x` #e16f24,
`xclass_10` #c9d1d9, `xclass_12` #f778ba, `xclass_13` #ffd33d, `xclass_13_h` #ff4d4d.

**Zubehör-Anker** (`accessory_anchors`, mm, Neigung °, Streifenlänge mm; oben = Drehachse der Action-Cam vor dem Akku,
unten = LED-Streifen): Whoops nur unten (0, 0,5, −25,6/−29,5/−33,5) mit 90° und 10/11/12 mm; `tp_3_x` oben (0, 18, −38) 15°;
`tp_35_stretched` oben (0, 21, −44) 15°; `cw_25_duct` (0, 25, −36) 10° / (0, −3, −20) 30° 20 mm; `cw_35_duct` (0, 31, −50) 10° /
(0, −3, −28) 30° 28 mm; `fs_5_bash` (0, 37, −69) 22° / (0, −3, −56) 34 mm; `fs_5_squashed` (0, 35, −62) 22° / (0, −3, −50) 34 mm;
`race_5_stretched` (0, 28,5, −70) 20° / (0, −2, −50) 28 mm; `deadcat_5` (0, 35, −66) 22° / (0, −3, −52) 34 mm; `hframe_5`
(0, 33, −82) 20° / (0, −3, −62) 40 mm; `lr_4_x` oben (0, 27, −52) 18°; `lr_6_x` (0, 35, −70) 20° / (0, −2,5, −58) 36 mm;
`lr_7_deadcat` (0, 37, −76) 20° / (0, −2,5, −62) 38 mm; `lr_8_x` (0, 39, −80) 20° / (0, −3, −66) 40 mm; `xclass_10` (0, 51, −100) /
(0, −4, −84) 50 mm; `xclass_12` (0, 61, −130) / (0, −5, −110) 60 mm; `xclass_13` (0, 66, −150) / (0, −5, −125) 60 mm;
`xclass_13_h` (0, 66, −230) / (0, −5, −170) 60 mm (alle oben 20°, unten 30°).

**Roll-/Nick-Autorität.** Seit Parts-API 1.4 liefert Propwash die Autoritäten selbst (`BuildStats#rollAuthorityRadS2()`,
`pitchAuthorityRadS2()`, `yawAuthorityRadS2()`, aus der Regelstrecke, aus der die Simulation auch die PIDs ableitet; API-Wunsch
W16). Der Prüfstand vergleicht damit jede Form mit einem True-X-Zwilling gleichen Radstands und gleicher Masse (Namensraum
`jmp_test`); die frühere eigene Schätzung (`frame/FrameAuthority`, Punktmassen und Stäbe nach Flugmodell-Spezifikation
§17.2) ist entfallen. Verhältnis Nick/Roll mit FREESTYLE-Teilen: True-X 0,96, Squashed-X 1,17, Stretched-X (Race) 0,78,
Deadcat 1,13, H-Frame 1,24; 3,5″-Toothpick Stretched-X 0,81 gegen True-X 0,90 — dieselben Richtungen wie die frühere
Schätzung und Propwashs eigene Charakterisierung (§17.8). Nur diese Richtungen stehen in der Hilfe, keine Zahlen.

**Modelle** (ab 1.0.1 aus der Stil-API, `python3 tools/assets/style_generate.py --category frame --category video --write`,
Regeln in `docs/design/asset-style.md` §4.5/§4.6): Drohnen-Modelle für 18 Rahmen und alle 7 Videosysteme mit den Bausteinen
und Malern der Propwash-Rahmen und -Kameras (Arme zweiteilig wie `frames.x_arms`, Motorpads mit Pad-Sheet, Carbonplatten mit
Bohrungen und Aufdruck, Alu-Abstandshalter und TPU in der Akzentfarbe, Kamerakäfig, Ducts wie `cinewhoop_3`/`whoop_65`),
Lack-Variante `_paint` (Kanäle `frame`/`tpu` bzw. `camera`/`antenna`) und Einträge in `assets/justmoreparts/drone_paint.json`.
Rahmen: Motorlagen aus `arms` (Deadcat gespreizt, Stretched-/Squashed-X, H-Frame mit Längsholmen und Querstreben),
Armbreite und -dicke nach Rolle, Rumpf bis vor die Kameralage, GPS-Mast ab 250 mm Long Range. Video: Kamera aus
`camera_width_mm`, Blende in der Akzentfarbe, Sender nach Bauform (Whoop-VTX, Race/Long Range mit Lollipop, leichtes
Digital-Modul, Air-Unit groß ab 20 g, über 600 mW schmaler mit längeren Antennen; die Pro-Einheit mit 18-mm-Linse und
Aufdruck „PRO“). Jedes Item hat ein eigenes Item-Modell mit den Anzeige-Transformationen der Propwash-Items. Die vier
X-Class-Rahmen sind größer als ein Minecraft-Modell (Elemente nur zwischen −16 und 32 Einheiten, ±192 mm um die Mitte):
das Drohnen-Modell `models/part/frame/<id>.json` ist gleichmäßig um den Anbaupunkt verkleinert (Faktor 0,46–0,82), die
Render-Definition skaliert es mit `transformation` wieder auf echte Größe (auch die Lack-Variante); im Inventar zeigt ein
verkleinertes Symbol im selben Stil (`models/item/icon/frame/<id>_icon.json`) den Rahmen mit kräftigeren Armen. H-Rahmen
teilen Längsholme und Querstreben in gleich lange Stücke mit gemeinsamer Textur (`hframe_5` 2,7 statt 1,8, `xclass_13_h`
1,26 statt 0,67 Texel je 8 mm), zu lange Bodenplatten bekommen eine eigene Nase. Whoop-Sender (`analog_whoop_25mw`,
`digital_micro`) tragen die Antenne steiler (25° bzw. 15°), damit sie in Hand, Rahmen und am Boden nicht größer wirken
als die Propwash-Kameras.

**Rezepte** (Gruppe `propwash_frame`/`propwash_video`, Reiter `equipment`): Whoops aus getrocknetem Seetang (Ducts) mit
Papier/Eisennugget/Faden, Toothpicks aus Stöcken und einer Carbonplatte, Cinewhoops Seetang + Carbon, 5″ aus Carbonplatten
mit Kennzeichen (Bash `CCC/CIC/C C`, Squashed Eisen seitlich, Race Goldnuggets, Deadcat V-Form, H-Frame H-Form), Long
Range mit Kompass, X-Class mit Eisenblock/Diamant (10″ `CIC/IBI/CIC` bis 13″ H `CBC/BDB/CBC`); Video aus Glas/Tönungsglas,
Platine bzw. HD-Chip, Kennzeichen Redstone (Racecore), Kupfer-/Eisen-Kühlkörper (Longshot, Horizon), Verstärker (Swift
Link), Diamant (Clarity HD Pro), Enderperle (Horizon LR). JUnit `frame/FrameDataTest` und `parts_sync.py --check` prüfen
Eindeutigkeit, auch gegen Propwash.

**Truhen-Beute** (`data/justmoreparts/loot_table/inject/frame/<stufe>.json`, je Truhe ein Pool, leer ist immer häufiger als
ein Fund; Zuordnung in `frame.FrameLoot`):

| Stufe | Truhen | Inhalt |
|---|---|---|
| `common` | Dorfhäuser (5), Verlies, verlassene Mine | Whoop-Rahmen, 2/2,5″-Toothpicks, Firefly Nano, Pixel Micro |
| `uncommon` | Wüsten-/Dschungeltempel, Außenposten, Schiffswrack-Vorrat, Prüfungskammer-Vorrat | 3/3,5″-Toothpicks, Cinewhoops, Bash/Squashed 5″, LR 4″, Racecore, Swift Link |
| `rare` | Festung (Gang, Kreuzung), Waldanwesen, Prüfungskammer-Tresor | Race/Deadcat/H 5″, LR 6–8″, X-Class 10″ (selten), Clarity HD Pro, Horizon LR, Longshot |
| `epic` | antike Stadt, Endstadt, Bastion, unheilvoller Tresor | Deadcat 5″, LR 8″, alle X-Class, Clarity HD Pro, Horizon LR |

Einspeisung: `frame.FrameLoot` liefert die Paare Vanilla-Tabelle → Stufe, `core.JmpLoot` meldet sie über
`PropwashApi.loot()` an (Parts-API 1.4); Propwash hängt je Paar einen Pool an, auf Fabric und NeoForge gleich.

**Flugschul-Szene** `justmoreparts:frames` (`frame.client.FrameShapesScene`, angemeldet in `FrameClient.init()`): fünf 5″-Drohnen
nebeneinander mit denselben eingebauten FREESTYLE-Teilen (True-X `propwash:freestyle_5`, `race_5_stretched`, `fs_5_squashed`,
`deadcat_5`, `hframe_5`), Schritte „Fünf Formen“ (Draufsicht, Kamera-Neigung 64°, Namensschild `callout.tag_<form>` an
jeder Drohne, Props stehen still), Stretched-X (Roll in 14 Ticks, Flip in 22, nur diese Drohne scharf), Squashed-X
(umgekehrt), Deadcat und H-Frame mit Blick von vorn oben, am Ende wieder die beschriftete Draufsicht; Texte unter
`justmoreparts.guide.scene.frames.*`.

## 8. Presets (Implementierer 4 — FRAMES + VIDEO)

Id `justmoreparts:<preset>`, Sprachschlüssel `justmoreparts.preset.<preset>`, Name in `DroneData` leer lassen (dann greift der Sprachschlüssel). Lackierung: je Preset eine eigene, gut lesbare Farbe für `frame` und `motors` (frei wählbar, im Bericht nennen). Alle Presets `creativeListed`.

| Preset | Name (de) | Name (en) | Rahmen | Stack | Motor | Prop | Video | Akku |
|---|---|---|---|---|---|---|---|---|
| `whoop75_1s` | Whoop 75 · 1S | Whoop 75 · 1S | `whoop_75_duct` | `aio_whoop_1s_6a` | `m0802_19000` | `p40_17_3b_pc` | `analog_whoop_25mw` | `b1s_450_hv` |
| `whoop85_2s` | Whoop 85 · 2S digital | Whoop 85 · 2S digital | `whoop_85_duct` | `aio_whoop_2s_12a` | `m1102_13500` | `p45_17_4b_bi` | `digital_micro` | `b2s_450_hv` |
| `toothpick2_2s` | Toothpick 2″ · 2S | Toothpick 2″ · 2S | `tp_2_x` | `aio_whoop_2s_12a` | `m1103_11000` | `p51_20_3b_pc` | `analog_whoop_25mw` | `b2s_450_hv` |
| `toothpick3_4s` | Toothpick 3″ · 4S | Toothpick 3″ · 4S | `tp_3_x` | `aio_tp_4s_20a` | `m1404_3800` | `p76_25_3b_hard` | `digital_micro` | `propwash:b4s_650` |
| `toothpick35_6s` | Toothpick 3,5″ · 6S | Toothpick 3.5″ · 6S | `tp_35_stretched` | `aio_tp_6s_20a` | `m1505_2500` | `p89_30_3b_pc` | `digital_micro` | `b6s_750` |
| `cinewhoop25_4s` | Cinewhoop 2,5″ · 4S | Cinewhoop 2.5″ · 4S | `cw_25_duct` | `aio_tp_4s_20a` | `m1404_3800` | `p65_22_4b_bi` | `digital_micro` | `b4s_850` |
| `cinewhoop35_6s` | Cinewhoop 3,5″ · 6S | Cinewhoop 3.5″ · 6S | `cw_35_duct` | `aio_20_6s_45a` | `m2004_1700` | `p89_25_5b_pc_duct` | `digital_hd_pro` | `b6s_850` |
| `bash5_6s` | Bash 5″ · 6S | Bash 5″ · 6S | `fs_5_bash` | `stack_30_6s_60a` | `m2207_1950` | `p130_41_3b_soft` | `digital_hd_pro` | `propwash:b6s_1300` |
| `squashed5_6s` | Squashed 5″ · 6S | Squashed 5″ · 6S | `fs_5_squashed` | `stack_30_6s_60a` | `m2207_1950` | `p130_36_3b_bi` | `digital_hd_pro` | `propwash:b6s_1300` |
| `deadcat5_6s` | Deadcat 5″ · 6S | Deadcat 5″ · 6S | `deadcat_5` | `stack_30_6s_60a` | `m22075_1850` | `p130_36_3b_bi` | `digital_hd_pro` | `propwash:b6s_1300` |
| `race5_4s` | Race 5″ Slim · 4S | Race 5″ Slim · 4S | `race_5_stretched` | `stack_30_4s_55a` | `m23065_2450` | `p130_46_3b_hard` | `digital_low_latency` | `b4s_1500` |
| `race5_6s` | Race 5″ Slim · 6S analog | Race 5″ Slim · 6S analog | `race_5_stretched` | `stack_30_6s_60a` | `m23065_2020` | `p130_50_3b_gfn` | `analog_race_800` | `propwash:b6s_1300` |
| `hframe5_6s` | H-Frame 5″ · 6S | H-Frame 5″ · 6S | `hframe_5` | `stack_30_6s_60a` | `m2207_1950` | `p130_35_4b_pc` | `digital_hd_pro` | `propwash:b6s_1300` |
| `lr4_6s_lion` | Long Range 4″ · 6S Li-Ion | Long Range 4″ · 6S Li-Ion | `lr_4_x` | `aio_20_6s_45a` | `m2004_1700` | `p102_30_3b_gfn` | `digital_micro` | `b6s_3000_lion` |
| `lr6_6s` | Long Range 6″ · 6S | Long Range 6″ · 6S | `lr_6_x` | `stack_30_6s_60a` | `m2507_1500` | `p152_33_3b_pc` | `digital_lr` | `b6s_2200` |
| `lr7_deadcat_lion` | Deadcat 7″ · 6S Li-Ion | Deadcat 7″ · 6S Li-Ion | `lr_7_deadcat` | `stack_30_6s_60a` | `m28065_1300` | `p178_40_2b_cfn` | `digital_lr` | `b6s_4500_lion` |
| `lr7_8s` | Deadcat 7″ · 8S | Deadcat 7″ · 8S | `lr_7_deadcat` | `stack_30_8s_65a` | `m2807_1100` | `p178_40_2b_cfn` | `digital_lr` | `b8s_2200` |
| `lr8_6s_lion` | Long Range 8″ · 6S2P Li-Ion | Long Range 8″ · 6S2P Li-Ion | `lr_8_x` | `stack_30_8s_65a` | `m2807_1100` | `p203_45_2b_cfn` | `digital_lr` | `b6s_8400_lion` |
| `xclass10_6s` | X-Class 10″ · 6S | X-Class 10″ · 6S | `xclass_10` | `stack_30_8s_100a` | `m3115_900` | `p254_50_2b_carbon` | `digital_hd_pro` | `b6s_5000` |
| `xclass12_8s` | X-Class 12″ · 8S | X-Class 12″ · 8S | `xclass_12` | `stack_30_8s_100a` | `m4214_400` | `p305_60_2b_cfn` | `digital_lr` | `b8s_5000` |
| `xclass13_8s` | X-Class 13″ · 8S | X-Class 13″ · 8S | `xclass_13` | `stack_30_8s_100a` | `m4214_400` | `p330_70_2b_carbon` | `digital_lr` | `b8s_5000` |
| `xclass13h_8s` | H-Frame 13″ · 8S | H-Frame 13″ · 8S | `xclass_13_h` | `stack_30_8s_100a` | `m4214_400` | `p330_60_3b_cfn` | `digital_lr` | `b8s_5000` |

### 8.1 Prüfstand-Ergebnis der Presets (Architekt-Vorlauf, 2026-09-29)

Gemessen mit `PropwashApi.builds().analyze` (Propwash 0.4.0, API 1.0.0) auf den Entwurfswerten dieses Katalogs (Spalten wie `Bench.header()`: Masse g, Schub/Gewicht, Schwebegas %, Schwebe-/Reiseflugzeit min, Topspeed km/h, Vollgas-Strom A, ESC-/Akku-Last %, Motortemperatur bei Dauervollgas im Stand °C, Props im Bild %, Probleme/Warnungen).

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
whoop75_1s                            32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  19.9   
whoop85_2s                            56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0   
toothpick2_2s                         57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   43.6  10.7   
toothpick3_4s                        142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4   
toothpick35_6s                       222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5   
cinewhoop25_4s                       193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2   
cinewhoop35_6s                       339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6   
bash5_6s                             530.3  11.7  21.2   14.9    6.2    140  124.7    52    80   92.3   5.8   
squashed5_6s                         515.5  12.2  21.6   15.8    6.7    139  112.5    47    72   88.6   4.3   
deadcat5_6s                          534.3  11.1  23.6   15.0    7.0    132  101.4    42    65   85.2   0.3   
race5_4s                             407.1  11.2  21.7   16.4    7.3    155  124.7    57    69   75.9   0.0   
race5_6s                             462.9  14.1  17.4   17.2    5.8    187  154.1    64    99   96.9   0.0   W[][heavy_sag]
hframe5_6s                           540.5  12.1  20.8   15.3    6.3    127  125.4    52    80   92.5   0.0   
lr4_6s_lion                          442.8   3.6  51.0   28.3   24.8     91   27.6    15    77   87.7   8.3   W[battery_weak][]
lr6_6s                               637.8  10.8  24.2   19.3    9.7    130  107.0    44    59  101.7   8.7   
lr7_deadcat_lion                     881.7   4.8  28.0   33.5   25.3    101   75.9    31   169   97.3   0.1   W[battery_weak][heavy_sag, battery_overload]
lr8_6s_lion                         1329.7   4.9  33.0   42.1   31.7    109  108.3    42   129  153.0   7.9   W[battery_weak][motor_thermal]
xclass10_6s                         1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  126.4  14.7   
xclass12_8s                         2629.7   5.5  39.2   19.7   14.8     91  121.5    30    40  114.5   5.4   
xclass13_8s                         3123.7   5.8  35.9   16.8   12.1     91  169.4    42    56  128.9   3.0   
xclass13h_8s                        3355.7   5.7  35.4   16.1   11.6     83  181.0    45    60  132.9   0.0
```

Nachtrag 1.0.2 (Propwash 0.4.1): Die Spalte „Bild%“ oben stammt aus der alten Analyse. Mit der Rechnung wie im FPV-Bild
(Web-Export 1.0.2, im Spiel an gerenderten Pixeln geprüft) zeigen die Presets: `whoop75_1s` 7,65, `whoop85_2s` 1,35,
`toothpick2_2s` 2,46, `toothpick3_4s` 11,58, `toothpick35_6s` 7,61, `cinewhoop25_4s` 17,36, `cinewhoop35_6s` 19,93,
`bash5_6s` 11,72, `squashed5_6s` 6,90, `deadcat5_6s` 0, `race5_4s` 1,93, `race5_6s` 0, `hframe5_6s` 0, `lr4_6s_lion` 8,93,
`lr6_6s` 17,94, `lr7_deadcat_lion` 1,84, `lr7_8s` 2,02 (beide seit 1.0.3, in 1.0.2 0), `lr8_6s_lion` 17,11, `xclass10_6s` 14,06, `xclass12_8s` 3,48,
`xclass13_8s` 1,65, `xclass13h_8s` 0 % – keine Warnung „Props im Bild“ (Schwelle 30 %). Referenzen Propwash: Whoop 8,78,
Cinewhoop 20,87, Freestyle 11,73, Racer 0, Long Range 19,50 %.

Nachtrag 1.0.0 öffentlich (Kamerahalter, Architektur §10.15, Propwash 0.4.0 öffentlich): `whoop75_1s` 7,65, `whoop85_2s`
6,20, `toothpick2_2s` 3,96, `toothpick3_4s` 5,43, `toothpick35_6s` 7,61, `cinewhoop25_4s` 6,81, `cinewhoop35_6s` 6,85,
`bash5_6s` 7,94, `squashed5_6s` 3,03, `deadcat5_6s` 0, `race5_4s` 9,77, `race5_6s` 10,23, `hframe5_6s` 0, `lr4_6s_lion`
6,16, `lr6_6s` 8,08, `lr7_deadcat_lion` 1,84, `lr7_8s` 2,02, `lr8_6s_lion` 6,61, `xclass10_6s` 7,07, `xclass12_8s` 3,32,
`xclass13_8s` 2,32, `xclass13h_8s` 0 %, im Spiel an gerenderten Pixeln höchstens 0,08 Prozentpunkte daneben. Referenzen
Propwash: Whoop 9,02, Cinewhoop 7,28, Freestyle 8,37, Racer 11,20, Long Range 9,47 %. Mit FREESTYLE-Teilen:
`race_5_stretched` 12,99, `propwash:freestyle_5` 8,37, `fs_5_squashed` 3,03, `deadcat_5` und `hframe_5` 0 %.

Nachtrag 2026-10-02, in 1.0.0 (Kamerahalter `race_5_stretched` nachkorrigiert, Architektur §10.18, Propwash
`docs/design/realism/props-in-view.md` §5.1): Objektiv (0; 18,0; −59,6) → (0; 16,5; −47,5), Uptilt 40°; `race5_4s`
9,77 → 11,31, `race5_6s` 10,23 → 12,19 %, alle anderen Presets unverändert. Mit FREESTYLE-Teilen `race_5_stretched`
12,99 → 13,40 %; Motor-Referenz 2207.5 (`m22075_2700`, Bi-Blend) 15,15 → 14,80 %.

Nachtrag 2026-10-02, Endstand 1.0.0 (Sichtfeld der Kamera im Videomodul, Architektur §10.20, Propwash
`props-in-view.md` §9.4): `whoop75_1s` 8,64, `whoop85_2s` 6,15, `toothpick2_2s` 4,25, `toothpick3_4s` 5,35,
`toothpick35_6s` 6,13, `cinewhoop25_4s` 6,67, `cinewhoop35_6s` 6,85, `bash5_6s` 7,94, `squashed5_6s` 3,03, `deadcat5_6s` 0,
`race5_4s` 11,08, `race5_6s` 11,73, `hframe5_6s` 0, `lr4_6s_lion` 6,08, `lr6_6s` 8,08, `lr7_deadcat_lion` 1,84, `lr7_8s`
2,02, `lr8_6s_lion` 6,61, `xclass10_6s` 7,07, `xclass12_8s` 3,32, `xclass13_8s` 2,32, `xclass13h_8s` 0 % – alle im
Zielbereich ihrer Rahmenart. Referenzen Propwash: Whoop 8,81, Cinewhoop 7,00, Freestyle 8,37, Racer 10,97, Long Range
9,47 %.

Offene Punkte der Presets für die Implementierer (Abnahme: Tabelle 1.7), Stand 1.0.0 alle erledigt:
- `lr8_6s_lion` meldete `motor_thermal` (153 °C bei Dauervollgas im Stand, Drosselung 140 °C) – **erledigt**: Implementierer 1
  hob die Fahrtwind-Kühlung des `m2807_1100` an (§3.1), heute 96 °C ohne Warnung; die Li-Ion-Warnungen bleiben.
- `race5_6s` meldet `heavy_sag` — erlaubt (ehrlicher Nachteil 6S-Race mit 1300 mAh).

---

### 8.2 Prüfstand-Ergebnis Implementierer 4 (2026-09-30)

Gemessen mit `frame/FrameBenchTest` (Motoren, Props, Akkus und Stacks aus `catalog-draft`, Rahmen und Video aus den
Ressourcen). Änderung am Preset: `lr6_6s` fliegt mit `b6s_2200` statt `b6s_1800` — mit 1800 mAh waren es 9,7 min Reiseflug
(Korridor ≥ 10); Rahmen-Masse und Aero änderten daran weniger als 0,2 min.

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
whoop75_1s                            32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  10.0
whoop85_2s                            56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0
toothpick2_2s                         57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   43.6  10.7
toothpick3_4s                        142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4
toothpick35_6s                       222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5
cinewhoop25_4s                       193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2
cinewhoop35_6s                       339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6
bash5_6s                             530.3  11.7  21.2   14.9    6.2    140  124.7    52    80   92.3   5.8
squashed5_6s                         515.5  12.2  21.6   15.8    6.7    139  112.5    47    72   88.6   4.3
deadcat5_6s                          534.3  11.1  23.6   15.0    7.0    132  101.4    42    65   85.2   0.3
race5_4s                             413.1  11.1  21.9   16.1    7.3    155  124.7    57    69   75.9   0.0
race5_6s                             468.9  13.9  17.6   17.0    5.8    187  154.1    64    99   96.9   0.0   W[][heavy_sag]
hframe5_6s                           540.5  12.1  20.8   15.3    6.3    127  125.4    52    80   92.5   0.0
lr4_6s_lion                          442.8   3.6  51.0   28.3   24.8     91   27.6    15    77   87.7   8.3   W[battery_weak][]
lr6_6s                               693.2  10.1  25.5   21.6   11.2    127  109.3    45    66  107.1   7.3
lr7_deadcat_lion                     881.7   4.8  28.0   33.5   25.3    101   75.9    31   169   97.3   0.1   W[battery_weak][heavy_sag, battery_overload]
lr8_6s_lion                         1329.7   4.9  33.0   42.1   31.7    109  108.3    42   129  153.0   7.9   W[battery_weak][motor_thermal]
xclass10_6s                         1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  126.4  14.7
xclass12_8s                         2629.7   5.5  39.2   19.7   14.8     91  121.5    30    40  114.5   5.4
xclass13_8s                         3123.7   5.8  35.9   16.8   12.1     91  169.4    42    56  128.9   3.0
xclass13h_8s                        3355.7   5.7  35.4   16.1   11.6     83  181.0    45    60  132.9   0.0
tp_25_x Referenz 4S                   97.4   6.7  26.3   11.1    6.5     89   35.0    44    97  119.7   9.6
whoop_65_lite Referenz                22.4   3.6  51.2    8.3    7.0     36    7.1    28    71   29.1  18.4
```

Referenzaufbauten der Rahmen ohne Preset: `tp_25_x` + `aio_tp_4s_20a` + `m12025_5500` + `p65_25_3b_pc` + `digital_micro` +
`b4s_450`; `whoop_65_lite` + `aio_whoop_1s_6a` + `m0702_23000` + `p31_16_2b_pc` + `analog_whoop_25mw` + `b1s_250_hv`;
`analog_lr_1600` im `lr_6_x`. Formvergleich mit eingebauten FREESTYLE-Teilen (`stack_30`, `m2207_1750`, `p51_3b`, `digital_hd`,
`b6s_1300`): True-X 525 g/140 km/h/1,9 %, Bash 538/135/0,0 (Aufprall 10 statt 8 m/s), Squashed 524/137/0,0, Stretched-Race
484/171/0,0 (Aufprall 6,5 m/s), Deadcat 532/133/0,0, H-Frame 545/124/0,0.

Damals offene Punkte für die Integration, Stand 1.0.0 beide **erledigt** (die Liste „offen“ in `FrameBenchTest` ist entfernt,
jedes Preset muss in seinem Korridor liegen):
- `lr8_6s_lion`: `motor_thermal` (153 °C) wie im Vorlauf – erledigt über die Kühlung des `m2807_1100` (§3.1), heute 96 °C.
- `lr4_6s_lion`: Schwebegas 51,0 % (Korridor ≤ 50 %) – erledigt mit den Referenz-Beiwerten des `p102_30_3b_gfn` (1.0.5, §13),
  heute 37,0 %. Der Rahmen hätte nicht helfen können: 6 g weniger Rahmen senkten das Schwebegas nur auf 50,5 %.

**Lackierung der Presets** (Rahmen · TPU · Motoren): whoop75_1s #3fb950 · – · #e6edf3, whoop85_2s #2f81f7 · – · #f0f6fc,
toothpick2_2s #1b1d20 · – · #f5c518, toothpick3_4s #1b1d20 · – · #a371f7, toothpick35_6s #1b1d20 · – · #19b3a6, cinewhoop25_4s
#1b1d20 · #e5534b · #c9d1d9, cinewhoop35_6s #1b1d20 · #d29922 · #30363d, bash5_6s #1b1d20 · #cf222e · #cf222e, squashed5_6s
#2d1b45 · #8250df · #8250df, deadcat5_6s #1b1d20 · #1a7f37 · #2da44e, race5_4s #0b3d91 · #0969da · #54aeff, race5_6s #1b1d20 ·
#ff7b29 · #ff7b29, hframe5_6s #1b1d20 · #bf8700 · #bf8700, lr4_6s_lion #1b1d20 · – · #54aeff, lr6_6s #1b1d20 · #fb8f44 · #fb8f44,
lr7_deadcat_lion #1b1d20 · #2da44e · #7ee787, lr7_8s #1b1d20 · #1f6feb · #79c0ff, lr8_6s_lion #1b1d20 · #e16f24 · #e16f24, xclass10_6s #1b1d20 · #3fb950 · #3fb950,
xclass12_8s #1b1d20 · #f778ba · #f778ba, xclass13_8s #1b1d20 · #ffd33d · #ffd33d, xclass13h_8s #1b1d20 · #ff4d4d · #ff4d4d (X-Class seit 02.10. carbon-schwarz wie alle anderen Rahmen).
Presets sind `creativeListed` und erscheinen damit ohne eigenen Kreativ-Eintrag im JMP-Tab „Propwash: Just More Parts“
(vor den Teil-Items, `core.JmpTab`).

### 8.3 Prüfstand nach der Integration (1.0.0, 2026-09-30)

Gemessen mit `frame/FrameBenchTest` auf den **endgültigen** Teile-Dateien aller Kategorien (keine Entwürfe mehr, Propwash
0.4.0, API 1.0.0). Alle Presets sind baubar und ohne Probleme; Warnungen nur, wo Tabelle 1.7 sie erlaubt.

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
whoop75_1s                            32.9   4.0  44.5   11.3    8.9     39   12.6    51    47   36.2  10.0
whoop85_2s                            56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0
toothpick2_2s                         57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   38.2  10.7
toothpick3_4s                        142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4
toothpick35_6s                       222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5
cinewhoop25_4s                       193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2
cinewhoop35_6s                       339.9   4.2  46.8   10.2    8.0     55   24.8    14    29   79.7  15.6
bash5_6s                             530.3  11.7  21.2   14.9    6.2    140  124.7    52    80   92.3   5.8
squashed5_6s                         515.5  12.2  21.6   15.8    6.7    139  112.5    47    72   88.6   4.3
deadcat5_6s                          534.3  11.1  23.6   15.0    7.0    132  101.4    42    65   85.2   0.3
race5_4s                             413.1  11.1  21.9   16.1    7.3    155  124.7    57    69   75.9   0.0
race5_6s                             468.9  13.9  17.6   17.0    5.8    187  154.1    64    99   96.9   0.0   W[][heavy_sag]
hframe5_6s                           540.5  12.1  20.8   15.3    6.3    127  125.4    52    80   92.5   0.0
lr4_6s_lion                          442.8   3.6  51.0   28.3   24.8     91   27.6    15    77   87.7   7.3   W[battery_weak][]
lr6_6s                               693.2  10.1  25.5   21.6   11.2    127  109.3    45    66  107.1   7.3
lr7_deadcat_lion                     881.7   5.2  27.7   36.4   26.7    104   73.7    31   164   78.5   0.1   W[battery_weak][heavy_sag, battery_overload]
lr8_6s_lion                         1329.7   4.9  32.9   42.4   31.9    110  108.0    41   129  125.0   8.2   W[battery_weak][]
xclass10_6s                         1692.5   8.6  25.6   24.5   13.8    115  220.6    55    74  110.5  14.7
xclass12_8s                         2629.7   5.5  39.2   19.7   14.8     91  121.5    30    40  114.5   5.4
xclass13_8s                         3123.7   5.9  35.7   18.0   12.8     92  160.4    40    53  125.9   3.0
xclass13h_8s                        3355.7   5.7  35.4   16.1   11.6     83  181.0    45    60  132.9   0.0
```

Ergebnis der Integration:
- `lr8_6s_lion`: erledigt — 125 °C Dauervollgas im Stand (Drosselung 140 °C) durch die Fahrtwind-Kühlung des
  `m2807_1100` (Abschnitt 3.1); nur die für Li-Ion erlaubte Warnung `battery_weak` bleibt.
- `lr4_6s_lion`: Schwebegas 51,0 % liegt 1 Prozentpunkt über der Tabelle 1.7 und damit innerhalb der Toleranz von ±5 %,
  die jetzt alle Prüfstände einheitlich anwenden (Motoren, Akkus, Presets). Ein leichterer Aufbau (1505/2500 KV mit
  4″-Zweiblatt: 36 % Schwebegas) unterschritte die Mindestmasse 440 g; das Preset bleibt deshalb wie entworfen.
- `race5_6s`: `heavy_sag` wie vorgesehen (ehrlicher Nachteil 6S-Race mit 1300 mAh).
- Durch die endgültigen Werte der anderen Kategorien verschoben sich einige Zahlen in Shift-Hilfen; angepasst und im
  jeweiligen Prüfstand neu belegt: `m0702_27000` (250-mAh-Akku läuft mit 86 % Last „am Limit“, nicht mehr „zu schwach“,
  seit der 250-mAh-Akku 50C hat), `p203_40_3b_gfn` („Motoren heißer als mit dem Zweiblatt“ statt „überhitzen“),
  `b4s_1800` (über 13 min), `b6s_4500_lion` (Schub/Gewicht 5,2: „nur gut 5 × Gewicht“), `b8s_5000`/`b8s_5000_ss` im 13″
  (18,0 bzw. 21,4 min).
- Akku-Referenzaufbauten an der Korridorgrenze (mit ±5 % im Korridor): Whoop 75 2S 300 mAh 13,2 min Schweben (bis 13),
  X-Class 13″ 8S 5000 Semi-Solid 15,2 min Reiseflug (bis 15).

### 8.4 Realismus-Review (2026-09-30)

Ein Review hat alle Klassen mit `PropwashApi.builds().analyze` gegen reale Klassenwerte gemessen, jedes Teil in jedem
Klassen-Aufbau gegen alle passenden Teile getauscht (Dominanz, 1 % Toleranz) und die Shift-Hilfen stichprobenartig
nachgemessen. Umgesetzt:

| Befund | Änderung | Wirkung (Prüfstand) |
|---|---|---|
| 7″ auf 8S nicht baubar | `lr_7_deadcat` `battery_fit.cells` 6–6 → 6–8; Preset `lr7_8s` (2807/1100, 8S 2200, 65-A-Stack) | 897 g, T/W 11,7, 21,7 % Schwebegas, 24,2/10,5 min, 145 km/h, keine Warnung; `m28065_1050` wird im 7″ auf 8S-LiPo heiß (`motor_thermal`, Hilfe warnt) |
| X-Class zu langsam (13″ 92 km/h, langsamer als ein 5″) | Stirnfläche `xclass_10` 0,035 → 0,0275, `xclass_12` 0,06 → 0,032, `xclass_13` 0,08 → 0,040, `xclass_13_h` 0,095 → 0,046 m² (Rumpf, Kamera und Akku wachsen nicht mit dem Radstand) | 10″ 124, 12″ 113, 13″ 117 (mit 530 KV 133), 13″ H 107 km/h; T/W, Schweben und Ströme unverändert |
| `digital_hd_pro` mit 12,5 g unrealistisch leicht | 24 g (reale Top-HD-Systeme 29–37 g mit Halter und Antenne); Hilfe: weniger Latenz und mehr Reichweite statt „leichter“ | Cinewhoop 3,5″ 351 g, Bash 5″ 542 g |
| `p203_40_3b_gfn` streng dominiert (an der Stromgrenze gilt Schub ∝ ct/cp) | `cp` 0,0509 → 0,0470 | 6S2P: T/W 5,1 statt 4,9, 44,7 statt 42,4 min Schweben, 108 statt 110 km/h, 7 g schwerer, 133 statt 125 °C; der Zweiblatt behält Tempo, Kühle, Reaktion und Steifigkeit |
| `p40_17_3b_pc` kaum mehr Schub als der Zweiblatt (+0,75 %) | `ct` 0,0889 → 0,0915 | 85 mm 2S: T/W 5,4 gegen 5,1 beim Zweiblatt (vorher +0,75 %); der Vierblatt fliegt jetzt kürzer als 2 und 3 Blätter (realistische Reihenfolge) |
| `m12025_11500` fast dominiert von `m1103_11000` (gleiches Statorvolumen, gleiches KV·√R, schwerer) | breiterer Stator, mehr Drehmoment: R 0,1001 → 0,0800 Ω, 4,9 → 4,7 g; gleicher Stator `m12025_5500`: R 0,4378 → 0,350 Ω, 4,7 g | reagiert schneller als 1103 (30 statt 32 ms), mit 2,5″-Props mehr Topspeed; das 1103 bleibt leichter und fliegt länger; `m12025_5500` auf 4S 111 statt 120 °C |
| `b6s_3000_ss` gegen `b6s_3000_lion` im 4″ in allen Flugwerten besser | 288,8 → 312 g (222 Wh/kg auf Pack-Ebene) | Li-Ion jetzt 10,6 g leichter und im Reiseflug länger (4″ 24,8 gegen 24,3 min, 6″ 21,3 gegen 16,7 min); Semi-Solid behält T/W und Schwebezeit |
| `p31_14_3b_bi` nur 0,01 g schwerer als `propwash:p31_3b` | 0,29 → 0,32 g | Contra „etwas schwerer“ spürbar (Motorreaktion 47 statt 45 ms) |
| `cw_35_duct` am unteren Tempo-Rand | Stirnfläche 0,024 → 0,020 m² | rund 60 km/h (mit `m1505_2500` 73 km/h) |
| Kaum messbare oder bedeutungslose Hilfe-Zeilen | Rezept-„günstiger“/„teurer“ bei `p203_*`, `p178_35_3b_pc`, `p178_40_2b_cfn` gestrichen (eine Carbonplatte kostet 2 Kohle und 1 Faden); `m1103_11000` „reagiert schneller“ gestrichen; `p40_20_2b_pc`, `p40_15_4b_pc` auf Reiseflugzeit, `p178_40_2b_cfn` auf Schwebezeit präzisiert; `b1s_250_hv` nennt die 1,2 g statt „bestes Schub/Gewicht“ (+1 %); `m4214_330` nur noch „auf 8S“ (kein 10–12S-Akku oder -ESC) | alle Vergleichsaussagen der Prüfstände verlangen jetzt 1 % Abstand (Abschnitt 1.6) |

Referenzaufbauten, die dadurch angepasst wurden: der Prop-Prüfstand misst den 2,5″ auf 4S mit `digital_micro` (wie
Motor- und Rahmen-Prüfstand); `p102_25_2b_pc` im 4″ mit Semi-Solid liegt mit 50,2 % Schwebegas 0,2 Punkte über der
Long-Range-Grenze und ist dort wie das Preset `lr4_6s_lion` (51 %) als bekannte Grenze der 4″-Klasse ausgenommen; die
frühere Masse-Ausnahme der beiden 4″-Props entfällt (449,8 und 453,4 g). Die Aussage „2 Blätter fliegen länger als 3“ gilt
für alle Größen außer 8″, wo der flachere Dreiblatt an der Stromgrenze länger schwebt.

Presets nach dem Review (`frame/FrameBenchTest`):

```
Aufbau                                   g   T/W Schw%  Schw' Reise'   km/h      A  ESC%  Akk%  Mot°C Bild%  Probleme / Warnungen
whoop75_1s                            32.9   4.1  43.6   11.7    9.1     40   12.6    51    47   36.2  10.0
whoop85_2s                            56.7   6.8  27.8   12.4    7.4     57   28.5    59    85   46.9  10.0
toothpick2_2s                         57.1   6.3  30.3   11.0    7.2     74   28.6    59    85   38.2  10.7
toothpick3_4s                        142.8   6.6  33.0   11.0    6.9     91   38.4    48    62   79.1   8.4
toothpick35_6s                       222.5   7.6  28.0   13.6    7.8    109   46.3    58    69  103.0   7.5
cinewhoop25_4s                       193.3   4.0  47.5   10.1    7.9     55   25.9    32    30   59.9  14.2
cinewhoop35_6s                       351.4   4.1  47.8    9.4    7.5     60   24.9    14    29   79.6  15.7
bash5_6s                             541.8  11.5  21.6   14.0    6.1    140  124.9    52    80   92.2   5.9
squashed5_6s                         527.0  11.9  22.0   14.9    6.6    139  112.6    47    72   88.5   4.4
deadcat5_6s                          545.8  10.8  24.0   14.1    6.8    132  101.5    42    65   85.2   0.3
race5_4s                             413.1  11.1  21.9   16.1    7.3    155  124.7    57    69   75.9   0.0
race5_6s                             468.9  13.9  17.6   17.0    5.8    187  154.1    64    99   96.9   0.0   W[][heavy_sag]
hframe5_6s                           552.0  11.9  21.1   14.4    6.2    127  125.6    52    80   92.4   0.0
lr4_6s_lion                          442.8   3.6  51.0   28.3   24.8     91   27.6    15    77   87.7   7.3   W[battery_weak][]
lr6_6s                               693.2  10.1  25.5   21.6   11.2    127  109.3    45    66  107.1   7.3
lr7_deadcat_lion                     881.7   5.2  27.7   36.4   26.7    104   73.7    31   164   78.5   0.1   W[battery_weak][heavy_sag, battery_overload]
lr7_8s                               896.9  11.7  21.7   24.2   10.5    145  137.5    53    62  123.6   0.0
lr8_6s_lion                         1329.7   4.9  32.9   42.4   31.9    110  108.0    41   129  125.0   8.2   W[battery_weak][]
xclass10_6s                         1704.0   8.5  25.7   23.9   13.8    124  220.7    55    74  110.4  14.7
xclass12_8s                         2629.7   5.5  39.2   19.7   15.0    113  121.5    30    40  114.5   5.4
xclass13_8s                         3123.7   5.9  35.7   18.0   13.0    117  160.4    40    53  125.9   3.0
xclass13h_8s                        3355.7   5.7  35.4   16.1   11.8    107  181.0    45    60  132.9   0.0
```

Dadurch verschobene Zahlen in Shift-Hilfen, neu belegt: `b6s_850` „über 9 min“ im 3,5″-Cinewhoop (9,4 min),
`b6s_5000` „rund 21 min“ im 10″ (21,5 min, seit 1.0.4), `cw_35_duct` „rund 60 km/h“. Nicht in JMP lösbar (Basis-Kalibrierung und
API-Wunsch W26): Architektur §9.2.

---

## 9. Rezepte (alle Besitzer, je eigene Teile)

- Datei `data/justmoreparts/recipe/<id>.json`, Typ `minecraft:crafting_shaped`, Gruppe `propwash_<kategorie>`,
  Kategorie `equipment`. Zutaten: Propwash-Zwischenteile (`propwash:carbon_plate`, `copper_coil`, `circuit_board`,
  `lipo_cell`, `hd_chip`) plus Vanilla-Material; Aufwand wächst mit der Größe (Whoop < 5″ < X-Class).
- Jedes Rezept muss eindeutig sein — auch gegenüber allen Propwash-Rezepten. `tools/parts_sync.py --check` erkennt
  gleiche Muster. Empfehlung: Grundmuster je Kategorie und Größenklasse, dazu ein **Kennzeichen-Material** je Variante
  (z. B. KV-Stufe über Redstone/Glowstone/Amethyst, Material über Kelp/Glas/Kohle/Netherit-Schrott).
- Freischalten: `tools/parts_sync.py --write` legt je Rezept einen Fortschritt `advancement/recipes/<kategorie>/<id>.json`
  an (Auslöser: ein beliebiges Teil aus `#propwash:parts` im Inventar).
- Tags `#propwash:parts/<kategorie>` erzeugt dasselbe Werkzeug aus den Teile-Dateien (`"replace": false`).

## 10. JSON-Entwurf

Alle Werte dieses Katalogs liegen als fertige Entwurfsdateien unter `docs/design/catalog-draft/<kategorie>/<id>.json`
(Schema `docs/design/parts-api-data.md` im Propwash-Repository, Standardwerte weggelassen, `sort` nach der Regel unten).
Mit genau diesen Dateien wurde Abschnitt 8.1 gemessen. Jeder Implementierer **kopiert** seine Kategorien nach
`common/src/main/resources/data/justmoreparts/propwash_parts/<kategorie>/` und arbeitet dort weiter (Akzentfarben,
Kalibrierung, Anker). Danach gilt nur noch die Datei in `resources`; den Ordner `catalog-draft` löscht der Integrator,
wenn alle vier Kategorien übernommen sind. **Erledigt mit 1.0.0:** Der Ordner ist gelöscht; es gelten nur noch die Dateien unter
`common/src/main/resources/data/justmoreparts/propwash_parts/`. Beispiel `motor/m2207_1950.json`:

```json
{"format": 1, "mass_g": 32.8, "stator": "2207", "kv": 1950, "mount": "propwash:m22", "max_current_a": 50.0,
 "resistance_ohm": 0.0443, "cells": [5, 6], "sort": 116}
```

`sort`: Motoren 100–199 nach Stator und KV, Props 200–299 nach Durchmesser, Akkus 300–399 nach Zellen und Kapazität,
Stacks 400–499, Video 500–599, Rahmen 600–699 nach Größe — so stehen JMP-Teile im eigenen Kreativ-Tab je Abschnitt
(Presets, Rahmen, Motoren, Props, Stacks, Akkus, Video) geordnet.

## 11. Quellen und Begründung

- **Statorbezeichnung und Motorklassen**: Durchmesser × Höhe in mm (Industrienorm der FPV-Motoren); Massen, Ströme,
  KV-Stufen und Zellbereiche aus veröffentlichten Datenblättern gängiger Motoren je Baugröße (0702 bis 4214),
  Widerstände über die Kennzahl `KV·√R` an Propwashs Kalibrierung angeschlossen (1.3).
- **Propeller**: statische Schub- und Leistungsbeiwerte kleiner Propeller aus öffentlich zugänglichen
  Windkanal-Messreihen von Hochschulen (UIUC-Propellerdatenbank: ct 0,08–0,15, cp 0,03–0,09 im Stand; mehr Blätter
  → höheres cp; kleine Durchmesser → schlechtere Werte), angeschlossen an die eingebauten Props (1.4). Materialeigenschaften
  nach der Praxis: Polycarbonat biegt, Glasfaser-Nylon splittert, Carbon ist steif und bricht bei harten Einschlägen.
- **Akkus**: Energiedichten handelsüblicher FPV-LiPo (≈ 125–150 Wh/kg), LiHV-Whoop-Zellen, 18650/21700-Hochstromzellen
  der 3–4,5-Ah-Klasse mit 30–45 A (≈ 210–220 Wh/kg im Pack) und Semi-Solid-State-Packs (≈ 230–260 Wh/kg, 15–25 C);
  Innenwiderstände nach Messwerten der Klasse, C-Raten wie Etiketten (Propwash ist darauf kalibriert).
- **Stacks**: Dauer-/Spitzenströme und Spannungsbereiche gängiger Whoop-/Toothpick-AIOs (1S 5–6 A, 2S 12 A, 4S 20 A),
  20×20- und 30×30-4-in-1-ESCs (35–100 A, 4S/6S/8S); Brownout 2,9 V für 1S-fähige Boards (Propwash-Standard).
- **Videosysteme**: Latenz analog ≈ 10–12 ms, digitale Systeme 14–32 ms je nach Klasse; Sendeleistung 25 mW (Whoop)
  bis 1,6 W; horizontales FOV 120–140° (Datenblätter geben meist das diagonale Sichtfeld an, das ist ≈ 20° größer).
- **Rahmen**: Radstände, Massen und Armdicken gängiger Rahmen je Klasse (65/75/85-mm-Whoops, 2–3,5″-Toothpicks,
  2,5/3,5″-Cinewhoops, 5″ mit 4–6 mm Armen, 6–8″ Long-Range, X-Class mit 800+ mm Diagonale nach dem X-Class-Regelwerk);
  Geometrien so gewählt, dass benachbarte Props mindestens 5 mm Abstand haben.
- **Zielkorridore** (1.7): Erfahrungswerte der jeweiligen Klasse (Schwebegas, Flugzeiten, Topspeed), abgeglichen mit den
  Kennwerten, die Propwash für seine eingebauten Presets liefert.


## 12. Kalibrierung 1.0.4 (Energie und Flugzeiten)

Propwash 0.4.2 rechnet Flugzeiten, Ströme und Temperaturen mit einer Energiebilanz (Propwash
`docs/design/realism/calibration.md`, Flugmodell-Spezifikation Kap. 18); die Flugphysik ist unverändert. Die
Kalibrierung lief in zwei Durchgängen (erste Fassung und Nachkalibrierung nach dem Realismus-Review, beide als 1.0.4
gegen Propwash 0.4.2). In JMP geändert:

- **Videoleistung** `power_w`: `analog_whoop_25mw` 0,7 W (Nachkalibrierung, vorher 1,3 W), `analog_race_800` 5,0 W,
  `analog_lr_1600` 8,5 W, `digital_micro` 5,1 W (Nachkalibrierung, vorher 4,5 W), `digital_low_latency` 5,8 W,
  `digital_lr` 10 W, `digital_hd_pro` 9,0 W. Mit den 0,6 W Grundlast von Propwash liegen alle in den Spannen von
  `realism/power-video.md` §6.3 (analoger Whoop-Sender 1,3 W, digitales Micro-System 5,7 W); Digital- statt Analogvideo
  kostet im 65-mm-Whoop jetzt 30 % Schwebezeit wie im Emax-Vergleich HDZero gegen analog.
- **Chemie** `lihv_ss`: `usable_fraction` 0,88 (Li-Ion-artige Entladung). Die Leerspannung 3,5 V bleibt, weil sie die
  Spannungskurve im Flug bestimmt (Entscheidung des Besitzers).
- **Motor-Kühlung** `cooling_w_per_k_per_rad_s`: `m1404_3800` 0,00011, `m1505_2500` 0,00012, `m2004_1700` 0,00013,
  `m2507_1500` 0,00022, `m2807_1100` 0,00045, `m3115_900` 0,0006, `m4214_400` 0,0007 – Ziel: Glocke nach 60 s Vollgas im
  Stand wie die Herstellerprüfstände (1404 45–68 °C, 1408/1505 60–81 °C, 2806,5–2808 81–99 °C). Datenregel
  `MotorDataTest`: Fahrtwind-Kühlung bis 2,5 × Standard. **Richtigstellung:** Der Wert wirkt nicht nur auf die
  Temperaturanzeige, sondern auch auf die Thermik der Flugphysik (Leistungsgrenze, Drosselung ab `derate_start_c`,
  Verschleiß). In allen 22 Presets und den geprüften Varianten erreichen diese Motoren die Drosselung nicht, das
  Flugverhalten dort ist gleich; bei Dauervollgas drosseln sie später als vorher.
- **Shift-Hilfen** mit Zahlen nach der Analyse. KV-Vergleiche nennen „Flugzeit“ (gemischt). Nach der Nachkalibrierung
  angepasst: `cw_25_duct` „nur rund 10 min Schweben“, `lr_4_x` „rund 31 min Schweben mit 6S Li-Ion“, `b1s_450_hv` „über
  7 min Schweben im 75-mm-Whoop“, `b4s_1100` „über 14 min Schweben im 3,5″ auf 4S“, `b6s_750` „über 13 min Schweben im
  3,5″“, `b6s_850` „über 10 min Schweben im 3,5″-Cinewhoop“, `m0802_19000` „T/W 4, 7 min Schweben“, `m1404_3800` „nach
  1 min Vollgas 59 statt 89 °C“. Unverändert gültig: `m2507_1800` „hoher Strom (162 statt 112 A), heißer“,
  `m1505_2500` „im Cinewhoop warm (über 80 °C)“, `m2004_1700` „bleibt im Cinewhoop kühl (unter 70 °C)“.
- **Zielkorridore** der Prüfstände (Schweben/Reisen in min): Whoop 2,5–9 / 2,3–8, Toothpick 6–16 / 4–13, Cinewhoop
  4,5–13 / 3,5–11, 5″ 8–14,5 / 5,5–11, 6–7″ Freestyle 10–22 / 6–15, Long Range 12–48 / 8–33, X-Class 12–26 / 8–18;
  Einzelprüfungen `cinewhoop25_4s` Schweben 9,5–10,5 min, `lr4_6s_lion` 30–32 min.
- **Owner-Testfall** `share.OwnerFlightTimeTest` mit den Grenzen der Referenz (Propwash `realism/builds.md` §15.2):
  Schweben digital 3:00–3:20, Reisen 3:00–3:30, gemischt 2:00–2:30, aggressiv 1:30–2:00, Landespannung 3,3–3,5 V,
  Gewicht 25–30 g, Vollgasstrom 12–16 A, 23000 KV +15–25 %, 3-Blatt +10–20 % in allen Stilen und gemischt +15–30 s,
  2-Blatt Schweben/Reisen +10–20 % und gemischt/aggressiv +15–30 s, Digital −30–35 % gegen analog; Schub/Gewicht 4,1
  und Schwebegas 43,8 % bleiben als flugwirksame Werte unverändert.

### 12.1 Flugzeiten der Presets nach der Nachkalibrierung

| Preset | Schweben | Reisen | gemischt | aggressiv | Vollgas A | Motor °C 60 s / gemischt |
|---|---|---|---|---|---|---|
| `whoop75_1s` | 7:11 | 6:14 | 4:30 | 3:32 | 13,5 | 36 / 29 |
| `whoop85_2s` | 7:54 | 7:20 | 4:58 | 3:46 | 26,1 | 47 / 31 |
| `toothpick2_2s` | 8:39 | 6:56 | 5:08 | 3:36 | 25,4 | 38 / 29 |
| `toothpick3_4s` | 9:59 | 7:57 | 6:07 | 4:27 | 30,0 | 59 / 35 |
| `toothpick35_6s` | 13:52 | 12:03 | 8:50 | 6:13 | 33,1 | 82 / 41 |
| `cinewhoop25_4s` | 9:58 | 8:06 | 7:41 | 5:58 | 21,1 | 47 / 34 |
| `cinewhoop35_6s` | 10:40 | 8:47 | 8:24 | 6:37 | 18,3 | 67 / 44 |
| `bash5_6s` | 11:05 | 7:40 | 5:34 | 4:02 | 131,3 | 91 / 51 |
| `squashed5_6s` | 11:39 | 8:03 | 6:02 | 4:29 | 118,8 | 87 / 50 |
| `deadcat5_6s` | 11:04 | 7:55 | 5:51 | 4:18 | 107,3 | 84 / 50 |
| `race5_4s` | 11:32 | 8:22 | 2:57 | 2:14 | 133,0 | 75 / 53 |
| `race5_6s` | 12:53 | 9:56 | 3:15 | 2:28 | 161,6 | 95 / 60 |
| `hframe5_6s` | 11:19 | 7:36 | 5:39 | 4:09 | 132,1 | 91 / 51 |
| `lr4_6s_lion` | 31:01 | 25:39 | 17:50 | 16:42 | 23,7 | 73 / 45 |
| `lr6_6s` | 16:51 | 9:26 | 6:58 | 5:08 | 114,4 | 93 / 50 |
| `lr7_deadcat_lion` | 31:33 | 20:43 | 11:32 | 10:14 | 78,1 | 70 / 48 |
| `lr7_8s` | 19:30 | 11:50 | 8:02 | 5:45 | 141,0 | 95 / 41 |
| `lr8_6s_lion` | 40:36 | 27:35 | 13:36 | 11:44 | 108,9 | 93 / 46 |
| `xclass10_6s` | 21:32 | 12:55 | 6:35 | 4:30 | 210,9 | 91 / 50 |
| `xclass12_8s` | 18:27 | 14:14 | 6:00 | 4:43 | 116,7 | 74 / 59 |
| `xclass13_8s` | 17:04 | 12:31 | 5:11 | 3:46 | 153,2 | 86 / 63 |
| `xclass13h_8s` | 15:28 | 11:28 | 4:26 | 3:22 | 172,3 | 93 / 68 |

Flugphysik-Werte (Gewicht, Schub/Gewicht, Schwebegas, Topspeed) sind unverändert. Abweichungen flugwirksamer Daten
(Motor- und Videomassen, Wicklungswiderstände, `m4214_*`-Ströme) stehen als Entscheidung des Besitzers in Propwash
`realism/calibration.md` §2.4.

## 13. Datenkorrektur 1.0.5 (Freigabe des Besitzers, gegen Propwash 0.4.3)

Der zweite Realismus-Review meldete flugwirksame Werte einzelner Presets und Teile als unrealistisch. Der Besitzer hat am
30.09. freigegeben, die **Daten** dieser Teile zu korrigieren; die Flug-Engine von Propwash bleibt unverändert. Nur die
unten genannten Teile fliegen anders – alle übrigen Teile und Presets fliegen bitgleich wie mit 1.0.4 (Beweis mit
`sim.MotionFingerprintTest` über alle 27 Presets mit allen Einzeltausch-Varianten: 1004 Varianten ohne korrigiertes Teil
bitgleich, 240 mit korrigiertem Teil anders; Propwash `docs/design/realism/calibration.md` §0.6).

| Teil | Wert | 1.0.4 | 1.0.5 | Grund |
|---|---|---|---|---|
| `p40_17_3b_pc` | ct / cp | 0,0915 / 0,0733 | 0,1555 / 0,1625 | Schwebegas `whoop75_1s` 43,6 → 34,9 % (Referenz 31–33 % bei 23000 KV; mehr würde den 6-A-AIO begrenzen) |
| `p65_22_4b_bi` | ct / cp | 0,0937 / 0,0814 | 0,2061 / 0,2656 | Schwebegas `cinewhoop25_4s` 47,5 → 32,2 % (23–33 %); damit 82 °C nach 60 s Vollgas (über der F1404-Zeile), seit 1.0.0 0,1874 / 0,1961 (§13.2) |
| `p89_25_5b_pc_duct` | ct / cp | 0,1065 / 0,0762 | 0,2662 / 0,3012 | Schub `cinewhoop35_6s` 357 → 594 g je Motor (580–870 g), Schwebegas 47,8 → 29,1 % (≈ 28 %) |
| `p102_30_3b_gfn` | ct / cp | 0,0946 / 0,0525 | 0,1703 / 0,1268 | Schwebegas `lr4_6s_lion` 51 → 37 %, Schub/Gewicht 3,6 → 4,5 |
| `cw_25_duct` | Stirnfläche / Draufsicht | 0,014 / (2,5 × =) 0,035 m² | 0,009 / 0,015 m² | Topspeed `cinewhoop25_4s` 55 → 85 km/h (80–100) |
| `cw_35_duct` | Stirnfläche / Draufsicht | 0,020 / 0,050 m² | 0,0137 / 0,0228 m² | dieselbe Regel wie `cw_25_duct` und `propwash:cinewhoop_3` (Draufsicht 1,67 × Stirnfläche, Fläche ∝ Radstand^1,21); Topspeed `cinewhoop35_6s` 60 → 89 km/h – die Quellen mit 100–150 km/h fliegen 2030–2450 KV, mit 1700 KV drehzahlskaliert 92–104 km/h (Propwash `realism/calibration.md` §0.7) |
| `m2004_1700` | Wicklungswiderstand / Fahrtwind-Kühlung | 0,3114 Ω / 0,00013 | 0,221 Ω / 0,0002 | F2004 1700 KV 221 mΩ; mit dem schwereren Prop 60-s-Wert 91 statt 118 °C (Datenregel bis 2,5 × Standard); seit 1.0.0 Kühlung 0,000236, 82 °C (§13.2) |
| `m1103_11000` | Fahrtwind-Kühlung | 0,000055 | 0,000022 | `toothpick2_2s` nach 60 s Vollgas 38 → 53 °C (1103–1404: 50–70 °C) |
| `p76_30_2b_pc` | Masse | 1,2 g | 0,8 g | Gemfan 3018 0,75–0,84 g |
| `m2807_1500` | Masse | 47,5 g | 53 g | 2807-Stator ≈ 53 g |

Props bekommen `ct · a` und `cp · a^1,5`: Die Gütezahl bleibt gleich (die Referenz bestätigt die Gütezahlen der Daten,
nur die absoluten Beiwerte waren um den Faktor 2–4 zu niedrig), der Motor läuft an der richtigen Stelle seiner Kennlinie.

**Geschwister ohne Korrektur (Stand 1.0.5, seit 1.0.0 korrigiert: §13.2).** Die übrigen Props derselben Größe (`p40_20_2b_pc`, `p40_15_4b_pc`, `p40_14_5b_soft`,
`p65_25_3b_pc`, `p89_30_3b_pc`, `p102_25_2b_pc`) waren nicht gemeldet und fliegen unverändert. Ihre Shift-Hilfen sagen jetzt
nur, was im Spiel stimmt: `p40_15_4b_pc` „+ mehr Schub als der Zweiblatt“, „− mehr Strom als der Zweiblatt“,
`p40_14_5b_soft` „+ mehr Schub als 2 und 4 Blätter“, „− mehr Strom als 2/4 Bl., auf 0802 träge“, `p65_25_3b_pc` „+ leichter,
längere Flugzeit als 4 Bl.“, „− im Duct weniger Schub als 4 Blätter“, `p102_25_2b_pc` „+ weniger als halb so viel Strom,
kühl“. Empfehlung an den Besitzer: dieselbe Korrektur für die ganzen Familien (Propwash `realism/calibration.md` §2.5).

**Weitere Shift-Hilfen 1.0.5:** `p65_22_4b_bi` „+ im Duct mehr Schub, schwebt länger“, neu „− überlastet 1202,5er (Hitze,
Einbruch)“; `p76_30_2b_pc` 0,80 g; `cw_25_duct` „− Ducts bremsen: rund 85 km/h“; `cw_35_duct` „− Ducts bremsen: rund
90 km/h“, „− schwere Cam kostet 30 % Schwebezeit“ (vorher „fast 60 % Schwebegas“); `lr_4_x` „+ über 32 min Schweben mit
6S Li-Ion“, „− wenig Reserve bei Wind (T/W um 4,5)“; `m1102_18000` „+ reagiert schneller (34 statt 46 ms)“; `m1404_3800`
„+ nach 1 min Vollgas 59 statt 88 °C“; `m1505_2500` „+ im Duct etwas mehr Schub als 2004“, „− im Cinewhoop zu heiß
(drosselt)“; `m2004_1700` „+ im Duct ohne Drosselung (91 °C nach 1 min)“, „+ im Duct ein Drittel weniger Strom als 1505“;
`m2807_1500` 53 g, „+ kräftig mit 7″-3-Blatt (T/W über 12)“, „− mit 3-Blatt warm (über 110 °C)“.

**Prüfstände:** Cinewhoop-Korridore nach `realism/builds.md` §0 (Schub/Gewicht 4–7, Schwebegas 23–35 %, Topspeed
80–150 km/h), Referenzaufbau „Toothpick 2,5″ 3S 650“ mit `p65_25_3b_pc` (der korrigierte Vierblatt überlastet den
1202,5er), Blattzahl-Vergleich der 40-mm-Props gegen den Zweiblatt; neuer Property-Test `bench.FlightTimeOrderTest`
(1177 Einzeltausch-Varianten, aggressiv ≤ gemischt ≤ Reisen ≤ Schweben).

**Propwash 0.4.3** ändert außerdem nur Energie- und Anzeigewerte: garantierte Reihenfolge der Flugzeiten, Vollgasstrom
kleiner Props nach den Prüfständen (`whoop85_2s` 26,1 → 31,8 A, `toothpick2_2s` 25,4 → 33,0 A, `toothpick3_4s`
30,0 → 40,8 A, `toothpick35_6s` 33,1 → 48,3 A), Profile 75–85-mm-Whoop (Schubstoß 3 : 1) und Cinewhoop (gemischt
83/12/5), Li-Ion-Akkus im gemischten Flug mit höchstens 6 C im Schubstoß (aggressiv bis zum Dauerstrom des Packs;
`lr4_6s_lion` gemischt 11:52 → 16:56, `lr7_deadcat_lion` 11:32 → 15:56, `lr8_6s_lion` 13:36 → 18:18 min, gemischt/Schweben
1,9–2,2 wie die Referenz ≈ 2).

**Nachtrag nach dem dritten Realismus-Review (1.0.5 neu erstellt):** Die erste Fassung von 1.0.5 hatte für `cw_35_duct`
0,018 / 0,013 m² – die Draufsicht kleiner als die eigene Stirnfläche und kleiner als beim kleineren `cw_25_duct`. Jetzt
gilt für alle drei Cinewhoop-Rahmen dieselbe Regel (Tabelle oben); der 3,5″-Cinewhoop fliegt 89 statt 103 km/h, Reisen
9:47, gemischt 8:31, aggressiv 6:27 min, alles andere gleich. `lr4_6s_lion` wird in Propwash wieder gegen die
Long-Range-Fenster geprüft (die Toothpick-Fenster der ersten Fassung sind zurückgenommen). Gegen die erste Fassung fliegen
nur die 34 Einzeltausch-Varianten mit `cw_35_duct` anders; gegen 1.0.4 bleiben 1004 Varianten ohne korrigiertes Teil
bitgleich. Shift-Hilfe `cw_35_duct` „− Ducts bremsen: rund 90 km/h“, Prüfstand 85–95 km/h.

### 13.1 Presets mit 1.0.5 und Propwash 0.4.3

| Preset | Schweben | Reisen | gemischt | aggressiv | Vollgas A | S/G | Schwebegas % | Motor °C 60 s / gemischt | Topspeed km/h |
|---|---|---|---|---|---|---|---|---|---|
| `whoop75_1s` | 7:04 | 5:58 | 4:24 | 3:29 | 21,3 | 4,77 | 34,9 | 49 / 32 | 41 |
| `whoop85_2s` | 7:54 | 7:20 | 5:26 | 4:19 | 31,8 | 6,76 | 27,8 | 47 / 31 | 57 |
| `toothpick2_2s` | 8:39 | 6:56 | 4:57 | 3:20 | 33,0 | 6,28 | 30,3 | 53 / 34 | 74 |
| `toothpick3_4s` | 9:59 | 7:57 | 5:54 | 4:07 | 40,8 | 6,61 | 33,0 | 59 / 35 | 91 |
| `toothpick35_6s` | 13:52 | 12:02 | 8:31 | 5:45 | 48,3 | 7,62 | 28,0 | 81 / 41 | 109 |
| `cinewhoop25_4s` | 10:12 | 8:48 | 7:31 | 5:30 | 56,1 | 5,80 | 32,2 | 82 / 38 | 85 |
| `cinewhoop35_6s` | 11:25 | 9:47 | 8:31 | 6:27 | 63,0 | 6,76 | 29,1 | 91 / 41 | 89 |
| `bash5_6s` | 11:05 | 7:40 | 5:34 | 4:02 | 131,3 | 11,48 | 21,6 | 91 / 51 | 140 |
| `squashed5_6s` | 11:39 | 8:03 | 6:02 | 4:29 | 118,8 | 11,89 | 22,0 | 87 / 50 | 139 |
| `deadcat5_6s` | 11:04 | 7:55 | 5:51 | 4:18 | 107,3 | 10,83 | 24,0 | 84 / 50 | 132 |
| `race5_4s` | 11:32 | 8:22 | 2:57 | 2:14 | 133,0 | 11,08 | 21,9 | 75 / 53 | 155 |
| `race5_6s` | 12:53 | 9:56 | 3:15 | 2:28 | 161,6 | 13,87 | 17,6 | 95 / 60 | 187 |
| `hframe5_6s` | 11:19 | 7:36 | 5:39 | 4:09 | 132,1 | 11,89 | 21,1 | 91 / 51 | 127 |
| `lr4_6s_lion` | 32:35 | 23:01 | 16:56 | 10:30 | 47,0 | 4,54 | 37,0 | 74 / 41 | 96 |
| `lr6_6s` | 16:51 | 9:26 | 6:58 | 5:08 | 114,4 | 10,13 | 25,5 | 93 / 50 | 127 |
| `lr7_deadcat_lion` | 31:33 | 20:43 | 15:56 | 10:14 | 78,1 | 5,17 | 27,7 | 70 / 45 | 104 |
| `lr7_8s` | 19:30 | 11:50 | 8:02 | 5:45 | 141,0 | 11,66 | 21,7 | 95 / 41 | 145 |
| `lr8_6s_lion` | 40:36 | 27:35 | 18:18 | 11:44 | 108,9 | 4,91 | 32,9 | 93 / 45 | 110 |
| `xclass10_6s` | 21:32 | 12:55 | 6:35 | 4:30 | 210,9 | 8,51 | 25,7 | 91 / 50 | 124 |
| `xclass12_8s` | 18:27 | 14:14 | 6:00 | 4:43 | 116,7 | 5,47 | 39,2 | 74 / 59 | 113 |
| `xclass13_8s` | 17:04 | 12:31 | 5:11 | 3:46 | 153,2 | 5,92 | 35,7 | 86 / 63 | 117 |
| `xclass13h_8s` | 15:28 | 11:28 | 4:26 | 3:22 | 172,3 | 5,69 | 35,4 | 93 / 68 | 107 |

Unverändert gegenüber §12.1: alle 5″-Presets, `lr6_6s`, `lr7_8s` und die X-Class-Presets. Anders: `whoop75_1s`,
`toothpick2_2s`, `cinewhoop25_4s`, `cinewhoop35_6s`, `lr4_6s_lion` (Datenkorrektur, fliegen anders), in Strom, gemischt und
aggressiv `whoop85_2s`, `toothpick3_4s`, `toothpick35_6s` (Vollgasstrom 0.4.3) und gemischt `lr7_deadcat_lion`,
`lr8_6s_lion` (Li-Ion-Regel 0.4.3); diese fünf fliegen unverändert.

### 13.2 Restpunkte des vierten Realismus-Reviews (öffentliche Version 1.0.0)

Öffentlich erscheinen die internen Stände 1.0.0–1.0.5 als **Just More Parts 1.0.0** (gegen Propwash 0.4.0,
`docs/RELEASING.md`). Vor dem Release hat der vierte Review drei Dinge gefunden, die hier behoben sind; Propwash
`docs/design/realism/calibration.md` §0.8 hat die ganze Rechnung.

1. **60-s-Temperaturen der korrigierten Cinewhoops.** Die Datenkorrektur 1.0.5 hob `cinewhoop25_4s` von 47 auf 82 °C
   (Ursache: `p65_22_4b_bi`, in 1.0.5 nirgends als Abweichung genannt) und `cinewhoop35_6s` von 67 auf 91 °C – beide über
   ihren eigenen Prüfstandszeilen (F1404: ΔT 25–44 K; F2004: ≈ 78–89 °C bei 25 °C). Jetzt 68 °C (ΔT 43 K, 177 W je Motor)
   und 82 °C (ΔT 57 K, 331 W). `lr4_6s_lion` (gleicher Motor) 74 → 68 °C.
2. **Blattzahl-Reihenfolge.** In 1.0.5 war je Familie nur ein Prop korrigiert; im 75-mm-Whoop hatte der Dreiblatt mehr
   Schub als Vier- und Fünfblatt, im 2,5″-Cinewhoop brachte der Vierblatt 5,8 statt 3,8 S/G und schwebte länger, der
   4″-Zweiblatt zog weniger als halb so viel Strom. Jetzt gilt in den Aufbauten der Familie: mehr Blätter, mehr Schub und
   Strom, kürzere Flugzeit (Tabelle in `calibration.md` §0.8).
3. **Dokumentation.** Richtigstellungen in `calibration.md` §0.4 (F2004 statt F1408/5″ als Maßstab), §0.5 (warum c_t des
   2,6″-Vierblatts unter der Duct-Referenz bleibt), §0.6 (Brownout bei Dauervollgas), §2.4/§2.5 (102 °C, offene Punkte).

| Teil | Wert | 1.0.5 | 1.0.0 (öffentlich) | Grund |
|---|---|---|---|---|
| `m2004_1700` | Fahrtwind-Kühlung | 0,0002 | 0,000236 | Obergrenze der Datenregel (2,5 × Standard) |
| `p65_22_4b_bi` | ct / cp | 0,2061 / 0,2656 | 0,1874 / 0,1961 | Gütezahl 0,28 → 0,33 (oberer Rand 2″-Vierblatt, in der Duct-Referenz); `m1404_3800` bleibt (steckt auch in `toothpick3_4s`) |
| `p65_25_3b_pc` | ct / cp | 0,0877 / 0,0774 | 0,114 / 0,0877 | Geschwister; nur ct · 1,3, weil die 2,5″-Toothpick-Motoren mehr Last nicht vertragen; Gütezahl 0,35 (über dem Vierblatt) |
| `p40_20_2b_pc` | ct / cp | 0,0804 / 0,0601 | 0,1367 / 0,1332 | Geschwister von `p40_17_3b_pc`, gleicher Faktor a = 1,7 |
| `p40_15_4b_pc` | ct / cp | 0,0984 / 0,0816 | 0,1673 / 0,1809 | wie oben |
| `p40_14_5b_soft` | ct / cp | 0,1051 / 0,0882 | 0,1787 / 0,1955 | wie oben |
| `p102_25_2b_pc` | ct / cp | 0,0763 / 0,0305 | 0,13 / 0,0763 | Geschwister von `p102_30_3b_gfn`; Referenz Gemfan 4024-2 (Gütezahl 0,49 statt 0,55) |

`p89_30_3b_pc` blieb in diesem Durchgang unverändert: Er ist das Prop des Presets `toothpick35_6s`, das damals bitgleich
bleiben musste, und mit dem Faktor des Duct-Fünfblatts (a = 2,5) würde das Preset den 20-A-AIO begrenzen und überhitzen.
Seit dem Nachtrag vom 02.10. ist er mit c_t · 1,3 korrigiert (§13.3).

**Nur diese Teile fliegen anders.** `tools/calibration.sh` in Propwash (`sim.MotionFingerprintTest`, 1245 Varianten):
0 Varianten ohne geändertes Teil anders, 120 mit geändertem Teil anders. Presets: `cinewhoop25_4s` (Prop),
`cinewhoop35_6s` und `lr4_6s_lion` (nur Motorwärme); die übrigen 19 JMP-Presets und alle eingebauten Presets bitgleich.

| Preset | Schweben | Reisen | gemischt | aggressiv | Vollgas A | S/G | Schwebegas % | Motor °C 60 s / gemischt | Topspeed km/h |
|---|---|---|---|---|---|---|---|---|---|
| `cinewhoop25_4s` | 11:15 | 9:50 | 8:31 | 6:25 | 48,1 | 6,0 | 33,1 | 68 / 36 | 87 |
| `cinewhoop35_6s` | 11:25 | 9:47 | 8:31 | 6:27 | 63,0 | 6,8 | 29,1 | 82 / 39 | 89 |
| `lr4_6s_lion` | 32:35 | 23:01 | 16:56 | 10:30 | 47,0 | 4,5 | 37,0 | 68 / 39 | 96 |

Alle anderen Presets wie §13.1.

**Shift-Hilfen 1.0.0:** `p40_17_3b_pc` „+ länger in der Luft als 4 und 5 Blätter“ (vorher „flink auch auf 0802“),
`p40_15_4b_pc` „− auf 1S kaum mehr Schub als 3 Blätter“ (vorher „auf 0802-Motoren träge“), `p40_14_5b_soft` „− viel
Strom: am 1S-AIO strombegrenzt“, `p65_22_4b_bi` „+ im Duct ein Viertel mehr Schub als 3 Bl.“ (vorher „schwebt länger“),
`p102_25_2b_pc` „+ länger in der Luft als 3 Blätter“, „+ ein Viertel weniger Strom, kühler“, `m2004_1700` „+ im Duct
ohne Drosselung (82 °C nach 1 min)“, `cw_25_duct` „− nur rund 11 min Schweben“, `tp_25_x` „+ leicht und schnell (rund
100 km/h auf 4S)“. Englisch entsprechend.

**Prüfstände:** Toothpick-Korridore nach `realism/builds.md` §0 erweitert (S/G bis 9, Schwebegas ab 20 %; der 2,5″-
Toothpick 4S 450 fliegt mit dem korrigierten Dreiblatt 8,7 : 1 bei 21 % Schwebegas), `tp_25_x` 95–105 km/h,
`cw_25_duct` 10,5–11,5 min Schweben. Referenzaufbau „Whoop 75 2S 300“ (Akku-Referenz, Selbsttest): `BATTERY_OVERLOAD` und
`HEAVY_SAG` zugelassen – der 45C-Akku liefert 13,5 A, der Aufbau zieht mit den 40-mm-Props 23 A. Motor-Referenz
`m0802_15500` auf 2S 450 mAh mit Digitalvideo (75 °C nach 1 min Vollgas, über der Whoop-Grenze 60 °C, deshalb ohne
Temperaturprüfung).

### 13.3 Nachtrag vom 02.10.: 89-mm-Dreiblatt, Motorwärme und offene Punkte (in 1.0.0)

Die offenen Punkte aus Propwash `docs/design/realism/calibration.md` §0.8 („89 mm“) und §2.5 sowie aus dem Leistungsbericht
sind mit der Freigabe des Besitzers (Daten einzelner unrealistischer Teile korrigieren, Flug-Engine unverändert) behoben.
Geändert sind nur diese drei Teile:

| Teil | Wert | vorher | nachher | Grund |
|---|---|---|---|---|
| `p89_30_3b_pc` | ct / cp | 0,0927 / 0,0658 (Gütezahl 0,34) | 0,1205 / 0,0845 (Gütezahl 0,395) | Geschwister des Duct-Fünfblatts, korrigiert wie `p65_25_3b_pc`: c_t · 1,3 und eine Gütezahl etwas über der des Fünfblatts (0,364). Vorher flog der offene Dreiblatt im 3,5″-Cinewhoop kürzer als der Fünfblatt (Blattzahl-Reihenfolge verletzt) |
| `m1505_2500` | Fahrtwind-Kühlung | 0,00012 | 0,00016 (2 × Standard) | mit dem korrigierten Prop käme `toothpick35_6s` nach 60 s Vollgas auf 93 °C (ΔT 68 K bei ≈ 300 W je Motor; nächste Prüfstandszeile F1408: 145–386 W, ΔT 41–62 K); jetzt 78 °C (ΔT 53 K, vorher 81 °C) |
| `m12025_5500` | Fahrtwind-Kühlung | Standard (4,1·10⁻⁵) | 0,00008 (2 × Standard) | Referenz „Toothpick 2,5″ 4S 450“ mit `p65_25_3b_pc` nach 60 s 121 °C (ΔT 96 K bei ≈ 155 W je Motor; F1203: 57–136 W, ΔT 31–46 K); jetzt 79 °C, auf 3S 450 61 °C, 3S 650 66 °C |

**Warum nicht mehr beim 89-mm-Dreiblatt?** Die Referenz (HQ 3520-3, P/D 0,57: c_t 0,17–0,18, Gütezahl 0,45–0,49; auf P/D
0,86 umgerechnet c_t ≈ 0,21) läge bei a ≈ 2,3. Mit gleicher Gütezahl überhitzt schon ab a = 1,4 der 1505/3600 auf 4S 1100
(`motor_thermal`), ab a ≈ 1,8 begrenzt der 20-A-AIO das Preset (`esc_current_limited`, `motor_thermal`), und mit der nötigen
Gütezahl (≈ 0,395) verlässt `toothpick35_6s` ab a ≈ 1,4 den Toothpick-Korridor (S/G über 9). Die Gütezahl ist zweimal begrenzt: unter 0,393 schwebt der
Dreiblatt im 3,5″-Cinewhoop kürzer als der Fünfblatt, über 0,396 verlässt `toothpick35_6s` im gemischten Flug das
Toothpick-Fenster der Propwash-Kalibrierung (0,38 W/g = 9:27 min). Gewählt: a = 1,3 wie beim 65-mm-Dreiblatt und die
Gütezahl 0,395 – beide Grenzen knapp eingehalten (Schweben im Duct +0,4 %, gemischt 9:26).

**Preset `toothpick35_6s`** (das einzige JMP-Preset, das anders fliegt; Propwash 0.4.0, Harness bei 25 °C):

| Preset | Masse g | S/G | Schwebegas % | Schweben | Reisen | gemischt | aggressiv | Vollgas A | Topspeed km/h | Motor °C 60 s / gemischt | Warnungen |
|---|---|---|---|---|---|---|---|---|---|---|---|
| `toothpick35_6s` vorher | 222,5 | 7,62 | 28,0 | 13:52 | 12:02 | 8:31 | 5:45 | 48,3 | 109 | 81 / 41 | keine |
| `toothpick35_6s` nachher | 222,5 | 8,88 | 24,1 | 15:05 | 12:54 | 9:26 | 6:34 | 55,2 | 115 | 78 / 38 | keine |

Topspeed 115 km/h liegt in der Referenz für 3,5″-Toothpicks (`builds.md` §0: 100–130 km/h). Die JMP-Prüfstände führen dafür
einen eigenen Korridor „Toothpick 3,5″“ mit 100–130 km/h (`FrameBenchTest`, `PropBenchTest`, `MotorBenchTest`,
`PowerReferences`); 2–3″-Toothpicks bleiben bei 70–110 km/h.

**Blattzahl-Reihenfolge der 89-mm-Familie** im 3,5″-Cinewhoop (`cw_35_duct`, `aio_20_6s_45a`, `m2004_1700`, `digital_hd_pro`,
`b6s_850`; Schweben / Reisen / gemischt / aggressiv, S/G, Vollgasstrom):

| Prop | Flugzeiten | S/G | Vollgas |
|---|---|---|---|
| 3 Blätter vorher | 10:26 / 9:44 / 8:13 / 6:27 | 3,8 | 24,8 A |
| 3 Blätter nachher | 11:28 / 10:36 / 9:09 / 6:48 | 4,7 | 29,4 A |
| 5 Blätter (unverändert) | 11:25 / 9:47 / 8:31 / 6:27 | 6,8 | 63,0 A |

Mehr Blätter geben jetzt mehr Schub (+43 %) und doppelt so viel Strom und fliegen in allen vier Stilen kürzer. Im
3,5″-Toothpick (20-A-AIO) ist der Fünfblatt strombegrenzt und überhitzt; dort ist er kein Vergleich (`PropBenchTest`
prüft die Blattzahl-Reihenfolge der 89-mm-Familie deshalb nur im Cinewhoop).

**Shift-Hilfen** (de/en): `p89_30_3b_pc` „+ im 3,5″ auf 6S: T/W fast 9, 115 km/h“, „+ im Duct halber Strom, länger in der
Luft“, „− im Duct 30 % weniger Schub als 5 Blätter“; `p89_25_5b_pc_duct` „+ im Duct gut 40 % mehr Schub als 3 Bl.“ (vorher
„… schwebt länger“), „− doppelt so viel Strom wie 3 Bl., wärmer“, neu „− kürzer in der Luft als der Dreiblatt“;
`m12025_5500` neu „+ auf 2,5″ 4S unter 80 °C nach 1 min“, „− mit 3″-Props überfordert: über 100 °C“ (vorher „wird heiß“ mit
`motor_thermal`, jetzt 110 °C mit `heavy_sag`); `m1505_3600` „T/W über 9“ (vorher „um 8“); `aio_20_4s_35a` „ESC-Last unter
50 % im 3,5″“ (48 %); `b6s_750` „über 15 min Schweben im 3,5″“.

**Ehrliche Empfehlungen für 2S im 75-mm-Whoop:** Der `b2s_300_hv` (45C, 13,5 A) ist mit jedem 40-mm-Prop überlastet
(`battery_overload`, `heavy_sag`; 23 A bei Vollgas), es gibt keinen warnungsfreien Aufbau mit ihm. Seine Hilfe sagt das jetzt
(„− nur 13,5 A: jeder 2S-Whoop überlastet ihn“, „Einsatz: Whoop 75 auf 2S, ruhig geflogen“, „Für Vollgas: 2S 450 HV (75C)“);
`whoop_75_duct` empfiehlt „1S 380–550 HV oder 2S 450 HV“, `m0802_15500` „2S 450 mAh HV“, `b2s_450_hv` „Whoop 75/85“ – mit
dem 450er fliegt jeder 40-mm-Prop im Whoop 75 ohne Warnung (`PowerClaimsTest`).

**Entschieden, nicht geändert:** `tp_2_x` trägt weiter höchstens 30 g Akku und `tp_25_x` bevorzugt 3S. Der 2S 650 (35,6 g)
passt damit nicht in den 2″ und meldet im 2,5″ den Hinweis „Akku ist für diesen Rahmen schwach“ (`battery_undersized`). Das
ist gewollt: 2″-Toothpicks fliegen 2S 300–450 (`builds.md` §4, Trockengewicht unter 60 g); ein 36-g-Akku machte den 2″ zu
einem 2,5″. Im 2,5″ ist 2S die schwächere Wahl (S/G 6,6 statt 8,8 mit 3S), genau das sagt der Hinweis, und die Hilfe des
`b2s_650` nennt ihn („Der 2,5″-Rahmen bevorzugt 3S (Hinweis)“).

**Prüfstände aufgeräumt:** `FrameBenchTest` hat keine Liste „offen“ mehr (der letzte Eintrag `lr8_6s_lion` war erledigt),
`PowerReferences` erlaubt beim „Long Range 8″ 6S2P 8400 Li-Ion“ kein `motor_thermal` mehr (96 °C), `PropBenchTest` braucht die
Schwebegas-Ausnahme des `p102_25_2b_pc` nicht mehr (37,6 %); die des `p254_45_3b_cfn` bleibt (24,1 % im X-Class 10″: der
Dreiblatt hat mehr Schub als der Zweiblatt des Presets, Korridor ab 25 %).

**Beweis** (Propwash `tools/calibration.sh` mit diesem Stand, vorher = `main` von 1.0.0): 1245 Varianten (27 Presets mit
allen Einzeltausch-Varianten), **55 anders, alle mit einem der drei Teile** (`p89_30_3b_pc` 42, `m1505_2500` 35,
`m12025_5500` 7; Varianten mit zwei der Teile zählen doppelt), **0 Varianten ohne geändertes Teil anders**, jede Variante mit
einem der Teile anders. Von den Presets fliegt nur `toothpick35_6s` anders; Owner-Fall und alle Propwash-Goldens bitgleich,
Harness 9/9 Tests grün (alle Presets in ihren Klassenfenstern), JMP `FlightTimeOrderTest` grün.

### 13.4 Nachtrag 1.1.0 (08.10.): Steigflug-Schub von Propwash 0.5.0

Propwash 0.5.0 rechnet den Schub im Steigflug nicht mehr linear (`1 − λ`), sondern mit Impuls- und
Blattelementtheorie; im Vorwärtsflug blendet der Effekt mit der induzierten Geschwindigkeit nach Glauert aus (Propwash
`docs/design/realism/flight-feel-gravity.md` §11). Schweben und Fall bleiben gleich, Reise und gemischt werden um
0,5–3 % länger, Topspeeds um 0–3 km/h höher.

| Prüfung | vorher | mit dem neuen Steigflug-Schub | mit c_p × 1,012 für beide 89-mm-Props |
|---|---|---|---|
| `toothpick35_6s` gemischt (Fenster bis 9:27) | 9:26 | 9:29 | 9:24 |
| `toothpick35_6s` Reise (Korridor bis 13,0 min) | 12,9 | 13,0 (darüber) | 12,9 |
| 3,5″-Cinewhoop: Dreiblatt schwebt länger als Fünfblatt (`FlightTimeOrderTest`) | ja | ja | ja |
| `cinewhoop35_6s` Schweben / gemischt | 11:25 / 8:31 | 11:25 / 8:40 | 11:21 / 8:35 |

- **Warum c_p und nicht c_t:** Die Gütezahl war schon in §13.3 die Stellgröße zwischen den beiden Grenzen; c_t würde
  Schub/Gewicht und die Aussage „T/W fast 9, 115 km/h“ verschieben.
- **Warum beide Props:** Mit c_p nur am Dreiblatt schwebte er im 3,5″-Cinewhoop kürzer als der Fünfblatt
  (Blattzahl-Reihenfolge). Derselbe Faktor an beiden lässt den Abstand gleich.
- **Aussagen:** `m0702_27000` „mehr Topspeed (57 statt 51 km/h)“ statt 56/50 (Prüfstand 65 mm 1S 380 jetzt 57,2 und
  51,1 km/h); `m2004_1700` „im Duct ohne Drosselung (83 °C nach 1 min)“ statt 82 °C (82,7 °C mit c_p × 1,012 am
  Fünfblatt, weiter ohne Drosselung).
