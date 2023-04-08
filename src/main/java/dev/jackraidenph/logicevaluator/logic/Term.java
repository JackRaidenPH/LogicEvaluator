package dev.jackraidenph.logicevaluator.logic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Term extends ArrayList<String> {

    public static final Comparator<String> UNIQUE_COMPARATOR =
            Comparator.comparing(l -> l.replaceFirst("!", ""));

    public Term(String... literals) {
        this.addAll(
                Arrays.stream(literals).distinct().sorted().toList()
        );
        if (this.size() !=
                (Arrays.stream(literals).map(l -> l.replaceFirst("!", "")).count())
        )
            throw new RuntimeException("Failed to make a term from repeating literals!");
    }

    public int toInteger() {
        int result = 0;
        for (int i = 0; i < size(); i++) {
            if (get(i).contains("!")) {
                result |= 1 << i;
            }
        }
        return result;
    }

    @Override
    public void add(int index, String element) {
        if (contains(element) || contains(negate(element)))
            return;
        super.add(index, element);
        sort(UNIQUE_COMPARATOR);
    }

    public void negateAtPos(int index) {
        this.set(index, negate(get(index)));
    }

    public static String matchBoolean(String literal, boolean positive) {
        if (positive)
            return literal;
        return "!" + literal;
    }

    public static String negate(String literal) {
        if (literal.contains("!"))
            return literal.replaceFirst("!", "");
        return "!" + literal;
    }
}
