#!/bin/bash

# Usage: scripts/predicton_server.sh 
# Runs the prediction server on port 8000


MAXHEAP="4g"
OS_ARGS=""
OTHER_ARGS="-XX:+UseConcMarkSweepGC -Djava.awt.headless=true"

MAIN_CLASS="edu.cmu.side.recipe.PredictionServer"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-java-1.96-SNAPSHOT.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
        
java $OS_ARGS -Xmx$MAXHEAP $OTHER_ARGS -classpath $CLASSPATH $MAIN_CLASS "$@"

