package compression.arithmaticCoding.benchmark.benchmark_new;

import compression.GenericRNAEncoder;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;
import compression.arithmaticCoding.core.nayuki.AdaptiveNayukiRuleCoder;
import compression.arithmaticCoding.nayukiAc.ArithmeticEncoderNayuki;
import compression.arithmaticCoding.nayukiAc.BitOutputStream;
import compression.grammar.*;
import compression.parser.SRFParser;
import compression.samplegrammars.LeftmostDerivation;
import compression.samplegrammars.LiuGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.samplegrammars.model.StaticRuleProbModel;

import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

/**
 * Dataset benchmark:
 * BigDecimal AC  vs  Adaptive Nayuki AC
 * One CSV row per RNA file (same format as legacy benchmark).
 */
public final class RunSmallDatasetBenchmarkLiuGrammar_Uniform_BigDecimal_vs_AdaptiveNayuki {

    private static final int WARMUP = 3;
    private static final int RUNS = 10;

    /* ===================== MEMORY HELPERS ===================== */

    private static long usedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void gcAndSleep() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {}
    }

    /* =========================== MAIN ========================== */

    public static void main(String[] args) throws Exception {

        Path datasetRoot = Path.of("datasets", "small-dataset");
        Path outputCsv = Path.of("SmallDatasetBenchmarkLiu_BD_vs_AdaptiveNayuki.csv");

        // ===== Grammar & model =====
        LiuGrammar liu = new LiuGrammar(false);
        RNAGrammar grammar = liu.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();

        RuleProbModel model =
                new StaticRuleProbModel(grammar, createUniformProbs(grammar));

        SRFParser<PairOfChar> parser =
                new SRFParser<>(grammar, RuleProbModel.DONT_CARE);

        List<String> csv = new ArrayList<>();
        csv.add(
                "File,Length," +
                        "BD_Enc_ms,BD_Dec_ms,BD_Size_bits,BD_Enc_Mem_bytes,BD_Observed_Dec_Heap_Growth," +
                        "AdaptiveNayuki_Enc_ms,AdaptiveNayuki_Size_bytes,AdaptiveNayuki_Enc_Mem_bytes"
        );

        Files.walk(datasetRoot)
                .filter(p -> p.toString().endsWith(".txt"))
                .sorted()
                .forEach(path -> {

                    try {
                        List<String> lines = Files.readAllLines(path);
                        if (lines.size() < 2) return;

                        RNAWithStructure rna =
                                new RNAWithStructure(lines.get(0).trim(), lines.get(1).trim());

                        /* ================= BigDecimal ================= */

                        GenericRNAEncoder bdEnc =
                                new GenericRNAEncoder(model, new ExactArithmeticEncoder(),
                                        grammar, startSymbol);

                        for (int i = 0; i < WARMUP; i++)
                            bdEnc.encodeRNA(rna);

                        long bdEncNs = 0, bdEncMem = 0;
                        int bdBits = 0;

                        for (int i = 0; i < RUNS; i++) {
                            gcAndSleep();
                            long m0 = usedMemoryBytes();
                            long t0 = System.nanoTime();

                            String enc = bdEnc.encodeRNA(rna);

                            long t1 = System.nanoTime();
                            gcAndSleep();
                            long m1 = usedMemoryBytes();

                            bdEncNs += (t1 - t0);
                            bdEncMem += (m1 - m0);
                            bdBits += enc.length();
                        }

                        /* ================= Adaptive Nayuki ================= */

                        for (int i = 0; i < WARMUP; i++)
                            encodeAdaptive(parser, grammar, rna);

                        long nyEncNs = 0, nyEncMem = 0;
                        int nyBytes = 0;

                        for (int i = 0; i < RUNS; i++) {
                            gcAndSleep();
                            long m0 = usedMemoryBytes();
                            long t0 = System.nanoTime();

                            byte[] enc = encodeAdaptive(parser, grammar, rna);

                            long t1 = System.nanoTime();
                            gcAndSleep();
                            long m1 = usedMemoryBytes();

                            nyEncNs += (t1 - t0);
                            nyEncMem += (m1 - m0);
                            nyBytes += enc.length;
                        }

                        csv.add(String.join(",",
                                datasetRoot.relativize(path).toString(),
                                String.valueOf(rna.getNumberOfBases()),
                                String.valueOf(bdEncNs / 1e6 / RUNS),
                                "0", // decode omitted (same as your new runs)
                                String.valueOf((double) bdBits / RUNS),
                                String.valueOf((double) bdEncMem / RUNS),
                                "0",
                                String.valueOf(nyEncNs / 1e6 / RUNS),
                                String.valueOf((double) nyBytes / RUNS),
                                String.valueOf((double) nyEncMem / RUNS)
                        ));

                    } catch (Exception e) {
                        System.err.println("Failed: " + path);
                    }
                });

        Files.write(outputCsv, csv);
        System.out.println("DONE → " + outputCsv.toAbsolutePath());
    }

    /* ===================== ADAPTIVE NAYUKI ===================== */

    private static byte[] encodeAdaptive(
            SRFParser<PairOfChar> parser,
            RNAGrammar grammar,
            RNAWithStructure rna
    ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitOutputStream bitOut = new BitOutputStream(baos);
        ArithmeticEncoderNayuki encoder =
                new ArithmeticEncoderNayuki(32, bitOut);

        AdaptiveNayukiRuleCoder coder =
                new AdaptiveNayukiRuleCoder(grammar, encoder);

        for (Rule r : LeftmostDerivation.rules(parser, rna)) {
            coder.encodeRule(r);
        }

        coder.finish();
        bitOut.close();
        return baos.toByteArray();
    }

    /* ===================== PROBS ===================== */

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
