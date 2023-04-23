package dev.jackraidenph.logicevaluator.logic;

import dev.jackraidenph.logicevaluator.utility.ProcessingSequence;
import javafx.util.Pair;

import java.util.*;
import java.util.regex.Pattern;

public class TruthTable {
    private final List<List<Boolean>>/*                      */contents = new ArrayList<>();
    private final List<String>/*                             */bufferedOperands = new ArrayList<>();
    private final List<Term>/*                               */bufferedPDNF = new ArrayList<>();
    private final List<Term>/*                               */bufferedPCNF = new ArrayList<>();
    private final List<Implicant>/*                          */bufferedPDNFPrimes = new ArrayList<>();
    private final List<Implicant>/*                          */bufferedPCNFPrimes = new ArrayList<>();
    private final List<Term>/*                               */bufferedCalculativeFDNF = new ArrayList<>();
    private final List<Term>/*                               */bufferedCalculativeFCNF = new ArrayList<>();
    private final List<Term>/*                               */bufferedQMCCFDNF = new ArrayList<>();
    private final List<Term>/*                               */bufferedQMCCFCNF = new ArrayList<>();

    private final String/*                                   */bufferedExpression;


    private static final Pattern OPERAND_PATTERN = Pattern.compile("(!?[A-Za-z]+)");

