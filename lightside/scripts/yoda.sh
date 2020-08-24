#!/bin/bash

#./scripts/yoda.sh FEATURE_SET  LEARNER  FEATURE_SELECTION  LABEL_COLUMN  TEXT_COLUMN  ENCODING  path/to/training/data.csv path/to/output/model.side.xml"
#Builds and evaluates a trained model");
#Examples:");
#./scripts/yoda.sh uni,bi,pos   logit_l2_c1.0  5000  score  essay_text   UTF-8  path/to/training/data.csv path/to/output/model.side.xml
#./scripts/yoda.sh tri,char     bayes          2000  label  text  windows-1252  path/to/training/data.csv path/to/output/model.side.xml

MAXHEAP="4g"
OS_ARGS=""
OTHER_ARGS="-XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -Djava.awt.headless=true"

MAIN_CLASS="edu.cmu.side.recipe.Trainer"

CLASSPATH="bin:lib/*:lib/xstream/*:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-java-1.96-SNAPSHOT.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar:plugins/genesis.jar"
        
java $OS_ARGS -Xmx$MAXHEAP $OTHER_ARGS -classpath $CLASSPATH $MAIN_CLASS "$@"

