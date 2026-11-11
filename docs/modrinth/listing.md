# Modrinth-Listing — Propwash: Just More Parts 1.2.0

Alles, was die Modrinth-Projektseite braucht. Beschreibung: [`description.md`](description.md) (Englisch, als
Projektbeschreibung einfügen) und [`description_de.md`](description_de.md) (Deutsch, für eine übersetzte Seite oder als
Spoiler-Abschnitt). Changelog für den Versions-Upload: [`CHANGELOG.md`](../../CHANGELOG.md), Abschnitt 1.2.0.

## Projekt

| Feld | Wert |
|---|---|
| Name | Propwash: Just More Parts |
| Slug (Vorschlag) | `propwash-just-more-parts` (`justmoreparts` passt zur Mod-Id, falls frei) |
| Projekttyp | Mod |
| Icon | `common/src/main/resources/assets/justmoreparts/icon.png` |
| Lizenz | bis 1.0.0 All Rights Reserved (Modrinth: `LicenseRef-All-Rights-Reserved`). Ab der ersten Version danach: eigene Lizenz `LicenseRef-PolyForm-Shield-1.0.0` mit dem Link auf `LICENSE` im öffentlichen Repository (bis es existiert: https://polyformproject.org/licenses/shield/1.0.0); die MIT-Teile (Doku) nennt `LICENSE-MIT` |
| Client | Pflicht |
| Server | Pflicht |
| Links (Quellcode, Issues, Wiki, Discord, Spenden) | Quellcode `https://github.com/LardTom/propwash-justmoreparts`, Issues `https://github.com/LardTom/propwash-justmoreparts/issues`, Wiki `https://propwashfpv.com/docs/`, Discord `https://discord.gg/bNhkSXYSgZ`, Spenden (Ko-fi) `https://ko-fi.com/propwashfpv`. Die Metadaten zeigen auf `https://modrinth.com/mod/propwash-just-more-parts` (`homepage_url` in `gradle.properties`, benutzt von `fabric.mod.json` und `neoforge.mods.toml`) |

### Kurzbeschreibung (≤ 256 Zeichen)

Englisch (243 Zeichen):

> 146 more real-world drone parts for Propwash FPV: motors, props, batteries, stacks, video and frames from 65 mm whoops
> to 13″ X-Class, new frame shapes (deadcat, H-frame, stretched, ducts), 23 presets, and every part in Propwash's share
> codes.

Deutsch (247 Zeichen, falls eine übersetzte Kurzbeschreibung gewünscht ist):

> 146 weitere echte Drohnenteile für Propwash FPV: Motoren, Props, Akkus, Stacks, Video und Rahmen vom 65-mm-Whoop bis
> zur 13″-X-Class, neue Rahmenformen (Deadcat, H-Frame, Stretched, Ducts), 23 Presets und jedes Teil in den Sharecodes
> von Propwash.

### Kategorien

| | |
|---|---|
| Hauptkategorien (max. 3) | Technology, Transportation, Equipment |
| Weitere Kategorien | Game Mechanics |

Suchbegriffe, die Kurzbeschreibung und Beschreibung abdecken: FPV, drone, quadcopter, Propwash, add-on, parts, whoop,
toothpick, cinewhoop, freestyle, racing, long range, X-Class, deadcat, share code.

## Versionen

Je Loader eine Version hochladen; beide entstehen aus demselben Commit.

| Feld | Fabric | NeoForge |
|---|---|---|
| Datei | `fabric/build/libs/justmoreparts-fabric-26.3-1.2.0.jar` | `neoforge/build/libs/justmoreparts-neoforge-26.3-1.2.0.jar` |
| Versionsnummer (Vorschlag) | `1.2.0+fabric` | `1.2.0+neoforge` |
| Versionsname (Vorschlag) | Just More Parts 1.2.0 (Fabric) | Just More Parts 1.2.0 (NeoForge) |
| Loader | Fabric | NeoForge |
| Spielversionen | 26.3 | 26.3 |
| Abhängigkeiten | Propwash FPV ≥ 0.6.0 (`0.6.0+fabric`) — Pflicht; Fabric API — Pflicht | Propwash FPV ≥ 0.6.0 (`0.6.0+neoforge`) — Pflicht |
| Kanal (Vorschlag) | Beta, solange Propwash Beta ist | Beta |
| Changelog | `CHANGELOG.md`, Abschnitt 1.2.0 | ebenso |

Die `-sources`-Jars nicht als Hauptdatei hochladen. Mindestversionen aus `gradle.properties`: Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3, NeoForge 26.3.0.10-beta, Java 25, Propwash FPV 0.6.0 (Add-on-API 1.8). Propwash 0.6.0 startet
nicht mit Just More Parts 1.1.x.

## Galerie

Alle Bilder 1920 × 1080 in `docs/screenshots/`, erzeugt mit `./gradlew :fabric:runClientSelftest -PjmpScreenshots`. In
dieser Reihenfolge hochladen; das hervorgehobene Bild zeigt Modrinth auf der Projektkarte und in der Suche.

| # | Datei | Titel (en) | Beschreibung (en) |
|---|---|---|---|
| 1 ★ hervorgehoben | `jmp_lineup.png` | From whoop to X-Class | Eight of the 23 presets – whoop, toothpick, cinewhoop, deadcat, race, H-frame, 7″ long range deadcat and 13″ X-Class – with weight, thrust-to-weight, top speed and hover time from Propwash's analysis. |
| 2 | `jmp_workbench_xclass13.png` | X-Class at the workbench | A 13″ X-Class built from add-on parts: 4214 motors, 13″ full-carbon props and an 8S 5000 mAh pack, 3.1 kg and ready to fly. |
| 3 | `jmp_frames_scene.png` | Frame shapes compared | The new Flight School lesson: five 5″ frame shapes with the same parts, seen from above and labelled. |
| 4 | `jmp_frames_scene_stretched.png` | Stretched X up close | The lesson zooms in on each shape – here the stretched X, whose rolls are snappier than its flips. |
| 5 | `jmp_scale.png` | Size classes to scale | 75 mm whoop, 3.5″ toothpick, 5″ deadcat, 7″ long range and 13″ X-Class side by side at the same scale, with wheelbase and a 20 cm bar. |
| 6 | `jmp_xclass_whoop.png` | X-Class next to a whoop | A 13″ X-Class (1.15 m across the prop tips, 3.1 kg) and a 75 mm whoop on the same patch of grass. |
| 7 | `jmp_inventory.png` | Parts in the inventory | Five presets in the hotbar, frames, motors and props from 31 mm to 13″ – every part with its own model in Propwash's style. |
| 8 | `jmp_tooltip_shift.png` | Shift help on every part | Hold Shift: figures, pros and cons, what it fits and the specs – here a soft 5.1″ prop that survives crashes but wobbles in prop wash. |

Das Bild `jmp_workbench_xclass13_de.png` zeigt dasselbe Fenster auf Deutsch (Prüfung der Übersetzung und der
Beschriftungsbreiten); es gehört nicht in die Galerie.

Seit 1.2.0 nicht mehr in die Galerie: `jmp_share_import.png`, `jmp_share_export.png`, `jmp_config_tab.png` (und ihre
Fassungen `_de`/`_small`) zeigen das eigene Share-Fenster und den eigenen Konfigurator-Tab von Just More Parts bis
1.1.x. Beides gibt es nicht mehr; Sharecodes zeigt die Galerie von Propwash.

Die Vergleichsbilder in `docs/screenshots/compare/` (`jmp_compare_<kategorie>_first|swap|third.png`) zeigen je ein
Propwash-Teil (links, Schild „Propwash FPV“) neben einem JMP-Teil (rechts, „Just More Parts“) in der Schnellleiste, in
beiden Händen, am Boden und im Rahmen; `_swap` tauscht die Seiten, `_third` zeigt die Hände von vorn. Sie belegen den
Stil und die Größen (seit dem internen Stand 1.0.1) und gehören nicht in die Galerie.

Die Bilder 6–8 erzeugt der Selbsttest seit 02.10. selbst (`dev.client.GalleryShots`; die Shift-Hilfe zeigt er über
Propwashs `PropwashClientApi.forceShiftHelp`). Statt des Kreativ-Tabs zeigt Bild 7 das Inventar, weil sich ein bestimmter
Kreativ-Tab mit der Vanilla-API nicht ohne Reflection auswählen lässt.

Fehlt noch für die Seite: **Deadcat im FPV-Bild** (keine Props im Bild) – Addon-Selbsttests können über die öffentliche
API nicht ins FPV schalten; von Hand im Spiel aufnehmen (Preset `deadcat5_6s`, Taste für das FPV-Bild).

Bilder in der Beschreibung: Modrinth kann keine Pfade aus dem Repository auflösen. Nach dem Hochladen der Galerie die
Bild-URLs aus der Galerie kopieren und bei Bedarf in die Beschreibung einbauen (etwa unter „What's inside“).
