package compression.coding.backend;

import compression.grammar.RNAWithStructure;

public interface RNAEncoderFacade {
    EncodedRNA encode(RNAWithStructure rna);
}
