package compression.samplegrammars.model.nayuki;


import compression.grammar.Category;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;

import java.util.List;

public interface RuleSymbolModel {
    int getSymbolFor(final Rule rule);

    int[] getFrequenciesFor(final NonTerminal lhs);

    List<Category> getRhsFor(int symbol, NonTerminal lhs);

    void updateOnEncode(Rule rule);

}
