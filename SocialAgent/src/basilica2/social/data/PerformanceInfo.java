package basilica2.social.data;

public class PerformanceInfo {

    private String behaviorPerformed;
    private String triggeredBy;
    private double confidence;

    public PerformanceInfo(String b, String f, double c) {
        behaviorPerformed = b;
        triggeredBy = f;
        confidence = c;
    }

    @Override
    public String toString() {
        return "<Perform behavior=\"" + behaviorPerformed + "\" trigger=\"" + triggeredBy + "\" confidence=\"" + confidence + "\" />";
    }
}
