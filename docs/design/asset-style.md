# Asset-Stil — JMP-Teile im Propwash-Stil (Stil-API)

Stand: JMP 1.0.1, Branch `fix/style-system`. Verbindlich für alle, die Modelle, Texturen oder Item-Symbole von JMP-Teilen
erzeugen. Ziel: Jedes JMP-Teil sieht aus, als käme es vom selben Zeichner wie die Teile von Propwash — gleiche
Geometrie-Sprache, gleiche Texeldichte und Texturgröße, gleiche Palette, Schattierung und Kanten — und zeigt trotzdem seine
echte Größe und seinen Charakter. Jedes Item benutzt in allen Ansichten dieselbe Logik für Anzeige-Transformationen wie
die Propwash-Items.

## 1. Anlass: Befund zum Feedback auf 1.0.0

Nachgestellt mit `tools/assets/compare_render.py` (Bilder in `docs/asset-sheets/style_compare_<kategorie>.png`):

| Befund | Ursache in 1.0.0 | Regel ab 1.0.1 |
|---|---|---|
| Motoren „krass anders“ | Eigener Generator (`tools/assets/motor/motorgen.py`): runde Glocke aus vielen Segmenten, 32er-Textur, breite schwarze Lüftungsschlitze, kurze Welle. **Achtung:** Im Feedback-Bild `motors_base_left_jmp_right.png` ist der runde rote Motor links der JMP-Motor `m2207_1950` und der kantige Motor rechts der Propwash-Motor `m2207_1750` (Hotbar-Plätze 1 und 2, im Vergleichsbild eindeutig). Propwash baut Glocken mit `kit.cylinder` als **Achteck**: dunkler Deckel mit kupferfarbenen Fenstern und silberner Nabe, farbiger Glockenmantel mit Kupfer-Schlitzen, sichtbarer Stator-Ring, Fuß mit vier Laschen, M5-Gewindewelle ab 22 mm Stator. | Motoren nur noch aus `jmpstyle.motor` (baut mit `motors.build_motor` der Basis). |
| Propeller „ein wenig“ anders | Eigener Generator (`tools/props/prop_assets.py`): andere Naben, Blattformen und Maler. | Props nur aus `jmpstyle.prop` (Blätter mit `props.blade_pieces`/`emit_piece`, Nabe und Mutter wie die Basis). |
| 6S-Akku „passt kaum in die Hand“ | Alle Items benutzen den Sonder-Renderer `propwash:part` mit `propwash:item/part_base`: das Teil wird auf einen Block eingepasst und dann mit den festen `part_base`-Werten gezeigt (erste Person Skala 0,56 auf den ganzen Block). Propwash-Akkus haben eigene Item-Modelle mit `kit.compute_display` (erste Person 1,6 px Zielgröße, Skala ≈ 0,21). Ebenso zu klein im Rahmen und am Boden. | Jedes Teil bekommt sein eigenes Item-Modell mit `display` aus `kit.compute_display` (§3). |
| Akkus, Stacks, Rahmen, Video „ähnlich, aber nicht gleich“ | Zwei weitere eigene Generatoren (`power_assets.py`, `jmp_models.py`): eigene Maler, Beschriftung auf der Stirnseite, farbige Platinen, Kameras ohne Blende. | Alle Kategorien aus `jmpstyle` (§4). |

## 2. Aufbau

| Pfad | Inhalt |
|---|---|
| `tools/assets/propwash_style/` | **Unveränderte** Kopie von Propwash 0.4.0 `tools/assets/parts` (README mit Quelle und Stand, `SHA256SUMS`; jede Abweichung meldet die Stil-Prüfung). |
| `tools/assets/jmpstyle/` | JMP-Stil-API auf dieser Kopie: `motor`, `prop`, `battery`, `stack`, `frame`, `video` (Vorlagen je Kategorie), `display` (Anzeige-Transformationen, Symbolmodelle für übergroße Teile), `palette` (Farbregeln), `writer` (Ausgabe wie der Addon-Generator der Basis, Lack-Varianten, `drone_paint.json`/`drone_layout.json` zusammenführen), `templates` (Teil-JSON → Vorlage), `check` (Stil-Prüfung), `itemview` (Nachbildung der Vanilla-Item-Kontexte für Vergleiche). |
| `tools/assets/style_generate.py` | Erzeugt Modelle, Texturen, Render-Definitionen, Lack-Varianten und Item-Symbole aus `data/justmoreparts/propwash_parts/**`. |
| `tools/assets/compare_render.py` | Vergleichsbilder Propwash gegen JMP je Kategorie (§6). |

Die Stil-API ruft nur die Vorlagen und Bausteine der Basis auf. Eigene Maler gibt es nur dort, wo die Basis einen festen
Wert eingebaut hat, der für JMP ein Parameter sein muss (Beschriftung des Li-Ion-Bands, Platinen-Beschriftung in
Akzentfarbe, Carbon-Blätter, Kondensatorhülle) — sie sind Kopien des Basis-Malers mit einem zusätzlichen Parameter.

## 3. Regeln für alle Kategorien

### 3.1 Bausteine, Maßstab, Textur

- Maßstab und Anbaupunkte wie Propwash `docs/ASSETS.md`: 1 Modell-Einheit = 8 mm, Anbaupunkt bei (8, 8, 8), oben +Y,
  Nase −Z. Jedes Drohnen-Modell zeigt die **echte Größe** des Teils.
- Nur `kit.Model` (Textur 64 × 64, `texture_size`, `gui_light: side`) mit `box`/`cbox`/`bar`/`cylinder`/`cylinder_x`/`ring`.
  Runde Teile sind **Achtecke** aus `kit.cylinder` (auch Motorglocken, Naben, Akkuzellen, Kondensatoren), Ringe aus
  `kit.ring` (16–24 Segmente). Keine feineren Rundungen, keine eigenen Segmentzahlen.
- Texeldichte je Kategorie wie die Basis (Texel je 8 mm; der Packer der Basis verringert in 10-%-Schritten, bis alles in
  64 × 64 passt). Nie andere Texturgrößen.

| Kategorie | Dichte |
|---|---|
| Motor | 8 (Glocke < 12 mm), 5 (< 20 mm), 4, X-Class-Glocken ab 40 mm 3 |
| Prop | 16 (Radius < 20 mm), 6 (< 40 mm), 3,6 |
| Akku | LiPo 390 / längste Kante in mm (2,2–8); Li-Ion 280 / längste Kante (2–4), höchstens 3,6 (Dichte der Basis), aber mindestens so hoch, dass eine Zellkappe 9 px hat wie bei `b6s_4000_lion` (21700 ≥ 3,25, 18650 ≥ 3,74) |
| Stack | Whoop-AIO 12, Zahnstocher-AIO 8, 20×20-AIO 6, Turm 20×20 7, 30×30 5,5 |
| Rahmen | nach Radstand 65/142/225/295/820 mm → 9/4,2/3,6/3,0/1,4 |
| Video | 10 (bis 14 mm Kamera), 6,5 analog, 6 digital leicht, 5,5 Air-Unit |

- Schattierung, Kanten und Rauschen kommen ausschließlich aus den Malern der Basis (`paint.carbon`, `alu`, `plastic`,
  `prop_material`, `shrink`, `wire`, `heatsink`, …): helle Oberkante, dunkle Unterkante, Unterseiten 85 %. Nichts von Hand
  nachzeichnen.

### 3.2 Palette und Akzentfarbe

