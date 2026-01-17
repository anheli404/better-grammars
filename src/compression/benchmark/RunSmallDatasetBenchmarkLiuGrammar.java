package compression.benchmark;

import compression.GenericRNAEncoder;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.RNAGrammar;
import compression.samplegrammars.LiuGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmarks ALL .txt files in datasets/small-dataset
 * and writes ONE CSV file with one row per RNA file.
 * Measures runtime, memory usage, and compressed size.
 */
public final class RunSmallDatasetBenchmarkLiuGrammar {

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
        Path outputCsv = Path.of("SmallDatasetBenchmarkLiu.csv");

        if (!Files.isDirectory(datasetRoot)) {
            throw new IllegalArgumentException("Folder not found: " + datasetRoot);
        }

        // ===== Grammar & model (shared) =====
        LiuGrammar liu = new LiuGrammar(false);
        RNAGrammar grammar = liu.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();
        RuleProbModel model = new UniformRuleProbModel(grammar);

        // ===== CSV header =====
        List<String> lines = new ArrayList<>();
        lines.add(
                "File,Length," +
                        "BD_Enc_ms,BD_Dec_ms,BD_Size_bits,BD_Enc_Mem_bytes,BD_Observed_Dec_Heap_Growth," +
                        "Nayuki_Enc_ms,Nayuki_Dec_ms,Nayuki_Size_bytes,Nayuki_Enc_Mem_bytes,Nayuki_Observed_Dec_Heap_Growth"
        );

        // ===== Walk through dataset =====
        Files.walk(datasetRoot)
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(path -> {

                    System.out.println("Processing: " + path);

                    try {
                        List<String> content = Files.readAllLines(path);
                        if (content.size() < 2) {
                            System.err.println("  Skipped (invalid format)");
                            return;
                        }

                        String primary = content.get(0).trim();
                        String secondary = content.get(1).trim();
                        RNAWithStructure rna = new RNAWithStructure(primary, secondary);

                        /* ================= BigDecimal ================= */

                        GenericRNAEncoder bdEnc =
                                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

                        for (int i = 0; i < WARMUP; i++) {
                            String enc = bdEnc.encodeRNA(rna);
                            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
                        }

                        long bdEncNs = 0, bdDecNs = 0;
                        long bdEncMem = 0, bdDecMem = 0;
                        int bdSizeBits = 0;

                        for (int i = 0; i < RUNS; i++) {

                            gcAndSleep();
                            long memBeforeEnc = usedMemoryBytes();
                            long t0 = System.nanoTime();
                            String enc = bdEnc.encodeRNA(rna);
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

                        double bdEncMs = bdEncNs / 1e6 / RUNS;
                        double bdDecMs = bdDecNs / 1e6 / RUNS;
                        double bdEncMemAvg = (double) bdEncMem / RUNS;
                        double bdDecMemAvg = (double) bdDecMem / RUNS;
                        double bdSizeAvg = (double) bdSizeBits / RUNS;

                        /* ================= Nayuki ================= */

                        GenericRNAEncoder nyEnc =
                                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

                        for (int i = 0; i < WARMUP; i++) {
                            byte[] enc = NayukiEncodeRunner.encode(nyEnc, rna, model);
                            NayukiDecodeRunner.decode(enc, model, startSymbol);
                        }

                        long nyEncNs = 0, nyDecNs = 0;
                        long nyEncMem = 0, nyDecMem = 0;
                        int nySizeBytes = 0;

                        for (int i = 0; i < RUNS; i++) {

                            gcAndSleep();
                            long memBeforeEnc = usedMemoryBytes();
                            long t0 = System.nanoTime();
                            byte[] enc = NayukiEncodeRunner.encode(nyEnc, rna, model);
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

                        double nyEncMs = nyEncNs / 1e6 / RUNS;
                        double nyDecMs = nyDecNs / 1e6 / RUNS;
                        double nyEncMemAvg = (double) nyEncMem / RUNS;
                        double nyDecMemAvg = (double) nyDecMem / RUNS;
                        double nySizeAvg = (double) nySizeBytes / RUNS;

                        /* ================= CSV row ================= */

                        lines.add(String.join(",",
                                datasetRoot.relativize(path).toString(),
                                String.valueOf(primary.length()),
                                String.valueOf(bdEncMs),
                                String.valueOf(bdDecMs),
                                String.valueOf(bdSizeAvg),
                                String.valueOf(bdEncMemAvg),
                                String.valueOf(bdDecMemAvg),
                                String.valueOf(nyEncMs),
                                String.valueOf(nyDecMs),
                                String.valueOf(nySizeAvg),
                                String.valueOf(nyEncMemAvg),
                                String.valueOf(nyDecMemAvg)
                        ));

                    } catch (Exception e) {
                        System.err.println("  Failed: " + e.getMessage());
                    }
                });

        Files.write(outputCsv, lines);
        System.out.println("\nDONE. Results written to " + outputCsv.toAbsolutePath());
    }
}
