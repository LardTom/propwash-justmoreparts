# Versionen und Releases

Just More Parts (JMP) folgt demselben Schema wie Propwash FPV (`docs/RELEASING.md` im Propwash-Repository). Es gibt
drei Nummern: die **Mod-Version** von JMP, die **Propwash-Version**, gegen die JMP gebaut ist und die es mindestens
braucht, und die **Parts-API-Version**, die JMP beim Start prüft.

## 1. Mod-Version

Öffentlich zählen nur Releases. Ein Release hat eine saubere Nummer nach SemVer (`MAJOR.MINOR.PATCH`), erscheint auf
CurseForge bzw. Modrinth und hat einen eigenen Abschnitt in `CHANGELOG.md`.

- **MAJOR** steigt, wenn Sharecodes, gespeicherte Drohnen oder Teil-Ids nicht mehr kompatibel sind (Teil-Ids sind
  eingefroren, `docs/design/sharecode.md` §11), **MINOR** bei neuen Teilen, Funktionen oder flugwirksamen Datenänderungen,
  **PATCH** bei Fehlerbehebungen und Texten ohne flugwirksame Änderung.
- Die Nummer steht nur in `gradle.properties` (`version=`); Jar-Namen, `fabric.mod.json`, `neoforge.mods.toml` und der
  Web-Export (`manifest.json`: `generator`, `mods.justmoreparts`) leiten sie ab. README, `docs/modrinth/` und
  `docs/ARCHITECTURE.md` nennen sie und werden beim Release angepasst.
- `CHANGELOG.md` ist kurz, englisch und für Spieler geschrieben und enthält nur öffentliche Releases (Entscheidung des
  Besitzers vom 02.10.2026, gleicher Aufbau wie bei Propwash): Überschrift `## [x.y.z] — <Titel in einer Zeile>`
  (bestehende Überschriften bleiben, damit die Anker stabil sind), `### Highlights` mit 2–3 Punkten, dann `### New`,
  `### Improved` und `### Fixed` mit je einer Zeile pro Punkt (leere Abschnitte entfallen) und `### Compatibility` mit
  2–4 Zeilen (Minecraft, Loader und Java, nötige Propwash- und Parts-API-Version, ob Drohnen und Sharecodes
  weiterlaufen). Keine Implementierungsdetails, keine Testzahlen, keine internen Versionsnamen; der Abschnitt wird
  unverändert als Changelog auf Modrinth und CurseForge eingefügt.
- Einzelheiten (Prüfstand-Zahlen, Namensbeispiele, Hinweise) stehen in `docs/RELEASE-NOTES.md`. Die Arbeitsschritte
  dazwischen stehen in den Tages-Changelogs unter `changelogs/`.

### 1.1 Interne Stände

Zwischen zwei Releases trägt jeder Stand die Nummer des **nächsten** geplanten Releases mit Vorab-Suffix:

| Stand | Beispiel | Bedeutung |
|---|---|---|
| Entwicklungsstand | `1.0.1-dev.1`, `1.0.1-dev.2`, … | Zwischenstände zum Testen und für den Web-Konfigurator |
| Release-Kandidat | `1.0.1-rc.1`, `1.0.1-rc.2`, … | vollständig gebaut und geprüft; wird unverändert zum Release |
| Release | `1.0.1` | öffentlich, gleicher Commit wie der letzte Release-Kandidat |

Ein interner Stand wird nie hochgeladen; seine Jars und sein Web-Export liegen unter `release/<version>/` (nicht im
Repository). Die Website bekommt Konfigurator-Daten nur aus Releases.

**Hinweis Versionsvergleich:** Fabric vergleicht nach SemVer (`1.0.1-dev.1` < `1.0.1-rc.1` < `1.0.1`), NeoForge wie
Maven – dort stehen unbekannte Suffixe wie `-dev.N` **hinter** dem Release, `-rc.N` korrekt davor. Abhängigkeiten nennen
deshalb als Untergrenze immer ein öffentliches Release; soll die Reihenfolge auch unter NeoForge stimmen, ist
`-alpha.N` statt `-dev.N` die Maven-sichere Schreibweise (Entscheidung des Besitzers).

### 1.2 Geschichte

Die intern gebauten Stände 1.0.0–1.0.5 (gegen die internen Propwash-Stände 0.4.0–0.4.4) sind zusammen die erste
Veröffentlichung **Just More Parts 1.0.0** gegen **Propwash FPV 0.4.0**. Ihre Dateien liegen unter
`release/internal/1.0.0-internal` … `release/internal/1.0.5-internal`, die öffentlichen unter `release/1.0.0/`.
`docs/ARCHITECTURE.md` und `docs/design/parts-catalog.md` nennen die internen Nummern weiter als Arbeitsstände.
Interne Stände bis zum nächsten Release hießen `1.0.1-dev.N` / `1.0.1-rc.N` (bzw. `1.1.0-…`).

