# Ausführliche Release-Notizen — Propwash: Just More Parts

`CHANGELOG.md` ist seit dem 02.10.2026 kurz, englisch und für Spieler geschrieben (Highlights, *New*, Kompatibilität).
Hier stehen die Einzelheiten, die dort keinen Platz haben: Zahlen aus dem Prüfstand, Namensbeispiele, Hinweise und
Verweise auf die Design-Dokumente. Teile im Einzelnen: [`design/parts-catalog.md`](design/parts-catalog.md); Namen:
[`design/part-naming.md`](design/part-naming.md); Sharecode: [`design/sharecode-pw2.md`](design/sharecode-pw2.md) (PW2) und [`design/sharecode.md`](design/sharecode.md) (PW1); Web-Export:
[`design/web-export.md`](design/web-export.md). Die Arbeitsschritte stehen in den Tages-Changelogs unter `changelogs/`.

---

## 1.1.1

Begleit-Release zu Propwash 0.5.2. Keine neuen Teile, keine Änderung an Speicherformaten oder Sharecodes; läuft mit
Propwash 0.4.0 bis 0.5.2 (Parts-API 1.4 oder neuer), gebaut und geprüft gegen Propwash 0.5.2.

### Akkus mit der Entladekurve von Propwash 0.5.2
- Die Semi-Solid-Chemie (`chemistry/lihv_ss.json`) übernimmt die neue LiPo-Ruhespannungskurve von Propwash 0.5.2
  (bei 50 % unverändert, darunter wie die üblichen LiPo-Tabellen). Alle anderen Akkus nutzen die Chemien von Propwash
  und folgen der neuen Kurve ohne Datenänderung; Flugzeiten der Presets meist unter +1 %, Li-Ion-Aufbauten bei
  aggressivem Fliegen bis +10 %.
- `PowerDataTest` prüft die Semi-Solid-Kurve als feste Werte.

### Gegen Propwash 0.5.2 geprüft
- Gebaut gegen Propwash 0.5.2 (`propwash_version`), die Prüfstände laufen mit dessen Flugmodell. Mit der neuen
  Entladekurve fliegen der 6″-Zweiblatt (`p152_40_2b_gfn`) und der Dreiblatt (`p152_33_3b_pc`) mit Li-Ion im Reiseflug
  fast gleich lang (+0,8 %), im Schweben bleibt der Zweiblatt 3 % vorn. Die Texte sagen jetzt „schwebt länger“ bzw.
  „schwebt kürzer“; der Vergleich Zweiblatt gegen Dreiblatt läuft für 6″ mit LiPo. Der 3,5″-Dreiblatt
  (`p89_30_3b_pc`) erreicht auf 6S 116 statt 115 km/h, der 5″-Freestyle-Standard im Rahmen-Prüfstand 142 km/h. Der
  2806,5/1300 bleibt leichter und schwebt 2,4 % länger als der 2807/1300, gemischt nur noch +0,9 %.
- Der Lizenz-Test prüft das englische `README.md` (Abschnitt License) und `README.de.md` (Abschnitt Lizenz).

### Sharecode: Halterung vorne
- PW1 und PW2 kennen den Zubehör-Platz 2 `front` (Propwash 0.5.2, `AccessorySlot.FRONT`). Er wird zur Laufzeit über
  `AccessorySlot#key` gefunden, deshalb läuft 1.1.1 weiter mit Propwash 0.4.0 bis 0.5.1. Codes ohne Zubehör vorne
  bleiben Byte für Byte gleich; ältere Versionen lesen Platz 2 und verwerfen ihn (`accessory_slot:2`).

### Sharecode: Race-Prop von Propwash 0.5.2
- `propwash:p51_29_3b` (5,1″ × 2,9″, 3 Blätter) steht als Nummer 43 am Ende der Prop-Tabelle von PW2 (44 Props);
  bestehende Codes bleiben Byte für Byte gleich ([`design/sharecode-pw2.md`](design/sharecode-pw2.md)).

## 1.1.0

