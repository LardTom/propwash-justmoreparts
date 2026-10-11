# Propwash: Just More Parts

Addon für **Propwash FPV** (Minecraft 26.3, Fabric und NeoForge): **143 weitere realistische Drohnenteile** vom
65-mm-Whoop über 2–3,5″-Toothpicks und Cinewhoops, 5″-Freestyle und -Race, 6–8″-Long-Range bis zur 13″-X-Class, **neue
Rahmenformen** (Stretched-X, Squashed-X, Deadcat, H-Frame, Ducts, X-Class) und **Sharecodes**, mit denen man Baupläne,
Lackierung, Flug-Tuning und OSD-Layout als kurzen Text teilt.

JMP bringt keine eigene Flugphysik mit: Jedes Teil ist ein Datensatz mit echten Kennzahlen (Masse, Statorgröße und KV,
Wicklungswiderstand, Wärmegrenzen, Schub- und Leistungsbeiwerte, Steifigkeit, Zellzahl, Kapazität, C-Rate,
Innenwiderstand, Radstand, Arm-Geometrie, Luftwiderstand …), und **Propwash rechnet daraus das Flugverhalten**. Vor- und
Nachteile entstehen deshalb von selbst: Ein Zweiblatt fliegt länger, ein Fünfblatt zieht mehr Strom, ein Li-Ion-Akku hält
ewig und hat keinen Punch, ein Deadcat hat (fast) keine Props im Bild. Die Shift-Hilfe jedes Teils nennt seine Stärken und
Schwächen — und jede dieser Aussagen ist im Prüfstand mit Propwashs eigener Analyse belegt.

