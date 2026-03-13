package compression.coding.backend;

import compression.coding.ArithmeticCodingFactory;

public final class EncodedRNA {
    private final ArithmeticCodingFactory.Backend backend;
    private final String bitString;
    private final byte[] bytes;

    public EncodedRNA(ArithmeticCodingFactory.Backend backend, String bitString, byte[] bytes) {
        this.backend = backend;
        this.bitString = bitString;
        this.bytes = bytes == null ? null : bytes.clone();
    }

    public ArithmeticCodingFactory.Backend getBackend() {
        return backend;
    }

    public String getBitString() {
        return bitString;
    }

    public byte[] getBytes() {
        return bytes == null ? null : bytes.clone();
    }

    public int bitLength() {
        return bitString != null ? bitString.length() : bytes.length * Byte.SIZE;
    }
}
