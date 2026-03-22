package benchmark;

import java.util.List;

public class BenchmarkResult {
    public final double bitsPerBase;
    public final double avgEncodeTimeMs;
    public final double avgDecodeTimeMs;
    public final double avgEncodeHeapGrowthBytes;
    public final double avgDecodeHeapGrowthBytes;

    // Raw data per RNA per round
    public final List<double[]> rawData; // [bitsPerBase, encodeTimeMs, decodeTimeMs, encodeHeapBytes, decodeHeapBytes]

    public BenchmarkResult(double bitsPerBase, double avgEncodeTimeMs,
                           double avgDecodeTimeMs, double avgEncodeHeapGrowthBytes,
                           double avgDecodeHeapGrowthBytes, List<double[]> rawData) {
        this.bitsPerBase = bitsPerBase;
        this.avgEncodeTimeMs = avgEncodeTimeMs;
        this.avgDecodeTimeMs = avgDecodeTimeMs;
        this.avgEncodeHeapGrowthBytes = avgEncodeHeapGrowthBytes;
        this.avgDecodeHeapGrowthBytes = avgDecodeHeapGrowthBytes;
        this.rawData = rawData;
    }
}