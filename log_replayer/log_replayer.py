import os
import argparse
import pandas as pd
import csv
from datetime import datetime
import time
import sys
import socketio
import threading

def replay_csv_file_writer(replay_csv_file, log_entries):
    with open(replay_csv_file, mode='w') as csv_file:
        fieldnames = ['timestamp', 'username', 'type', 'content']
        writer = csv.DictWriter(csv_file, fieldnames=fieldnames)
        writer.writeheader()
        for e in log_entries:
            writer.writerow({'timestamp':e[0].strftime("%Y-%m-%d %H:%M:%S"), 'username':e[1], 'type':e[2], 'content':e[3]})

class BazaarSocketWrapper():
    def __init__(self, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='cloudtest', clientID='LogReplayer', environmentID='Room126', userID=1, bazaarAgent='User1', BotName='OPEBot'):
        sio = socketio.Client()
        self.bazaarAgent = bazaarAgent
        self.socket = BazaarSocket(
            sio, endpoint, agentName, clientID, environmentID, userID, bazaarAgent, BotName)
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
    def __init__(self, sio=socketio.Client(), endpoint='https://bazaar.lti.cs.cmu.edu', agentName='cloudtest', clientID='LogReplayer', environmentID='Room126', userID=1, bazaarAgent='User1', BotName='OPEBot'):
        self.sio = sio
        self.namespace = '/'
        self.replay_log_entries = []
        self.endpoint = endpoint
        self.agentName = agentName
        self.clientID = clientID
        self.environmentID = environmentID
        self.userID = userID
        self.bazaarAgent = bazaarAgent
        self.BotName = BotName
        self.bot_init_response = None
        
        self.transports = ['websocket', 'polling']
        self.token = ''
        self.path = "/bazsocket/"
        super().__init__('/')

    def connect_chat(self):
        auth = {'token': self.token,
                'agent': {'name': self.agentName, 'configuration': {'clientID': self.clientID}},
                'chat': {'id': self.environmentID},
                'user': {'id': self.userID, 'name': self.bazaarAgent}}
        print("    connect_chat/auth: ", auth)
        
        # connect to Bazaar
        print("    >>> socket.io - self.sio.connect started")
        try:
            self.sio.connect(self.endpoint, auth=auth,
                             transports=self.transports, socketio_path=self.path)
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'presence', 'join'])
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- connection failed: ", e,"\n")
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'presenceERROR', 'join'])

    def on_connect(self):
        print("    >>> socket.io - connected!")

    def on_connect_error(self, error):
        print("    >>> socket.io - connection failed!")

    def on_message(self, data):
        print('    >>> socket.io - Message - ', data)

    def on_updatechat(self, user, data):
        if (user != self.bazaarAgent):
            print("    >>> on_updatechat, From: ", user, " To: ", self.bazaarAgent, " content: ", data)
            if user == self.BotName:
                self.replay_log_entries.append([datetime.now(), self.BotName, 'text', data])
            if user == self.BotName and self.bot_init_response==None:
                self.bot_init_response = datetime.now()
                print("    >>> bot_init_response: ", self.bot_init_response)
        
    def disconnect_chat(self):
        try:
            self.sio.disconnect()
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'presence', 'leave'])
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- disconnection failed: ", e,"\n")
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'presenceERROR', 'leave'])
    

    def sendChatMessage(self, user, message):
        print('    >>> socket.io - sendchat  --  ', user, ': ', message)
        try:
            self.sio.emit('sendchat', message)
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'text', message])
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- sending message failed: ", e,"\n")
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'textERROR', message])

    def sendImage(self, user, imageUrl):
        print('    >>> socket.io - sendimage  --  ', user, ': ', imageUrl)
        try:
            self.sio.emit('sendimage', imageUrl)
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'image', imageUrl])
        except Exception as e:
            print("\n* Error: socketio "+ self.bazaarAgent +" -- sending image failed: ", e,"\n")
            self.replay_log_entries.append([datetime.now(), self.bazaarAgent, 'imageERROR', imageUrl])
    