    public TruthTable(String expression) {
        bufferedExpression = expression;

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
            bufferedOperands.addAll(getUniqueOperands(bufferedExpression));
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

    protected static List<List<Boolean>> generateStates(int size) {
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
        List<Term> buffer = PCNF ? bufferedPCNF : bufferedPDNF;
        List<Term> localBuffer = new ArrayList<>();

        StringBuilder result = new StringBuilder();

        for (List<Boolean> row : contents) {
            if (PCNF ^ row.get(getWidth() - 1)) {
                if (!result.isEmpty()) {
                    result.append(PCNF ? " * " : " + ");
                }
                String constituent = PCNF ? constructZerosConstituent(row) : constructOnesConstituent(row);
                localBuffer.add(new Term(Arrays.stream(constituent
                                .replaceAll("[^!A-z\\s]", "")
                                .replaceAll("\\s{2,}", " ")
                                .split("\\s"))
                        .toList())
                );
                result.append(constituent);
            }
        }

        if (buffer.isEmpty()) {
            buffer.addAll(localBuffer);
        }

        return result.toString();
    }

    public String getPDNF() {
        return getPrincipal(false);
    }

    public String getPCNF() {
        return getPrincipal(true);
    }

    public String getNumeric(boolean PCNF) {
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

        return result.toString();
    }

    public String getNumericPDNF() {
        return getNumeric(false);
    }

    public String getNumericPCNF() {
        return getNumeric(true);
    }

    private String constructConstituent(boolean ones, List<Boolean> row) {
        StringBuilder result = new StringBuilder();

        for (int index = 0; index < getWidth() - 1; index++) {
            String element = bufferedOperands.get(index);
            if (ones ^ row.get(index)) {
                element = "!" + element;
            }

            result.append(element);
            if (index != (getWidth() - 2)) {
                result.append(ones ? " * " : " + ");
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
        StringBuilder result = new StringBuilder();

        StringBuilder indexStringRepresentation = new StringBuilder();
        for (List<Boolean> row : contents) {
            indexStringRepresentation.append((row.get(getWidth() - 1) ? '1' : '0'));
        }

        int indexOfFunction = Integer.parseInt(indexStringRepresentation.toString(), 2);

        result.insert(0, "f(" + (getWidth() - 1)).append(")").append(indexOfFunction);

        return result.toString();
    }

    private boolean isPrime(Term implicant, List<Term> allImplicants) {
        for (Term compareAgainst : allImplicants) {
            if (!implicant.match(compareAgainst).equals(implicant))
                return false;
        }
        return true;
    }

    private List<Pair<Term, Implicant>> getIterationPrimes(List<Pair<Term, Implicant>> pairs) {
        List<Pair<Term, Implicant>> primes = new ArrayList<>();
        List<Term> terms = pairs.stream().map(Pair::getKey).toList();
        for (Pair<Term, Implicant> term : pairs) {
            if (isPrime(term.getKey(), terms)) {
                primes.add(term);
            }
        }
        return primes;
    }

    public String getSDNF() {
        return getShortenedForm(false);
    }

    public String getSCNF() {
        return getShortenedForm(true);
    }

    public String getShortenedForm(boolean SCNF) {
        List<Implicant> buffer = SCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        if (buffer.isEmpty()) {
            if (SCNF) {
                buildPCNFPrimes();
            } else {
                buildPDNFPrimes();
            }
        }

        List<Term> primes = buffer.stream().map(impl -> impl.reducedTerm(bufferedOperands, true)).toList();

        StringBuilder result = new StringBuilder();

        for (Term prime : primes) {
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

        return result.toString();
    }

    private void buildPDNFPrimes() {
        buildFormPrimes(false);
    }

    private void buildPCNFPrimes() {
        buildFormPrimes(true);
    }

    private List<Pair<Term, Implicant>> withMatchInfo(List<Term> list) {
        return list
                .stream()
                .map(term -> new Pair<>(term, new Implicant(term.toInteger())))
                .toList();
    }

    private void buildFormPrimes(boolean PCNF) {
        List<Term> buffer = PCNF ? bufferedPCNF : bufferedPDNF;
        List<Implicant> primeBuffer = PCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        if (!primeBuffer.isEmpty())
            return;

        if (buffer.isEmpty()) {
            if (PCNF) {
                getPCNF();
            } else {
                getPDNF();
            }
        }

        List<Pair<Term, Implicant>> converted = withMatchInfo(buffer);
        primeBuffer.addAll(getPrimeImplicants(converted, new ArrayList<>()).stream().map(Pair::getValue).toList());
    }

    private List<Pair<Term, Implicant>> getPrimeImplicants(List<Pair<Term, Implicant>> toReduce,
                                                           List<Pair<Term, Implicant>> primes) {
        List<Pair<Term, Implicant>> shortened = new ArrayList<>();
        List<List<Pair<Term, Implicant>>> implicantLevels = new ArrayList<>();

        for (Pair<Term, Implicant> implicant : toReduce) {
            int level = implicant.getKey().getPositivesCount();
            while (implicantLevels.size() <= level) {
                implicantLevels.add(new ArrayList<>());
            }
            implicantLevels.get(level).add(implicant);
        }

        for (int level = 0; level < implicantLevels.size() - 1; level++) {
            List<Pair<Term, Implicant>> thisLevel = implicantLevels.get(level);
            List<Pair<Term, Implicant>> nextLevel = implicantLevels.get(level + 1);

            for (Pair<Term, Implicant> pair : thisLevel) {
                for (Pair<Term, Implicant> toCheck : nextLevel) {
                    Implicant joinedIndices = new Implicant(pair.getValue());
                    joinedIndices.addAll(toCheck.getValue());

                    if (shortened.stream().map(Pair::getValue).toList().contains(joinedIndices))
                        continue;

                    Pair<Term, Implicant> matched =
                            new Pair<>(pair.getKey().match(toCheck.getKey()), joinedIndices);

                    if (!matched.getKey().equals(pair.getKey())) {
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
                    .map(pair -> new Pair<>(pair.getKey().clearDashes(), pair.getValue()))
                    .distinct()
                    .toList();
        }
    }

    public String getCalculativeFDNF() {
        return constructFromList(false, calculativeReduction(false));
    }

    public String getCalculativeFCNF() {
        return constructFromList(true, calculativeReduction(true));
    }

    private List<Term> calculativeReduction(boolean FCNF) {
        List<Term> resultBuffer = FCNF ? bufferedCalculativeFCNF : bufferedCalculativeFDNF;
        if (!resultBuffer.isEmpty())
            return resultBuffer;

        List<Implicant> buffer = FCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        if (buffer.isEmpty()) {
            if (FCNF) {
                getSCNF();
            } else {
                getSDNF();
            }
        }

        List<Term> copy = new ArrayList<>(buffer.stream()
                .map(impl -> impl.reducedTerm(bufferedOperands, true))
                .toList());

        for (int index = 0; index < copy.size(); index++) {
            List<Term> remainder = new ArrayList<>(copy);
            Term currentImplicant = remainder.remove(index);

            String remainderExpression = constructFromList(FCNF, remainder);

            String remainderIndex = new TruthTable(remainderExpression).getIndexForm();
            if (remainderIndex.equals(getIndexForm())) {
                copy.remove(currentImplicant);
            }
        }

        resultBuffer.addAll(copy);

        return resultBuffer;
    }

    public String getQuineMcCluskeyFDNF() {
        return getQuineMcCluskey(false);
    }

    public String getQuineMcCluskeyFCNF() {
        return getQuineMcCluskey(true);
    }

    private String getQuineMcCluskey(boolean FCNF) {
        return constructFromList(FCNF, QuineMcCluskey(FCNF));
    }

    public List<Term> QuineMcCluskey(boolean FCNF) {
        List<Term> resultBuffer = FCNF ? bufferedQMCCFCNF : bufferedQMCCFDNF;
        if (!resultBuffer.isEmpty())
            return resultBuffer;

        List<Implicant> buffer = FCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        List<Term> result = new ArrayList<>();

        for (Implicant implicant : buffer) {
            List<Implicant> areas = buffer
                    .stream()
                    .filter(val -> !val.equals(implicant))
                    .toList();
            for (Integer term : implicant) {
                if (uniqueCoverage(term, areas)) {
                    result.add(implicant.reducedTerm(bufferedOperands, true));
                }
            }
        }

        resultBuffer.addAll(result);

        return resultBuffer;
    }

    public static boolean uniqueCoverage(Integer checking, List<Implicant> toCheck) {
        for (Implicant implicantAreaCheck : toCheck) {
            for (Integer check : implicantAreaCheck) {
                if (checking.equals(check)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String constructFromList(boolean CNF, List<Term> list) {
        StringBuilder result = new StringBuilder();
        for (Term term : list) {
            if (!result.isEmpty() && (list.indexOf(term) != list.size())) {
                result.append(CNF ? " * " : " + ");
            }
            StringBuilder constituent = new StringBuilder().append("(");
            for (ListIterator<String> opIterator = term.listIterator(); opIterator.hasNext(); ) {
                int opIndex = opIterator.nextIndex();
                constituent.append(opIterator.next());
                if (opIndex != (term.size() - 1)) {
                    constituent.append(CNF ? " + " : " * ");
                }
            }
            constituent.append(")");
            result.append(constituent);
        }
        return result.toString();
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