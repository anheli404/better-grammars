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
    private final Map<NonTerminal, int[]> lhsToFreqs = new HashMap<>();

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
        if (lhsMap == null)
            throw new IllegalArgumentException("Unknown lhs: " + lhs);

        Integer symbol = lhsMap.get(Arrays.asList(rule.getRight()));
        if (symbol == null)
            throw new IllegalArgumentException("Rule not found: " + rule);

        return symbol;
    }

    @Override
    public int[] getFrequenciesFor(NonTerminal lhs) {
        int[] freqs = lhsToFreqs.get(lhs);
        if (freqs == null)
            throw new IllegalArgumentException("No frequencies for lhs: " + lhs);
        return freqs.clone();
    }

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

    public void updateOnEncode(Rule rule) {
        NonTerminal lhs = rule.getLeft();
        Integer symbol = ruleToSymbol.get(lhs).get(Arrays.asList(rule.getRight()));
        increment(lhs, symbol);
    }

}

