# Konventionen — Propwash: Just More Parts

Gilt für alle, die an JMP arbeiten (Architekt, Implementierer, Integrator). Vorbild: die Konventionen von Propwash FPV.

## Code
- **Keine Kommentare** in `common/src`, `fabric/src`, `neoforge/src` (auch nicht in Tests, `devtest` und Werkzeugen):
  keine Javadoc-, Block- oder Zeilenkommentare. Erklärungen gehören in `docs/`, Commit-Nachrichten oder sprechende Namen.
  Vor jedem Commit: `python3 tools/strip_comments.py common/src fabric/src neoforge/src`.
- Pakete: `de.lardtom.justmoreparts.<modul>` (Logik), `<modul>.client` (nur Client), Loader-Glue in
  `de.lardtom.justmoreparts.fabric|neoforge`. Keine Wildcard-Imports, `final` und Records wo möglich.
- Klassen, Methoden, Felder auf Englisch; alle Texte für Spieler nur über Sprachdateien.
- **Nur die öffentliche Propwash-API**: `de.lardtom.propwash.api.**` sowie die freigegebenen Klassen
  `ui.client.config.ConfigTab`, `ConfigTabHost`, `StepSlider`, `ui.client.theme.Palette`, `Metrics`, `ThemeText`,
  `ThemeDraw`, `ThemedWidgets`, `ThemedScreen`, `ui.client.widget.FlatButton`, `ScrollPanel`, `TextField` (seit Parts-API 1.4),
  `guide.scene.*`.
  Kein Import aus anderen Propwash-Paketen, keine Mixins, keine Reflection auf Propwash. Fehlt etwas → API-Wunsch
  (Architektur §9) mit genauer Signatur.
- Minecraft-, Fabric- und NeoForge-APIs nur aus den 26.3-Quellen in Propwashs `.ref/` und den Notizen in Propwashs
  `docs/api/*.md` — keine APIs aus dem Gedächtnis älterer Versionen.
- Enums der Propwash-API im `switch` immer mit `default`. Definitionen (`…Definition`) nur über Builder bauen;
  Wertrecords (`BuildSpec`, `DroneData`, `PaintSpec`, `TuneSpec`, …) dürfen ihren kanonischen Konstruktor nutzen, der seit
  Parts-API 1.4 stabil ist (Propwash `parts-api.md` §3.3); Fabriken wie `ImportRequest.onHeldDrone` haben Vorrang.
- Nebenläufigkeit: NeoForge ruft die gemeinsame Init auf einem Worker-Thread; dort nur API-Anmeldungen, nichts mit Welt
  oder Client.

## Daten
- Teile nur als JSON unter `data/justmoreparts/propwash_parts/<kategorie>/<id>.json` mit `"format": 1`; Werte nach
  `docs/design/parts-catalog.md`. Standardwerte weglassen. Ganzzahlige Felder ohne Nachkommastellen.
- Id-Regeln und Namensmuster: `parts-catalog.md` §1.2; geprüft von `tools/parts_sync.py --check`.
- Nach jeder Änderung an Teilen: `python3 tools/parts_sync.py --write` (Item-Symbole, Tags, Rezept-Fortschritte), dann
  `--check`.
- Rezepte eindeutig (auch gegen Propwash), Gruppe `propwash_<kategorie>`, Reiter `equipment`.

## Sprache
- Jeder Modul-Besitzer pflegt `common/src/lang/<modul>/de_de.json` und `en_us.json`; `:common:mergeLang` erzeugt
  `assets/justmoreparts/lang/*.json` (eingecheckt, nie von Hand ändern, doppelte Schlüssel brechen den Build).
- Deutsch **mit Umlauten und ß** (nie ae/oe/ue/ss als Ersatz), typografische Anführungszeichen „…“, Zoll als `″`,
  Dezimalkomma. Englisch sinngemäß, gleich viele Zeilen.
- Pflichtschlüssel je Teil: `part.justmoreparts.<id>`, `justmoreparts.help.<id>.summary`; Rahmen zusätzlich
  `justmoreparts.frame_short.<id>`. Hilfezeilen `.what.N`/`.use.N` höchstens 48 Zeichen, Aufbau nach
  `parts-catalog.md` §2.1 (Kennzahlen, `+ Vorteil`, `− Nachteil`, Einsatz).
- Keine Marken, keine echten Namen, keine E-Mail-Adressen, keine lokalen Pfade in Texten, Assets, Metadaten und Doku.
  Autor und Marke: nur „lardtom“.

## Oberflächen
- Jedes Fenster und jeder Tab im Propwash-Theme (Architektur §6). Knöpfe `FlatButton`, keine Vanilla-Widgets sichtbar.
- Keine Chat-Spam-Ausgaben; Ergebnisse im Fenster, Befehle antworten mit übersetzten Texten.

## Tests
- JUnit ohne Spielstart (`tools/fastcheck.sh test`); Teile nur über `bench.PartFiles.registerAll()` anmelden,
  Versuchsteile im Namensraum `jmp_test`, nie `propwash`.
- Selbsttest-Schritte heißen `<modul>: <was geprüft wird>` (deutsch) und räumen hinter sich auf (Inventar, offene Fenster).
- Test-Instanzen sind stumm (`prepareRuns`), Selbsttest-Welten flach und kreativ.

## Git
- Branch `main` ist die Integration; Implementierer arbeiten in einem eigenen Worktree `<worktrees>/<name>` auf `feature/<name>`.
- Commit-Nachrichten deutsch, im Präsens-Stil „Motoren: 34 Teile, Bench-Test …“; Autor ist die Repo-Einstellung
  (`lardtom`). **Nie pushen, nie hochladen**, kein Remote.
- Nur eigene Dateien ändern (Besitz-Tabelle Architektur §10.2).

## Lizenz und öffentliches Repository
- Nach 1.0.0 wird der Quellcode öffentlich (Export ohne Historie mit `tools/public_export.py`, dasselbe Werkzeug wie in
  Propwash). Alles im Repository muss öffentlich sein dürfen: keine echten Namen, keine E-Mail-Adressen, keine lokalen
  Pfade, keine wörtlichen Zitate des Besitzers oder aus privaten Nachrichten – Anforderungen sinngemäß beschreiben.
  Der Export prüft das und bricht sonst ab.
- Lizenz: Kern unter PolyForm Shield 1.0.0 (`LICENSE`), MIT nur für die Pfade in `LICENSE-MIT` (Doku). Keine
  Lizenzköpfe in Quelldateien. Fremdes Material nur mit verträglicher Lizenz und Eintrag in `NOTICE`; Beiträge von
  außen nur mit Zustimmung zu `CLA.md`.

## Changelog
- Tages-Changelog `changelogs/changelog-YYYY-MM-DD.md` (Vorlage: Neue Features, UI-Änderungen, Bugfixes, Sonstiges),
  deutsch, kurz, was sich geändert hat. Jeder hängt nur eigene Zeilen an (`merge=union`). Kleinigkeiten (Tippfehler,
  Formatierung) nicht eintragen.
