package compression.benchmark;

import compression.arithmaticCoding.bigDecimalAc.BigDecimalInterval;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.grammar.Category;
import compression.grammar.Grammar;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * Skewed rule probability model.
 * One dominant rule per LHS gets alpha probability,
 * remaining rules share (1-alpha) uniformly.
 */
public final class SkewedRuleProbModel implements RuleProbModel {

    private static final MathContext MC = MathContext.DECIMAL128;

    private final Map<NonTerminal, List<Rule>> rulesByLhs = new HashMap<>();
    private final Map<Rule, Interval> intervals = new HashMap<>();

    public SkewedRuleProbModel(Grammar<?> grammar, double alpha) {
        if (alpha <= 0.0 || alpha >= 1.0)
            throw new IllegalArgumentException("alpha must be in (0,1)");

        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            rulesByLhs.put(lhs, rules);

            int n = rules.size();
            if (n == 0) continue;

            BigDecimal left = BigDecimal.ZERO;

            BigDecimal alphaBD = BigDecimal.valueOf(alpha);
            BigDecimal restBD = BigDecimal.ONE.subtract(alphaBD, MC);

            for (int i = 0; i < n; i++) {
                BigDecimal p;
                if (i == 0) {
                    p = alphaBD;
                } else {
                    p = restBD.divide(BigDecimal.valueOf(n - 1), MC);
                }

                intervals.put(rules.get(i), new BigDecimalInterval(left, p));
                left = left.add(p, MC);
            }
        }
    }

    @Override
    public Interval getIntervalFor(Rule rule) {
        return intervals.get(rule);
    }

    @Override
    public List<Interval> getIntervalList(NonTerminal lhs) {
        List<Interval> res = new ArrayList<>();
        for (Rule r : rulesByLhs.get(lhs)) {
            res.add(intervals.get(r));
        }
        return res;
    }

    @Override
    public List<Category> getRhsFor(Interval interval, NonTerminal lhs) {
        for (Rule r : rulesByLhs.get(lhs)) {
            if (intervals.get(r).equals(interval)) {
                return Arrays.asList(r.getRight());
            }
        }
        throw new IllegalArgumentException("Interval not found");
    }
}
