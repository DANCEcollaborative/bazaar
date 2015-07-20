package basilica2.social.data;

public class StrategyScores {

    private double scoreStrategy2a = 0.0;
    private double scoreStrategy2b = 0.0;
    private double scoreStrategy2c = 0.0;
    private double scoreStrategy2d = 0.0;
    private double scoreStrategy2e = 0.0;
    private double scoreStrategy2f = 0.0;
    private double scoreStrategy2g = 0.0;
    private double scoreStrategy3a = 0.0;
    private double scoreStrategy3cd = 0.0;
    private double scoreStrategy4a = 0.0;
    private double scoreStrategy4b = 0.0;
    private double scoreStrategy6 = 0.0;
    private double scoreStrategy7 = 0.0;
    private boolean flagStateChanged = false;
    private boolean flagDormantStudent = false;
    private boolean flagDormantGroup = false;
    private boolean flagNewMessage = false;

    public StrategyScores() {
    }

    public StrategyScores(double s2a, double s2b, double s2c, double s2d, double s2e, double s2f, double s2g, double s3a, double s3cd, double s4a, double s4b, double s6, double s7, boolean fSC, boolean fDS, boolean fDG, boolean fNM) {
        scoreStrategy2a = s2a;
        scoreStrategy2b = s2b;
        scoreStrategy2c = s2c;
        scoreStrategy2d = s2d;
        scoreStrategy2e = s2e;
        scoreStrategy2f = s2f;
        scoreStrategy2g = s2g;
        scoreStrategy3a = s3a;
        scoreStrategy3cd = s3cd;
        scoreStrategy4a = s4a;
        scoreStrategy4b = s4b;
        scoreStrategy6 = s6;
        scoreStrategy7 = s7;
        flagStateChanged = fSC;
        flagDormantStudent = fDS;
        flagDormantGroup = fDG;
        flagNewMessage = fNM;
    }
        
    public void reset(boolean usedStateChanged, boolean usedDormantGroup, boolean usedDormantStudent, boolean usedNewMessage) 
    {
         scoreStrategy2a = (usedNewMessage) ? 0.0 : this.get2aScore();
         scoreStrategy2b = (usedNewMessage) ? 0.0 : this.get2bScore();
         scoreStrategy2c = (usedStateChanged) ? 0.0 : this.get2cScore();
         scoreStrategy2d = (usedDormantStudent) ? 0.0 : this.get2dScore();
         scoreStrategy2e = (usedDormantGroup) ? 0.0 : this.get2eScore();
         scoreStrategy2f = (usedNewMessage) ? 0.0 : this.get2fScore();
         scoreStrategy2g = (usedNewMessage) ? 0.0 : this.get2gScore();
         scoreStrategy3a = (usedStateChanged) ? 0.0 : this.get3aScore();
         scoreStrategy3cd = (usedStateChanged) ? 0.0 : this.get3cdScore();
         scoreStrategy4a = (usedNewMessage) ? 0.0 : this.get4aScore();
         scoreStrategy4b = (usedNewMessage) ? 0.0 : this.get4bScore();
         scoreStrategy6 = (usedNewMessage) ? 0.0 : this.get6Score();
         scoreStrategy7 = (usedNewMessage) ? 0.0 : this.get7Score();
         flagStateChanged = (usedStateChanged) ? false : this.hasStateChanged();
         flagDormantGroup = (usedDormantGroup) ? false : this.isGroupDormant();
         flagDormantStudent = (usedDormantStudent) ? false : this.isStudentDormant();
         flagNewMessage = (usedNewMessage) ? false : this.isNewMessage();
    }

