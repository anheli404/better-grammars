package compression.arithmaticCoding.core.nayuki;

import compression.arithmaticCoding.core.ArithmeticDecoder;
import compression.arithmaticCoding.core.symbol.RuleSymbolModel;
import compression.arithmaticCoding.nayukiAc.ArithmeticDecoderNayuki;
import compression.arithmaticCoding.nayukiAc.BitInputStream;
import compression.arithmaticCoding.nayukiAc.FrequencyTable;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class NayukiArithmeticDecoder implements ArithmeticDecoder {

    private final RuleSymbolModel symbolModel;
    private final ArithmeticDecoderNayuki decoder;

    public NayukiArithmeticDecoder(byte[] encoded, RuleSymbolModel symbolModel, int symbolLimit) {
        this.symbolModel = symbolModel;
        try {
            BitInputStream bitIn = new BitInputStream(new ByteArrayInputStream(encoded));
            this.decoder = new ArithmeticDecoderNayuki(symbolLimit, bitIn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Rule decodeRule(NonTerminal lhs, RuleProbModel model) {

        FrequencyTable freq = symbolModel.getFrequencyTable(lhs);

        int symbol;
        try {
            symbol = decoder.read(freq);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return symbolModel.getRule(lhs, symbol);
    }
}
