#!/bin/bash

reportfile="test-input-received.txt"

while [ 1 = 1 ]
do
  read response
  echo $response >> $reportfile
  echo $response
done
