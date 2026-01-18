package compression.arithmaticCoding.core.nayuki;

import compression.arithmaticCoding.nayukiAc.*;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.grammar.RNAGrammar;

import java.io.IOException;
import java.util.*;

public final class AdaptiveNayukiRuleDecoder {

    private final ArithmeticDecoderNayuki decoder;
    private final Map<NonTerminal, SimpleFrequencyTable> freqTables;
    private final Map<NonTerminal, List<Rule>> rulesByLhs;

    public AdaptiveNayukiRuleDecoder(
            RNAGrammar grammar,
            ArithmeticDecoderNayuki decoder
    ) {
        this.decoder = decoder;
        this.freqTables = new HashMap<>();
        this.rulesByLhs = new HashMap<>();

        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            rulesByLhs.put(lhs, rules);

            int[] freqs = new int[rules.size()];
            Arrays.fill(freqs, 1);

            freqTables.put(lhs, new SimpleFrequencyTable(freqs));
        }
    }

    public Rule decodeRule(NonTerminal lhs) throws IOException {
        SimpleFrequencyTable freq = freqTables.get(lhs);
        int symbol = decoder.read(freq);

        Rule rule = rulesByLhs.get(lhs).get(symbol);
        freq.increment(symbol);
        return rule;
    }
}
