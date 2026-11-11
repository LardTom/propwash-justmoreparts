# Akku gesteckt (ab 1.2.0, mit Propwash 0.6.0)

Für Spieler. Sitzt der Akku in einer JMP-Drohne, zeigt das Modell keine Stecker und keine Kabel: Der Akku hat dann weder
Stromkabel mit Stecker noch Balancer-Kabel, der Stack keine Kabel nach hinten. Nichts steht hinter dem Akku oder dem
Rahmen ab – an allen 22 JMP-Rahmen, mit dem Akku oben und unter dem Rahmen (Whoops, Race-Rahmen), mit JMP- und
Propwash-Akkus, in der Welt, in der Hand, in der Werkbank, bei Ghosts und in der Flugschule. Ohne Akku zeigt die Drohne
den Stecker ihres Stacks wie bisher; ein einzelner Akku (in der Hand, im Regal, am Boden) behält Kabel und Stecker.

Das Ausblenden macht Propwash (Vertrag in Propwash `docs/battery-plug.md` und `docs/ASSETS.md` „Akku gesteckt“). Just
More Parts liefert nur die Stack-Varianten dafür. Mit Propwash vor 0.6.0 bleibt alles wie bisher: Die zusätzlichen
Modelle werden dort nicht benutzt.

## Was Just More Parts liefert

- **Stack-Varianten ohne Stecker:** Jeder der 12 JMP-Stacks hat `<id>_mounted` und `<id>_mounted_paint`
  (`models/part/stack/`, `items/drone_render/stack/`). Sie sind das Grundmodell ohne die Stecker-Elemente
  `connector`, `connector_grip` und `bt_connector`; die beiden Stromkabel heißen `pigtail`, damit Propwash sie an der
  Drohne ausblendet. Gleiche Textur wie das Grundmodell, kein Inventar-Item; die Lack-Gruppen stehen in
  `drone_paint.json` unter `stack/<id>_mounted`.
- **Akku-Modelle unverändert:** Alle 88 Akku-Modelle (mit `_paint` und `_under`) haben ihre Buchse schon
  (`connector` + `connector_grip` bzw. `bt_connector`), ab 2S dazu `balance_plug`, die Kabel heißen `wire`. An der
  Drohne blendet Propwash sie aus. Die Steckerart kommt aus dem Feld `connector` der Akku-Definition.
- **Keine Rahmen-Änderung:** Kein JMP-Rahmen braucht etwas Eigenes, auch die vergrößert gezeichneten X-Class-Rahmen
  nicht: Propwash blendet im Modell aus, bevor Minecraft die `transformation` des Render-Items anwendet. Einträge
  `power_lead` in `drone_layout.json` ignoriert Propwash ab 0.6.0; Just More Parts hat keine.
- **Keine Daten:** reine Darstellung im Client. Welten, Drohnen, Presets und Codes bleiben gleich.

## Werkzeug

`python3 tools/connector_models.py` leitet die Varianten aus den Stack-Modellen ab und schreibt die Lack-Gruppen.
`python3 tools/connector_models.py --check` prüft ohne zu schreiben: Varianten vollständig und aktuell, keine
verwaisten `_mounted`-Dateien, jede Akku-Buchse gefunden, Steckerart passt zur Buchse (weiß nur mit `bt_connector`).

`tools/assets/style_generate.py --write` löscht beim Neubau eines Stacks auch dessen `_mounted`-Dateien. Nach jedem
Neubau von Stacks deshalb `python3 tools/connector_models.py` aufrufen; sonst schlägt `PowerPlugModelsTest` fehl.

## Prüfen

- JUnit `power/PowerPlugModelsTest` (läuft in `tools/fastcheck.sh test`, gegen Propwash 0.5.2 und 0.6.0).
- JUnit `power/PowerPlugTextsTest`: Diese Seite, README, CHANGELOG, Release-Notizen, Modrinth-Texte und Sprachdateien
  versprechen kein gestecktes Paar mit Kabel, keine Ressource hat einen Eintrag `power_lead`.
- Selbsttest „power: Akku gesteckt – gesteckte Stack-Varianten und Akku-Buchsen aus JMP geladen“ (beide Loader):
  24 Modelle `_mounted` aus demselben Ressourcenpaket wie das Grundmodell, 88 Akku-Buchsen.
- Bildschirmfotos (Fabric, `-PjmpScreenshots`, gegen Propwash 0.6.0):
  `docs/screenshots/jmp_battery_plugged_<rahmen>.png` für jeden JMP-Rahmen schräg von hinten auf das Akku-Ende, dazu
  `…_fs_5_bash_unpainted.png` (Stack ohne Lack) und `…_propwash_<rahmen>.png` für die fünf Propwash-Rahmen als
  Gegenprobe. Zu sehen: Akku und Stack ohne Stecker und ohne Kabel. Die 28 Bilder im Repo stammen noch aus Propwash
  0.6.0-dev.5 und zeigen das frühere Paar; sie werden mit der ersten Propwash-Version neu erzeugt, die Stecker und
  Kabel ausblendet. Aufruf:

  ```sh
  JMP_SELFTEST_ONLY='power: Akku gesteckt|bildschirmfotos: Akku gesteckt' ./gradlew -PjmpScreenshots :fabric:runClientSelftest
  ```

- Im Log von Propwash: „Drohnen-Renderer: Akku-Stecker ausgeblendet: … Akku-Modelle mit Stecker, 16 Stacks mit
  _mounted“ (4 von Propwash, 12 von JMP) und keine Zeile „… wird nicht benutzt (…), Stecker bleiben sichtbar“ für
  `justmoreparts`.
