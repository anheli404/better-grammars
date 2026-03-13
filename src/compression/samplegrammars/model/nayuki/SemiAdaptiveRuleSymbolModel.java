package compression.samplegrammars.model.nayuki;

import compression.grammar.*;
import compression.samplegrammars.LeftmostDerivation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemiAdaptiveRuleSymbolModel extends StaticRuleSymbolModel {

    public SemiAdaptiveRuleSymbolModel(RNAGrammar grammar, RNAWithStructure rna) {
        super(grammar, obtainRuleCounts(grammar, rna));
    }

    public static Map<Rule, Long> obtainRuleCounts(RNAGrammar grammar, RNAWithStructure rna) {
        Map<Rule, Long> rulesToFrequency = new HashMap<>();
        grammar.getAllRules().forEach(r -> rulesToFrequency.put(r, 0L));
        List<Rule> rules = LeftmostDerivation.rules(grammar, rna);
        rules.forEach((rule) -> rulesToFrequency.replace(rule, rulesToFrequency.get(rule) + 1));
        return rulesToFrequency;
    }

    public void updateOnEncode(Rule rule) {
        throw new UnsupportedOperationException("Semi-adaptive model does not update shit!!!!");
    }

}