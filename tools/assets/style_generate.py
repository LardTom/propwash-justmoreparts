import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from jmpstyle import check, paths, templates, writer


def main(argv=None):
    parser = argparse.ArgumentParser(description="Erzeugt JMP-Teilmodelle im Propwash-Stil (Stil-API jmpstyle).")
    parser.add_argument("--category", action="append", choices=templates.CATEGORIES,
                        help="nur diese Kategorie (mehrfach möglich), Standard: alle")
    parser.add_argument("--only", action="append", help="nur diese Teil-Id (mehrfach möglich)")
    parser.add_argument("--parts", default=str(paths.PARTS), help="Ordner <kategorie>/<id>.json")
    parser.add_argument("--out", default=str(paths.PREVIEW_ASSETS),
                        help="Ziel-Ordner assets (Standard: build/style-preview/assets, --write: Mod-Ressourcen)")
    parser.add_argument("--write", action="store_true", help="in common/src/main/resources/assets schreiben (nur Kategorie-Besitzer)")
    parser.add_argument("--no-icons", action="store_true", help="items/<id>.json nicht schreiben")
    parser.add_argument("--oversize", choices=("icon", "scaled"), default="scaled",
                        help="Teile außerhalb der Modellgrenzen: verkleinertes Drohnen-Modell mit transformation (Standard) "
                             "oder nur Inventar-Symbol")
    parser.add_argument("--fragments", help="optional: Ordner für drone_paint.json/drone_layout.json-Fragmente")
    parser.add_argument("--no-clean", action="store_true", help="alte Dateien der erzeugten Teile nicht vorher löschen")
    parser.add_argument("--check-only", metavar="ORDNER", help="nichts erzeugen, nur einen assets-Ordner gegen die Stilregeln prüfen")
    args = parser.parse_args(argv)
    if args.check_only:
        problems = check.vendored_intact() + check.check(args.check_only)
        for line in problems:
            print(line)
        print(f"STIL-PRÜFUNG {'OK' if not problems else f'{len(problems)} Befunde'}")
        return 1 if problems else 0
    out = paths.ASSETS_ROOT if args.write else Path(args.out)
    categories = tuple(args.category) if args.category else templates.CATEGORIES
    parts = templates.load(args.parts, categories, set(args.only) if args.only else None)
    w = writer.Writer(out, icons=not args.no_icons, oversize=args.oversize)
    for category, key, definition in parts:
        if not args.no_clean:
            w.clean(category, key)
        templates.build_into(w, category, key, definition)
    report = w.finish(args.fragments)
    for category, key, note in report:
        if note != "modell":
            print(f"{category}/{key}: {note}")
    print(f"{paths.NS}: {len(report)} Teile im Propwash-Stil nach {out} ({len(w.out.written)} Dateien, "
          f"{len(w.out.paint_groups)} Lack-Varianten)")
    problems = check.vendored_intact() + check.check(out, keys={key for _, key, _ in parts})
    for line in problems:
        print(line)
    print(f"STIL-PRÜFUNG {'OK' if not problems else f'{len(problems)} Befunde'}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
