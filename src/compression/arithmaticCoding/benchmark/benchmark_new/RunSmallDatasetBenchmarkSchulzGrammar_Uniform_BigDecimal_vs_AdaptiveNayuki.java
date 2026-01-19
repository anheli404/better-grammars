package compression.arithmaticCoding.benchmark.benchmark_new;

import compression.GenericRNAEncoder;
import compression.arithmaticCoding.core.bigdecimal.BigDecimalArithmeticCoder;
import compression.arithmaticCoding.core.nayuki.AdaptiveNayukiRuleCoder;
import compression.arithmaticCoding.nayukiAc.ArithmeticEncoderNayuki;
import compression.arithmaticCoding.nayukiAc.BitOutputStream;
import compression.grammar.*;
import compression.samplegrammars.SchulzGrammar;
import compression.samplegrammars.LeftmostDerivation;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;

import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

public final class RunSmallDatasetBenchmarkSchulzGrammar_Uniform_BigDecimal_vs_AdaptiveNayuki {

    private static final int RUNS = 10;

    private static long usedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void gcAndSleep() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
    }

    public static void main(String[] args) throws Exception {

        Path datasetRoot = Path.of("datasets", "small-dataset");
        Path outputCsv =
                Path.of("SmallDataset_Schulz_Uniform_AdaptiveNayuki.csv");

        SchulzGrammar schulz = new SchulzGrammar(false);
        RNAGrammar grammar = schulz.getGrammar();

        Map<Rule, Double> probs = createUniformProbs(grammar);
        RuleProbModel model = new StaticRuleProbModel(grammar, probs);

        List<String> csv = new ArrayList<>();
        csv.add(
                "File,Length," +
                        "BD_Enc_ms,BD_Enc_Mem_bytes," +
                        "Nayuki_Enc_ms,Nayuki_Size_bytes,Nayuki_Enc_Mem_bytes"
        );

        Files.walk(datasetRoot)
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(path -> {

                    System.out.println("Processing " + path.getFileName());

                    try {
                        List<String> lines = Files.readAllLines(path);
                        if (lines.size() < 2) return;

                        RNAWithStructure rna =
                                new RNAWithStructure(
                                        lines.get(0).trim(),
                                        lines.get(1).trim()
                                );

                        List<Rule> derivation =
                                LeftmostDerivation.rules(grammar, rna);

                        long bdEncNs = 0;
                        long bdEncMem = 0;

                        for (int i = 0; i < RUNS; i++) {
                            gcAndSleep();
                            long memBefore = usedMemoryBytes();
                            long t0 = System.nanoTime();

                            BigDecimalArithmeticCoder bdCoder =
                                    new BigDecimalArithmeticCoder();

                            for (Rule r : derivation)
                                bdCoder.encodeRule(r, model);
                            bdCoder.finish();

                            long t1 = System.nanoTime();
                            gcAndSleep();
                            long memAfter = usedMemoryBytes();

                            bdEncNs += (t1 - t0);
                            bdEncMem += (memAfter - memBefore);
                        }

                        long nyEncNs = 0;
                        long nyEncMem = 0;
                        int nySizeBytes = 0;

                        for (int i = 0; i < RUNS; i++) {
                            gcAndSleep();
                            long memBefore = usedMemoryBytes();
                            long t0 = System.nanoTime();

                            ByteArrayOutputStream baos =
                                    new ByteArrayOutputStream();
                            BitOutputStream bitOut =
                                    new BitOutputStream(baos);

                            ArithmeticEncoderNayuki encoder =
                                    new ArithmeticEncoderNayuki(32, bitOut);

                            AdaptiveNayukiRuleCoder coder =
                                    new AdaptiveNayukiRuleCoder(grammar, encoder);

                            for (Rule r : derivation)
                                coder.encodeRule(r);
                            coder.finish();
                            bitOut.close();

                            long t1 = System.nanoTime();
                            gcAndSleep();
                            long memAfter = usedMemoryBytes();

                            nyEncNs += (t1 - t0);
                            nyEncMem += (memAfter - memBefore);
                            nySizeBytes += baos.toByteArray().length;
                        }

                        csv.add(String.join(",",
                                datasetRoot.relativize(path).toString(),
                                String.valueOf(rna.getNumberOfBases()),
                                String.valueOf(bdEncNs / 1e6 / RUNS),
                                String.valueOf(bdEncMem / RUNS),
                                String.valueOf(nyEncNs / 1e6 / RUNS),
                                String.valueOf(nySizeBytes / RUNS),
                                String.valueOf(nyEncMem / RUNS)
                        ));

                    } catch (Exception e) {
                        System.err.println("FAILED: " + e.getMessage());
                    }
                });

        Files.write(outputCsv, csv);
        System.out.println("\nDONE → " + outputCsv.toAbsolutePath());
    }

    private static Map<Rule, Double> createUniformProbs(RNAGrammar grammar) {
        Map<Rule, Double> probs = new HashMap<>();
        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            double p = 1.0 / rules.size();
            for (Rule r : rules)
                probs.put(r, p);
        }
        return probs;
    }
}
