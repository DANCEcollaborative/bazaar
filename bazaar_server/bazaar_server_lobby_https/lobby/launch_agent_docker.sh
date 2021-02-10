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
agentdir="../${roomname}agent/"

echo $agentdir
echo hello
echo $jar_name
echo bye
cd $agentdir
echo loading room $roomname$(printf '%02d' $num);
echo condition:$condition;

x=$(( (($num-1) % $cols)*$width ))
y=$(( ((($num-1) / $cols)%rows)*$height ))

echo $cols across, $width x $height at $x, $y;

# echo nohup xvfb-run -a -e /dev/null java -Xmx128M -jar "${roomname}agent.jar" -room $roomname$(printf '%02d' $num) -out logs -x$x -y$y -condition "$condition" -launch &

echo nohup java -jar "${roomname}agent.jar" -room $roomname$(printf '%02d' $num) -out logs -x$x -y$y -condition "$condition" -launch &

nohup java -jar "${roomname}agent.jar" -room $roomname$(printf '%02d' $num) -out logs -x$x -y$y -condition "$condition" -launch &
