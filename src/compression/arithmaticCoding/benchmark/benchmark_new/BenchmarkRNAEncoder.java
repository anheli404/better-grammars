package compression.arithmaticCoding.benchmark.benchmark_new;

import compression.arithmaticCoding.core.ArithmeticCoder;
import compression.grammar.*;
import compression.parser.Parser;
import compression.parser.SRFParser;
import compression.samplegrammars.LeftmostDerivation;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;

public final class BenchmarkRNAEncoder {

    private final ArithmeticCoder coder;
    private final RuleProbModel model;
    private final Parser<PairOfChar> parser;

    public BenchmarkRNAEncoder(
            ArithmeticCoder coder,
            RuleProbModel model,
            RNAGrammar grammar) {

        this.coder = coder;
        this.model = model;

        this.parser = new SRFParser<>(
                grammar,
                model instanceof StaticRuleProbModel
                        ? model
                        : RuleProbModel.DONT_CARE
        );
    }

    public void encode(RNAWithStructure rna) {
        for (Rule rule : LeftmostDerivation.rules(parser, rna)) {
            coder.encodeRule(rule, model);
        }
        coder.finish();
    }
}