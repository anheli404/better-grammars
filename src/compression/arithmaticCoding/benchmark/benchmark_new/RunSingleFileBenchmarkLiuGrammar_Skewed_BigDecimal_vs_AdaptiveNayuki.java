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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Single-file benchmark:
 * Liu grammar
 * Skewed rule probabilities
 * BigDecimal AC vs Adaptive Nayuki AC
 */
public final class RunSingleFileBenchmarkLiuGrammar_Skewed_BigDecimal_vs_AdaptiveNayuki {

    /* ===================== CONFIG ===================== */

    // 🔧 CHỈ ĐỔI SỐ Ở ĐÂY
    private static final double SKEW_MAIN_PROB = 0.7;

    private static final int WARMUP = 5;
    private static final int RUNS = 20;

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

        Path inputFile = Path.of("datasets/small-dataset/165_120_c.txt");

        // ===== READ FILE =====
        List<String> lines = Files.readAllLines(inputFile);
        if (lines.size() < 2)
            throw new IllegalArgumentException("Invalid RNA file");

        RNAWithStructure rna =
                new RNAWithStructure(lines.get(0).trim(), lines.get(1).trim());

        // ===== GRAMMAR =====
        LiuGrammar liu = new LiuGrammar(false);
        RNAGrammar grammar = liu.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();

        // ===== SKEWED STATIC MODEL =====
        RuleProbModel model =
                new StaticRuleProbModel(grammar, createSkewedProbs(grammar));

        SRFParser<PairOfChar> parser =
                new SRFParser<>(grammar, RuleProbModel.DONT_CARE);

        /* =========================================================
           BIG DECIMAL BENCHMARK
         ========================================================= */

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

        /* =========================================================
           ADAPTIVE NAYUKI BENCHMARK
         ========================================================= */

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

        /* ======================= CSV ======================= */

        String csv =
                "File,Length," +
                        "BD_Enc_ms,BD_Size_bits,BD_Enc_Mem_bytes," +
                        "AdaptiveNayuki_Enc_ms,AdaptiveNayuki_Size_bytes,AdaptiveNayuki_Enc_Mem_bytes\n" +

                        inputFile.getFileName() + "," +
                        rna.getNumberOfBases() + "," +
                        (bdEncNs / 1e6 / RUNS) + "," +
                        ((double) bdBits / RUNS) + "," +
                        ((double) bdEncMem / RUNS) + "," +
                        (nyEncNs / 1e6 / RUNS) + "," +
                        ((double) nyBytes / RUNS) + "," +
                        ((double) nyEncMem / RUNS);

        Files.writeString(
                Path.of("SingleFileBenchmarkLiu_Skewed_BD_vs_AdaptiveNayuki.csv"),
                csv
        );

        System.out.println("Benchmark completed:");
        System.out.println(csv);
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

    /* ===================== SKEWED PROBS ===================== */

    private static Map<Rule, Double> createSkewedProbs(RNAGrammar grammar) {

        Map<Rule, Double> probs = new HashMap<>();

        for (NonTerminal lhs : grammar.getNonTerminals()) {
            List<Rule> rules = new ArrayList<>(grammar.getRules(lhs));
            int n = rules.size();

            if (n == 1) {
                probs.put(rules.get(0), 1.0);
                continue;
            }

            // dominant rule
            probs.put(rules.get(0), SKEW_MAIN_PROB);

            double rest = (1.0 - SKEW_MAIN_PROB) / (n - 1);
            for (int i = 1; i < n; i++) {
                probs.put(rules.get(i), rest);
            }
        }
        return probs;
    }
}
