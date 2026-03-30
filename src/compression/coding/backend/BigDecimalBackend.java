package compression.coding.backend;

import compression.GenericRNADecoder;
import compression.GenericRNAEncoder;
import compression.coding.ArithmeticCodingFactory;
import compression.coding.bigdecimal.ExactArithmeticDecoder;
import compression.coding.bigdecimal.ExactArithmeticEncoder;
import compression.grammar.NonTerminal;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;
import compression.RuleProbType;

import java.util.Map;

/**
 * Arithmetic coding backend implementation using the BigDecimal-based
 * exact arithmetic coder. It creates encoders and decoders that operate
 * on probability intervals rather than discrete symbols.
 */
public final class BigDecimalBackend implements ArithmeticCodingBackend {

    /**
     * Returns the identifier of this backend.
     *
     * @return BIG_DECIMAL backend type
     */
    @Override
    public ArithmeticCodingFactory.Backend getBackend() {
        return ArithmeticCodingFactory.Backend.BIG_DECIMAL;
    }

    /**
     * Creates an encoder using the BigDecimal arithmetic coder.
     *
     * @param ruleProbModel the probability model for rules
     * @param ruleSymbolModel unused for this backend
     * @param grammar the RNA grammar
     * @param startSymbol the start symbol of the grammar
     * @return an encoder facade for encoding RNA data
     */
    @Override
    public RNAEncoderFacade createEncoder(RuleProbModel ruleProbModel, RuleSymbolModel ruleSymbolModel, RNAGrammar grammar, NonTerminal startSymbol) {
        return rna -> {
            GenericRNAEncoder encoder =
                    new GenericRNAEncoder(ruleProbModel, new ExactArithmeticEncoder(), grammar, startSymbol);
            return new EncodedRNA(getBackend(), encoder.encodeRNA(rna), null);
        };
    }

    /**
     * Creates a decoder using the BigDecimal arithmetic coder.
     * Validates that the encoded data matches this backend.
     *
     * @param ruleProbModel the probability model for rules
     * @param ruleSymbolModel unused for this backend
     * @param startSymbol the start symbol of the grammar
     * @param encoded the encoded RNA data
     * @return a decoder facade for decoding RNA data
     * @throws IllegalArgumentException if the backend type or payload is invalid
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
        if (encoded.getBitString() == null) {
            throw new IllegalArgumentException("Missing bit-string payload for BigDecimal decoder.");
        }
        GenericRNADecoder decoder = new GenericRNADecoder(
                ruleProbModel,
                new ExactArithmeticDecoder(encoded.getBitString()),
                startSymbol);
        return decoder::decode;
    }

    /**
     * Creates a rule symbol model for this backend.
     * Not used in the BigDecimal implementation.
     *
     * @param modelType the type of probability model
     * @param grammar the RNA grammar
     * @param rna the RNA sequence
     * @param staticRuleCounts rule counts for static models
     * @return null, since symbol models are not required for this backend
     */
    @Override
    public RuleSymbolModel createRuleSymbolModel(
            RuleProbType modelType,
            RNAGrammar grammar,
            RNAWithStructure rna,
            Map<Rule, Long> staticRuleCounts
    ) {
        return null;
    }
}
