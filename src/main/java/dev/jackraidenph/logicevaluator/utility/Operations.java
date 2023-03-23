package dev.jackraidenph.logicevaluator.utility;

import java.util.function.BiFunction;

public enum Operations {
    NOT(true, 4, (f, s) -> !f),
    AND(false, 3, (f, s) -> f && s),
    OR(false, 2, (f, s) -> f || s),
    XOR(false, 2, (f, s) -> f ^ s),
    OPEN(true, -1, (f, s) -> false),
    CLOSE(true, -1, (f, s) -> false);

    public boolean isUnary() {
        return unary;
    }

    public int getPrecedence() {
        return precedence;
    }

    public boolean apply(Boolean first, Boolean second) {
        return function.apply(first, second);
    }

    public final boolean unary;
    public final int precedence;
    public final BiFunction<Boolean, Boolean, Boolean> function;
    Operations(boolean unary, int precedence, BiFunction<Boolean, Boolean, Boolean> function) {
        this.unary = unary;
        this.precedence = precedence;
        this.function = function;
    }
}
