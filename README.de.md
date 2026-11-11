English version: [README.md](README.md)

# Propwash: Just More Parts

Addon für **Propwash FPV** (Minecraft 26.3, Fabric und NeoForge): **146 weitere realistische Drohnenteile** vom
65-mm-Whoop über 2 bis 3,5″-Toothpicks und Cinewhoops, 5″-Freestyle und -Race, 6 bis 8″-Long-Range bis zur 13″-X-Class, **neue
Rahmenformen** (Stretched-X, Squashed-X, Deadcat, H-Frame, Ducts, X-Class) und 23 flugfertige Presets. Jedes Teil
reist in den **Sharecodes** von Propwash mit, ein Bauplan mit Teilen aus Just More Parts lässt sich also wie jeder
andere als kurzer Text teilen.

JMP bringt keine eigene Flugphysik mit: Jedes Teil ist ein Datensatz mit echten Kennzahlen (Masse, Statorgröße und KV,
Wicklungswiderstand, Wärmegrenzen, Schub- und Leistungsbeiwerte, Steifigkeit, Zellzahl, Kapazität, C-Rate,
Innenwiderstand, Radstand, Arm-Geometrie, Luftwiderstand …), und **Propwash rechnet daraus das Flugverhalten**. Vor- und
Nachteile entstehen deshalb von selbst: Ein Zweiblatt fliegt länger, ein Fünfblatt zieht mehr Strom, ein Li-Ion-Akku hält
ewig und hat keinen Punch, ein Deadcat hat (fast) keine Props im Bild. Die Shift-Hilfe jedes Teils nennt seine Stärken und
Schwächen, und jede dieser Aussagen ist im Prüfstand mit Propwashs eigener Analyse belegt.

