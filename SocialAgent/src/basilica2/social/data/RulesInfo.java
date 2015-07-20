package basilica2.social.data;

public class RulesInfo {

    private String behavior;

    public RulesInfo(String b) {
        behavior = b;
    }

    @Override
    public String toString() {
        return "<RulesInfo behavior=\"" + behavior + "\"/>";
    }
}
