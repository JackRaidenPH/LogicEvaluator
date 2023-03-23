package dev.jackraidenph.logicevaluator.utility;

import java.util.*;
import java.util.stream.Collectors;

public class ProcessingSequence {
    private final List<String> operands;
    private final List<String> processingQueue;

    private ProcessingSequence(List<String> operands, List<String> processingQueue) {
        this.operands = operands;
        this.processingQueue = processingQueue;
    }

    private boolean checkInput(List<String> operands, List<Operations> operators) {
        boolean hasBinary = operators.stream().anyMatch(Operations::isUnary);
        int operandsCount = operands.size();
        int operatorsCount = operators.size();
        return (!hasBinary || ((operandsCount + 1) == operatorsCount))
                && (operandsCount == operatorsCount);
    }

    public boolean evaluate(List<Boolean> values) {
        List<String> unique = operands
                .stream()
                .map(operand -> operand.replace("!", ""))
                .distinct()
                .toList();

        if (values.size() != unique.size())
            throw new RuntimeException("Can not evaluate expression: values mismatch!");

        Iterator<Boolean> iterator = values.iterator();
        Map<String, Boolean> operandsMap = unique
                .stream()
                .sorted()
                .collect(HashMap::new,
                        (map, operand) -> {
                            boolean value = iterator.next();
                            map.put(operand, value);
                            map.put("!" + operand, !value);
                        },
                        HashMap::putAll);

        Deque<String> resultStack = new ArrayDeque<>();
        List<String> valuesQueue = processingQueue.stream().map(token -> {
            if (operandsMap.containsKey(token))
                return String.valueOf(operandsMap.get(token));
            return token;
        }).toList();

        for (String token : valuesQueue) {
            if (!((token.length() == 1) && ParsingUtils.OPERATORS.contains(token))) {
                resultStack.addFirst(token);
            } else {
                Operations operation = ParsingUtils.STR_TO_OP.get(token);

                Boolean left = Boolean.parseBoolean(resultStack.removeFirst());
                Boolean right = null;
                if (!operation.isUnary())
                    right = Boolean.parseBoolean(resultStack.removeFirst());

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
