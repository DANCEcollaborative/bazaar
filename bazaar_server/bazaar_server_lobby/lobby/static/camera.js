const preview = document.querySelector("#cameraPreview");
const canvas = document.querySelector("#frameCanvas");
const fallback = document.querySelector("#cameraFallback");
const sessionInput = document.querySelector("#sessionInput");
const problemInput = document.querySelector("#problemInput");
const startButton = document.querySelector("#startButton");
const stopButton = document.querySelector("#stopButton");
const statusEl = document.querySelector("#cameraStatus");
const mobileFeed = document.querySelector("#mobileFeed");

const urlParams = new URLSearchParams(window.location.search);
const sessionFromUrl = urlParams.get("sessionId");
const problemFromUrl = urlParams.get("problemId");

// Bazaar's own camera API, served from the same origin as this page
// (port 8300, reached through Apache's generic /bazaar ProxyPass rule).
const API_BASE = "/bazaar/api/camera";

// The username camera frames are broadcast under. Must match the literal
// "CameraPhone" string server_lobby_https.js's /bazaar/api/camera/frame
// handler uses, both so WebsocketChatClient recognizes it and so this
// client can filter its own broadcasts back out of the feedback feed.
const CAMERA_USERNAME = "CameraPhone";

// Same join event/signature every other Bazaar client uses:
// socket.on('adduser', async (room, username, temporary, id, perspective) => ...)
const JOIN_EVENT = "adduser";

let stream;
let socket;
let uploadTimer;
let isUploading = false;
let cameraUploadIntervalMs = 2000;

if (sessionFromUrl) {
  sessionInput.value = sessionFromUrl;
}

if (problemFromUrl) {
  problemInput.value = problemFromUrl;
}

// Start stays disabled until the user has actually entered a session id —
// nothing in this file should attempt to pair a session or join a socket
// room before that happens.
startButton.disabled = true;
updateStartButtonState();

sessionInput.addEventListener("input", updateStartButtonState);
startButton.addEventListener("click", startCamera);
stopButton.addEventListener("click", stopCamera);
window.addEventListener("pagehide", stopCamera);

function updateStartButtonState() {
  startButton.disabled = sessionInput.value.trim().length === 0;
}

async function startCamera() {
  const sessionId = sessionInput.value.trim();

  if (!sessionId) {
    setStatus("Session id is required.");
    return;
  }

  if (!navigator.mediaDevices?.getUserMedia) {
    fallback.hidden = false;
    setStatus("This browser cannot access the camera from this page.");
    return;
  }

  try {
    setStatus("Requesting camera access...");
    stream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        facingMode: { ideal: "environment" },
        width: { ideal: 1280 },
        height: { ideal: 720 }
      }
    });

    preview.srcObject = stream;
    await preview.play();
    await loadCameraSettings();
    await pairSession(sessionId);
    connectSocket(sessionId);

    startButton.disabled = true;
    stopButton.disabled = false;
    fallback.hidden = true;
    setStatus(`Camera is streaming a snapshot every ${formatInterval(cameraUploadIntervalMs)}.`);
    uploadFrame();
    uploadTimer = window.setInterval(uploadFrame, cameraUploadIntervalMs);
  } catch (error) {
    fallback.hidden = false;
    setStatus(`Camera failed: ${error.message}`);
    stopCamera();
  }
}

function stopCamera() {
  window.clearInterval(uploadTimer);
  uploadTimer = undefined;
  socket?.disconnect();
  socket = undefined;

  if (stream) {
    for (const track of stream.getTracks()) {
      track.stop();
    }
  }

  stream = undefined;
  preview.srcObject = null;
  startButton.disabled = false;
  stopButton.disabled = true;
}

async function pairSession(sessionId) {
  const response = await fetch(`${API_BASE}/session`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ sessionId })
  });

  if (!response.ok) {
    throw new Error("Could not pair with server.");
  }
}

async function loadCameraSettings() {
  const response = await fetch(`${API_BASE}/health`);
  const health = await response.json();
  cameraUploadIntervalMs = Number(health.cameraUploadIntervalMs || 2000);
}

