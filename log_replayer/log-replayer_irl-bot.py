import os
import pandas as pd
import csv
from datetime import datetime
import time
import socketio
import selenium
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.firefox.options import Options
from selenium.common.exceptions import TimeoutException, NoSuchElementException, JavascriptException, WebDriverException
import threading
from collections import deque

class BazaarSocketWrapper():
    def __init__(self, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='jeopardybigwgu', clientID='ClientServer', environmentID='150', userID=1, bazaarAgent='Sage the Owl'):
        sio = socketio.Client()
        self.bazaarAgent = bazaarAgent
        self.socket = BazaarSocket(
            sio, endpoint, agentName, clientID, environmentID, userID, bazaarAgent)
        # register_namespace expects an instance of ClientNamespace with a namespace string
        # sio.register_namespace(self.socket)

    def login(self):
        self.socket.login()

    def connect_chat(self):
        self.socket.connect_chat()

    def sendChatMessage(self, user, message):
        self.socket.sendChatMessage(user, message)
        
    def sendImage(self, user, imageUrl):
        self.socket.sendImage(user, imageUrl)

    def disconnect_chat(self):
        self.socket.disconnect_chat()


class BazaarSocket(socketio.ClientNamespace):
    def __init__(self, sio=socketio.Client(), endpoint='https://bazaar.lti.cs.cmu.edu', agentName='jeopardybigwgu', clientID='ClientServer', environmentID='150', userID=1, bazaarAgent='OPEBot'):
        self.sio = sio
        self.namespace = '/'
        self.endpoint = endpoint
        self.agentName = agentName
        self.clientID = clientID
        self.environmentID = environmentID
        self.userID = userID
        self.bazaarAgent = bazaarAgent

        # Selenium / headless browser setup
        self.options = Options()
        self.options.headless = True
        # NOTE: adjust executable_path as needed on your system
        self.driver = webdriver.Firefox(
            options=self.options, executable_path='/usr/local/bin/geckodriver')

        # guarded JS function — returns true on success, false if app is not ready
        self.CHAT_MESSAGE_JS = """
            try {
                if (window.app && window.app.collab && typeof window.app.collab.send === 'function') {
                    window.app.collab.send("CHAT_MESSAGE", {text: arguments[0]});
                    return true;
                }
            } catch (e) {
                // swallow JS exceptions and return false to indicate failure
            }
            return false;
        """

        # buffer and readiness state
        self.message_buffer = deque()
        self.buffer_lock = threading.Lock()
        self.server_ready = False
        self._stop_watcher = threading.Event()

        self.MOB_USER = 'OPE_Bot'
        self.MOB_USER_PASSWORD = 'iu]8ejtGgXqv'

        self.transports = ['websocket', 'polling']
        self.token = ''
        self.path = "/bazsocket/"
        super().__init__('/')

        # Start readiness watcher thread (daemon)
        self.readiness_thread = threading.Thread(target=self._readiness_watcher, name=f"ReadinessWatcher-{self.bazaarAgent}", daemon=True)
        self.readiness_thread.start()

    def login(self):
        """
        Navigate to the Bazaar login page with the appropriate parameters.
        This should be called before sending any messages.
        """
        
        login_url = (f"{self.endpoint}/bazaar/login?"
                    f"roomName={self.agentName}&"
                    f"roomId={self.environmentID}&"
                    f"id=20&"
                    f"username={self.bazaarAgent}&"
                    f"html=sharing_space_chat_mm")
        
        print(f">>> Logging in {self.bazaarAgent} via Selenium: {login_url}")
        try:
            self.driver.get(login_url)
            # Give the page some time to load
            time.sleep(2)
            print(f">>> Login page loaded for {self.bazaarAgent}")
        except Exception as e:
            print(f">>> Login failed for {self.bazaarAgent}: {e}")

    def connect_chat(self):
        auth = {'token': self.token,
                'agent': {'name': self.agentName, 'configuration': {'clientID': self.clientID}},
                'chat': {'id': self.environmentID},
                'user': {'id': self.userID, 'name': self.bazaarAgent}}
        print("connect_chat/auth: \n", auth)

        # connect to Bazaar
        print(">>> socket.io - self.sio.connect started")
        try:
            self.sio.connect(self.endpoint, auth=auth,
                             transports=self.transports, socketio_path=self.path)
            print(">>> socket.io - self.sio.connect completed")
        except Exception as e:
            print(">>> socket.io - connect exception:", e)

    def on_connect(self):
        print(">>> socket.io - connected!")

    def on_connect_error(self, error):
        print(">>> socket.io - connection failed!", error)

    def on_message(self, data):
        print('>>> socket.io - Message - ', data)

    # -- buffering-aware handler for incoming chat forwarded to the Selenium page --
    def on_updatechat(self, user, data):
        # Only forward messages from others (not from the configured bazaarAgent itself)
        print('>>> socket.io - on_updatechat - ', user, ': ', data)
        if user == self.bazaarAgent:
            # don't forward our own agent's messages into the page
            return

        # Use buffering mechanism — thread-safe
        self._enqueue_or_send(data)

    def disconnect_chat(self):
        self.sio.disconnect()
        # stop watcher thread
        self._stop_watcher.set()
        try:
            if self.driver:
                self.driver.quit()
        except Exception:
            pass

    def formatMultiModalMessage(self, user, message):
        multiModalMessage = "multimodal:true;%;identity:{};%;speech:\"{}\"".format(user,message)
        print('>>> formatMultimodalMessage -- ', multiModalMessage)
        return multiModalMessage

    def sendChatMessage(self, user, message):
        print('>>> socket.io - sendchat  --  ', user, ': ', message)
        # formatted_message = self.formatMultiModalMessage(user, message)
        self.sio.emit('sendchat', message)

    def sendImage(self, user, imageUrl):
        print('>>> socket.io - sendimage  --  ', user, ': ', imageUrl)
        # formatted_message = self.formatMultiModalMessage(user, message)
        self.sio.emit('sendimage', imageUrl)

    # -------------------------
    # Buffering / readiness helpers
    # -------------------------
    def _is_frontend_ready(self):
        """
        Attempt to detect frontend readiness by executing a small JS snippet that
        returns true if window.app && window.app.collab are present and callable.
        Any exception or falsy response -> not ready.
        """
        try:
            result = self.driver.execute_script("return !!(window.app && window.app.collab && typeof window.app.collab.send === 'function');")
            return bool(result)
        except (JavascriptException, WebDriverException) as e:
            # Driver/frame might not be ready yet; treat as not ready
            return False
        except Exception:
            return False

    def _readiness_watcher(self):
        """
        Background daemon that periodically checks for frontend readiness.
        When readiness is detected, set server_ready and flush the buffer.
        Runs until _stop_watcher is set.
        """
        poll_interval = 0.5  # seconds
        consecutive_ready = 0
        required_consecutive = 1  # simple stabilization; increase if flakiness observed

        while not self._stop_watcher.is_set():
            try:
                ready = self._is_frontend_ready()
                if ready:
                    consecutive_ready += 1
                else:
                    consecutive_ready = 0

                if consecutive_ready >= required_consecutive and not self.server_ready:
                    print(f">>> Frontend appears ready for agent {self.bazaarAgent}. Flushing buffer.")
                    self.server_ready = True
                    # flush buffer in separate thread to avoid blocking watcher for long time
                    t = threading.Thread(target=self.flush_buffer, name=f"BufferFlusher-{self.bazaarAgent}", daemon=True)
                    t.start()
                elif not ready and self.server_ready:
                    # frontend lost readiness (page reload or navigation)
                    print(f">>> Frontend no longer ready for agent {self.bazaarAgent}; buffering messages.")
                    self.server_ready = False

            except Exception as e:
                # Log and continue; don't allow watcher thread to die
                print("Readiness watcher exception:", e)
            time.sleep(poll_interval)

    def _enqueue_or_send(self, message):
        """
        If frontend is ready, try sending immediately. If send fails,
        put message into buffer and mark server_ready False so watcher will retry.
        If frontend not ready, just append to buffer.
        """
        if not self.server_ready:
            with self.buffer_lock:
                self.message_buffer.append(message)
            # Debug
            print(f">>> Buffered message (not ready). Buffer size now: {len(self.message_buffer)}")
            return

        # Try to send immediately
        try:
            ok = False
            try:
                ok = self.driver.execute_script(self.CHAT_MESSAGE_JS, message)
            except (JavascriptException, WebDriverException):
                ok = False

            if ok:
                # Sent successfully
                # Optionally log success
                # print(">>> Message delivered immediately.")
                return
            else:
                # JS says not ready or failed — buffer it and mark not ready
                with self.buffer_lock:
                    self.message_buffer.append(message)
                self.server_ready = False
                print(">>> Message delivery failed (frontend not ready) — message buffered.")
        except Exception as e:
            # Any unexpected error: buffer and reset ready flag
            with self.buffer_lock:
                self.message_buffer.append(message)
            self.server_ready = False
            print(">>> Exception trying to send message; buffered. Exception:", e)

    def flush_buffer(self):
        """
        Flush messages in FIFO order. If a send fails mid-flush, put remaining messages back
        into the buffer and mark server as not ready.
        """
        if not self.server_ready:
            return

        with self.buffer_lock:
            if not self.message_buffer:
                print(">>> flush_buffer: buffer empty.")
                return
            # Move messages to a local list to avoid holding the lock while doing network ops
            to_send = list(self.message_buffer)
            self.message_buffer.clear()

        print(f">>> Flushing {len(to_send)} buffered messages...")

        for idx, msg in enumerate(to_send):
            try:
                ok = False
                try:
                    ok = self.driver.execute_script(self.CHAT_MESSAGE_JS, msg)
                except (JavascriptException, WebDriverException):
                    ok = False

                if not ok:
                    # failure — requeue remaining messages (including current)
                    remaining = to_send[idx:]
                    with self.buffer_lock:
                        # Prepend remaining to existing buffer to preserve order
                        # we extendleft reversed so that the leftmost element is the earliest
                        for m in reversed(remaining):
                            self.message_buffer.appendleft(m)
                    self.server_ready = False
                    print(f">>> flush_buffer: send failed for message index {idx}. Re-queued {len(remaining)} messages.")
                    return
                # else: success, continue to next message
            except Exception as e:
                # Unexpected exception — requeue remaining messages and mark not ready
                remaining = to_send[idx:]
                with self.buffer_lock:
                    for m in reversed(remaining):
                        self.message_buffer.appendleft(m)
                self.server_ready = False
                print(">>> flush_buffer exception: re-queued remaining messages. Exception:", e)
                return

        print(">>> flush_buffer: all buffered messages delivered successfully.")

