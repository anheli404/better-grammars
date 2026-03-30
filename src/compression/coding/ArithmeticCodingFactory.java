package compression.coding;

import compression.coding.backend.ArithmeticCodingBackend;
import compression.coding.backend.BigDecimalBackend;
import compression.coding.backend.NayukiBackend;
/**
 * Factory class for creating arithmetic coding backends.
 * It provides a simple way to select between different implementations.
 */
public final class ArithmeticCodingFactory {
    /**
     * Enumeration of the available arithmetic coding backends.
     */
    public enum Backend {
        BIG_DECIMAL,
        NAYUKI
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ArithmeticCodingFactory() {
    }

    /**
     * Creates an arithmetic coding backend based on the selected type.
     *
     * @param backend the backend implementation to create
     * @return the corresponding ArithmeticCodingBackend instance
     * @throws AssertionError if the backend type is not supported
     */
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
