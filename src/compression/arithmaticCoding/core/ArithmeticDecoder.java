package compression.arithmaticCoding.core;

import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

public interface ArithmeticDecoder {

    /**
     * Decode the next rule for the given non-terminal
     * using the same probability model as the encoder.
     */
    Rule decodeRule(NonTerminal lhs, RuleProbModel model);
}