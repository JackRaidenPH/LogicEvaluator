package dev.jackraidenph.logicevaluator.utility;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsingUtils {
    public static final Map<String, Operations> STR_TO_OP = new HashMap<>() {{
        put("!", Operations.NOT);
        put("*", Operations.AND);
        put("+", Operations.OR);
        put("^", Operations.XOR);
        put("(", Operations.OPEN);
        put(")", Operations.CLOSE);
    }};

    public static final String OPERATORS = String.join("", STR_TO_OP.keySet());

    public static final Pattern TOKEN_PATTERN = Pattern.compile("(!?[A-z]+)|[/*+!^()]");

    public static List<String> infixToPostfix(String toConvert) {
        Matcher tokenMatcher = TOKEN_PATTERN.matcher(toConvert);
        Deque<String> stack = new LinkedList<>();
        List<String> output = new ArrayList<>();

        while (!toConvert.isEmpty()) {
            if (tokenMatcher.find()) {
                String token = tokenMatcher.group();
                if ((token.length() == 1) && OPERATORS.contains(token)) {
                    if (stack.isEmpty())
                        stack.addFirst(token);
                    else if (token.equals(")")) {
                        while (!stack.peekFirst().equals("("))
                            output.add(stack.removeFirst());
                        stack.removeFirst();
                    } else {
                        int stackPrecedence = STR_TO_OP.get(stack.peekFirst()).getPrecedence();
                        int tokenPrecedence = STR_TO_OP.get(token).getPrecedence();
                        while (!token.equals("(")
                                && (stackPrecedence > tokenPrecedence)
                        ) {
                            output.add(stack.removeFirst());
                            stackPrecedence = STR_TO_OP.get(stack.peekFirst()).getPrecedence();
                        }
                        stack.addFirst(token);
                    }
                } else output.add(token);
                toConvert = toConvert.replaceFirst(Pattern.quote(token), "");
            } else
                break;
        }

        while (!stack.isEmpty())
            output.add(stack.removeFirst());

        return output;
    }
    //!((x + !y) * !(x * z)) // Self // (x + y + z) * (x + y + !z) * (!x + y + z) * (!x + !y + z)
    //(!a * !b * c) + (!a * b * !c) + (!a * b * c) + (a * b * !c)
    //(!a * b * !c * !d) + (a * !b * !c * !d) + (a * !b * !c * d) + (a * !b * c * !d) + (a * !b * c * d) + (a * b * !c * !d) + (a * b * c * !d) + (a * b * c * d)
}
