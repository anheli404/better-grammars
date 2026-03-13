package compression.coding;

import compression.coding.backend.ArithmeticCodingBackend;
import compression.coding.backend.BigDecimalBackend;
import compression.coding.backend.NayukiBackend;

public final class ArithmeticCodingFactory {

    public enum Backend {
        BIG_DECIMAL,
        NAYUKI
    }

    private ArithmeticCodingFactory() {
    }

    public static ArithmeticCodingBackend create(Backend backend) {
        switch (backend) {
            case BIG_DECIMAL:
                return new BigDecimalBackend();
            case NAYUKI:
                return new NayukiBackend();
            default:
                throw new AssertionError("Unsupported arithmetic coding backend: " + backend);
        }
    }
}
