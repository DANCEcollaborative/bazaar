#!/bin/bash
AGENT=$1
cd processes
mkdir crazy_dir
/Users/rcmurray/.pyenv/shims/python3 chat_logs.py bazaar_2021-06-11-am.csv $AGENT 06/04/21 00 07/08/21 23 >> python_output.txt
mkdir crazier_dir
