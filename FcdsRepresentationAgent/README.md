# FCDS representation Dev activity

Source for the dedicated `fcdsrepresentation` Bazaar agent deployed on Bree for module/activity `fcds-p2-26-fall-1a`. The original llmcamera agent and shared Java classes are unchanged.

Maintained branch: `main` (PR #239 merged). Older `codex/fcds-data-capture` and `codex/fcds-representation-activity`
branches are superseded. Start with the
[platform deployment guide](https://github.com/DANCEcollaborative/ope-platform-operator/tree/dev/deploy/fcds)
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

A failed direct tutor request (including a rotation lookup failure) produces a
private retry notice and a sanitized `tutor.question_error` capture event. It
does not terminate the input worker or send an image with an assumed rotation.
Later messages and camera previews continue. Background failures keep their
existing retry/cooldown behavior and do not send failure notices.

## Files and build

`bazaar/src/` contains only new classes; `bazaar/runtime/` contains activity plans and overrides. `ui/` contains the personal page, MIT-licensed qrcode-generator 1.4.4, and transformations of the existing camera/login files. `final_grader/` contains the matching single-task adapter for the separate Activity Server. Student content lives in `DANCEcollaborative/fcds-p2-representation` (`main`).

Copy Bree's `/usr0/DANCEcollaborative/bazaar/bazaar_server/bazaar_server_lobby/agents/fcdsrepresentationagent/llmcamera-runtime.jar` into `bazaar/vendor/llmcameraagent.jar` locally. It must have SHA-256 `441ecf88afaeb9645f4ae18f1d2068aa6972e86d0657101021cab6eaa32e84c0`. This pinned dependency is intentionally not committed. It supplies the inherited camera agent framework; our behavior is in `bazaar/src/`. Use a JDK with `--release 8` support, setting `JAVAC` if your default compiler is Java 8:

```sh
python3 bazaar/build.py
python3 -m unittest discover -s tests -v
node --test tests/camera_queue.test.cjs
```

The HTTPS rotation integration test additionally requires Pillow, OpenSSL, and
JDK `keytool`. It respects `JAVA`, `JAVAC`, and `KEYTOOL` executable overrides.

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


## Picture orientation

The served pages in `bazaar_server/bazaar_server_lobby/lobby/html_pages/` and
`lobby/static/` are the source for the private picture viewer and recorded camera.
The corresponding `ui/` paths are links to these files so preparation uses
the same pages rather than overwriting them with older copies.

The private Paper tutor page provides zoom, left/right quarter turns, a precise
clockwise angle, and separate size/angle resets. Rotation stays selected as new
camera frames arrive and fits the rotated image within the viewer; enlarged
images scroll to every edge. The viewer saves each image's clockwise angle to the recorder using the
participant-scoped credential forwarded in the private iframe's URL fragment.
The displayed original's SHA-256 resolves to its archived frame ID; writes bind
to that ID so a late save cannot change the next image. Repeated identical bytes
resolve to the latest matching capture for that participant. Angle choices stay
visible across new images, including images already in flight when an angle
changes. Save failures are shown and retried in order; keep the page open until
its angle is saved. Reloads restore the saved angle. Zoom remains local.

The recorder assigns the latest saved participant angle to new captures and
keeps per-frame changes as separate immutable events. Original uploaded bytes
are retained; exports and relay requests carry clockwise rotation metadata,
with `pixels_rotated: false`, for export rendering. No automatic orientation
model or camera-side rotation is performed. The Java tutor now reads the latest
saved student angle immediately before each image-bearing LLM request and
renders its images in memory using that clockwise angle. This includes prior
comparison images. Zero preserves the original bytes. The angle is read once
per request, so a setting change applies to the next question even if the
original capture is older. Lookup failures stop that image request instead of
silently assuming zero; direct questions receive a private retry notice and the
input worker continues processing later events. Text-only requests do not need a rotation lookup.

`camera.rotation.url` in `RepresentationCameraListener.properties` defaults to
`https://bree.lti.cs.cmu.edu/fcds-recorder/v1/camera/image-rotation`. The lookup uses
the existing signing key to obtain a participant-scoped credential; no API key
or credential is put into URLs. Deploy the recorder's `current_rotation` read
support before the rebuilt activity JAR; only new agent processes load the JAR.
The activity's preview override sends original bytes, preserving the identity
used by the browser to save angles. The shared inherited runtime JAR is unchanged.

Temporary evidence saving is currently ENABLED at the `saveTutorImageEvidence`
call in `RepresentationCameraListener.sendToOpenAI`. It captures the first 20
image occurrences handed to the LLM transport per JVM, including any comparison
images. A text-only request does not count. On first use a private temporary
`fcds-llm-image-evidence-*` directory is created and its full path is printed as
`FCDS_LLM_IMAGE_EVIDENCE_DIR`. Each image has JSON metadata with applied angle,
revision, original/sent image hashes, full outgoing request hash, and its position
in that request. `-Dfcds.llm.evidence.dir=/absolute/private/path` can select an
empty review directory. Use a fresh directory for each run. These temporary
files contain student work; they are not published by the application.

The evidence is captured just before the existing HTTPS transport. It shows
exact outgoing bytes, not proof that a provider accepted or analyzed them.
After the user reviews the evidence, comment out only the clearly marked
`saveTutorImageEvidence(prepared,request,participant);` call and rebuild. Leave
rotation enabled. The permanent capture journal retains original request images
and rendering metadata/hashes, not the temporary rotated copies.

`tests/test_llm_rotation.py` exercises the production Java HTTPS sender and signed
recorder lookup with a local HTTPS receiver. It verifies exact request/file byte
matches, whole-request hashes, rotation direction/bounds, changing settings,
original preservation, preview identity, comparison images, and the 20-image
cap. It calls no external model.

Validation: `node --test tests/camera_queue.test.cjs tests/private_chat_ready.test.cjs tests/picture_orientation.test.cjs tests/picture_rotation_storage.test.cjs`.
