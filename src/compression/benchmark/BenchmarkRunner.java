package compression.benchmark;

import compression.GenericRNAEncoder;
import compression.GenericRNADecoder;
import compression.grammar.RNAWithStructure;
import compression.grammar.NonTerminal;
import compression.samplegrammars.model.RuleProbModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BenchmarkRunner {

    public static void benchmarkNayuki(
            GenericRNAEncoder encoder,
            RuleProbModel model,
            NonTerminal startSymbol,
            RNAWithStructure rna
    ) throws Exception {

        int warmup = 5;
        int runs = 20;

        // warm-up
        for (int i = 0; i < warmup; i++) {
            byte[] enc = NayukiEncodeRunner.encode(encoder, rna, model);
            NayukiDecodeRunner.decode(enc, model, startSymbol);
        }

        long totalEncNs = 0;
        long totalDecNs = 0;
        long totalSizeBytes = 0;

        for (int i = 0; i < runs; i++) {

            long t0 = System.nanoTime();
            byte[] enc = NayukiEncodeRunner.encode(encoder, rna, model);
            long t1 = System.nanoTime();

            long t2 = System.nanoTime();
            RNAWithStructure dec = NayukiDecodeRunner.decode(enc, model, startSymbol);
            long t3 = System.nanoTime();

            if (!rna.equals(dec))
                throw new AssertionError("Decoded RNA != original");

            totalEncNs += (t1 - t0);
            totalDecNs += (t3 - t2);
            totalSizeBytes += enc.length;
        }

        double avgEncMs = totalEncNs / 1e6 / runs;
        double avgDecMs = totalDecNs / 1e6 / runs;
        double avgSizeB = (double) totalSizeBytes / runs;

        System.out.println("=== NAYUKI RESULTS ===");
        System.out.println("Avg encode (ms): " + avgEncMs);
        System.out.println("Avg decode (ms): " + avgDecMs);
        System.out.println("Avg size (bytes): " + avgSizeB);

        // CSV  cho Nayuki
        String csv = "AvgEncodeMs,AvgDecodeMs,AvgSizeBytes\n"
                + avgEncMs + "," + avgDecMs + "," + avgSizeB;
        Files.writeString(Path.of("NayukiBenchmark.csv"), csv);
        System.out.println("Nayuki results written to NayukiBenchmark.csv");
    }

    public static void benchmarkBigDecimal(
            GenericRNAEncoder encoder,
            RuleProbModel model,
            NonTerminal startSymbol,
            RNAWithStructure rna
    ) throws IOException {

        int warmup = 5;
        int runs = 20;

        // warm-up
        for (int i = 0; i < warmup; i++) {
            String enc = encoder.encodeRNA(rna);
            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
        }

        long totalEncNs = 0;
        long totalDecNs = 0;
        long totalSizeBits = 0;

        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            String enc = encoder.encodeRNA(rna);
            long t1 = System.nanoTime();

            long t2 = System.nanoTime();
            RNAWithStructure dec = BigDecimalDecodeRunner.decode(enc, model, startSymbol);
            long t3 = System.nanoTime();

            if (!rna.equals(dec)) {
                throw new AssertionError("Decoded RNA != original");
            }

            totalEncNs += (t1 - t0);
            totalDecNs += (t3 - t2);
            totalSizeBits += enc.length();
        }

        double avgEncMs = totalEncNs / 1e6 / runs;
        double avgDecMs = totalDecNs / 1e6 / runs;
        double avgSizeBits = (double) totalSizeBits / runs;

        System.out.println("=== BIG DECIMAL RESULTS ===");
        System.out.println("Avg encode (ms): " + avgEncMs);
        System.out.println("Avg decode (ms): " + avgDecMs);
        System.out.println("Avg size (bits): " + avgSizeBits);

        // CSV để vẽ biểu đồ
        String csv = "AvgEncodeMs,AvgDecodeMs,AvgSizeBits\n"
                + avgEncMs + "," + avgDecMs + "," + avgSizeBits;
        Files.writeString(Path.of("BigDecimalBenchmark.csv"), csv);
        System.out.println("BigDecimal results written to BigDecimalBenchmark.csv");
    }

    private BenchmarkRunner() {}
}
