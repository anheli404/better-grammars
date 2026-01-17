package compression.benchmark;

import compression.GenericRNAEncoder;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.RNAGrammar;
import compression.samplegrammars.LiuGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point for benchmarking ONE RNA file.
 * Measures runtime, memory usage, and compressed size
 * for BigDecimal vs Nayuki arithmetic coding.
 */
public final class RunSingleFileBenchmarkLiuSkewed {

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

        String primary = lines.get(0).trim();
        String secondary = lines.get(1).trim();
        RNAWithStructure rna = new RNAWithStructure(primary, secondary);

        // ===== GRAMMAR =====
        LiuGrammar liu = new LiuGrammar(false);
        RNAGrammar grammar = liu.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();

        // ===== PROBABILITY MODEL =====
        RuleProbModel model = new SkewedRuleProbModel(grammar, 0.5);

        /* =========================================================
           BIG DECIMAL BENCHMARK
         ========================================================= */
        GenericRNAEncoder bigEnc =
                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

        for (int i = 0; i < warmup; i++) {
            String enc = bigEnc.encodeRNA(rna);
            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
        }

        long bdEncNs = 0, bdDecNs = 0;
        long bdEncMem = 0, bdDecMem = 0;
        int bdSizeBits = 0;

        for (int i = 0; i < runs; i++) {

            gcAndSleep();
            long memBeforeEnc = usedMemoryBytes();
            long t0 = System.nanoTime();
            String enc = bigEnc.encodeRNA(rna);
            long t1 = System.nanoTime();
            gcAndSleep();
            long memAfterEnc = usedMemoryBytes();

            gcAndSleep();
            long memBeforeDec = usedMemoryBytes();
            long t2 = System.nanoTime();
            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
            long t3 = System.nanoTime();
            gcAndSleep();
            long memAfterDec = usedMemoryBytes();

            bdEncNs += (t1 - t0);
            bdDecNs += (t3 - t2);
            bdEncMem += (memAfterEnc - memBeforeEnc);
            bdDecMem += (memAfterDec - memBeforeDec);
            bdSizeBits += enc.length();
        }

        double bdEncMs = bdEncNs / 1e6 / runs;
        double bdDecMs = bdDecNs / 1e6 / runs;
        double bdEncMemAvg = (double) bdEncMem / runs;
        double bdDecMemAvg = (double) bdDecMem / runs;
        double bdSizeAvg = (double) bdSizeBits / runs;

        /* =========================================================
           NAYUKI BENCHMARK
         ========================================================= */
        GenericRNAEncoder nayukiEnc =
                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

        for (int i = 0; i < warmup; i++) {
            byte[] enc = NayukiEncodeRunner.encode(nayukiEnc, rna, model);
            NayukiDecodeRunner.decode(enc, model, startSymbol);
        }

        long nyEncNs = 0, nyDecNs = 0;
        long nyEncMem = 0, nyDecMem = 0;
        int nySizeBytes = 0;

        for (int i = 0; i < runs; i++) {

            gcAndSleep();
            long memBeforeEnc = usedMemoryBytes();
            long t0 = System.nanoTime();
            byte[] enc = NayukiEncodeRunner.encode(nayukiEnc, rna, model);
            long t1 = System.nanoTime();
            gcAndSleep();
            long memAfterEnc = usedMemoryBytes();

            gcAndSleep();
            long memBeforeDec = usedMemoryBytes();
            long t2 = System.nanoTime();
            NayukiDecodeRunner.decode(enc, model, startSymbol);
            long t3 = System.nanoTime();
            gcAndSleep();
            long memAfterDec = usedMemoryBytes();

            nyEncNs += (t1 - t0);
            nyDecNs += (t3 - t2);
            nyEncMem += (memAfterEnc - memBeforeEnc);
            nyDecMem += (memAfterDec - memBeforeDec);
            nySizeBytes += enc.length;
        }

        double nyEncMs = nyEncNs / 1e6 / runs;
        double nyDecMs = nyDecNs / 1e6 / runs;
        double nyEncMemAvg = (double) nyEncMem / runs;
        double nyDecMemAvg = (double) nyDecMem / runs;
        double nySizeAvg = (double) nySizeBytes / runs;

        /* ======================= WRITE CSV ======================= */
        String csv =
                "File,Length," +
                        "BD_Enc_ms,BD_Dec_ms,BD_Size_bits,BD_Enc_Mem_bytes,BD_Observed_Dec_Heap_Growth," +
                        "Nayuki_Enc_ms,Nayuki_Dec_ms,Nayuki_Size_bytes,Nayuki_Enc_Mem_bytes,Nayuki_Observed_Dec_Heap_Growth\n" +
                        inputFile.getFileName() + "," +
                        primary.length() + "," +
                        bdEncMs + "," +
                        bdDecMs + "," +
                        bdSizeAvg + "," +
                        bdEncMemAvg + "," +
                        bdDecMemAvg + "," +
                        nyEncMs + "," +
                        nyDecMs + "," +
                        nySizeAvg + "," +
                        nyEncMemAvg + "," +
                        nyDecMemAvg;

        Files.writeString(Path.of("SingleFileBenchmarkLiu_Skew50.csv"), csv);

        System.out.println("Benchmark completed:");
        System.out.println(csv);
    }
}