Version **1.2.0** · Autor: lardtom · Lizenz: siehe [Lizenz](#lizenz).

## Voraussetzungen
- Minecraft 26.3, Java 25
- **Propwash FPV 0.6.0 oder neuer** (Add-on-API 1.8), Pflicht auf Client **und** Server
- Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 **oder** NeoForge 26.3.0.10-beta

**Beide zusammen aktualisieren:** Just More Parts 1.2.0 braucht Propwash 0.6.0, und Propwash 0.6.0 startet nicht mit
Just More Parts 1.1.x (Fabric nennt die Unverträglichkeit auf seinem Fehlerbildschirm, NeoForge zeigt einen Ladefehler
mit der Bitte, auf 1.2.0 zu aktualisieren).

JMP ist eine eigene Mod mit eigener Mod-Id (`justmoreparts`) und benutzt nur die öffentliche API von Propwash. Welten,
Drohnen und Einstellungen von Propwash bleiben unverändert; ohne JMP behalten Drohnen seine Teile als *Unbekanntes
Bauteil* und sind wieder vollständig, sobald es zurück ist. Auf einem Server ohne JMP bleibt es dort einfach aus; ein
Server mit JMP braucht es bei jedem Spieler, in derselben Version.

## Neu in 1.2.0
- **Drei 1S-Renn-Whoop-Motoren:** 0702 mit 30000 und 35000 KV für 65-mm-Whoops, 0802 mit 27200 KV für 75-mm-Whoops,
  jeder nach den Datenblättern von mindestens drei Herstellern. Bei langem Vollgas werden sie heiß und verlieren
  Leistung wie echte.
- **Neues Preset Renn-Whoop 65 · 1S** mit dem 35000-KV-Motor: sehr spritzig, kurze Flüge, will einen Akku mit hoher
  C-Rate.
- **Sharecodes von Propwash:** Das eigene Share-Fenster von JMP entfällt, `/jmp code` verweist jetzt auf
  `/propwash code`; die PW3-Codes von Propwash 0.6.0 kennen jedes JMP-Teil (siehe [Sharecodes](#sharecodes)).
- **Saubere Akku-Halterung:** Sitzt der Akku, zeigen alle 22 Rahmen keine Stecker und keine Kabel, mit dem Akku oben
  oder unten.
- **Mehr Zubehör-Halterungen:** Jeder Rahmen nimmt das Zubehör seines Propwash-Gegenstücks an, mit Halterung vorne an
  18 Rahmen.
- **Motor-Hitze:** Die Kühlung der 7″- und X-Class-Motoren passt zur neuen Motor-Hitze von Propwash 0.6.0.

## Inhalt

| Kategorie | Anzahl | Spannweite |
|---|---|---|
| Motoren | 37 | 0702 bis 4214, mehrere KV-Stufen je Stator (35000 KV bis 330 KV), 1S-Renn-Whoop-Motoren, neue Halterungen 31xx und 42xx |
| Propeller | 38 | 31 mm bis 13″, 2 bis 5 Blätter, PC, weiches/hartes PC, Bi-Blend, Glasfaser-/Carbonfaser-Nylon, Vollcarbon, Duct-Props |
| Akkus | 30 | 1S 250 mAh bis 8S 5000 mAh: LiHV, LiPo, Li-Ion (18650/21700, auch 6S2P), Semi-Solid |
| Zellchemie | 1 | Semi-Solid-LiHV (bis 4,45 V je Zelle): leicht, aber nur 20 bis 25 C |
| Stacks/AIOs | 12 | 1S-Whoop-AIO bis 30×30-8S-Stack mit 100 A |
| Videosysteme | 7 | analog 25 mW bis 1,6 W, digital von Micro bis Long Range, mit dem Sichtfeld ihrer Kamera (110 bis 133°) |
| Rahmen | 22 | Whoops 65 bis 85 mm, Toothpicks 2 bis 3,5″, Cinewhoops 2,5/3,5″, 5″ True-X/Squashed/Stretched/Deadcat/H-Frame, Long Range 4 bis 8″, X-Class 10/12/13″ und 13″-H |
| Presets | 23 | je Klasse fertige, lackierte Drohnen im eigenen Kreativ-Tab, vom Renn-Whoop 65 · 1S bis zur 13″-H-Frame-X-Class |

Außerdem:
- **Überleben:** Rezepte für jedes Teil (Aufwand steigt mit der Größe) und Truhen-Beute vom Dorfhaus bis zur Endsiedlung;
  X-Class-Teile sind ungewöhnlich oder selten und liegen erst in späten Strukturen.
- **Eigene Modelle im Propwash-Stil** mit Lack-Varianten für Motoren, Propeller (mit Schadensstufen), Akkus, Stacks,
  Rahmen und Videosysteme, gebaut aus denselben Bausteinen wie Propwashs eigene Teile und mit derselben Anzeige in Hand,
  Inventar, Rahmen und am Boden; auch die X-Class-Rahmen haben an der Drohne ein echtes Modell in voller Größe.
- **Flugschul-Szene „Rahmenformen im Vergleich“** (fünf 5″-Formen mit gleichen Teilen). Die Flugschul-Taste (M) über
  einem JMP-Teil öffnet seine Lektionen wie bei Propwashs eigenen Teilen: Werkbank und Herstellen für jedes Teil, dazu
  je nach Art Rahmenformen, Schäden und Props tauschen, Akkus und Ladegerät, Konfigurator oder Videosysteme; Presets
  zusätzlich „Builds teilen“ von Propwash.
- **Zubehör** wie beim Propwash-Gegenstück jedes Rahmens: Halterung vorne an 18 Rahmen, dazu unten am 4″-Long-Range
  und an den Toothpicks und oben am 2,5″-Toothpick.
- **Deutsch und Englisch** für alle Namen, Hilfen und Fenster; die Teilnamen folgen demselben Schema wie die
  Propwash-Teile (`6S-Akku 2200 mAh`, `1404-Motor, 3800 KV`, `40-mm-Propeller (3 Blätter, 1,7″ Steigung, PC)`,
  Regeln in [`docs/design/part-naming.md`](docs/design/part-naming.md)).

## Rahmenformen kurz erklärt
Die Unterschiede rechnet Propwash aus der Arm-Geometrie, Masse, Kameralage und dem Luftwiderstand jedes Rahmens:
- **True-X:** alle Arme gleich lang, ausgewogen in Roll und Nick.
- **Stretched-X:** Motoren weiter vorne/hinten als seitlich, mehr Roll-Autorität, trägeres Nicken; schlank und schnell
  geradeaus (Race). Die Vorderprops laufen nah an der Kamera vorbei: mehr Props im Bild als beim True-X.
- **Squashed-X:** Motoren weiter seitlich, mehr Nick-Autorität, spritzigere Flips, weniger Props im Bild, dafür trägere
  Rolls.
- **Deadcat:** vordere Arme gespreizt, keine Props im Kamerabild (der 7″-Long-Range zeigt mit 130°-Brille nur die
  Propspitzen an den Bildrändern, rund 2 %), dafür etwas schwerer, und Roll und Nick reagieren unterschiedlich.
- **H-Frame:** Kamera weit vorn, keine Props im Bild, spritzigere Flips; schwer, viel Luftwiderstand, trägere Rolls.
- **Ducts (Whoop/Cinewhoop):** Schutzringe um die Props, prallen ab statt zu brechen und tragen eine Action-Cam, aber
  viel Luftwiderstand.
- **X-Class 10 bis 13″:** 460 bis 820 mm Radstand (Renn-Regelmaß ab 800 mm), 3115- und 4214-Motoren, 6 bis 8S, viel Masse
  und Trägheit, große Props, lange Flüge.

## Sharecodes
Seit 1.2.0 gehören Sharecodes zu **Propwash 0.6.0**: JMP hat kein eigenes Share-Fenster und kein eigenes Code-Format
mehr, nur eine Teile-Tabelle (`data/justmoreparts/propwash_share/table.json`), über die die **PW3**-Codes von Propwash
jedes JMP-Teil kennen. Ein Code enthält wahlweise **Teile, Lackierung, Flug-Tuning, OSD-Layout und Namen** einer
Drohne.

- **Wo:** Knopf *Sharecode* an der Drohnen-Werkbank, Tab *Sharecode* im Konfigurator (Taste `K`) und `/propwash code`
  (`export`, `import`, `info`, `convert`, für Operatoren `give`).
- **Codes nennen ihre Addons:** Ein Bauplan mit JMP-Teilen sagt das, mit der nötigen Version (1.2.0 für die drei
  neuen Whoop-Motoren, 1.0.0 für alles andere) und wo es das Addon gibt. Ohne JMP gehen Lack, Tuning und OSD trotzdem,
  und jedes JMP-Teil hat ein Ersatzteil aus Propwash, die Drohne lässt sich also mit Ersatzteilen bauen.
- **Klick im Chat:** Maus über einen Code, den ein anderer Pilot gepostet hat, zeigt die Drohne, ein Klick öffnet ihn
  im Import.
- **Alte Codes funktionieren weiter:** PW1- und PW2-Codes mit JMP-Teilen, auch die aus JMP 1.0 und 1.1, werden
  weiter gelesen; *Als PW3 kopieren* im Import-Fenster oder `/propwash code convert` macht daraus PW3-Codes.
- `/jmp code …` (auch `/justmoreparts code …`) zeigt nur noch einen anklickbaren Hinweis auf denselben Befehl
  `/propwash code …`.

Die Teil-Ids von JMP sind eingefroren, Codes bleiben also gültig. Wie die Teile-Tabelle funktioniert:
`docs/design/share-table.md`; die Formate selbst stehen im Propwash-Repository (`docs/sharecode/pw3.md`). Die alten
JMP-Formate bleiben als Referenz in `docs/design/sharecode-pw2.md` (PW2) und `docs/design/sharecode.md` (PW1).

## Entwickeln
1. Im Propwash-Repository einmal `./gradlew publishToMavenLocal` (die Version aus `propwash_version` in
   `gradle.properties`).
2. Hier einmal `JAVA_HOME=<jdk25> ./gradlew writeClasspath` (für `tools/fastcheck.sh`), danach `./gradlew build`
   (prüft mit `checkShareTable` auch die Teile-Tabelle gegen `common/sharecode-baseline.json`, mit dem Prüfwerkzeug
   aus Propwash).
3. Schnell prüfen ohne Gradle: `tools/fastcheck.sh all`, `tools/fastcheck.sh test` (alle JUnit-Tests inklusive der
   Prüfstände aller Kategorien).
4. Spielen: `./gradlew :fabric:runClient` bzw. `:neoforge:runClient` (Propwash wird mitgeladen, Ton stumm).
5. Selbsttest: `./gradlew :fabric:runClientSelftest` (mit `-PjmpScreenshots` zusätzlich Bildschirmfotos nach
   `docs/screenshots/`, darunter die Vergleichsbilder Propwash gegen JMP in `docs/screenshots/compare/`), Rauchtests
   `runClientSmoke`, `runServerSmoke`; Web-Export: `./gradlew webExport` → `build/web-export/` (ruft auf dem
   Dev-Server `/propwash webexport all` von Propwash auf).
6. Teile ändern: JSON unter `common/src/main/resources/data/justmoreparts/propwash_parts/`, danach
   `python3 tools/parts_sync.py --write` und `--check`; Sprachtexte in `common/src/lang/<modul>/`. Ein neues Teil
   braucht außerdem eine Zeile in der Teile-Tabelle für Sharecodes (`docs/design/share-table.md`).
7. Modelle ändern: Vorlagen in `tools/assets/jmpstyle/`, dann `python3 tools/assets/style_generate.py --write`
   (braucht numpy und Pillow; Regeln in `docs/design/asset-style.md`), Vergleich mit `tools/assets/compare_render.py`.
   Nach einer Änderung an einem Stack-Modell schreibt `python3 tools/connector_models.py` seine Varianten für den
   gesteckten Akku (`--check` prüft nur; Regeln in `docs/battery-plug.md`).

## Dokumentation
- `CHANGELOG.md`: Änderungen je Version, kurz und für Spieler (englisch)
- `docs/RELEASE-NOTES.md`: Einzelheiten je Version: Prüfstand-Zahlen, Namensbeispiele, Hinweise
- `docs/ARCHITECTURE.md`: Aufbau, Lebenszyklus, API-Wünsche an Propwash
- `docs/CONVENTIONS.md`: Regeln für Code, Daten, Sprache, Git
- `docs/design/parts-catalog.md`: alle Teile mit Kennzahlen, Pro/Contra und Prüfstand-Werten
- `docs/design/asset-style.md`: Stil-Regeln der Modelle (Propwash-Bausteine, Texturen, Anzeige)
- `docs/design/share-table.md`: Teile-Tabelle von JMP für die PW3-Sharecodes von Propwash
- `docs/design/sharecode-pw2.md`, `docs/design/sharecode.md`: die alten JMP-Sharecode-Formate PW2 und PW1 (Referenz;
  Propwash liest sie weiter)
- `docs/design/web-export.md`: das alte Web-Export-Format 1 (Referenz; der Web-Export kommt jetzt aus Propwash)
- `docs/battery-plug.md`: Stack-Varianten für den gesteckten Akku
- `docs/modrinth/`: Modrinth-Beschreibung (de/en) und Listing

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
