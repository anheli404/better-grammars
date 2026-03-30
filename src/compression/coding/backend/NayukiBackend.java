package compression.coding.backend;

import compression.GenericRNADecoderNayuki;
import compression.GenericRNAEncoderNayuki;
import compression.RuleProbType;
import compression.coding.ArithmeticCodingFactory;
import compression.coding.nayuki.BitInputStream;
import compression.coding.nayuki.BitOutputStream;
import compression.coding.nayuki.NayukiDecoder;
import compression.coding.nayuki.NayukiEncoder;
import compression.grammar.NonTerminal;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.nayuki.AdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import compression.samplegrammars.model.nayuki.SemiAdaptiveRuleSymbolModel;
import compression.samplegrammars.model.nayuki.StaticRuleSymbolModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Arithmetic coding backend implementation using the Nayuki arithmetic coder.
 * It creates encoders and decoders that operate on integer symbols and
 * frequency tables instead of probability intervals.
 */
public final class NayukiBackend implements ArithmeticCodingBackend {
    private static final int STATE_BITS = 32;

    /**
     * Returns the identifier of this backend.
     *
     * @return NAYUKI backend type
     */
    @Override
    public ArithmeticCodingFactory.Backend getBackend() {
        return ArithmeticCodingFactory.Backend.NAYUKI;
    }

    /**
     * Creates an encoder using the Nayuki arithmetic coder.
     *
     * @param ruleProbModel the probability model for rules
     * @param ruleSymbolModel the symbol model used for encoding
     * @param grammar the RNA grammar
     * @param startSymbol the start symbol of the grammar
     * @return an encoder facade for encoding RNA data
     */
    @Override
    public RNAEncoderFacade createEncoder(
            RuleProbModel ruleProbModel,
            RuleSymbolModel ruleSymbolModel,
            RNAGrammar grammar,
            NonTerminal startSymbol
    ) {
        RuleSymbolModel requiredSymbolModel = requireSymbolModel(ruleSymbolModel);
        return rna -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BitOutputStream bitOut = new BitOutputStream(out);
            NayukiEncoder encoder = new NayukiEncoder(STATE_BITS, bitOut);
            GenericRNAEncoderNayuki genericEncoder = new GenericRNAEncoderNayuki(
                    ruleProbModel, requiredSymbolModel, encoder, out, bitOut, grammar, startSymbol);
            return new EncodedRNA(getBackend(), null, genericEncoder.encodeRNANayuki(rna));
        };
    }

    /**
     * Creates a decoder using the Nayuki arithmetic coder.
     * Validates that the encoded data matches this backend.
     *
     * @param ruleProbModel the probability model for rules
     * @param ruleSymbolModel the symbol model used for decoding
     * @param startSymbol the start symbol of the grammar
     * @param encoded the encoded RNA data
     * @return a decoder facade for decoding RNA data
     * @throws IllegalArgumentException if the backend type or payload is invalid
     * @throws RuntimeException if the decoder initialization fails
     */
    @Override
    public RNADecoderFacade createDecoder(
            RuleProbModel ruleProbModel,
            RuleSymbolModel ruleSymbolModel,
            NonTerminal startSymbol,
            EncodedRNA encoded
    ) {
        if (encoded.getBackend() != getBackend()) {
            throw new IllegalArgumentException("Encoded payload was produced by " + encoded.getBackend());
        }
        byte[] bytes = encoded.getBytes();
        if (bytes == null) {
            throw new IllegalArgumentException("Missing byte payload for Nayuki decoder.");
        }
        RuleSymbolModel requiredSymbolModel = requireSymbolModel(ruleSymbolModel);
        try {
            GenericRNADecoderNayuki decoder = new GenericRNADecoderNayuki(
                    requiredSymbolModel,
                    new NayukiDecoder(STATE_BITS, new BitInputStream(new ByteArrayInputStream(bytes))),
                    startSymbol);
            return decoder::decode;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Nayuki decoder.", e);
        }
    }

    /**
     * Creates a rule symbol model based on the selected probability type.
     *
     * @param modelType the type of probability model
     * @param grammar the RNA grammar
     * @param rna the RNA sequence used for initialization (if needed)
     * @param staticRuleCounts rule counts used for static models
     * @return the created RuleSymbolModel
     * @throws IllegalArgumentException if required data is missing
     * @throws UnsupportedOperationException if the model type is not supported
     */
    @Override
    public RuleSymbolModel createRuleSymbolModel(
            RuleProbType modelType,
            RNAGrammar grammar,
            RNAWithStructure rna,
            Map<Rule, Long> staticRuleCounts
    ) {
        switch (modelType) {
            case STATIC:
                if (staticRuleCounts == null) {
                    throw new IllegalArgumentException("Static Nayuki encoding requires rule counts.");
                }
                return new StaticRuleSymbolModel(grammar, staticRuleCounts);
            case STATIC_FROM_FILE:
                throw new UnsupportedOperationException(
                        "Nayuki backend does not support STATIC_FROM_FILE because it requires rule counts, not probabilities.");
            case SEMI_ADAPTIVE:
                return new SemiAdaptiveRuleSymbolModel(grammar, rna);
            case ADAPTIVE:
                return new AdaptiveRuleSymbolModel(grammar);
            default:
                throw new AssertionError("Unsupported rule probability model: " + modelType);
        }
    }

    /**
     * Ensures that a symbol model is provided for the Nayuki backend.
     *
     * @param ruleSymbolModel the symbol model to check
     * @return the validated symbol model
     * @throws IllegalArgumentException if the symbol model is null
     */
    private static RuleSymbolModel requireSymbolModel(RuleSymbolModel ruleSymbolModel) {
        if (ruleSymbolModel == null) {
            throw new IllegalArgumentException("A symbol model is required for the Nayuki backend.");
        }
        return ruleSymbolModel;
    }
}
