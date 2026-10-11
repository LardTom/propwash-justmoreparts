# Propwash: Just More Parts — Architektur

Addon für **Propwash FPV** (Minecraft 26.3, Fabric + NeoForge, Java 25), eigenes Projekt nach dem Vorbild der
Create-Addons: eigene Mod-Id `justmoreparts`, Paket `de.lardtom.justmoreparts`, harte Abhängigkeit auf Propwash FPV
**≥ 0.4.0** (Parts-API **1.4**). Autor: lardtom. Stand: **Version 1.1.0** für Propwash 0.5.0 (§10.22); davor öffentliche Version 1.0.0 (§10.14, Nachträge §10.15–§10.20). Sie fasst die internen
Stände zusammen, die dieses Dokument weiter mit ihren Nummern nennt (`docs/RELEASING.md`): 1.0.5 Datenkorrektur nach dem
zweiten Realismus-Review, §10.13; 1.0.4 realistische Flugzeiten und eigener Kreativ-Tab, §10.12; 1.0.3 Deadcat LR 7″
wieder mit Kamera wie geflogen, §10.11; 1.0.2 „Props im Bild“ wie im FPV-Bild, §10.10; 1.0.1 Modelle im Propwash-Stil
§10.8; 1.0.0 siehe §10.6.

JMP benutzt **ausschließlich die öffentliche API** von Propwash (`de.lardtom.propwash.api.*` und die dort freigegebenen
Theme-/Widget-Klassen). Keine Mixins, kein Reflection-Zugriff auf Propwash-Interna, keine kopierten Propwash-Klassen.
Fehlt etwas Wesentliches, steht es als **API-Wunsch** in Abschnitt 9 (mit genauer Signatur) und wird in Propwash
nachgerüstet — nie im Addon nachgebaut.

Maßgebliche Dokumente:

| Dokument | Inhalt |
|---|---|
| `docs/CONVENTIONS.md` | Code-, Daten-, Sprach- und Git-Regeln |
| `docs/design/parts-catalog.md` | vollständige Teile-Liste mit Kennzahlen, Pro/Contra, Rahmenformen, Presets, Prüfstand-Ergebnissen |
| `docs/design/sharecode-pw2.md` | Sharecode-Format PW2 (Standard ab 06.10.2026, gemessene Längen, Testvektoren) |
| `docs/design/sharecode.md` | Sharecode-Format PW1 (versioniert, mit Testvektoren; wird weiter gelesen) |
| `docs/design/web-export.md` | JSON-Export für den Web-Konfigurator |
| Propwash-Repository, Branch `integration` (0.4.0): `docs/api/addon-guide.md`, `docs/design/parts-api.md`, `docs/design/parts-api-data.md`, `docs/ASSETS.md`, `docs/design/flight-model-spec.md` §17 | Vertrag und Handbuch der Parts-API |
| Propwash-Repository `.ref/` und `docs/api/*.md` | echte 26.3-Quellen und -Notizen (Minecraft, Fabric API, NeoForge) — nur daraus APIs nehmen |

---

## 1. Build

MultiLoader-Aufbau wie Propwash (Gradle 9.7.1, Wrapper im Repository):

| Modul | Inhalt |
|---|---|
| `build-logic` | Konventionen `multiloader-common` (Java 25, UTF-8, Ressourcen-Expansion, Repository `mavenLocal` exklusiv für `de.lardtom`) und `multiloader-loader` (Quell-Sets `main` + `devtest` aus `common` einbinden) |
| `common` | gesamter Code und alle Ressourcen, kompiliert gegen NeoForm 26.3 (Mojang-Namen); Sprachdateien werden aus `common/src/lang/<modul>/` zusammengeführt (`:common:mergeLang`, doppelte Schlüssel brechen ab) |
| `fabric` | Einstiege, Plattformdienst, `fabric.mod.json`; Loom 1.18.1 |
| `neoforge` | Einstiege, Plattformdienst, `neoforge.mods.toml`; ModDevGradle 2.0.147 |

**Propwash-Abhängigkeit (mavenLocal, im Propwash-Repository mit `./gradlew publishToMavenLocal` veröffentlicht):**

| Modul | Konfiguration | Koordinate |
|---|---|---|
| `common` | `compileOnly` und `testImplementation` | `de.lardtom:propwash-common-26.3:0.4.0` |
| `fabric` | `implementation` (Dev-Runs laden Propwash mit) | `de.lardtom:propwash-fabric-26.3:0.4.0` |
| `neoforge` | `implementation` (Dev-Runs laden Propwash mit) | `de.lardtom:propwash-neoforge-26.3:0.4.0` |

Version und Bereiche stehen in `gradle.properties` (`propwash_version`, `propwash_version_range` für NeoForge
`[0.4.0,)`, `propwash_fabric_range` für Fabric `>=0.4.0`, `propwash_api_version` 1.4). Die Varianten tragen das Attribut
`io.github.mcgradleconventions.loader` wie bei Propwash; jedes Modul fordert seine Variante an.

**Runs** (Gruppe `justmoreparts`; Client-Runs stumm über `prepareRuns`: Master 0,001, alle Kategorien 0, kein Pausieren,
kein Barrierefreiheits-Onboarding):

| Task | Zweck | Ergebnis |
|---|---|---|
| `:fabric:runClient`, `:neoforge:runClient` | Entwicklung (Propwash + JMP) | — |
| `:fabric:runClientSelftest`, `:neoforge:runClientSelftest` | Selbsttest: Flachwelt, alle `SelfTest`-Schritte, Ende | `runs/selftest-client/justmoreparts-selftest-result.txt` (`PASS …`/`FAIL …`), Task scheitert bei FAIL |
| `:fabric:runClientSmoke`, `:neoforge:runClientSmoke` | Start-Rauchtest: Welt anlegen, betreten, verlassen | wie oben |
| `:fabric:runServerSmoke`, `:neoforge:runServerSmoke` | dedizierter Server, 100 Ticks, Register eingefroren, Drohne erzeugbar, Log ohne ERROR | wie oben |
| `:fabric:runWebExport`, `:neoforge:runWebExport` | dedizierter Server schreibt den Web-Export nach `build/web-export/` und beendet sich | wie oben (Implementierer 5) |
| `webExport` (Wurzel) | Kurzform für `:fabric:runWebExport` | `build/web-export/` |

Bildschirmfotos: `./gradlew :fabric:runClientSelftest -PjmpScreenshots` setzt `justmoreparts.screenshots` auf
`docs/screenshots/`; nach allen Selbsttest-Schritten stellt `dev.client.ShowcaseShots` das Fenster auf 1920 × 1080
(GUI-Stufe 3) und schreibt `jmp_lineup`, `jmp_scale` (Größenklassen im gemeinsamen Maßstab), `jmp_workbench_xclass13`,
`jmp_share_export`, `jmp_share_import`, `jmp_config_tab` (echter Konfigurator: Taste `key.propwash.configurator`, dann
der Seitenleisten-Eintrag), `jmp_frames_scene` (beschriftete Draufsicht) und `jmp_frames_scene_stretched`; danach
schaltet es auf Deutsch (`LanguageManager#setSelected`, Ressourcen neu laden) für `jmp_workbench_xclass13_de`,
`jmp_share_import_de` und `jmp_config_tab_de` und wieder zurück (`dev.client.Screenshots`, Vorbild Propwash). Für den
Tab „Sharecode“ bei kleiner Oberfläche stellt es das Fenster dazwischen auf 640 × 480 (GUI-Stufe 2, 320 × 240 Punkte),
scrollt den Tab ans Ende und schreibt `jmp_config_tab_small_de` und `jmp_config_tab_small`; der Schritt verlangt, dass
die Zusammenfassung umbricht (`ShareConfigTab.flowProblem`).

Nur die Selbsttest-/Rauchtest-/Export-Runs laden das Quell-Set `devtest` (Fabric); NeoForge führt es in allen Dev-Runs
als zweites Quell-Set, ohne Property bleibt es inaktiv.

**Werkzeuge** (`tools/`):

| Datei | Zweck |
|---|---|
| `fastcheck.sh [common\|fabric\|neoforge\|all\|test …]` | kompiliert ohne Gradle gegen `.classpath/` (einmalig im Hauptbaum `./gradlew writeClasspath`); `test` führt JUnit mit dem Konsolenstarter aus (`JMP_TAIL=200` für lange Ausgaben, `--select-class …` für eine Klasse) |
| `strip_comments.py <pfade>` | entfernt Kommentare aus Java-Code (vor jedem Commit) |
| `parts_sync.py --write\|--check` | erzeugt Item-Symbole (`propwash:part`-Renderer), Tags `#propwash:parts/<kategorie>` und Rezept-Fortschritte aus den Teile-Dateien; prüft Id-Regeln, Namen und Hilfen in de/en, Zeilenlängen, Umlaut-Umschreibungen, Rezepte und Rezept-Kollisionen (auch gegen Propwash) |
| `sharecode_ref.py [code]` | Referenz-Kodierer/-Dekodierer PW1, erzeugt die Testvektoren |
| `public_export.py [--check-only]` | öffentlicher Stand ohne Historie nach `<Hauptbaum>-public`: Ausschlüsse in `public_export.json` (Tages-Changelogs, Bildschirmfotos, das Werkzeug selbst), Prüfungen (Lizenzdateien, Sperrliste außerhalb des Repositorys plus Namen und Adressen der Git-Historie, lokale Pfade, Zitate, interne Ordner), Build im Export, Lizenzdateien in den Jars, genau ein Commit dort (ein vorhandener Export wird ohne seine frühere Historie neu angelegt; mit Remote Abbruch, dann `--append` oder `--fresh`); kein Remote, kein Push. Gleiche Datei wie in Propwash, nicht im Export |

Worktrees nutzen den Klassenpfad des Hauptbaums (`fastcheck.sh` findet ihn über `git rev-parse --git-common-dir`).

---

## 2. Quell-Sets

| Quell-Set | Inhalt | Im Release-Jar |
|---|---|---|
| `common/src/main` (+ `fabric/src/main`, `neoforge/src/main`) | das Addon | ja |
| `common/src/devtest` | Selbsttest-Harness (`dev.SelfTest`, `dev.client.SelfTestRunner`), Rauchtests, Web-Export-Run, Selbsttest-Schritte je Modul | nein |
| `common/src/test` | JUnit, ohne Spielstart; Prüfstand `bench.*` (meldet die JMP-Teile über die öffentliche API an und misst `builds().analyze`) | nein |

`core.DevHooks` lädt `dev.DevBootstrap` (am Ende von `JustMoreParts.init()`) und `dev.client.DevClientBootstrap` (am
Ende von `JustMorePartsClient.init()`) per Reflection, falls vorhanden. Der Mod-Code kennt keine Klasse aus `devtest`.

---

## 3. Pakete (`common`)

| Paket | Zweck | Seite | Besitzer |
|---|---|---|---|
| `de.lardtom.justmoreparts` | `JustMoreParts` (Mod-Id, Logger, `init()`, `id()`) | beide | Architekt |
| `client` | `JustMorePartsClient` (Client-Einstieg), `SchoolLinks` (Flugschul-Szenen je Teil-Art, §4) | Client | Architekt |
| `core`, `core.client` | `Hook`, `JmpEvents` (Server gestartet/Tick/gestoppt), `JmpClientEvents` (Client-Tick), `DevHooks`, `JmpTab` (eigener Kreativ-Tab „Propwash: Just More Parts“: Symbol, Reihenfolge), `JmpLoot` (Truhen-Einspeisung aller Module je Vanilla-Tabelle, für Fabric) | beide/Client | Architekt |
| `platform` | `Platform`-Dienst (ServiceLoader): Loader-Name, Mod geladen/Version, Pfade, Mod-Ressourcen lesen, sortierte Kreativ-Tabs (NeoForge, für den Selbsttest) | beide | Architekt |
| `motor` | `MotorModule` (Java-Anteile der Motoren, z. B. Seltenheit) | beide | 1 |
| `prop` | `PropModule` | beide | 2 |
| `power` | `PowerModule` (Akkus, Stacks, Zellchemie) | beide | 3 |
| `frame`, `frame.client` | `FrameModule` (Rahmen, X-Class-Seltenheit), `FrameClient` (Flugschul-Szene „Rahmenformen“) | beide/Client | 4 |
| `video` | `VideoModule` | beide | 4 |
| `preset` | `PresetModule` (Presets, Kreativ-Einträge) | beide | 4 |
| `share`, `share.client` | Sharecode-Codec PW2 und PW1, Import-Logik, Werkbank-Knopf, Kind-Fenster, Konfigurator-Tab | beide/Client | 5 |
| `command` | `JmpCommands` (`/jmp …`, von beiden Loadern verdrahtet) | Server | 5 |
| `export` (nur `devtest`) | Web-Export | Server | 5 |

