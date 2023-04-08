package dev.jackraidenph.logicevaluator.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Implicant extends HashSet<Integer> {

    public Implicant(Term... terms) {
        addAll(Arrays.stream(terms).map(Term::toInteger).toList());
    }

    public List<Term> toTerms(List<String> literals, boolean positive) {
        List<Term> result = new ArrayList<>();

        for (Integer t : this) {
            Term term = new Term();
            for (int i = 0; i < literals.size(); i++) {
                term.add(Term.matchBoolean(
                                literals.get(i), ((1 & (t >> i)) == 1) == positive
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
            } else if (implicants.stream().allMatch(impl -> impl.contains(Term.negate(l)))) {
                prime.add(Term.negate(l));
            }
        }
        return prime;
    }
}
