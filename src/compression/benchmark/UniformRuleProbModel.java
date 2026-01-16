package compression.benchmark;

import compression.arithmaticCoding.bigDecimalAc.BigDecimalInterval;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.grammar.Category;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.Grammar;
import compression.samplegrammars.model.RuleProbModel;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * Uniform rule probability model.
 * All rules with the same LHS get equal probability.
 * Suitable for encode + decode benchmark.
 */
public final class UniformRuleProbModel implements RuleProbModel {

    private final Grammar<?> grammar;
    private final Map<NonTerminal, List<Rule>> rulesByLhs = new HashMap<>();
    private final Map<Rule, Interval> ruleIntervals = new HashMap<>();

    public UniformRuleProbModel(Grammar<?> grammar) {
        this.grammar = grammar;
        build();
    }

    private void build() {
        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            rulesByLhs.put(lhs, rules);

            int n = rules.size();
            if (n == 0) continue;

            BigDecimal p = BigDecimal.ONE.divide(
                    BigDecimal.valueOf(n),
                    MathContext.DECIMAL128
            );

            BigDecimal left = BigDecimal.ZERO;

            for (Rule r : rules) {
                ruleIntervals.put(r, new BigDecimalInterval(left, p));
                left = left.add(p);
            }
        }
    }

    @Override
    public Interval getIntervalFor(Rule rule) {
        return ruleIntervals.get(rule);
    }

    @Override
    public List<Interval> getIntervalList(NonTerminal lhs) {
        List<Interval> res = new ArrayList<>();
        for (Rule r : rulesByLhs.get(lhs)) {
            res.add(ruleIntervals.get(r));
        }
        return res;
    }

    @Override
    public List<Category> getRhsFor(Interval interval, NonTerminal lhs) {
        for (Rule r : rulesByLhs.get(lhs)) {
            if (ruleIntervals.get(r).equals(interval)) {
                return Arrays.asList(r.getRight());
            }
        }
        throw new IllegalArgumentException("Interval not found for lhs " + lhs);
    }
}
