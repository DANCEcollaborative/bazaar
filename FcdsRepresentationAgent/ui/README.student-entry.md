# Sensor recovery student entry

`student-entry.html` is the complete source for Bree's `/var/www/html/k8sjchat.html`.
Use https://bree.lti.cs.cmu.edu/k8sjchat.html for students. The original Bazaar
hostname is a different server and is not this deployment.

The page launches only activity `fcds-p2-26-fall-1a`, titled **Recover the sensor
readings**. It uses the existing Google client, enabled-activity enrollment API,
and Lobby allocation endpoint. It automatically enrolls newly signed-in CMU accounts through the activity-specific
server endpoint after Google verification; it does not alter other activities.
Existing invited non-CMU accounts retain access. The Google credential stays in
memory, not browser storage. A stored but unenrolled CMU identity must sign in
again to activate access. Unknown accounts, disabled activities, enrollment outages,
popup blocking and allocation failures have visible recovery instructions.

Publish this whole file; the historical `patch_login.py` and `patch_group_wait.py`
scripts document earlier incremental changes and are not needed on this file.
Before installation, capture the current public page and compare its hash to the
version reviewed. Install atomically with mode 0644. No service restart is needed.
The activity display name in Bree's `activities` record must match; keep its
activity ID, module, grader, roster and room mappings unchanged.

Test with `node tests/student_entry.test.cjs` from the agent directory using a
Node environment with Playwright and Chromium. These browser tests stub Google
and API responses; follow with a fresh live enrolled synthetic identity through
the actual Lobby, plus a real Google sign-in and phone-camera acceptance test.
Do not commit authenticated launch URLs or account credentials.

The Lobby must remain running during student sessions: its inherited startup
code clears room assignments. Always drain before restarting it.
