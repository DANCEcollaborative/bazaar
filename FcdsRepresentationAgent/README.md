# FCDS representation Dev activity

Source for the dedicated `fcdsrepresentation` Bazaar agent deployed on Bree for module/activity `fcds-p2-26-fall-1a`. The original llmcamera agent and shared Java classes are unchanged.

## Behavior

- Supports 1–4 actual participants; paper → NumPy coding → saved-notebook submission.
- Readiness requires all present participants, plus two distinct passing task callbacks for early Coding completion. Deadlines still allow unfinished submission.
- Ignores synthetic/private readiness, duplicate callbacks, and stale timers; retains early readiness during introductory prompts.
- Keeps paper readiness sent during Setup, including withdrawals. Exact participant names and reconnects are tracked by an activity-local presence watcher; Alex and Alexander remain distinct.
- Processes each private question once, including questions from different students sent together, without the inherited room-wide time filter.
- Room-specific tutoring history; paper images omitted from Coding prompts; inherited tutor JSON response contract preserved.
- One personal laptop tutor link per participant, with phone-camera QR pairing and private preview/chat. Extensionless HTML selector plus old-link compatibility alias.
- Notebook opens by default through a scoped patch to Bree's existing login page.

## Files and build

`bazaar/src/` contains only new classes; `bazaar/runtime/` contains activity plans and overrides. `ui/` contains the personal page, MIT-licensed qrcode-generator 1.4.4, and transformations of the existing camera/login files. `final_grader/` contains the matching two-task adapters for the separate Activity Server. Student content lives in `nachiketdk/fcds-p2-representation`.

Copy the deployed `llmcameraagent.jar` into `bazaar/vendor/llmcameraagent.jar` locally. It must have SHA-256 `441ecf88afaeb9645f4ae18f1d2068aa6972e86d0657101021cab6eaa32e84c0`. This pinned dependency is intentionally not committed. Use a JDK with `--release 8` support:

```sh
python3 bazaar/build.py
python3 -m unittest discover -s tests -v
node --test tests/camera_queue.test.cjs
```

The small JAR uses that unchanged dependency in its manifest. No shared base JAR is overwritten. Java tests run offline with placeholder credentials and cover all participant counts from 1–4, phase gates, Setup readiness, disconnect/reconnect, prefix-related names, rapid private questions, early callbacks, room isolation, and tutor payload/response format. The camera queue test uses mocked browser services to check offline retention, acknowledgement validation, replay after reload, and participant queue isolation; it does not replace a real-phone acceptance check.

## Deployment boundaries

`prepare_bree.py --bundle <this-directory> --destination <new-private-directory>` prepares files from the existing Bree baseline and refuses to replace a staging directory. It inherits runtime secrets locally on Bree; never commit or upload the prepared runtime. Install only the new agent, camera, and HTML files under `/usr0/DANCEcollaborative/bazaar/bazaar_server/bazaar_server_lobby/` using the existing container mounts. Keep backups, including `agent.xml` and overridden properties. Apply `ui/patch_login.py` to a backed-up `/var/www/html/k8sjchat.html` separately. No shared service restart is necessary; fresh agent processes load the new files.

The existing Node route appends `.html`, so `html-page-group` must be `tab-share-representation`, without an extension. Serve `tab-share-representation.html.html` as a compatibility alias for links already posted in old rooms.

New rooms select the agent through their OPE module. Existing rooms retain their agent/process state. Comprehensive research logging is outside this change.

## Browser validation

Solo student browser flow passed through camera preview, private tutor response, readiness, failing/passing checks, submission receipt and final grade 2/2. Two-student readiness and private routing passed, but the inherited v19 Jupyter image had incompatible collaboration packages. That separate repair lives on the `ope-platform-operator` branch `codex/fcds-dev-collaboration`. A full two-browser rerun is required after its image change. Google identity/Lobby allocation were fixtures in the isolated browser test; physical phone acceptance remains separate.
