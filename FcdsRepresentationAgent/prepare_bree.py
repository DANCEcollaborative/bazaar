"""Prepare a separate runtime on Bree; does not install, restart, or select it."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
from ui.patch_camera import patch_camera

BASE = Path("/usr0/DANCEcollaborative/bazaar/bazaar_server/bazaar_server_lobby")
BASE_HASH = "441ecf88afaeb9645f4ae18f1d2068aa6972e86d0657101021cab6eaa32e84c0"


def copy_new(source, target):
    if target.exists():
        raise RuntimeError("Refusing to replace an existing prepared directory: " + str(target))
    shutil.copytree(source, target)


def append_properties(path, additions):
    text = path.read_text().rstrip() + "\n\n# Representation activity overrides\n"
    for key, value in additions.items():
        text += key + "=" + value.replace("\\", "\\\\").replace("\n", "\\n") + "\n"
    path.write_text(text)


def prepare(bundle, destination):
    baseline = BASE / "agents/llmcameraagent"
    jar = baseline / "llmcameraagent.jar"
    if hashlib.sha256(jar.read_bytes()).hexdigest() != BASE_HASH:
        raise RuntimeError("Deployed camera runtime changed; review compatibility before preparing.")
    destination.mkdir(mode=0o700, parents=True, exist_ok=False)
    runtime = destination / "fcdsrepresentationagent"
    runtime.mkdir()
    for name in ("properties", "accountable", "dialogues", "dict", "dictionaries"):
        if (baseline / name).is_dir():
            shutil.copytree(baseline / name, runtime / name)
    for name in ("agent.xml", "log.properties"):
        shutil.copyfile(baseline / name, runtime / name)
    shutil.copyfile(jar, runtime / "llmcamera-runtime.jar")
    shutil.copyfile(bundle / "bazaar/build/fcdsrepresentationagent.jar", runtime / "fcdsrepresentationagent.jar")
    shutil.copyfile(bundle / "bazaar/build/build.json", runtime / "build.json")
    shutil.copytree(bundle / "bazaar/runtime", runtime, dirs_exist_ok=True)
    agent_xml = runtime / "agent.xml"
    agent_xml.write_text(agent_xml.read_text().replace(
        'class="basilica2.agents.components.OutputCoordinator"',
        'class="basilica2.agents.components.RepresentationOutputCoordinator"'))
    shutil.copytree(bundle / "bazaar/src", runtime / "src")
    for directory in ("logs", "chat_logs", "chat_history", "planstatus"):
        (runtime / directory).mkdir(exist_ok=True)
    for name in ("apiKey.properties", "apiKeys.properties"):
        path = runtime / "properties" / name
        if path.exists():
            path.chmod(0o600)
    camera_properties = runtime / "properties/RepresentationCameraListener.properties"
    shutil.copyfile(baseline / "properties/LlmCameraListener.properties", camera_properties)
    append_properties(camera_properties, {
        "name": "OPEBot", "model": "openai",
        "openai.prompt.context": (bundle / "bazaar/tutor-context.txt").read_text().strip(),
        "camera-url": "https://bree.lti.cs.cmu.edu/bazaar/static/camera_fcds-representation.html",
        "html-page-group": "tab-share-representation",
    })
    append_properties(runtime / "properties/OutputCoordinator.properties", {
        "history_listener_name": "RepresentationHistoryListener"})
    static = destination / "static"
    static.mkdir()
    html = (BASE / "lobby/static/camera_fcds-p2-26-fall-1a.html").read_text()
    html = html.replace("camera_fcds-p2-26-fall-1a.js", "camera_fcds-representation.js")
    (static / "camera_fcds-representation.html").write_text(html)
    js = (BASE / "lobby/static/camera_fcds-p2-26-fall-1a.js").read_text()
    match = re.search(r'const SESSION_ID_PREFIX = "(llmcamera[^"]+)";', js)
    if match is None:
        raise RuntimeError("Camera prefix format changed; inspect the client.")
    prefix = "fcdsrepresentation" + match.group(1)[len("llmcamera"):]
    js = js.replace(match.group(1), prefix)
    # Keep a capture tied to the identities used when the connection was opened.
    js = js.replace("startButton.disabled = true;\n    stopButton.disabled = false;",
        "startButton.disabled = true;\n    sessionInput.disabled = true;\n    problemInput.disabled = true;\n    stopButton.disabled = false;")
    js = js.replace("startButton.disabled = false;\n  stopButton.disabled = true;",
        "sessionInput.disabled = false;\n  problemInput.disabled = false;\n  updateStartButtonState();\n  stopButton.disabled = true;")
    js = js.replace("    const data = await response.json();",
        '    if (!response.ok) throw new Error(`Server rejected frame (${response.status}).`);\n    const data = await response.json();')
    (static / "camera_fcds-representation.js").write_text(patch_camera(js))
    pages = destination / "html_pages"
    pages.mkdir()
    page = (BASE / "lobby/html_pages/tab-share-chat.html").read_text()
    page = page.replace("https://tinyurl.com/bazaarcam1", "https://bree.lti.cs.cmu.edu/bazaar/static/camera_fcds-representation.html")
    (pages / "tab-share-representation.html").write_text(page)
    (pages / "tab-share-representation.html.html").write_text(page)
    shutil.copyfile(bundle / "ui/representation-student.html", pages / "representation-student.html")
    shutil.copyfile(bundle / "ui/qrcode-1.4.4.js", static / "representation-qrcode.js")
    shutil.copytree(bundle / "final_grader", destination / "grader_fcds_representation")
    info = {"agent": "fcdsrepresentation", "activity": "fcds-p2-26-fall-1a",
            "camera_prefix": prefix, "base_sha256": BASE_HASH,
            "status": "prepared, not installed or selected"}
    (destination / "prepared.json").write_text(json.dumps(info, indent=2) + "\n")
    print(json.dumps(info, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--destination", type=Path, required=True)
    args = parser.parse_args()
    prepare(args.bundle, args.destination)
