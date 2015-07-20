package basilica2.social.data;

public class ModelInfo {

    private String type;
    private double predictedConfidence;
    private double currentSocialRatio;
    private double filterSocialRatioThreshold;
    private int numberOfRecentSocialTurns;

    public ModelInfo(String t, double c, double sr, double th, int r) {
        type = t;
        predictedConfidence = c;
        currentSocialRatio = sr;
        filterSocialRatioThreshold = th;
        numberOfRecentSocialTurns = r;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "<Model type=\"" + type + "\" confidence=\"" + predictedConfidence + "\" socialRatio=\"" + currentSocialRatio + "\" threshold=\"" + filterSocialRatioThreshold + "\" recentturns=\"" + numberOfRecentSocialTurns + "\" />";
    }
}