- Grundfarben der Basis: Carbon `#1b1d20`, Whoop-Kunststoff `#232428`, Platine `#141619` mit Goldpads `#d9a33a`,
  Motorfuß `#1f2023` (ab 22 mm Stator `#2a2b2f`), Motordeckel `#1d1e21`, Akkuhülle `#1c1d21`, TPU schwarz `#262628`.
- `visual.accent` der Teil-Definition erscheint **nur dort, wo die Basis eine Teilfarbe trägt**: Motorglocke, Propblatt und
  Nabe, Akku-Band und -Beschriftung (1S: Hülle), Li-Ion-Schrumpfschlauch, TPU-Teile und Alu-Abstandshalter des Rahmens,
  Kamerablende, Kondensatorhülle und Beschriftung der Stacks.
- Jede Akzentfarbe wird auf die Lack-Referenz ihres Kanals abgedunkelt (`paintable.CHANNELS` − 2: Motor 206,
  TPU 240, Akku 198, Kamera 230), sonst bricht die Lack-Prüfung; die Lack-Variante (`_paint`) entsteht automatisch.
  **Ausnahme Props:** Sie tragen nur den Kanal `prop`, die Lack-Prüfung greift erst ab zwei Kanälen, und die Props der
  Basis liegen selbst bis 255 (`p31_3b` `#ff4fa3`, `p31_4b` `#ff7a1a`). JMP-Props behalten deshalb ihre volle Helligkeit;
  ein PC-Prop trägt genau seine Akzentfarbe (JUnit `PropDataTest`).

### 3.3 Anzeige-Transformationen (alle Item-Kontexte)

- Jedes Teil bekommt `assets/justmoreparts/items/<id>.json` im Format der Basis (`kit.item_definition`: `select` auf
  `display_context`, Fall `none` → Drohnen-Modell, sonst dasselbe Modell mit Vor-Skalierung; Props zusätzlich `ccw`,
  `blur` und die Schadensstufen über `propwash:prop_damage`) und im Modell den `display`-Block aus **`kit.compute_display`**
  (`jmpstyle.display.compute`). Kein `minecraft:special` mit `propwash:item/part_base` mehr.
- Damit gelten dieselben Zielgrößen wie in Propwash: GUI 15 px (Motor 14, Prop 15,5), Boden 7, Rahmen (fixed) 12,
  Regal 10, dritte Person 7 × f, erste Person 9 × f (Handfaktor f der echten Größe, unten), Kopf 8; Akkus erste Person
  1,6 (Drehung 5/20/0, Versatz −0,4/4,4/1,13),
  Boden 6, Rahmen 18, Regal 15; Video im Rahmen 16, im Regal 14; Stacks im Rahmen 16, im Regal 13. Drehungen: GUI je
  Kategorie (unten), Boden 0/0/0, Rahmen und Regal 0/180/0 (Akkus und Video 0/90/0 quer, Stacks im Rahmen flach
  −90/180/0 und im Regal 30/180/0), dritte Person 70/45/0, erste Person 10/45/0. Skalen über 3,9
  wandern als Vor-Skalierung in die Item-Definition.
- **Gemessen wird seit Propwash 0.4.0 (Zweig `fix/ground-items`, 02.10.) je nach Kontext:** Boden, Item-Rahmen, Regal und
  Kopf nach dem ganzen gedrehten Begrenzungsquader (längste Kante), GUI und Hände nach der Vorderansicht; in den Händen
  darf der Quader zusätzlich höchstens 0,9 Block (14,4 px) lang sein. Vorher zählte überall nur die Vorderansicht, lange
  Teile lagen deshalb riesig am Boden (JMP-1S-Akkus bis 1,4 Blöcke, 6S bis 0,75) und ragten aus Item-Rahmen und Regal.
  Am Boden gilt zusätzlich das Würfelmaß (Kubikwurzel aus Breite × Höhe × Tiefe) höchstens 0,3 Block
  (`GROUND_BULK_PX` 4,8 px): Motoren lagen vorher 0,44 Block in jeder Richtung und damit größer als ein 5″-Rahmen, jetzt
  wie ein fallengelassener Block (Kante 0,30–0,33). Größenregel und Prüfung: Propwashs `docs/ASSETS.md`, „Größenregel der
  Items“.
- **Item-Rahmen und Regal wie Propwash 0.4.0:** Rahmen-Teile und Props hängen im Item-Rahmen (`fixed`) flach an der Wand,
  Oberseite zum Betrachter, Nase oben (Drehung −90/180/0, Fußkreis um die Hüllenmitte 24 Item-Pixel = Außenmaß des
  Item-Rahmens, Unterseite 0,5 px vor der Rückwand, bei Props zählen die Schadensstufen mit); im Regal (`on_shelf`) sind
  sie um 30° zum Betrachter geneigt (30/180/0). Fertige JMP-Drohnen zeichnet Propwash, sie hängen ebenso an der Wand
  (Fußkreis 72–100 % wie im Inventar). Regeln in Propwashs `docs/ASSETS.md`, „Item-Rahmen und Regal“.
- **Linke Hand** hat wie bei der Basis keine eigenen Einträge: Vanilla nimmt die rechte Hand und spiegelt (Versatz x,
  Drehung y und z negiert). Seit Propwash 0.5.0 (06.10.) misst `kit.compute_display` die Vorderansicht für beide Hände
  (`kit.hand_extent`); Zweiblatt-Props lagen in der linken Hand vorher quer und bis 0,9 Block breit.
- GUI-Drehung je Kategorie (`jmpstyle.display.GUI`): Motor 25/225/0, Prop 58/225/0, Akku 30/225/0, Stack 50/225/0
  (Turm 48), Video 20/205/0, Rahmen mit Ducts 45/205/0, übrige Rahmen 52/200/0.
- **Größen:** Wie in Propwash füllt jedes Item in GUI, Boden, Rahmen, Regal und Kopf seine Zielgröße. Die **echte
  Größe** zeigt das Drohnen-Modell (Drohne, Werkbank, Geist, Flugschule) und im Vergleichsbild der Streifen „gemeinsamer
  Maßstab“, in dem Propwash- und JMP-Modell mit gleichen Pixeln je Millimeter nebeneinander stehen.
  **Entschieden (02.10.):** Gehaltene Items werden nicht nur in JMP nach Teilegröße gestaffelt (Stil-Review 1.0.1,
  Hinweis 6), weil das das Ziel „wie vom selben Zeichner“ bräche; eine Staffelung müsste Propwash für alle Teile
  einführen. **Geändert (06.10., Propwash 0.5.0):** Propwash staffelt jetzt alle Teile in der Hand nach ihrer echten
  Größe – Zielmaß 9 bzw. 7 px × f, f = (L / 180 mm)^0,6, begrenzt auf 0,25–1, L = längste Kante (ein 2207-Motor 0,18 statt
  0,56 Block in der ersten Person, 1103 0,14, 4214 0,26; ab 180 mm unverändert, Akkus in der ersten Person unverändert).
  JMP übernimmt das unverändert über `kit.compute_display` (vendierte `kit.py`), JMP- und Propwash-Teile gleicher Größe
  sind in der Hand gleich groß. Regel, Grenzen und Prüfung: Propwashs `docs/ASSETS.md`, „Teile in der Hand“.
