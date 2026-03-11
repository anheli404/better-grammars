package compression.samplegrammars.model.nayuki;

import compression.grammar.Category;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;

import java.util.*;

public class AdaptiveRuleSymbolModel implements RuleSymbolModel {
    private final Grammar<?> G;
    private final Map<NonTerminal, Map<List<Category>, Integer>> ruleToSymbol = new HashMap<>();
    private final Map<NonTerminal, List<List<Category>>> symbolToRule = new HashMap<>();

    // Mutable adaptive frequencies, one array per lhs
    private final Map<NonTerminal, int[]> lhsToFreqs = new HashMap<>();
    private NonTerminal pendingLhs = null;
    private Integer pendingSymbol = null;

    public AdaptiveRuleSymbolModel(Grammar<?> grammar) {
        this.G = grammar;
        initializeAllMaps();
    }

    private void initializeAllMaps() {
        for (NonTerminal lhs : G.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(G.getRules(lhs));

            Map<List<Category>, Integer> ruleSymbolMap = new HashMap<>();
            List<List<Category>> rulesList = new ArrayList<>();
            int[] freqs = new int[rules.size()];

            for (int symbol = 0; symbol < rules.size(); symbol++) {
                Rule rule = rules.get(symbol);
                List<Category> rhs = Arrays.asList(rule.getRight());

                ruleSymbolMap.put(rhs, symbol);
                rulesList.add(rhs);

                // Adaptive model starts with frequency 1 for every rule
                freqs[symbol] = 1;
            }
            ruleToSymbol.put(lhs, ruleSymbolMap);
            symbolToRule.put(lhs, rulesList);
            lhsToFreqs.put(lhs, freqs);
        }
    }

    @Override
    public int getSymbolFor(Rule rule) {
        NonTerminal lhs = rule.getLeft();
        Map<List<Category>, Integer> lhsMap = ruleToSymbol.get(lhs);
        if (lhsMap == null) {
            throw new IllegalArgumentException("Unknown lhs: " + lhs);
        }
        List<Category> rhs = Arrays.asList(rule.getRight());
        Integer symbol = lhsMap.get(rhs);
        if (symbol == null) {
            throw new IllegalArgumentException("Rule not found: " + rule);
        }

        pendingLhs = lhs;
        pendingSymbol = symbol;

        return symbol;
    }


    @Override
    public int[] getFrequenciesFor(NonTerminal lhs) {
        int[] freqs = lhsToFreqs.get(lhs);
        if (freqs == null) {
            throw new IllegalArgumentException("No frequencies for lhs: " + lhs);
        }

        // Return snapshot of current frequencies (old model state)
        int[] snapshot = freqs.clone();

        // If this call belongs to the encoder step that just asked for a symbol,
        // apply the deferred update only AFTER returning the old frequencies.
        if (pendingLhs != null && pendingLhs.equals(lhs) && pendingSymbol != null) {
            increment(lhs, pendingSymbol);
            pendingLhs = null;
            pendingSymbol = null;
        }

        return snapshot;
    }

    @Override
    public List<Category> getRhsFor(int symbol, NonTerminal lhs) {
        List<List<Category>> rules = symbolToRule.get(lhs);
        if (rules == null) {
            throw new IllegalArgumentException("Unknown NT: " + lhs);
        }
        if (symbol < 0 || symbol >= rules.size()) {
            throw new IllegalArgumentException("Invalid symbol passed: " + symbol);
        }

        List<Category> rhs = rules.get(symbol);

        // Decoder mirrors AdaptiveRuleProbModel:
        // first determine rhs using old frequencies, then update.
        increment(lhs, symbol);

        return rhs;
    }

    private void increment(NonTerminal lhs, int symbol) {
        // Mirror BigDecimal AdaptiveRuleProbModel: do not update <start>
        if ("<start>".equals(lhs.toString())) {
            return;
        }

        int[] freqs = lhsToFreqs.get(lhs);
        if (freqs == null) {
            throw new IllegalArgumentException("Unknown lhs: " + lhs);
        }
        if (symbol < 0 || symbol >= freqs.length) {
            throw new IllegalArgumentException("Symbol out of range: " + symbol);
        }
        if (freqs[symbol] == Integer.MAX_VALUE) {
            throw new ArithmeticException("Frequency overflow for lhs " + lhs + ", symbol " + symbol);
        }

        freqs[symbol]++;
    }
}
