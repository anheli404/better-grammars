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

public final class BigDecimalBackend implements ArithmeticCodingBackend {

    @Override
    public ArithmeticCodingFactory.Backend getBackend() {
        return ArithmeticCodingFactory.Backend.BIG_DECIMAL;
    }

    @Override
    public RNAEncoderFacade createEncoder(RuleProbModel ruleProbModel, RuleSymbolModel ruleSymbolModel, RNAGrammar grammar, NonTerminal startSymbol) {
        return rna -> {
            GenericRNAEncoder encoder =
                    new GenericRNAEncoder(ruleProbModel, new ExactArithmeticEncoder(), grammar, startSymbol);
            return new EncodedRNA(getBackend(), encoder.encodeRNA(rna), null);
        };
    }

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