- **Größte Ausdehnung:** Weil Boden, Rahmen, Regal und Kopf die Tiefe mitmessen, wirken weit nach hinten ragende Teile
  dort nicht mehr größer, sondern kleiner. Die Faustregel „Tiefe höchstens etwa das 1,75-Fache der Frontansicht“ (Whoop-
  Antennen steiler, XT60-Zuleitung am 20×20-AIO 9 mm) bleibt für die Lesbarkeit der Vorderansicht. Bewusste Ausnahme ist
  `race_5_stretched` (+11 %): Die Streckung ist der Charakter des Rahmens.
- **Übergroße Teile** (Elemente außerhalb −16…32 Einheiten, heute die vier X-Class-Rahmen ab 460 mm Radstand):
  - Drohnen-Modell: eine gleichmäßig um den Anbaupunkt verkleinerte Kopie (`display.render_factor`: 1/k mit k auf
    0,01 aufgerundet, xclass_10 0,82 … xclass_13 0,46). Die Render-Definition und ihre `_paint`-Variante skalieren sie mit
    `transformation` (`scale` k, `translation` 0,5 − 0,5·k) zurück auf echte Größe. Die Lack-Variante entsteht aus der
    verkleinerten Kopie.
  - Propwash 0.4.0 wendet das an: `DroneRenderResources.resolve` löst die Render-Definition mit `ItemModel.update` auf,
    Vanilla setzt `transformation` als `localTransform` der Ebene (`CuboidItemModelWrapper`). Das gilt beim Zeichnen
    (`ItemStackRenderState.submit`), für Ober- und Unterkante (`getModelBoundingBox`) und in allen Ansichten, die die
    Drohne zusammensetzen: Welt, Werkbank, Geist, Drohnen-Item, Flugschule und `PropwashClientApi.renderPreview` im
    Sharecode-Fenster. Die Modellprüfung `withinModelBounds` sieht die verkleinerten Elemente, sie bestehen sie.
  - Einzige Abweichung: `VisualHull.boundsOf` kappt Teil-Hüllen über 4 Blöcken auf ±2 Blöcke (±256 mm). Die Hülle der
    Drohne bestimmen bei der X-Class aber Motoren und Props an den Armen, die Kappung ändert dort nichts. Die
    Zusicherung dafür bleibt API-Wunsch W30.
  - Inventar-Symbol: eine gleichmäßig verkleinerte Kopie `justmoreparts:item/icon/<kategorie>/<id>`. `compute_display`
    ist maßstabsunabhängig, das Symbol verhält sich also wie ein echtes Modell. Für Rahmen gilt dabei die
    Lesbarkeitsregel der Basis (`FallbackShapes.frameIcon`): Arme mindestens 7 % des Radstands breit, Motorpads 8 %,
    Dicke 1,2 %.
  - `--oversize icon` schreibt wie vor der Stil-Review nur das Symbol, dann zeichnet Propwash die Form aus der Definition.
  - Prüfung: JUnit `FrameDataTest#xClassDroneModelsAreScaledBackToRealSize` (Skala, Versatz, `_paint`, Grenzen,
    Motorpad an der echten Motorposition) und Selbsttest „stil: Drohnen-Modell für jedes Teil, Rahmen in echter Größe“.

## 4. Regeln je Kategorie

### 4.1 Motor (`jmpstyle.motor`)

Aus `stator` („2207“, „1202.5“, „22075“ oder `stator_mm`), `mount` und `visual.accent`. Alle Maße werden über die sechs
Propwash-Motoren (0802, 1404, 2004, 2207, 2306, 2807) interpoliert und darüber hinaus proportional fortgeführt.

| Merkmal | Regel |
|---|---|
| Glocke | Achteck (`kit.cylinder`), Durchmesser = Stator + 2,5…6 mm (interpoliert), Höhe = Statorhöhe + Aufbau über dem Stator, interpoliert durch 0802/1404/2004/2207/2807 der Basis (3,0/5,1/5,7/5,8/6,9 mm, darüber linear fortgeführt, höchstens 9 mm; 2306 der Basis mit 7,2 mm bleibt als Ausreißer außen vor) — 1404 und 2004 sitzen damit so hoch wie die Basis (12,5/13,5 mm), 2306,5 ist sichtbar flacher als 2207,5, 4214 breit und hoch |
| Mantel | Maler `bell_side` der Basis in der Akzentfarbe, Kupfer-Schlitze im unteren Drittel (`#c8733a`, Whoop `#9a5a2a`) |
| Deckel | Sheet `bell_top` (Speichenfenster, Kupfer, silberne Nabe); dunkel `#1d1e21` ab 20 mm Stator, darunter in Glockenfarbe (wie 0802/1404 der Basis) |
| Speichen | vom nächstgelegenen Propwash-Motor (4/5/6), ab 30 mm Stator 6 |
| Fuß und Laschen | Fuß-Achteck und vier Laschen ab 11 mm Stator; Laschenlänge mindestens Schraubkreis der Halterung + 0,55 · Laschenbreite (`m31` 17,7 mm, `m42` 21,2 mm) |
| Welle | unter 11 mm Stator 1-mm-Stift, 11–21 mm 1,5 mm (T-Mount-Props), 22–39 mm M5-Gewinde 9,5–11 mm, ab 40 mm 6 mm Gewinde |
| KV-Varianten | gleiche Statorgröße = gleiche Geometrie; die KV unterscheidet nur die Glockenfarbe (`visual.accent` je Teil) |
| Sitzhöhe | Fuß + Spalt + Glocke → `motor_seat` in `drone_layout.json` (Nabenhöhe der Props) |

Stand 1.0.1: alle 34 Motoren in den Mod-Ressourcen aus dieser Vorlage (`--category motor --write`). Bei gleicher
Statorgröße stimmen Glocke, Fuß, Spalt und Sitzhöhe mit dem Propwash-Motor überein (0802, 1404, 2004, 2207, 2807; nur die
Welle der T-Mount-Motoren ist 1,5 statt 1,6 mm, die Laschen des 2207 reichen 0,26 mm weiter bis zur m22-Schraube).
Übersicht aller Motoren neben den Propwash-Motoren im Inventar und im gemeinsamen Maßstab:
`python3 tools/assets/motor_sheet.py` (Bild `docs/asset-sheets/motor_1.0.1_overview.png`), Vergleich in allen Ansichten
`docs/asset-sheets/motor_1.0.1_compare.png`.

### 4.2 Prop (`jmpstyle.prop`)

Aus `diameter_mm`, `pitch_in`, `blades`, Material (Id-Endung `_pc|_gfn|_cfn|_carbon|_bi|_soft|_hard`, sonst `material`),
Endung `_duct`, `visual.accent`.

