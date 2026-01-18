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
 * BigDecimal AC  vs  Adaptive Nayuki AC
 * Output format mirrors legacy benchmark.
 */
public final class RunSingleFileBenchmarkLiuGrammar_Uniform_BigDecimal_vs_AdaptiveNayuki {

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

        // ===== CONFIG =====
        Path inputFile = Path.of("datasets/small-dataset/165_120_c.txt");
        int warmup = 5;
        int runs = 20;

        // ===== READ FILE =====
        List<String> lines = Files.readAllLines(inputFile);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("Input file must contain primary + secondary structure");
        }

        RNAWithStructure rna =
                new RNAWithStructure(lines.get(0).trim(), lines.get(1).trim());

        // ===== GRAMMAR =====
        LiuGrammar liu = new LiuGrammar(false);
        RNAGrammar grammar = liu.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();

        // ===== STATIC UNIFORM MODEL (for BigDecimal + parser) =====
        RuleProbModel model =
                new StaticRuleProbModel(grammar, createUniformProbs(grammar));

        SRFParser<PairOfChar> parser =
                new SRFParser<>(grammar, RuleProbModel.DONT_CARE);

        /* =========================================================
           BIG DECIMAL BENCHMARK
         ========================================================= */

        GenericRNAEncoder bdEnc =
                new GenericRNAEncoder(model, new ExactArithmeticEncoder(),
                        grammar, startSymbol);

        for (int i = 0; i < warmup; i++)
            bdEnc.encodeRNA(rna);

        long bdEncNs = 0;
        long bdEncMem = 0;
        int bdBits = 0;

        for (int i = 0; i < runs; i++) {
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

        for (int i = 0; i < warmup; i++)
            encodeAdaptive(parser, grammar, rna);

        long nyEncNs = 0;
        long nyEncMem = 0;
        int nyBytes = 0;

        for (int i = 0; i < runs; i++) {
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
                        (bdEncNs / 1e6 / runs) + "," +
                        ((double) bdBits / runs) + "," +
                        ((double) bdEncMem / runs) + "," +
                        (nyEncNs / 1e6 / runs) + "," +
                        ((double) nyBytes / runs) + "," +
                        ((double) nyEncMem / runs);

        Files.writeString(Path.of("SingleFileBenchmarkLiu_BD_vs_AdaptiveNayuki.csv"), csv);

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
