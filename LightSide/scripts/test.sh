#!/bin/bash

# Usage: scripts/test.sh {data-encoding} saved/template.model.xml path/for/output.csv data/labeled-test-data.csv...
# Applies a trained model to a new (labeled) data set (one or more CSV files), and evaluates performance.
# A new CSV with the predictions added is saved to path/for/output.csv
# Common data encodings are UTF-8, windows-1252, and MacRoman.
# (Make sure that the text columns, class column, 
#  and any columns used as features have the same names 
#  in the new data as they did for the template.)

MAXHEAP="4g"
OS_ARGS=""
#OTHER_ARGS=""
OTHER_ARGS="-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -Djava.awt.headless=true"

MAIN_CLASS="edu.cmu.side.recipe.Tester"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-java-1.96-SNAPSHOT.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
        
java $OS_ARGS -Xmx$MAXHEAP $OTHER_ARGS -classpath $CLASSPATH $MAIN_CLASS "$@"

