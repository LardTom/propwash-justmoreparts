import os
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
TOOLS = HERE.parent
ROOT = HERE.parents[2]
STYLE = TOOLS / "propwash_style"
NS = "justmoreparts"
RES = ROOT / "common/src/main/resources"
ASSETS_ROOT = RES / "assets"
PARTS = RES / "data" / NS / "propwash_parts"
BUILD = ROOT / "build"
PREVIEW_ASSETS = BUILD / "style-preview" / "assets"

if str(STYLE) not in sys.path:
    sys.path.insert(0, str(STYLE))


def gradle_properties():
    values = {}
    file = ROOT / "gradle.properties"
    if file.exists():
        for line in file.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def propwash_jar():
    props = gradle_properties()
    mc = props.get("minecraft_version", "26.3")
    version = props.get("propwash_version", "0.4.0")
    artifact = f"propwash-common-{mc}"
    jar = Path.home() / ".m2/repository/de/lardtom" / artifact / version / f"{artifact}-{version}.jar"
    return jar if jar.exists() else None


def propwash_assets(explicit=None):
    candidates = []
    if explicit:
        candidates.append(Path(explicit).expanduser())
    env = os.environ.get("PROPWASH_ASSETS")
    if env:
        candidates.append(Path(env).expanduser())
    jar = propwash_jar()
    if jar is not None:
        candidates.append(jar)
    for base in ROOT.parents:
        candidates.append(base / "propwash" / "common/src/main/resources/assets")
    for candidate in candidates:
        if candidate.is_dir() and (candidate / "propwash").is_dir():
            return candidate
        if candidate.is_dir() and (candidate / "assets/propwash").is_dir():
            return candidate / "assets"
        if candidate.is_file() and zipfile.is_zipfile(candidate):
            return candidate
    raise SystemExit("Propwash-Assets nicht gefunden: --propwash-assets <Ordner oder Jar> oder PROPWASH_ASSETS setzen "
                     "(Standard: propwash-common aus mavenLocal)")