**Optional für Spieler auf Servern ohne Just More Parts (mit Propwash 0.5.0):** Wer Just More Parts installiert hat, kann
Servern ohne Just More Parts beitreten. Propwash schaltet das Add-on für diese Sitzung beim Spieler ab (Teile,
Teil-Items, eigener Kreativ-Tab, Presets, Sharecode-Tab und Werkbank-Knopf, Lektionen „Rahmenformen im Vergleich“ und
„Builds teilen“) und zeigt „Propwash: Just More Parts ist auf diesem Server nicht installiert und für diese Sitzung
inaktiv.“; nach dem Trennen ist alles wieder da. Just More Parts braucht dafür keine Änderung: Es meldet keine eigenen
Netzwerk-Kanäle an und legt alle Ids im eigenen Namensraum an. Hat der Server Just More Parts, bleibt es für Spieler
Pflicht. Geprüft mit zwei Prozessen auf Fabric und NeoForge (Propwash `docs/design/client-only-addons.md`).

Geprüft gegen Propwash FPV 0.5.0 (Parts-API 1.5.0, Netzprotokoll 15) und gegen 0.4.0 (Rauchtests Client und Server
beider Loader gegen die veröffentlichten 0.4.0-Jars bestanden, Selbsttest NeoForge 38 von 39 Schritten; gegen 0.4.0
übersetzt unterscheiden sich die Klassen vom Release-Jar nur in der API-Nummer der Startmeldung); gebaut gegen 0.5.0.
Der eine Schritt, der gegen 0.4.0 scheitert, ist die Stil-Prüfung „Größe je Ansicht im Bereich der Propwash-Teile“: Seit
den Hand-Größen (unten) zeigt Just More Parts seine Teile in der Hand nach ihrer echten Größe wie Propwash 0.5.0;
Propwash 0.4.0 zeigt seine eigenen Teile noch in der alten Einheitsgröße, deshalb liegen die Add-on-Teile in der Hand
dort kleiner als die eingebauten. Das ist nur Darstellung, Laden, Bauen, Fliegen und Server sind davon nicht betroffen. Fasst den nie veröffentlichten internen Stand 1.0.1 zusammen; neue Sharecodes sind PW2 wie in 1.0.0. Inhalt und Teile-Daten sind gegenüber 1.0.0 unverändert bis auf die Draufsicht zweier Whoop-Rahmen (unten); die Modelle der Rahmen mit Ducts sind neu erzeugt.

### Whoop-Rahmen: Widerstand von oben nach Rumpf und Blättern
- `whoop_65_lite`, `whoop_75_duct` und `whoop_85_duct` tragen `aero.top_area_m2` 0,0039, 0,0046 bzw. 0,0065 m² (1.0.0:
  2,5 × Stirnfläche = 0,0085, 0,01125 bzw. 0,014 m²; der nie veröffentlichte Stand 1.0.1 hatte für die beiden kleinen
  0,0058 und 0,0087 nach dem ganzen Umriss). Herleitung wie beim eingebauten 65-mm-Whoop von Propwash 0.5.0: Rumpf
  (Duct-Ringe, Motoren, Streben, Mitte) × 1,2 plus Blattfläche der Props × 1,66, dieselbe Regel, die 5″, 7″ und
  Cinewhoop von Propwash nachrechnet (Propwash `docs/design/realism/flight-feel-gravity.md` §10). Der Umriss zählte die
  Prop-Scheiben in den Ducts wie eine volle Platte; beim 85-mm-Whoop lag 2,5 × Stirnfläche sogar über einer vollen
  Platte seines Umrisses.
- Preset `whoop75_1s` (gegen 1.0.0): flach im Leerlauf 6,4 → 10,0 m/s, nach 1 s Fall 5,71 → 7,23 m/s, Topspeed
  41 → 59 km/h, Reisen 5:58 → 6:41, gemischt 4:24 → 4:40, aggressiv 3:29 → 3:36. Preset `whoop85_2s`: flach 7,5 →
  11,0 m/s, nach 1 s 6,26 → 7,43 m/s, Topspeed 57 → 79 km/h, Reisen 7:20 → 7:43, gemischt 5:26 → 5:35, aggressiv
  4:19 → 4:23 (mit dem Steigflug-Schub von Propwash 0.5.0, Abschnitt unten). Schweben, Schwebegas, Ströme und Schub/Gewicht gleich.
