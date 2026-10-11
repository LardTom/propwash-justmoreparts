# Propwash: Just More Parts

**143 weitere echte Drohnenteile für Propwash FPV.** Vom 65-mm-Whoop über 2–3,5″-Toothpicks und Cinewhoops, 5″-Freestyle
und -Race und 6–8″-Long-Range bis zur 13″-X-Class: Motoren, Propeller, Akkus, Stacks, Videosysteme und Rahmen in neuen
Formen – Stretched-X, Squashed-X, Deadcat, H-Frame, Ducts und X-Class. Dazu **Sharecodes**: einen ganzen Bauplan mit
Lackierung, Flug-Tuning und OSD-Layout als kurzen Text kopieren und überall nachbauen.

> **Benötigt [Propwash FPV](https://modrinth.com/mod/propwash-fpv) 0.4.0 oder neuer** auf Client und Server. Das hier ist
> ein Addon: Es bringt Inhalte, geflogen wird mit Propwash.

---

## Ehrliche Teile, echte Physik

Just More Parts täuscht nichts vor. Jedes Teil ist ein Datenblatt mit echten Kennzahlen – Masse, Statorgröße und KV,
Wicklungswiderstand, Hitzegrenzen, Schub- und Leistungsbeiwerte, Blattsteifigkeit, Zellzahl, Kapazität, C-Rate,
Innenwiderstand, Radstand, Arm-Geometrie, Luftwiderstand – und **Propwashs eigenes Flugmodell macht daraus das
Flugverhalten**. Die Vor- und Nachteile entstehen von selbst:

- ein Zweiblatt fliegt länger, ein Fünfblatt schiebt mehr und zieht mehr Strom
- ein Li-Ion-Akku schwebt eine halbe Stunde und hat keinen Punch; ein Semi-Solid-Akku ist leicht, aber bei Vollgas
  überlastet
- ein 4S-Stack auf 6S brennt beim Scharfschalten durch
- Deadcat und H-Frame halten die Props aus dem Kamerabild; ein Stretched-X rollt schneller und flippt träger
- eine X-Class ist schwer und träge – und fliegt lange

**Shift** über einem Teil zeigt Kennzahlen, Vorteile, Nachteile und Einsatzzweck. Jede dieser Aussagen wird in einem
automatischen Prüfstand gegen Propwashs Analyse geprüft.

## Was drin ist

| | Anzahl | Spannweite |
|---|---|---|
| Motoren | 34 | 0702 bis 4214, mehrere KV-Stufen je Stator, neue Halterungen 31xx und 42xx |
| Propeller | 38 | 31 mm bis 13″, 2–5 Blätter, weiches/hartes PC, Bi-Blend, Glasfaser-/Carbonfaser-Nylon, Vollcarbon, Duct-Props |
| Akkus | 30 | 1S 250 mAh bis 8S 5000 mAh: LiHV, LiPo, Li-Ion (18650/21700, 6S2P), Semi-Solid |
| Zellchemie | 1 | Semi-Solid-LiHV (bis 4,45 V je Zelle) |
| Stacks/AIOs | 12 | 1S-Whoop-AIO bis 30×30-8S-Stack mit 100 A |
| Videosysteme | 7 | analog 25 mW bis 1,6 W, digital von Micro bis Long Range, mit dem Sichtfeld ihrer Kamera (110–133°) |
| Rahmen | 22 | Whoops, Toothpicks, Cinewhoops, 5″ True-X/Stretched/Squashed/Deadcat/H, Long Range 4–8″, X-Class 10/12/13″ |
| Presets | 22 | fertige, lackierte Drohnen für jede Klasse im eigenen Kreativ-Tab „Propwash: Just More Parts“ |

- **Überleben:** Jedes Teil hat ein Rezept (größere Teile kosten mehr) und liegt in Truhen vom Dorfhaus bis zur
  Endsiedlung; X-Class-Teile sind ungewöhnlich oder selten und liegen erst in späten Strukturen.
- **Eigene Modelle im Propwash-Stil** mit Lack-Varianten für jeden Motor, Propeller (mit Schadensstufen), Akku, Stack,
  Rahmen und jedes Videosystem – gebaut wie Propwashs eigene Teile und in der Hand, im Inventar, im Rahmen und am Boden
  genauso groß.
- **Flugschule:** zwei neue Lektionen, *Rahmenformen im Vergleich* und *Builds teilen*, und die Flugschul-Taste (M)
  öffnet über jedem Teil von Just More Parts die passenden Lektionen – wie bei Propwashs eigenen Teilen.
- Komplett auf **Deutsch und Englisch**.

## Sharecodes

Ein Sharecode ist ein kurzes Wort wie `PW21RSaEPg8Rt` (ein fertiges Preset; mit Tuning und Name meist 50–75 Zeichen) und trägt wahlweise **Teile, Lack, Flug-Tuning (samt Kamerawinkel der Drohne), OSD-Layout und Name**. Ältere Codes `PW1-…` funktionieren weiter.

- **An der Drohnen-Werkbank:** Knopf *Sharecode* (`</>`), Reiter *Exportieren* legt den fertigen Bauplan in die
  Zwischenablage; der Reiter *Importieren* zeigt die Drohne in 3D, prüft jedes Teil, nennt die Flugwerte und – im
  Überlebensmodus – eine Einkaufsliste und übernimmt dann Teile, Lack, Tuning oder OSD. Im Überlebensmodus braucht
  die Werkbank die Teile und Farbstoffe, im Kreativmodus entstehen sie von selbst.
- **Im Konfigurator** (`K`, Tab *Sharecode*): die Drohne in der Hand teilen, nur das Tuning oder nur den Lack
  übernehmen, ein OSD-Layout übernehmen oder im Kreativmodus die Drohne erzeugen.
- **Per Befehl:** `/jmp code export`, `/jmp code info <code>`, `/jmp code import <code>` und für Operatoren
  `/jmp code give <code> [spieler]`.

Leerzeichen, Zeilenumbrüche und Kleinbuchstaben sind egal, Tippfehler erkennt eine Prüfsumme, und Teile von Addons,
die der Server nicht hat, meldet der Import als unbekannt, statt abzubrechen.

## Kompatibilität

- Minecraft 26.3 · Fabric (Fabric API nötig) oder NeoForge · Java 25
- Propwash FPV 0.4.0 oder neuer, auf Client **und** Server
- Propwash-Welten, Drohnen und Einstellungen bleiben, wie sie sind. Entfernt man das Addon, behalten Drohnen seine Teile
  als *Unbekanntes Bauteil* und sind wieder vollständig, sobald es zurück ist.
