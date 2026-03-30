package compression;

import compression.coding.nayuki.FrequencyTable;
import compression.coding.nayuki.NayukiDecoder;
import compression.coding.nayuki.SimpleFrequencyTable;
import compression.grammar.*;
import compression.samplegrammars.model.nayuki.RuleSymbolModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Decoder component for Nayuki arithmetic coding backend.
 * It reconstructs an RNAWithStructure object from an encoded bitstream
 * by repeatedly decoding grammar-rule symbols and expanding the current
 * leftmost non-terminal in the derivation.
 */
public class GenericRNADecoderNayuki {
    protected final NayukiDecoder decoder;
    protected final RuleSymbolModel symbolModel;
    protected final NonTerminal startSymbol;

    public GenericRNADecoderNayuki(RuleSymbolModel symbolModel, NayukiDecoder decoder, NonTerminal startSymbol) {
        this.decoder = decoder;
        this.symbolModel = symbolModel;
        this.startSymbol = startSymbol;
    }

    /**
     * Decodes an RNA sequence from the arithmetic-coded input.
     * Starting from the start symbol, the method repeatedly decodes
     * symbols and expands the leftmost nonterminal until only terminals remain.
     *
     * @return the decoded RNA sequence with its secondary structure
     * @throws RuntimeException if an error occurs while reading from the decoder
     */
    public RNAWithStructure decode() {
        final List<Category> leftmostDerivation = new LinkedList<>();
        NonTerminal leftmostNT = startSymbol;
        leftmostDerivation.add(leftmostNT);
        while (leftmostNT != null) {
            final int[] freqs = symbolModel.getFrequenciesFor(leftmostNT);
            final FrequencyTable freqTable = new SimpleFrequencyTable(freqs);
            final int symbol;
            try {
                symbol = decoder.read(freqTable);
            } catch (IOException e) {
                throw new RuntimeException("Error while decoding symbol for NT: " + leftmostNT, e);
            }
            final List<Category> rhs = symbolModel.getRhsFor(symbol, leftmostNT);
            leftmostNT = replaceFirstNonterminal(leftmostDerivation, rhs);
        }
        return getRNAString(decodeCategoryList(leftmostDerivation));
    }

    /**
     * Replaces the first nonterminal in the derivation with the given rhs categories.
     * After the replacement, the next leftmost nonterminal is returned.
     *
     * @param leftmostDerivation the current derivation
     * @param rhs the right-hand side of the decoded rule
     * @return the next nonterminal to expand, or null if none remain
     */
    private static NonTerminal replaceFirstNonterminal(final List<Category> leftmostDerivation, List<Category> rhs) {
        for (ListIterator<Category> iterator = leftmostDerivation.listIterator(); iterator.hasNext(); ) {
            final Category cat = iterator.next();
            if (Category.isNonTerminal(cat)) {
                // replace the nonterminal with the rhs
                iterator.remove();
                for (Category category : rhs) iterator.add(category);
                // find next nonterminal, can be either in rhs or in rest of derivation
                for (Category category : rhs) iterator.previous(); // backtrack
                while (iterator.hasNext()) {
                    final Category nextCat = iterator.next();
                    if (Category.isNonTerminal(nextCat)) {
                        return (NonTerminal) nextCat;
                    }
                }
            }
        }
        return null; // no more nonterminals
    }

    /**
     * Converts a list of terminal categories into a list of PairOfChar objects.
     *
     * @param catList the list of categories (must contain only terminals)
     * @return a list of PairOfChar objects extracted from the terminals
     * @throws IllegalArgumentException if a nonterminal is encountered
     */
    private ArrayList<PairOfChar> decodeCategoryList(List<Category> catList) {
        ArrayList<PairOfChar> pairOfCharList = new ArrayList<>();
        for (Category cat : catList) {
            if (!Category.isTerminal(cat)) throw new IllegalArgumentException("only terminals allowed here");
            pairOfCharList.add(((PairOfCharTerminal) cat).getChars());
        }
        return pairOfCharList;
    }

    /**
     * Builds an RNAWithStructure object from a list of character pairs.
     *
     * @param POCList the decoded list of PairOfChar objects
     * @return the reconstructed RNA sequence and secondary structure
     */
    public RNAWithStructure getRNAString(ArrayList<PairOfChar> POCList) {
        StringBuilder primary = new StringBuilder(POCList.size()),
                secondary = new StringBuilder(POCList.size());
        for (PairOfChar POC : POCList) {
            primary.append(POC.getPry());
            secondary.append(POC.getSec());
        }
        return new RNAWithStructure(primary.toString(), secondary.toString());
    }


}

