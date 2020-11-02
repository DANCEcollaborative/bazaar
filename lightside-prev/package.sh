ant build
lsv=LightSide-`git describe --tags`.zip
cd ..
zip -r $lsv lightside/copyright lightside/*md lightside/*app lightside/*bat
zip -ur $lsv lightside/scripts lightside/run.sh lightside/plugins lightside/wekafiles
zip -ur $lsv lightside/toolkits lightside/lib lightside/bin lightside/junit lightside/testData
zip -ur $lsv lightside/src lightside/build.xml
zip -d $lsv  lightside/toolkits/segmentation/ctb.gz
zip -d $lsv  lightside/lib/stanford-parser-*models.jar
zip -d $lsv  lightside/toolkits/segmentation/pku.gz
zip -d $lsv  "lightside/toolkits/segmentation/dict-chris6*"

cd lightside
ant build-intl
lsv=LightSide-`git describe --tags`+zh.zip
cd ..
zip -r $lsv lightside/copyright lightside/*md lightside/*app lightside/*bat
zip -ur $lsv lightside/scripts lightside/run.sh lightside/plugins lightside/wekafiles
zip -ur $lsv lightside/toolkits lightside/lib lightside/bin lightside/junit lightside/testData
zip -ur $lsv lightside/src lightside/build.xml
zip -d $lsv  lightside/lib/stanford-parser-*models.jar
cd lightside