Version **1.1.0** · Autor: lardtom · Lizenz: siehe [Lizenz](#lizenz).

## Voraussetzungen
- Minecraft 26.3, Java 25
- **Propwash FPV 0.4.0 oder neuer** (Parts-API 1.4) — Pflicht auf Client **und** Server
- Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 **oder** NeoForge 26.3.0.10-beta

JMP ist eine eigene Mod mit eigener Mod-Id (`justmoreparts`) und benutzt nur die öffentliche API von Propwash. Welten,
Drohnen und Einstellungen von Propwash bleiben unverändert; ohne JMP zeigt Propwash fehlende Teile als Platzhalter.

## Inhalt

| Kategorie | Anzahl | Spannweite |
|---|---|---|
| Motoren | 34 | 0702 bis 4214, mehrere KV-Stufen je Stator (27000 KV bis 330 KV), neue Halterungen 31xx und 42xx |
| Propeller | 38 | 31 mm bis 13″, 2–5 Blätter, PC, weiches/hartes PC, Bi-Blend, Glasfaser-/Carbonfaser-Nylon, Vollcarbon, Duct-Props |
| Akkus | 30 | 1S 250 mAh bis 8S 5000 mAh: LiHV, LiPo, Li-Ion (18650/21700, auch 6S2P), Semi-Solid |
| Zellchemie | 1 | Semi-Solid-LiHV (bis 4,45 V je Zelle): leicht, aber nur 20–25 C |
| Stacks/AIOs | 12 | 1S-Whoop-AIO bis 30×30-8S-Stack mit 100 A |
| Videosysteme | 7 | analog 25 mW bis 1,6 W, digital von Micro bis Long Range, mit dem Sichtfeld ihrer Kamera (110–133°) |
| Rahmen | 22 | Whoops 65–85 mm, Toothpicks 2–3,5″, Cinewhoops 2,5/3,5″, 5″ True-X/Squashed/Stretched/Deadcat/H-Frame, Long Range 4–8″, X-Class 10/12/13″ und 13″-H |
| Presets | 22 | je Klasse fertige, lackierte Drohnen im eigenen Kreativ-Tab |

Außerdem:
- **Überleben:** Rezepte für jedes Teil (Aufwand steigt mit der Größe) und Truhen-Beute vom Dorfhaus bis zur Endsiedlung;
  X-Class-Teile sind ungewöhnlich oder selten und liegen erst in späten Strukturen.
- **Eigene Modelle im Propwash-Stil** mit Lack-Varianten für Motoren, Propeller (mit Schadensstufen), Akkus, Stacks,
  Rahmen und Videosysteme, gebaut aus denselben Bausteinen wie Propwashs eigene Teile und mit derselben Anzeige in Hand,
  Inventar, Rahmen und am Boden; auch die X-Class-Rahmen haben an der Drohne ein echtes Modell in voller Größe.
- **Flugschul-Szenen:** „Rahmenformen im Vergleich“ (fünf 5″-Formen mit gleichen Teilen) und „Builds teilen“. Die
  Flugschul-Taste (M) über einem JMP-Teil öffnet seine Lektionen wie bei Propwashs eigenen Teilen: Werkbank und
  Herstellen für jedes Teil, dazu je nach Art Rahmenformen, Schäden und Props tauschen, Akkus und Ladegerät,
  Konfigurator oder Videosysteme; Presets zusätzlich „Builds teilen“.
- **Deutsch und Englisch** für alle Namen, Hilfen und Fenster; die Teilnamen folgen demselben Schema wie die
  Propwash-Teile (`6S-Akku 2200 mAh`, `1404-Motor, 3800 KV`, `40-mm-Propeller (3 Blätter, 1,7″ Steigung, PC)`,
  Regeln in [`docs/design/part-naming.md`](docs/design/part-naming.md)).

## Rahmenformen kurz erklärt
Die Unterschiede rechnet Propwash aus der Arm-Geometrie, Masse, Kameralage und dem Luftwiderstand jedes Rahmens:
- **True-X:** alle Arme gleich lang — ausgewogen in Roll und Nick.
- **Stretched-X:** Motoren weiter vorne/hinten als seitlich — mehr Roll-Autorität, trägeres Nicken; schlank und schnell
  geradeaus (Race). Die Vorderprops laufen nah an der Kamera vorbei: mehr Props im Bild als beim True-X.
- **Squashed-X:** Motoren weiter seitlich — mehr Nick-Autorität, spritzigere Flips, weniger Props im Bild, dafür trägere
  Rolls.
- **Deadcat:** vordere Arme gespreizt — keine Props im Kamerabild (der 7″-Long-Range zeigt mit 130°-Brille nur die
  Propspitzen an den Bildrändern, rund 2 %), dafür etwas schwerer, und Roll und Nick reagieren unterschiedlich.
- **H-Frame:** Kamera weit vorn — keine Props im Bild, spritzigere Flips; schwer, viel Luftwiderstand, trägere Rolls.
- **Ducts (Whoop/Cinewhoop):** Schutzringe um die Props — prallen ab statt zu brechen und tragen eine Action-Cam, aber
  viel Luftwiderstand.
- **X-Class 10–13″:** 460 bis 820 mm Radstand (Renn-Regelmaß ab 800 mm), 3115- und 4214-Motoren, 6–8S — viel Masse
  und Trägheit, große Props, lange Flüge.

## Sharecodes: so geht's
Ein Sharecode ist ein kurzer Text wie `PW21RSaEPg8Rt` (ein fertiges Preset), mit Tuning und Namen meist 50–75 Zeichen.
Er enthält wahlweise **Teile, Lackierung, Flug-Tuning, OSD-Layout und Namen** einer Drohne. Leerzeichen und
Zeilenumbrüche beim Einfügen sind egal, Groß- und Kleinschreibung zählt, Beschädigungen erkennt die Prüfsumme. Ältere
Codes `PW1-…` aus Testversionen funktionieren weiter.

**An der Werkbank (Teile, Lack, Tuning, OSD)**
1. Drohne in der Propwash-Werkbank zusammenbauen.
2. Knopf **„Sharecode“** oben in der Werkbank (auf Deutsch als Symbol `</>`, damit der Titel lesbar bleibt), Reiter
   **„Exportieren“**: Häkchen für Lack, Tuning, OSD und Namen setzen, **„In die Zwischenablage“** — der Code kann in
   Chat, Discord oder ein Forum.
3. Zum Nachbauen im selben Fenster den Reiter **„Importieren“** wählen, Code einfügen: Das Fenster zeigt die Drohne
   in 3D, prüft alle Teile, nennt Flugwerte und — im Überlebensmodus — eine Einkaufsliste der fehlenden Teile. Ebenen
   wählen (Teile, Lack, Tuning, OSD) und **„Übernehmen“**. Im Überlebensmodus braucht die Werkbank die Teile (und
   Farbstoff für den Lack); im Kreativmodus entstehen sie von selbst.

**Im Konfigurator (Taste `K`, Tab „Sharecode“)**
- *Drohne in der Hand teilen:* Ebenen wählen, Code kopieren.
- *Code anwenden:* nur **Tuning** oder nur **Lack** auf die gehaltene Drohne übernehmen, das **OSD-Layout** übernehmen;
  im Kreativmodus **„Drohne erzeugen“**, im Überlebensmodus die Einkaufsliste.

**Per Befehl** (auch als `/justmoreparts …`)
- `/jmp code export [ebenen]` — Code der Drohne in der Haupthand als anklickbare Chat-Zeile (Klick kopiert), z. B.
  `/jmp code export parts,paint,tune`.
- `/jmp code info <code>` — zeigt Ebenen, Teile, unbekannte Teile und Probleme, ohne etwas zu ändern.
- `/jmp code import <code>` — Kreativmodus: neue Drohne ins Inventar; Überleben: Tuning und Lack auf die gehaltene Drohne.
- `/jmp code give <code> [spieler]` — nur Operatoren: erzeugt die Drohne des Codes im Inventar.

Codes bleiben gültig: Teil-Ids von JMP sind eingefroren; Teile anderer Addons, die fehlen, meldet der Import als
„unbekannt“, statt abzubrechen. Das Format steht in `docs/design/sharecode-pw2.md` (PW2, mit gemessenen Längen) und
`docs/design/sharecode.md` (PW1, wird weiter gelesen), beide mit Testvektoren.

## Entwickeln
1. Im Propwash-Repository einmal `./gradlew publishToMavenLocal` (Propwash 0.5.0).
2. Hier einmal `JAVA_HOME=<jdk25> ./gradlew writeClasspath` (für `tools/fastcheck.sh`), danach `./gradlew build`.
3. Schnell prüfen ohne Gradle: `tools/fastcheck.sh all`, `tools/fastcheck.sh test` (135 JUnit-Tests inklusive der
   Prüfstände aller Kategorien).
4. Spielen: `./gradlew :fabric:runClient` bzw. `:neoforge:runClient` (Propwash wird mitgeladen, Ton stumm).
5. Selbsttest: `./gradlew :fabric:runClientSelftest` (mit `-PjmpScreenshots` zusätzlich Bildschirmfotos nach
   `docs/screenshots/`, darunter die Vergleichsbilder Propwash gegen JMP in `docs/screenshots/compare/`), Rauchtests
   `runClientSmoke`, `runServerSmoke`; Web-Export: `./gradlew webExport` → `build/web-export/`.
6. Teile ändern: JSON unter `common/src/main/resources/data/justmoreparts/propwash_parts/`, danach
   `python3 tools/parts_sync.py --write` und `--check`; Sprachtexte in `common/src/lang/<modul>/`.
7. Modelle ändern: Vorlagen in `tools/assets/jmpstyle/`, dann `python3 tools/assets/style_generate.py --write`
   (braucht numpy und Pillow; Regeln in `docs/design/asset-style.md`), Vergleich mit `tools/assets/compare_render.py`.

## Dokumentation
- `CHANGELOG.md` — Änderungen je Version, kurz und für Spieler (englisch)
- `docs/RELEASE-NOTES.md` — Einzelheiten je Version: Prüfstand-Zahlen, Namensbeispiele, Hinweise
- `docs/ARCHITECTURE.md` — Aufbau, Lebenszyklus, API-Wünsche an Propwash
- `docs/CONVENTIONS.md` — Regeln für Code, Daten, Sprache, Git
- `docs/design/parts-catalog.md` — alle Teile mit Kennzahlen, Pro/Contra und Prüfstand-Werten
- `docs/design/asset-style.md` — Stil-Regeln der Modelle (Propwash-Bausteine, Texturen, Anzeige)
- `docs/design/sharecode-pw2.md` — Sharecode-Format PW2 (Standard)
- `docs/design/sharecode.md` — Sharecode-Format PW1 (wird weiter gelesen)
- `docs/design/web-export.md` — Datenpaket für den Web-Konfigurator
- `docs/modrinth/` — Modrinth-Beschreibung (de/en) und Listing

## Mitmachen
Pull Requests sind willkommen. Vorher [`CONTRIBUTING.md`](CONTRIBUTING.md) lesen; jeder Pull Request braucht den kurzen
Beitragsvertrag [`CLA.md`](CLA.md) (Häkchen in der Pull-Request-Vorlage).

## Lizenz
Ab Version 1.1.0 erscheint der Quellcode von Propwash: Just More Parts unter der
[PolyForm Shield License 1.0.0](https://polyformproject.org/licenses/shield/1.0.0), derselben Lizenz wie bei Sodium und
Propwash FPV. In einfachen Worten:

- **Erlaubt:** den Code lesen und prüfen, selbst bauen, JMP in Modpacks aufnehmen und per Pull Request beitragen (mit
  einem kurzen Beitragsvertrag).
- **Nicht erlaubt:** JMP neu hochladen, Forks als alternative Downloads veröffentlichen und aus der Codebasis
  konkurrierende Mods bauen.
- **MIT:** Die Doku steht unter der MIT-Lizenz; welche Dateien genau, steht in [`LICENSE-MIT`](LICENSE-MIT).
- **Ausstiegsklausel:** Gibt es zwölf Monate lang kein neues Release und keine Aktivität der Maintainer, steht der Code
  automatisch zusätzlich unter der MIT-Lizenz.
- Version 1.0.0 und älter bleiben „Alle Rechte vorbehalten“.

Verbindlich sind die Lizenztexte: [`LICENSE`](LICENSE), [`LICENSE-MIT`](LICENSE-MIT) und [`NOTICE`](NOTICE).

NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
