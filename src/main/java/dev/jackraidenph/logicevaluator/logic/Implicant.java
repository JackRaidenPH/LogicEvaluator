package dev.jackraidenph.logicevaluator.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Implicant extends HashSet<Integer> {

    public Implicant() {
    }

    public Implicant(Integer... terms) {
        addAll(Arrays.stream(terms).distinct().toList());
    }

    public Implicant(Implicant copy) {
        addAll(new ArrayList<>(copy));
    }

    public List<Term> toTerms(List<String> literals, boolean positive) {
        List<Term> result = new ArrayList<>();

        for (Integer t : this) {
            Term term = new Term();
            final int lSize = literals.size();
            for (int i = 0; i < lSize; i++) {
                term.add(Term.matchBoolean(
                                literals.get(i), ((1 & (t >> (lSize - 1 - i))) == 1) == positive
                        )
                );
            }
            result.add(term);
        }

        return result;
    }

    public Term reducedTerm(List<String> literals, boolean positive) {
        List<Term> implicants = toTerms(literals, positive);
        Term prime = new Term();
        for (String l : literals) {
            if (implicants.stream().allMatch(impl -> impl.contains(l))) {
                prime.add(l);
            } else if (implicants.stream().allMatch(impl -> impl.contains(Term.negateLiteral(l)))) {
                prime.add(Term.negateLiteral(l));
            }
        }
        return prime;
    }
}
