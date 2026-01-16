package compression.arithmaticCoding.nayukiAc;

import compression.arithmaticCoding.bigDecimalAc.Interval;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class NayukiArithmeticEncoderAdapter {

    private static final int STATE_BITS = 32;
    private static final int TOTAL_SCALE = 1 << 18;

    private final ArithmeticEncoderNayuki encoder;

    public NayukiArithmeticEncoderAdapter(BitOutputStream out) {
        this.encoder = new ArithmeticEncoderNayuki(STATE_BITS, out);
    }

    public void encodeNext(List<Interval> options, Interval chosen) throws IOException {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Options list must not be empty");
        }

        int symbol = indexOf(options, chosen);
        if (symbol < 0) {
            throw new IllegalArgumentException("Chosen interval not found in options");
        }

        int[] freqs = buildFrequencies(options);
        FrequencyTable table = new SimpleFrequencyTable(freqs);

        encoder.write(table, symbol);
    }

    public void finish() throws IOException {
        encoder.finish();
    }

    /**
     * Chuyển danh sách interval thành bảng tần suất int >= 1.
     * Áp dụng *giống hệt* bên decoder.
     */
    static int[] buildFrequencies(List<Interval> options) {
        int n = options.size();
        int[] freqs = new int[n];

        // Tổng độ dài, để chuẩn hóa thành xác suất
        BigDecimal totalLength = BigDecimal.ZERO;
        for (Interval it : options) {
            totalLength = totalLength.add(it.getLength());
        }

        if (totalLength.signum() <= 0) {
            throw new IllegalStateException("Total interval length must be positive");
        }

        int sum = 0;
        for (int i = 0; i < n; i++) {
            BigDecimal len = options.get(i).getLength();

            int w;
            if (len.signum() <= 0) {
                // nếu vì lý do nào đó length <= 0 → vẫn cho tần suất tối thiểu
                w = 1;
            } else {
                // w ≈ len / totalLength * TOTAL_SCALE, làm tròn HALF_UP
                BigDecimal scaled =
                        len.multiply(BigDecimal.valueOf(TOTAL_SCALE))
                                .divide(totalLength, 0, RoundingMode.HALF_UP);
                w = scaled.intValue();
                if (w <= 0) {
                    w = 1;
                }
            }
            freqs[i] = w;
            sum += w;
        }

        // Nếu (cực kì hiếm) tổng <= 0 thì fallback uniform
        if (sum <= 0) {
            for (int i = 0; i < n; i++) {
                freqs[i] = 1;
            }
        }

        return freqs;
    }

    private static int indexOf(List<Interval> options, Interval chosen) {
        for (int i = 0; i < options.size(); i++) {
            Interval it = options.get(i);
            if (it.getLowerBound().compareTo(chosen.getLowerBound()) == 0 &&
                    it.getUpperBound().compareTo(chosen.getUpperBound()) == 0) {
                return i;
            }
        }
        return -1;
    }
}