class LogReplayer():
    def __init__(self, logpath=None, endpoint='https://bazaar.lti.cs.cmu.edu', agentName='cloudtest', clientID='LogReplayer', environmentID='Replayer', BotName='OPEBot'):
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
        
        self.replay_csv_file = self.logpath.replace('.csv', '_'+self.environmentID+'.csv')
        print(">>> replay_csv_file: ", self.replay_csv_file)
        
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
                if entry['username']!=self.BotName:
                    users.append(entry['username'])
            users = list(set(users))
            return entries, users, start_time
    
    def replay(self):
        replay_start_time = datetime.now()
        print("==================================")
        print(">>> ", self.environmentID, " start replaying at ", replay_start_time)
        print("==================================")
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
                if self.replay_bot_init_response == None:
                    print(">>> waiting for bazaar agent's initial message ... ")
                while self.replay_bot_init_response==None:
                    for usr, so in self.sockets.items():
                        if so.socket.bot_init_response!=None:
                            print(usr, " receive the bot's initial response at ", so.socket.bot_init_response)
                            self.replay_bot_init_response = so.socket.bot_init_response
                            break
                if print_time - self.log_bot_init_response > datetime.now() - self.replay_bot_init_response:
                    wait_time = (print_time - self.log_bot_init_response) - (datetime.now() - self.replay_bot_init_response)
                    print("Wait    ", wait_time)
                    time.sleep(wait_time.total_seconds())
            
            if entry['type'] == 'text':
                user_socket.sendChatMessage(user=entry['username'], message=entry['content'])
                print(">>> "+entry['username']+" : "+entry['content'])
            elif entry['type'] == 'presence':
                if entry['content'] == 'join':
                    user_socket.connect_chat()
                    print(">>> "+entry['username']+" has connected")
                elif entry['content'] == 'leave':
                    user_socket.disconnect_chat()
                    print(">>> "+entry['username']+" has disconnected")
            elif entry['type'] == 'image':
                user_socket.sendImage(user=entry['username'], imageUrl=entry['content'])
        
        if self.entries[-1]['timestamp'] - self.log_start_time > datetime.now() - replay_start_time:
            wait_time = (self.entries[-1]['timestamp'] - self.log_start_time) - (datetime.now() - replay_start_time)
            print(">>> Waiting for bazaar agent to end the session. Wait    ", wait_time)
            time.sleep(wait_time.total_seconds())
        time.sleep(60)
        print(">>> Writing replay log to ", self.replay_csv_file)
        log_entries = []
        for usr, so in self.sockets.items():
            log_entries.extend(so.socket.replay_log_entries)
        log_entries = sorted(log_entries, key=lambda x: x[0])
        cleaned_log_entries = []
        for i,e in enumerate(log_entries):
            if i>0 and log_entries[i][1]==log_entries[i-1][1] and log_entries[i][2]==log_entries[i-1][2] and log_entries[i][3]==log_entries[i-1][3]:
                continue
            cleaned_log_entries.append(e)
        replay_csv_file_writer(self.replay_csv_file, cleaned_log_entries)
        
        return

def get_args_parser():
    parser = argparse.ArgumentParser('Set log_replayer arguments', add_help=False)
    parser.add_argument('--replay_path', type=str, default='', help="A folder or a single log file to replay.")
    parser.add_argument('--agent_name', type=str, default='', help="Your agent’s name without the ‘agent’ at the end. e.g. 'jeopardybigwgu'")
    parser.add_argument('--bot_name', type=str, default='', help="The name of the online tutor. e.g. 'Sage the Owl'")
    return parser

def main(args):
    replay_path = args.replay_path
    agent_name = args.agent_name
    bot_name = args.bot_name
    
    if os.path.isdir(replay_path):
        replay_single_file = False
        print("* Replaying the logs in "+replay_path +" *")
    elif os.path.isfile(replay_path):
        replay_single_file = True
        print("* Replaying a single log at "+replay_path+" *")
    else:
        sys.exit("* Please choose either a folder or a single log file to replay. *")

    
    config = {'endpoint': 'https://bazaar.lti.cs.cmu.edu', 
                'agentName': agent_name,
                'clientID': 'LogReplayer', 
                'environmentID': 'Replay', 
                'BotName': bot_name}
    
    if replay_single_file:
        config['environmentID'] = 'ReplayAt' + datetime.now().strftime("%Y%m%d%H%M%S")
        single_log_replayer = LogReplayer(replay_path, **config)
        single_log_replayer.replay()
    else:
        chatId = 1
        log_replayers = []
        replay_threads = []
        for logpath in os.listdir(replay_path):
            logpath = os.path.join(replay_path, logpath)
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



if __name__ == '__main__':
    
    parser = get_args_parser()
    args = parser.parse_args()
    main(args)

    