function connectSocket(sessionId) {
  socket?.disconnect();

  // Bazaar's Socket.IO server is mounted at path '/bazsocket' (see
  // server_lobby_https.js: require('socket.io')(server, {path: '/bazsocket', ...})).
  // The client library is auto-served at that same path by Socket.IO itself —
  // see the <script src="/bazsocket/socket.io.js"> tag in camera.html.
  socket = io("/", { path: "/bazsocket" });

  socket.on("connect", () => {
    // Same join call every regular Bazaar web client makes. `temporary`
    // matches the non-snoop default (false, i.e. this join gets logged
    // like any other room participant); `id` and `perspective` aren't
    // meaningful for a phone-only client, so they're left null/empty.
    // If your other clients pass something different for id/perspective,
    // update this call to match exactly.
    socket.emit(JOIN_EVENT, sessionId, CAMERA_USERNAME, true, "", null);
    addSystemFeedItem("Connected", `Joined room "${sessionId}" as ${CAMERA_USERNAME}.`);
  });

  socket.on("disconnect", () => {
    addSystemFeedItem("Disconnected", "Lost connection to Bazaar.");
  });

  socket.on("connect_error", (err) => {
    addFeedItem("Connection error", err.message, "chat-error");
  });

  // Agent feedback rides the same 'updatechat' event camera frames use.
  // Skip our own frame broadcasts (from === CAMERA_USERNAME) so the feed
  // only shows what the agent actually says back.
  socket.on("updatechat", (from, data) => {
    if (from === CAMERA_USERNAME) return;
    addFeedItem(from, extractDisplayText(data), "ai-recommendation");
  });
}

// Agent feedback may come back as a plain string, or as a multimodal
// message using the same ;%; / ::: delimiters the frame handler uses.
// Pull out a "speech" field if present, otherwise show it as-is.
function extractDisplayText(rawMessage) {
  if (typeof rawMessage !== "string") return String(rawMessage);
  if (rawMessage.indexOf(";%;") === -1) return rawMessage;

  const fields = {};
  for (const segment of rawMessage.split(";%;")) {
    const idx = segment.indexOf(":::");
    if (idx === -1) continue;
    fields[segment.slice(0, idx)] = segment.slice(idx + 3);
  }
  return fields.speech || rawMessage;
}

async function uploadFrame() {
  if (isUploading || !stream || preview.readyState < HTMLMediaElement.HAVE_CURRENT_DATA) {
    return;
  }

  isUploading = true;

  try {
    const maxWidth = 960;
    const scale = Math.min(1, maxWidth / preview.videoWidth);
    canvas.width = Math.round(preview.videoWidth * scale);
    canvas.height = Math.round(preview.videoHeight * scale);

    const context = canvas.getContext("2d");
    context.drawImage(preview, 0, 0, canvas.width, canvas.height);
    const imageBase64 = canvas.toDataURL("image/jpeg", 0.55).split(",")[1];

    const response = await fetch(`${API_BASE}/frame`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        sessionId: sessionInput.value.trim(),
        problemId: problemInput.value.trim() || undefined,
        imageBase64,
        mimeType: "image/jpeg",
        width: canvas.width,
        height: canvas.height
      })
    });
    const data = await response.json();

    setStatus(`Sent frame ${data.frameCount}.`);
  } catch (error) {
    setStatus(`Frame upload failed: ${error.message}`);
  } finally {
    isUploading = false;
  }
}

function setStatus(message) {
  statusEl.textContent = message;
}

function formatInterval(milliseconds) {
  if (milliseconds % 1000 === 0) {
    const seconds = milliseconds / 1000;
    return `${seconds} ${seconds === 1 ? "second" : "seconds"}`;
  }

  return `${milliseconds} ms`;
}

function addFeedItem(title, text, variantClass) {
  const item = document.createElement("div");
  item.className = variantClass ? `feed-item ${variantClass}` : "feed-item";
  item.innerHTML = `
    <strong></strong>
    <p></p>
  `;
  item.querySelector("strong").textContent = title;
  item.querySelector("p").textContent = text;
  mobileFeed.prepend(item);
}

function addSystemFeedItem(title, text) {
  addFeedItem(title, text, "system-message");
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}