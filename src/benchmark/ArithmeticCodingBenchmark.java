package benchmark;

import compression.RuleProbType;
import compression.data.CachedDataset;
import compression.data.Dataset;
import compression.data.FolderBasedDataset;
import compression.data.TrainingDataset;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.SampleGrammar;
import compression.util.AllGrammars;
import compression.util.CSVFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArithmeticCodingBenchmark {

    private static final int WARMUP_ROUNDS = 2;
    private static final int TIMED_ROUNDS  = 5;

    private static final String DATASET_PATH = "dowell-benchmark-10-percent";
    private static final String TRAINING_DATASET_PATH = "TestTrainingData";
    private static final boolean WITH_NCR = true;

    public static void main(String[] args) throws Exception {
        Dataset dataset = new CachedDataset(new FolderBasedDataset(DATASET_PATH));
        TrainingDataset trainingDataset = new TrainingDataset(TRAINING_DATASET_PATH);

        List<RNAWithStructure> rnas = new ArrayList<>();
        for (RNAWithStructure rna : dataset) rnas.add(rna);

        // Avg CSV
        CSVFile csvAvg = new CSVFile(
                new File("benchmark-results-avg.csv"),
                "grammar", "model", "backend", "bits_per_base",
                "avg_encode_time_ms", "avg_decode_time_ms",
                "avg_encode_heap_bytes", "avg_decode_heap_bytes"
        );

        // Raw CSV
        CSVFile csvRaw = new CSVFile(
                new File("benchmark-results-raw.csv"),
                "grammar", "model", "backend", "round", "rna_index",
                "bits_per_base", "encode_time_ms", "decode_time_ms",
                "encode_heap_bytes", "decode_heap_bytes"
        );

        for (Map.Entry<String, SampleGrammar> entry : AllGrammars.allGrammars(WITH_NCR).entrySet()) {
            String grammarName = entry.getKey();
            SampleGrammar sg   = entry.getValue();
            RNAGrammar G       = sg.getGrammar();

            System.out.println("Running grammar: " + grammarName);

            Map<Rule, Long>   staticCounts;
            Map<Rule, Double> staticProbs;
            try {
                staticCounts = G.computeRuleCounts(new CachedDataset(trainingDataset));
                staticProbs  = G.computeRulesToProbs(staticCounts);
            } catch (RuntimeException e) {
                System.out.println("  Skipping static probs for " + grammarName + ": " + e.getMessage());
                staticCounts = null;
                staticProbs  = null;
            }

            for (RuleProbType model : new RuleProbType[]{
                    RuleProbType.STATIC,
                    RuleProbType.SEMI_ADAPTIVE,
                    RuleProbType.ADAPTIVE}) {

                System.out.println("  Model: " + model);

                // BigDecimal
                System.out.println("    Running BigDecimal...");
                BenchmarkResult bdResult = new BigDecimalBenchmarkRunner(G, model, staticProbs)
                        .run(rnas, WARMUP_ROUNDS, TIMED_ROUNDS);
                System.out.println("    BigDecimal done");

                csvAvg.appendRow(
                        grammarName, model.toString(), "BigDecimal",
                        bdResult != null ? String.format("%.4f", bdResult.bitsPerBase)              : "N/A",
                        bdResult != null ? String.format("%.3f", bdResult.avgEncodeTimeMs)          : "N/A",
                        bdResult != null ? String.format("%.3f", bdResult.avgDecodeTimeMs)          : "N/A",
                        bdResult != null ? String.format("%.0f", bdResult.avgEncodeHeapGrowthBytes) : "N/A",
                        bdResult != null ? String.format("%.0f", bdResult.avgDecodeHeapGrowthBytes) : "N/A"
                );

                if (bdResult != null) {
                    int rnaIndex = 0;
                    for (double[] row : bdResult.rawData) {
                        int round = (rnaIndex / rnas.size()) + 1;
                        int rnaIdx = (rnaIndex % rnas.size()) + 1;
                        csvRaw.appendRow(
                                grammarName, model.toString(), "BigDecimal",
                                String.valueOf(round), String.valueOf(rnaIdx),
                                String.format("%.4f", row[0]),
                                String.format("%.3f", row[1]),
                                String.format("%.3f", row[2]),
                                String.format("%.0f", row[3]),
                                String.format("%.0f", row[4])
                        );
                        rnaIndex++;
                    }
                }

                // Nayuki
                System.out.println("    Running Nayuki...");
                BenchmarkResult nayukiResult = new NayukiBenchmarkRunner(G, model, staticProbs, staticCounts)
                        .run(rnas, WARMUP_ROUNDS, TIMED_ROUNDS);
                System.out.println("    Nayuki done");

                csvAvg.appendRow(
                        grammarName, model.toString(), "Nayuki",
                        nayukiResult != null ? String.format("%.4f", nayukiResult.bitsPerBase)              : "N/A",
                        nayukiResult != null ? String.format("%.3f", nayukiResult.avgEncodeTimeMs)          : "N/A",
                        nayukiResult != null ? String.format("%.3f", nayukiResult.avgDecodeTimeMs)          : "N/A",
                        nayukiResult != null ? String.format("%.0f", nayukiResult.avgEncodeHeapGrowthBytes) : "N/A",
                        nayukiResult != null ? String.format("%.0f", nayukiResult.avgDecodeHeapGrowthBytes) : "N/A"
                );

                if (nayukiResult != null) {
                    int rnaIndex = 0;
                    for (double[] row : nayukiResult.rawData) {
                        int round = (rnaIndex / rnas.size()) + 1;
                        int rnaIdx = (rnaIndex % rnas.size()) + 1;
                        csvRaw.appendRow(
                                grammarName, model.toString(), "Nayuki",
                                String.valueOf(round), String.valueOf(rnaIdx),
                                String.format("%.4f", row[0]),
                                String.format("%.3f", row[1]),
                                String.format("%.3f", row[2]),
                                String.format("%.0f", row[3]),
                                String.format("%.0f", row[4])
                        );
                        rnaIndex++;
                    }
                }

                System.out.println("  " + model + " done");
            }
        }

        csvAvg.close();
        csvRaw.close();
        System.out.println("Done. Results written to benchmark-results-avg.csv and benchmark-results-raw.csv");
    }
}