| Merkmal | Regel |
|---|---|
| Blattform | Familie nach Durchmesser: bis 45 mm `p31` (ab 4 Blättern `p31_4b`), bis 99 mm `p3_3b`, bis 159 mm `p51_3b`, darüber `p7_2b`; Segmente auf den Radius skaliert, Blatttiefe × (Blätter der Vorlage / Blätter)^0,3 (Ducted ohne diesen Abschlag) |
| Steigung | Blattwinkel = atan(tan(α_Vorlage) · k), k = clamp((P/D) / (P/D)_Vorlage, 0,7, 1,45); Bezug 1,2 / 0,9 / 0,85 / 0,6 |
| Ducted (`_duct`) | volle Blatttiefe der Vorlage auch bei fünf Blättern, äußere Segmente mindestens 96 % bzw. 90 % der breitesten Blatttiefe: breite Paddel mit eckigen Spitzen wie echte Duct-Props |
| Nabe | Durchmesser, Höhe und Blattdicke aus den Propwash-Props interpoliert (5,1/9/13/14 mm Nabe bei 31/76/130/178 mm); unter 60 mm ohne Mutter, 60–99 mm zwei T-Mount-Schrauben, ab 100 mm Nylock-Mutter (ab 250 mm 10,4 mm statt 8,6 mm) |
| Material | PC: Maler `prop_material` wie Basis, genau in der Akzentfarbe · weich (`_soft`): Blätter 0,88 dick, heller und weniger gesättigt · hart (`_hard`): 1,12 dick, satter und dunkler · Bi-Blend (`_bi`): ab 62 % Radius zweite Farbe (Akzent mit 45 % `#f4f1ea`, eigene Tint-Ebene im Kanal `prop`) · GFN/CFN: entsättigt · Carbon: Köperbindung des Basis-Carbon-Malers in der Akzentfarbe (Webung × 1,13, Glanzpunkte × 1,3), darüber der Verlauf Nabe → Spitze von `prop_material` (0,88 → 1,12) und eine helle Glanzkante an der Vorderkante |
| Varianten | `_ccw`, `_blur` (Scheibe der Basis), `_worn`, `_damaged`, `_broken` (Schäden der Basis) samt Lack-Varianten; Texturen wie die Basis je Schadensstufe und Drehrichtung eigen, `_ccw` teilt die Textur mit cw, die Scheibe nutzt die cw-Textur für die Nabe |
| Stand 1.0.1 | alle 38 Props aus `jmpstyle.prop` (`style_generate.py --category prop --write`), alter Generator `tools/props/prop_assets.py` entfernt; Ids, Modell- und Texturpfade unverändert, neu sind die Texturen der Schadensstufen. Übersicht aller Varianten: `python3 tools/assets/prop_sheet.py` → `build/style-compare/prop_variants.png` (Stand: `docs/asset-sheets/style_prop_variants.png`); Vergleich `docs/asset-sheets/style_compare_prop.png` mit den Zeilen Propwash, JMP 1.0.0 und JMP 1.0.1 |

### 4.3 Akku (`jmpstyle.battery`)

Aus `cells`, `capacity_mah`, `mass_g`, `chemistry`, `connector`, `pack_mm`, `visual.accent`.

| Merkmal | Regel |
|---|---|
| LiPo/LiHV/Semi-Solid | Quader wie `batteries.build_lipo`: Stirnseiten `pack_end`, Hüllen-Maler `wrap`, Etikett `label_decal` auf beiden Seiten („6S“ / „2200“), Band oben, Balancer-Kabel ab 2S, Gurt ab 5S oder 70 mm Länge (nie bei 1S-Whoop-Akkus) |
| Akku unter dem Rahmen | Akkus mit Gurt bekommen zusätzlich `<id>_under` (+ `_under_paint`, nur `items/drone_render`, kein Inventar-Item) aus `build_lipo(…, under=True)`: Pack von y = −Höhe bis 0, Gurt `batteries.strap_band_under` um den Boden und durch die Platte, Schnalle unten, Etikett-Band auf der Unterseite. Propwash 0.5.0 zeichnet sie an Rahmen mit dem Akku unten (Race-Rahmen); ohne Variante hängt es das normale Modell an seiner Oberkante auf (Packs ohne Gurt sehen so gleich aus) |
| Maße | `pack_mm` [Länge, Breite, Höhe], sonst 1S: Breite 11 + C/200, Höhe 5 + C/120, Länge aus Masse / 1,6 g/cm³; ab 2S: Zelldicke 4,2 + C/800 mm, höchstens 8,8 mm, Höhe = Zellen · Dicke (immer einreihig gestapelt wie echte Packs), Breite und Länge aus Masse / 2,1 g/cm³ mit Länge = 2,2 × Breite bis 1500 mAh, darüber länger (2,9 × bei 2200 mAh, 3,1 × ab 5000 mAh) — trifft die 6S-Akkus der Basis auf ±2,5 mm, 1S und 4S auf ±7 mm; 6S 2200 ≈ 106 × 36 × 42 mm, 6S 5000 ≈ 145 × 46 × 53 mm, 8S 5000 ≈ 145 × 46 × 70 mm |
| Farben | 1S wie Basis `b1s_300_hv`: Hülle in Akzentfarbe, weißes Band, Schrift in Hüllenfarbe. Ab 2S wie Basis 4S/6S: dunkle Hülle `#1c1d21` (Semi-Solid `#2b2c30` mit Akzentstreifen an den Kanten), Band und Etikett in Akzentfarbe, Schrift dunkel oder weiß nach Helligkeit |
| Stecker | PH2.0, BT2.0, BT3.0 (weiß), A30 (schwarz), XT30, XT60, XT90, AS150 (gelb) mit Kabelstärke 0,9…5,2 mm; Maße in `CONNECTORS` |
| Li-Ion | Aufbau von `batteries.build_lion`: Zellen als Achteck-Zylinder, Etikettband, Schrumpfschlauch-Ringe, Nickelstreifen, Kabel, Stecker, Balancer. Format 21700 (21 × 70 mm) ab 3600 mAh je Parallelstrang, sonst 18650 (18,3 × 65 mm); 2P über 5000 mAh; Anordnung aus `pack_mm` (beste Passung, Zellen längs oder quer zur Flugrichtung) oder 2/3/4 Reihen; Schrumpfschlauch in Akzentfarbe; Zellkappen mit dem Kappen-Maler der Basis (Ring in der Wickelfarbe, heller Ring, Pol) bei 9 px (Dichte §3.1); Band „LI-ION“ bzw. „6S2P“ und „6S 8400“ |
| Li-Ion quer | 6S2P 21700 mit `pack_mm` 133 × 74 × 46 liegt quer: 6 × 2 Zellen, Kappen an beiden Seiten. Die Schrumpfschlauch-Ringe sitzen 14 mm vor den Kappen (statt 5 mm), damit die Oberkante der Kappenseite die Zylinder-Silhouette zeigt, und sind in zwei gleiche Hälften geteilt (gemeinsame Textur, sonst passt die Dichte nicht in 64²). Das Band ist oben wie bei der Basis nur dunkel mit Akzentstreifen; „6S2P“ steht aufrecht auf den Stirnseiten des Bands wie „6S“ bei `b6s_4000_lion`. Nickelstreifen nur als 9 mm hoher Sammelschienen-Streifen unter den Kabeln |

### 4.4 Stack und AIO (`jmpstyle.stack`)

Aus `mount`, `all_in_one`, `cells`, `esc_continuous_a`, `board_size_mm`, `visual.accent`.

