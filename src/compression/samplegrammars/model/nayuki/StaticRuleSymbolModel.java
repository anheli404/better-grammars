package compression.samplegrammars.model.nayuki;

import compression.grammar.Category;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;

import java.util.*;

public class StaticRuleSymbolModel implements RuleSymbolModel {

    private final Map<NonTerminal, Map<List<Category>, Integer>> ruleToSymbol = new HashMap<>();
    private final Map<NonTerminal, List<List<Category>>> symbolToRule = new HashMap<>();
    private final Map<NonTerminal, int[]> lhsToFreqs = new HashMap<>();

    public StaticRuleSymbolModel(Grammar<?> grammar, Map<Rule, Long> ruleCounts) {
        initializeAllMaps(grammar, ruleCounts);
    }

    public void initializeAllMaps(Grammar<?> grammar, Map<Rule,Long> ruleCounts) {
        // for each NT in given grammar
        for (NonTerminal lhs : grammar.getNonTerminals()) {
            // get all rules for this NT
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            // create map: key = rhs/rule, value = symbol
            Map<List<Category>, Integer> ruleSymbolMap = new HashMap<>();
            // create list with rules, where index of each rule is its symbol
            List<List<Category>> rulesList = new ArrayList<>();
            // create array with frequencies for this NT (for future FrequencyTable)
            int[] freqs = new int[rules.size()];

            // total number of times that current NT was seen in dataset
            long total = 0L;
            for (Rule rule : rules) {
                // number of times this rule was seen in dataset
                Long count = ruleCounts.get(rule);
                if (count != null) {
                    total = total + count;
                }
            }
            // for each rule (out of rules for this NT)
            for (int symbol = 0; symbol < rules.size(); symbol++) {
                Rule rule = rules.get(symbol);
                List<Category> rhs = Arrays.asList(rule.getRight());
                // put rule's rhs into map as key with symbol as value
                ruleSymbolMap.put(rhs, symbol);
                // add rule to list at symbol position
                rulesList.add(rhs);
                // get this rule's count from the list of counts
                long count = ruleCounts.getOrDefault(rule, 0L);
                // if this NT has not been seen, or this rule has not been seen in dataset
                if (total == 0L || count == 0L) {
                    // then default frequency 1
                    freqs[symbol] = 1;
                } else {
                    // else frequency is the rule count from ruleCounts map passed to static model
                    freqs[symbol] = safeLongToInt(count);
                }
            }
            // Map(key=NT, value=Map(key=rule,value=symbol))
            ruleToSymbol.put(lhs, ruleSymbolMap);
            // Map(key=NT, value=List(rules))
            symbolToRule.put(lhs, rulesList);
            // Map(key=NT, value= rule frequencies array)
            lhsToFreqs.put(lhs, freqs);
        }
    }

    @Override
    public int getSymbolFor(Rule rule) {
        // NT of the rule
        NonTerminal lhs = rule.getLeft();
        // Get the rule -> symbol Map that corresponds to this NT
        Map<List<Category>, Integer> lhsMap = ruleToSymbol.get(lhs);
        // No such NT in grammar
        if(lhsMap == null) {
            throw new IllegalArgumentException("Unknown lhs: " + rule.getLeft());
        }
        // rhs of the rule
        List<Category> rhs = Arrays.asList(rule.getRight());
        // get the symbol corresponding to this rhs from the rule -> symbol Map.
        Integer symbol = lhsMap.get(rhs);
        // check that this rule exists in grammar
        if (symbol == null) {
            throw new IllegalArgumentException("Rule not found: " + rule);
        }
        return symbol;
    }

    @Override
    public int[] getFrequenciesFor(NonTerminal lhs) {
        // Get frequencies corresponding to given NT
        int[] freqs = lhsToFreqs.get(lhs);
        // Check that given NT actually exists in grammar
        if (freqs == null) {
            throw new IllegalArgumentException("No frequencies for lhs: " + lhs);
        }
        // Return copy to prevent exposing original mutable array to client code
        return freqs.clone();
    }

    @Override
    public List<Category> getRhsFor(int symbol, NonTerminal lhs) {
        // Get the list of rules corresponding to given NT
        List<List<Category>> rules = symbolToRule.get(lhs);
        // Check that the passed NT actually exists
        if (rules == null) {
            throw new IllegalArgumentException("Unknown NT: " + lhs);
        }
        // Check that symbol is positive int
        if (symbol < 0 || symbol >= rules.size()) {
            throw new IllegalArgumentException("Invalid symbol passed: " + symbol);
        }
        // Return the rule at given symbol index
        return rules.get(symbol);
    }

    public static int safeLongToInt(long value) {
        if (value <= 0L) {
            throw new IllegalArgumentException("Frequency must be positive");
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    public void updateOnEncode(Rule rule) {
        throw new UnsupportedOperationException("Static model does not update shit!!!!");
    }

}