`fabric`/`neoforge`: `JustMorePartsFabric`/`JustMorePartsNeoForge` (gemeinsame Init, Server-Events, Befehle; Truhen-Beute
ohne Loader-Code: `core.JmpLoot.register()` meldet in `JustMoreParts.init()` jede Einspeisung der Module über
`PropwashApi.loot().inject(ziel, tabelle)` an, Propwash hängt sie auf beiden Loadern ein — Parts-API 1.4, W8),
`…Client` (Client-Init, Client-Tick), `platform.FabricPlatform`/`NeoForgePlatform` — fertig, Besitzer Architekt.

---

## 4. Lebenszyklus

`JustMoreParts.init()` (Fabric `onInitialize`, NeoForge `@Mod`-Konstruktor; beide Seiten, offene Phase der Parts-API):

1. `PropwashApi.requireVersion("justmoreparts", "1.4")` (1.2 wegen `PropwashApi.extensionTab` und der Flugzeit-Kennzahlen in `BuildStats`, 1.3 wegen der Flugschul-Verknüpfungen, 1.4 wegen des Kamerawinkels im Sharecode und des Uptilt-Bereichs im Web-Export)
2. `PropwashApi.parts().loadBundled("justmoreparts")` — **alle Teile als JSON** aus
   `data/justmoreparts/propwash_parts/<kategorie>/` (Propwash lädt `chemistry` zuerst).
3. `MotorModule.init()`, `PropModule.init()`, `PowerModule.init()`, `FrameModule.init()`, `VideoModule.init()` — nur, was
   JSON nicht kann (Events; die Seltenheit steht seit Parts-API 1.4 als `"rarity"` in den Teile-Dateien).
4. `PropwashApi.partItems().registerItems("justmoreparts")` — ein Propwash-Teil-Item je übrigem Teil;
   `JustMoreParts.items()` = einzeln angemeldete + übrige (143).
5. `PresetModule.init()` — Presets (`builds().registerPreset`) und Kreativ-Einträge.
6. `JmpTab.init()` — eigener Kreativ-Tab über `PropwashApi.extensionTab("justmoreparts")` (API 1.1): Titel „Propwash:
   Just More Parts“ (Propwash setzt „Propwash: “ vor den JMP-Schlüssel `itemGroup.justmoreparts`), Symbol `deadcat_5`,
   Reihenfolge Presets → Rahmen → Motoren → Props → Stacks → Akkus → Video. Propwash legt den Tab
   `propwash:main-justmoreparts` direkt hinter seinem eigenen an und stellt alle JMP-Teil-Items und -Presets dort statt
   in den Propwash-Tab.
7. `ShareModule.init()` — Events (`PropwashEvents.BUILD_IMPORTED` für Statistik/Log), nichts Clientseitiges.
8. `DevHooks.common()`.

`JustMorePartsClient.init()` (Fabric `onInitializeClient`, NeoForge `@Mod(dist = CLIENT)`): `FrameClient.init()`
(Flugschul-Szene), `ShareClient.init()` (Werkbank-Erweiterung, Konfigurator-Tab, Flugschul-Szene), `SchoolLinks.init()`,
`DevHooks.client()`.

**Flugschul-Taste:** `client.SchoolLinks` ist die einzige Zuordnung von JMP-Teilen zu Flugschul-Szenen und meldet sie
über `PropwashClientApi.linkFlightSchoolParts(szene, "justmoreparts", arten…)` (API 1.3) an. Hält der Spieler die
Flugschul-Taste (M) über einem JMP-Item, bietet Propwash genau diese Szenen an:

| Teil-Art | Szenen (Reihenfolge der Tabelle; Propwash zeigt sie in der Reihenfolge der Übersicht) |
|---|---|
| Rahmen | `propwash:drone_build`, `justmoreparts:frames`, `propwash:survival` |
| Stack | `propwash:drone_build`, `propwash:configurator`, `propwash:survival` |
| Motor | `propwash:drone_build`, `propwash:repair`, `propwash:survival` |
| Prop | `propwash:drone_build`, `propwash:repair`, `propwash:survival` |
| Video | `propwash:drone_build`, `propwash:video`, `propwash:survival` |
| Akku | `propwash:drone_build`, `propwash:battery`, `propwash:survival` |
| Presets (`propwash:drone`) | zusätzlich `justmoreparts:sharing` (über die Item-Liste der Szene) |

Werkbank und Überlebensmodus bekäme jedes Teil auch ohne Verknüpfung (`anyPart()`), Akku, Video, Stack und Prop die
Propwash-Bauteil-Szene ebenfalls; JMP meldet trotzdem die ganze Tabelle an, damit sie die Erwartung des Selbsttests
ist. Prüfungen: JUnit `client.SchoolLinksTest` (Tabelle, Szenen-Ids) und Selbsttest „jmp: Flugschul-Taste über jedem
JMP-Item und jedem JMP-Preset öffnet passende Szenen“ (`dev.client.SchoolSelfTests`: jedes JMP-Item im Register hat
mindestens eine Szene, jedes Teil alle Szenen seiner Art, alle 22 Rahmen die Rahmenformen-Szene, jedes Preset „Builds
teilen“, `openFlightSchool` öffnet für `deadcat_5` ein Fenster im Propwash-Theme).

Befehle: beide Loader rufen `JmpCommands.register(dispatcher, buildContext, selection)`.
Server-Events: `JmpEvents.SERVER_STARTED`, `SERVER_TICK_END`, `SERVER_STOPPED`; Client: `JmpClientEvents.TICK_END`.

Keiner der Implementierer ändert `JustMoreParts`, `JustMorePartsClient`, die Loader-Klassen oder die Harness — die
Einstiege der Module sind fertig verdrahtet und leer.

---

## 5. Daten und Ressourcen

| Pfad | Inhalt | Erzeugt von |
|---|---|---|
| `data/justmoreparts/propwash_parts/<kategorie>/<id>.json` | Teile (Schema Propwash `parts-api-data.md`, `"format": 1`) | von Hand (aus `catalog-draft`) |
| `assets/justmoreparts/items/<id>.json` | Inventar-Symbol, Sonder-Renderer `propwash:part` | `parts_sync.py --write` (eigene Modelle dürfen die Datei ersetzen) |
| `data/propwash/tags/item/parts/<kategorie>.json` | Tags `#propwash:parts/<kategorie>` mit `"replace": false` | `parts_sync.py --write` |
| `data/justmoreparts/recipe/<id>.json` | Vanilla-Rezepte, Gruppe `propwash_<kategorie>`, Reiter `equipment` | von Hand |
| `data/justmoreparts/advancement/recipes/<kategorie>/<id>.json` | Freischalten der Rezepte | `parts_sync.py --write` |
| `common/src/lang/<modul>/{de_de,en_us}.json` | Namen (`part.justmoreparts.<id>`), Hilfen (`justmoreparts.help.<id>.summary/what.N/use.N`), Kurznamen, Halterungen, Chemie, Presets, UI | von Hand; `:common:mergeLang` erzeugt `assets/justmoreparts/lang/*.json` (eingecheckt, nie von Hand ändern) |
| `assets/justmoreparts/drone_layout.json`, `drone_paint.json` | optische Feinlage / Lack-Varianten eigener Modelle | Implementierer 4 (andere melden Einträge an ihn) |
| `assets/justmoreparts/models/part/<kategorie>/…`, `items/drone_render/<kategorie>/…`, Texturen | optionale eigene Modelle | jeweiliger Besitzer der Kategorie |

Ohne eigene Modelle zeichnet Propwash jedes Teil prozedural aus der Definition — 0.1 kommt damit aus.

---

## 6. Oberflächen

Jedes Fenster benutzt das Propwash-Theme: `Screen` + Marker `ui.client.theme.ThemedScreen`, Zeichnen mit `ThemeDraw`
(`backdrop`, `window`, `brandedHeader`, `footerBar`, `previewStage`, `sectionTitle` …), Text mit `ThemeText`, Farben aus
`Palette`, Maße aus `Metrics`, Knöpfe `ui.client.widget.FlatButton`, Listen `ScrollPanel`, Regler
`ui.client.config.StepSlider`, Konfigurator-Tabs über `ui.client.config.ConfigTab`. Diese Klassen sind von Propwash für
Addons freigegeben (Parts-API §3.2). Vorbild: `dev.example.client.ExampleImportScreen` und `ExampleConfigTab` im
Propwash-Quell-Set `devtest` (Branch `integration`). Keine Vanilla-Knöpfe, keine eigenen Farben außer aus `Palette`.

---

## 7. Tests

| Ebene | Wo | Was |
|---|---|---|
| JUnit | `common/src/test/java/**` | Prüfstand (`bench.PartFiles`, `bench.Bench`), je Implementierer eigene Bench-Tests der Zielkorridore, Sharecode-Codec mit Testvektoren; `tools/fastcheck.sh test` |
| Selbsttest | `common/src/devtest/java/**/client/*SelfTests.java` | im Spiel: Items, Tooltips, Werkbank-Bau, Presets, Flug kurz scharf, Sharecode-Rundreise, Fenster, Stil (`dev.client.StyleSelfTests`: jedes Teil-Item mit eigenem Modell, Größe je Ansicht innerhalb ±10 % der Propwash-Teile gleicher Art, in der Hand geteilt durch den Handfaktor (L / 180 mm)^0,6 (0,25–1, L = längste Kante des Drohnen-Modells; Akkus in der ersten Person ohne Faktor, wie in Propwashs `kit.py`), im Inventar Breite/Höhe, sonst der ganze gedrehte Quader mit Tiefe wie Propwashs `ItemSizeRule`); `runClientSelftest` beider Loader |
| Vergleichsbilder | `dev.client.CompareShots` (nur mit `-PjmpScreenshots`) | Propwash-Teil neben JMP-Teil in Schnellleiste, beiden Händen, am Boden und im Rahmen, erste Person (beide Seiten) und dritte Person, nach `docs/screenshots/compare/` |
| Rauchtests | Harness | Start, Welt, Server 100 Ticks |
| Web-Export | `runWebExport` | Export vollständig, Testvektoren dekodieren und analysieren |

Der Architekt hat mit dem Prüfstand alle Presets gemessen (`parts-catalog.md` §8.1); die Werte sind die Basis der
Abnahme.

---

## 8. Stabilität

- Teil-Ids, Preset-Ids, Halterungs- und Chemie-Ids sind ab 0.1.0 eingefroren (Sharecodes, Speicherstände, Handshake).
  Umbenennen nur mit Eintrag in der Sharecode-Alias-Tabelle.
- JMP hängt nur an stabiler Propwash-API 1.x; neue Nebenversionen der API werden mit `PropwashApi.isCompatible("1.x")`
  abgefragt, bevor JMP sie benutzt.
- Enums der API (`FrameRole`, `FrameShape`, `PropMaterial`, `TuneGroup`, `ImportStatus` …) immer mit `default` im `switch`.

---

## 9. API-Wünsche an Propwash (für eine spätere Parts-API 1.1)