| Bauform | Regel |
|---|---|
| Whoop-AIO (`whoop_aio`, bis 2S) | Aufbau von `stacks.build_aio_whoop` (24-mm-Platine, Motorstecker, Empfänger, Antenne, BT-Stecker; 2S breiter mit Aufdruck „2S“) |
| Zahnstocher-AIO (`whoop_aio`, ab 3S) | 26-mm-Platine, flache Dämpfer, XT30, kleiner Kondensator (5 mm) |
| 20×20-AIO | Aufbau von `stacks.build_aio_20` (27 mm, Aufdruck F4), XT30 bis 4S (11,5 mm Zuleitung), sonst XT60 mit 9 mm Zuleitung (Tiefe 56 statt 62 mm, wie `aio_20` der Basis mit 54 mm); Kondensator 6,3 mm (bis 4S) bzw. 8 mm |
| Turm 20×20/30×30 | Aufbau von `stacks.build_stack_30`: ESC-Platine mit Strom-Aufdruck („60A“), FETs je Viertel 1 (bis 35 A), 2 (bis 65 A), 3 (darüber), FET-Größe 2,6–5,0 mm nach Strom, Nylon-Abstandshalter, FC-Platine (F7 bei 30×30) mit MCU, Gyro, Summer (30×30), LED, USB, Muttern, Steckern; 30×30 zwei Kondensatoren 10 × 16 mm, ab 8S 12 × 18 mm; 20×20 ein Kondensator 8 × 12 mm (8S 10 × 16 mm) neben der Zuleitung; XT60, ab 8S und 80 A XT90 |
| Farben | Platine immer `#141619` mit Goldpads (Lack-Kanal `stack`), Akzent als Kondensatorhülle (35 % abgedunkelt) und Aufdruck (35 % aufgehellt), LED rot ab 6S, sonst blau |

### 4.5 Rahmen (`jmpstyle.frame`)

Aus `role`, `shape`, `wheelbase_mm`, `arms`, `props_mm`, `duct`, `battery`, `camera`, `stack_mounts`, `motor_mounts`,
`visual.accent`. Motorpositionen immer aus `arms` (sonst True-X aus dem Radstand) — Deadcat, Stretched-X und Squashed-X
entstehen allein aus den Positionen.

| Merkmal | Regel |
|---|---|
| Arme | Zweiteilig wie `frames.x_arms` (Wurzel bis 50 % der Reichweite, Spitze 0,78 × Wurzelbreite); Wurzelbreite = Faktor × Radstand: Freestyle 7,1 %, Race 4,65 %, Long Range 5,4 %, Cinewhoop 9,9 %, Zahnstocher 5 %, X-Class 3,6 %; Bash × 1,12. Dicke: Freestyle 6 (Deadcat/Squashed 5,5, Bash 6,5), Race 5, Long Range 3/4,5/5, Cinewhoop 3, Zahnstocher 2,5/3, X-Class 5/6 mm |
| Motorpads | Achteck mit Sheet `pad_top`, Durchmesser nach Motorhalterung: m08 11,5, m14 16, m22 25, m28 30, m31 36, m42 46 mm; Schrauben in Akzentfarbe |
| Armschützer | TPU in Akzentfarbe an den Armspitzen (nicht bei Ducts und Zahnstochern) |
| Rumpf | Bodenplatte (Breite nach Stack: 30×30 ±21, 20×20 ±17, Whoop ±14 mm; Race ±16; X-Class 6,5 % der Körperspanne) von −27 % bis +25 % der Körperspanne (vorn mindestens bis 16 mm vor die Kamera), Stack-Bohrungen, Schlitz; Alu-Abstandshalter in Akzentfarbe (drei Paare, Race/Cinewhoop zwei) bis `battery.plate_mm` (Akku unten: bis zur Deckplatte bei 0,1 × Radstand, auf mm gerundet); Deckplatte mit Schlitzen (Akku unten: ohne Schlitze, zwei Bohrungen) und Aufdruck nach der Zollklasse des größten Props auf halbe Zoll gerundet („5IN“, „7IN“, „3.5“ bei halben Zoll, „RACE“, „TP3“, „XC13“), Freestyle zusätzlich zwei Bohrungen wie `freestyle_5`; Gummi-Akkupad (nicht Race); Akku unter dem Rahmen (`battery.on_top: false` ab 100 mm Radstand, die Race-Rahmen): Riemenschlitze an beiden Seiten der Bodenplatte, Schlitzmuster auch auf der Unterseite, Gummipad unter der Bodenplatte bis `plate_mm` (−2,8 = Bodenplatte 2 mm + Pad 0,8 mm) wie `race_5` in Propwash 0.5.0; Kamerakäfig um `camera.pos` (innen 10,8/10,6 mm, Zahnstocher 7,6 mm); GPS-Mast mit Puck ab 250 mm Long Range und X-Class; TPU-Heckhalter in Akzentfarbe (Freestyle mit Aufsatz wie `freestyle_5`), Race wie `race_5` schmal in `tpu_black` (18 × 7 × 6 mm) |
| H-Rahmen | zwei achsparallele Längsholme durch die Motoren jeder Seite, zwei Querstreben, Pads und Rumpf wie oben; Armschützer wie bei `frames.x_arms` innen neben dem Pad (Pad bleibt frei für den Motorfuß). Holme und Streben bestehen aus gleich langen Stücken von höchstens 40 Texeln mit gemeinsamer Textur (sonst drückt ein 500–660 mm langer Holm die Dichte auf die Hälfte); eine Bodenplatte über 64 Texel bekommt eine eigene Nase vor dem Hauptteil |
| Cinewhoop | Aufbau von `frames.build_cinewhoop`, Ducts nach `jmpstyle/duct.py` (unten): `tpu_black`, Rand in Akzentfarbe, zwei Streben je Duct auf Armhöhe, Platten, Abstandshalter; Arme und Pads oben auf Motorhöhe − 0,3 mm, Duct-Unterkante auf den Streben (Motorhöhe − 0,3 mm). Kamerahalter über den Ducts vor dem Akku (Kamerahalter unten): Käfig (innen 10,6 mm) von 0,4 mm über der Duct-Oberkante bis `camera.pos` + 6 mm, z von `camera.pos` − 2 bis + 18 mm, Boden aus Carbon (1,2 mm) unter der Kamera, schmale TPU-Stütze (±3 mm) zwischen den vorderen Ducts auf eine Carbon-Zunge, die die Deckplatte nach vorn verlängert |
| Whoop | Aufbau von `frames.build_whoop`, Ducts nach `jmpstyle/duct.py` (unten): Einteiler `black_matte` mit Lippe, Motorbecher (1,9 mm unter dem Motor bis zum Motorfuß), Speichen und Duct-Unterkante am Becherboden, Brücken, Boden, Akkuschienen, Canopy in Akzentfarbe; Kamerawangen, Achse und Canopy folgen `camera.pos` (Bezug: Linse des 65-mm-Whoops von Propwash bei y 9,7, z −17,8), die Canopy liegt mindestens 0,2 mm über der hinteren Oberkante einer 14-mm-Kamera im Uptilt; sitzt die Kamera weiter vorn, beginnen Wangen und Wangenfüße 0,3 mm über der Duct-Oberkante; `_lite`: Wand × 0,9 ohne Lippe und ohne Canopy (nur Kamerawangen, unverändert) |
| Zahnstocher | dünne Arme, Unibody-Platte (±12,5/14 mm), TPU-Canopy in Akzentfarbe bis `battery.plate_mm`, Gummipad, Kamerawangen |
| X-Class | wie X-Rahmen mit breiten Armen und großem Rumpf; Drohnen-Modell verkleinert mit `transformation` und Symbol nach §3.3 (übergroß) |

