#!/bin/bash
cd -- "$(dirname "$BASH_SOURCE")"
date >> ./lightside_log.log
bash run.sh >> ./lightside_log.log 2>&1 &
