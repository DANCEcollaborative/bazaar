#!/usr/bin/env python3
"""
Bazaar Socket.IO Client
Connects to the Bazaar NodeJS server and initiates a socket.io session.

Usage:
    python3 bazaar-client.py [--agent AGENT_NAME] [--chat-id CHAT_ID] [--user-name USER_NAME] [--user-id USER_ID]

Example:
    python3 bazaar-client.py --agent jeopardybigwgu --chat-id 250101000 --user-name "Bot" --user-id 100
"""

import argparse
import time
import socketio

# ──────────────────────────────────────────────
# CONSTANTS 
# ──────────────────────────────────────────────
SERVER_URL   = "https://bazaar.lti.cs.cmu.edu"
SOCKET_PATH  = "/bazsocket"          # socket.io endpoint
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
# Main
# ──────────────────────────────────────────────
def main(agent_name: str, chat_id: str, user_id: str, user_name: str):
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
            "id": chat_id
        },
        "user": {
            "id": user_id,                     
            "name": user_name
        }
    }

    print(f"[*] Connecting to {SERVER_URL}  (path={SOCKET_PATH})")
    print(f"    agent={agent_name!r}  chat={chat_id!r}  user={user_name!r}")

    sio.connect(
        SERVER_URL,
        socketio_path=SOCKET_PATH,
        auth=auth_payload,
        transports=["websocket"],  
        wait_timeout=10
    )

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
        "--agent",
        default="jeopardybigwgu",
        help="Bazaar server's agent name without its 'agent' suffix"
    )
    parser.add_argument(
        "--chat-id",
        default="20250101000",  # This MUST be different than previous sessions. Suggestion: YYYYMMDD###, with ### incremented each day
        help="Chat / room ID"
    )
    parser.add_argument(
        "--user-name",
        default="WhisperBot",
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
        chat_id=args.chat_id,
        user_name=args.user_name,
        user_id=args.user_id
    )