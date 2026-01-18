package compression.arithmaticCoding.core;

import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

public interface ArithmeticCoder {

    /**
     * Encode exactly one grammar rule using the given probability model.
     * The arithmetic coder is responsible for:
     *  - querying probabilities from the model
     *  - updating its internal coding range
     */
    void encodeRule(Rule rule, RuleProbModel model);

    /**
     * Finish encoding and flush remaining bits/state.
     */
    void finish();
}

