#!/bin/bash
ROOM_PREFIX=llmcamerafcds-p2-26-fall-1-room260910046
AGENT_DIRECTORY=../agents/llmcameraagent
yesterday=$(date -v-1d +%m/%d/%y)
python3 chat_logs.py ~/Downloads/$ROOM_PREFIX.csv $ROOM_PREFIX --startdate $yesterday --starttime '00'
# rm /mysql-files/$ROOM_PREFIX.csv