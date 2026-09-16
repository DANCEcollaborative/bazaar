# Etherpad + Java agent integration

## Files

- `docker-compose.yml`, `.env.example` — Etherpad + Postgres, based on the
  official `ether/etherpad` docker-compose example.
- `java/EtherpadClient.java` — thin wrapper over Etherpad's HTTP API.
- `java/AgentEtherpadManager.java` — ties agent-instance lifecycle to
  Etherpad groups/pads (2a) and provides scheduled + on-demand text fetch (2b).
- `java/pad-chat.html.template` — the per-user page: pad on the left, your
  chat on the right.
- `nginx-etherpad.conf` — two reverse-proxy options so the pad's session
  cookie is visible to your agent's own web app.

## 1. Bring up Etherpad

```bash
cp .env.example .env
# edit .env: set ETHERPAD_ADMIN_PASSWORD, ETHERPAD_API_KEY, POSTGRES_PASSWORD
docker compose up -d
```

A single Etherpad container already serves unlimited documents — each pad is
just addressed by its own `padID`. No extra containers or config are needed
for "multiple documents simultaneously"; that's native Etherpad behavior.

Smoke-test the API (replace `$KEY` with `ETHERPAD_API_KEY`):

```bash
curl -s -X POST "http://localhost:9001/api/1.2.15/createGroup" -d "apikey=$KEY"
# {"code":0,"message":"ok","data":{"groupID":"g.xxxxxxxx"}}
```

`EtherpadClient` auto-detects the API version at runtime by calling `GET /api`,
so you don't need to hardcode `1.2.15` in your own code.

## 2. How the integration maps to your two requirements

**2a — one Etherpad instance (pad) per agent instance.**
On agent-instance creation, call:

```java
AgentPadContext ctx = manager.register(agentInstanceId);
```

This creates an Etherpad *group* deterministically mapped to your
`agentInstanceId` (via `createGroupIfNotExistsFor`, which is idempotent — safe
to call again after a restart) and one pad inside it (`createGroupPad`). All
users of that agent instance share `ctx.padID()`.

When each user logs into that instance:

```java
UserPadAccess access = manager.registerUser(agentInstanceId, userId, displayName);
```

This maps the user to an Etherpad *author* (`createAuthorIfNotExistsFor`,
also idempotent per user) and grants them a *session* on the group
(`createSession`), which is what lets Etherpad attribute edits to the right
person, in their own color, and lets you revoke access independently per
user later (`deleteSession`) without affecting anyone else.

**2b — fetch pad text periodically or on request.**

```java
String text = manager.fetchTextNow(agentInstanceId); // on user request
```

`AgentEtherpadManager` also schedules this automatically per registered
instance (interval configurable in the constructor) and hands the result to
whatever callback you passed in (`onTextFetched`), so you can pipe it
straight into your agent's own processing without extra wiring.

## 3. Rendering the shared pad in your agent's UI

Etherpad's group-pad model is session-cookie based: the user's browser must
carry a `sessionID` cookie **scoped to Etherpad's own origin** before loading
the pad iframe (`GET /p/<padID>`). If your agent's web app and Etherpad are
different origins, a cookie set from your page won't reach Etherpad, and the
embedded pad will fall back to anonymous/no access.

**Recommended fix:** run Etherpad on a subdomain that shares a parent domain
with your agent app (e.g. `agent.yourapp.com` + `pad.yourapp.com`), then set
the cookie with `Domain=.yourapp.com`. See `nginx-etherpad.conf` (Option A)
and `pad-chat.html.template`. An alternative — mounting Etherpad under a
sub-path of the same host — is included as Option B, but Etherpad's sub-path
support is less battle-tested; verify static assets and the Socket.IO
connection actually load before relying on it.

Render `pad-chat.html.template` per user, filling in `PAD_ID`, `SESSION_ID`
(from `UserPadAccess`), `ETHERPAD_BASE`, and `COOKIE_DOMAIN`. The template
loads the pad with `showChat=false` since your agent has its own chat pane;
drop that param if you'd rather use Etherpad's built-in chat instead of (or
alongside) your own.

## 4. Dependencies for the Java code

`EtherpadClient` uses `java.net.http.HttpClient` (JDK 11+, no extra dep) and
Jackson for JSON:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-databind</artifactId>
  <version>2.17.0</version>
</dependency>
```

## Notes / things to decide as you productionize

- **Cleanup**: `AgentEtherpadManager` doesn't delete Etherpad groups/pads
  when an agent instance is retired — decide whether you want to keep pad
  history around or call `deletePad`/`deleteGroup` in your own teardown path.
- **Session TTL**: sessions expire (`sessionTtl` in the constructor); a user
  whose session lapses mid-conversation will need `registerUser` called
  again — e.g. on each login, not just once ever.
- **Backups**: the `postgres_data` volume is where all pad content and
  revision history live — back it up like any other production database.