    public boolean scoresChanged(StrategyScores ss) {
        boolean ret = false;
        if (this.scoreStrategy2a != ss.scoreStrategy2a) {
            return true;
        } else if (this.scoreStrategy2b != ss.scoreStrategy2b) {
            return true;
        } else if (this.scoreStrategy2c != ss.scoreStrategy2c) {
            return true;
        } else if (this.scoreStrategy2d != ss.scoreStrategy2d) {
            return true;
        } else if (this.scoreStrategy2e != ss.scoreStrategy2e) {
            return true;
        } else if (this.scoreStrategy2f != ss.scoreStrategy2f) {
            return true;
        } else if (this.scoreStrategy2g != ss.scoreStrategy2g) {
            return true;
        } else if (this.scoreStrategy3a != ss.scoreStrategy3a) {
            return true;
        } else if (this.scoreStrategy3cd != ss.scoreStrategy3cd) {
            return true;
        } else if (this.scoreStrategy4a != ss.scoreStrategy4a) {
            return true;
        } else if (this.scoreStrategy4b != ss.scoreStrategy4b) {
            return true;
        } else if (this.scoreStrategy6 != ss.scoreStrategy6) {
            return true;
        } else if (this.scoreStrategy7 != ss.scoreStrategy7) {
            return true;
        }
        return ret;
    }

    public double get2aScore() {
        return scoreStrategy2a;
    }

    public double get2bScore() {
        return scoreStrategy2b;
    }

    public double get2cScore() {
        return scoreStrategy2c;
    }

    public double get2dScore() {
        return scoreStrategy2d;
    }

    public double get2eScore() {
        return scoreStrategy2e;
    }

    public double get2fScore() {
        return scoreStrategy2f;
    }

    public double get2gScore() {
        return scoreStrategy2g;
    }

    public double get3aScore() {
        return scoreStrategy3a;
    }

    public double get3cdScore() {
        return scoreStrategy3cd;
    }

    public double get4aScore() {
        return scoreStrategy4a;
    }

    public double get4bScore() {
        return scoreStrategy4b;
    }

    public double get6Score() {
        return scoreStrategy6;
    }

    public double get7Score() {
        return scoreStrategy7;
    }

    public boolean hasStateChanged() {
        return flagStateChanged;
    }

    public boolean isStudentDormant() {
        return flagDormantStudent;
    }

    public boolean isGroupDormant() {
        return flagDormantGroup;
    }

    public boolean isNewMessage() {
        return flagNewMessage;
    }

    @Override
    public String toString() {
        String ret = "<StrategyScore stateChanged=\"" + flagStateChanged + "\" dormantstudent=\"" + flagDormantStudent + "\" dormantgroup=\"" + flagDormantGroup + "\" newmessage=\"" + flagNewMessage + "\">";
        ret += "\t<score strategy=\"2a\" value=\"" + scoreStrategy2a + "\"/>";
        ret += "\t<score strategy=\"2b\" value=\"" + scoreStrategy2b + "\"/>";
        ret += "\t<score strategy=\"2c\" value=\"" + scoreStrategy2c + "\"/>";
        ret += "\t<score strategy=\"2d\" value=\"" + scoreStrategy2d + "\"/>";
        ret += "\t<score strategy=\"2e\" value=\"" + scoreStrategy2e + "\"/>";
        ret += "\t<score strategy=\"2f\" value=\"" + scoreStrategy2f + "\"/>";
        ret += "\t<score strategy=\"2g\" value=\"" + scoreStrategy2g + "\"/>";
        ret += "\t<score strategy=\"3a\" value=\"" + scoreStrategy3a + "\"/>";
        ret += "\t<score strategy=\"3cd\" value=\"" + scoreStrategy3cd + "\"/>";
        ret += "\t<score strategy=\"4a\" value=\"" + scoreStrategy4a + "\"/>";
        ret += "\t<score strategy=\"4b\" value=\"" + scoreStrategy4b + "\"/>";
        ret += "\t<score strategy=\"6\" value=\"" + scoreStrategy6 + "\"/>";
        ret += "\t<score strategy=\"7\" value=\"" + scoreStrategy7 + "\"/>";
        ret += "</StrategyScore>";
        return ret;
    }
}
