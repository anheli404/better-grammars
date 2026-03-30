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
 * Encoder class that connects RNA grammar/parsing part of project
 * with Nayuki arithmetic coding backend. Its job is to take an RNAWithStructure
 * object, compute its leftmost derivation, convert each grammar rule in that
 * derivation into a symbol, and then write those symbols with the Nayuki
 * arithmetic encoder using the corresponding frequency tables.
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

    /**
     * Helper method that delegates to LeftmostDerivation class.
     * @param RNA for which we need to compute lmd
     * @return lmd as list of Rule objects
     */
    public List<Rule> leftmostDerivationFor(RNAWithStructure RNA){
        return LeftmostDerivation.rules(parser, RNA);
    }


    /**
     * Computes the lmd, then processes rules one by one.
     * For each rule, it asks the RuleSymbolModel for two things: symbol
     * assigned to that rule, frequency table for all rules that share
     * the same lhs. These frequencies are then wrapped into a Nayuki
     * SimpleFrequencyTable, and the resulting symbol is written to
     * the arithmetic encoder. At each step, the NT currently being expanded
     * determines the coding alphabet and the chosen production rule is encoded
     * as one symbol within that alphabet.
     * @param RNA that needs to be encoded
     * @return final byte array which is the binary encoding of the given RNA.
     */
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
                symbolModel.updateOnEncode(rule);
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

    /**
     * Convenience method that turns the encoded RNA byte array
     * into a bit string.
     * @param RNA to be encoded
     * @return encoded RNA bit string
     */
    public String encodeRNANayukiToBitString(RNAWithStructure RNA) {
        byte[] encoded = encodeRNANayuki(RNA);
        return bytesToBitString(encoded);
    }


    /**
     * Helper method for converting the byte array containing
     * the encoding of the RNA structure into a bit string
     * @param encoded encoded RNA byte array
     * @return encoded RNA bit string
     */
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
