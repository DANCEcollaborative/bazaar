#!/bin/bash
#usage: ./run.sh roomname num condition [numcols [width height]]

roomname=${1:-"Week_"};
num=${2:-1};
condition=${3:-""}
cols=${4:-3};
width=${5:-300};
height=${6:-180};
rows=4;

jar_name= "talk"
agentdir="/usr0/home/gtomar/bazaar/oneport_bazaar_mturk/${roomname}agent/"

echo $agentdir
echo hello
echo $jar_name
echo bye
cd $agentdir
echo loading room $roomname$num;
echo condition:$condition;

x=$(( (($num-1) % $cols)*$width ))
y=$(( ((($num-1) / $cols)%rows)*$height ))

echo $cols across, $width x $height at $x, $y;

echo java -Xmx128M -jar "${roomname}agent.jar" -room $roomname$num -out logs -x$x -y$y -condition "$condition" -launch &

java -jar "${roomname}agent.jar" -room $roomname$num -out logs -x$x -y$y -condition "$condition" -launch &
