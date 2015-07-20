Agent Playback:

"clean up" a ConcertChat transcript: copy and paste the HTML table transcript into an excel spreadsheet (ensure that the header row is row #1)

Export the spreadsheet as a CSV file, and save it to the ChatterBox agent's runtime folder.

in operation.properties, specify:

#the file to play back
chatterbox.transcript=transcript.csv 

#the speed-up factor in the playback, i.e., twice as fast
chatterbox.time_scale=2.0

#how long after the agent launch the playback should start
chatterbox.start_time=10.0

#which users from the transcript to play back
#for example, omit the tutor and launch another agent 
#to see how it performs against the same input
chatterbox.users=s077,s072,s067,Alex(Tutor)  

Note - because of a static assignment in ConcertChat, for now only the last-launched agent actually receives new messages. You may need to send a launch event to each of the participant agents in order to start their playback - but don't worry, it will be synchronized once it gets started.