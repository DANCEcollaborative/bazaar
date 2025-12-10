# Log Replayer

The Log Replayer plays transcripts of timed user/student messages along with Bazaar agent ("bot") responses that are either pre-recorded within the transcript or real-time (i.e., "fresh") responses from a bot running on a server. Bazaar agents optionally support creating such transcripts, called *chat logs*, that can be used without modification as input to the Log Replayer. Alternatively, transcripts of user/student messages can be created and replayed to see how a real-time bot would respond to the sequence of messages.

## Two versions of the Log Replayer

There are two versions of the Log Replayer: 

1. ```log_replayer.py```: Replays a chat log, including the recorded (previous) contributions of the bot.
2. ```log-replayer_fresh-bot.py```: Replays the non-bot portion of a chat log, interfacing with an online bot for fresh bot contributions. 

For both versions, a chat log of the replay is created. 

## Environment Setup

- Python version: 3.10

- To create a python3 virtual environment:
```
python3 -m venv <name_of_virtualenv>
source <name_of_virtualenv>/bin/activate
```
- Install packages:
`pip install -r log_replayer_requirements.txt`

## Sample Chat Log 
Logs must be in the following .csv format (this is an abbreviated sample log). In this log, the bot's name is "Sage the Owl." For replay using a fresh bot, bot responses may or may not be included in the chat log. If they are included in a chat log, the Log Replayer will skip over them and instead play any responses that will be received from the fresh bot. 
```
timestamp,username,type,content
2025-12-04 15:37:13,Joe Cool,presence,join
2025-12-04 15:37:20,Jane Cooler,presence,join
2025-12-04 15:37:29,Sage the Owl,text,Hello! I'm Sage. Take a minute to introduce yourselves. Go ahead and chat a little more if you have extra time. :-)
2025-12-04 15:38:04,Jane Cooler,text,I'm Jane.
```



## 1. ```log_replayer.py```: Replays previous bot messages
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

### Difference between the replay and the original chat log

- The order of the bazaar agent's presence may be different because the log replayer's bazaar agent is started together with the first user's presence, so bazaar agent will not be the first one to appear. In this way, it is guaranteed that the user socket can always catch the first message from the bazaar agent. The time of emitting user messages in the following entries is aligned with the time of bazaar agent's first message. 

- There may be errors in the original chat logs. For example, a user shown disconnected can still talk in the chat and create entries in the log, but in the replayer, this will raise errors because the socket is aleady disconnected. The errors are printed in the console and replayed log files. 



## 2. ```log-replayer_fresh-bot.py```: Plays fresh bot messages
Much of this replayer's functionality is the same as```log_replayer.py```. Differences and additions are listed below.

The replayed user/student messages and fresh bot messages will be displayed in both the system console and in the saved replay log. 

The chat log may *optionally* include recorded (previous) contributions from the bot, but only fresh bot contributions will be played.

**Generally-required Arguments:**

- **--replay_path PATH**: A folder or a single log file to replay. Default is ```.```. 
- **--agent_name NAME**: The bot agent’s name in lower case without the ‘agent’ at the end. Default is  ```jeopardybigwgu```.
- **--bot_name NAME**: The name of the fresh online bot. Default is ```Sage the Owl```. 



**Optional Arguments:**

- **--headless**: Run without displaying the replay in a browser window. Default is to replay in a browser window.
- **--init_delay SECONDS**: Time to delay replay after connecting with the fresh bot. Useful to avoid issues with bot startup delay. Default is ```15``` seconds.
- **--end-delay SECONDS**: Time to extend replay after the chat log ends to collect any additional messages from the fresh bot. Default is ```30``` seconds.
- **--html_page PAGE**: Any HTML page listed in the bot's ```html_pages``` directory in which to display the chat within a browser. Don't include the ```.html``` suffix in the PAGE value. Default is ```sharing_space_chat```.
- **--server SERVER**: The server that will run the fresh bot. Default is  ```'https://bazaar.lti.cs.cmu.edu'```. 

## Add optional "Observer" to the corresponding Bazaar agent

To support viewing the replay in a browser in real time without affecting the playback by making the bot think that another student/user is in the session, an "Observer" participant can be defined as a 'non_user_client_name' in the Bazaar agent's PresenceWatcher.properties file *on the server*. 

