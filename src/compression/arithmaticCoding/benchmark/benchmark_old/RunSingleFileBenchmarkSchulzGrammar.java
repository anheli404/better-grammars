package compression.arithmaticCoding.benchmark.benchmark_old;

import compression.GenericRNAEncoder;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.RNAGrammar;
import compression.samplegrammars.SchulzGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Benchmark ONE RNA file using SchulzGrammar.
 */
public final class RunSingleFileBenchmarkSchulzGrammar {

    private static final int WARMUP = 3;
    private static final int RUNS = 10;

    public static void main(String[] args) throws Exception {

        Path inputFile = Path.of("datasets/small-dataset/165_120_c.txt");

        // ===== Read RNA file =====
        List<String> lines = Files.readAllLines(inputFile);
        if (lines.size() < 2) {
            throw new IllegalArgumentException("Invalid RNA file: " + inputFile);
        }

        String primary = lines.get(0).trim();
        String secondary = lines.get(1).trim();
        RNAWithStructure rna = new RNAWithStructure(primary, secondary);

        // ===== Grammar =====
        SchulzGrammar schulz = new SchulzGrammar(false);
        RNAGrammar grammar = schulz.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();

        // ===== Probability model =====
        RuleProbModel model = new UniformRuleProbModel(grammar);

        // ================= BIG DECIMAL =================
        GenericRNAEncoder bdEnc =
                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

        for (int i = 0; i < WARMUP; i++) {
            String enc = bdEnc.encodeRNA(rna);
            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
        }

        long bdEncNs = 0, bdDecNs = 0;
        int bdSizeBits = 0;

        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            String enc = bdEnc.encodeRNA(rna);
            long t1 = System.nanoTime();

            long t2 = System.nanoTime();
            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
            long t3 = System.nanoTime();

            bdEncNs += (t1 - t0);
            bdDecNs += (t3 - t2);
            bdSizeBits += enc.length();
        }

        double bdEncMs = bdEncNs / 1e6 / RUNS;
        double bdDecMs = bdDecNs / 1e6 / RUNS;
        double bdSizeAvg = (double) bdSizeBits / RUNS;

        // ================= NAYUKI =================
        GenericRNAEncoder nyEnc =
                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

        for (int i = 0; i < WARMUP; i++) {
            byte[] enc = NayukiEncodeRunner.encode(nyEnc, rna, model);
            NayukiDecodeRunner.decode(enc, model, startSymbol);
        }

        long nyEncNs = 0, nyDecNs = 0;
        int nySizeBytes = 0;

        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            byte[] enc = NayukiEncodeRunner.encode(nyEnc, rna, model);
            long t1 = System.nanoTime();

            long t2 = System.nanoTime();
            NayukiDecodeRunner.decode(enc, model, startSymbol);
            long t3 = System.nanoTime();

            nyEncNs += (t1 - t0);
            nyDecNs += (t3 - t2);
            nySizeBytes += enc.length;
        }

        double nyEncMs = nyEncNs / 1e6 / RUNS;
        double nyDecMs = nyDecNs / 1e6 / RUNS;
        double nySizeAvg = (double) nySizeBytes / RUNS;

        // ===== CSV =====
        String csv =
                "File,Length,BD_Enc_ms,BD_Dec_ms,BD_Size_bits,Nayuki_Enc_ms,Nayuki_Dec_ms,Nayuki_Size_bytes\n" +
                        inputFile.getFileName() + "," +
                        primary.length() + "," +
                        bdEncMs + "," +
                        bdDecMs + "," +
                        bdSizeAvg + "," +
                        nyEncMs + "," +
                        nyDecMs + "," +
                        nySizeAvg;

        Files.writeString(Path.of("SingleFileBenchmarkSchulz.csv"), csv);

        System.out.println("DONE:");
        System.out.println(csv);
    }
}

