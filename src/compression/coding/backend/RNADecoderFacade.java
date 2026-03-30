package compression.coding.backend;

import compression.grammar.RNAWithStructure;

/**
 * Functional interface representing a decoder for RNA data.
 * It provides a single method to reconstruct an RNA sequence
 * from encoded input.
 */
public interface RNADecoderFacade {

    /**
     * Decodes the encoded RNA data.
     *
     * @return the decoded RNA sequence with its secondary structure
     */
    RNAWithStructure decode();
}
