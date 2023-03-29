package dev.jackraidenph.logicevaluator.logic;

import dev.jackraidenph.logicevaluator.utility.ProcessingSequence;
import javafx.util.Pair;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class TruthTable {
    private final List<List<Boolean>>/*                      */contents = new ArrayList<>();
    private final List<String>/*                             */bufferedOperands = new ArrayList<>();
    private final List<List<String>>/*                       */bufferedPDNF = new ArrayList<>();
    private final List<List<String>>/*                       */bufferedPCNF = new ArrayList<>();
    private final List<Pair<List<String>, List<Integer>>>/*  */bufferedPDNFPrimes = new ArrayList<>();
    private final List<Pair<List<String>, List<Integer>>>/*  */bufferedPCNFPrimes = new ArrayList<>();
    private final List<List<String>>/*                       */bufferedCalculativeFDNF = new ArrayList<>();
    private final List<List<String>>/*                       */bufferedCalculativeFCNF = new ArrayList<>();

    private final StringBuilder/*                            */bufferedExpression = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringPDNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringPCNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedNumericStringPDNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedNumericStringPCNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringIndex = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringSDNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringSCNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringCalculativeFDNF = new StringBuilder();
    private final StringBuilder/*                            */bufferedStringCalculativeFCNF = new StringBuilder();

    private static final Pattern OPERAND_PATTERN = Pattern.compile("(!?[A-z]+)");

    public TruthTable(String expression) {
        bufferedExpression.append(expression);

        int operandsCount = countOperands();

        ProcessingSequence sequence = ProcessingSequence.fromString(expression);

        for (List<Boolean> row : generateStates(operandsCount)) {
            boolean result = sequence.evaluate(row);
            row.add(result);
            contents.add(row);
        }
    }

    public List<String> getOperands() {
        countOperands();
        return bufferedOperands.stream().toList();
    }

    public List<List<Boolean>> getContents() {
        return contents.stream().toList();
    }

    public int getWidth() {
        return contents.get(0).size();
    }

    private int countOperands() {
        if (bufferedOperands.isEmpty()) {
            bufferedOperands.addAll(getUniqueOperands(bufferedExpression.toString()));
        }
        return bufferedOperands.size();
    }

    private List<String> getUniqueOperands(String expression) {
        return OPERAND_PATTERN
                .matcher(expression)
                .results()
                .map(matchResult -> matchResult.group().replace("!", ""))
                .distinct()
                .sorted()
                .toList();
    }

    private List<List<Boolean>> generateStates(int size) {
        List<List<Boolean>> rows = new ArrayList<>();
        int rowsCount = (int) Math.pow(2, size);

        for (int i = 0; i < rowsCount; i++) {
            rows.add(new ArrayList<>());
            for (int j = 0; j < size; j++) {
                rows.get(i).add((i & (1 << j)) != 0);
            }
            Collections.reverse(rows.get(i));
        }

        return rows;
    }

    public String getPrincipal(boolean PCNF) {
        List<List<String>> buffer = PCNF ? bufferedPCNF : bufferedPDNF;
        List<List<String>> localBuffer = new ArrayList<>();
        StringBuilder bufferedString = PCNF ? bufferedStringPCNF : bufferedStringPDNF;

        if (!bufferedString.isEmpty())
            return bufferedString.toString();

        StringBuilder result = new StringBuilder();

        for (List<Boolean> row : contents) {
            if (PCNF ^ row.get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(PCNF ? " * " : " + ");
                }
                String constituent = PCNF ? constructZerosConstituent(row) : constructOnesConstituent(row);
                localBuffer.add(Arrays.stream(constituent
                                .replaceAll("[^!A-z\\s]", "")
                                .replaceAll("\\s{2,}", " ")
                                .split("\\s"))
                        .toList()
                );
                result.append(constituent);
            }
        }

        if (buffer.isEmpty()) {
            buffer.addAll(localBuffer);
        }

        bufferedString.append(result);

        return bufferedString.toString();
    }

    public String getPDNF() {
        return getPrincipal(false);
    }

    public String getPCNF() {
        return getPrincipal(true);
    }

    public String getNumeric(boolean PCNF) {
        StringBuilder bufferedString = PCNF ? bufferedNumericStringPCNF : bufferedNumericStringPDNF;

        if (!bufferedString.isEmpty())
            return bufferedString.toString();

        StringBuilder result = new StringBuilder();

        Iterator<List<Boolean>> iterator = contents.iterator();
        for (int index = 0; iterator.hasNext(); index++) {
            if (PCNF ^ iterator.next().get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(index);
            }
        }

        result.insert(0, PCNF ? "*(" : "+(").append(")");
        bufferedString.append(result);

        return bufferedString.toString();
    }

    public String getNumericPDNF() {
        return getNumeric(false);
    }

    public String getNumericPCNF() {
        return getNumeric(true);
    }

    private String constructConstituent(boolean Ones, List<Boolean> row) {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < getWidth() - 1; index++) {
            String element = bufferedOperands.get(index);
            if (Ones ^ row.get(index)) {
                element = "!" + element;
            }

            result.append(element);
            if (index != (getWidth() - 2)) {
                result.append(Ones ? " * " : " + ");
            }
        }

        return result.insert(0, "(").append(")").toString();
    }

    private String constructOnesConstituent(List<Boolean> row) {
        return constructConstituent(true, row);
    }

    private String constructZerosConstituent(List<Boolean> row) {
        return constructConstituent(false, row);
    }

    public String getIndexForm() {
        if (!bufferedStringIndex.isEmpty())
            return bufferedStringIndex.toString();

        StringBuilder result = new StringBuilder();

        StringBuilder indexStringRepresentation = new StringBuilder();
        for (List<Boolean> row : contents) {
            indexStringRepresentation.append((row.get(getWidth() - 1) ? '1' : '0'));
        }

        int indexOfFunction = Integer.parseInt(indexStringRepresentation.toString(), 2);

        result.insert(0, "f(" + (getWidth() - 1)).append(")").append(indexOfFunction);

        bufferedStringIndex.append(result);

        return bufferedStringIndex.toString();
    }

    private boolean isPrime(List<String> implicant, List<List<String>> allImplicants) {
        for (List<String> compareAgainst : allImplicants) {
            if (implicant.equals(compareAgainst) || !implicant.contains("-") || !compareAgainst.contains("-")) {
                continue;
            }

            boolean dashesAlign = IntStream.range(0, implicant.size())
                    .reduce(0, (result, index) -> {
                        String implicantToken = implicant.get(index);
                        String comparisonToken = compareAgainst.get(index);
                        if ((implicantToken.equals("-") || comparisonToken.equals("-")) && !implicantToken.equals(comparisonToken)) {
                            return result + 1;
                        }
                        return result;
                    }) == 0;

            if (dashesAlign)
                return false;
        }
        return true;
    }

    private List<Pair<List<String>, List<Integer>>> getIterationPrimes(List<Pair<List<String>, List<Integer>>> reducedForm) {
        List<List<String>> implicantsOnly = reducedForm.stream().map(Pair::getKey).toList();

        List<Pair<List<String>, List<Integer>>> primes = new ArrayList<>();
        for (Pair<List<String>, List<Integer>> pair : reducedForm) {
            if (isPrime(pair.getKey(), implicantsOnly)) {
                primes.add(pair);
            }
        }
        return primes;
    }

    private List<String> matchImplicants(List<String> implicant1, List<String> implicant2) {
        List<String> buffer1 = new ArrayList<>(implicant1);
        List<String> buffer2 = new ArrayList<>(implicant2);
        buffer1.removeAll(implicant2);
        buffer2.removeAll(implicant1);

        List<String> result = new ArrayList<>(implicant1);

        if (!((buffer1.size() == buffer2.size()) && (buffer1.size() == 1))) {
            return result;
        }

        String diff1 = buffer1.get(0);
        String diff2 = buffer2.get(0);

        boolean equals = diff1.replaceFirst("!", "")
                .equals(diff2.replaceFirst("!", ""));

        if (equals) {
            result.set(result.indexOf(diff1), "-");
        }
        return result;
    }

    public String getSCNF() {
        return getShortenedForm(true);
    }

    public String getSDNF() {
        return getShortenedForm(false);
    }

    public String getShortenedForm(boolean SCNF) {
        List<Pair<List<String>, List<Integer>>> buffer = SCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;
        StringBuilder bufferedString = SCNF ? bufferedStringSCNF : bufferedStringSDNF;

        if (!bufferedString.isEmpty())
            return bufferedString.toString();

        if (buffer.isEmpty()) {
            if (SCNF) {
                getPCNFPrimes();
            } else {
                getPDNFPrimes();
            }
        }

        List<List<String>> primes = buffer.stream().map(Pair::getKey).toList();

        StringBuilder result = new StringBuilder();

        for (List<String> prime : primes) {
            StringBuilder primeStringBuilder = new StringBuilder("(");
            for (String operand : prime) {
                primeStringBuilder.append(operand);
                if (!operand.equals(prime.get(prime.size() - 1))) {
                    primeStringBuilder.append(SCNF ? " + " : " * ");
                }
            }
            primeStringBuilder.append(")");
            result.append(primeStringBuilder);
            if (!prime.equals(primes.get(primes.size() - 1))) {
                result.append(SCNF ? " * " : " + ");
            }
        }

        bufferedString.append(result);

        return bufferedString.toString();
    }

    private List<Pair<List<String>, List<Integer>>> getPDNFPrimes() {
        return getFormPrimes(false);
    }

    private List<Pair<List<String>, List<Integer>>> getPCNFPrimes() {
        return getFormPrimes(true);
    }

    private List<Pair<List<String>, List<Integer>>> withMatchInfo(List<List<String>> implicant) {
        return implicant
                .stream()
                .map(list -> new Pair<>(list, (List<Integer>) new ArrayList<Integer>()))
                .toList();
    }

    private List<List<String>> noMatchInfo(List<Pair<List<String>, List<Integer>>> implicant) {
        return implicant
                .stream()
                .map(Pair::getKey)
                .toList();
    }

    private List<Pair<List<String>, List<Integer>>> getFormPrimes(boolean PCNF) {
        List<List<String>> buffer = PCNF ? bufferedPCNF : bufferedPDNF;
        List<Pair<List<String>, List<Integer>>> primeBuffer = PCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        if (!primeBuffer.isEmpty())
            return primeBuffer;

        if (buffer.isEmpty()) {
            if (PCNF) {
                getPCNF();
            } else {
                getPDNF();
            }
        }

        List<Pair<List<String>, List<Integer>>> converted = withMatchInfo(buffer);
        primeBuffer.addAll(getPrimeImplicants(converted, new ArrayList<>()));

        return primeBuffer;
    }

    private int constituentToInt(List<String> consistent) {
        return Integer.parseInt(consistent.stream().collect(
                StringBuilder::new,
                (builder, op) -> builder.append(op.contains("!") ? 0 : 1),
                StringBuilder::append
        ).toString(), 2);
    }

    private List<Pair<List<String>, List<Integer>>> getPrimeImplicants(List<Pair<List<String>, List<Integer>>> toReduce, List<Pair<List<String>, List<Integer>>> primes) {
        List<Pair<List<String>, List<Integer>>> shortened = new ArrayList<>();
        List<List<Pair<List<String>, List<Integer>>>> implicantLevels = new ArrayList<>();

        for (var implicantIterator = toReduce.listIterator(); implicantIterator.hasNext(); ) {
            int index = implicantIterator.nextIndex();
            Pair<List<String>, List<Integer>> implicant = implicantIterator.next();
            if (implicant.getValue().isEmpty()) {
                implicant.getValue().add(constituentToInt(implicant.getKey()));
            }
        }

        for (var implicant : toReduce) {
            int level = implicant.getKey().stream().map(op -> op.startsWith("!") ? 0 : 1).reduce(0, Integer::sum);
            while (implicantLevels.size() <= level) {
                implicantLevels.add(new ArrayList<>());
            }
            implicantLevels.get(level).add(implicant);
        }

        for (int level = 0; level < implicantLevels.size() - 1; level++) {
            List<Pair<List<String>, List<Integer>>> thisLevel = implicantLevels.get(level);
            List<Pair<List<String>, List<Integer>>> nextLevel = implicantLevels.get(level + 1);
            for (Pair<List<String>, List<Integer>> implicant : thisLevel) {
                for (Pair<List<String>, List<Integer>> toCheck : nextLevel) {
                    List<Integer> joinedIndices = new ArrayList<>(implicant.getValue());
                    joinedIndices.addAll(toCheck.getValue());

                    Pair<List<String>, List<Integer>> matched =
                            new Pair<>(matchImplicants(implicant.getKey(), toCheck.getKey()),
                                    joinedIndices);

                    if (matched.getKey().stream().filter(pos -> pos.equals("-")).count() >
                            toCheck.getKey().stream().filter(pos -> pos.equals("-")).count()
                            && !noMatchInfo(shortened).contains(matched.getKey())) {
                        shortened.add(matched);
                    }
                }
            }
        }

        primes.addAll(getIterationPrimes(shortened));

        boolean canReduceFurther = primes.size() < shortened.size();

        if (canReduceFurther) {
            return getPrimeImplicants(shortened, primes);
        } else {
            return primes
                    .stream()
                    .map(pair -> new Pair<>(pair.getKey()
                            .stream()
                            .filter(val -> !val.equals("-"))
                            .toList(), pair.getValue()))
                    .toList();
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (List<Boolean> row : contents) {
            for (Boolean value : row) {
                result.append(value ? "1" : "0").append(" ");
            }
            result.append("\n");
        }
        return result.toString();
    }
}