- Prüfstand: Korridor Whoop-Topspeed 35–60/65 → 35–85 km/h (75 mm auf 2S mit 0802 15500 KV jetzt 76 km/h,
  85 mm 2S 79 km/h; eine echte Meteor75 Pro II auf 1S schätzt ein Testbericht auf rund 80 km/h), Reisezeit bis 9 statt
  8 min, Aussage des 0702-27000-KV-Motors „mehr Topspeed (57 statt 51 km/h)“ (1.0.0: 41 statt 38). Die Aussage
  „Toothpick 2″ schneller als der 85-mm-Whoop“ galt nur mit dem zu hohen Whoop-Widerstand; der 85-mm-Whoop hat auf 2S
  mehr Schub/Gewicht (6,8 statt 6,3), beide sind jetzt etwa gleich schnell (79 und 74 km/h).

### Steigflug-Schub von Propwash 0.5.0: 3,5″-Props
- Propwash 0.5.0 rechnet den Schub im Steigflug mit Impuls- und Blattelementtheorie: Steigt der Prop entlang seiner
  Achse, sinkt die induzierte Geschwindigkeit mit, und er verliert nur etwa halb so viel Schub wie mit der bisherigen
  linearen Formel (Propwash `docs/design/realism/flight-feel-gravity.md` §11). Im Vorwärtsflug blendet der Effekt
  aus. Die Presets fliegen damit 0,5–3 % länger auf Reise und 0–3 km/h schneller; Schweben, Schwebegas, Ströme und
  Fall bleiben gleich.
- `p89_30_3b_pc` c_p 0,0845 → 0,0855 und `p89_25_5b_pc_duct` c_p 0,3012 → 0,3048 (beide × 1,012, Gütezahl 0,395 →
  0,390 bzw. 0,364 → 0,360): `toothpick35_6s` lag schon vorher am Rand der Toothpick-Fenster (gemischt 9:26 bei
  höchstens 9:27, Reise 12,9 bei höchstens 13,0 min) und wäre mit dem neuen Steigflug-Schub darüber gelandet (9:29
  und 13,0). Beide 89-mm-Props bekommen denselben Faktor, damit der Dreiblatt im 3,5″-Cinewhoop weiter länger schwebt
  als der Fünfblatt (Blattzahl-Reihenfolge, Teilekatalog §13.4). `toothpick35_6s` jetzt Schweben 15:00, Reise 12:56,
  gemischt 9:24; `cinewhoop35_6s` Schweben 11:21, gemischt 8:35.

### Ducts wie echte
- Whoop 65 Lite, Whoop 75, Whoop 85, Cinewhoop 2,5″ und 3,5″ mit neu erzeugten Ducts nach denselben Regeln wie die
  Ducts von Propwash 0.5.0 (`tools/assets/jmpstyle/duct.py`): Wand 0,35 + 0,016 × Prop in mm (Whoop 75 0,99 statt 1,3 mm,
  Cinewhoop 3,5″ 1,79 statt 2,39 mm), schmale Lippe bzw. schmaler Rand, 24 Segmente, Oberkante 1–1,2 mm über den
  Blattspitzen. Mit Propwash 0.5.0 erscheinen sie auch im FPV-Bild.
- Becher, Speichen, Arme, Pads und Ducts sitzen auf der Motorhöhe, die Propwash für Rahmen ohne Layout annimmt (1,5 mm
  bei Whoops, 2,5 mm bei Cinewhoops); vorher schwebten die Motoren über ihren Haltern und die Props ragten 1–2,7 mm aus den
  Ducts. Teile-Daten, Flugwerte und Web-Export bis auf die Modelle und Texturen dieser Rahmen unverändert.

### Akku unter dem Race-Rahmen
- `race_5_stretched` trägt den Akku wie echte Race-Drohnen unter dem Rahmen (`battery.on_top: false`, `plate_mm` −2,8 =
  Bodenplatte 2 mm + Gummipad 0,8 mm, vorher oben bei 24), passend zum eingebauten `race_5` von Propwash 0.5.0. Als Race
  zählen nur Rahmen mit `role: race`; Toothpicks (auch der gestreckte 3,5″) und X-Class behalten den Akku oben.
- Flugwerte (nur Daten): `race5_4s` Schwerpunkt 18,7 → −3,6 mm, Props 18,1 mm über dem Schwerpunkt, Ixx −3 %, Izz −6 %,
  Roll-Autorität +6 %, Auto-PIDs Roll 38/45/30 → 36/42/28; `race5_6s` Schwerpunkt 21,4 → −5,5 mm, Roll-PIDs 30/36/24 →
  28/33/23. Schwebegas, Topspeed und Flugzeit bleiben (Propwash `tools/calibration.sh`: `calibration.txt` gleich,
  `motion.txt`/`fall.txt` nur in den `race_5_stretched`-Zeilen neu).
