package dev.jackraidenph.logicevaluator.logic;

import java.util.ArrayList;
import java.util.List;

public class Term extends ArrayList<String> {

    public Term() {
    }

    public Term(Term copy) {
        this(new ArrayList<>(copy));
    }

    public Term(List<String> literals) {
        addAll(literals);
        if (size() !=
                (literals.stream().map(l -> l.replaceFirst("!", "")).count())
        )
            throw new RuntimeException("Failed to make a term from repeating literals!");
    }

    public int toInteger() {
        int result = 0;
        for (int i = 0; i < size(); i++) {
            if (!get(i).contains("!")) {
                result |= 1 << (size() - 1 - i);
            }
        }
        return result;
    }

    public int getPositivesCount() {
        return (int) this.stream().filter(l -> !l.startsWith("!") && !l.equals("-")).count();
    }

    public Term match(Term toMatch) {
        Term result = new Term(this);

        if (contains("-") != toMatch.contains("-"))
            return this;

        int replaced = 0;
        for (int i = 0; i < size(); i++) {
            if (get(i).equals("-") && (!get(i).equals(toMatch.get(i))))
                return this;
            if (get(i).equals(negateLiteral(toMatch.get(i)))) {
                result.set(i, "-");
                replaced++;
                if (replaced > 1)
                    return this;
            }
        }

        return result;
    }

    @Override
    public void add(int index, String element) {
        if (contains(element) || contains(negateLiteral(element)))
            return;
        super.add(index, element);
    }

    public static String matchBoolean(String literal, boolean positive) {
        if (positive)
            return literal;
        return "!" + literal;
    }

    public static String negateLiteral(String literal) {
        if (literal.contains("!"))
            return literal.replaceFirst("!", "");
        return "!" + literal;
    }

    public Term clearDashes() {
        while (indexOf("-") != -1) {
            remove(indexOf("-"));
        }
        return this;
    }
}
