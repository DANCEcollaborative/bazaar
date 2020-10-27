#!/bin/bash
#usage: ./run.sh roomname num condition [numcols [width height]]

roomname=${1:-"Week_"};
num=${2:-1};
condition=${3:-""}
cols=${4:-3};
width=${5:-300};
height=${6:-180};
rows=4;

# AW: adjust agentdir for our installation
# agentdir="/usr0/bazaar/oneport_bazaar/${roomname}agent/"
agentdir="/usr/agents/${roomname}Agent/"

echo "launch_agent: "
echo agentdir:$agentdir
echo roomname:$roomname
echo num:$num
echo condition:$condition;
roomid=$roomname$num
echo joining room $roomid

cd $agentdir

# x,y cols control GUI placement, which we suppress with -launch
x=0
y=0
echo $cols across, $width x $height at $x, $y;

echo java -Xmx128M -jar "${roomname}Agent.jar" -room $roomid -out logs -condition "$condition" -launch &

# AW: command from Chas Murray to work around lack of X11 display to render GUI
nohup java -Xmx128M -jar "${roomname}Agent.jar" -room $roomid -out logs  -condition "$condition" -launch &
