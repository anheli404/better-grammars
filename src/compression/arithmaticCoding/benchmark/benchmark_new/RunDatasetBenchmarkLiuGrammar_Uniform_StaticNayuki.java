package compression.arithmaticCoding.benchmark.benchmark_new;

import compression.arithmaticCoding.core.ArithmeticCoder;
import compression.arithmaticCoding.core.bigdecimal.BigDecimalArithmeticCoder;
import compression.arithmaticCoding.core.nayuki.NayukiArithmeticCoder;
import compression.arithmaticCoding.core.symbol.RuleSymbolModel;
import compression.grammar.*;
import compression.samplegrammars.LiuGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;

import java.nio.file.*;
import java.util.*;

public final class RunDatasetBenchmarkLiuGrammar_Uniform_StaticNayuki {

    public static void main(String[] args) throws Exception {

        RNAGrammar grammar = new LiuGrammar(false).getGrammar();
        Path datasetDir = Path.of("datasets/small-dataset");

        Map<Rule, Double> probs = createUniformProbs(grammar);
        RuleProbModel model = new StaticRuleProbModel(grammar, probs);

        RuleSymbolModel symbolModel =
                new RuleSymbolModel(grammar, model);

        ArithmeticCoder bd = new BigDecimalArithmeticCoder();
        ArithmeticCoder ny = new NayukiArithmeticCoder(symbolModel);

        BenchmarkRNAEncoder bdEnc =
                new BenchmarkRNAEncoder(bd, model, grammar);
        BenchmarkRNAEncoder nyEnc =
                new BenchmarkRNAEncoder(ny, model, grammar);

        long bdTime = 0;
        long nyTime = 0;

        for (Path file : Files.newDirectoryStream(datasetDir, "*.txt")) {

            List<String> lines = Files.readAllLines(file);
            RNAWithStructure rna = new RNAWithStructure(
                    lines.get(0).trim(),
                    lines.get(1).trim()
            );

            long t0 = System.nanoTime();
            bdEnc.encode(rna);
            long t1 = System.nanoTime();

            long t2 = System.nanoTime();
            nyEnc.encode(rna);
            long t3 = System.nanoTime();

            bdTime += (t1 - t0);
            nyTime += (t3 - t2);
        }

        System.out.println("Dataset Liu Uniform");
        System.out.println("BD total ms = " + bdTime / 1e6);
        System.out.println("Nayuki total ms = " + nyTime / 1e6);
    }

    private static Map<Rule, Double> createUniformProbs(RNAGrammar grammar) {
        Map<Rule, Double> probs = new HashMap<>();
        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            double p = 1.0 / rules.size();
            for (Rule r : rules) probs.put(r, p);
        }
        return probs;
    }
}