**Ducts (`jmpstyle/duct.py`, Stand Propwash 0.5.0):** dieselben Regeln wie `tools/assets/parts/ducts.py` von Propwash,
D = größter Prop des Rahmens. Innenradius D/2 + 1,1 mm (bis 50 mm Prop), sonst D/2 + 1,5 mm; Wand 0,35 + 0,016 × D
(0,8–3 mm); Lippe/Rand oben max(0,45; 0,55 × Wand) breit, max(0,8; 0,02 × D) hoch; 24 Segmente; Oberkante = Nabe +
Blattspitzen (1,75 + 0,024 × D) + 1 mm (Whoop) bzw. 1,2 mm. Die Nabe liegt auf Motorhöhe + Sitz des typischen Motors
(`TYPICAL_MOTOR`: Whoop 65 Lite/75 0802, Whoop 85 1102, Cinewhoop 2,5″ 1404, 3,5″ 2004; `motor_seat` aus
`drone_layout.json`). Die Motorhöhe ist die, die Propwash für Rahmen ohne eigenes Layout nimmt (`FpvRig.motorMountHeightMm`:
1,5 mm unter 100 mm Radstand, 2,5 mm bis 200 mm, darüber 3 mm; mit `arms` deren Höhe). Bis 1.0.0 setzte der Generator
die Motoren auf 0 mm: Motoren schwebten 1,5–2,5 mm über Becher und Pads, die Props ragten 1–2,7 mm oben aus den Ducts.
Propwash zeichnet die Ducts seit 0.5.0 auch im FPV-Bild (Ringsegmente um die Motorachse, siehe Propwash `docs/ASSETS.md`).

**Kamerahalter der Rahmen mit Ducts (Stand 1.1.0):** Das Objektiv sitzt über der Duct-Oberkante, damit die Ducts im
FPV-Bild wie bei echten Whoops und Cinewhoops in den unteren Ecken liegen und Kamera, Linse und Käfig nicht in den Ducts
oder Props stecken. Regel: Kamerakörper (14 mm, bei Cinewhoop 3,5″ 20 mm) samt Uptilt frei von Duct-Wand, Prop-Kreis,
Akku und Action-Cam; Whoops etwa 7 mm über der Oberkante und 2,5–3 mm hinter den vorderen Duct-Mitten, Cinewhoops knapp
über den Ducts vor dem Akku. Der Action-Cam-Halter oben sitzt darüber.

| Rahmen | `camera.pos` vorher → jetzt | Duct-Oberkante | Ducts im FPV-Bild 0/20/40° vorher → jetzt | Props im Bild (Prüfstand) |
|---|---|---|---|---|
| `whoop_75_duct` | (0, 8, −26) → (0, 19, −24) | 12,2 mm | 65/60/56 % → 21/11/0 % | 8,6 → 6,0 % |
| `whoop_85_duct` | (0, 13,4, −17,5) → (0, 21, −27) | 14,1 mm | 46/32/16 % → 21/12/0 % | 6,1 → 7,4 % |
| `whoop_65_lite` | (0, 11,7, −17,5) → (0, 18,5, −17,5), ohne Haube: Kamerawangen vom Boden bis zur Kamera | 12,0 mm | 38/25/10 % → 22/10/0 % (Analyse außerhalb des Spiels, keine Vorlage) | – |
| `cw_25_duct` | (0, 16,4, −33,5) → (0, 29,5, −48); Action-Cam y 25 → 40,5 | 19,5 mm | 75/66/57 % → 19/5/0 % | 6,7 → 2,4 % |
| `cw_35_duct` | (0, 16,5, −43,2) → (0, 34,8, −52); Action-Cam y 31 → 50,5 | 21,1 mm | 69/57/46 % → 16/15/0 % | 6,9 → 13,3 % |

Vorher saß das Objektiv zwischen den vorderen Ducts und unter deren Oberkante: Im FPV-Bild standen zwei Wände über die
volle Bildhöhe, auch bei 40° Uptilt. Die Werte im FPV-Bild stammen aus Propwashs Selbsttest
„drone-render: Guards im FPV-Bild – Analyse gegen gerendertes Bild“ (1708 × 960, gerendert und Analyse gleich). Die
Flugwerte bleiben gleich (Propwashs `motion.txt`: nur die Prüfsumme `spec` der vier Rahmen ändert sich, weil Objektiv und
Kamerapunkt der Hülle mitwandern; `acro`, `angle`, `drop`, `calibration.txt` und `fall.txt` sind gleich). Mit einer Action-Cam
oben liegt deren Masse jetzt höher.

### 4.6 Video (`jmpstyle.video`)

Aus `link`, `camera_width_mm`, `vtx_power_mw`, `mass_g`, `visual.accent` (Kamerablende, Lack-Kanal `camera`).

| Bauform | Regel |
|---|---|
| Analog Whoop (bis 14 mm) | Nano-Kamera der Basis (`analog_nano`) mit Platine und Kabeln, dahinter kleine VTX-Platine mit Chip und Drahtantenne (25° nach hinten) |
| Digital Micro (bis 14 mm) | Nano-Kamera mit flachem Digital-Modul, Kühlblech und einer T-Antenne (15° nach hinten, sonst ragt das Teil so weit nach hinten, dass es am Boden doppelt so groß wie `digital_lite` wirkt) |
| Analog Race / Long Range | Aufbau von `analog_race`: VTX im Schrumpfschlauch, MMCX, Lollipop; ab 1000 mW längerer VTX mit vier Kühlrippen und größerem Lollipop |
| Digital leicht | Aufbau von `digital_lite`: VTX-Gehäuse, Kühlblech, zwei T-Antennen |
| Digital HD / Long Range | Aufbau von `digital_hd`: Air-Unit mit Rippen und zwei Antennen mit roten Spitzen; ab 20 g groß (30 × 10,5 × 34 mm, 6 Rippen), über 600 mW und leichter als 20 g schmaler mit längeren Antennen und größeren Spitzen |
| Kamera | Breite aus `camera_width_mm`, Linse 12 mm analog, 11/12,5 mm digital, 16 mm HD |
| Pro-Einheit | `digital_hd_pro` über `templates.OVERRIDES`: Linse 18 mm (größerer Sensor) und Seitenaufdruck „PRO“ statt „HD“ (Kopie von `video.hd_side` mit Text-Parameter) |

## 5. Ablauf für die Kategorie-Besitzer

```bash
python3 tools/assets/style_generate.py                         # alle 143 Teile nach build/style-preview/assets, danach Stil-Prüfung
                                                               # (--oversize scaled ist Standard, --oversize icon nur Symbol)
python3 tools/assets/style_generate.py --category motor        # nur eine Kategorie (mehrfach möglich), --only <id>
python3 tools/assets/style_generate.py --category motor --write --fragments build/fragments/motor
python3 tools/assets/style_generate.py --check-only common/src/main/resources/assets
python3 tools/assets/compare_render.py --category motor --all  # jedes Teil gegen das nächstgroße Propwash-Teil
```

- `--write` schreibt in die Mod-Ressourcen (Modelle, Texturen, Render-Definitionen, Lack-Varianten, `items/<id>.json`)
  und löscht vorher alle Dateien der erzeugten Ids (auch die der alten Generatoren). `drone_paint.json` und
  `drone_layout.json` gehören Implementierer 4: die anderen Besitzer geben ihm die Fragmente aus `--fragments`; der Writer
  führt beim Schreiben nur die eigenen Schlüssel zusammen.
- Danach die alten Generatoren der Kategorie entfernen: `tools/assets/motor/` (Motoren), `tools/props/prop_assets.py`
  (Props), `tools/assets/power_assets.py` samt `power_paint.json` (Akkus, Stacks), `tools/assets/jmp_models.py` und
  `jmp_preview.py` (Rahmen, Video). `tools/parts_sync.py --write` überschreibt vorhandene `items/<id>.json` nicht.
