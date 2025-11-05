# LabAssistant Agent: End-to-End Overview

This guide explains how the LabAssistant agent is structured, how it connects through the Bazaar server stack, and how each part of the runtime plan operates. It is intended for developers newly onboarding to the project.

## 1. High-Level Architecture

```
Client Browser  <->  Node.js Lobby Server  <->  Socket.IO  <->  Java Agent (LabAssistant)
```

1. **Browser**: Loads an HTML chat interface (e.g., `chem-lab_mm_1.html`).
2. **Lobby Server**: Node/Express app in `bazaar_server/bazaar_server_lobby`. Manages rooms, sockets, static content.
3. **Socket.IO**: Real-time channel (path `/bazsocket`).
4. **LabAssistant Java Agent**: Deployed under `LabAssistantAgent/`. Uses `WebsocketChatClient` to connect to the lobby server, send/receive events, and drive conversations via plan steps.

## 2. Key Directories & Files

### Agent runtime
- `LabAssistantAgent/runtime` — packaged assets.
  - `plans/labassistant_plan_steps.xml` — plan definition (stages & steps).
  - `plans/labassistant_plan_prompts.xml` — prompt text used by various steps.
  - `properties/PlanExecutor.properties` — plan configuration (handlers, resources).
  - `properties/WebsocketChatClient.properties` — socket connection settings.
  - `plans/commands.xml` — command payloads used by `send_command` steps.
  - `plans/gatekeeper_prompts.xml` — Gatekeeper prompts used by `ready_secret` steps.

### Java sources
- `LabAssistantAgent/src/basilica2/...`
  - `agents/listeners/plan/PlanExecutor.java` — central planner implementation.
  - `agents/listeners/plan/*StepHandler.java` — implementations for each step type (e.g., `PromptStepHandler`, `GatedStepHandler`, `ReactionStepHandler`, etc.).
  - `socketchat/WebsocketChatClient.java` (under `SocketIOClient`) — shared library used by agents for Socket.IO connectivity.

### Server & client
- `bazaar_server/bazaar_server_lobby` — Node.js lobby server.
  - `server_bazaar_local.js` — main entrypoint; serves HTML and proxies sockets.
  - `agents/` — deployment copies of agent runtime (when deployed server-side).
  - `bazaar/index.html` (and corresponding variants) — HTML chat templates.
  - `lobby/html_pages/chem-lab_mm_1.html` — LabAssistant specific chat UI (tabs, KaTeX, etc.).

## 3. Runtime Flow

### 3.1 Agent launch
1. Launch command (via IDE or CLI) runs the Java agent with runtime config.
2. `PlanExecutor` loads stage/step definitions (`labassistant_plan_steps.xml`).
3. `PlanExecutor.properties` registers which step handlers process each step type.
4. Agent connects to Socket.IO server using `WebsocketChatClient` and joins the room indicated (default `ROOM`).

### 3.2 Socket handshake & queueing

- The lobby server script (`server_bazaar_local.js`) accepts connections at `/bazsocket` and places users in rooms based on the URL path (e.g., `/chat/ROOM/1/STUDENT/1/?html=chem-lab_mm_1`).
- Once connected, the Java agent listens for `LaunchEvent`, then begins executing the plan.

### 3.3 Message flow

- **Outbound**: Step handlers create `MessageEvent`s (prompt text, feedback, etc.). `OutputCoordinator` pushes them over the Socket.IO connection.
- **Inbound**: User messages, readiness signals, or specific commands insert `MessageEvent`s back into the agent pipeline. Step handlers (e.g., `GatedStepHandler`, `ReactionStepHandler`) evaluate them and progress the plan.

### 3.4 Plan execution (Stages)

Current plan is defined in `labassistant_plan_steps.xml`:

1. **StageInitialization**
   - Two `prompt` steps (WELCOME).
2. **StagePart1**
   - Prompts: question, collaboration reminder, ready instructions.
   - `ready_secret` step waits for `ready:part1` or a secret word (per properties file).
   - `reaction` step monitors `reaction:` answers, granting multiple attempts and sending feedback.
   - `solution` step reveals the correct answer (using `SolutionStepHandler`).
3. **StagePart2**
   - Sends `SHOW_VLAB2` command to the HTML (via `send_command`).
   - Repeats prompt → ready → reaction → solution cycle for the second part.
