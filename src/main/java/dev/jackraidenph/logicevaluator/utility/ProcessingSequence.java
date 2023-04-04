package dev.jackraidenph.logicevaluator.utility;

import java.util.*;

public class ProcessingSequence {
    private final List<String> unique;
    private final List<String> processingQueue;

    private ProcessingSequence(List<String> operands, List<String> processingQueue) {
        this.processingQueue = processingQueue;
        unique = operands
                .stream()
                .map(operand -> operand.replace("!", ""))
                .distinct()
                .toList();
    }

    public boolean evaluate(List<Boolean> values) {
        Iterator<Boolean> iterator = values.iterator();
        Map<String, Boolean> operandsMap = unique
                .stream()
                .sorted()
                .collect(HashMap::new,
                        (map, operand) -> {
                            if (iterator.hasNext()) {
                                boolean value = iterator.next();
                                map.put(operand, value);
                                map.put("!" + operand, !value);
                            }
                        },
                        HashMap::putAll);

        List<String> valuesQueue = processingQueue
                .stream()
                .map(token -> operandsMap.containsKey(token)
                ? String.valueOf(operandsMap.get(token))
                : token
        ).toList();

        Deque<String> resultStack = new ArrayDeque<>();
        for (String token : valuesQueue) {
            if (!ParsingUtils.OPERATORS.contains(token)) {
                resultStack.addFirst(token);
            } else {
                Operation operation = ParsingUtils.STR_TO_OP.get(token);

                Boolean left = Boolean.parseBoolean(resultStack.removeFirst());
                Boolean right = !operation.isUnary() && Boolean.parseBoolean(resultStack.removeFirst());

                Object result = operation.apply(left, right);

                resultStack.addFirst(result.toString());
            }
        }

        return !resultStack.isEmpty() && Boolean.parseBoolean(resultStack.removeFirst());
    }

    public static ProcessingSequence fromString(String expression) {
        return postfixToSequence(ParsingUtils.infixToPostfix(expression));
    }

    private static ProcessingSequence postfixToSequence(List<String> queue) {
        List<String> operands = new ArrayList<>();
        for (String token : queue) {
            if (!ParsingUtils.OPERATORS.contains(token)) {
                operands.add(token);
            }
        }
        return new ProcessingSequence(operands, queue);
    }
}
