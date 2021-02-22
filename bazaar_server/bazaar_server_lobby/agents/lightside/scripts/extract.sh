#!/bin/bash

# Usage: scripts/extract.sh {arff|csv|xml} {data-encoding} path/to/template.xml path/to/output/table path/to/data.csv
# Extracts a new feature table with the same extraction settings as template.xml 
# (any saved LightSide feature table or model)
# Feature table can be saved in ARFF, CSV, or LightSide XML formats.
# Common data encodings are UTF-8, windows-1252, and MacRoman.
# (Make sure that the text columns and any columns used as features 
#  have the same names in the new data as they did in the template.)

MAXHEAP="4g"
OS_ARGS=""
#OTHER_ARGS=""
OTHER_ARGS="-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -Djava.awt.headless=true"

MAIN_CLASS="edu.cmu.side.recipe.Extractor"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-java-1.96-SNAPSHOT.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
        
java $OS_ARGS -Xmx$MAXHEAP $OTHER_ARGS -classpath $CLASSPATH $MAIN_CLASS "$@"

