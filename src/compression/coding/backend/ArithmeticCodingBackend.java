package compression.coding.backend;

import compression.RuleProbType;
import compression.coding.ArithmeticCodingFactory;
import compression.grammar.NonTerminal;
import compression.grammar.RNAGrammar;
import compression.grammar.RNAWithStructure;
import compression.grammar.Rule;
import compression.samplegrammars.model.bigdecimal.RuleProbModel;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;

import java.util.Map;

/**
 * Interface for arithmetic coding backends.
 * It defines methods for creating encoders, decoders, and symbol models
 * for a specific arithmetic coding implementation.
 */
public interface ArithmeticCodingBackend {

    /**
     * Returns the type of backend implementation.
     *
     * @return the backend identifier
     */
    ArithmeticCodingFactory.Backend getBackend();

    /**
     * Creates an encoder for the given grammar and models.
     *
     * @param ruleProbModel the probability model for rules
     * @param ruleSymbolModel the symbol model used for encoding
     * @param grammar the RNA grammar
     * @param startSymbol the start symbol of the grammar
     * @return an encoder facade for encoding RNA data
     */
    RNAEncoderFacade createEncoder(RuleProbModel ruleProbModel, RuleSymbolModel ruleSymbolModel, RNAGrammar grammar, NonTerminal startSymbol);

    /**
     * Creates a decoder for the given models and encoded data.
     *
     * @param ruleProbModel the probability model for rules
     * @param ruleSymbolModel the symbol model used for decoding
     * @param startSymbol the start symbol of the grammar
     * @param encoded the encoded RNA data
     * @return a decoder facade for decoding RNA data
     */
    RNADecoderFacade createDecoder(RuleProbModel ruleProbModel, RuleSymbolModel ruleSymbolModel, NonTerminal startSymbol, EncodedRNA encoded);

    /**
     * Creates a rule symbol model based on the selected probability type.
     *
     * @param modelType the type of probability model (e.g., static or adaptive)
     * @param grammar the RNA grammar
     * @param rna the RNA sequence used for initialization (if needed)
     * @param staticRuleCounts rule counts used for static models
     * @return the created RuleSymbolModel
     */
    RuleSymbolModel createRuleSymbolModel(RuleProbType modelType, RNAGrammar grammar, RNAWithStructure rna, Map<Rule, Long> staticRuleCounts);
}