Am 06.10.2026 wurde 1.0.0 vor der Veröffentlichung mit den kurzen Sharecodes PW2 (`design/sharecode-pw2.md`) neu
erstellt; die Version bleibt 1.0.0, der Tag `v1.0.0` zeigt auf den neuen Stand (Zweig `release/1.0.0-sharecode`). Der
Stand davor liegt unter `release/internal/1.0.0-pre-sharecode`. Der für Propwash 0.5.0 gebaute, nie veröffentlichte
Stand 1.0.1 liegt unter `release/internal/1.0.1-for-0.5.0`; Just More Parts für Propwash 0.5.0 erscheint als **1.1.0** (Tag `v1.1.0`, Dateien unter
`release/1.1.0/`); der Tag `v1.0.1` bleibt als Markierung dieses internen Stands. Interne Stände bis zum nächsten
Release heißen `1.1.1-dev.N` / `1.1.1-rc.N` (bzw. `1.2.0-…`).

## 2. Propwash-Abhängigkeit

- `gradle.properties`: `propwash_version` = die öffentliche Propwash-Version, gegen die gebaut wird (aus mavenLocal,
  im Propwash-Repository `./gradlew publishToMavenLocal`), `propwash_version_range` (NeoForge, `[0.4.0,)`) und
  `propwash_fabric_range` (Fabric, `>=0.4.0`) mit dem ältesten öffentlichen Propwash-Release, das alles Nötige hat.
- Ein Release von JMP wird immer gegen ein **öffentliches** Propwash-Release gebaut und geprüft, nie gegen einen
  `-dev`- oder `-rc`-Stand.

## 3. Parts-API

- JMP prüft beim Start `PropwashApi.requireVersion("justmoreparts", JustMoreParts.REQUIRED_API)` (heute **1.4**:
  Flugzeit-Kennzahlen, `power_w`, `usable_fraction`, eigener Kreativ-Tab, Flugschul-Verknüpfungen, Beute-API); `propwash_api_version` in
  `gradle.properties` nennt denselben Wert für die Doku.
- Die API-Version ist unabhängig von der Mod-Version (Propwash 0.4.0 bringt API 1.4.0). Braucht JMP eine neue
  API-Funktion, steigen `REQUIRED_API` und die Propwash-Untergrenze gemeinsam.

## 4. Ablauf eines Releases

1. Version in `gradle.properties` setzen, `CHANGELOG.md` (Aufbau nach §1) und `docs/RELEASE-NOTES.md` mit einem
   Abschnitt für das Release (alle internen Stände seit dem letzten Release), README, `docs/modrinth/` und
   `docs/ARCHITECTURE.md` anpassen.
2. Propwash-Release in mavenLocal veröffentlichen, hier `./gradlew writeClasspath`.
3. `python3 tools/strip_comments.py`, `python3 tools/parts_sync.py --check`, `tools/fastcheck.sh all` und
   `tools/fastcheck.sh test`; im Propwash-Repository `tools/calibration.sh` (Klassenfenster, Owner-Fall, Reihenfolge der
   Flugzeiten, Bewegungs-Fingerabdruck).
4. `JAVA_HOME=<jdk25> ./gradlew clean build`.
5. Stumme Läufe einzeln nacheinander: Selbsttest Fabric (mit Bildschirmfotos) und NeoForge, Rauchtests Client und
   Server beider Loader. Nennt der Changelog ein älteres Propwash-Release als lauffähig (Untergrenze aus §2), dieselben
   Rauchtests und den Selbsttest NeoForge auch gegen dieses Release:
   `./gradlew -Ppropwash_version=<älteres Release> :fabric:runClientSmoke :fabric:runServerSmoke :neoforge:runClientSmoke
   :neoforge:runServerSmoke :neoforge:runClientSelftest` (die Jars aus mavenLocal müssen die veröffentlichten sein,
   `SHA256SUMS` vergleichen). Danach normal gegen `propwash_version` neu bauen.
6. `./gradlew webExport` (beide Loader, gleich bis auf den Zeitstempel).
7. Jars (ohne `-sources`/`-javadoc`) und `web-export/` nach `release/<version>/` mit `SHA256SUMS`; interne Stände nach
   `release/internal/` verschieben, nie löschen.
8. Commit auf `main`. Die Website bekommt die Konfigurator-Daten aus `release/<version>/web-export`. Hochladen auf
   CurseForge und Modrinth macht der Besitzer.
9. Öffentlicher Quellcode (ab der ersten Version nach 1.0.0): `python3 tools/public_export.py` erzeugt aus dem
   Release-Commit den Stand ohne Historie in `<Hauptbaum>-public`, prüft ihn (Lizenzdateien, Sperrliste, lokale Pfade,
   Zitate, interne Ordner), baut ihn gegen Propwash aus mavenLocal und legt dort genau einen Commit an. Ein vorhandener
   Export wird dabei samt seiner Historie neu angelegt, damit nichts aus älteren Exporten mit veröffentlicht wird; hat er
   schon ein Remote, bricht das Werkzeug ab (`--append` hängt dann einen Commit an, `--fresh` legt trotzdem neu an).
   Veröffentlicht (Remote, Push) wird nur nach ausdrücklicher Freigabe des Besitzers.
