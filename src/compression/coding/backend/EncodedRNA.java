package compression.coding.backend;

import compression.coding.ArithmeticCodingFactory;

/**
 * Container class for encoded RNA data.
 * It stores the backend used for encoding and the encoded payload
 * as either a bit string or a byte array.
 */
public final class EncodedRNA {
    private final ArithmeticCodingFactory.Backend backend;
    private final String bitString;
    private final byte[] bytes;

    /**
     * Creates a new encoded RNA container.
     *
     * @param backend the backend used to produce the encoded data
     * @param bitString the encoded data as a bit string (may be null)
     * @param bytes the encoded data as a byte array (may be null)
     */
    public EncodedRNA(ArithmeticCodingFactory.Backend backend, String bitString, byte[] bytes) {
        this.backend = backend;
        this.bitString = bitString;
        this.bytes = bytes == null ? null : bytes.clone();
    }

    /**
     * Returns the backend that produced this encoded data.
     *
     * @return the backend identifier
     */
    public ArithmeticCodingFactory.Backend getBackend() {
        return backend;
    }


    /**
     * Returns the encoded data as a bit string.
     *
     * @return the bit string, or null if not available
     */
    public String getBitString() {
        return bitString;
    }

    /**
     * Returns the encoded data as a byte array.
     * A copy of the internal array is returned to preserve immutability.
     *
     * @return a copy of the byte array, or null if not available
     */
    public byte[] getBytes() {
        return bytes == null ? null : bytes.clone();
    }

    /**
     * Returns the length of the encoded data in bits.
     *
     * @return the number of bits in the encoded payload
     */
    public int bitLength() {
        return bitString != null ? bitString.length() : bytes.length * Byte.SIZE;
    }
}
