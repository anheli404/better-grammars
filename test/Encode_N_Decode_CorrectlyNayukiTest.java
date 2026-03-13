import compression.GenericRNADecoderNayuki;
import compression.GenericRNAEncoderNayuki;
import compression.LocalConfig;
import compression.coding.nayuki.BitInputStream;
import compression.coding.nayuki.BitOutputStream;
import compression.coding.nayuki.NayukiDecoder;
import compression.coding.nayuki.NayukiEncoder;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.data.TrainingDataset;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.SecondaryStructureGrammar;
import compression.parser.GrammarReaderNWriter;
import compression.samplegrammars.DowellGrammar1Bound;
import compression.samplegrammars.SampleGrammar;
import compression.samplegrammars.model.bigdecimal.AdaptiveRuleProbModel;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.nayuki.AdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Encode_N_Decode_CorrectlyNayukiTest {

    Dataset dataset = new FolderBasedDataset("dowell-benchmark-10-percent");
    TrainingDataset trainingDataset = new TrainingDataset("TestTrainingData");
    boolean withNonCanonicalRules = true;

    List<SampleGrammar> listOfGrammars = List.of(
            new DowellGrammar1Bound(withNonCanonicalRules)
    );

    @Test
    public void testEncodeNDecode4AutoGenGrammarsNayuki() throws IOException {
        List<SecondaryStructureGrammar> listOfGrammars = new ArrayList<>();
        String folderNameForGrammars = "testing-10";
        File grammarFiles = new File(LocalConfig.GIT_ROOT + "/grammars/" + folderNameForGrammars);

        File[] listOfFiles = grammarFiles.listFiles();
        Random random = new Random(11124);
        for (int i = 0; i < 30; i++) {
            File randomGrammarFileSelection = listOfFiles[random.nextInt(listOfFiles.length)];
            RNAGrammar g = RNAGrammar.from(
                    new GrammarReaderNWriter(randomGrammarFileSelection.getPath()).getGrammarFromFile(),
                    true);

            Dataset dataset = new FolderBasedDataset("parsable");
            File RNAFiles = new File(LocalConfig.GIT_ROOT + "/datasets/parsable");
            File[] listOfRNAFiles = RNAFiles.listFiles();
            RNAWithStructure rnaws = FolderBasedDataset.readRNA(listOfRNAFiles[random.nextInt(listOfRNAFiles.length)]);

            RuleProbModel rpm = new AdaptiveRuleProbModel(g);
            RuleSymbolModel encodingSymbolModel = new AdaptiveRuleSymbolModel(g);

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            BitOutputStream bitOut = new BitOutputStream(byteOut);
            NayukiEncoder encoder = new NayukiEncoder(32, bitOut);
            GenericRNAEncoderNayuki genericEncoder = new GenericRNAEncoderNayuki(
                    rpm, encodingSymbolModel, encoder, byteOut, bitOut, g, g.getStartSymbol());
            byte[] encodedBytes = genericEncoder.encodeRNANayuki(rnaws);

            RuleSymbolModel decodingSymbolModel = new AdaptiveRuleSymbolModel(g);
            ByteArrayInputStream byteIn = new ByteArrayInputStream(encodedBytes);
            BitInputStream bitIn = new BitInputStream(byteIn);
            NayukiDecoder decoder = new NayukiDecoder(32, bitIn);
            GenericRNADecoderNayuki genericDecoder =
                    new GenericRNADecoderNayuki(decodingSymbolModel, decoder, g.getStartSymbol());
            RNAWithStructure decoded = genericDecoder.decode();

            Assert.assertEquals(rnaws, decoded);
        }
    }

    @Test
    public void testCorrectnessNayuki() throws IOException {
        for (SampleGrammar grammar : listOfGrammars) {
            for (RNAWithStructure RNAWS : dataset) {
                SampleInstanceNayuki4Tests SI4T = new SampleInstanceNayuki4Tests(grammar);

                System.out.println(RNAWS);
                SI4T.runEncodeNDecodeStaticNayuki(RNAWS, trainingDataset);
                SI4T.runEncodeNDecodeSemiAdaptiveNayuki(RNAWS);
                SI4T.runEncodeNDecodeAdaptiveNayuki(RNAWS);

            }
        }
    }
}
