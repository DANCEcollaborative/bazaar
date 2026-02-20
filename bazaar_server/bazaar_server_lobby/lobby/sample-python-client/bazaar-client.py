#!/usr/bin/env python3
"""
Bazaar Socket.IO Client
Connects to the Bazaar NodeJS server and initiates a socket.io session.

Usage:
    python3 bazaar-client.py [--agent AGENT_NAME] [--room-id ROOM_ID] [--user-name USER_NAME] [--user-id USER_ID] --server SERVER_PREFIX]

Example:
    python3 bazaar-client.py --agent jeopardybigwgu --room-id 250101000 --user-name "Bot" --user-id 100 --server bree
"""

import argparse
import time
import socketio

# ──────────────────────────────────────────────
# CONSTANTS
# ──────────────────────────────────────────────
SOCKET_PATH  = "/bazsocket"
CLIENT_ID    = "ClientServer"


# ──────────────────────────────────────────────
# Socket.IO client setup
# ──────────────────────────────────────────────
sio = socketio.Client(ssl_verify=True, logger=True, engineio_logger=False)


@sio.event
def connect():
    print(f"[+] Connected  (sid={sio.sid})")


@sio.event
def connect_error(data):
    print(f"[!] Connection error: {data}")


@sio.event
def disconnect():
    print("[-] Disconnected from server")


# ── Incoming events ────

@sio.on("updatechat")
def on_updatechat(username, message):
    print(f"[chat] {username}: {message}")


@sio.on("updateusers")
def on_updateusers(*args):
    print(f"[users] {args}")


@sio.on("updatepresence")
def on_updatepresence(*args):
    print(f"[presence] {args}")


@sio.on("*")
def catch_all(event, data):
    print(f"[event:{event}] {data}")


# ──────────────────────────────────────────────
# Sending messages
# ──────────────────────────────────────────────

def send_chat_message(user_name: str, message: str):
    payload = f"multimodal:::true;%;from:::{user_name};%;speech:::{message}"
    sio.emit("sendchat", payload)   


def send_chat_messages():
    time.sleep(40)
    send_chat_message("Sonny", "I'm Sonny.")
    time.sleep(3)
    send_chat_message("Cher", "And I'm Cher. Let's rock!")


# ──────────────────────────────────────────────
# Main
# ──────────────────────────────────────────────
def main(server: str, agent_name: str, room_id: str, user_id: str, user_name: str):
    """Connect to the Bazaar server and join the specified room."""

    auth_payload = {
        "token": "",
        "agent": { 
            "name": agent_name,
            "configuration": {
                "clientID": CLIENT_ID    
            }
        },
        "chat": {
            "id": room_id
        },
        "user": {
            "id": user_id,
            "name": user_name
        }
    }

    SERVER_URL = f"https://{server}.lti.cs.cmu.edu"     # Hardcoded to be the 'server' prefix followed by ".lti.cs.cmu.edu"
    print(f"[*] Connecting to {SERVER_URL}  (path={SOCKET_PATH})")
    print(f"    agent={agent_name!r}  room={room_id!r}  user={user_name!r}")

    sio.connect(
        SERVER_URL,
        socketio_path=SOCKET_PATH,
        auth=auth_payload,
        transports=["websocket"],
        wait_timeout=10
    )

    send_chat_messages()

    try:
        # Keep the connection alive; press Ctrl-C to exit.
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n[*] Shutting down…")
    finally:
        sio.disconnect()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Bazaar Socket.IO client")
    parser.add_argument(
        "--server",
        default="bree",
        help="The prefix to use for the server URL. The full URL will be https://{server}.lti.cs.cmu.edu. Default is 'bree'."
    )
    parser.add_argument(
        "--agent",
        default="jeopardybigwgu",
        help="Bazaar server's agent name without its 'agent' suffix"
    )
    parser.add_argument(
        "--room-id",
        default="20250101000",  # This MUST be different than previous sessions. Suggestion: YYYYMMDD###, with ### incremented each day
        help="Room ID"
    )
    parser.add_argument(
        "--user-name",
        default="Bot",
        help='Display name for this client. Can be constant '
    )
    parser.add_argument(
        "--user-id",
        default=100,
        help='User ID for this client. Can be constant'
    )
    args = parser.parse_args()

    main(
        agent_name=args.agent,
        room_id=args.room_id,
        user_name=args.user_name,
        user_id=args.user_id,
        server=args.server
    )