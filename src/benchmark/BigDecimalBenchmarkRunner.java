package benchmark;

import compression.GenericRNADecoder;
import compression.GenericRNAEncoderForPrecision;
import compression.RuleProbType;
import compression.coding.bigdecimal.ExactArithmeticDecoder;
import compression.coding.bigdecimal.ExactArithmeticEncoder;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.model.bigdecimal.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BigDecimalBenchmarkRunner {

    private final RNAGrammar grammar;
    private final RuleProbType model;
    private final Map<Rule, Double> staticProbs;

    public BigDecimalBenchmarkRunner(RNAGrammar grammar, RuleProbType model, Map<Rule, Double> staticProbs) {
        this.grammar = grammar;
        this.model = model;
        this.staticProbs = staticProbs;
    }

    public BenchmarkResult run(List<RNAWithStructure> rnas, int warmupRounds, int timedRounds) {
        for (int r = 0; r < warmupRounds; r++)
            for (RNAWithStructure rna : rnas)
                encodeAndDecode(rna);

        long totalEncodeNs         = 0;
        long totalDecodeNs         = 0;
        long totalBits             = 0;
        long totalBases            = 0;
        long totalEncodeHeapGrowth = 0;
        long totalDecodeHeapGrowth = 0;
        int  count                 = 0;
        List<double[]> rawData     = new ArrayList<>();

        for (int r = 0; r < timedRounds; r++) {
            for (RNAWithStructure rna : rnas) {
                System.gc();

                long[] result = encodeAndDecode(rna);
                if (result == null) continue;

                totalEncodeNs         += result[0];
                totalDecodeNs         += result[1];
                totalBits             += result[2];
                totalEncodeHeapGrowth += result[3];
                totalDecodeHeapGrowth += result[4];
                totalBases            += rna.getNumberOfBases();
                count++;

                rawData.add(new double[]{
                        (double) result[2] / rna.getNumberOfBases(), // bitsPerBase
                        result[0] / 1_000_000.0,                     // encodeTimeMs
                        result[1] / 1_000_000.0,                     // decodeTimeMs
                        result[3],                                    // encodeHeapBytes
                        result[4]                                     // decodeHeapBytes
                });
            }
        }

        if (totalBases == 0 || count == 0) return null;

        double avgBitsPerBase           = (double) totalBits / totalBases;
        double avgEncodeTimeMs          = (totalEncodeNs / 1_000_000.0) / count;
        double avgDecodeTimeMs          = (totalDecodeNs / 1_000_000.0) / count;
        double avgEncodeHeapGrowthBytes = (double) totalEncodeHeapGrowth / count;
        double avgDecodeHeapGrowthBytes = (double) totalDecodeHeapGrowth / count;

        return new BenchmarkResult(avgBitsPerBase, avgEncodeTimeMs, avgDecodeTimeMs,
                avgEncodeHeapGrowthBytes, avgDecodeHeapGrowthBytes, rawData);
    }

    private long[] encodeAndDecode(RNAWithStructure rna) {
        try {
            Runtime runtime = Runtime.getRuntime();

            RuleProbModel probModel = buildModel(rna);
            ExactArithmeticEncoder acEncoder = new ExactArithmeticEncoder();

            long encMemBefore = runtime.totalMemory() - runtime.freeMemory();
            long t0 = System.nanoTime();
            new GenericRNAEncoderForPrecision(probModel, acEncoder, grammar, grammar.getStartSymbol())
                    .getPrecisionForRNACode(rna);
            long t1 = System.nanoTime();
            long encMemAfter = runtime.totalMemory() - runtime.freeMemory();

            String encoded = acEncoder.getFinalEncoding();

            RuleProbModel probModelForDecode = buildModel(rna);
            ExactArithmeticDecoder acDecoder = new ExactArithmeticDecoder(encoded);

            long decMemBefore = runtime.totalMemory() - runtime.freeMemory();
            long t2 = System.nanoTime();
            new GenericRNADecoder(probModelForDecode, acDecoder, grammar.getStartSymbol()).decode();
            long t3 = System.nanoTime();
            long decMemAfter = runtime.totalMemory() - runtime.freeMemory();

            return new long[]{
                    t1 - t0,
                    t3 - t2,
                    encoded.length(),
                    encMemAfter - encMemBefore,
                    decMemAfter - decMemBefore
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    private RuleProbModel buildModel(RNAWithStructure rna) {
        switch (model) {
            case STATIC:
            case STATIC_FROM_FILE:
                return new StaticRuleProbModel(grammar.getGrammar(), staticProbs);
            case SEMI_ADAPTIVE:
                return new SemiAdaptiveRuleProbModel(grammar, rna);
            case ADAPTIVE:
                return new AdaptiveRuleProbModel(grammar.getGrammar());
            default:
                throw new AssertionError("Unknown model: " + model);
        }
    }
}