- Bild: Riemenschlitze und Gummipad an der Bodenplatte, Deckplatte ohne Schlitze; hängende Varianten `<id>_under` (+ Lack)
  für alle 14 Akkus mit Gurt, Gurt um den Boden, Schnalle und Etikett unten (`tools/assets/jmpstyle/battery.py`,
  `build_under`; `propwash_style/batteries.py` aus Propwash 0.5.0 neu kopiert). Mit Propwash 0.4.0 hängt der Akku auch
  unter dem Rahmen, wird dort aber mit dem normalen Modell gezeichnet (Gurt oben). Hilfezeile „+ Akku unten: tiefer
  Schwerpunkt“. JUnit `RaceBatteryDataTest`.

### Kamera über den Ducts
- Mit Propwash 0.5.0 erscheinen die Ducts im FPV-Bild. Bei den Whoops 65 Lite, 75 und 85 mm und den Cinewhoops 2,5″ und 3,5″ saß
  das Objektiv zwischen den vorderen Ducts und unter deren Oberkante: Im Bild standen zwei Wände über die volle Höhe
  (38–75 % des Bildes, bei vier Rahmen auch bei 40° Uptilt). Jetzt sitzt die Kamera wie bei echten Drohnen über den Ducts, die Ducts
  liegen in den unteren Ecken: Whoop 65 Lite 22/10/0 %, Whoop 75 21/11/0 %, Whoop 85 21/12/0 %, Cinewhoop 2,5″ 19/5/0 %, Cinewhoop 3,5″ 16/15/0 %
  bei 0/20/40° Uptilt (vorher 38/25/10, 65/60/56, 46/32/16, 75/66/57, 69/57/46 %).
- `camera.pos`: Whoop 65 Lite (0, 18,5, −17,5) mit Kamerawangen vom Boden bis zur Kamera, Whoop 75 (0, 19, −24), Whoop 85 (0, 21, −27) mit höherer Haube; Cinewhoop 2,5″ (0, 29,5, −48) und 3,5″
  (0, 34,8, −52) mit neuem Halter über den Ducts vor dem Akku. Der Action-Cam-Halter oben sitzt darüber
  (2,5″ y 25 → 40,5 mm, 3,5″ y 31 → 50,5 mm). Vorher steckten Kamera, Linse und Käfig teils in Ducts, Props und Akku.
- Props im Bild (Prüfstand, Werkbank): Whoop 75 8,6 → 6,0 %, Whoop 85 6,1 → 7,4 %, Cinewhoop 2,5″ 6,7 → 2,4 %,
  Cinewhoop 3,5″ 6,9 → 13,3 % (Propwash warnt ab 30 %). Flugwerte gleich; eine Action-Cam oben liegt höher.
  Regeln und Tabelle: `design/asset-style.md` (Kamerahalter der Rahmen mit Ducts).

### Teile in der Hand
- Alle Teile-Modelle sind mit dem Generator von Propwash 0.5.0 neu erzeugt (vendierte `kit.py`/`addon.py`): Teile werden
  in der Hand nach ihrer echten Größe gezeigt, 9 bzw. 7 px × f, f = (L / 180 mm)^0,6, begrenzt auf 0,25–1. Erste Person
  vorher → jetzt: Motoren 0702 bis 1202.5 0,56 → 0,14, 2207 0,56 → 0,18, 3115 0,56 → 0,23, 4214 0,56 → 0,26 (dritte Person
  0,44 → 0,11/0,14/0,18/0,20); Whoop-Rahmen 75 mm 0,39, Video-Einheiten, Stacks und kleine Props entsprechend kleiner;
  Rahmen und Props ab 180 mm (5″) gleich. Akkus behalten in der ersten Person ihre Größe; Zweiblatt-Props liegen in der
  linken Hand nicht mehr quer (vorher bis 0,9 Block breit).
- Geändert haben sich nur die Hand-Einträge (`display.thirdperson_righthand`, `display.firstperson_righthand`) der 864
  Modell-Dateien; GUI, Boden, Item-Rahmen, Regal, Kopf, Texturen, Teile-Daten und Item-Definitionen sind gleich.
