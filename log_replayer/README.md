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
Arguments:

--replay_path	#A folder or a single log file to replay.
--agent_name	#Your agent’s name without the ‘agent’ at the end. e.g. 'jeopardybigwgu'
--bot_name	#The name of the online tutor. e.g. 'Sage the Owl'
```

- If a single log is being replayed, the environmentID (a.k.a. roomID) is "ReplayAt+timestamp". Multiple logs can be replayed concurrently using multiple threads. If multiple logs are being replayed, the environmentID for each chat is formatted as "Replay+chatID+At+timestamp" where chatID is a unique number for each log.

- There will be a replayed log file created by the log replayer for each log. The name of the replayed log file is 'path_to_log_file+environmentID.csv', so you can find the replayed log file at the same place where the log is. The replayed log files record what the users see, so the replayed log files contain text messages before all the users leave the room. The replayed log files only contain text and presence messages, not images.

- System errors during the replay will be printed to the console starting with "* Error:", and also be recorded in the replayed log files.

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

- There may be errors in the original chat logs. For example, a user shown disconnected can still talk in the chat and create entries in the log, but in the replayer, this will raise errors because the socket is aleady disconnected. The errors are printed in the console and replayed log files. 

