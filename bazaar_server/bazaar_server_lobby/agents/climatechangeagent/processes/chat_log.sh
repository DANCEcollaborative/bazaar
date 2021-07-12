#!/bin/bash
ROOM_PREFIX=$1
cd processes
chmod 777 /mysql-files
mysql -u root -psmoot -h nodechat  -P 3306 <<MYSQL_QUERY
USE nodechat
SELECT 'type', 'username', 'useraddress', 'userid', 'timestamp', 'roomname', 'content' UNION ALL SELECT type,username,useraddress,userid,timestamp,'$ROOM_PREFIX' AS roomname,REPLACE(content,'"',"'") AS content FROM message JOIN (SELECT id FROM room WHERE name = '$ROOM_PREFIX') subquery ON subquery.id = message.roomid INTO OUTFILE '/mysql-files/$ROOM_PREFIX.csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
MYSQL_QUERY
python3 chat_logs.py /mysql-files/$ROOM_PREFIX.csv $ROOM_PREFIX
rm /mysql-files/$ROOM_PREFIX.csv
mv *.csv ../chat_logs
