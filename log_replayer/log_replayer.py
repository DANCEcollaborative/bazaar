import os
import pandas as pd
import csv
from datetime import datetime
import time
import sys
import socketio
import selenium
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.firefox.options import Options
from selenium.common.exceptions import TimeoutException, NoSuchElementException
import threading
from multiprocessing import Process

class BazaarSocketWrapper():
    def __init__(self, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='cloudtest', clientID='ClientServer', environmentID='Room126', userID=1, bazaarAgent='User1', BotName='OPEBot'):
        sio = socketio.Client()
        self.bazaarAgent = bazaarAgent
        self.socket = BazaarSocket(
            sio, endpoint, agentName, clientID, environmentID, userID, bazaarAgent, BotName)
        sio.register_namespace(self.socket)

    def connect_chat(self, start_agent):
        self.socket.connect_chat(start_agent)

    def sendChatMessage(self, user, message):
        self.socket.sendChatMessage(user, message)
    def sendImage(self, user, imageUrl):
        self.socket.sendImage(user, imageUrl)

    def disconnect_chat(self):
        self.socket.disconnect_chat()

class BazaarSocket(socketio.ClientNamespace):
    def __init__(self, sio=socketio.Client(), endpoint='https://bazaar.lti.cs.cmu.edu', agentName='cloudtest', clientID='ClientServer', environmentID='Room126', userID=1, bazaarAgent='User1', BotName='OPEBot'):
        self.sio = sio
        self.namespace = '/'
        self.endpoint = endpoint
        self.agentName = agentName
        self.clientID = clientID
        self.environmentID = environmentID
        self.userID = userID
        self.bazaarAgent = bazaarAgent
        self.BotName = BotName
        self.bot_init_response = None

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

    def connect_chat(self, start_agent):
        auth = {'token': self.token,
                'agent': {'name': self.agentName, 'configuration': {'clientID': self.clientID}},
                'chat': {'id': self.environmentID},
                'user': {'id': self.userID, 'name': self.bazaarAgent}}
        print("    connect_chat/auth: ", auth)

        if start_agent:
            print("    >>> socket.io - Cloud9 login. Start bazaar agent.")
            try:
                self.cloud9Login()
            except Exception as e:
                print("\n* Error: socketio "+ self.bazaarAgent +" -- cloud9Login failed: ", e,"\n")
            
        # connect to Bazaar
        print("    >>> socket.io - self.sio.connect started")
        try:
            self.sio.connect(self.endpoint, auth=auth,
                             transports=self.transports, socketio_path=self.path)
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- connection failed: ", e,"\n")
        #print("    >>> socket.io - self.sio.connect completed")
        #print('>>> socket.io sid is ', self.sio.sid)

    def on_connect(self):
        print("    >>> socket.io - connected!")

    def on_connect_error(self, error):
        print("    >>> socket.io - connection failed!")

    def on_message(self, data):
        print('    >>> socket.io - Message - ', data)

    def on_updatechat(self, user, data):
        if (user != self.bazaarAgent):
            print("    >>> on_updatechat, From: ", user, " To: ", self.bazaarAgent, " content: ", data)
            if user == self.BotName and self.bot_init_response==None:
                self.bot_init_response = datetime.now()
                print("    >>> bot_init_response: ", self.bot_init_response)
            #self.driver.execute_script(self.CHAT_MESSAGE_FUNCTION, data)
        
    def disconnect_chat(self):
        try:
            self.sio.disconnect()
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- disconnection failed: ", e,"\n")
    '''
    def formatMultiModalMessage(self, user, message):
        multiModalMessage = "multimodal:true;%;identity:{};%;speech:\"{}\"".format(user,message)
        print('    >>> formatMultimodalMessage -- ', multiModalMessage)
        return multiModalMessage
    '''

    def sendChatMessage(self, user, message):
        print('    >>> socket.io - sendchat  --  ', user, ': ', message)
        #formatted_message = self.formatMultiModalMessage(user, message)
        try:
            self.sio.emit('sendchat', message)
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- sending message failed: ", e,"\n")

    def sendImage(self, user, imageUrl):
        print('    >>> socket.io - sendimage  --  ', user, ': ', imageUrl)
        #formatted_message = self.formatMultiModalMessage(user, message)
        try:
            self.sio.emit('sendimage', imageUrl)
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- sending image failed: ", e,"\n")

    def cloud9Login(self):
        login_url = "https://752574329361.signin.aws.amazon.com/console"
        environment_url = "https://console.aws.amazon.com/cloud9/ide/{}".format(
            self.environmentID)
        self.driver.get(login_url)

        username_element = WebDriverWait(self.driver, 20, ignored_exceptions=(
            selenium.common.exceptions.NoSuchElementException,
            selenium.common.exceptions.StaleElementReferenceException,)).until(
                EC.presence_of_element_located((By.ID, "username")))

        # Some weirdness going on with the element rewriting itself?
        time.sleep(1)
        username_element = WebDriverWait(self.driver, 20, ignored_exceptions=(
            selenium.common.exceptions.NoSuchElementException,
            selenium.common.exceptions.StaleElementReferenceException,)).until(
                EC.presence_of_element_located((By.ID, "username")))

        try:
            innerHTML = self.driver.execute_script("return document.body")
        except Exception as e:
            print(">>> socketio -- Failed to dump innerHTML after waiting for username to appear\n", e)

        password_element = self.driver.find_element_by_id('password')

        try:
            username_element.send_keys(self.MOB_USER)
        except Exception as e:
            print(">>> socketio -- send_keys(self.MOB_USER) failed\n", e)
        try:
            password_element.send_keys(self.MOB_USER_PASSWORD)
        except Exception as e:
            print(">>> socketio -- send_keys(self.MOB_USER_PASSWORD) failed\n", e)

        signin_button = self.driver.find_element_by_id('signin_button')

        try:
            signin_button.click()
        except Exception as e:
            print(">>> socketio -- signin_button.click failure\n", e)

        try:
            WebDriverWait(self.driver, 20).until(
                EC.presence_of_element_located((By.ID, 'nav-logo')))
        except Exception as e:
            pass

        self.driver.get(environment_url)


