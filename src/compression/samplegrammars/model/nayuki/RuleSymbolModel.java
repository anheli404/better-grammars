package compression.samplegrammars.model.nayuki;


import compression.grammar.Category;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;

import java.util.List;

/**
 * Interface that provides mapping between grammar rules and integer "symbols"
 * used by the Nayuki arithmetic coder.
 */
public interface RuleSymbolModel {
    /**
     * Returns integer symbol corresponding to the given rule.
     * @param rule the grammar rule to encode
     * @return the integer symbol representing this rule
     */
    int getSymbolFor(final Rule rule);

    /**
     * Returns the frequency array for all rules with the given Non-Terminal
     * on the left-hand side.
     * @param lhs the non-terminal whose rule frequencies are requested.
     * @return frequencies array for this Non-Terminal
     */
    int[] getFrequenciesFor(final NonTerminal lhs);

    /**
     * Returns the right-hand side of the rule corresponding to
     * the given symbol (each rhs is a 'Category' object).
     * @param symbol the symbol read from the arithmetic decoder
     * @param lhs the non-terminal currently being expanded
     * @return the list of categories forming the rule's right-hand side.
     */
    List<Category> getRhsFor(int symbol, NonTerminal lhs);

    /**
     * Updates the model after encoding a rule.
     * Used by adaptive rule-symbol models to modify their frequency counts
     * dynamically based on observed rules.
     * @param rule the rule that was just encoded.
     */
    void updateOnEncode(Rule rule);

}
