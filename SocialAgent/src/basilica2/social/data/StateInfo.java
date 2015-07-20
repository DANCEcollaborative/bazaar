package basilica2.social.data;

public class StateInfo {

    private String plannerState;

    public StateInfo(String s) {
        plannerState = s;
    }

    @Override
    public String toString() {
        return "<State name=\"" + plannerState + "\" />";
    }
}