class LogReplayer():
    def __init__(self, logpath=None, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='cloudtest', clientID='ClientServer', environmentID='Replayer', BotName='OPEBot'):
        self.endpoint = endpoint
        self.agentName = agentName
        self.clientID = clientID
        self.environmentID = environmentID
        self.BotName = BotName
        self.logpath = logpath
        self.log_bot_init_response = None
        self.replay_bot_init_response = None
        self.entries, self.users, self.log_start_time = self.decompose_log(self.logpath)
        self.sockets = {}
        print(">>> ", environmentID, " replaying log at ", logpath)
        print(">>> Sockets Initialization ...\n")
        for i, usr in enumerate(self.users):
            self.sockets[usr] = BazaarSocketWrapper(endpoint, agentName, clientID, environmentID, userID=i+1, bazaarAgent=usr, BotName=BotName)
            print("environmentID: ", environmentID, "userID: ", i+1, " userName: ", usr)
        print("\n>>> Sockets Initialization Done")


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
                if entry['username']!=self.BotName:
                    users.append(entry['username'])
            users = list(set(users))
            return entries, users, start_time
	
    def replay(self):
        replay_start_time = datetime.now()
        print("==================================")
        print(">>> ", self.environmentID, " start replaying at ", replay_start_time)
        print("==================================")
        start_agent = True
        for i, entry in enumerate(self.entries):
            if entry['username']==self.BotName:
                if self.log_bot_init_response == None and entry['type']=='text':
                    self.log_bot_init_response = entry['timestamp']
                continue
            if i!=0 and entry['timestamp']==self.entries[i-1]['timestamp']:
                time.sleep(0.1)

            print_time = entry['timestamp']
            user_socket = self.sockets[entry['username']]
            if self.log_bot_init_response==None and print_time - self.log_start_time > datetime.now() - replay_start_time:
                wait_time = (print_time - self.log_start_time) - (datetime.now() - replay_start_time)
                time.sleep(wait_time.total_seconds())
            if self.log_bot_init_response!=None:
                print(">>> waiting for bazaar agent's initial message ... ")
                while self.replay_bot_init_response==None:
                    for usr, so in self.sockets.items():
                        if so.socket.bot_init_response!=None:
                            print(usr, so.socket.bot_init_response)
                            self.replay_bot_init_response = so.socket.bot_init_response
                            break
                if print_time - self.log_bot_init_response > datetime.now() - self.replay_bot_init_response:
                    wait_time = (print_time - self.log_bot_init_response) - (datetime.now() - self.replay_bot_init_response)
                    print("Wait    ", self.replay_bot_init_response)
                    time.sleep(wait_time.total_seconds())
            
            if entry['type'] == 'text':
                user_socket.sendChatMessage(user=entry['username'], message=entry['content'])
                print(">>> "+entry['username']+" : "+entry['content'])
            elif entry['type'] == 'presence':
                if entry['content'] == 'join':
                    user_socket.connect_chat(start_agent=start_agent)
                    if start_agent:
                        start_agent=False
                    print(">>> "+entry['username']+" has connected")
                elif entry['content'] == 'leave':
                    user_socket.disconnect_chat()
                    print(">>> "+entry['username']+" has disconnected")
            elif entry['type'] == 'image':
                user_socket.sendImage(user=entry['username'], imageUrl=entry['content'])
        return


			
