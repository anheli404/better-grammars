import compression.GenericRNADecoderNayuki;
import compression.GenericRNAEncoderNayuki;
import compression.Training;
import compression.coding.nayuki.BitInputStream;
import compression.coding.nayuki.BitOutputStream;
import compression.coding.nayuki.NayukiDecoder;
import compression.coding.nayuki.NayukiEncoder;
import compression.data.TrainingDataset;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.RuleCountsForGrammarLaPlace;
import compression.samplegrammars.SampleGrammar;
import compression.samplegrammars.model.bigdecimal.AdaptiveRuleProbModel;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.bigdecimal.SemiAdaptiveRuleProbModel;
import compression.samplegrammars.model.bigdecimal.StaticRuleProbModel;
import compression.samplegrammars.model.nayuki.AdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import compression.samplegrammars.model.nayuki.SemiAdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.StaticRuleSymbolModel;
import junit.framework.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class SampleInstanceNayuki4Tests {

    SampleGrammar G;

    public SampleInstanceNayuki4Tests(SampleGrammar newG) {
        this.G = newG;
    }

    public void runEncodeNDecodeStaticNayuki(RNAWithStructure rnaws, TrainingDataset tDataset) throws IOException {
        // Build StaticRuleProbModel as well, because GenericRNAEncoderNayuki uses it in parser for static derivation choice
        RuleProbModel rpmStatic = new StaticRuleProbModel(G.getGrammar(), G.readRuleProbs(tDataset.ruleProbsFileFor(G)));

        // Build rule counts for the Nayuki symbol model
        Map<Rule, Long> ruleCounts = new RuleCountsForGrammarLaPlace(G.getGrammar(), tDataset).ruleCounts();
        RuleSymbolModel rsmStatic = new StaticRuleSymbolModel(G.getGrammar(), ruleCounts);

        // Encoding
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(byteOut);
        NayukiEncoder encoder = new NayukiEncoder(32, bitOut);
        GenericRNAEncoderNayuki genericEncoderStatic = new GenericRNAEncoderNayuki(rpmStatic, rsmStatic, encoder, byteOut, bitOut, G.getGrammar(), G.getStartSymbol());
        byte[] encodedBytes = genericEncoderStatic.encodeRNANayuki(rnaws);

        // Decoding
        ByteArrayInputStream byteIn = new ByteArrayInputStream(encodedBytes);
        BitInputStream bitIn = new BitInputStream(byteIn);
        NayukiDecoder decoder = new NayukiDecoder(32, bitIn);
        GenericRNADecoderNayuki genericDecoderStatic = new GenericRNADecoderNayuki(rsmStatic, decoder, G.getStartSymbol());
        RNAWithStructure decoded = genericDecoderStatic.decode();

        // Comparison
        Assert.assertEquals(rnaws, decoded);
    }

    public void runEncodeNDecodeSemiAdaptiveNayuki(RNAWithStructure rnaws) throws IOException {
        // Build StaticRuleProbModel as well, because GenericRNAEncoderNayuki uses it in parser for static derivation choice
        RuleProbModel rpm = new SemiAdaptiveRuleProbModel(G.getGrammar(), rnaws);
        RuleSymbolModel rsmSemiAdaptive = new SemiAdaptiveRuleSymbolModel(G.getGrammar(), rnaws);

        // Encoding
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(byteOut);
        NayukiEncoder encoder = new NayukiEncoder(32, bitOut);
        GenericRNAEncoderNayuki genericEncoderSemiAdaptive = new GenericRNAEncoderNayuki(rpm, rsmSemiAdaptive, encoder, byteOut, bitOut, G.getGrammar(), G.getStartSymbol());
        byte[] encodedBytes = genericEncoderSemiAdaptive.encodeRNANayuki(rnaws);

        // Decoding
        ByteArrayInputStream byteIn = new ByteArrayInputStream(encodedBytes);
        BitInputStream bitIn = new BitInputStream(byteIn);
        NayukiDecoder decoder = new NayukiDecoder(32, bitIn);
        GenericRNADecoderNayuki genericDecoderSemiAdaptive = new GenericRNADecoderNayuki(rsmSemiAdaptive, decoder, G.getStartSymbol());
        RNAWithStructure decoded = genericDecoderSemiAdaptive.decode();

        // Comparison
        Assert.assertEquals(rnaws, decoded);
    }

    public void runEncodeNDecodeAdaptiveNayuki(RNAWithStructure rnaws) throws IOException {
        RuleProbModel rpm = new AdaptiveRuleProbModel(G.getGrammar());
        RuleSymbolModel rsmAdaptiveEncoding = new AdaptiveRuleSymbolModel(G.getGrammar());
        RuleSymbolModel rsmAdaptiveDecoding = new AdaptiveRuleSymbolModel(G.getGrammar());

        // Encoding
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(byteOut);
        NayukiEncoder encoder = new NayukiEncoder(32, bitOut);
        GenericRNAEncoderNayuki genericEncoderAdaptive = new GenericRNAEncoderNayuki(rpm, rsmAdaptiveEncoding, encoder, byteOut, bitOut, G.getGrammar(), G.getStartSymbol());
        byte[] encodedBytes = genericEncoderAdaptive.encodeRNANayuki(rnaws);

         // Decoding
         ByteArrayInputStream byteIn = new ByteArrayInputStream(encodedBytes);
         BitInputStream bitIn = new BitInputStream(byteIn);
         NayukiDecoder decoder = new NayukiDecoder(32, bitIn);
         GenericRNADecoderNayuki genericDecoderAdaptive = new GenericRNADecoderNayuki(rsmAdaptiveDecoding, decoder, G.getStartSymbol());
         RNAWithStructure decoded = genericDecoderAdaptive.decode();

         // Compare
         Assert.assertEquals(rnaws, decoded);

    }

}