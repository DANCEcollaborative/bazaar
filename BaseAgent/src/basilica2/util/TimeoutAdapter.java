package basilica2.util;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;

public abstract class TimeoutAdapter implements TimeoutReceiver
{

	@Override
	public void log(String from, String level, String msg)
	{
		Logger.commonLog(from, level, msg);
	}

}
