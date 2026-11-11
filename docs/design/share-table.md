# Sharecodes ab 1.2.0 — Teile-Tabelle statt eigenem Codec

Ab Just More Parts 1.2.0 (für Propwash 0.6.0) gehören Sharecodes Propwash: Propwash liest PW1, PW2 und PW3 und schreibt
nur noch PW3 (`docs/sharecode/pw3.md` im Propwash-Repository). JMP bringt keinen eigenen Codec, kein Share-Fenster und
keinen Web-Export mehr mit, sondern nur seine **Teile-Tabelle**. Die alten Spezifikationen
[`sharecode.md`](sharecode.md) (PW1), [`sharecode-pw2.md`](sharecode-pw2.md) und [`web-export.md`](web-export.md)
(Format 1) bleiben als Referenz.

## 1. Teile-Tabelle

- Datei: `common/src/main/resources/data/justmoreparts/propwash_share/table.json` (Format und Regeln:
  `docs/sharecode/tables.md` im Propwash-Repository).
- Kurzkennung `short_id` 2 (fest vergeben vom offiziellen Verzeichnis, 1 ist Propwash).
- Angelegt mit `ShareTableCheck init --resources common/src/main/resources --namespace justmoreparts --version 1.0.0
  --short-id 2`: je Art die 143 Teile von 1.1.1 in der Reihenfolge der eingefrorenen PW2-Tabellen (alphabetisch je Art),
  `since` 1.0.0, je Teil ein `fallback` aus den Propwash-Presets. Das Ergebnis ist bis auf die Teilenamen gleich dem
  JMP-Eintrag im Verzeichnis-Abzug von Propwash (`data/propwash/propwash_share/directory.json`).
- Neue Teile kommen nur hinten dazu: `ShareTableCheck add --resources common/src/main/resources --version <neue version>`
  (die Whoop-Motoren von 1.2.0 mit `since` 1.2.0), danach `fallback` prüfen. Nie umsortieren, nie löschen; ein
  umbenanntes Teil bekommt einen neuen Eintrag, der alte `deprecated` mit `replaced_by`.
- Stand 1.2.0: 146 Einträge, am Ende der Art `motor` die drei Renn-Whoop-Motoren `m0702_30000`, `m0702_35000` und
  `m0802_27200` mit `since` 1.2.0 und `fallback` `propwash:m0802_22000`; ein weiterer Lauf von `add --version 1.2.0` hängt
  nichts mehr an.
- Vergleichsdatei `common/sharecode-baseline.json` (Ergebnis von `freeze`, Stand 1.2.0 mit den drei Motoren). Nach jedem
  Release neu einfrieren: `ShareTableCheck freeze --resources common/src/main/resources --out common/sharecode-baseline.json`.
- Verzeichnis-Abzug in Propwash: nach einer neuen Tabelle im Propwash-Repository
  `python3 tools/sharecode/sync_directory.py --addon justmoreparts=<Jar oder common/src/main/resources von JMP>
  --version justmoreparts=<Version>` ausführen (bei jedem Propwash-Release). Gegen den Abzug von Propwash 0.6.0-dev.5
  (JMP 1.1.1, 143 Einträge) ändern sich damit nur die Version und die drei angehängten Motoren; die Vergleichsdatei
  `directory-baseline.json` von Propwash gilt weiter, weil nur hinten angehängt wird.

## 2. Prüfungen

- Gradle: `:common:checkShareTable` (hängt an `check`, läuft also in `./gradlew build`) ruft den `ShareTableCheck` aus dem
  Propwash-Jar mit der Projektversion und der Vergleichsdatei auf; jeder Befund bricht den Build ab.
- JUnit `share/ShareTableTest`: keine Befunde mit und ohne Vergleichsdatei, jedes Teil genau ein Eintrag, Vergleichsdatei
  gleich `freeze`, das Werkzeug findet eine Verschiebung und eine Lücke, keine Einträge `breaks`/`incompatible` in den
  Metadaten, Versionsbereiche bis zum Release `>=0.6.0-` und `[0.6.0-alpha,)`, ab dem Release 1.2.0 `>=0.6.0` und
  `[0.6.0,)`.
- JUnit `share/JmpTableTest`: Tabelle = PW2-Tabelle von 1.1.1 (`common/src/test/resources/share/pw2-parts-1.1.1.json`)
  gefiltert auf `justmoreparts:`, neue Teile nur dahinter mit `since` ab 1.2.0; Tabelle gleich dem Verzeichnis-Abzug.
- JUnit `share/LegacyCodesTest`: alle 124 Vektoren aus dem Web-Export von 1.1.1
  (`common/src/test/resources/share/legacy-vectors-1.1.1.json`) und die 61 PW1-Codes von 1.0.0 lesen über
  `PropwashApi.shareCodes()` denselben Bau, Lack, dasselbe Tuning, denselben Namen und dasselbe OSD wie damals; Codes mit
  JMP-Teilen rechnet `convert` in PW3 um, das wieder denselben Bau liest.

## 3. Befehl `/jmp code`

`/jmp code …` und `/justmoreparts code …` nehmen jede alte Form an (`export`, `import`, `info`, `give`, mit Argumenten) und
schreiben nur den Hinweis „Sharecodes gehören jetzt zu Propwash: /propwash code …“. Der Befehl ist anklickbar und setzt
`/propwash code <dieselben Argumente>` in den Chat; angezeigt wird nur der Befehl ohne Code. Der Hinweis entfällt mit
JMP 1.3.0. Texte: Sprachmodul `common/src/lang/command/`. Test: `command/JmpCommandsTest`.

## 4. Web-Export

`./gradlew webExport` (bzw. `:fabric:runWebExport`, `:neoforge:runWebExport`) startet den Dev-Server mit Propwash aus
mavenLocal; `dev.WebExportRun` ruft dort `/propwash webexport all` (Format 2, `docs/web-export.md` im Propwash-Repository)
und kopiert `propwash/web-export` des Lauf-Ordners nach `build/web-export/`. Die Daten für Website und Verzeichnis kommen
damit aus Propwash; JMP prüft nur noch, dass der Befehl `PASS` meldet.

## 5. Propwash ohne JMP 1.2.0

Propwash 0.6.0 mit JMP 1.1.x läuft im Altmodus (JMP-Teile da, nur der Sharecode-Knopf von Propwash, JMP-Teile über die
Tabelle im Verzeichnis-Abzug, Hinweis „Just More Parts 1.2.0 empfohlen“). JMP 1.1.1 liest PW3-Codes nicht und meldet
`UNSUPPORTED_VERSION` (geprüft mit allen 145 PW3-Vektoren von Propwash gegen Tag `v1.1.1`).
