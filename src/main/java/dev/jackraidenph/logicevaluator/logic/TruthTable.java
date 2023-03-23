package dev.jackraidenph.logicevaluator.logic;

import dev.jackraidenph.logicevaluator.utility.ProcessingSequence;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TruthTable {
    private final List<List<Boolean>> contents = new ArrayList<>();
    private final List<String> operands = new ArrayList<>();
    private final List<List<String>> bufferedPDNF = new ArrayList<>();
    private final List<List<String>> bufferedPCNF = new ArrayList<>();
    private final List<List<String>> bufferedPDNFPrimes = new ArrayList<>();
    private final List<List<String>> bufferedPCNFPrimes = new ArrayList<>();

    private static final Pattern OPERAND_PATTERN = Pattern.compile("(!?[A-z]+)");

    public TruthTable(String expression) {
        int operandsCount = countOperands(expression);

        ProcessingSequence sequence = ProcessingSequence.fromString(expression);

        for (List<Boolean> row : generateStates(operandsCount)) {
            boolean result = sequence.evaluate(row);
            row.add(result);
            contents.add(row);
        }
    }

    public List<String> getOperands() {
        return operands.stream().toList();
    }

    public List<List<Boolean>> getContents() {
        return contents.stream().toList();
    }

    public int getWidth() {
        return contents.get(0).size();
    }

    private int countOperands(String expression) {
        return (int) OPERAND_PATTERN
                .matcher(expression)
                .results()
                .map(matchResult -> matchResult.group().replace("!", ""))
                .distinct()
                .sorted()
                .peek(operands::add)
                .count();
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

    public String getPDNF() {
        bufferedPDNF.clear();

        StringBuilder result = new StringBuilder();

        for (List<Boolean> row : contents) {
            if (row.get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(" + ");
                }
                String onesConstituent = constructOnesConstituent(row);
                bufferedPDNF.add(Arrays.stream(onesConstituent
                                .replaceAll("[^!A-z\\s]", "")
                                .replaceAll("\\s{2,}", " ")
                                .split("\\s"))
                        .collect(Collectors.toList())
                );
                result.append(onesConstituent);
            }
        }

        return result.toString();
    }

    public String getNumericPDNF() {
        StringBuilder result = new StringBuilder();

        Iterator<List<Boolean>> iterator = contents.iterator();
        for (int index = 0; iterator.hasNext(); index++) {
            if (iterator.next().get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(index);
            }
        }

        return result.insert(0, "+(").append(")").toString();
    }

    public String getIndexForm() {
        StringBuilder result = new StringBuilder();

        StringBuilder indexStringRepresentation = new StringBuilder();
        for (List<Boolean> row : contents) {
            indexStringRepresentation.append((row.get(getWidth() - 1) ? '1' : '0'));
        }

        int indexOfFunction = Integer.parseInt(indexStringRepresentation.toString(), 2);

        return result.insert(0, "f(" + (getWidth() - 1)).append(")").append(indexOfFunction).toString();
    }

    public String getNumericPCNF() {
        StringBuilder result = new StringBuilder();

        Iterator<List<Boolean>> iterator = contents.iterator();
        for (int index = 0; iterator.hasNext(); index++) {
            if (!iterator.next().get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(index);
            }
        }

        return result.insert(0, "*(").append(")").toString();
    }

    public String getPCNF() {
        bufferedPCNF.clear();

        StringBuilder result = new StringBuilder();

        for (List<Boolean> row : contents) {
            if (!row.get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(" * ");
                }
                String zeroesConstituent = constructZerosConstituent(row);
                bufferedPCNF.add(Arrays.stream(zeroesConstituent
                                .replaceAll("[^!A-z\\s]", "")
                                .replaceAll("\\s{2,}", " ")
                                .split("\\s"))
                        .map(String::trim)
                        .collect(Collectors.toList())
                );
                result.append(zeroesConstituent);
            }
        }

        return result.toString();
    }

    private String constructOnesConstituent(List<Boolean> row) {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < getWidth() - 1; index++) {
            String element = operands.get(index);
            if (!row.get(index)) {
                element = "!" + element;
            }

            result.append(element);
            if (index != (getWidth() - 2)) {
                result.append(" * ");
            }
        }

        return result.insert(0, "(").append(")").toString();
    }

    private String constructZerosConstituent(List<Boolean> row) {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < getWidth() - 1; index++) {
            String element = operands.get(index);
            if (row.get(index)) {
                element = "!" + element;
            }

            result.append(element);
            if (index != (getWidth() - 2)) {
                result.append(" + ");
            }
        }

        return result.insert(0, "(").append(")").toString();
    }

    private boolean isPrime(List<String> implicant, List<List<String>> allImplicants) {
        for (List<String> compareAgainst : allImplicants) {
            if (implicant.equals(compareAgainst) || !implicant.contains("-") || !compareAgainst.contains("-")) {
                continue;
            }

            List<Integer> dashes1 = IntStream.range(0, implicant.size())
                    .map(index -> implicant.get(index).equals("-") ? index : -1)
                    .filter(val -> val != -1)
                    .boxed()
                    .toList();

            List<Integer> dashes2 = IntStream.range(0, compareAgainst.size())
                    .map(index -> compareAgainst.get(index).equals("-") ? index : -1)
                    .filter(val -> val != -1)
                    .boxed()
                    .toList();

            boolean dashesAlign = dashes1.equals(dashes2);

            if (dashesAlign) {
                return false;
            }
        }
        return true;
    }

    private List<List<String>> getIterationPrimes(List<List<String>> reducedForm) {
        List<List<String>> primes = new ArrayList<>();
        for (List<String> implicant : reducedForm) {
            if (isPrime(implicant, reducedForm)) {
                primes.add(implicant.stream().toList());
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
        List<List<String>> buffer = SCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        if (buffer.isEmpty()) {
            if (SCNF) {
                getPCNFPrimes();
            } else {
                getPDNFPrimes();
            }
        }
        StringBuilder result = new StringBuilder();

        for (List<String> prime : buffer) {
            StringBuilder primeStringBuilder = new StringBuilder("(");
            for (String operand : prime) {
                primeStringBuilder.append(operand);
                if (!operand.equals(prime.get(prime.size() - 1))) {
                    primeStringBuilder.append(SCNF ? " + " : " * ");
                }
            }
            primeStringBuilder.append(")");
            result.append(primeStringBuilder);
            if (!prime.equals(buffer.get(buffer.size() - 1))) {
                result.append(SCNF ? " * " : " + ");
            }
        }

        return result.toString();
    }

    private void getPDNFPrimes() {
        getFormPrimes(false);
    }

    private void getPCNFPrimes() {
        getFormPrimes(true);
    }

    private void getFormPrimes(boolean PCNF) {
        List<List<String>> buffer = PCNF ? bufferedPCNF : bufferedPDNF;
        List<List<String>> primeBuffer = PCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        if (buffer.isEmpty()) {
            if (PCNF) {
                getPCNF();
            } else {
                getPDNF();
            }
        }
        primeBuffer.clear();
        primeBuffer.addAll(getPrimeImplicants(buffer, new ArrayList<>()));
    }

    private List<List<String>> getPrimeImplicants(List<List<String>> toReduce, List<List<String>> primes) {
        List<List<String>> shortened = new ArrayList<>();
        List<List<List<String>>> implicantLevels = new ArrayList<>();

        for (List<String> implicant : toReduce) {
            int level = implicant.stream().map(op -> op.startsWith("!") ? 0 : 1).reduce(0, Integer::sum);
            while (implicantLevels.size() <= level) {
                implicantLevels.add(new ArrayList<>());
            }
            implicantLevels.get(level).add(implicant);
        }

        for (int level = 0; level < implicantLevels.size() - 1; level++) {
            for (List<String> implicant : implicantLevels.get(level)) {
                for (List<String> toCheck : implicantLevels.get(level + 1)) {
                    List<String> matched = matchImplicants(implicant, toCheck);
                    if (matched.stream().filter(pos -> pos.equals("-")).count() >
                            implicant.stream().filter(pos -> pos.equals("-")).count()
                            && !shortened.contains(matched)) {
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
                    .map(prime -> prime
                            .stream()
                            .filter(val -> !val.equals("-"))
                            .toList())
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
