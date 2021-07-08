#!/bin/bash
cd processes
chmod 777 /mysql-files
mysql -u root -psmoot -h nodechat  -P 3306 <<MYSQL_QUERY
USE nodechat
SELECT 'type', 'username', 'useraddress', 'userid', 'timestamp', 'roomname' , 'content' UNION ALL SELECT type,username,useraddress,userid,timestamp,name as roomname,replace(content,'"',"'") as content FROM message JOIN room ON  room.id = message.roomid INTO OUTFILE '/mysql-files/bazaar_2021-07-08a.csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
MYSQL_QUERY
python3 chat_logs.py /mysql-files/bazaar_2021-07-08a.csv jeopardy 06/04/21 00 07/08/21 23 >> python_output.txt
