#!/bin/bash

MAXHEAP="8g"
OS_ARGS=""
#OTHER_ARGS=""
OTHER_ARGS="-XX:+UseConcMarkSweepGC"

if [ `uname` == "Darwin" ]; then
    OS_ARGS="-Xdock:icon=toolkits/icons/bulbs/bulb_128.png -Xdock:name=LightSide"
elif [ `uname` == "Linux" ]; then
    OS_ARGS="-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel"
fi

if [[ -z "$1" ]]; then
    echo 'no DISPLAY variable set. Using DISPLAY=:0.0...' 
    export DISPLAY=:0.0
fi

MAIN_CLASS="edu.cmu.side.Workbench"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-java-1.96-SNAPSHOT.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
    
java $OS_ARGS -Xmx$MAXHEAP $OTHER_ARGS -splash:toolkits/icons/logo.png -classpath $CLASSPATH $MAIN_CLASS $@

