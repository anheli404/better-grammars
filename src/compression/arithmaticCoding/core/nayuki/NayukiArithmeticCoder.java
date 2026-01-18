package compression.arithmaticCoding.core.nayuki;

import compression.arithmaticCoding.core.ArithmeticCoder;
import compression.arithmaticCoding.core.symbol.RuleSymbolModel;
import compression.arithmaticCoding.nayukiAc.ArithmeticEncoderNayuki;
import compression.arithmaticCoding.nayukiAc.BitOutputStream;
import compression.arithmaticCoding.nayukiAc.FrequencyTable;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class NayukiArithmeticCoder implements ArithmeticCoder {

    private final RuleSymbolModel symbolModel;

    private final ByteArrayOutputStream byteOut;
    private final BitOutputStream bitOut;

    private ArithmeticEncoderNayuki encoder; // lazy init

    public NayukiArithmeticCoder(RuleSymbolModel symbolModel) {
        this.symbolModel = symbolModel;
        this.byteOut = new ByteArrayOutputStream();
        this.bitOut = new BitOutputStream(byteOut);
        this.encoder = null;
    }

    @Override
    public void encodeRule(Rule rule, RuleProbModel model) {

        NonTerminal lhs = rule.getLeft();
        int symbol = symbolModel.getSymbol(rule);
        FrequencyTable freq = symbolModel.getFrequencyTable(lhs);

        if (encoder == null) {
            encoder = new ArithmeticEncoderNayuki(freq.getSymbolLimit(), bitOut);
        }

        try {
            encoder.write(freq, symbol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void finish() {
        if (encoder != null) {
            try {
                encoder.finish();
                bitOut.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public byte[] getEncodedBytes() {
        return byteOut.toByteArray();
    }
}
