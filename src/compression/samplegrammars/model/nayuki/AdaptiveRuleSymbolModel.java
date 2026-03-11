package compression.samplegrammars.model.nayuki;

import compression.grammar.Category;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.samplegrammars.model.bigdecimal.AdaptiveRuleProbModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdaptiveRuleSymbolModel implements RuleSymbolModel {
    private final Grammar<?> grammar;
    private final Map<NonTerminal, Map<List<Category>, Long>> ruleCounts = new HashMap<>();


    public AdaptiveRuleProbModel


    @Override
    public int getSymbolFor(Rule rule) {
        return 0;
    }

    @Override
    public int[] getFrequenciesFor(NonTerminal lhs) {
        return new int[0];
    }

    @Override
    public List<Category> getRhsFor(int symbol, NonTerminal lhs) {
        return null;
    }
}
