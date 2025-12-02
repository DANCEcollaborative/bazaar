import os
import pandas as pd
import csv
from datetime import datetime
import time
#from socket_io_client import BazaarSocketWrapper
import socketio
import selenium
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.firefox.options import Options
from selenium.common.exceptions import TimeoutException, NoSuchElementException
import threading
from collections import deque

class BazaarSocketWrapper():
    def __init__(self, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='jeopardybigwgu', clientID='ClientServer', environmentID='Room131', userID=1, bazaarAgent='OPEBot'):
        sio = socketio.Client()
        self.bazaarAgent = bazaarAgent
        self.socket = BazaarSocket(
            sio, endpoint, agentName, clientID, environmentID, userID, bazaarAgent)
        sio.register_namespace(self.socket)

    def connect_chat(self):
        self.socket.connect_chat()

    def sendChatMessage(self, user, message):
        self.socket.sendChatMessage(user, message)
    def sendImage(self, user, imageUrl):
        self.socket.sendImage(user, imageUrl)

    def disconnect_chat(self):
        self.socket.disconnect_chat()
class BazaarSocket(socketio.ClientNamespace):
    def __init__(self, sio=socketio.Client(), endpoint='https://bazaar.lti.cs.cmu.edu', agentName='jeopardybigwgu', clientID='ClientServer', environmentID='Room131', userID=1, bazaarAgent='OPEBot'):
        self.sio = sio
        self.namespace = '/'
        self.endpoint = endpoint
        self.agentName = agentName
        self.clientID = clientID
        self.environmentID = environmentID
        self.userID = userID
        self.bazaarAgent = bazaarAgent

        self.options = Options()
        self.options.headless = True
        self.driver = webdriver.Firefox(
            options=self.options, executable_path='/usr/local/bin/geckodriver')
        self.CHAT_MESSAGE_FUNCTION = 'window.app.collab.send("CHAT_MESSAGE", {text: arguments[0]})'
        self.MOB_USER = 'OPE_Bot'
        self.MOB_USER_PASSWORD = 'iu]8ejtGgXqv'

        self.transports = ['websocket', 'polling']
        self.token = ''
        self.path = "/bazsocket/"
        super().__init__('/')

    def connect_chat(self):
        auth = {'token': self.token,
                'agent': {'name': self.agentName, 'configuration': {'clientID': self.clientID}},
                'chat': {'id': self.environmentID},
                'user': {'id': self.userID, 'name': self.bazaarAgent}}
        print("connect_chat/auth: \n", auth)

        #self.cloud9Login()

        # Wait 2 minutes for Cloud9 login to complete
        #print(">>> socket.io - waiting 2 min for Cloud9 login to complete")
        #time.sleep(10)

        # connect to Bazaar
        print(">>> socket.io - self.sio.connect started")
        self.sio.connect(self.endpoint, auth=auth,
                         transports=self.transports, socketio_path=self.path)
        print(">>> socket.io - self.sio.connect completed")
        #print('>>> socket.io sid is ', self.sio.sid)

        # self.talk()    # Temp to start communicating with Bazaar

    def on_connect(self):
        print(">>> socket.io - connected!")

    def on_connect_error(self, error):
        print(">>> socket.io - connection failed!")

    def on_message(self, data):
        print('>>> socket.io - Message - ', data)

    def on_updatechat(self, user, data):
        print('>>> socket.io - on_updatechat - ', user, ': ', data)
        if (user != self.bazaarAgent):
            print(">>> on_updatechat, user: ", user, "bazaarAgent: ", self.bazaarAgent)
            # print('>>> socket.io - on_updatechat - forwarding chat from OTHER: ', user, "  -- msg: ", data)
            self.driver.execute_script(self.CHAT_MESSAGE_FUNCTION, data)
        # else:
        #     print('>>> socket.io - on_updatechat - NOT forwarding chat from SELF: ', user, "  -- msg: ", data)

    def disconnect_chat(self):
        self.sio.disconnect()

    def formatMultiModalMessage(self, user, message):
        multiModalMessage = "multimodal:true;%;identity:{};%;speech:\"{}\"".format(user,message)
        print('>>> formatMultimodalMessage -- ', multiModalMessage)
        return multiModalMessage

    def sendChatMessage(self, user, message):
        print('>>> socket.io - sendchat  --  ', user, ': ', message)
        #formatted_message = self.formatMultiModalMessage(user, message)
        self.sio.emit('sendchat', message)

    def sendImage(self, user, imageUrl):
        print('>>> socket.io - sendimage  --  ', user, ': ', imageUrl)
        #formatted_message = self.formatMultiModalMessage(user, message)
        self.sio.emit('sendimage', imageUrl)

class LogReplayer():
	def __init__(self, logpath, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='jeopardybigwgu', clientID='ClientServer', environmentID='Room131'):
		self.endpoint = endpoint
		self.agentName = agentName
		self.clientID = clientID
		self.environmentID = environmentID
		self.logpath = logpath
		self.entries, self.users, self.log_start_time = self.decompose_log(self.logpath)
		self.sockets = {}
		print(">>> Sockets Initialization ...")
		for i, usr in enumerate(self.users):
			self.sockets[usr] = BazaarSocketWrapper(endpoint, agentName, clientID, environmentID, userID=i+1, bazaarAgent=usr)
		print(">>> Sockets Initialization Done")
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
				#print(entry)
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
				'environmentID': '131'}
	# watch the replay at https://bazaar.lti.cs.cmu.edu/bazaar/chat/jeopardybigwgu131/20/Watcher/undefined/?html=sharing_space_chat_mm
	log_replayer = LogReplayer(logpath=logpath, **config)