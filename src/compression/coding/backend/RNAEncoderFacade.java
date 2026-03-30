package compression.coding.backend;

import compression.grammar.RNAWithStructure;

/**
 * Functional interface representing an encoder for RNA data.
 * It provides a single method to encode an RNA sequence.
 */
public interface RNAEncoderFacade {

    /**
     * Functional interface representing an encoder for RNA data.
     * It provides a single method to encode an RNA sequence.
     */
    EncodedRNA encode(RNAWithStructure rna);
}