JMP kommt ohne sie aus; die Werte stehen bis dahin im Tooltip und werden über vorhandene Felder angenähert
(`parts-catalog.md`). Jeder Wunsch ist rein ergänzend (Nebenversion), mit neutralem Standard für alle eingebauten Teile.

| Nr. | Wunsch | Signatur | JSON | Standard / Wirkung |
|---|---|---|---|---|
| W1 | Rahmen-Steifigkeit und Armdicke | `FrameDefinition.Builder armThickness(double mm)`, `FrameDefinition.Builder stiffness(double factor)`; Record-Komponenten `double armThicknessMm`, `double stiffness` mit Zugriff `armThicknessMm()`, `stiffness()` | `arm_thickness_mm`, `stiffness` | Armdicke leer = 0,02·Radstand (Anzeige, Crash-Dosis der Arme ∝ 1/Dicke²); Steifigkeit 1,0: Rahmenresonanz → Gyro-Rauschen und D-Term-Last `÷ √stiffness` |
| W2 | Props für Ducts | `PropDefinition.Builder ductGain(double factor)`; Komponente `double ductGain` | `duct_gain` | 1,0: multipliziert den Duct-Schubgewinn des Rahmens (0,8–1,3), in offenen Rahmen ohne Wirkung |
| W3 | Stecker mit Stromgrenze | `BatteryDefinition.Builder connectorLimit(double amps)`; Komponente `OptionalDouble connectorLimitAmps`; neue Warnung `BuildCheck`-Schlüssel `connector_undersized` | `connector_limit_a` | leer = unbegrenzt; Warnung, wenn 4 × Motor-Spitzenstrom > Grenze; Flugmodell: Kontaktwiderstand 0,5 mΩ je 10 A Unterdimensionierung |
| W4 | Klang je Prop | `PropDefinition.Builder noise(double factor)`; Komponente `double noise` | `noise` | 1,0: Lautstärke/Obertöne im Motorton (0,5–2,0) |
| W5 | Motor-Leerlaufstrom und Rotorträgheit | `MotorDefinition.Builder noLoadCurrent(double amps)`, `MotorDefinition.Builder rotorInertia(double kgM2)`; Komponenten `OptionalDouble noLoadCurrentAmps`, `OptionalDouble rotorInertiaKgM2` | `no_load_current_a`, `rotor_inertia_kg_m2` | leer = 3 % von `max_current_a` bzw. aus den Statormaßen |
| W6 | JSON-Codec öffentlich | `PartRegistry#toJson(PartDefinition definition): com.google.gson.JsonObject`, `PartRegistry#toJson(CellChemistry chemistry): JsonObject`, `PartRegistry#fromJson(PartKind kind, Identifier id, JsonObject json): PartDefinition` (wirft `IllegalArgumentException` mit Feldname) | — | Web-Export und Prüfstand benutzen dann Propwashs eigenen Leser/Schreiber statt `bench.PartFiles` und des Export-Kodierers |
| W7 | Sprachtexte ohne Client | `PropwashApi.text(String languageCode, String key): Optional<String>` (liest `assets/*/lang/<code>.json` aller Mods, auch auf dem Server) | — | Web-Export liest Namen/Hilfen ohne eigenen Ressourcen-Leser |


### 9.1 Wünsche aus der Umsetzung (Implementierer-Berichte, 2026-09-30)

Neu nummeriert (die Berichte benutzten sich überschneidende Nummern; Herkunft in Klammern). Alle rein ergänzend.

| Nr. | Wunsch | Signatur | Herkunft | JMP 1.0.0 ohne den Wunsch |
|---|---|---|---|---|
| W8 | Truhen-Beute für Addons | `LootInjections PropwashApi.loot()` mit `void inject(ResourceKey<LootTable> vanillaTarget, ResourceKey<LootTable> addonTable)`, offene Phase, Propwash verdrahtet beide Loader | MOTORS A1, PROPS W8, POWER P3, FRAMES W12 | `core.JmpLoot` + Fabric `LootTableEvents.MODIFY`, NeoForge-Loot-Modifier als Daten |
| W9 | Builder für Motor-Wärme | `MotorThermal.builder().heatCapacity(double).cooling(double).coolingPerRadS(double).limits(double derate, double max).wearAtMax(double).build()` | MOTORS A2 | JSON-Felder der Teile-Dateien |
| W10 | Propwash-Rauschen messbar | `BuildStats#propwashNoise(): double` (Faktor gegenüber Steifigkeit 1,0) | PROPS W9 | Prüfstand belegt über die Prop-Definition (Steifigkeit) |
| W11 | Seltenheit ohne Java | JSON `"rarity"` bzw. `PartBasics#withRarity(Rarity)` | PROPS W10 | `core.JmpItems.register(id, p -> p.rarity(…))` |
| W12 | Steifigkeit im Tooltip | Zeile `propwash.parts.tooltip.stiffness`, wenn die Steifigkeit vom Material-Standard abweicht | PROPS W11 | Shift-Hilfe nennt sie |
| W13 | Durchgebrannter ESC als Ereignis | `PropwashEvents.ESC_BURNED` mit `EscBurned#onEscBurned(ServerPlayer player, ItemStack drone, DroneData data, int cells)`; `BuildAccess#partCondition(ItemStack drone, PartKind kind): OptionalDouble` | POWER P1 | Selbsttest prüft die Warnung `esc_overvoltage` statt des Durchbrennens |
| W14 | Akku mit Teilladung als Stack | `PartItems#stack(Identifier partId, float charge)`; Komponente `propwash:battery_charge` als stabil dokumentiert | POWER P2 | Beute-Akkus sind voll geladen |
| W15 | Zellspannung bei Vollgas | `BuildStats#fullThrottleCellVoltage(): double` | POWER P4 | Brownout-/Sag-Aussagen über Warnungen belegt |
| W16 | Achsen-Autorität | `BuildStats#rollAuthorityRadS2()`, `pitchAuthorityRadS2()`, `yawAuthorityRadS2()` (alle `double`) | FRAMES W8 | Prüfstand schätzt sie (`frame/FrameAuthority`) |
| W17 | Große Modelle | `PartVisual#withModelScale(double scale)`, JSON `visual.model_scale` | FRAMES W11 | seit 1.0.1 über die Vanilla-`transformation` der Render-Definition gelöst (verkleinertes Modell, zurück auf echte Größe skaliert); offen ist nur die Zusicherung, siehe W30 |
| W18 | Videosender und Sichtfeld wirken | `VideoDefinition.vtxPowerMw` → Reichweite der Videoverbindung, `cameraFovDeg` → FPV-Sichtfeld (Verhaltensänderung, abfragbar mit `isCompatible("1.1")`) | FRAMES W13 | Sichtfeld **erfüllt** in Propwash 0.4.0 (API 1.4, §10.18); Sendeleistung weiter nur im Tooltip und Web-Export |
| W19 | Name beim Werkbank-Import | `ImportLayer.NAME` | SHARECODE W8 | Fenster weist darauf hin; Befehl und „Drohne erzeugen“ übernehmen den Namen |
| W20 | Kurzbeschriftung für Werkbank-Knöpfe | `default void WorkbenchContext#addButton(Component label, Component shortLabel, Component tooltip, Runnable onPress)` | SHARECODE W9 | ein einziger Knopf „Sharecode“; die Beschriftung wählt JMP selbst nach gemessenem Platz („Sharecode“ → „Code“ → `</>`), volle Erklärung im Tooltip |
| W21 | Konfigurator-Tab öffnen | `PropwashClientApi.openConfigTab(String tabId, @Nullable Screen parent): boolean` | SHARECODE W10 | Selbsttest benutzt ein eigenes Theme-Fenster; die Bildschirmfotos öffnen den echten Konfigurator über die Taste `key.propwash.configurator` und drücken den Seitenleisten-Eintrag |
| W22 | Drohne erzeugen ohne Befehl | `PropwashClientApi.sendCreateDrone(DroneData data): CompletableFuture<ImportResult>` (Server prüft Kreativ/Operator) | SHARECODE W11 | Tab schickt `/jmp code import` |
| W23 | Fehlende Teile laut Server | `WorkbenchContext#missingParts(DroneData data): List<Identifier>` | SHARECODE W12 | Einkaufsliste zählt Items selbst |
| W24 | Farbstoff-Kosten vorab | `BuildAccess#paintCost(ItemStack drone, PaintSpec target): Map<String, Integer>` | SHARECODE W13 | Kosten erst in der Antwort |
| W25 | OSD-Layout prüfen | `OsdLayoutAccess#validate(OsdLayoutSpec layout): boolean` | SHARECODE W14 | Prüfung beim Übernehmen |
| W27 | Werkbank-Titel bleibt lesbar | Propwash reserviert im Werkbank-Kopf Marke + Titel statt fester 64 px (`DroneWorkbenchScreen`, Verhalten) und meldet den Rest: `int WorkbenchContext#headerSpace()` (Pixel für Addon-Knöpfe inklusive Abständen) | Visueller Review 1.0.0 | JMP rechnet den Platz aus der Kopfbreite nach (`ShareWorkbenchExtension.labelBudget`: 248 px − Marke − Titel − „Lackierung“) und meldet im Selbsttest, wenn der Titel abgeschnitten würde |
| W28 | Symbol-Knöpfe für Werkbank-Erweiterungen | `default void WorkbenchContext#addIconButton(Identifier sprite, Component tooltip, Runnable onPress)` (13 × 13 px, GUI-Sprite aus dem Addon) | Visueller Review 1.0.0 | Textsymbol `</>` als Beschriftung, wenn der Platz nicht für ein Wort reicht |
| W29 | Vorschau im festen Maßstab | `PropwashClientApi.renderPreview(GuiGraphicsExtractor graphics, DroneData drone, int x0, int y0, int x1, int y1, float yawDegrees, float pixelsPerMillimetre)` und `BuildAccess#footprintMm(BuildSpec spec): double` (Durchmesser der Hülle über die Propspitzen) | Visueller Review 1.0.0 | Größenvergleich (`dev.client.ScaleScreen`) gibt jeder Drohne ein Feld, dessen Breite proportional zu Radstand + Prop-Durchmesser ist; die automatische Einpassung ergibt so annähernd einen gemeinsamen Maßstab |

**Bewertung bei der Integration 1.0.0:** Keiner der Wünsche W1–W25 und W27–W29 ist wesentlich — für jeden läuft in JMP 1.0.0 ein
Weg über die öffentliche API 1.0 (letzte Spalte), alle Selbsttests sind auf beiden Loadern grün, und jede Shift-Hilfe
behauptet nur, was der Prüfstand mit Propwashs eigener Analyse belegt. Propwash bleibt deshalb bei **0.4.0**; JMP hängt
an `propwash >= 0.4.0`. Empfohlene Reihenfolge für eine Parts-API 1.1: W8 (Beute, spart jedem Addon Loader-Code),
W1 (Rahmen-Steifigkeit und Armdicke, macht die Rahmen-Unterschiede physikalisch), W2/W4 (Duct-Props, Prop-Klang),
W19/W20 (Werkbank-Import mit Name, Kurzbeschriftung), W21/W22 (Konfigurator-Tab öffnen, Drohne erzeugen).

### 9.2 Aus dem Realismus-Review (2026-09-30)

JMP hat alle Befunde, die in den eigenen Teile-Dateien lösbar sind, selbst behoben (`parts-catalog.md` §8.4). Zwei Punkte
liegen in Propwash:

| Nr. | Wunsch | Signatur | JSON | Standard / Wirkung | JMP 1.0.0 ohne den Wunsch |
|---|---|---|---|---|---|
| B1 | Basis-Kalibrierung der Mikroklassen (keine API-Änderung; **erledigt** in Propwash 0.4.0, §9.4) | — | — | Mikroklassen schweben in der Analyse mit etwa 4,4 g/W, real sind es 2–2,5 g/W; Flugzeiten bei 65–85 mm deshalb 2- bis 3-mal, bei 2,5–3″ etwa 1,5-mal zu lang (Propwash nennt das selbst in `flight-model-characterization.md`). Ziele: 65 mm 1S 300 mAh etwa 5–6 min Schweben und 4 min Reise, 75 mm 2S 300 mAh etwa 5 / 3,5 min. Stellschrauben: Reynolds- bzw. Kleinmotor-Verlust bei D < 80 mm oder höherer Leerlaufstrom I₀ (vgl. W5) | JMP korrigiert das nicht allein, sonst wichen seine 31- und 40-mm-Props von den eingebauten ab; die Whoop-Korridore (1.7) bleiben an Propwash ausgerichtet |
| W26 | Leistungsaufnahme der Videosysteme | `VideoDefinition.Builder powerDraw(double watts)`; Record-Komponente `OptionalDouble powerDrawWatts` mit Zugriff `powerDrawWatts()` | `power_w` | leer = heutiges Verhalten (0,3 W/g · Masse); digitale Whoop-Systeme ziehen real 3–4,5 W (heute etwa 2 W) und kosten dann ehrlich Flugzeit | Leistungsaufnahme folgt der Masse; nicht zwingend nötig |

Hinweis 12S: `m4214_330` ist laut Datenblatt bis 12S spezifiziert (`cells` 8–12), aber weder JMP noch Propwash haben
10–12S-Akkus oder -ESCs. Die Shift-Hilfe nennt deshalb nur 8S; 12S-Akkus und ein 12S-Stack sind ein möglicher Ausbau.

### 9.3 Aus dem Stil-System (1.0.1)

Die Wünsche W30–W32 stehen mit genauer Form in `docs/design/asset-style.md` §7: **W30** `PartVisual withRenderScale(double
scale)` (JSON `visual.render_scale`) für echte Drohnen-Modelle der X-Class-Rahmen, **W31** Kategorie-Basismodelle
`propwash:item/part_base/<kategorie>` für Addon-Teile ohne eigenes Item-Modell, **W32** der Stil-Baukasten als Artefakt
`de.lardtom:propwash-assetkit`. Keiner ist für 1.0.1 wesentlich: JMP liefert für jedes Teil ein eigenes Item-Modell,
die X-Class-Rahmen haben seit der Stil-Review ein verkleinertes Drohnen-Modell, das die Vanilla-`transformation` der
Render-Definition auf echte Größe zurückskaliert (Propwash 0.4.0 wendet sie über `ItemModel.update` an; W30 bleibt als
Wunsch nach Zusicherung und ungekappter Hülle), und der Baukasten liegt als unveränderte, geprüfte Kopie in
`tools/assets/propwash_style/`.

### 9.4 Stand mit Parts-API 1.4 (Propwash 0.4.0, 2026-10-02)

Propwash 0.4.0 bringt Parts-API **1.4.0**; JMP verlangt sie (`REQUIRED_API` 1.4). Status jedes Wunsches:

| Nr. | Stand | In JMP benutzt |
|---|---|---|
| W1 | offen: Armdicke und Rahmen-Steifigkeit wirken auf Flugmodell und Crash-Dosis; die Flug-Engine bleibt nach Vorgabe des Besitzers unverändert | Werte weiter in Tooltip und Teile-Katalog |
| W2, W3, W5 | offen aus demselben Grund (Duct-Schub, Stecker-Kontaktwiderstand, Leerlaufstrom/Rotorträgheit ändern die Physik) | — |
| W4 | offen: Klang je Prop wäre ein neues Audio-Merkmal | — |
| W6 | umgesetzt (`PartRegistry#toJson/fromJson/chemistryFromJson`) | Prüfstand `bench.PartFiles` liest mit Propwashs Codec statt eines eigenen Mappers (143 JUnit-Tests unverändert grün) |
| W7 | umgesetzt (`PropwashApi.text`) | nicht nötig: der Web-Export sammelt die benutzten Texte je Sprache selbst (`export.LangTable`) |
| W8 | umgesetzt (`PropwashApi.loot()`) | `core.JmpLoot` meldet alle Einspeisungen an, Loader-Code und NeoForge-Modifier entfallen |
| W9 | umgesetzt (`MotorThermal.builder()`) | JMP beschreibt Motoren in JSON, kein Java-Bedarf |
| W10 | umgesetzt (`BuildStats#propwashNoise`) | `PropBenchTest`: „weich: wackelt im Propwash“ und Glasfaser-Nylon über die Analyse belegt |
| W11 | umgesetzt (`"rarity"` bzw. `PartBasics#withRarity`) | 24 Teile-Dateien tragen `"rarity"`, `core.JmpItems` entfällt; `RarityDataTest` prüft die Regeln |
| W12 | umgesetzt (Tooltip-Zeile „Steifigkeit“) | automatisch bei allen Props mit eigener Steifigkeit |
| W13 | umgesetzt (`ESC_BURNED`, `partCondition`) | nicht benutzt: JMP reagiert nicht selbst auf ein durchgebranntes ESC; der Selbsttest prüft die Warnung `esc_overvoltage` |
| W14 | umgesetzt (`PartItems#stack(id, charge)`) | nicht benutzt: Beute-Akkus bleiben voll geladen wie die eingebauten |
| W15 | umgesetzt (`BuildStats#fullThrottleCellVoltage`) | nicht nötig: Sag-Aussagen hängen an `heavy_sag`, das Propwash aus derselben Spannung ableitet |
| W16 | umgesetzt (Roll-/Nick-/Gier-Autorität in `BuildStats`) | `FrameBenchTest` statt der eigenen Schätzung `frame/FrameAuthority` (entfällt); Richtungen unverändert, Teile-Katalog §8 |
| W17, W30 | umgesetzt: `transformation` der Render-Definition ist zugesichert, Hülle bis ±8 Modell-Blöcke | X-Class-Rahmen wie seit 1.0.1 |
| W18 | Sichtfeld umgesetzt (`cameraFovDeg` wirkt im FPV-Bild, API 1.4, §10.20); Sendeleistung weiter nur Anzeige | JMP-Videomodule tragen `camera_fov_deg`, Sharecode Revision 1.2 mit Kamerawinkel; Sendeleistung nur im Tooltip und Web-Export |
| W19 | umgesetzt (`ImportLayer.NAME`) | Sharecode-Fenster übernimmt den Namen mit dem Bauplan |
| W20, W27, W28 | umgesetzt (`addButton` mit Kurzform, `headerSpace`, `addIconButton`) | Sharecode-Knopf „Sharecode“/„Code“, sonst Symbol `</>` (13 × 13); `labelBudget` entfällt |
| W21 | umgesetzt (`openConfigTab`) | Bildschirmfotos öffnen den Sharecode-Tab direkt |
| W22 | umgesetzt (`sendCreateDrone`) | „Drohne erzeugen“ im Konfigurator-Tab statt `/jmp code import` |
| W23 | umgesetzt (`WorkbenchContext#missingParts`) | Einkaufsliste im Sharecode-Fenster zählt nach den Regeln des Servers |
| W24 | umgesetzt (`BuildAccess#paintCost`) | Konfigurator-Tab zeigt die Lackkosten der gehaltenen Drohne vorab |
| W25 | umgesetzt (`OsdLayoutAccess#validate`) | OSD-Layout wird vor dem Übernehmen geprüft |
| W26 | seit API 1.2 umgesetzt (`power_w`) | Videosysteme |
| W29 | umgesetzt (`renderPreview(…, pixelsPerMillimetre)`, `footprintMm`) | Größenvergleich `dev.client.ScaleScreen` |
| neu | `PropwashClientApi.forceShiftHelp` (Shift-Hilfe ohne Taste) | Galeriebild `jmp_tooltip_shift.png` (`dev.client.GalleryShots`) |
| W31 | umgesetzt (`propwash:item/part_base/<kategorie>`) | nicht nötig: jedes JMP-Teil hat ein eigenes Item-Modell |
| W32 | offen: Stil-Baukasten als eigenes Maven-Artefakt ist ein neues Werkzeug-Paket; JMP behält die geprüfte Kopie in `tools/assets/propwash_style/` | — |
| B1 | erledigt durch die Realismus-Kalibrierung von Propwash 0.4.0 (`realism/calibration.md`, Energiemodell statt Flugphysik): die Mikroklassen liegen in den Referenzfenstern aus `realism/builds.md` (Owner-Fall 65 mm 1S digital Schweben 3:00–3:20 min), alle Presets in ihren Klassenfenstern (Harness 9/9) | — |

---

## 10. Umsetzungsplan — fünf parallele Implementierer

### 10.1 Ausgangslage
Alle zweigen von `main` ab (Commit „Gerüst …“): fertiges, ladendes Addon (beide Loader, Selbsttest „JMP geladen und
Propwash-API verfügbar“, Server-Rauchtest), leere Modul-Einstiege, Prüfstand, Werkzeuge, alle Design-Dokumente,
JSON-Entwürfe aller Teile unter `docs/design/catalog-draft/`.

Worktree je Implementierer:

```
git -C <repo> worktree add <worktrees>/motors    -b feature/motors    main
git -C <repo> worktree add <worktrees>/props     -b feature/props     main
git -C <repo> worktree add <worktrees>/power     -b feature/power     main
git -C <repo> worktree add <worktrees>/frames    -b feature/frames    main
git -C <repo> worktree add <worktrees>/sharecode -b feature/sharecode main
```

(`<repo>` = Wurzel dieses Repositorys; `<worktrees>` = lokaler Ordner für die Worktrees, nicht im Repository.)

### 10.2 Besitz (nur der Besitzer ändert eine Datei; Lesen und Aufrufen ist frei)

Pfade relativ zu `common/src/` bzw. `common/src/main/resources/`; `<id>` = Teil-Id der eigenen Kategorie
(Motoren `m…`, Props `p…`, Akkus `b…`, Stacks `aio_…`/`stack_…`, Video `analog_…`/`digital_…`, Rahmen alle übrigen).

| Implementierer | Branch | Besitz |
|---|---|---|
| **1 MOTORS** | `feature/motors` | Java `main/java/**/motor/**`, `devtest/java/**/motor/**`, `test/java/**/motor/**`; Daten `data/justmoreparts/propwash_parts/motor/**`, `data/justmoreparts/recipe/m*.json`, `data/justmoreparts/advancement/recipes/motor/**`, `data/propwash/tags/item/parts/motor.json`, `assets/justmoreparts/items/m*.json`, eigene Modelle `…/motor/**`; Sprache `lang/motor/**` (inkl. Halterungen `justmoreparts.mount.motor.m31|m42`); Doku `parts-catalog.md` §3 |
| **2 PROPS** | `feature/props` | `main/java/**/prop/**`, `devtest/java/**/prop/**`, `test/java/**/prop/**`; `propwash_parts/prop/**`, `recipe/p*.json`, `advancement/recipes/prop/**`, `tags/item/parts/prop.json`, `items/p*.json`, Modelle `…/prop/**`; `lang/prop/**`; `parts-catalog.md` §4 |
| **3 POWER** | `feature/power` | `main/java/**/power/**`, `devtest/java/**/power/**`, `test/java/**/power/**`; `propwash_parts/battery/**`, `propwash_parts/stack/**`, `propwash_parts/chemistry/**`, `recipe/b*.json`, `recipe/aio_*.json`, `recipe/stack_*.json`, `advancement/recipes/battery/**`, `advancement/recipes/stack/**`, `tags/item/parts/battery.json`, `tags/item/parts/stack.json`, `items/b*.json`, `items/aio_*.json`, `items/stack_*.json`, Modelle `…/battery/**`, `…/stack/**`; `lang/power/**` (inkl. `justmoreparts.chemistry.lihv_ss`); `parts-catalog.md` §5 |
| **4 FRAMES + VIDEO** | `feature/frames` | `main/java/**/frame/**`, `**/video/**`, `**/preset/**` (inkl. `frame/client/FrameClient`), `devtest/java/**/frame/**`, `test/java/**/frame/**`; `propwash_parts/frame/**`, `propwash_parts/video/**`, Rezepte/Fortschritte/Tags/Items/Modelle der Rahmen und Videosysteme, `assets/justmoreparts/drone_layout.json`, `drone_paint.json`; `lang/frame/**`, `lang/video/**`, `lang/preset/**`; `parts-catalog.md` §6–8 |
| **5 SHARECODE + UI + BEFEHL + WEB-EXPORT** | `feature/sharecode` | `main/java/**/share/**`, `main/java/**/command/**`, `devtest/java/**/share/**`, `devtest/java/**/export/**`, `devtest/java/**/dev/WebExportRun.java`, `test/java/**/share/**`, `test/java/**/export/**`; `lang/share/**`; `docs/design/sharecode.md`, `docs/design/web-export.md`, `tools/sharecode_ref.py` |
| **Architekt** (nur auf Zuruf) | `main` | alles Übrige: Build, Einstiege, `core`, `platform`, Loader-Module, Harness (`dev/**` außer `WebExportRun`), `bench/**`, `tools/fastcheck.sh`, `tools/parts_sync.py`, `tools/strip_comments.py`, `lang/core/**`, `docs/ARCHITECTURE.md`, `docs/CONVENTIONS.md`, `docs/design/catalog-draft/**` |