- Bildschirmfotos aus Propwashs Selbsttest mit diesem Stand im `mods`-Ordner: `docs/screenshots/hand_first_person_m1103_8000.png`,
  `…_m4214_400.png`, `hand_third_person_m1103_8000.png`, `…_m4214_400.png` (neben einer Eisenspitzhacke) und
  `hand_lineup_motors_1–3.png` (alle Motorgrößen von 0702 bis 4214 auf Rüstungsständern neben Spitzhacke und Stein).
- Die Stil-Prüfung im Selbsttest („Größe je Ansicht im Bereich der Propwash-Teile“) teilt die Größe in der Hand durch
  denselben Faktor f (L aus dem Drohnen-Modell, Akkus in der ersten Person ohne); vorher fielen die großen Motoren durch,
  weil Propwash selbst keinen Motor über 2807 hat (4214 in der ersten Person 0,288 gegen 0,150–0,224).

### Sharecode bei großer GUI-Skala

- **Sharecode-Fenster:** Ein langer Code scrollt im Kasten mit dem Mausrad (Scrollleiste) statt mit „…“ zu enden; die
  Werte der Drohne scrollen statt wegzufallen.
- **Tab „Sharecode“ im Konfigurator (Fix vom 11.10.):** Die Zeilen „In der Hand: …“, die Meldung nach dem Kopieren
  und die Zusammenfassung des eingefügten Codes liefen als Laufschrift, wenn sie nicht in die Zeile passten – bei
  320 × 240 Punkten stand von „10″ X-Class Frame · 3115 Motor, 900 KV · 6S …“ in einem Standbild nur die Mitte. Sie
  brechen jetzt um; der Tab rückt alles darunter nach (er baut sich neu auf, sobald sich die Zeilenzahl ändert, und
  gibt dem Eingabefeld Fokus und Cursor zurück). Benutzt nur, was es schon in Propwash 0.4.0 gibt.
- Der Selbsttest öffnet den Tab dafür bei 640 × 480 Bildpunkten mit Skala 2 auf Deutsch und Englisch
  (`jmp_config_tab_small_de.png`, `jmp_config_tab_small.png`) und verlangt, dass die Zusammenfassung umbricht, keine
  Zeile breiter als der Tab ist und die Knöpfe darunter nicht im Text liegen.

### Lizenz und öffentlicher Quellcode
- Der Quellcode erscheint öffentlich auf GitHub, als Stand je Release ohne die interne Entwicklungsgeschichte; 1.0.0
  bleibt „All Rights Reserved“.
- Kern (Code, Teile-Daten, Assets, Build-Skripte, Werkzeuge): PolyForm Shield License 1.0.0 mit der Pflichtzeile
  `Required Notice: Copyright (c) 2026 lardtom` (`LICENSE`, `NOTICE`) und denselben Zusatzrechten wie Propwash
  (Modpacks, Ausstiegsklausel nach zwölf Monaten ohne Release und ohne Maintainer-Aktivität → zusätzlich MIT).
- MIT (`LICENSE-MIT`): die Doku unter `docs/` (ohne `docs/screenshots/` und `docs/asset-sheets/`), `README.md`,
  `CHANGELOG.md`, `CONTRIBUTING.md`. Das Sharecode-Format (`docs/design/sharecode.md`) ist damit frei nachbaubar.
- Mod-Metadaten `LicenseRef-PolyForm-Shield-1.0.0`; jedes Jar enthält `LICENSE`, `LICENSE-MIT` und `NOTICE`.
- Beiträge: `CONTRIBUTING.md`, `CLA.md` (gleicher Beitragsvertrag wie bei Propwash), Pull-Request-Vorlage.

---

## 1.0.0

Minecraft 26.3 · Fabric (Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3) und NeoForge (26.3.0.10-beta+) · Java 25 ·
benötigt Propwash FPV 0.4.0 oder neuer (Parts-API 1.4) · Pflicht auf Client und Server.

### Neue Teile (143)
- **34 Motoren** von 0702 bis 4214 mit mehreren KV-Stufen je Stator (0702, 0802, 1102, 1103, 1202,5, 1404, 1505, 2004,
  2207, 2207,5, 2306,5, 2507, 2806,5, 2807, 3115, 4214) mit echten Massen, Strömen, Wicklungswiderständen und
  Hitzegrenzen; neue Motorhalterungen 31xx und 42xx für X-Class.