4. **StagePart3**
   - Reveals tab 3 (`SHOW_VLAB3`), waits briefly (`no-op` with delay), dumps chat log, ends session.

### 3.5 Step Handlers (PlanExecutor step types)

Step types you’ll see in `labassistant_plan_steps.xml` with corresponding handlers:

| Step type         | Handler class                                            | Description |
|-------------------|----------------------------------------------------------|-------------|
| `prompt`          | `PromptStepHandler`                                      | Sends a chat message from `labassistant_plan_prompts.xml`. Supports `${PROPERTY|fallback}` placeholders. |
| `gated`           | `GatedStepHandler` + `Gatekeeper`                        | Waits for ready keywords. Uses `plans/gatekeeper_prompts.xml` for follow-up.|
| `ready_secret`    | `ReadyOrSecretStepHandler`                               | Extends gated logic: also advances if a configured secret phrase is entered.|
| `reaction`        | `ReactionStepHandler`                                    | Monitors `reaction:` answers, validates against configured reactions, gives specific feedback (reactants, products, stoichiometry, general), and handles attempt counts. Secret triggers also short-circuit.|
| `send_command`    | `SendCommandStepHandler`                                 | Looks up command text in `plans/commands.xml` (e.g., `show:#tab-virtualLab2-li`) and emits it via Socket.IO (`sendcommandevent`).|
| `solution`        | `SolutionStepHandler`                                    | Pulls the solution text from `PlanExecutor.properties` and sends it as a message.|
| `no-op`           | `DoNothingStepHandler`                                   | Performs no action, immediately marks step done (after optional delay).|
| `chatlog`         | `ChatLogHandler`                                         | Streams chat log to output or file (if configured).|
| `send_end`        | `EndStepHandler`                                         | Sends termination message / signals end to clients.|
| `logout`          | `LogoutStepHandler`                                      | Logs the agent off and closes connections.|

### 3.6 Prompt resolution

- `PromptStepHandler` pulls prompt text by ID from `labassistant_plan_prompts.xml`.
- It substitutes placeholders (e.g., `[NAME1]`, `[REACTION_FORMAT]`, etc.) using shared state (`StateMemory`).
- `${PROPERTY|fallback}` in prompt XML resolves values from `PlanExecutor.properties` or system properties.

### 3.7 Reaction checking logic (ReactionStepHandler)

- Parses expected reaction strings from `PlanExecutor.properties` or step attributes.
- Accepts multiple valid reactions (`expected_reactions` separated by `;` or `,`).
- Permits input in the format `reaction: A+B->C`, `A+B=2C`, etc.; order-insensitive.
- Compares reactant sets, product sets, and stoichiometric ratios.
- Issues targeted feedback prompts based on the mismatch.

## 4. Web Front-End (chem-lab_mm_1.html)

- Located at `bazaar_server/bazaar_server_lobby/lobby/html_pages/chem-lab_mm_1.html`.
- Key features:
  - Loads KaTeX CSS/JS for math rendering (currently renders both MathML and HTML branches). Existing implementation may display fallback text if both branches remain; adjust if needed.
  - Tabbed view for Part 1, scratchpads, Part 2 & Part 3, each showing different iframes.
  - `appendMessage`, `appendNote`, `appendImage`, `appendHTML` functions insert inbound data into the DOM.
  - `renderMath` (auto-render) is currently in place to handle TeX inside new messages.

## 5. Lobby Server & Socket Details

- **File**: `bazaar_server/bazaar_server_lobby/lobby/server_bazaar_local.js`.
- Serves chat HTML pages (`/chat/...` routes) and sets up Socket.IO on `/bazsocket`.
- Broadcasts events (`updatechat`, `sendcommandevent`, etc.) to all clients in a room.
- Agents can be deployed into `bazaar_server/bazaar_server_lobby/agents/<agentname>/` for server-side use.
- Developers typically run the server via `npm start` or similar commands defined in that directory.

## 6. Socket.IO Client (WebsocketChatClient)

- Located under `SocketIOClient/src/basilica2/socketchat/WebsocketChatClient.java`.
- Handles connecting, reconnecting, emitting and listening to socket events.
- Listens for `updatechat`, `updateusers`, `sendcommandevent`, etc., mapping them to agent events.
- On process events (message, ready, reaction, etc.), pushes events up through the agent listeners (PromptStepHandler, ReactionStepHandler, etc.) and ultimately Stage/Plan logic.

