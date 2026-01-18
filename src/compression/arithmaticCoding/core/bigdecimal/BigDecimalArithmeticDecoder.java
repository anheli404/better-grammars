package compression.arithmaticCoding.core.bigdecimal;

import compression.arithmaticCoding.core.ArithmeticDecoder;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.grammar.Category;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

import java.util.List;

public final class BigDecimalArithmeticDecoder implements ArithmeticDecoder {

    private final compression.arithmaticCoding.bigDecimalAc.ArithmeticDecoder decoder;

    public BigDecimalArithmeticDecoder(
            compression.arithmaticCoding.bigDecimalAc.ArithmeticDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Rule decodeRule(NonTerminal lhs, RuleProbModel model) {

        List<Interval> options = model.getIntervalList(lhs);
        Interval chosen = decoder.decodeNext(options);
        List<Category> rhs = model.getRhsFor(chosen, lhs);

        // IMPORTANT: convert List<Category> → Category[]
        return new Rule(lhs, rhs.toArray(new Category[0]));
    }
}
