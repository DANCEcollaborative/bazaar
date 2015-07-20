/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.social.data;

import basilica2.util.Timer;

/**
 *
 * @author rohitk
 */
public class TriggerInfo {

    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_STATE_INFO = "STATE_INFO";
    public static final String TYPE_MODEL_INFO = "MODEL_INFO";
    public static final String TYPE_RANDOM_INFO = "RANDOM_INFO";
    public static final String TYPE_RULES_INFO = "RULES_INFO";
    public static final String TYPE_STRATEGY_SCORES = "STRATEGY_SCORES";
    public static final String TYPE_PERFORMANCE_INFO = "PERFORMANCE_INFO";
    public static final String TRIGGER_RANDOM = "RANDOM";
    public static final String TRIGGER_MODEL = "MODEL";
    public static final String TRIGGER_RULES = "RULES";
    private long timestamp;
    private String type;
    private Message message;
    private ModelInfo modelInfo;
    private StateInfo stateInfo;
    private StrategyScores strategyScores;
    private RulesInfo rulesInfo;
    private PerformanceInfo performanceInfo;

    public TriggerInfo(Message message) {
        timestamp = Timer.currentTimeMillis();
        type = TYPE_MESSAGE;
        this.message = message;
    }

    public TriggerInfo(ModelInfo modelInfo) {
        timestamp = Timer.currentTimeMillis();
        type = modelInfo.getType();
        this.modelInfo = modelInfo;
    }

    public TriggerInfo(StateInfo stateInfo) {
        timestamp = Timer.currentTimeMillis();
        type = TYPE_STATE_INFO;
        this.stateInfo = stateInfo;
    }

    public TriggerInfo(StrategyScores strategyScores) {
        timestamp = Timer.currentTimeMillis();
        type = TYPE_STRATEGY_SCORES;
        this.strategyScores = strategyScores;
    }

    public TriggerInfo(RulesInfo rulesInfo) {
        timestamp = Timer.currentTimeMillis();
        type = TYPE_RULES_INFO;
        this.rulesInfo = rulesInfo;
    }

    public TriggerInfo(PerformanceInfo performanceInfo) {
        timestamp = Timer.currentTimeMillis();
        type = TYPE_PERFORMANCE_INFO;
        this.performanceInfo = performanceInfo;
    }

    @Override
    public String toString() {
        String ret = "<TriggerInfo type=\"" + type + "\" time=\"" + timestamp + "\">\n";
        if (type.equals(TYPE_MESSAGE)) {
            ret += message.toString();
        } else if (type.equals(TYPE_MODEL_INFO)) {
            ret += modelInfo.toString();
        } else if (type.equals(TYPE_RANDOM_INFO)) {
            ret += modelInfo.toString();
        } else if (type.equals(TYPE_STRATEGY_SCORES)) {
            ret += strategyScores.toString();
        } else if (type.equals(TYPE_STATE_INFO)) {
            ret += stateInfo.toString();
        } else if (type.equals(TYPE_RULES_INFO)) {
            ret += rulesInfo.toString();
        } else if (type.equals(TYPE_PERFORMANCE_INFO)) {
            ret += performanceInfo.toString();
        }
        ret += "\n</TriggerInfo>\n";
        return ret;
    }
}