## 7. Plan Summary (current flow)

| Stage | Purpose | Key steps |
|-------|---------|-----------|
| StageInitialization | Greeting & wait | Two `prompt`s (WELCOME_WAIT/CONTEXT). |
| StagePart1 | Part 1 question & answer | `prompt` cues, `ready_secret`, `reaction` (3 attempts), `solution`. |
| StagePart2 | Part 2 challenge | `send_command` (show tab), similar prompt → ready → reaction → solution. Supports multiple valid reactions. |
| StagePart3 | Wrap-up | Reveal Part 3 tab, wait (no-op), log chat, send end message, logout. |

## 8. Configuration Properties

`PlanExecutor.properties`
- Defines which handlers process each step type.
- Stores replacement values (e.g., `PART1_EXPECTED`, `PART2_EXPECTED`, `PART1_SOLUTION`, etc.).
- Adjust secret words, expected reactions, and other plan settings here.

`WebsocketChatClient.properties`
- `socket_url`, `socket_suburl` determine where the agent connects.

`commands.xml`
- Each `<prompt id="...">` contains the command text (e.g., `show:#tab-virtualLab2-li`).
- Step `type="send_command"` references these IDs via `name`.

`gatekeeper_prompts.xml`
- Additional prompts Gatekeeper uses for ready states.

## 9. Deploying / Running LabAssistant

1. Ensure Node lobby server is running (serving `bazaar_server/bazaar_server_lobby`).
2. Launch LabAssistant agent (via IDE or command line) pointing to runtime directory.
3. Open the chat page: `http://localhost/chat/ROOM/1/STUDENT/1/?html=chem-lab_mm_1` (adjust ROOM/IDs as needed).
4. The agent will greet users and progress through plan steps based on chat inputs.

## 10. Summary of Step Types & Behavior

| Step Type | Handler | Expected behavior |
|-----------|---------|-------------------|
| `prompt` | `PromptStepHandler` | Send a message after optional typewriter delay. |
| `gated` | `GatedStepHandler` | Wait for ready keywords; prompt/ack users. |
| `ready_secret` | `ReadyOrSecretStepHandler` | Gated plus secret phrase unlock. |
| `reaction` | `ReactionStepHandler` | Evaluate reactions, provide targeted feedback, enforce attempts. |
| `send_command` | `SendCommandStepHandler` | Emit commands to the Socket.IO server (e.g., reveal tabs). |
| `solution` | `SolutionStepHandler` | Pull solution text from properties, send to chat. |
| `no-op` | `DoNothingStepHandler` | After configured delay, mark step done. |
| `chatlog` | `ChatLogHandler` | Dump chat logs (requires configuration). |
| `send_end` | `EndStepHandler` | Send closing message. |
| `logout` | `LogoutStepHandler` | Disconnect agent.

## 11. Tips for Extending LabAssistant

- **Add new stages/steps**: Update `labassistant_plan_steps.xml`. Ensure corresponding prompts exist and handlers are configured.
- **Create new prompt copy**: Add `<prompt id="...">` entries in `labassistant_plan_prompts.xml`.
- **Update solutions or expected answers**: Edit `PlanExecutor.properties` (e.g., `PART1_EXPECTED`, `PART1_SOLUTION`).
- **Introduce new step types**: Implement a new `StepHandler`, register it in `PlanExecutor.properties`, and reference in the plan XML.
- **Modify front-end behavior**: Update `chem-lab_mm_1.html` to adjust UI, KaTeX rendering, tabs, etc.

## 12. Useful References

- `LabAssistantAgent/src/basilica2/agents/listeners/plan/PlanExecutor.java` — core plan engine.
- `LabAssistantAgent/src/basilica2/agents/listeners/plan/ReactionStepHandler.java` — reaction analysis logic.
- `SocketIOClient/src/basilica2/socketchat/WebsocketChatClient.java` — Socket.IO connectivity.
- `bazaar_server/bazaar_server_lobby/lobby/server_bazaar_local.js` — lobby server implementation.
- `bazaar_server/bazaar_server_lobby/lobby/html_pages/chem-lab_mm_1.html` — main chat page.

By following this guide, new developers can understand the full stack of the LabAssistant agent—from plan configuration and step handlers to front-end rendering and Socket.IO communication.