class LogReplayer():
    def __init__(self, logpath, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='jeopardybigwgu', clientID='ClientServer', environmentID='150'):
        self.endpoint = endpoint
        self.agentName = agentName
        self.clientID = clientID
        self.environmentID = environmentID
        self.logpath = logpath
        self.entries, self.users, self.log_start_time = self.decompose_log(self.logpath)
        self.sockets = {}
        print(">>> Sockets Initialization ...")
        for i, usr in enumerate(self.users):
            # create a socket wrapper for each user; each wrapper creates its own driver and watcher
            self.sockets[usr] = BazaarSocketWrapper(endpoint, agentName, clientID, environmentID, userID=i+1, bazaarAgent=usr)
        print(">>> Sockets Initialization Done")
        
        # Login all users before replay
        # print(">>> Logging in all users ...")
        # for usr in self.users:
        #     self.sockets[usr].login()
        # print(">>> All users logged in")

        # Login bot before replay
        print(">>> Logging in bot ...")
        self.sockets[usr].login()
        print(">>> Bot logged in")
        
        self.replay()

    def decompose_log(self, logpath):
        with open(logpath, 'r') as csv_file:
            log_file = csv.reader(csv_file)
            rows = [row for row in log_file]
            headers = rows[0]
            rows = rows[1:]
            start_time = datetime.strptime(rows[0][0], '%Y-%m-%d %H:%M:%S')
            entries = []
            users = []
            for r in rows:
                entry = {}
                for i,h in enumerate(headers):
                    if h=='timestamp':
                        entry[h] = datetime.strptime(r[i], '%Y-%m-%d %H:%M:%S')
                    else:
                        entry[h] = r[i]
                entries.append(entry)
                users.append(entry['username'])
            users = list(set(users))
            return entries, users, start_time

    def replay(self):
        replay_start_time = datetime.now()
        print(">>> Start replaying at ", replay_start_time)
        for i, entry in enumerate(self.entries):
            if entry['username']=='Sage the Owl':
                continue
            if i!=0 and entry['timestamp']==self.entries[i-1]['timestamp']:
                time.sleep(0.1)
            print_time = entry['timestamp']
            if print_time - self.log_start_time > datetime.now() - replay_start_time:
                print(print_time - self.log_start_time)
                print(datetime.now() - replay_start_time)
                wait_time = (print_time - self.log_start_time) - (datetime.now() - replay_start_time)
                time.sleep(wait_time.total_seconds())
            user_socket = self.sockets[entry['username']]
            if entry['type'] == 'text':
                user_socket.sendChatMessage(user=entry['username'], message=entry['content'])
            elif entry['type'] == 'presence':
                if entry['content'] == 'join':
                    user_socket.connect_chat()
                elif entry['content'] == 'leave':
                    user_socket.disconnect_chat()
            elif entry['type'] == 'image':
                user_socket.sendImage(user=entry['username'], imageUrl=entry['content'])

if __name__ == '__main__':
    logpath = 'jeopardybigwgu2123_Hannah_DanielWolkwitz.csv'
    config = {'endpoint': 'https://bazaar.lti.cs.cmu.edu',
                'agentName': 'jeopardybigwgu',
                'clientID': 'ClientServer',
                'environmentID': '151'}
    # watch the replay at https://bazaar.lti.cs.cmu.edu/bazaar/chat/jeopardybigwgu150/50/Watcher/undefined/?html=sharing_space_chat_mm
    log_replayer = LogReplayer(logpath=logpath, **config)