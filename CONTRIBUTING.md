# Contributing to Propwash: Just More Parts

Thanks for helping! Propwash: Just More Parts (JMP) is source-available: you can read the code, build it and send
pull requests. New parts with good real-world data, fixes, translations and model improvements are all welcome.

## Before you start

- For anything bigger than a small fix, open an issue first. New parts need real data (mass, motor and prop figures,
  battery chemistry, frame geometry) with a source, because Propwash computes the flight behaviour from them.
- Every pull request needs the contributor agreement: tick the box in the pull request template, see
  [`CLA.md`](CLA.md). It is short and you keep the copyright in your work.
- Contributions are licensed like the files they change: the core under the PolyForm Shield License 1.0.0
  ([`LICENSE`](LICENSE)), the documentation under the MIT License ([`LICENSE-MIT`](LICENSE-MIT)).

## Building and testing

You need a Java 25 JDK (set `JAVA_HOME` if your default Java is older) and Propwash FPV in your local Maven repository:
run `./gradlew publishToMavenLocal` once in a checkout of Propwash FPV (the version in `propwash_version` of
`gradle.properties`).

| Command | What it does |
|---|---|
| `./gradlew build` | builds both loaders and runs the JUnit tests (including the part benches) |
| `./gradlew :fabric:runClient`, `:neoforge:runClient` | development client with Propwash (starts muted) |
| `./gradlew :fabric:runClientSelftest`, `:neoforge:runClientSelftest` | the in-game self-test; must stay green |
| `./gradlew :fabric:runClientSmoke`, `:fabric:runServerSmoke` (same for `:neoforge`) | start-up smoke tests for client and server |
| `python3 tools/parts_sync.py --write`, `--check` | item models, tags and recipe unlocks from the part files, then the checks |

Run only one Minecraft instance at a time.

## Rules

The full rules are in [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md) (German). In short:

- **No comments in code** (`common/src`, `fabric/src`, `neoforge/src`, tests and tools included); explanations belong
  in `docs/` or in commit messages. `python3 tools/strip_comments.py common/src fabric/src neoforge/src` removes them.
- JMP uses **only the public Propwash API** (`de.lardtom.propwash.api` and the classes listed in the conventions): no
  other Propwash packages, no mixins, no reflection on Propwash.
- Parts are JSON files under `common/src/main/resources/data/justmoreparts/propwash_parts/<category>/`, with values and
  names following [`docs/design/parts-catalog.md`](docs/design/parts-catalog.md) and
  [`docs/design/part-naming.md`](docs/design/part-naming.md).
- Text that players see goes through `common/src/lang/<module>/en_us.json` and `de_de.json`. If you do not speak
  German, put the English text into both files and say so in the pull request.
- Models follow Propwash's style ([`docs/design/asset-style.md`](docs/design/asset-style.md)).
- Only your own work or material under a compatible license; name third-party material and its license in the pull
  request and in [`NOTICE`](NOTICE). Never commit build outputs, personal data or local paths.

Issues and pull requests in English are welcome; the developer documentation in `docs/` is in German.
