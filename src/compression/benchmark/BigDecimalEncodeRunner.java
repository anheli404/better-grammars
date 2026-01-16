package compression.benchmark;

import compression.GenericRNAEncoder;
import compression.grammar.*;
import compression.arithmaticCoding.bigDecimalAc.*;
import compression.samplegrammars.model.RuleProbModel;

import java.util.List;

public final class BigDecimalEncodeRunner {

    // Returns final binary encoding string
    public static String encode(
            GenericRNAEncoder encoder,
            RNAWithStructure rna,
            RuleProbModel model
    ) {

        ExactArithmeticEncoder engine = new ExactArithmeticEncoder();

        for (Rule rule : encoder.leftmostDerivationFor(rna)) {
            Interval chosen = model.getIntervalFor(rule);
            engine.encodeNext(chosen);
        }

        return engine.getFinalEncoding();
    }

    private BigDecimalEncodeRunner() {}
}
