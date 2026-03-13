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

public interface ArithmeticCodingBackend {
    ArithmeticCodingFactory.Backend getBackend();

    RNAEncoderFacade createEncoder(RuleProbModel ruleProbModel, RuleSymbolModel ruleSymbolModel, RNAGrammar grammar, NonTerminal startSymbol);

    RNADecoderFacade createDecoder(RuleProbModel ruleProbModel, RuleSymbolModel ruleSymbolModel, NonTerminal startSymbol, EncodedRNA encoded);

    RuleSymbolModel createRuleSymbolModel(RuleProbType modelType, RNAGrammar grammar, RNAWithStructure rna, Map<Rule, Long> staticRuleCounts);
}
