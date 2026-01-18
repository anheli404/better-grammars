package compression.arithmaticCoding.benchmark.benchmark_new;


import compression.arithmaticCoding.core.ArithmeticCoder;
import compression.arithmaticCoding.core.bigdecimal.BigDecimalArithmeticCoder;
import compression.arithmaticCoding.core.nayuki.NayukiArithmeticCoder;
import compression.arithmaticCoding.core.symbol.RuleSymbolModel;
import compression.grammar.*;
import compression.samplegrammars.LiuGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class BenchmarkSingle_Liu_Skew70 {

    public static void main(String[] args) throws Exception {

        Path input = Path.of("datasets/small-dataset/165_120_c.txt");
        List<String> lines = Files.readAllLines(input);
        RNAWithStructure rna = new RNAWithStructure(
                lines.get(0).trim(),
                lines.get(1).trim()
        );

        RNAGrammar grammar = new LiuGrammar(false).getGrammar();

        Map<Rule, Double> probs = createSkewedProbs(grammar);
        RuleProbModel model = new StaticRuleProbModel(grammar, probs);

        RuleSymbolModel symbolModel =
                new RuleSymbolModel(grammar, model);

        ArithmeticCoder bd = new BigDecimalArithmeticCoder();
        ArithmeticCoder ny = new NayukiArithmeticCoder(symbolModel);

        BenchmarkRNAEncoder bdEnc =
                new BenchmarkRNAEncoder(bd, model, grammar);
        BenchmarkRNAEncoder nyEnc =
                new BenchmarkRNAEncoder(ny, model, grammar);

        long t0 = System.nanoTime();
        bdEnc.encode(rna);
        long t1 = System.nanoTime();

        long t2 = System.nanoTime();
        nyEnc.encode(rna);
        long t3 = System.nanoTime();

        System.out.println("Single Liu Skew70");
        System.out.println("BD ms = " + (t1 - t0) / 1e6);
        System.out.println("Nayuki ms = " + (t3 - t2) / 1e6);
    }

    private static Map<Rule, Double> createSkewedProbs(RNAGrammar grammar) {
        Map<Rule, Double> probs = new HashMap<>();
        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            probs.put(rules.get(0), 0.7);
            double rest = 0.3 / (rules.size() - 1);
            for (int i = 1; i < rules.size(); i++)
                probs.put(rules.get(i), rest);
        }
        return probs;
    }
}
