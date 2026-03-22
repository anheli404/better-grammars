package benchmark;

import compression.GenericRNADecoderNayuki;
import compression.GenericRNAEncoderNayuki;
import compression.RuleProbType;
import compression.coding.nayuki.BitInputStream;
import compression.coding.nayuki.BitOutputStream;
import compression.coding.nayuki.NayukiDecoder;
import compression.coding.nayuki.NayukiEncoder;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.model.bigdecimal.AdaptiveRuleProbModel;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.bigdecimal.SemiAdaptiveRuleProbModel;
import compression.samplegrammars.model.bigdecimal.StaticRuleProbModel;
import compression.samplegrammars.model.nayuki.AdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import compression.samplegrammars.model.nayuki.SemiAdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.StaticRuleSymbolModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NayukiBenchmarkRunner {

    private static final int NAYUKI_NUM_BITS = 32;

    private final RNAGrammar grammar;
    private final RuleProbType model;
    private final Map<Rule, Double> staticProbs;
    private final Map<Rule, Long> staticCounts;

    public NayukiBenchmarkRunner(RNAGrammar grammar, RuleProbType model,
                                 Map<Rule, Double> staticProbs, Map<Rule, Long> staticCounts) {
        this.grammar = grammar;
        this.model = model;
        this.staticProbs = staticProbs;
        this.staticCounts = staticCounts;
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

            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            BitOutputStream bitOut = new BitOutputStream(byteOut);
            NayukiEncoder enc = new NayukiEncoder(NAYUKI_NUM_BITS, bitOut);
            RuleProbModel probModel = buildProbModel(rna);
            RuleSymbolModel symbolModel = buildSymbolModel(rna);

            long encMemBefore = runtime.totalMemory() - runtime.freeMemory();
            long t0 = System.nanoTime();
            new GenericRNAEncoderNayuki(probModel, symbolModel, enc, byteOut, bitOut, grammar, grammar.getStartSymbol())
                    .encodeRNANayuki(rna);
            long t1 = System.nanoTime();
            long encMemAfter = runtime.totalMemory() - runtime.freeMemory();

            byte[] encoded = byteOut.toByteArray();

            RuleSymbolModel symbolModelForDecode = buildSymbolModel(rna);
            ByteArrayInputStream byteIn = new ByteArrayInputStream(encoded);
            BitInputStream bitIn = new BitInputStream(byteIn);
            NayukiDecoder dec;
            try {
                dec = new NayukiDecoder(NAYUKI_NUM_BITS, bitIn);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            long decMemBefore = runtime.totalMemory() - runtime.freeMemory();
            long t2 = System.nanoTime();
            new GenericRNADecoderNayuki(symbolModelForDecode, dec, grammar.getStartSymbol()).decode();
            long t3 = System.nanoTime();
            long decMemAfter = runtime.totalMemory() - runtime.freeMemory();

            return new long[]{
                    t1 - t0,
                    t3 - t2,
                    encoded.length * 8L,
                    encMemAfter - encMemBefore,
                    decMemAfter - decMemBefore
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    private RuleProbModel buildProbModel(RNAWithStructure rna) {
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

    private RuleSymbolModel buildSymbolModel(RNAWithStructure rna) {
        switch (model) {
            case STATIC:
            case STATIC_FROM_FILE:
                return new StaticRuleSymbolModel(grammar.getGrammar(), staticCounts);
            case SEMI_ADAPTIVE:
                return new SemiAdaptiveRuleSymbolModel(grammar, rna);
            case ADAPTIVE:
                return new AdaptiveRuleSymbolModel(grammar.getGrammar());
            default:
                throw new AssertionError("Unknown model: " + model);
        }
    }
}