- **38 Propeller** von 31 mm bis 13″ mit 2 bis 5 Blättern in PC, weichem und hartem PC, Bi-Blend, Glasfaser- und
  Carbonfaser-Nylon und Vollcarbon, dazu zwei Duct-Props für Cinewhoops. Mehr Blätter schieben mehr und ziehen mehr
  Strom, weniger Blätter fliegen länger, mehr Steigung bringt mehr Topspeed, weiches PC übersteht Crashs und wackelt im
  Propwash.
- **30 Akkus** von 1S 250 mAh bis 8S 5000 mAh: LiHV, LiPo, Li-Ion (18650/21700, auch 6S2P) und die neue Zellchemie
  **Semi-Solid-LiHV** (bis 4,45 V je Zelle, leichter als LiPo, aber bei Vollgas überlastet).
- **12 Stacks und AIOs** vom 1S-Whoop-AIO bis zum 30×30-8S-Stack mit 100 A. Falsche Kombinationen meldet die Werkbank
  (Überspannung, ESC zu schwach), ein 4S-Stack auf 6S brennt beim Scharfschalten durch.
- **7 Videosysteme** – analog 25 mW bis 1,6 W, digital von Micro bis Long Range – mit eigener Latenz, Reichweite,
  Bildqualität, echter Leistungsaufnahme und dem Sichtfeld ihrer Kamera: Firefly Nano 126°, Racecore 800 125°,
  Longshot 1600 110°, Pixel Micro 126°, Swift Link 133°, Clarity HD Pro und Horizon LR 130° (horizontal, aus echten
  Kameras recherchiert). Seit Propwash 0.4.0 kommt das FPV-Sichtfeld wie in echt von der Kamera im Videosystem.
- **22 Rahmen** in neuen Formen: True-X, Stretched-X, Squashed-X, Deadcat und H-Frame (kaum oder keine Props im Bild),
  Ducts für Whoops und Cinewhoops, Long Range 4–8″ und X-Class 10, 12 und 13″ (auch als H).
- **22 Presets** vom Whoop 75 bis zum H-Frame 13″ (X-Class) mit eigener Lackierung, darunter der Deadcat 7″ auf 8S.
- **Eigener Kreativ-Tab „Propwash: Just More Parts“** direkt hinter dem Propwash-Tab: zuerst die Presets, dann Rahmen,
  Motoren, Props, Stacks und Akkus, Video.

### Modelle, Größen, Item-Rahmen
- Jedes Teil hat ein eigenes Modell im Propwash-Stil (gleiche Bauweise, Pixeldichte, Farbpalette und Schattierung wie
  Propwashs Teile, echte Größe und Charakter; X-Class-Rahmen an der Drohne in voller Größe) mit Lack-Variante
  ([`design/asset-style.md`](design/asset-style.md)).
- Die Teile liegen im Inventar, in der Hand, am Boden und im Rahmen so groß wie die Propwash-Items: am Boden höchstens
  einen halben Block (Akkus 0,37 Block lang, Motoren wie ein fallengelassener Block), Akkus im Item-Rahmen quer mit
  Aufschrift, Videosysteme im Profil und Stacks flach statt bis 1,5 Blöcke aus der Wand.
- JMP-Drohnen (auch Deadcat, H-Frame und X-Class), Rahmen und Props hängen im Item-Rahmen und Leuchtrahmen flach an der
  Wand wie Propwashs eigene, Oberseite zum Betrachter und Nase oben; die acht Drehstufen drehen sie in der Wandebene.
  Im Regal sind Rahmen und Props zum Betrachter geneigt.
- Namen und eine Shift-Hilfe mit Kennzahlen, Vor- und Nachteilen und Einsatzzweck auf Deutsch und Englisch; jede
  Aussage ist im Prüfstand mit Propwashs Analyse belegt.

### Teilnamen
- Alle Namen folgen demselben Schema wie die eingebauten Teile (Kenngröße und Gerät, Zusätze in Klammern):
  `6S-Akku 2200 mAh` / `6S 2200 mAh Battery`, `6S-Akku 3000 mAh (Li-Ion)`, `1404-Motor, 3800 KV` /
  `1404 Motor, 3800 KV`, `40-mm-Propeller (3 Blätter, 1,7″ Steigung, PC)` / `40 mm Propeller (3-blade, 1.7″ pitch, PC)`,
  `30×30-FC/ESC-Stack (3–6S, 60 A)`, `5-Zoll-Freestyle-Rahmen (Squashed-X)`, `Digitales HD-Videosystem (Clarity HD Pro)`.
