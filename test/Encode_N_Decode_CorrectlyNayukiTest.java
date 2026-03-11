import compression.GenericRNADecoderNayuki;
import compression.GenericRNAEncoderNayuki;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.data.TrainingDataset;
import compression.grammar.RNAWithStructure;
import compression.samplegrammars.DowellGrammar1Bound;
import compression.samplegrammars.SampleGrammar;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class Encode_N_Decode_CorrectlyNayukiTest {

    Dataset dataset = new FolderBasedDataset("TestDataSet");
    TrainingDataset trainingDataset = new TrainingDataset("TestTrainingData");
    boolean withNonCanonicalRules = true;

    List<SampleGrammar> listOfGrammars = List.of(
            new DowellGrammar1Bound(withNonCanonicalRules)
    );

    @Test
    public void testCorrectnessStaticNayuki() throws IOException {
        for (SampleGrammar grammar : listOfGrammars) {
            for (RNAWithStructure RNAWS : dataset) {
                SampleInstanceNayuki4Tests SI4T = new SampleInstanceNayuki4Tests(grammar);

                System.out.println(RNAWS);
                SI4T.runEncodeNDecodeStaticNayuki(RNAWS, trainingDataset);
            }
        }
    }
}