if __name__ == '__main__':
    log_folder_path = '../bazaar_chat_logs/jeopardy_chat_logs/wgu_2021-10-24/'
    #log_file_path = '../bazaar_chat_logs/jeopardy_chat_logs/wgu_2021-10-24/jeopardybigwgu2124_RachelMyron_Kelsey.csv'
    log_file_path = None
    config = {'endpoint': 'https://bazaar.lti.cs.cmu.edu', 
                'agentName': 'jeopardybigwgu', # your agent’s name without the ‘agent’ at the end
                'clientID': 'ClientServer', 
                'environmentID': 'Replay', # a unique number of not more than 5 digits. Change an environmentID if you run the same agent again.
                'BotName': 'Sage the Owl'} # The name of the online tutor

    if log_file_path and log_folder_path and not (log_file_path.startswith(log_folder_path)):
        sys.exit('* Error: You are replaying logs from two different sources. Please choose either a folder or a single log file to replay. *')
    elif log_file_path:
        replay_single_file = True
        print("* Replaying a single log at "+log_file_path+" *")
    elif log_folder_path:
        replay_single_file = False
        print("* Replaying the logs in "+log_folder_path +" *")
    elif log_file_path==None and log_file_path==None:
        sys.exit("* Please choose either a folder or a single log file to replay. *")
    
    if replay_single_file:
        config['environmentID'] = 'ReplayAt' + datetime.now().strftime("%Y%m%d%H%M%S")
        single_log_replayer = LogReplayer(log_file_path, **config)
        single_log_replayer.replay()
    else:
        chatId = 1
        log_replayers = []
        replay_threads = []
        for logpath in os.listdir(log_folder_path):
            logpath = os.path.join(log_folder_path, logpath)
            if not os.path.exists(logpath) or not logpath.endswith('csv'):
                print("* Invalid log file: "+logpath+" *")
                continue
            config['environmentID'] = 'Replay' + str(chatId) + 'At' + datetime.now().strftime("%Y%m%d%H%M%S")
            single_log_replayer = LogReplayer(logpath, **config)
            t = threading.Thread(target=single_log_replayer.replay)
            log_replayers.append(single_log_replayer)
            replay_threads.append(t)
            chatId+=1

        for t in replay_threads:
            t.start()
            
        for t in replay_threads:
            t.join()
    print("* Finish! *")