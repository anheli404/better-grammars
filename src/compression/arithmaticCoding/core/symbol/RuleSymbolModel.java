package compression.arithmaticCoding.core.symbol;

import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.RNAGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.arithmaticCoding.nayukiAc.FrequencyTable;
import compression.arithmaticCoding.nayukiAc.SimpleFrequencyTable;

import java.util.*;

/**
 * Static mapping from grammar rules to Nayuki symbols + frequency tables.
 *
 * FINAL FIX:
 * - Allow ZERO frequencies
 * - Normalize total strictly below Nayuki MAX_TOTAL
 */
public final class RuleSymbolModel {

    /** Nayuki hard limit */
    private static final int MAX_TOTAL = (1 << 15) - 1; // 32767

    private final Map<NonTerminal, List<Rule>> rulesByLhs;
    private final Map<Rule, Integer> ruleToSymbol;
    private final Map<NonTerminal, FrequencyTable> freqTables;

    public RuleSymbolModel(RNAGrammar grammar, RuleProbModel model) {

        this.rulesByLhs = new HashMap<>();
        this.ruleToSymbol = new HashMap<>();
        this.freqTables = new HashMap<>();

        for (NonTerminal lhs : grammar.getNonTerminals()) {

            /* ================= RULE LIST ================= */

            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            rulesByLhs.put(lhs, rules);

            for (int i = 0; i < rules.size(); i++) {
                ruleToSymbol.put(rules.get(i), i);
            }

            /* ================= INTERVALS ================= */

            List<Interval> intervals = model.getIntervalList(lhs);
            int n = intervals.size();

            if (n != rules.size()) {
                throw new IllegalStateException(
                        "Rule count mismatch for " + lhs +
                                ": grammar=" + rules.size() +
                                ", model=" + n
                );
            }

            /* ================= PROBABILITIES ================= */

            double[] probs = new double[n];
            double sum = 0.0;

            for (int i = 0; i < n; i++) {
                probs[i] = intervals.get(i).getLength().doubleValue();
                sum += probs[i];
            }

            /* ================= FREQUENCY NORMALIZATION ================= */

            int[] freqs = new int[n];
            int total = 0;

            for (int i = 0; i < n; i++) {
                int f = (int) Math.floor((probs[i] / sum) * MAX_TOTAL);
                freqs[i] = Math.max(0, f);   // 🚨 ZERO ALLOWED
                total += freqs[i];
            }

            /* ensure at least one symbol is encodable */
            if (total == 0) {
                freqs[0] = 1;
                total = 1;
            }

            /* hard safety clamp */
            while (total > MAX_TOTAL) {
                for (int i = 0; i < n && total > MAX_TOTAL; i++) {
                    if (freqs[i] > 0) {
                        freqs[i]--;
                        total--;
                    }
                }
            }

            freqTables.put(lhs, new SimpleFrequencyTable(freqs));
        }
    }

    /** Rule → symbol */
    public int getSymbol(Rule rule) {
        Integer s = ruleToSymbol.get(rule);
        if (s == null) {
            throw new IllegalStateException("Rule not found in symbol model: " + rule);
        }
        return s;
    }

    /** (lhs, symbol) → Rule */
    public Rule getRule(NonTerminal lhs, int symbol) {
        return rulesByLhs.get(lhs).get(symbol);
    }

    /** Frequency table for a non-terminal */
    public FrequencyTable getFrequencyTable(NonTerminal lhs) {
        return freqTables.get(lhs);
    }
}
