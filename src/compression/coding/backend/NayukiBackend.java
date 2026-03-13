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

public final class NayukiBackend implements ArithmeticCodingBackend {
    private static final int STATE_BITS = 32;

    @Override
    public ArithmeticCodingFactory.Backend getBackend() {
        return ArithmeticCodingFactory.Backend.NAYUKI;
    }

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

    private static RuleSymbolModel requireSymbolModel(RuleSymbolModel ruleSymbolModel) {
        if (ruleSymbolModel == null) {
            throw new IllegalArgumentException("A symbol model is required for the Nayuki backend.");
        }
        return ruleSymbolModel;
    }
}
