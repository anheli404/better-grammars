package compression.arithmaticCoding.benchmark.benchmark_old;

import compression.arithmaticCoding.bigDecimalAc.ExactArithmeticDecoder;
import compression.arithmaticCoding.bigDecimalAc.Interval;
import compression.grammar.Category;
import compression.grammar.NonTerminal;
import compression.grammar.PairOfChar;
import compression.grammar.PairOfCharTerminal;
import compression.grammar.RNAWithStructure;
import compression.samplegrammars.model.RuleProbModel;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public final class BigDecimalDecodeRunner {

    public static RNAWithStructure decode(
            String encodedBits,
            RuleProbModel model,
            NonTerminal startSymbol
    ) throws IOException {

        ExactArithmeticDecoder dec = new ExactArithmeticDecoder(encodedBits);

        final List<Category> leftmostDerivation = new LinkedList<>();
        NonTerminal leftmostNT = startSymbol;
        leftmostDerivation.add(leftmostNT);

        while (leftmostNT != null) {
            // lấy các interval có thể cho nonterminal hiện tại
            List<Interval> options = model.getIntervalList(leftmostNT);
            Interval interval = dec.decodeNext(options);

            // find RHS corresponding to chosen interval
            List<Category> rhs = model.getRhsFor(interval, leftmostNT);
            leftmostNT = replaceFirstNonterminal(leftmostDerivation, rhs);
        }

        return getRNAString(decodeCategoryList(leftmostDerivation));
    }

    private static NonTerminal replaceFirstNonterminal(
            final List<Category> leftmostDerivation,
            List<Category> rhs
    ) {
        for (ListIterator<Category> iterator = leftmostDerivation.listIterator(); iterator.hasNext(); ) {
            final Category cat = iterator.next();
            if (Category.isNonTerminal(cat)) {
                iterator.remove();
                for (Category category : rhs) iterator.add(category);

                // quay lại trước RHS vừa thêm để tìm nonterminal tiếp theo (nếu có)
                for (Category ignored : rhs) iterator.previous();
                while (iterator.hasNext()) {
                    final Category nextCat = iterator.next();
                    if (Category.isNonTerminal(nextCat)) {
                        return (NonTerminal) nextCat;
                    }
                }
            }
        }
        return null;
    }

    private static List<PairOfChar> decodeCategoryList(List<Category> catList) {
        List<PairOfChar> pairOfCharList = new java.util.ArrayList<>();
        for (Category cat : catList) {
            if (!Category.isTerminal(cat)) {
                throw new IllegalArgumentException("only terminals allowed here");
            }
            pairOfCharList.add(((PairOfCharTerminal) cat).getChars());
        }
        return pairOfCharList;
    }

    private static RNAWithStructure getRNAString(List<PairOfChar> pocList) {
        StringBuilder primary = new StringBuilder(pocList.size());
        StringBuilder secondary = new StringBuilder(pocList.size());
        for (PairOfChar poc : pocList) {
            primary.append(poc.getPry());
            secondary.append(poc.getSec());
        }
        return new RNAWithStructure(primary.toString(), secondary.toString());
    }

    private BigDecimalDecodeRunner() {}
}
