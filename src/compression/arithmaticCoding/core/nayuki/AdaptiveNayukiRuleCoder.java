package compression.arithmaticCoding.core.nayuki;

import compression.arithmaticCoding.nayukiAc.*;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.RNAGrammar;
import compression.samplegrammars.model.RuleProbModel;

import java.io.IOException;
import java.util.*;

public final class AdaptiveNayukiRuleCoder {

    private final ArithmeticEncoderNayuki encoder;
    private final Map<NonTerminal, SimpleFrequencyTable> freqTables;
    private final Map<NonTerminal, List<Rule>> rulesByLhs;

    public AdaptiveNayukiRuleCoder(
            RNAGrammar grammar,
            ArithmeticEncoderNayuki encoder
    ) {
        this.encoder = encoder;
        this.freqTables = new HashMap<>();
        this.rulesByLhs = new HashMap<>();

        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            rulesByLhs.put(lhs, rules);

            int[] freqs = new int[rules.size()];
            Arrays.fill(freqs, 1); // adaptive, start uniform

            freqTables.put(lhs, new SimpleFrequencyTable(freqs));
        }
    }

    public void encodeRule(Rule rule) throws IOException {
        NonTerminal lhs = rule.getLeft();
        List<Rule> rules = rulesByLhs.get(lhs);
        int symbol = rules.indexOf(rule);

        if (symbol < 0) {
            throw new IllegalStateException("Rule not found for LHS");
        }

        SimpleFrequencyTable freq = freqTables.get(lhs);
        encoder.write(freq, symbol);
        freq.increment(symbol);
    }

    public void finish() throws IOException {
        encoder.finish();
    }
}
