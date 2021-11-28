# Environment Setup

- Python version: 3.x

- To create a python3 virtual environment:
```
python3 -m venv <name_of_virtualenv>
source <name_of_virtualenv>/bin/activate
```
- Install packages:
`pip install -r log_replayer_requirements.txt`

# Instructions
```
BazaarSocket.driver = webdriver.Firefox(
            options=self.options, executable_path='</usr/local/bin/geckodriver>') # use your path to geckodriver

log_folder_path = '<path to log folder>' or None
log_file_path = '<path to a single log file>' or None
config = {'endpoint': 'https://bazaar.lti.cs.cmu.edu', # The name or IP address of your Linux server
          'agentName': 'cloudtest', # your agent’s name without the ‘agent’ at the end
          'clientID': 'ClientServer', 
          'environmentID': 'Replay',
          'BotName': 'OPEBot' # The name of the online tutor
         } 
```

- To replay a single log file, set log_file_path to the path of the log file and set log_folder_path=None. To replay all the log files in a folder, set log_folder_path to the path of the folder and set log_file_path=None. If log_file_path contains log_folder_path, it will replay the single file at log_file_path. In other cases, the log replayer will throw error and terminate. 

- Multiple logs are replayed concurrently using multiple threads. The environmentID of each chat is formatted as "Replay+chatID+datetime.now"

- Any system error during the replay will be printed to the console starting with "* Error:".

- You can get environmentID, userID, and userName when you run the log_replayer. It is printed in the format ("environmentID: ", #, "userID: ", #, " userName: ", #) at the beginning. 

- Then you can watch the replay at https://bazaar.lti.cs.cmu.edu/bazaar/chat/{agentName}{environmentID}/{userID}/{userName}/undefined/?html=index&forum=undefined. This will create a new entry 'userName present' in the chat log, but it will not be treated as a new user since the userID has appeared before.

- In the console, you can see the printed messages like the following.

```
>>>  Replay1at20211117194814  replaying log at  ../bazaar_chat_logs/jeopardy_chat_logs_mini/jeopardybigwgu2120_Libby_marielalara.csv
>>> Sockets Initialization ...

environmentID:  Replay1at20211117194814 userID:  1  userName:  Libby
environmentID:  Replay1at20211117194814 userID:  2  userName:  marielalara

>>> Sockets Initialization Done
>>>  Replay2at20211117194818  replaying log at  ../bazaar_chat_logs/jeopardy_chat_logs_mini/jeopardybigwgu2124_RachelMyron_Kelsey.csv
>>> Sockets Initialization ...

environmentID:  Replay2at20211117194818 userID:  1  userName:  Kelsey
environmentID:  Replay2at20211117194818 userID:  2  userName:  RachelMyron

>>> Sockets Initialization Done
==================================
>>>  Replay1at20211117194814  start replaying at  2021-11-17 19:48:22.898772
==================================
==================================
>>>  Replay2at20211117194818  start replaying at  2021-11-17 19:48:22.899905
==================================
```

# Difference between the replay and the original chat log

- The order of the bazaar agent's presence may be different because the log replayer's bazaar agent is started together with the first user's presence, so bazaar agent will not be the first one to appear. In this way, it is guaranteed that the user socket can always catch the first message from the bazaar agent. The time of emitting user messages in the following entries is aligned with the time of bazaar agent's first message. 

- There may be errors in the original chat logs. For example, a user shown disconnected can still talk in the chat and create entries in the log, but in the replayer, this will raise errors because the socket is aleady disconnected. The errors are printed in the replay console. 

