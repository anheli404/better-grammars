package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.IOException;
import java.util.List;

public final class NayukiArithmeticDecoderAdapter {

    private static final int STATE_BITS = 32;
    private final ArithmeticDecoderNayuki decoder;

    public NayukiArithmeticDecoderAdapter(BitInputStream in) throws IOException {
        this.decoder = new ArithmeticDecoderNayuki(STATE_BITS, in);
    }

    /**
     * Decodes the next grammar choice.
     */
    public Interval decodeNext(List<Interval> options) throws IOException {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list must not be empty");
        }

        int[] freqs = NayukiArithmeticEncoderAdapter.buildFrequencies(options);
        FrequencyTable table = new SimpleFrequencyTable(freqs);

        int symbol = decoder.read(table);
        return options.get(symbol);
    }
}