Gemeinsam (konfliktarm): `changelogs/changelog-<datum>.md` (jeder hängt nur eigene Zeilen an; `merge=union`),
generierte Sprachdateien `assets/justmoreparts/lang/*.json` (bei Konflikt nie von Hand lösen: `:common:mergeLang` bzw.
`fastcheck` neu laufen lassen und die Ausgabe nehmen), `README.md` (nur der Integrator).

Braucht ein Implementierer eine Änderung in fremdem Besitz (z. B. ein Selbsttest einer anderen Kategorie, ein Eintrag in
`drone_paint.json`, eine Zeile im Harness), schreibt er einen **Architektur-Wunsch** in seinen Abschlussbericht und
ändert die Datei nicht.

### 10.3 Aufgaben und Abnahme

**Für alle Teile-Implementierer (1–4):**
1. Entwürfe der eigenen Kategorien aus `docs/design/catalog-draft/` nach `resources/data/justmoreparts/propwash_parts/`
   kopieren; Akzentfarben (`visual.accent`) ergänzen; Werte nur innerhalb der Regeln aus `parts-catalog.md` §1 ändern.
2. Namen und Hilfen de/en für jedes Teil (Aufbau `parts-catalog.md` §2.1: Kennzahlen, `+`/`−`-Zeilen, Einsatz), deutsch mit
   Umlauten und ß; Rahmen zusätzlich `justmoreparts.frame_short.<id>`.
3. Rezepte (eindeutig, auch gegen Propwash), danach `python3 tools/parts_sync.py --write` und `--check` ohne Probleme
   für die eigenen Teile.
4. Bench-Test im eigenen Paket (`<paket>/…BenchTest`): jedes eigene Teil in mindestens einem Referenzaufbau, Zielkorridore
   (`parts-catalog.md` §1.7) und alle behaupteten Pro/Contra-Richtungen als Assertions. Teile anderer Kategorien liefert
   der Prüfstand automatisch aus `docs/design/catalog-draft/<kategorie>/`, solange deren Ordner in `resources` fehlt
   (`bench.PartFiles`); so misst jeder Worktree mit dem vollständigen Katalog.
5. Selbsttest-Schritte in `<paket>/client/<Name>SelfTests.register()` (Präfix `motor: `, `prop: `, `power: `, `frame: `):
   jedes Item existiert, hat Namen und Shift-Hilfe; mindestens ein Aufbau je Größenklasse lässt sich über
   `PropwashApi.builds().createDrone` erzeugen und wieder lesen; `check` ohne Probleme.
6. Changelog-Zeilen (deutsch) in `changelogs/changelog-<datum>.md` unter der passenden Kategorie.

Abnahme 1–4: `tools/fastcheck.sh all` und `tools/fastcheck.sh test` grün, `parts_sync.py --check` ohne Probleme in der
eigenen Kategorie, keine Kommentare (`python3 tools/strip_comments.py common/src fabric/src neoforge/src`), Commit im
eigenen Branch.

**1 MOTORS** — 34 Motoren (`parts-catalog.md` §3), Halterungen `m31`/`m42` (Sprache), Hitzegrenzen; Nachweise: kein
Preset mit `sluggish_motors`/`motor_thermal` durch einen JMP-Motor, 4S-Motoren melden auf 6S `motor_overvoltage`, KV-Reihe
eines Stators zeigt mehr Topspeed und mehr Strom mit steigender KV. Offener Punkt: Kühlung `m2807_1100` für `lr8_6s_lion`.

**2 PROPS** — 38 Propeller (§4) samt Materialien soft/hard/bi (Steifigkeit, Haltbarkeit), Duct-Props; Nachweise:
2-Blatt fliegt länger als 3-Blatt, 4/5-Blatt mehr Schub und mehr Strom, höhere Steigung mehr Topspeed, weiches PC übersteht
Crashs (Haltbarkeit) und rauscht mehr (Steifigkeit < 1).

**3 POWER** — 30 Akkus, Zellchemie `lihv_ss`, 12 Stacks/AIOs (§5); Nachweise: Li-Ion längste Schwebezeit, schwächster
Punch; Semi-Solid leichter als LiPo gleicher Kapazität; 4S-Stacks melden auf 6S `stack_voltage` (und brennen beim
Scharfschalten durch, Selbsttest mit Server-Regel `partWear`); 1S-AIOs mit Brownout 2,9 V.

**4 FRAMES + VIDEO** — 22 Rahmen mit Geometrie, Ducts, Ankern (§7), 7 Videosysteme (§6), 21 Presets (§8), Seltenheit der
X-Class-Items, Flugschul-Szene `justmoreparts:frames` (fünf 5″-Formen nebeneinander, Props im Bild, Rolls vs. Flips);
Nachweise: Deadcat/H-Frame < 1 % Props im Bild, Stretched schneller geradeaus, Squashed mehr Nick-Autorität,
Presets innerhalb der Zielkorridore §1.7 und ohne Probleme (`check`). Presets werden erst bei der Integration vollständig
(Teile der anderen); der Selbsttest der Presets läuft deshalb erst nach allen anderen Schritten.

**5 SHARECODE + UI + BEFEHL + WEB-EXPORT**
1. `share.ShareCodec` nach `docs/design/sharecode.md` (PW1): `encode(ShareContent, Compression)`, `decode(String)` mit
   Status, `unknownParts`, `wrongKind`, `renamed`, `skipped`; `share.PartAliases` (leer). JUnit `share/ShareCodecTest`:
   Testvektoren A–F, Rundreisen, Grenzen, zufällige Byte-Fehler (nie eine Ausnahme).
2. `share.ShareContent` (Bauplan, Lack, Tuning nur `TuneGroup.FLIGHT`, OSD als Version + JSON-Text, Name) mit
   Umrechnung von/nach `DroneData`.
3. Client: Werkbank-Erweiterung „Code“ (`WorkbenchExtension`) mit Kind-Fenster „Sharecode“ (Theme): Export der
   Werkbank-Drohne (`WorkbenchContext#result`) mit Ebenen-Häkchen (Lack an, Tuning an, OSD aus, Name an), Kopieren in die
   Zwischenablage; Import-Feld (Einfügen, Länge bis 4000), Dekodieren beim Tippen, Vorschau (`renderPreview`), Teile-Check
   (`check`, unbekannte Teile, falsche Kategorie), Analyse (`analyze`), Ebenenwahl, Übernehmen (`importIntoWorkbench`),
   Ergebnisanzeige (`ImportResult`, fehlende Teile/Farbstoffe, `BUSY` → Knopf 1 s gesperrt). Konfigurator-Tab
   `justmoreparts:sharecode` (`order` 1000, `Storage.MIXED`): Export/Import für die **gehaltene** Drohne (Tuning, Lack)
   und das OSD-Layout.
4. Befehl `/jmp code export|import|info` (Server) nach `sharecode.md` §9.
5. Web-Export (`devtest`, Run `runWebExport`) nach `docs/design/web-export.md` inklusive Testvektoren mit Analysewerten.
6. Selbsttest `share: …`: Rundreise Werkbank → Code → Import in die Werkbank (Kreativ), Tuning-/Lack-Import auf die
   gehaltene Drohne, OSD-Rundreise, Fenster öffnen/schließen, Befehl `info`.

Abnahme 5: `fastcheck all` + `test` grün, alle Testvektoren, keine Kommentare, Theme in jedem Fenster, alle Texte in
`lang/share`.

### 10.4 Integration
Reihenfolge: 3 POWER → 1 MOTORS → 2 PROPS → 4 FRAMES + VIDEO → 5 SHARECODE (Presets brauchen alle Teile; der Web-Export
zeigt alles). Danach: `docs/design/catalog-draft/` löschen, `python3 tools/parts_sync.py --check` ohne Probleme,
`./gradlew build`, `runClientSelftest`, `runClientSmoke`, `runServerSmoke`, `runWebExport` auf beiden Loadern, Bench-Tests
aller Kategorien grün, README und Tages-Changelog ergänzen, Version 1.0.0 (erledigt, §10.6).

### 10.5 Regeln für alle
- Kein Gradle-Lauf in den Worktrees nötig: `tools/fastcheck.sh all` und `tools/fastcheck.sh test`; Selbsttests trotzdem
  schreiben (laufen bei der Integration).
- Nur die öffentliche Propwash-API und die freigegebenen Theme-/Widget-Klassen; fehlt etwas: API-Wunsch im Bericht.
- Minecraft-, Fabric- und NeoForge-APIs nur aus Propwashs `.ref/` bzw. `docs/api/*.md` (26.3).
- Keine Kommentare im Code, Texte nur in Sprachdateien (de_de mit Umlauten und ß, en_us), Theme für jedes Fenster,
  Test-Instanzen stumm (`prepareRuns`).
- Deutsche Commit-Nachrichten, Autor lardtom (Repo-Einstellung), nie pushen, nie hochladen, keine echten Namen, E-Mails
  oder lokalen Pfade in Code, Assets, Metadaten und Doku.

### 10.6 Integration 1.0.0 (2026-09-30)
- Zusammengeführt in der Reihenfolge von 10.4: `feature/power`, `feature/motors`, `feature/props`, `feature/frames`,
  `feature/sharecode`. Konflikte nur in den erzeugten Sprachdateien (mit `:common:mergeLang` neu erzeugt) und im
  Tages-Changelog (neu gegliedert).
- Architektur-Wünsche der Implementierer umgesetzt: Fabric-Beute über `core.JmpLoot`; Lack-Einträge aller Kategorien in
  `drone_paint.json` (`prop_assets.py --merge-paint`, `power_assets.py check --paint-into`, Motor-Fragment;
  `jmp_models.py` pflegt nur noch Rahmen/Video); `drone_layout.json` mit `motor_seat` der Motoren; Preset-Selbsttest nach
  den Sharecode-Schritten (`FrameSelfTests.registerPresets()`); Gradle-Task `webExport`; `logs/` in `.gitignore`;
  „Schub/Gewicht“ nicht mehr als Umschreibung gemeldet.
- Fehler, die erst im Spiel sichtbar wurden: Rezept-Freischaltungen (`recipe_unlocked` braucht `recipes`) brachen das
  Laden der Welt ab; Server-Prüfungen der Rahmen-Selbsttests starteten nie; Beute-Selbsttests würfelten mit
  aufeinanderfolgenden Startwerten (korreliert); Item-Zählung ohne die einzeln angemeldeten Items.
- Prüfstand mit den endgültigen Teilen: Shift-Hilfen mit verschobenen Zahlen angepasst (Katalog §8.3), einheitliche
  Korridor-Toleranz ±5 %, `docs/design/catalog-draft/` gelöscht.
- API-Wünsche: gesammelt in §9.1, keiner wesentlich; Propwash unverändert 0.4.0.