- Abnahme: „STIL-PRÜFUNG OK“ (Modellgrenzen, Texturen 64², alle `display`-Kontexte mit Skala ≤ 4, `gui_light: side`,
  Modell-Verweise der Items, kein `part_base`-Sonder-Renderer, Lack-Referenzen, unveränderte Basis-Kopie) und
  Vergleichsbilder ohne Ausreißer; im Spiel ein Teil jeder Kategorie in beiden Händen, am Boden und im Rahmen neben dem
  passenden Propwash-Teil.
- Eigene Feinheiten nur über Parameter: jede Vorlage nimmt `overrides` (z. B.
  `motor.build(key, d, {"spokes": 6})`), dauerhaft in `jmpstyle/templates.py` (`OVERRIDES` je Teil-Id) eintragen — nie in
  `propwash_style/`.
- `--write` und `--only` löschen nur die Dateien der erzeugten Id, nicht die einer längeren Id mit gleichem Anfang
  (`xclass_13` lässt `xclass_13_h` stehen).
- Python: ein `python3` mit numpy und Pillow (wie der Propwash-Generator).

## 6. Vergleichswerkzeug `tools/assets/compare_render.py`

- Liest Propwash-Assets aus dem Jar `propwash-common-<minecraft>-<propwash>` in mavenLocal (Versionen aus
  `gradle.properties`), sonst `--propwash-assets <Ordner|Jar>` oder `PROPWASH_ASSETS`. JMP-Quellen: „JMP-Ressourcen“
  (Mod-Ressourcen) und, falls vorhanden, „Stil-API-Vorschau“ (`build/style-preview/assets`); eigene mit
  `--jmp LABEL=ORDNER`.
- Je Kategorie ein Bild `build/style-compare/compare_<kategorie>.png`: je Paar eine Zeile Propwash und eine Zeile je
  JMP-Quelle mit den Spalten GUI (Slot, GUI-Maßstab 8), erste Person rechts und links, dritte Person rechts und links,
  Boden und Rahmen, darunter der Streifen „gemeinsamer Maßstab“ (Drohnen-Modelle mit gleichen Pixeln je Millimeter und
  Hüllenmaß, 50-mm-Lineal; die `transformation` der Render-Definition wird angewendet, X-Class also in echter Größe).
- Nachgebildet nach den 26.3-Quellen: `ItemTransform.apply` (Versatz/16 auf ±5 begrenzt, Drehung XYZ, Skala auf ±4
  begrenzt, linke Hand gespiegelt), `transformation` der Item-Definition, Sonder-Renderer `propwash:part` (Einpassen der
  Hülle aus 45°/225°, Rahmen und Video −20°), `FirstPersonHandsAndItemsRenderer` (0,56/−0,52/−0,72, HUD-Sichtfeld 70°),
  `ItemInHandLayer` (Arm-Drehpunkt −5/2/0 px, X −90°, Y 180°, 1/2/−10 px), `ItemEntityRenderer` (unterste Kante
  1/16 Block über dem Boden) und `ItemFrameRenderer` (Skala 0,5, Kontext `fixed`). Prozedurale Formen aus der Definition
  (Items ohne Modell) werden nicht nachgebildet und als Hinweis markiert.
- Paare: kuratiert je Kategorie (Tabelle `PAIRS`), `--all` für jedes JMP-Teil gegen das Propwash-Teil mit der
  ähnlichsten Hülle, `--pair basis=jmp` für eigene Paare, `--contexts gui,fp_right,…` für eine Auswahl.
- Durchscheinende Modelle (Prop-Scheibe `_blur` mit `force_translucent`) liest `itemview` mit; Texturverweise als Objekt
  (`{"sprite": …}`) werden aufgelöst.
- `docs/asset-sheets/style_compare_<kategorie>.png` zeigen seit 02.10. für alle sechs Kategorien den heutigen Stand
  (Propwash gegen „JMP 1.0.0 (02.10.)“, erzeugt mit `--jmp "JMP 1.0.0 (02.10.)=common/src/main/resources/assets"`); die
  früheren Bögen mit dem Vorher-Stand „JMP-Ressourcen 1.0.0“ der Stil-API-Einführung sind damit ersetzt.
  `style_compare_prop_fixes.png` zeigt die am 02.10. aufgehellten bzw. gesättigten Props (§8.1).

### 6.1 Prüfung im Spiel (seit der Integration 1.0.1)

- Selbsttest `stil: …` (`dev.client.StyleSelfTests`, beide Loader, auch ohne Bildschirmfotos): jedes der 143 Teil-Items
  hat ein eigenes Item-Modell (kein `MissingItemModel`, kein Sonder-Renderer), und in jedem Kontext (GUI, erste und
  dritte Person beide Hände, Boden, Rahmen, Kopf, Regal) liegt die sichtbare Größe jedes JMP-Teils – Ausdehnung der
  Modell-Eckpunkte nach Anzeige-Transformation und Item-`transformation` quer zur Blickrichtung, also dasselbe Maß wie
  `kit.compute_display` – innerhalb von ±10 % der Spanne der Propwash-Teile gleicher Art. Bei der Integration lagen alle
  Teile zwischen ×0,99 und ×1,00. Seit der Stil-Review zusätzlich: jedes Teil hat eine Render-Definition
  `drone_render/<kategorie>/<id>` (kein `MissingItemModel`, also keine Form aus der Definition an der Drohne), und bei
  jedem Rahmen reicht die Hülle des aufgelösten Drohnen-Modells (mit `transformation`, nur Vanilla-API) vom Anbaupunkt bis
  zur äußersten Motorposition der Definition und höchstens 60 mm darüber hinaus.
- Vergleichsbilder `dev.client.CompareShots` (nur mit `-PjmpScreenshots`): vor einer weißen Wand je ein Propwash-Teil
  (links, Schild „Propwash FPV“) und ein JMP-Teil (rechts, „Just More Parts“) im Rahmen, am Boden, in der Schnellleiste
  und in beiden Händen; `_first` erste Person mit dem JMP-Teil rechts, `_swap` (Motor, Akku) mit getauschten Seiten,
  `_third` dritte Person von vorn. Paare: 2207 (Feedback-Paar), 2807 gegen 4214, 5,1″-Dreiblatt gegen 5,1×4,6″ hart, 7″-Zweiblatt gegen 13″ Vollcarbon,
  6S 1300 gegen 6S 2200 (Feedback-Paar) und 8S 5000, 30×30-Stacks, 5″-Freestyle gegen Deadcat, 7″ gegen X-Class 13″,
  Digital HD gegen Clarity HD Pro. Ablage `docs/screenshots/compare/jmp_compare_<paar>_<ansicht>.png`.

## 7. API-Wünsche an Propwash

