#!/bin/bash
echo "script started"
cd processes
/Users/rcmurray/.pyenv/shims/python3 chat_logs.py bazaar_2021-06-11-am.csv jeopardy 06/04/21 00 06/04/21 23 >> python_output.txt
echo "script ended"