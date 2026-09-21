"""Build a small activity JAR against the pinned deployed camera runtime."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
BASE_HASH = "441ecf88afaeb9645f4ae18f1d2068aa6972e86d0657101021cab6eaa32e84c0"


def build():
    base = ROOT / "vendor/llmcameraagent.jar"
    digest = hashlib.sha256(base.read_bytes()).hexdigest()
    if digest != BASE_HASH:
        raise RuntimeError("The pinned deployed runtime changed; review it before rebuilding: " + digest)
    javac = os.environ.get("JAVAC") or shutil.which("javac")
    classes = ROOT / "build/classes"
    classes.mkdir(parents=True, exist_ok=True)
    subprocess.run([javac, "--release", "8", "-Xlint:-options", "-cp", str(base), "-d", str(classes),
                    *map(str, sorted((ROOT / "src").rglob("*.java")))], check=True)
    out = ROOT / "build/fcdsrepresentationagent.jar"
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as jar:
        jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nMain-Class: basilica2.myagent.operation.NewAgentRunner\nClass-Path: llmcamera-runtime.jar\n\n")
        for file in classes.rglob("*.class"):
            jar.write(file, file.relative_to(classes))
    # Dependencies stay byte-for-byte identical; no modification of shared classes.
    shutil.copyfile(base, ROOT / "build/llmcamera-runtime.jar")
    metadata = {"base_sha256": digest, "activity_sha256": hashlib.sha256(out.read_bytes()).hexdigest(),
                "java_release": 8, "agent": "fcdsrepresentation", "activity": "fcds-p2-26-fall-1a"}
    (ROOT / "build/build.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    build()
