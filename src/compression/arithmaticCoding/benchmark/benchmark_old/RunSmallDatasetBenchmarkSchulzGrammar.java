package compression.arithmaticCoding.benchmark.benchmark_old;

import compression.GenericRNAEncoder;
import compression.grammar.NonTerminal;
import compression.grammar.RNAWithStructure;
import compression.grammar.RNAGrammar;
import compression.samplegrammars.SchulzGrammar;
import compression.samplegrammars.model.RuleProbModel;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmarks ALL .txt files in datasets/small-dataset using SchulzGrammar.
 */
public final class RunSmallDatasetBenchmarkSchulzGrammar {

    private static final int WARMUP = 3;
    private static final int RUNS = 10;

    public static void main(String[] args) throws Exception {

        Path datasetRoot = Path.of("datasets", "small-dataset");
        Path outputCsv = Path.of("SmallDatasetBenchmarkSchulz.csv");

        SchulzGrammar schulz = new SchulzGrammar(false);
        RNAGrammar grammar = schulz.getGrammar();
        NonTerminal startSymbol = grammar.getStartSymbol();
        RuleProbModel model = new UniformRuleProbModel(grammar);

        List<String> csv = new ArrayList<>();
        csv.add("File,Length,BD_Enc_ms,BD_Dec_ms,BD_Size_bits,Nayuki_Enc_ms,Nayuki_Dec_ms,Nayuki_Size_bytes");

        Files.walk(datasetRoot)
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(path -> {
                    System.out.println("Processing: " + path);

                    try {
                        List<String> lines = Files.readAllLines(path);
                        if (lines.size() < 2) return;

                        RNAWithStructure rna =
                                new RNAWithStructure(lines.get(0).trim(), lines.get(1).trim());

                        // ---------- BigDecimal ----------
                        GenericRNAEncoder bdEnc =
                                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

                        for (int i = 0; i < WARMUP; i++) {
                            String enc = bdEnc.encodeRNA(rna);
                            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
                        }

                        long bdEncNs = 0, bdDecNs = 0;
                        int bdBits = 0;

                        for (int i = 0; i < RUNS; i++) {
                            long t0 = System.nanoTime();
                            String enc = bdEnc.encodeRNA(rna);
                            long t1 = System.nanoTime();

                            long t2 = System.nanoTime();
                            BigDecimalDecodeRunner.decode(enc, model, startSymbol);
                            long t3 = System.nanoTime();

                            bdEncNs += (t1 - t0);
                            bdDecNs += (t3 - t2);
                            bdBits += enc.length();
                        }

                        // ---------- Nayuki ----------
                        GenericRNAEncoder nyEnc =
                                new GenericRNAEncoder(model, new ExactArithmeticEncoder(), grammar, startSymbol);

                        for (int i = 0; i < WARMUP; i++) {
                            byte[] enc = NayukiEncodeRunner.encode(nyEnc, rna, model);
                            NayukiDecodeRunner.decode(enc, model, startSymbol);
                        }

                        long nyEncNs = 0, nyDecNs = 0;
                        int nyBytes = 0;

                        for (int i = 0; i < RUNS; i++) {
                            long t0 = System.nanoTime();
                            byte[] enc = NayukiEncodeRunner.encode(nyEnc, rna, model);
                            long t1 = System.nanoTime();

                            long t2 = System.nanoTime();
                            NayukiDecodeRunner.decode(enc, model, startSymbol);
                            long t3 = System.nanoTime();

                            nyEncNs += (t1 - t0);
                            nyDecNs += (t3 - t2);
                            nyBytes += enc.length;
                        }

                        csv.add(String.join(",",
                                datasetRoot.relativize(path).toString(),
                                String.valueOf(rna.getNumberOfBases()),
                                String.valueOf(bdEncNs / 1e6 / RUNS),
                                String.valueOf(bdDecNs / 1e6 / RUNS),
                                String.valueOf((double) bdBits / RUNS),
                                String.valueOf(nyEncNs / 1e6 / RUNS),
                                String.valueOf(nyDecNs / 1e6 / RUNS),
                                String.valueOf((double) nyBytes / RUNS)
                        ));

                    } catch (Exception e) {
                        System.err.println("  Skipped (parse failed)");
                    }
                });

        Files.write(outputCsv, csv);
        System.out.println("\nDONE → " + outputCsv.toAbsolutePath());
    }
}

