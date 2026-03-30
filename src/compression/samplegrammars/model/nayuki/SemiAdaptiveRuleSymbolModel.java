package compression.samplegrammars.model.nayuki;

import compression.grammar.*;
import compression.samplegrammars.LeftmostDerivation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of RuleSymbolModel that supports semi-adaptive
 * arithmetic encoding of RNAs. It is a specialization of StaticRuleSymbolModel
 * since it computes rule frequencies once before the encoding begins, but not
 * based on a training dataset, but rather the lmd of the current RNA sequence
 * itself. THe frequencies remain fixed throughout the coding process.
 */
public class SemiAdaptiveRuleSymbolModel extends StaticRuleSymbolModel {

    public SemiAdaptiveRuleSymbolModel(RNAGrammar grammar, RNAWithStructure rna) {
        super(grammar, obtainRuleCounts(grammar, rna));
    }

    /**
     * Computes the frequency of each grammar rule in RNA structure.
     * @param grammar used for encoding/decoding.
     * @param rna sequence with its secondary structure, from which lmd the model
     *            obtains the constant rule frequencies.
     * @return Map: Rule -> # of times it occurs in lmd.
     */
    public static Map<Rule, Long> obtainRuleCounts(RNAGrammar grammar, RNAWithStructure rna) {
        Map<Rule, Long> rulesToFrequency = new HashMap<>();
        grammar.getAllRules().forEach(r -> rulesToFrequency.put(r, 0L));
        List<Rule> rules = LeftmostDerivation.rules(grammar, rna);
        rules.forEach((rule) -> rulesToFrequency.replace(rule, rulesToFrequency.get(rule) + 1));
        return rulesToFrequency;
    }

    /**
     * Unsupported method, since semi-adaptive model does not modify rule frequencies.
     * @param rule the rule that was just encoded.
     */
    public void updateOnEncode(Rule rule) {
        throw new UnsupportedOperationException("Semi-adaptive model does not update rule counts.");
    }

}