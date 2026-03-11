import compression.GenericRNADecoderNayuki;
import compression.GenericRNAEncoderNayuki;
import compression.coding.nayuki.BitInputStream;
import compression.coding.nayuki.BitOutputStream;
import compression.coding.nayuki.NayukiDecoder;
import compression.coding.nayuki.NayukiEncoder;
import compression.data.TrainingDataset;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.RuleCountsForGrammarLaPlace;
import compression.samplegrammars.SampleGrammar;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.bigdecimal.StaticRuleProbModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import compression.samplegrammars.model.nayuki.StaticRuleSymbolModel;
import junit.framework.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class SampleInstanceNayuki4Tests {

    SampleGrammar G;

    public SampleInstanceNayuki4Tests(SampleGrammar newG) {
        G = newG;
    }

    public void runEncodeNDecodeStaticNayuki(RNAWithStructure rnaws, TrainingDataset tDataset) throws IOException {
        // Build the old static probability model as well, because
        // GenericRNAEncoderNayuki uses it in the parser for static derivation choice
        RuleProbModel rpmStatic = new StaticRuleProbModel(
                G.getGrammar(),
                G.readRuleProbs(tDataset.ruleProbsFileFor(G))
        );

        // Build rule counts for the Nayuki symbol model
        Map<Rule, Long> ruleCounts = new RuleCountsForGrammarLaPlace(G.getGrammar(), tDataset).ruleCounts();
        RuleSymbolModel rsmStatic = new StaticRuleSymbolModel(G.getGrammar(), ruleCounts);

        // Encoding
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(byteOut);
        NayukiEncoder encoder = new NayukiEncoder(32, bitOut);

        GenericRNAEncoderNayuki graStatic = new GenericRNAEncoderNayuki(
                rpmStatic,
                rsmStatic,
                encoder,
                byteOut,
                bitOut,
                G.getGrammar(),
                G.getStartSymbol()
        );

        byte[] encodedBytes = graStatic.encodeRNANayuki(rnaws);

        // Decoding
        ByteArrayInputStream byteIn = new ByteArrayInputStream(encodedBytes);
        BitInputStream bitIn = new BitInputStream(byteIn);
        NayukiDecoder decoder = new NayukiDecoder(32, bitIn);

        GenericRNADecoderNayuki grad = new GenericRNADecoderNayuki(
                rsmStatic,
                decoder,
                G.getStartSymbol()
        );

        RNAWithStructure decoded = grad.decode();

        Assert.assertEquals(rnaws, decoded);
    }
}