"""Package the current built Debug APK and portable Android Studio sources."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import zipfile

root = Path(__file__).resolve().parents[1]
version = re.search(r'versionName\s*=\s*"([^"]+)"', (root / "app/build.gradle.kts").read_text()).group(1)
out = root / "artifacts"
out.mkdir(exist_ok=True)
source = out / f"JayArchive-{version}-source.zip"
files = [root / name for name in (
    "README.md", "SOURCES.md", "ACCEPTANCE.md", ".gitignore", "build.gradle.kts",
    "settings.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "app/build.gradle.kts",
)]
for folder in ("gradle", "app/src", "scripts"):
    files.extend(p for p in (root / folder).rglob("*") if p.is_file() and "__pycache__" not in p.parts)
with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in sorted(set(files)):
        archive.write(path, "JayArchive/" + path.relative_to(root).as_posix())
with zipfile.ZipFile(source) as archive:
    assert archive.testzip() is None
    assert "JayArchive/gradle/wrapper/gradle-wrapper.jar" in archive.namelist()
    studio = json.loads((root / "app/src/main/assets/albums.json").read_text(encoding="utf-8"))["albums"]
    extra = json.loads((root / "app/src/main/assets/extended-albums.json").read_text(encoding="utf-8"))["albums"]
    assert len([n for n in archive.namelist() if "/assets/covers/" in n]) == len(studio) + len(extra)
apk = out / f"JayArchive-{version}-debug.apk"
shutil.copy2(root / "app/build/outputs/apk/debug/app-debug.apk", apk)
manifest = {path.name: {"bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
            for path in (apk, source)}
(out / f"delivery-manifest-{version}.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print(json.dumps(manifest, indent=2))
