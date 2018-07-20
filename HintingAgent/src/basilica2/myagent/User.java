package basilica2.myagent;

import java.sql.Timestamp;

public class User {
	
    public String name;
    public String id;
	public Timestamp time_of_entry;
	public int score;
    
    public User(String user_name, String user_id, Timestamp timestamp) {
    	name = user_name;
    	id = user_id;
    	time_of_entry = timestamp;
    	score = 1;
    }
    
}
