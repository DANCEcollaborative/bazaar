# FCDS representation Dev activity

Source for the dedicated `fcdsrepresentation` Bazaar agent deployed on Bree for module/activity `fcds-p2-26-fall-1a`. The original llmcamera agent and shared Java classes are unchanged.

Maintained branch: `codex/fcds-preview-reconnect` until PR #239 merges, then
`main`. Older `codex/fcds-data-capture` and `codex/fcds-representation-activity`
branches are superseded. Start with the
[platform deployment guide](https://github.com/DANCEcollaborative/ope-platform-operator/tree/main/deploy/fcds)
for all component locations and routine start/stop commands.

## Behavior

- Supports 1–3 actual participants; paper → NumPy coding → saved-notebook submission.
- Readiness requires all present participants, plus a passing task1 recovery callback for early Coding completion. Deadlines still allow unfinished submission.
- Ignores synthetic/private readiness, duplicate callbacks, and stale timers; retains early readiness during introductory prompts.
- Keeps paper readiness sent during Setup, including withdrawals. Exact participant names and reconnects are tracked by an activity-local presence watcher; Alex and Alexander remain distinct.
- Processes each private question once, including questions from different students sent together, without the inherited room-wide time filter.
- Replays the cached paper preview when a private page connects or reconnects, without another upload or model request.
- Room-specific tutoring history; paper images omitted from Coding prompts; inherited tutor JSON response contract preserved.
- One personal laptop tutor link per participant, shown in that student's JupyterLab view rather than posted in group chat, with phone-camera QR pairing and private preview/chat. Extensionless HTML selector plus old-link compatibility alias.
- The Dev phone page supports continuous still capture or a student-selected single photo. Choosing a photo stops continuous capture to release the phone camera. Each selected photo is reviewed before upload and archived through the same Bree queue; the Dev tutor handles it even when it resembles the previous frame. The phone page distinguishes local queueing from Bree archival and tutor-service relay.
- Notebook opens by default through a scoped patch to Bree's existing login page.

## Files and build

`bazaar/src/` contains only new classes; `bazaar/runtime/` contains activity plans and overrides. `ui/` contains the personal page, MIT-licensed qrcode-generator 1.4.4, and transformations of the existing camera/login files. `final_grader/` contains the matching single-task adapter for the separate Activity Server. Student content lives in `DANCEcollaborative/fcds-p2-representation` (`main`).

Copy Bree's `/usr0/DANCEcollaborative/bazaar/bazaar_server/bazaar_server_lobby/agents/fcdsrepresentationagent/llmcamera-runtime.jar` into `bazaar/vendor/llmcameraagent.jar` locally. It must have SHA-256 `441ecf88afaeb9645f4ae18f1d2068aa6972e86d0657101021cab6eaa32e84c0`. This pinned dependency is intentionally not committed. It supplies the inherited camera agent framework; our behavior is in `bazaar/src/`. Use a JDK with `--release 8` support, setting `JAVAC` if your default compiler is Java 8:

```sh
python3 bazaar/build.py
python3 -m unittest discover -s tests -v
node --test tests/camera_queue.test.cjs
```

The small JAR uses that unchanged dependency in its manifest. No shared base JAR is overwritten. Java tests run offline with placeholder credentials and cover all participant counts from 1–3, phase gates, Setup readiness, disconnect/reconnect, prefix-related names, rapid private questions, early callbacks, room isolation, and tutor payload/response format. The camera queue test uses mocked browser services to check offline retention, acknowledgement validation, replay after reload, and participant queue isolation; it does not replace a real-phone acceptance check.

## Deployment boundaries

The installed files are under
`/usr0/DANCEcollaborative/bazaar/bazaar_server/bazaar_server_lobby/` on Bree:

| Source | Installed location relative to that directory |
| --- | --- |
| `bazaar/build/fcdsrepresentationagent.jar` | `agents/fcdsrepresentationagent/fcdsrepresentationagent.jar` |
| `bazaar/runtime/` plans and properties | Same relative paths under `agents/fcdsrepresentationagent/` |
| `bazaar/tutor-context.txt` | `openai.prompt.context` in the agent's `properties/RepresentationCameraListener.properties` |
| `ui/representation-*.html` | `lobby/html_pages/` |
| `ui/camera_fcds-recorded.html`, `ui/camera_fcds-recorded.js` | `lobby/static/` |
| `ui/qrcode-1.4.4.js` | `lobby/static/representation-qrcode.js` |

`ui/student-entry.html` is the complete `/var/www/html/k8sjchat.html` source;
publish it directly. The old incremental login patch scripts are historical.
`final_grader/` is installed separately under the Activity Server's mounted
`grading/grader_fcds_representation/` directory selected by this activity's `task_graders` setting.

For an existing installation, deploy only changed files from this checkout,
atomically, preserving ownership and a rollback. Update `FCDS_RELEASE.json`
beside the deployed JAR with the source commit and JAR SHA-256. Runtime copies
of Java source are only references; edit/build from Git. Keep `agent.xml`,
secret-bearing properties, `capture.key` and the capture spool intact. New room
processes load the new JAR; no shared service restart is necessary.

`prepare_bree.py --bundle <this-directory> --destination <new-private-directory>`
is the original bootstrap helper, not an update installer. It inherits local
runtime secrets and generates a new signing key. Do not replace an existing
runtime with its output or rotate an existing key while updating. Prepared
directories must never be committed. A genuinely new deployment also needs
the shared Bazaar/Apache setup and activity registration, as described by the
platform guide; cloning this agent alone does not recreate shared infrastructure.

The existing Node route appends `.html`, so `html-page-group` must be `tab-share-representation`, without an extension. Serve `tab-share-representation.html.html` as a compatibility alias for links already posted in old rooms.

New rooms select the agent through their OPE module. Existing rooms retain their agent/process state. Recording and the manual-photo workflow use the existing Dev recorder integration.

## Current activity

Recover the sensor readings: r[i] = s[i] + (1/3) s[i-1], s[-1] = 0. Twenty minutes on paper and twenty-five minutes coding one recover_readings function. Student source and reproducible notebook builder live in the content repository's lesson directory. The final score is out of 1.

Deployment and browser-test evidence is recorded in the project docs; historical two-task results do not certify the current exercise. Physical-phone acceptance remains separate from automated browser tests.
