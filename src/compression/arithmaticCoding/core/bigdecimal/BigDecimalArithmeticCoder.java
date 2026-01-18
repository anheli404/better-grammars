package compression.arithmaticCoding.core.bigdecimal;

import compression.arithmaticCoding.core.ArithmeticCoder;
import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticEncoder;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.grammar.Rule;
import compression.samplegrammars.model.RuleProbModel;

/**
 * BigDecimal-based arithmetic coder.
 *
 * This class wraps the existing ExactArithmeticEncoder and preserves
 * the original BigDecimal arithmetic coding behavior.
 */
public final class BigDecimalArithmeticCoder implements ArithmeticCoder {

    private final ExactArithmeticEncoder encoder;

    public BigDecimalArithmeticCoder() {
        this.encoder = new ExactArithmeticEncoder();
    }

    @Override
    public void encodeRule(Rule rule, RuleProbModel model) {
        // BigDecimal arithmetic coding:
        // 1) Query the model for the rule interval (BigDecimal-based)
        // 2) Zoom into that interval using the exact arithmetic encoder
        Interval interval = model.getIntervalFor(rule);
        encoder.encodeNext(interval);
    }

    @Override
    public void finish() {
        // No-op: ExactArithmeticEncoder finalizes on getFinalEncoding()
    }

    /**
     * Expose final encoding for benchmarking / output.
     */
    public String getFinalEncoding() {
        return encoder.getFinalEncoding();
    }

    /**
     * Expose final precision (in bits).
     */
    public int getFinalPrecision() {
        return encoder.getFinalPrecision();
    }
}