### 10.7 Integration der Reviews (2026-09-30)
- Zusammengeführt: `fix/review-realism`, dann `fix/review-visual`. Konflikt nur in `parts-catalog.md` §4 (Beiwerte und
  Shift-Hilfen aus dem Realismus-Review, Namen und Akzentfarben aus dem visuellen Review, zellenweise zusammengeführt)
  und in der Presetlack-Liste (`lr7_8s` und X-Class-Lack, seit 02.10. #1b1d20 wie alle Rahmen). Die Sprachdateien aus `:common:mergeLang` sind
  byte-gleich mit dem Git-Ergebnis.
- Nachgezogen: Modell und Textur des Semi-Solid 6S 3000 an die neue Masse von 312 g (`power_assets.py write`),
  Pro/Contra-Spalten in §4 an die Shift-Hilfen angeglichen, Namensschilder in `jmp_scale` bleiben im Fenster.
- Alle Bildschirmfotos mit `-PjmpScreenshots` neu erzeugt (elf Bilder, davon drei deutsch); Selbsttest Fabric 47 Schritte
  (32 plus 15 Bildschirmfoto-Schritte), NeoForge 32 Schritte, Rauchtests Client und Server beider Loader und Web-Export
  beider Loader grün.

### 10.8 Integration 1.0.1 – Modelle im Propwash-Stil (2026-09-30)
- Anlass: Feedback auf 1.0.0 „Modelle nicht stilidentisch“ und „Akku passt kaum in die Hand“. Befund des Stil-Systems:
  Im Feedback-Bild der Motoren ist der runde Motor der JMP-Motor und der kantige der Propwash-Motor; zu groß in der
  Hand waren alle Teile, weil sie Propwashs Sonder-Renderer `part_base` benutzten (`asset-style.md` §1).
- Zusammengeführt: `fix/style-system`, dann `fix/style-motors`, `fix/style-props`, `fix/style-power`, `fix/style-frames`
  – ohne Konflikte; `drone_paint.json`, `drone_layout.json`, `writer.py`, Doku und Tages-Changelog fügten sich
  automatisch zusammen.
- Danach alle 143 Teile mit dem zusammengeführten Stand einmal neu erzeugt (`style_generate.py --write`, „STIL-PRÜFUNG
  OK“): Texturen, Geometrie und Lack-Einträge byte-gleich mit den Branches, nur 206 Modelle von Motoren, Akkus, Stacks,
  Rahmen und Video trugen noch den Basis-Vermerk „Propwash FPV - generated by tools/assets/parts“ und haben jetzt
  „Propwash: Just More Parts - lardtom - tools/assets/style_generate.py“.
- Neu im Selbsttest: `stil: …` (`StyleSelfTests`, eigenes Item-Modell für alle 143 Teile, Größe je Ansicht innerhalb
  ±10 % der Propwash-Spanne – gemessen lagen alle Teile bei ×0,99–1,00) und die Vergleichsbilder `CompareShots`
  (22 Bilder in `docs/screenshots/compare/`). Die Vergleichsbilder schalten die Reichweite des Spielers vorübergehend
  ab (kein Blockrahmen im Bild); `tutorialStep:none` steht jetzt in den stummen Optionen der Selbsttest-Instanzen.
- Läufe: `fastcheck all` und 127 JUnit-Tests, `clean build`, Selbsttest Fabric 72 Schritte (34 plus 15 Bildschirmfotos
  plus 23 Vergleichsschritte) und NeoForge 34 Schritte, Rauchtests Client und Server beider Loader, Web-Export beider
  Loader (174 Teile, 60 Vektoren) – alles grün. Alle elf Galerie-Bilder neu erzeugt.
- API-Wünsche W30–W32 (§9.3) sind nicht wesentlich; Propwash bleibt 0.4.0, Version 1.0.1.

### 10.9 Release 1.0.1 (2026-09-30)
- Zusammengeführt: `fix/style-review` (X-Class-Rahmen mit echtem Drohnen-Modell über `transformation`, runde Zellkappen
  der Li-Ion-Packs, eigene Farben für vier Teile, steilere Whoop-Antennen, kürzerer XT60 am 6S-AIO, geteilte H-Holme).
- Läufe: `strip_comments` ohne Änderung, `fastcheck all` und 128 JUnit-Tests, `parts_sync --check`, `clean build`,
  Selbsttest Fabric 73 Schritte (mit Bildschirmfotos und Vergleichsschritten) und NeoForge 35 Schritte, Rauchtests
  Client und Server beider Loader, Web-Export beider Loader (174 Teile, 60 Vektoren) – alles grün, nacheinander und stumm.
- Alle Bildschirmfotos im Spiel neu erzeugt (elf Galerie-Bilder und 22 Vergleichsbilder), die X-Class-Aufnahmen zeigen
  jetzt das echte Rahmenmodell.
- Release unter `release/1.0.1/` (nicht im Repository; dieser interne Stand liegt heute unter
  `release/internal/1.0.1-internal/`, `release/1.0.1/` ist seit 2026-10-03 die öffentliche Version 1.0.1, §10.21):
  Fabric- und NeoForge-Jar, `SHA256SUMS`, `web-export/`.

### 10.10 Release 1.0.2 – „Props im Bild“ wie im FPV-Bild (2026-09-30)
- Anlass: Bericht aus dem Playtest – Web-Konfigurator und Werkbank zeigten für „Deadcat LR 7″ · 8S“ 0,0 % Props im Bild,
  im Spiel waren an beiden Bildrändern Propscheiben zu sehen. Ursache in Propwash 0.4.0 (Analyse mit Rotorebene des
  Flugmodells 1 mm unter statt Nabe 6 mm über dem Objektiv, Kamera-FOV 120° statt Brillen-FOV 130°, grobes 96×54-Raster)
  und in den JMP-Daten (Objektiv nur 6 mm vor den Vorderarmen). Propwash 0.4.1 rechnet mit dem Aufbau des Renderers und
  bietet ihn über `BuildAccess#cameraView` (Parts-API 1.1) an.
- Daten: Kamera `lr_7_deadcat` (0, 16, −125) statt −78, `deadcat_5` (0, 14, −105) statt −62, `tp_35_stretched`
  (0, 12, −80) statt −58 (vorher 33 % Props im Bild und Warnung). Modelle mit `style_generate.py` neu (Deadcat-Nase bis zur
  Kamera; Toothpick-Generator setzt bei gesetzter Kamera vor dem Rumpf eine Nasenplatte, alle anderen 19 Rahmen
  byte-gleich). Sharecodes unverändert.
- Shift-Hilfen: Deadcat LR 7″ „0 statt 19 %“, Squashed „weniger Props im Bild (7 statt 12 %)“, Toothpick 2″ „rund 2 %“,
  Toothpick 3″ „rund 12 %“; „weites Sichtfeld zeigt mehr Props“ bei Firefly Nano, Pixel Micro und Clarity HD Pro
  gestrichen (das Bild benutzt das Brillen-Sichtfeld; Pixel Micro stattdessen „Bild unter HD-Niveau“). Korridore „Props im
  Bild“ nach den eingebauten Referenzen: 5″ 15 %, Long Range 20 %, Cinewhoop 25 %, X-Class 20 %.
- Web-Export: `camera_view` je Preset und Variante (`web-export.md` §2.8.1) für die Kamera-Vorschau des Konfigurators.
- Selbsttest „frame: Deadcats und H-Frames ohne Props im FPV-Bild (Kamera-Aufbau der API)“; Gegenprobe im Spiel mit
  Propwashs Selbsttest „drone-render: Props im FPV-Bild – Analyse gegen gerendertes Bild“ und dem JMP-Jar im Mods-Ordner:
  alle 22 JMP-Presets innerhalb 0,1 Prozentpunkten (Deadcat LR 7″ · 8S: 0,00 % Bild, 0,00 % Analyse; mit 1.0.1-Daten
  2,01 % / 2,02 %).

### 10.11 Release 1.0.3 – Deadcat LR 7″ wieder wie geflogen (2026-09-30)
- Anlass: Prüfung der Meldung zu „Deadcat LR 7″ · 8S“. 1.0.2 hatte neben der Analyse (Propwash 0.4.1) auch die Kamera von
  `lr_7_deadcat` auf −125 mm verschoben; der gemeldete Build zeigte danach 0,0 % und im Spiel keine Props mehr, das Bild
  aus der Meldung (Propspitzen links und rechts) gab es nicht mehr. Gefordert war die richtige Zahl, nicht ein anderer
  Rahmen.
- Daten: Kamera `lr_7_deadcat` wieder (0, 16, −78), 22° wie bis 1.0.1; Modell und Textur mit `style_generate.py --category
  frame --only lr_7_deadcat --write` neu (byte-gleich mit 1.0.1). `deadcat_5` (−105) und `tp_35_stretched` (−80) bleiben
  wie in 1.0.2. Sharecodes unverändert (27 Presets, 60 Vektoren gleich).
- „Props im Bild“ mit Propwash 0.4.1: `lr7_8s` 2,02 %, `lr7_deadcat_lion` 1,84 %. Shift-Hilfe „+ kaum Props im Bild
  (2 statt 19 %)“ und „− Propspitzen an beiden Bildrändern“; `FrameBenchTest` erwartet 1–3 %.
- Selbsttest „frame: Deadcats und H-Frames ohne Props im FPV-Bild, Deadcat LR 7″ mit Propspitzen am Bildrand
  (Kamera-Aufbau der API)“: `deadcat_5`, `hframe_5`, `xclass_13_h` 0 %, `lr_7_deadcat` zwischen 1 und 3 %, Werkbank
  gleich `cameraView`.
- Gegenprobe im Spiel (Propwash-Selbsttest „drone-render: Props im FPV-Bild – Analyse gegen gerendertes Bild“ mit dem
  1.0.3-Jar): alle 30 Presets innerhalb 0,2 Prozentpunkten; `lr_7_deadcat` 2,01 % Bild gegen 2,02 % Analyse (8S) und
  1,83 % gegen 1,84 % (6S Li-Ion), die Propscheiben liegen als schmale Sicheln am linken und rechten Bildrand.
- Release unter `release/1.0.3/` (nicht im Repository): Fabric- und NeoForge-Jar, `SHA256SUMS`, `web-export/`.

### 10.12 Release 1.0.4 – realistische Flugzeiten und eigener Kreativ-Tab (2026-09-30)
- Gegen Propwash 0.4.2 (Parts-API 1.2): `REQUIRED_API` 1.2, `propwash_version` 0.4.2, Bereiche `[0.4.2,)`.
- Zusammengeführt: `feature/own-creative-tab` (Tab „Propwash: Just More Parts“ direkt hinter dem Propwash-Tab).
- Kalibrierung (Propwash `docs/design/realism/calibration.md`): Propwash rechnet Flugzeiten, Ströme und Temperaturen mit
  einer Energiebilanz, die Flugphysik bleibt unverändert. JMP-Daten: `power_w` für alle 7 Videoteile, `usable_fraction`
  0,88 für `lihv_ss`, mehr Fahrtwind-Kühlung für 7 Motoren (60-s-Werte wie Herstellerprüfstände), Shift-Hilfen mit den
  neuen Zahlen (Teile-Katalog §12). Sharecodes, Ids, Rezepte und Beute unverändert.
- Anzeige: Sharecode-Fenster und `/jmp code info` nennen gemischt/aggressiv; Web-Export §2.9 mit den neuen Kennzahlen.
- Tests: Zielkorridore der Prüfstände nach den Referenzen, `OwnerFlightTimeTest` (Owner-Sharecode: Reisen 3:00–3:30,
  gemischt 2:00–2:30, aggressiv 1:30–2:00 min, Landespannung 3,3–3,5 V, Tendenzen KV und Blattzahl).
- Release unter `release/1.0.4/` (nicht im Repository). 1.0.3 war bereits veröffentlicht, deshalb 1.0.4.
- **Nachkalibrierung nach dem Realismus-Review** (dieselbe Version, Dateien unter `release/1.0.4/` ersetzt, gegen die
  ebenfalls ersetzte Propwash 0.4.2): Propwash rechnet die Blattinterferenz kleiner Vierblatt-Props als Teillastverlust,
  neue Energiefaktoren für 31–102-mm-Props, kräftigere Profile für 65-mm-Whoops (Vollgasstöße), Race, Long Range und
  X-Class, Landung bei Landespannung im ruhigen Flug und Fahrtwind in der Temperatur nach gemischtem Flug. JMP-Daten:
  `digital_micro` 5,1 W, `analog_whoop_25mw` 0,7 W. Shift-Hilfen mit den neuen Zahlen (Teile-Katalog §12),
  `OwnerFlightTimeTest` mit den Grenzen der Referenz (Schweben digital 3:00–3:20, Vollgasstrom 12–16 A, 23000 KV +15–25 %,
  Blattzahl in allen Stilen, Digital −30–35 %, flugwirksame Werte eingefroren). Sharecodes, Ids, Rezepte, Beute und das
  Flugverhalten unverändert.

### 10.13 Release 1.0.5 – Datenkorrektur nach dem zweiten Realismus-Review (2026-09-30)
- Gegen Propwash 0.4.3 (Parts-API 1.2 unverändert): `propwash_version` 0.4.3, Bereiche `[0.4.3,)` bzw. `>=0.4.3`,
  `REQUIRED_API` bleibt 1.2. Propwash 0.4.3 garantiert die Reihenfolge der Flugzeiten, rechnet den Vollgasstrom kleiner
  Props nach den Prüfständen und korrigiert die Beschriftung der Werkbank (Propwash `docs/design/realism/calibration.md` §0).
- Mit Freigabe des Besitzers vom 30.09. korrigierte **flugwirksame Daten** (nur diese Teile fliegen anders, Teile-Katalog
  §13): Beiwerte `p40_17_3b_pc`, `p65_22_4b_bi`, `p89_25_5b_pc_duct`, `p102_30_3b_gfn` (Referenz-Beiwerte, Gütezahl
  gleich), Luftwiderstand der Ducts `cw_25_duct` und `cw_35_duct` (Stirn- und Draufsicht), `m2004_1700` Wicklungswiderstand
  und Fahrtwind-Kühlung, `m1103_11000` Fahrtwind-Kühlung, Massen `p76_30_2b_pc` und `m2807_1500`. Ids, Sharecodes,
  Rezepte und Beute unverändert.
- Beweis der Bitgleichheit: Propwash `sim.MotionFingerprintTest` über alle 27 Presets mit allen Einzeltausch-Varianten –
  1004 Varianten ohne korrigiertes Teil fliegen bitgleich wie mit 1.0.4 + Propwash 0.4.2, 240 mit korrigiertem Teil anders.
- Neu erstellt nach dem dritten Realismus-Review: `cw_35_duct` mit 0,0137 / 0,0228 m² nach derselben Regel wie
  `cw_25_duct` und `propwash:cinewhoop_3` (vorher 0,018 / 0,013, Draufsicht kleiner als die Stirnfläche), 3,5″-Cinewhoop
  89 km/h; Propwash 0.4.3 fliegt Li-Ion-Akkus im gemischten Flug mit höchstens 6 C (JMP-Li-Ion-Presets gemischt 16–18 min).
  Gegen die erste Fassung fliegen nur die 34 Varianten mit `cw_35_duct` anders (Teile-Katalog §13).
- Tests: neuer Property-Test `bench.FlightTimeOrderTest` (1177 Einzeltausch-Varianten über die öffentliche API,
  aggressiv ≤ gemischt ≤ Reisen ≤ Schweben), Cinewhoop-Korridore nach `realism/builds.md` §0 (Schub/Gewicht 4–7,
  Schwebegas 23–35 %, Topspeed 80–150 km/h), Pro/Contra-Prüfungen und Shift-Hilfen mit den neuen Werten; 135 JUnit-Tests.
- Release unter `release/1.0.5/` (nicht im Repository): Fabric- und NeoForge-Jar, `SHA256SUMS`, `web-export/`.

### 10.14 Öffentliche Version 1.0.0 (2026-10-01)
- Versionsschema des Besitzers (`docs/RELEASING.md`): öffentlich zählen nur Releases. Die internen Stände 1.0.0–1.0.5 sind
  zusammen die erste Veröffentlichung **1.0.0** gegen die öffentliche **Propwash 0.4.0** (die internen Propwash-Stände
  0.4.0–0.4.4). `version` 1.0.0, `propwash_version` 0.4.0, Bereiche `[0.4.0,)` bzw. `>=0.4.0`, `REQUIRED_API` bleibt 1.2.
  Interne Stände bis zum nächsten Release heißen `1.0.1-dev.N` / `1.0.1-rc.N`.
- Vorher die Restpunkte des vierten Realismus-Reviews (Teile-Katalog §13.2, Propwash `realism/calibration.md` §0.8):
  `m2004_1700` Fahrtwind-Kühlung 0,000236 (Obergrenze der Datenregel), `p65_22_4b_bi` ct/cp 0,1874/0,1961 (Gütezahl 0,33),
  die Geschwister-Props `p40_20_2b_pc`, `p40_15_4b_pc`, `p40_14_5b_soft` (a = 1,7), `p65_25_3b_pc` (ct · 1,3, Gütezahl
  0,35) und `p102_25_2b_pc` (Referenz 4024-2); `p89_30_3b_pc` unverändert (Preset `toothpick35_6s`). Shift-Hilfen und
  Prüfstände angepasst (Toothpick-Korridore nach `builds.md` §0, Referenz „Whoop 75 2S 300“ mit erlaubtem
  `battery_overload`/`heavy_sag`, 0802/15500-Referenz auf 2S 450 digital ohne Temperaturprüfung).
- Beweis: Propwash `tools/calibration.sh` – 1245 Varianten, 0 ohne geändertes Teil anders; nur `cinewhoop25_4s` fliegt
  anders, `cinewhoop35_6s` und `lr4_6s_lion` nur in der Motorwärme; Harness 5/5, Owner-Fall unverändert.
- Prüfung gegen Propwash 0.4.0 aus mavenLocal: strip_comments sauber, `parts_sync --check` 0 Probleme, fastcheck all,
  135 JUnit-Tests, clean build, Selbsttest Fabric 75 Schritte (Bildschirmfotos neu) und NeoForge 37 Schritte, Rauchtests
  Client und Server beider Loader, Web-Export beider Loader (174 Teile, 60 Vektoren, gleich bis auf den Zeitstempel,
  Sharecode-Spezifikation byte-gleich, Testvektoren bis auf `analysis` von 6 Codes gleich).
- Release unter `release/1.0.0/` (nicht im Repository): Fabric- und NeoForge-Jar, `SHA256SUMS`, `web-export/`; die
  internen Stände liegen unter `release/internal/1.0.0-internal` … `1.0.5-internal`.

### 10.15 Flugschul-Taste für JMP-Teile (Besitzer-Rückmeldung zu 1.0.0, Punkt 2)
- Befund: Die Flugschul-Taste (M) öffnete über JMP-Teilen nichts. Propwash ließ in `GuideOpener.scenesFor` nur Items im
  Namensraum `propwash` zu, und die öffentliche API hatte keinen Weg, Items mit Propwash-Szenen zu verknüpfen. Ein
  JMP-seitiger Ersatz war nicht möglich (wesentlicher API-Wunsch).
- Propwash 0.4.0 (neu erstellt) bringt dafür die Parts-API **1.3.0**: die Taste wirkt für Items jedes Namensraums,
  Akkus, Videos, Stacks und Props von Addons bekommen die passende Propwash-Szene automatisch, und
  `PropwashClientApi.linkFlightSchoolParts`/`linkFlightSchoolItems`, `flightSchoolScenes(stack)`,
  `openFlightSchool(stack, parent)` sowie `api.client.FlightSchoolScenes` sind neu.
- JMP 1.0.0 (neu erstellt): `REQUIRED_API` und `propwash_api_version` 1.3, Zuordnung `client.SchoolLinks` (§4), die
  Rahmenformen-Szene gilt für alle 22 Rahmen statt für sechs fest eingetragene, JUnit `client.SchoolLinksTest`,
  Selbsttest `dev.client.SchoolSelfTests` (143 Items, 22 Presets; Items je Szene: Werkbank 143, Überleben 143,
  Schäden/Props 72, Akku 30, Rahmenformen 22, Konfigurator 12, Video 7).

### 10.16 Props im FPV-Bild nach Rahmen und Kamerahalter (in 1.0.0, 2026-10-01)
- Anlass: Rückmeldung zu Propwash 0.4.0 / JMP 1.0.0, in keiner Drohne seien mehr Props im Bild zu sehen; ein
  Race-Rahmen mit eng stehenden Armen sollte mehr Props zeigen als ein normaler X-Frame. Ursache: Rahmen ohne
  `camera.pos` bekommen in Propwash die Altformel (Objektiv 15 mm über dem Schwerpunkt) und lagen damit 9–28 mm über der
  Propebene; der Race-Stretch zeigte 0–1,9 %, mit gleichen Teilen 0,7 % gegen 11,7 % beim True-X.
- Daten: alle 16 Rahmen ohne Kamerahalter haben jetzt `camera.pos` (x 0, z 10 mm vor der bisherigen Kameramitte −0,21 ·
  Radstand, Whoops bei z −17,5 mm vor den Kamerawangen, Höhe wenige Millimeter unter der Propebene des Presets, Whoops
  knapp über den Ducts); `deadcat_5`, `hframe_5`, `lr_7_deadcat`, `tp_35_stretched`, `whoop_75_duct`, `xclass_13_h`
  unverändert. Werte und Begründung: Propwash `docs/design/realism/props-in-view.md` §4.2/§5. Rahmenmodelle mit
  `style_generate.py --category frame --write` neu; Sharecodes unverändert (nur Rahmen-Daten).
- Ergebnis: Race-Stretch 9,8/10,2 % (mit 2207.5 bis 15 %; nachkorrigiert auf 11,3/12,2 %, §10.18), True-X 5″ 7,9 %, Squashed 3,0 %, Deadcat/H 0 %, Whoops
  5,9–7,7 %, Cinewhoops 6,8–6,9 %, Toothpicks 4,0–7,6 %, Long Range 6,2–8,1 % (Deadcat 7″ 1,8–2,0 %), X-Class 2,3–7,1 %.
- Physik: Propwashs Bewegungs-Fingerabdrücke (1245 Varianten) bitgleich; bei Rahmen ohne eigene Arme ändert sich nur das
  Objektiv, bei `fs_5_squashed` und `race_5_stretched` zusätzlich der Kamerapunkt der Hülle, der in keinem Prüfflug
  Kontakt hat.
- Prüfung: `FrameBenchTest` und `MotorBenchTest` angepasst, neuer Selbsttest „frame: jeder Rahmen mit Kamerahalter,
  Props im FPV-Bild nach Rahmenform (Kamera-Aufbau der API)“; Gegenprobe mit Propwashs Selbsttest „drone-render: Props
  im FPV-Bild – Analyse gegen gerendertes Bild“ und dem JMP-Jar: alle 22 JMP-Presets innerhalb 0,08 Prozentpunkten;
  Maskenbilder je Rahmen unter `docs/screenshots/fpv_coverage/`. Web-Export (`camera_view`) beim Release neu erzeugen.

### 10.17 Release 1.0.0 neu erstellt (2026-10-01)
- Nach der Besitzer-Rückmeldung zu Propwash 0.4.0 / JMP 1.0.0 (Teilnamen, Flugschul-Taste, Props im FPV-Bild) sind
  `fix/part-naming`, `fix/school-links` (§10.15) und `fix/props-view` (§10.16) in `main` zusammengeführt. Die Versionen
  bleiben 0.4.0 und 1.0.0, weil beide noch nicht veröffentlicht sind; Jars und Web-Export unter `release/1.0.0/` sind
  ersetzt.
- Gebaut gegen die neu erstellte Propwash 0.4.0 aus mavenLocal (Parts-API 1.3.0). Prüfungen: `parts_sync --check`,
  fastcheck all, 138 JUnit-Tests, Selbsttest Fabric (mit Bildschirmfotos) und NeoForge, Rauchtests Client und Server
  beider Loader, Web-Export beider Loader gleich bis auf den Zeitstempel.
- Nachprüfung: Die Prop-Kurzbeschreibungen nennen das Material jetzt wie der Name (`part-naming.md` §6, `fiber` statt
  `fibre`); `tools/parts_sync.py --check` prüft die Kurzkennung jeder Prop-Kurzbeschreibung und verbietet `fibre`,
  `Li-ion` und `"` als Zoll in allen Texten, `PropDataTest` vergleicht das Material mit dem Namen. Jars und Web-Export
  unter `release/1.0.0/` danach noch einmal ersetzt (nur Texte, Flugwerte und Sharecodes unverändert).

### 10.18 Kurzbeschreibungen nach §6 und Race-Stretch-Kamerahalter (in 1.0.0, 2026-10-02)
- Version bleibt 1.0.0 (noch nicht hochgeladen, Besitzer 02.10.; der nächste Release-Schritt baut die Jars neu). Nur
  Texte und ein Kamerahalter, gebaut gegen Propwash 0.4.0; die Änderungen brauchen keine neue API.
- Shift-Kurzbeschreibungen: `tools/parts_sync.py --check` prüft jetzt die Kurzkennung jedes Teils nach
  `part-naming.md` §6 (Motor, Prop, Akku, Stack, Rahmen, Video, Zubehör; genau ein Einsatz ohne Schlusspunkt, Englisch
  klein, „Schutzring“ verboten). Mit `--propwash <Propwash-Ordner oder -Jar>` prüft es dieselben Regeln für die
  eingebauten Propwash-Teile (und im Ordner für die JSON-Teile des Beispiel-Addons). Abweichungen vorher: fünf Rahmen
  (`fs_5_bash`, `deadcat_5`, `hframe_5`, `lr_7_deadcat`, `xclass_13_h`) in beiden Sprachen; jetzt `<Größe> <klasse>
  frame[ (<Variante>)]` mit der Form vorn im Einsatz, z. B. `5″ freestyle frame – deadcat, no props in view`.
- Kamerahalter `race_5_stretched` (0; 18,0; −59,6) → (0; 16,5; −47,5), Uptilt 40°: Props im FPV-Bild `race5_4s`
  9,77 → 11,31 %, `race5_6s` 10,23 → 12,19 % (im Spiel 11,28 / 12,16 %), 2207.5-Referenz 15,15 → 14,80 %; Rahmenmodell neu
  (Kamerakäfig vor den vorderen Abstandshaltern). Begründung und Tabelle: Propwash `docs/design/realism/props-in-view.md`
  §5.1. Physik: Propwashs Bewegungs-Fingerabdrücke (1245 Varianten) bitgleich, Spezifikation nur in Objektiv und
  Kamerapunkt der Hülle anders. Maskenbild `docs/screenshots/fpv_coverage/justmoreparts_race_5_stretched.png` neu.

### 10.19 Offene Punkte vor der Pause (in 1.0.0, 2026-10-02)
- Auftrag: alle Punkte, die vor der Pause offen geblieben waren, gleich mit erledigen.
  Gesammelt aus allen Berichten und Dokumenten, geprüft und in `fix/open-items` erledigt (Propwash und JMP); Versionen
  bleiben 0.4.0 / 1.0.0.
- Daten (`fix/open-items-data`): 89-mm-Dreiblatt korrigiert, Fahrtwind-Kühlung zweier Toothpick-Motoren, ehrliche
  2S-Empfehlungen, Prüfstände ohne Ausnahmen (Teile-Katalog §13.3, Propwash `calibration.md` §0.10); übersetzbare
  Sharecode-Fehler (`share.ShareException`); Selbsttest ohne veraltetes `Ingredient#items`.
- Parts-API 1.4 (§9.4): Beute über `PropwashApi.loot()`, Werkbank-Kopf mit Kurzform und Symbol, Name beim Import,
  Drohne erzeugen ohne Befehl, Einkaufsliste nach Server-Regeln, Lackkosten vorab, OSD-Prüfung, Seltenheit per JSON,
  Prüfstand mit Propwashs Codec, Autorität und Prop-Wash-Rauschen aus `BuildStats`.
- Stil, Bildschirmfotos: siehe Tages-Changelog 2026-10-02.

### 10.20 Sichtfeld vom Videosystem, Kamerawinkel im Sharecode (in 1.0.0, 2026-10-02)
- Anlass: Ideen des Besitzers zu Propwash 0.4.0: das FPV-Sichtfeld kommt wie in echt von der Kamera im Videomodul, der
  Kamerawinkel gehört zur Drohne. Propwash 0.4.0 (Parts-API **1.4.0**) zeichnet seitdem mit `camera_fov_deg` und
  speichert den Winkel im Tuning der Drohne (`camera.uptiltDeg`), begrenzt auf den Kamerahalter der Rahmenklasse
  (`BuildAccess#uptiltRange`, `CameraView#uptiltRange()`).
- Daten: Sichtfelder aller sieben Videosysteme aus echten Kameras recherchiert (Propwash `power-video.md` §4.4):
  Firefly Nano 126°, Racecore 800 125°, Longshot 1600 110°, Pixel Micro 126°, Swift Link 133°, Clarity HD Pro und
  Horizon LR 130° (Katalog §6). Shift-Hilfe: Sichtfeld als Vorteil bei Longshot 1600, Pixel Micro und Swift Link.
- Sharecode PW1 Revision 1.2 (`sharecode.md`): `camera.uptiltDeg` als Tabellen-Index 56 im TUNE-Block,
  `ShareTables.TUNE_GROUPS` = Flug-Gruppen + `CAMERA` für Kodieren, Dekodieren und alle Übernahmen; alte Codes ohne
  Winkel setzen den Standard des Rahmens, Vektoren A–F bytegleich, neuer Vektor G; Decoder der Revision 1.1 überspringen
  nur `tune_index:56`.
- Web-Export: Rahmen `camera.tilt_min_deg`/`tilt_max_deg`, `camera_view` mit `uptilt_min_deg`/`uptilt_max_deg` und dem
  Sichtfeld der Kamera, `tune.json` mit `groups.shared`. `REQUIRED_API` und `propwash_api_version` 1.4.
- Props im Bild (Propwash-Selbsttest mit dem JMP-Jar, alle 22 JMP-Presets innerhalb 0,08 Prozentpunkten): Whoop 75 8,6 %,
  Whoop 85 6,2 %, Toothpicks 4,3–6,1 %, Cinewhoops 6,7–6,9 %, True-X 5″ 7,9 %, Squashed 3,0 %, Race-Stretch 9,6/10,0 %,
  Deadcat/H 0 %, Long Range 6,1–8,1 % (Deadcat 7″ 1,8–2,0 %), X-Class 0–7,1 %. `race5_4s` lag im Kamera-Zweig mit 9,6 %
  knapp unter dem Ziel 10–15 % (vorher 9,8 %, Kamerahalter nicht geändert). Physik bitgleich (Propwash
  `tools/calibration.sh` mit JMP-Daten: 1245 Bewegungs-Fingerabdrücke gleich).
- Endstand nach dem Zusammenführen mit §10.18 (Race-Stretch-Halter) und §10.19: Race-Stretch `race5_4s` 11,08 %
  (133°-Kamera), `race5_6s` 11,73 % (125°), beide im Ziel 10–15 %; alle 22 JMP-Presets und die 8 Propwash-Presets liegen in
  ihren Zielbereichen (Propwash `props-in-view.md` §9.4; dort wurden nur die Kamerahalter von `whoop_65` und
  `cinewhoop_3` nachgezogen, JMP-Daten unverändert). Whoop 75 (8,64 %) zeigt weiter weniger Ducts als der 65-mm-Whoop
  (8,81 %). Maskenbilder `docs/screenshots/fpv_coverage/justmoreparts_*.png` neu.
- Prüfung: fastcheck all, 139 JUnit-Tests (neu: Vektor G, Kamerawinkel im Code, alte Codes = Rahmen-Standard),
  `parts_sync --check`, Selbsttest Fabric und NeoForge (je 39 Schritte), Web-Export Fabric gegen die Propwash-Testversion
  aus dem Zweig `feat/camera-module`.

### 10.21 Öffentliche Version 1.0.1 für Propwash 0.5.0 (2026-10-03)
- Zusammengeführt: `chore/license` (PolyForm Shield 1.0.0 für Kern, Teile-Daten und Assets, MIT für die Doku, CLA,
  `tools/public_export.py`). Teile, Daten und Code sonst unverändert gegenüber 1.0.0.
- Gebaut gegen Propwash 0.5.0 aus mavenLocal (`propwash_version`), Untergrenzen bleiben `[0.4.0,)` bzw. `>=0.4.0`,
  `REQUIRED_API` bleibt 1.4 (Propwash 0.5.0 bringt API 1.5.0).
- Prüfung: strip_comments ohne Änderung, `parts_sync --check` 144 Dateien, `fastcheck all`, 150 JUnit-Tests,
  `clean build`, Selbsttest Fabric mit Bildschirmfotos 80 Schritte und NeoForge 39 Schritte, Rauchtests Client und
  Server beider Loader, Web-Export beider Loader (176 Teile mit den neuen Propwash-Zubehören Greifer und Action-Cam
  (Flashback), 61 Vektoren) – alles grün, nacheinander und stumm. Die Bildschirmfotos zeigten nur Render-Rauschen und
  bleiben unverändert.
- Release unter `release/1.0.1/` (nicht im Repository): Fabric- und NeoForge-Jar, `SHA256SUMS`, `web-export/`.

### 10.22 Version 1.1.0 für Propwash 0.5.0 (2026-10-06)
- Zusammengeführt: `feat/sharecode-v2` (kurze Sharecodes PW2 auf main wie in 1.0.0, Tabellen, Testvektoren, Web-Export mit
  PW2). Version 1.1.0 fasst den nie veröffentlichten Stand 1.0.1 zusammen (Lizenz, Whoop-Rahmen mit Draufsicht nach dem
  Umriss); `CHANGELOG.md` nennt nur 1.0.0 und 1.1.0.
- Gebaut gegen das finale Propwash 0.5.0 aus mavenLocal (Parts-API 1.5.0, Netzprotokoll 15), Untergrenzen bleiben
  `[0.4.0,)` bzw. `>=0.4.0`, `REQUIRED_API` bleibt 1.4.
- Web-Export: Der Kreativ-Flugcontroller `propwash:creative_fc` (passt in jeden Rahmen) wird nicht exportiert und in den
  Zufallsbauten der Regelprüfung ausgelassen (`design/web-export.md` §2.2); vorher brach der Export mit 50 Abweichungen
  der Formel `stack_mount` ab. Betrifft nur den Devtest-Code, nicht die Jars.
- Prüfung: strip_comments ohne Änderung, `parts_sync --check` 144 Dateien, `fastcheck all`, 167 JUnit-Tests,
  `clean build`, Selbsttest Fabric mit Bildschirmfotos 80 Schritte und NeoForge 39 Schritte, Rauchtests Client und
  Server beider Loader, Web-Export beider Loader gleich bis auf den Zeitstempel (176 Teile, 124 Vektoren). Neue
  Bildschirmfotos nur mit Inhaltsänderung aus Propwash 0.5.0: Shift-Tooltip eines Props mit „Absturz“ und
  Werkbank X-Class 13″ mit Trocken- und Abfluggewicht (englisch und deutsch).
- Release unter `release/1.1.0/` (nicht im Repository): Fabric- und NeoForge-Jar, `SHA256SUMS`, `web-export/`;
  Tag `v1.1.0`.