Sample PresenceWatcher.properties file:
```
#this listener triggers a "launch event" to trigger the macro-script
#after either the expected number of students or the timeout (in seconds) has been reached.
expected_number_of_students=3
launch_timeout=120
non_user_client_name=Observer
```

## Sample Invocations

1. Play a single log with bot agent JeopardyBigWGUAgent and an initial delay of 10 seconds using HTML page ```share_chat.html```: 
    - ```python log-replayer_fresh-bot.py --agent_name=jeopardybigwgu --bot_name='Sage the Owl' --replay_path='test_log.csv' --init_delay=10 --html_page=share_chat``` 
2. Play the set of logs in directory ```logs``` with bot agent FcdsP3Agent and an ending delay of 1 minute on server 'https://bree.lti.cs.cmu.edu':
  
    - ```python log-replayer_fresh-bot.py --agent_name=fcdsp3 --bot_name='OPEBot' --replay_path='logs' --end_delay=60 --server='https://bree.lti.cs.cmu.edu'```

3. Same as (2) but headless:

    - ```python log-replayer_fresh-bot.py --agent_name=fcdsp3 --bot_name='OPEBot' --replay_path='logs' --headless --end_delay=60 --server='https://bree.lti.cs.cmu.edu'```


## Sample system console output

The replay log will be very similar to the input chat log except that the bot contributions will be from the fresh bot. 

Sample system console (i.e. terminal) output follows. In the terminal, the lines will wrap.
```
$ python log-replayer_fresh-bot.py --agent_name=fcdsp3 --bot_name='OPEBot' --replay_path='fcdsp3ope-log1.csv' --init_delay=10 --end_delay=15
* Replaying a single log at fcdsp3ope-log1.csv *
>>>  ReplayAt20251209130018  replaying log at  fcdsp3ope-log1-shortened-task1.csv
>>> replay_csv_file:  fcdsp3ope-log1-shortened-task1_ReplayAt20251209130018.csv
roomID:  ReplayAt20251209130018 userID:  1  userName:  Billy Bob
roomID:  ReplayAt20251209130018 userID:  2  userName:  Mary Ann
roomID:  ReplayAt20251209130018 userID:  3  userName:  Jane Cooler
roomID:  ReplayAt20251209130018 userID:  4  userName:  Joe Cool


>>>>> Observer URL: https://bazaar.lti.cs.cmu.edu/bazaar/chat/fcdsp3ReplayAt20251209130018/50/Observer/undefined/?html=sharing_space_chat_mm


sleeping for initial delay after login:  10
Done sleeping

==================================
>>>  ReplayAt20251209130018  start replaying at  2025-12-09 13:00:33.672291
==================================

>>> Jane Cooler has connected


* Error: socketio Jane Cooler -- connection failed:  Already connected 

>>> Jane Cooler has connected

OPEBot :  Welcome! Before starting please make sure you have attempted the pre-quiz. You'll get credit just for submitting it. 

OPEBot :  We're starting! I'm OPE_Bot. 

OPEBot :  Beginning now, you will have approximately 80 minutes to complete the OPE tasks and submit to receive your grades. 

>>> Mary Ann has connected

OPEBot :  Okay! Let's assign your initial roles and then you can get started. We will rotate roles for each task. 

OPEBot :  Your initial roles are -
Driver - Mary Ann. 
Navigator - Jane Cooler. 
Researcher - Billy Bob. 
Recall that the researcher refers to resources like the primer as necessary. The project manager role is unassigned because this OPE is designed for teams of three. 

OPEBot :  You can begin the exercise. The Jupyter Notebook includes everything you need, including the submission code. Please read the instructions carefully. 

>>> Joe Cool has connected

Jane Cooler :  Give me a sec to read 

Mary Ann :  Okay 

Mary Ann :  Let me know when you're ready to proceed. 

Jane Cooler :  Ight let's do this 

Joe Cool :  Woot! 

Jane Cooler :  I think PLOT_TYPE should be 'bar' 

    >>> socket.io - sendfile  --   Jane Cooler :  testcase-complete_1
OPEBot :  You've passed the testcase! 

OPEBot :  Which genres have the highest production rates? Name a few. 

OPEBot :  Ok, we are switching roles now. Let's move on to the second analysis. 

OPEBot :  The new roles are -
Driver - Jane Cooler. 
Navigator - Mary Ann. 
Researcher - Joe Cool. 
Whoever doesn't have an assignment is a researcher for this round. Recall that the researcher refers to resources like the primer as necessary. 
```