- Kurznamen für Bestenliste, HUD und Presets ebenso einheitlich (`Race 5″ Slim`, `Deadcat 7″`, `H-Frame 13″`).
- Die graue Kurzbeschreibung unter dem Namen beginnt bei jedem Teil mit einer Kurzkennung aus Name und Daten, beim Prop
  mit dem Material wie im Namen (`5,1″-Dreiblatt, hartes PC`, `glass-fiber nylon`/`Glasfaser-Nylon`, `Carbon`), beim
  Rahmen mit Größe und Klasse und der Form vorn im Einsatz (`5″-Freestyle-Rahmen – Deadcat, Bild ohne Props`,
  `13″ X-Class frame – H-frame, stiff, no props in view`).

### Realistisch fliegen
- Alle Teile tragen realistische Daten; das Flugverhalten rechnet Propwash selbst. Die Werte sind gegen
  Hersteller-Prüfstände und echte Aufbauten abgeglichen: Schub/Gewicht, Schwebegas, Topspeed, Vollgasstrom und die
  Motortemperatur nach einer Minute Vollgas. Beispiele: Cinewhoop 2,5″ · 4S 87 km/h bei 33 % Schwebegas und 68 °C,
  Cinewhoop 3,5″ · 6S 89 km/h, 594 g Schub je Motor und 82 °C, Whoop 75 · 1S 35 % Schwebegas.
- Werkbank, Sharecode-Fenster, `/jmp code info` und Web-Konfigurator zeigen mit Propwash 0.4.0 vier Flugzeiten:
  Schweben, Reisen, gemischt (Hauptwert, typisches Fliegen der Klasse) und aggressiv. Beispiele: 65-mm-Whoop mit
  0702 27000 KV, 31-mm-Vierblatt, 1S 300 mAh LiHV und Digitalvideo Schweben 3:19, Reisen 3:01, gemischt 2:02,
  aggressiv 1:39 min (mit 23000 KV 16–21 % länger, mit Dreiblatt 11–17 % länger); „Deadcat 7″ · 8S“ Schweben 19,5,
  Reisen 11,8, gemischt 8,0, aggressiv 5,7 min; Li-Ion-Long-Range gemischt 16–18 min; 5″-Race auf 6S 1300 gemischt gut
  3 min.
- Der 3,5″-Dreiblatt trägt Beiwerte nach derselben Regel wie die übrigen Dreiblatt-Props: Toothpick 3,5″ · 6S mit
  S/G 8,9 bei 24 % Schwebegas, 115 km/h und 9:26 min gemischt; im 3,5″-Cinewhoop schiebt der Duct-Fünfblatt 43 % mehr
  und zieht doppelt so viel Strom, der Dreiblatt fliegt länger. Die kleinen Toothpick-Motoren (1202,5/5500 KV,
  1505/2500 KV) werden nach einer Minute Vollgas so warm wie auf echten Prüfständen (79 bzw. 78 °C).

### Kamera und Props im Bild
- „Props im Bild“ rechnet genau wie das FPV-Bild im Spiel – mit dem Sichtfeld der Kamera und dem Standard-Kamerawinkel
  des Rahmens (Deadcat 7″ ehrlich 2 %, Deadcat 5″ und H-Frame 5″ 0 %).
- Jeder Rahmen hat einen Kamerahalter mit Bereich (Whoops 10–35°, Toothpicks 10–40°, Cinewhoops 0–35°, 5″ Freestyle
  0–50°, Race 15–70°, Long Range 0–40°, X-Class 0–50°); den Winkel der Drohne stellt man in Propwashs Wartungsmenü ein.
- Wie viel Prop man sieht, hängt an Rahmenform und Kamerahalter: Das Objektiv sitzt in der Kamera wenige Millimeter
  unter den Props (Whoops knapp über den Ducts) statt bis zu 28 mm darüber. Der Race-Stretch mit den Vorderprops nah an
  der Kamera zeigt am meisten (rund 11–12 %, vorher 0–2 %), True-X 5″ rund 8 %, Squashed 3 %, Deadcat und H-Frame
  nichts; Whoops, Cinewhoops und Toothpicks 4–8 %, Long Range 6–8 %, X-Class 2–7 %. Reine Darstellung und Analyse: alle
  Drohnen fliegen wie vorher.

