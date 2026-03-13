package compression;

import compression.coding.nayuki.BitOutputStream;
import compression.coding.nayuki.FrequencyTable;
import compression.coding.nayuki.NayukiEncoder;
import compression.coding.nayuki.SimpleFrequencyTable;
import compression.parser.SRFParser;
import compression.parser.StochasticParser;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.LeftmostDerivation;
import compression.grammar.NonTerminal;
import compression.grammar.Rule;

import compression.grammar.*;
import compression.samplegrammars.model.bigdecimal.StaticRuleProbModel;
import compression.samplegrammars.model.nayuki.AdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


/**
 * @author Anhelina Kichihina
 */
public class GenericRNAEncoderNayuki {

    protected final NayukiEncoder encoder;
    protected final RuleSymbolModel symbolModel;
    protected final RuleProbModel probModel;
    protected final RNAGrammar grammar;
    protected final NonTerminal startSymbol;
    protected final StochasticParser<PairOfChar> parser;
    private final ByteArrayOutputStream out;
    private final BitOutputStream bitOut;

    public GenericRNAEncoderNayuki(RuleProbModel probModel, RuleSymbolModel symbolModel, NayukiEncoder encoder,
                                   ByteArrayOutputStream out, BitOutputStream bitOut, RNAGrammar grammar, NonTerminal startSymbol) {
        this.encoder = encoder;
        this.probModel = probModel;
        this.symbolModel = symbolModel;
        this.out = out;
        this.grammar = grammar;
        this.startSymbol = startSymbol;
        this.bitOut = bitOut;
        // Only use the ruleProbModel in the parser if it is static (otherwise use dummy model)
        // NB: We should NOT use a semiadaptive model in the parser (even though it is static after training),
        // as require to get the SAME derivation
        if (probModel instanceof StaticRuleProbModel)
            this.parser = new SRFParser<>(grammar, probModel);
        else
            this.parser = new SRFParser<>(grammar, RuleProbModel.DONT_CARE);
    }

    public List<Rule> leftmostDerivationFor(RNAWithStructure RNA){
        return LeftmostDerivation.rules(parser, RNA);
    }


    public byte[] encodeRNANayuki(RNAWithStructure RNA) {
        List<Rule> lmd = leftmostDerivationFor(RNA);
        for (Rule rule : lmd) {
            int symbol = symbolModel.getSymbolFor(rule);
            int[] freqs = symbolModel.getFrequenciesFor(rule.getLeft());
            FrequencyTable freqTable = new SimpleFrequencyTable(freqs);
            try {
                encoder.write(freqTable, symbol);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            if (symbolModel instanceof AdaptiveRuleSymbolModel) {
                ((AdaptiveRuleSymbolModel) symbolModel).updateOnEncode(rule);
            }
        }
        try {
            encoder.finish();
            bitOut.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return out.toByteArray();
    }

    public String encodeRNANayukiToBitString(RNAWithStructure RNA) {
        byte[] encoded = encodeRNANayuki(RNA);
        return bytesToBitString(encoded);
    }


    private String bytesToBitString(byte[] encoded) {
        StringBuilder sb = new StringBuilder(encoded.length * 8);
        for (byte b : encoded) {
            for (int i = 7; i >= 0; i--) {
                sb.append((b >>> i) & 1);
            }
        }
        return sb.toString();
    }

}
