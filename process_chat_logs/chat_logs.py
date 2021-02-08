# Creates a set of chat logs with name format 'ROOM_NAME_PREFX_USER-ID1_USER-ID2_.._USER_IDn.csv'
# -- Usage: python chat_logs.py <Bazaar_chat_log_file> <room_name_prefix> <target_start_mm/dd/yy> <target_start_hour> <target_end_mm/dd/yy> <target_end_hour>")
#      -- E.g., python chat_logs.py chat_logs_2021-01-10.csv PhysicsModule1 01/08/21 18 01/08/21 22
# -- All USER-ID<n> are users that are neither the agent nor any other user in 'users_to_exclude' -- these are generally testers rather than students
# -- No chat log is created if there are no users for the room that are not in 'users_to_exclude'.
# -- No chat log is created for entries whose date-time is not between the start date-time and the end date-time.
# -- <target_start_hour> and <target_end_hour> are in local 24-hour time. Adjust constant 'UTC_offset' as necessary for the local time zone.
#      -- For start hour SS and end hour EE, valid times are between SS:00 and EE:59 (between the start of start hour and the end of end hour).
# -- <Bazaar_chat_log_file> must be a .csv file. It is assumed that it is created with a query like the following from a MySQL database:
#       use nodechat;
#       SELECT 'type', 'username', 'useraddress', 'userid', 'timestamp', 'roomname' , 'content' UNION ALL SELECT type,username,useraddress,userid,timestamp,name as roomname,replace(content,'"',"'") as content FROM message JOIN room ON  room.id = message.roomid INTO OUTFILE '/bazaar/misc/chat_logs.csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"';

import sys
import csv
import datetime
from datetime import timedelta
import operator
import os

UTC_offset = 5
row_list = []
user_list = []
user_count = 0;
room_name_prefix = ""

# users_to_exclude: Don't include chat log for room if the room's only users are the agent itself (e.g., "Dr___") or (e.g.) users who are testers
users_to_exclude = ["rcmurray","csealfon","cprose","rgachuhi","DrEvergreen","DrSpruce","DrDogwood","DrSassafras","DrPawPaw","DrYew","DrML"]

def create_filename (prefix, suffix):
    # suffix includes the period, if any -- e.g., '.csv
    filename = prefix + suffix
    if os.path.isfile(filename):
        extra = 1
        while True:
            extra += 1
            new_filename = filename.split(suffix)[0] + "-" + str(extra) + ".csv"
            if os.path.isfile(new_filename):
                continue
            else:
                filename = new_filename
                break
    return filename



# Process one chat_log room. Called with a valid start_index.
def process_room (chat_list, start_index):
    global user_count
    user_list.clear()
    room_name = chat_list[start_index][5]
    next_room_name = room_name
    index = start_index;
    
    # First pass thru room: get any users that aren't in users_to_exclude
    while next_room_name == room_name:
        username = chat_list[index][1]        # username
        if username not in users_to_exclude and username not in user_list:
            user_list.append(username)
        index += 1;
        if index < len(chat_list):
            next_room_name = chat_list[index][5]
        else:
            next_room_name = ""
        end_index = index;
        # At this point, end_index is either
        #    -- past the end of the overall chat_list
        #    -- at the first index for a new room


    # If room has any non-excluded users, write out the chat log entries to a file named by room_name_prefix plus all non-excluded user IDs
    if len(user_list) > 0:
        user_count += len(user_list)
        file_prefix = room_name_prefix
        for i in range(len(user_list)):
            file_prefix += "_"
            file_prefix += user_list[i]
        filename = create_filename(file_prefix,".csv")
        out_file = open(filename, 'w')
        writer = csv.writer(out_file)
        row_list=["timestamp","username","type","content"]
        writer.writerow(row_list)

        # Write a chat log row for each entry in room
        for i in range(start_index,end_index):           
            # Adjust UTC time to local time based on UTC offset
            chat_time = datetime.datetime.strptime(chat_list[i][4], '%Y-%m-%d %H:%M:%S')   # timestamp in UTC
            chat_time = chat_time - timedelta(hours=UTC_offset)
            row_list[0] = chat_time
            row_list[1] = chat_list[i][1]     # username
            row_list[2] = chat_list[i][0]     # type
            row_list[3] = chat_list[i][6]     # content
            writer.writerow(row_list)

        out_file.close()
    return end_index;   # return the last index for the room plus 1


try:
    csvfile = open(sys.argv[1],'r')
    room_name_prefix = sys.argv[2]

    #################################################################################
    # Process min & max date(s)-time(s) to search for ###############################
    target_start_date = datetime.datetime.strptime(sys.argv[3], '%m/%d/%y')
    target_start_year = int(target_start_date.year)
    target_start_month = int(target_start_date.month)
    target_start_day = int(target_start_date.day)
    target_start_hour = int(sys.argv[4])
    target_start_UTC_hour = target_start_hour + UTC_offset
    if target_start_UTC_hour > 23:
        target_start_UTC_hour = target_start_UTC_hour - 24
        target_start_UTC_day = target_start_day + 1
    else:
        target_start_UTC_day = target_start_day

    target_end_date = datetime.datetime.strptime(sys.argv[5], '%m/%d/%y')
    target_end_year = int(target_end_date.year)
    target_end_month = int(target_end_date.month)
    target_end_day = int(target_end_date.day)
    target_end_hour = int(sys.argv[6])

    target_end_UTC_hour = target_end_hour + UTC_offset
    if target_end_UTC_hour > 23:
        target_end_UTC_hour = target_end_UTC_hour - 24
        target_end_UTC_day = target_end_day + 1
    else:
        target_end_UTC_day = target_end_day

    target_start_time = datetime.datetime(target_start_year,target_start_month,target_start_UTC_day,target_start_UTC_hour,0)
    target_end_time = datetime.datetime(target_end_year,target_end_month,target_end_UTC_day,target_end_UTC_hour,59,59)
    print(target_start_time)
    print(target_end_time)
    #################################################################################

    reader = csv.reader(csvfile)

    # Select only chat log entries with the sought room_name_prefix between the specified start & end times
    filtered = []
    for row in reader:
        if room_name_prefix in row[5]:
            chat_datetime = datetime.datetime.strptime(row[4], '%Y-%m-%d %H:%M:%S')
            if target_start_time <= chat_datetime <= target_end_time:
                filtered.append(row)
    csvfile.close()

    # Sort the chat log entries by (1) room_name, then (2) time
    sort = sorted(filtered,key=operator.itemgetter(5,4))

    # Process each room in the filtered, sorted chat log entries
    next_index = 0
    while next_index < len(sort):
        next_index = process_room(sort,next_index)

    print(str(user_count) + " users")

except NameError:
    print("Usage: python chat_logs.py <Bazaar_chat_log_file> <room_name_prefix> <target_start_mm/dd/yy> <target_start_hour> <target_end_mm/dd/yy> <target_end_hour>")