### Überleben
- Rezepte für alle Teile, im Aufwand nach Größe gestaffelt; sie erscheinen im Rezeptbuch, sobald man ein Propwash-Teil
  besitzt.
- Truhen-Beute in Dörfern, Minen, Schiffswracks, Verliesen, Außenposten, Tempeln, Festungen, Prüfungskammern, antiken
  Städten und Endsiedlungen. X-Class- und Semi-Solid-Teile sind ungewöhnlich oder selten und liegen erst in späten
  Strukturen. Die Einspeisung läuft auf Fabric und NeoForge gleich über Propwashs Beute-API (`PropwashApi.loot()`),
  ohne eigene Loader-Anbindung.

### Sharecodes
- Bauplan, Lackierung, Flug-Tuning samt Kamerawinkel der Drohne, OSD-Layout und Name als kurzer Text (`PW2…`) teilen
  und nachbauen. Seit dem Neubau vom 06.10.2026 schreibt 1.0.0 das Format PW2: ein Preset rund 18 statt 191 Zeichen,
  mit 19 Tuning-Werten und Name rund 72 statt 344, mit OSD-Layout rund 115 statt 1597 (alle 27 Presets gemessen,
  `design/sharecode-pw2.md` §1). Der Code ist ein Wort aus Buchstaben und Ziffern ohne Bindestriche (Doppelklick
  markiert ihn ganz), Groß- und Kleinschreibung zählt. Codes `PW1-…` aus den Testversionen werden weiter gelesen;
  die Website liest PW2 ab dem 06.10.2026. Codes ohne Kamerawinkel (ältere oder Drohnen mit Standardwinkel) setzen beim Übernehmen den Standard
  des Rahmens.
- Werkbank: Knopf **„Sharecode“** („Code“, wenn es eng wird, sonst ein `</>`-Symbol; der Werkbank-Titel bleibt lesbar)
  mit den Reitern „Exportieren“ und „Importieren“: Vorschau, Teile-Check, Flugwerte, Einkaufsliste für den
  Überlebensmodus (zählt wie der Server) und Auswahl, was übernommen wird. Der Name aus dem Code kommt mit dem Bauplan.
- Konfigurator-Tab **„Sharecode“**: gehaltene Drohne teilen, nur Tuning oder nur Lack übernehmen (mit den
  Farbstoff-Kosten vorab), OSD-Layout übernehmen (wird vorher geprüft), im Kreativmodus eine Drohne aus dem Code
  erzeugen.
- Gründe, warum sich eine Drohne nicht teilen lässt, erscheinen in der Sprache des Spielers.
- Befehl `/jmp code export|import|info|give` (auch `/justmoreparts …`).
- Hinweise: Teile anderer Addons in einem Sharecode, die auf dem eigenen Server fehlen, meldet der Import als
  „unbekannt“. Beim Import an der Werkbank wird der Name der Drohne nicht übernommen (Propwash kennt dafür keine
  Ebene); der Befehl und „Drohne erzeugen“ übernehmen ihn.

### Flugschule
- Neue Szenen **„Rahmenformen im Vergleich“** und **„Builds teilen“**.
- Flugschul-Taste (M) über jedem JMP-Teil: öffnet wie bei Propwashs eigenen Teilen die passenden Lektionen. Jedes Teil
  zeigt „Drohne bauen an der Werkbank“ und „Überlebensmodus: Teile herstellen“, dazu Rahmen „Rahmenformen im
  Vergleich“, Motoren und Props „Props wechseln und Schäden“, Akkus „Akku tauschen und laden“, Stacks „Konfigurator:
  Rates, PID, Modi“ und Videosysteme „Videosysteme und Brillen“. Die JMP-Presets öffnen zusätzlich „Builds teilen“.

### Web-Konfigurator
- Web-Export (`webExport`) mit allen Teilen, Presets, Varianten, Analysewerten (vier Flugzeiten, Ströme, Landespannung,
  Grundlast, Motortemperaturen), Beschriftungen wie in der Werkbank (de/en), `camera_view` für eine Kamera-Vorschau
  (mit dem Sichtfeld der Kamera und dem Bereich des Kamerahalters), Sharecode-Spezifikation und Prüfvektoren
  ([`design/web-export.md`](design/web-export.md)).
