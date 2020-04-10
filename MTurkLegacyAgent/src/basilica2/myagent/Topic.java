package basilica2.myagent;

import java.sql.Timestamp;

public class Topic {
	
    public String name;
    public String detailed_name;
	public Timestamp topic_detected;
    public Timestamp topic_requested;
    public Timestamp topic_prompted;
    public Timestamp topic_discussed;
    
    public Topic(String topicName, String topicDetailedName) {
    	name = topicName;
    	detailed_name = topicDetailedName;
        topic_detected = null;
        topic_requested = null;
        topic_prompted = null;
        topic_discussed = null;
    }
}