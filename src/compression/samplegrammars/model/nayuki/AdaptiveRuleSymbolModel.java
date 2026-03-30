package compression.samplegrammars.model.nayuki;

import compression.grammar.Category;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;

import java.util.*;

/**
 * Implementation of RuleSymbolModel interface, that represents the
 * adaptive arithmetic coding model that uses the Nayuki library.
 * Unlike StaticRuleSymbolModel, this model does not use fixed probabilities,
 * but rather it dynamically updates symbol frequencies during encoding and decoding
 * based on the rules that occur.
 */
public class AdaptiveRuleSymbolModel implements RuleSymbolModel {
    // Grammar used for encoding/decoding. Its production rules define symbols.
    private final Grammar<?> G;
    // Map structure: NT -> (rhs -> symbol). Allows us to access
    // symbol of rule, given only rule. Mainly used in encoding, where rules get
    // converted to symbols (that are expected by encoder).
    private final Map<NonTerminal, Map<List<Category>, Integer>> ruleToSymbol = new HashMap<>();

    // Map structure: NT -> list of rhs's. Allows to access rule, given only symbol.
    // Reverse mapping of ruleToSymbol. Mainly used in decoding, where decoder outputs
    // a symbol, and corresponding grammar rule must be reconstructed.
    private final Map<NonTerminal, List<List<Category>>> symbolToRule = new HashMap<>();

    // Map structure: NT -> frequencies array
    // The frequencies array is updated dynamically whenever a rule is used.
    private final Map<NonTerminal, int[]> lhsToFreqs = new HashMap<>();

    public AdaptiveRuleSymbolModel(Grammar<?> grammar) {
        this.G = grammar;
        initializeAllMaps();
    }

    /**
     * Builds all rule mappings and initializes frequency tables.
     * For each NT in grammar:
     * 1. Assigns symbols to rules
     * 2. Creates reverse symbol mappings
     * 3. Initializes frequency tables (with 1's initially)
     */
    private void initializeAllMaps() {
        for (NonTerminal lhs : G.getNonTerminals()) { // iterate over all NTs in grammar
            List<Rule> rules = new ArrayList<>(G.getRules(lhs)); // get all production rules for current NT

            Map<List<Category>, Integer> ruleSymbolMap = new HashMap<>(); // map from rule rhs to symbol index
            List<List<Category>> rulesList = new ArrayList<>(); // list storing rhs entries indexed by symbol
            int[] freqs = new int[rules.size()]; // frequency table for this NT

            for (int symbol = 0; symbol < rules.size(); symbol++) { // assign symbol to each rule
                Rule rule = rules.get(symbol); // get current rule
                List<Category> rhs = Arrays.asList(rule.getRight()); // convert rule rhs to list
                ruleSymbolMap.put(rhs, symbol); //store mapping: rhs -> symbol
                rulesList.add(rhs); // store reverse mapping: symbol -> rhs
                freqs[symbol] = 1; // initialize frequency to 1
            }

            ruleToSymbol.put(lhs, ruleSymbolMap); // save rule-to-symbol mapping for this NT
            symbolToRule.put(lhs, rulesList); // save symbol-to-rule mapping for this NT
            lhsToFreqs.put(lhs, freqs); // save frequency table for this NT
        }
    }

    /**
     * Looks up integer symbol assigned to a rule based on its lhs and rhs.
     * That symbol is then passed to the arithmetic encoder.
     * Used by encoder
     * @param rule the grammar rule to encode
     * @return symbol integer corresponding to given grammar rule.
     */
    @Override
    public int getSymbolFor(Rule rule) {
        NonTerminal lhs = rule.getLeft();
        Map<List<Category>, Integer> lhsMap = ruleToSymbol.get(lhs);
        if (lhsMap == null)
            throw new IllegalArgumentException("Unknown lhs: " + lhs);

        Integer symbol = lhsMap.get(Arrays.asList(rule.getRight()));
        if (symbol == null)
            throw new IllegalArgumentException("Rule not found: " + rule);

        return symbol;
    }

    /**
     * Returns frequency table for specific NT which is later given to arithmetic
     * encoder.
     * @param lhs the non-terminal whose rule frequencies are requested.
     * @return frequency table for NT
     */
    @Override
    public int[] getFrequenciesFor(NonTerminal lhs) {
        int[] freqs = lhsToFreqs.get(lhs);
        if (freqs == null)
            throw new IllegalArgumentException("No frequencies for lhs: " + lhs);
        return freqs.clone();
    }

    /**
     * Performs reverse lookup from symbol to rule.
     * After retrieving the rule, it updates the frequency table to reflect
     * that the rule has been used.
     * @param symbol the symbol read from the arithmetic decoder
     * @param lhs the non-terminal currently being expanded
     * @return rhs of the rule associated with given symbol.
     */
    @Override
    public List<Category> getRhsFor(int symbol, NonTerminal lhs) {
        List<List<Category>> rules = symbolToRule.get(lhs);
        if (rules == null)
            throw new IllegalArgumentException("Unknown NT: " + lhs);
        if (symbol < 0 || symbol >= rules.size())
            throw new IllegalArgumentException("Invalid symbol: " + symbol);

        List<Category> rhs = rules.get(symbol);
        increment(lhs, symbol);   // decoder updates after identifying rule
        return rhs;
    }

    /**
     * Increases the frequency of a rule for the specified symbol.
     * @param lhs for whose freq table the update has to be performed
     * @param symbol that points to exact value in freq table that we want
     *               to increment.
     */
    private void increment(NonTerminal lhs, int symbol) {
        int[] freqs = lhsToFreqs.get(lhs);
        if (freqs == null)
            throw new IllegalArgumentException("Unknown lhs: " + lhs);
        if (symbol < 0 || symbol >= freqs.length)
            throw new IllegalArgumentException("Symbol out of range: " + symbol);
        if (freqs[symbol] == Integer.MAX_VALUE)
            throw new ArithmeticException("Frequency overflow");
        freqs[symbol]++;
    }

    /**
     * Updates frequency table of a given rule after it was encoded.
     * Unlike decoding, where the update happens automatically, encoding requires
     * an explicit update call after a rule has been encoded.
     * @param rule the rule that was just encoded.
     */
    public void updateOnEncode(Rule rule) {
        NonTerminal lhs = rule.getLeft();
        Integer symbol = ruleToSymbol.get(lhs).get(Arrays.asList(rule.getRight()));
        increment(lhs, symbol);
    }

}

