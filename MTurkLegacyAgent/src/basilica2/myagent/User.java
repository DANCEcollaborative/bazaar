package basilica2.myagent;

import java.sql.Timestamp;
import java.util.HashMap;

public class User {
	
    public String name;
    public String id;
	public Timestamp time_of_entry;
	public int plan;
	public int score;
	public boolean[] reasoning_flag;
//	public boolean[] user_flag;
	public boolean reasoning;
	
	public boolean promptflag;
	public String reasoning_type;
	public boolean choice_flag;
	public int wait_duration;
	public int perspective;
	public HashMap<String, Integer> user_flag;
    
    public User(String user_name, String user_id, Timestamp timestamp, int user_perspective) {
    	name = user_name;
    	id = user_id;
    	time_of_entry = timestamp;
    	score = 1;
    	reasoning_flag = new boolean[4];
    	user_flag= new HashMap<String, Integer>();
   // 	user_flag= new boolean[4];
    	choice_flag=false;
    	reasoning_type = "";
    	reasoning = false;
    	promptflag=false;
    	wait_duration = 10;
    	perspective = user_perspective;
    }
    
}
