package compression.coding;

import compression.RuleProbType;
import compression.coding.backend.ArithmeticCodingBackend;
import compression.coding.backend.EncodedRNA;
import compression.data.FolderBasedDataset;
import compression.grammar.RNAWithStructure;
import compression.samplegrammars.DowellGrammar1Bound;
import compression.samplegrammars.SampleGrammar;
import compression.samplegrammars.model.bigdecimal.AdaptiveRuleProbModel;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class ArithmeticCodingFactoryTest {

    @Test
    public void roundTripAdaptiveBigDecimal() throws IOException {
        assertRoundTrip(ArithmeticCodingFactory.Backend.BIG_DECIMAL);
    }

    @Test
    public void roundTripAdaptiveNayuki() throws IOException {
        assertRoundTrip(ArithmeticCodingFactory.Backend.NAYUKI);
    }

    private void assertRoundTrip(ArithmeticCodingFactory.Backend backend) throws IOException {
        SampleGrammar grammar = new DowellGrammar1Bound(true);
        RNAWithStructure rna = new FolderBasedDataset("TestDataSet").getRNA("testRna.txt");
        ArithmeticCodingBackend codingBackend = ArithmeticCodingFactory.create(backend);

        RuleProbModel encodingProbModel = new AdaptiveRuleProbModel(grammar.getGrammar());
        RuleSymbolModel encodingSymbolModel = codingBackend.createRuleSymbolModel(
                RuleProbType.ADAPTIVE,
                grammar.getGrammar(),
                rna,
                null);

        EncodedRNA encoded = codingBackend.createEncoder(
                        encodingProbModel,
                        encodingSymbolModel,
                        grammar.getGrammar(),
                        grammar.getStartSymbol())
                .encode(rna);

        RuleProbModel decodingProbModel = new AdaptiveRuleProbModel(grammar.getGrammar());
        RuleSymbolModel decodingSymbolModel = codingBackend.createRuleSymbolModel(
                RuleProbType.ADAPTIVE,
                grammar.getGrammar(),
                rna,
                null);

        RNAWithStructure decoded = codingBackend.createDecoder(
                        decodingProbModel,
                        decodingSymbolModel,
                        grammar.getStartSymbol(),
                        encoded)
                .decode();

        Assert.assertEquals(rna, decoded);
        Assert.assertTrue(encoded.bitLength() > 0);
    }
}
