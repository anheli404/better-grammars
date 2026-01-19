package compression.arithmaticCoding.benchmark.benchmark_new;

import compression.GenericRNAEncoder;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;
import compression.arithmaticCoding.core.ArithmeticCoder;
import compression.arithmaticCoding.core.nayuki.NayukiArithmeticCoder;
import compression.arithmaticCoding.core.symbol.RuleSymbolModel;
import compression.grammar.*;
import compression.samplegrammars.LiuGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;

import java.nio.file.*;
import java.util.*;

public final class RunSingleFileBenchmarkLiuGrammar_Uniform_StaticNayuki {

    private static long usedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void gc() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) throws Exception {

        Path input = Path.of("datasets/small-dataset/165_120_c.txt");
        List<String> lines = Files.readAllLines(input);

        RNAWithStructure rna =
                new RNAWithStructure(lines.get(0).trim(), lines.get(1).trim());

        RNAGrammar grammar = new LiuGrammar(false).getGrammar();

        Map<Rule, Double> probs = createUniformProbs(grammar);
        RuleProbModel model = new StaticRuleProbModel(grammar, probs);

        // BIGDECIMAL
        GenericRNAEncoder bdEnc =
                new GenericRNAEncoder(model, new ExactArithmeticEncoder(),
                        grammar, grammar.getStartSymbol());

        long bdNs = 0;
        for (int i = 0; i < 20; i++) {
            gc();
            long t0 = System.nanoTime();
            bdEnc.encodeRNA(rna);
            long t1 = System.nanoTime();
            bdNs += (t1 - t0);
        }

         //NAYUKI (FIXED)
        RuleSymbolModel symbolModel =
                new RuleSymbolModel(grammar, model);

        ArithmeticCoder nayukiCoder =
                new NayukiArithmeticCoder(symbolModel);

        BenchmarkRNAEncoder nayukiEnc =
                new BenchmarkRNAEncoder(nayukiCoder, model, grammar);

        long nyNs = 0;
        for (int i = 0; i < 20; i++) {
            gc();
            long t0 = System.nanoTime();
            nayukiEnc.encode(rna);
            long t1 = System.nanoTime();
            nyNs += (t1 - t0);
        }

        String csv =
                "benchmark,grammar,mode,input,backend,encode_ms\n" +
                        "single,liu,uniform," + input.getFileName() +
                        ",BigDecimal," + (bdNs / 20.0 / 1e6) + "\n" +
                        "single,liu,uniform," + input.getFileName() +
                        ",Nayuki," + (nyNs / 20.0 / 1e6);

        Files.writeString(Path.of("single_liu_uniform_fixed.csv"), csv);
        System.out.println(csv);
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