| Nr. | Wunsch | Genaue Form | Nutzen / heute |
|---|---|---|---|
| W30 | Zusicherung für skalierte Drohnen-Modelle | Addon-Handbuch §6 sichert zu, dass `transformation` in `items/drone_render/**` samt Varianten überall gilt (Welt, Werkbank, Geist, Drohnen-Item, Flugschule, `renderPreview`) und die Modellprüfung die Elemente vor der Transformation prüft; `VisualHull.boundsOf` kappt Teil-Hüllen erst jenseits von ±8 Blöcken (heute ±2) oder rechnet mit der transformierten Hülle ohne Kappung. Alternativ wie bisher gewünscht `PartVisual withRenderScale(double scale)`, JSON `visual.render_scale` | Seit der Stil-Review nutzt JMP die Vanilla-`transformation` für die X-Class-Rahmen: 0.4.0 wendet sie über `ItemModel.update` an (§3.3). Ohne Zusicherung könnte eine spätere Propwash-Version die Render-Definition anders auswerten; der Selbsttest „stil: Drohnen-Modell …“ und `FrameDataTest` merken das |
| W31 | Kategorie-Basis für den Sonder-Renderer | Ressourcen `propwash:item/part_base/{frame,stack,motor,prop,video,battery,accessory}` mit den `display`-Werten von `kit.compute_display` für eine Hülle von einem Block (Akku erste Person 1,6 px usw.); `PartItemRenderer` wählt ohne `base`-Angabe die Kategorie-Basis | Addon-Teile ohne eigenes Modell lägen wie Propwash-Teile in der Hand; heute: JMP liefert für jedes Teil ein eigenes Item-Modell |
| W32 | Stil-Baukasten als veröffentlichtes Artefakt | Maven `de.lardtom:propwash-assetkit:<version>` (Zip mit `tools/assets/parts/*.py` und `SHA256SUMS`) neben `propwash-common` in mavenLocal | JMP müsste nicht kopieren; heute: unveränderte Kopie in `tools/assets/propwash_style/` mit Prüfsummen |

Keiner der Wünsche ist für 1.0.1 wesentlich. **Stand Parts-API 1.4 (Propwash 0.4.0):** W30 umgesetzt als Zusicherung
(Addon-Handbuch §6, Selbsttest „drone-render: skalierte Drohnen-Modelle …“, Hülle bis ±8 Modell-Blöcke), W31 umgesetzt
(`propwash:item/part_base/<kategorie>`; JMP braucht sie nicht, jedes Teil hat ein eigenes Modell), W32 offen (eigenes
Werkzeug-Artefakt, kein Polierpunkt; die geprüfte Kopie bleibt).

## 8. Stil-Review 1.0.1: Befunde und Behebung

| Befund | Behebung |
|---|---|
| 1. X-Class-Rahmen an der Drohne als Ersatzform (flache Platten, keine Pads, keine TPU-Teile, lila Abstandshalter) | Echte Drohnen-Modelle aus `jmpstyle.frame`, verkleinert um den Anbaupunkt und per `transformation` zurück auf echte Größe (§3.3), samt `_paint`-Variante und Einträgen in `drone_paint.json`. H-Rahmen mit geteilten Holmen, deshalb `xclass_13_h` mit 1,26 statt 0,67 Texel je 8 mm (`xclass_13` 1,4) |
| 2. `b6s_8400_lion`: eckige Kappen, flache Seite, Etikett „auf dem Kopf“ | Dichte 3,26 statt 2,2, Kappen jetzt 9 px wie bei der Basis. Schrumpfringe 14 mm vor den Kappen, dadurch zeigt die Oberkante die Zylinder-Silhouette. „6S2P“ steht aufrecht auf den Stirnseiten des Bands wie „6S“ bei der Basis, oben nur Band mit Akzentstreifen. Die bisherige Schrift oben war rechnerisch im GUI aufrecht, aber um 27° schräg und in 3-px-Glyphen als „6:2F“ kaum lesbar (§4.3 „Li-Ion quer“) |
| 3. `b6s_3000_lion` fast eckige 18650-Kappen (8 px) | Dichte 3,74, Kappen 9 px |
| 4. Klone im Inventar | `b6s_4500_lion` Violett `#6a4bc4`, `m0802_19000` Orange `#f28a26`, `m2207_1950` Gelb `#e8c22a`, `m28065_1300` Pink `#d84a8c` |
| 5. Größte Ausdehnung über der Basis | `digital_micro` 17,3 → 12,2 (Basis bis 12,1) und `analog_whoop_25mw` 13,7 → 10,8 über steilere Antennen; `aio_20_6s_45a` 15,5 → 14,0 (Basis bis 13,5) über 9 mm XT60-Zuleitung. `race_5_stretched` bleibt bei +11 %, weil die Streckung der Charakter des Rahmens ist. `b8s_4500_lion` liegt mit −18 % unter der Spanne, weil der Pack fast würfelförmig ist; das entspricht der Logik der Basis |
| 6. Größe gehaltener Items nach Teilegröße | Entschieden 02.10.: bleibt wie bei der Basis (jedes Item füllt seine Zielgröße), Begründung §3.3 |
| 7. Feedback-Bild `motors_base_left_jmp_right.png` falsch benannt | Entschieden 02.10.: keine Änderung am Bild (Eingabe des Owners, liegt außerhalb des Repos); die richtige Zuordnung steht in §1 – links JMP 1.0.0 `m2207_1950`, rechts Propwash `m2207_1750` |

### 8.1 Restbefunde der finalen Stil-Review 1.0.1 (behoben 02.10., in 1.0.0)

| Befund | Behebung |
|---|---|
| Dunkle Carbon- und CFN-Props in dunklen Slots fast unsichtbar | Akzentfarbe bei gleichem Farbton auf die Helligkeit `#6e7482` angehoben: `p330_70_2b_carbon` #4d515b → #6e7482, `p330_60_3b_cfn` → #6f7582, `p305_60_2b_cfn` → #737882, `p254_50_2b_carbon` → #6f7482, `p254_45_3b_cfn` → #6f7682, `p203_45_2b_cfn` #54533f → #828062, `p178_40_2b_cfn` #604a3c → #826451 |
| Klone ohne eigenen Charakter | `aio_whoop_1s_6a` trägt „1S“ in Grün auf der Platine (Whoop-AIOs immer mit Zellzahl), Toothpick-AIOs zeigen ihre Zellzahl („4S“, „6S“) statt „F4“ (XT30 hatten sie schon); `analog_race_800` Magenta-Blende #bf3989 und zwei Kühlrippen (ab 500 mW); `digital_hd_pro` Champagner-Blende #b08d57 und 18-mm-Objektiv; `b6s_3000_lion` Kupfer #c25e1f statt Türkis |
| Bi-Blend- und Soft-Props laufen fast bis Weiß aus | Weiches PC ohne Aufhellung (Farbe ×1,0, Sättigung 0,95 statt ×1,08/0,82), Bi-Blend-Spitze 25 statt 45 % Richtung Weiß, blasse Akzente gesättigt: `p31_14_3b_bi` #f2609e, `p40_14_5b_soft` #df5fa0, `p45_17_4b_bi` #8a7af0, `p130_36_3b_bi` #f0ad55, `p130_41_3b_soft` #f08c35 |
| X-Class-Presets hellgrau | Rahmenlack der vier X-Class-Presets #1b1d20 wie alle anderen Rahmen statt #4a525d; der 10″ mit grünen Motoren und Props (#3fb950) statt fast weißen; Preset-Sharecodes ändern sich (keine im Umlauf), feste Prüfvektoren hängen nicht daran |
| Abgeschnittene Plattenaufschrift „:C10“, „C1:“, „C1.“, „C1“ | X-Class-Platten tragen nur die Zollzahl („10“, „12“, „13“; der H-Rahmen „13“, „13H“ wäre um ein Pixel zu breit), die in die kleine Textur der verkleinerten Modelle passt |
| Alte Vergleichsbögen zeigen „JMP-Ressourcen 1.0.0“ | Alle sechs Bögen neu erzeugt (§6) |
