#!/bin/bash

# Usage: scripts/train.sh {full|predict} {data-encoding} saved/template.model.xml saved/new.model.xml data.csv...
# Follows a trained model template on a new data set.
# Model can be saved in full (for error analysis), or in a prediction-only format.
# Common data encodings are UTF-8, windows-1252, and MacRoman.
# (Make sure that the text columns, class column, and any columns used as features 
#  have the same names in the new data as they did for the template.)

MAXHEAP="4g"
OS_ARGS=""
#OTHER_ARGS=""
OTHER_ARGS="-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -Djava.awt.headless=true"

MAIN_CLASS="edu.cmu.side.recipe.Chef"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-java-1.96-SNAPSHOT.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
        
java $OS_ARGS -Xmx$MAXHEAP $OTHER_ARGS -classpath $CLASSPATH $MAIN_CLASS "$@"

