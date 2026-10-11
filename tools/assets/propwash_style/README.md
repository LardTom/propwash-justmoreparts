# propwash_style — unveränderte Kopie des Propwash-Teilegenerators

Quelle: **Propwash FPV 0.4.0**, Ordner `tools/assets/parts` (Stand des Release-Commits `b868af1`, letzte Änderung am
Ordner `2ca96d8` vom 2026-09-29; `kit.py` und `addon.py` aus dem Item-Rahmen-Stand `0c04991` vom 2026-10-02, der mit
dem Neubau von 0.4.0 erscheint: Rahmen und Props hängen im Item-Rahmen flach an der Wand, im Regal geneigt; `addon.py`
und `layout.py` aus dem zusammengeführten 0.4.0-Stand `5de05ef1` vom 2026-10-02: Symbole ohne Modell mit den
Kategorie-Basismodellen `part_base/<kategorie>`, Kamera-Positionen der eingebauten Rahmen wie in `drone_layout.json`;
`kit.py` und `addon.py` aus dem Zweig `fix/ground-items` (`952376d6` und `6916917e`, 2026-10-02): Boden, Item-Rahmen,
Regal und Kopf messen den ganzen gedrehten Begrenzungsquader statt nur der Vorderansicht, Akkus am Boden 6 px und im
Rahmen quer und länger, Symbole ohne Modell mit `"box": true`; danach `4a621b90`: am Boden höchstens Würfelmaß 0,3 Block
für sperrige Teile wie Motoren, `GROUND_BULK_PX`, Symbole ohne Modell am Boden mit `"ground": true`); `kit.py` und
`addon.py` aus dem Zweig `fix/item-hand-size` (2026-10-06, Propwash 0.5.0): Teile in der Hand nach ihrer echten Größe
(`hand_factor`, `HAND_FULL_MM`, `HAND_EXPONENT`, `HAND_MIN_FACTOR`), Vorderansicht für beide Hände (`hand_extent`), Versatz
in der dritten Person zur Hand (`GRIP_CONTEXTS`), Symbole ohne Modell in den Händen mit `"hand": true`; `batteries.py` aus
dem Zweig `feat/race-battery` (`287b6ff6`, 2026-10-08, Propwash 0.5.0): Akkus unter dem Rahmen – `strap_band_under`
(Riemen um den Boden, Schnalle unten), `wrap(…, under=True)` (Etikett-Band auf der Unterseite), `UNDER_SUFFIX`,
`build_lipo(key, under=True)` hängt den Pack mit der Oberseite an y = 0.
Aus diesem Stand sind die Teilmodelle im Jar `propwash-common-26.3-0.4.0` erzeugt, gegen das JMP baut.

Die Dateien sind **byte-gleich** mit der Quelle (Prüfsummen in `SHA256SUMS`, geprüft von
`tools/assets/style_generate.py` bei jedem Lauf). Hier wird nichts geändert: JMP-eigene Regeln stehen ausschließlich in
`tools/assets/jmpstyle/` (Stil-API, siehe `docs/design/asset-style.md`).

| Datei | Inhalt |
|---|---|
| `kit.py` | Modell-Baukasten: `Model`, `box`, `cbox`, `bar`, `cylinder` (Achteck aus zwei um 45° versetzten Kastensätzen), `cylinder_x`, `ring`, Textur-Packer (64 × 64, Dichte in Texel je 8 mm), `compute_display` (Anzeige-Transformationen aller Item-Kontexte), `item_definition`, JSON- und Blockbench-Ausgabe |
| `paint.py` | Palette und Maler: `carbon`, `alu`, `plastic`, `prop_material`, `shrink`, `wire`, `heatsink`, Schrift, Rauschen, Kanten |
| `paintable.py` | Lack-Varianten (`_paint`), Kanäle und Referenz-Grau, `drone_paint.json` |
| `motors.py`, `props.py`, `batteries.py`, `stacks.py`, `frames.py`, `video.py`, `accessories.py` | Vorlagen und Teile der eingebauten Kategorien |
| `layout.py` | Rahmen-Layout und Sitzhöhen der eingebauten Teile |
| `render.py` | Software-Rasterer der Vorschauen (nutzt `compare_render.py`) |
| `addon.py`, `addon_preview.py` | Addon-Generator der Basis (Referenz; JMP nutzt daraus `Output`, `write_paint`, `write_props`, `register_materials`) |

Nicht kopiert: `generate.py`, `preview.py`, `paint_preview.py`, `validate.py` (schreiben oder prüfen nur Propwash-eigene
Ressourcen).

## Aktualisieren

1. Die Dateien der Tabelle aus dem neuen Propwash-Stand unverändert hierher kopieren.
2. `shasum -a 256 *.py > SHA256SUMS` in diesem Ordner, Quelle und Stand oben anpassen.
3. `python3 tools/assets/style_generate.py` und `python3 tools/assets/compare_render.py` laufen lassen und die
   Vergleichsbilder ansehen; brechen Aufrufe aus `jmpstyle`, dort anpassen (nie hier).

Benötigt ein `python3` mit numpy und Pillow (wie der Generator in Propwash).
