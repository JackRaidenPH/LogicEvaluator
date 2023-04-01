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
    private final List<List<String>>/*                       */bufferedQMCCFDNF = new ArrayList<>();
    private final List<List<String>>/*                       */bufferedQMCCFCNF = new ArrayList<>();
    private final List<List<String>>/*                       */bufferedKMap = new ArrayList<>();
    private final List<Integer>/*                            */bufferedGrayRow = new ArrayList<>();
    private final List<Integer>/*                            */bufferedGrayCol = new ArrayList<>();

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

        final int size = getWidth() - 1;
        bufferedGrayRow.addAll(grayCode(size / 2));
        bufferedGrayCol.addAll(grayCode(size - size / 2));
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

        for (Pair<List<String>, List<Integer>> implicant : toReduce) {
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

    public String getCalculativeFDNF() {
        return getCalculativeReduction(false);
    }

    public String getCalculativeFCNF() {
        return getCalculativeReduction(true);
    }

    private String getCalculativeReduction(boolean FCNF) {
        StringBuilder resultBuffer = FCNF ? bufferedStringCalculativeFCNF : bufferedStringCalculativeFDNF;
        if (!resultBuffer.isEmpty())
            return resultBuffer.toString();

        List<List<String>> reduced = calculativeReduction(FCNF);

        StringBuilder result = new StringBuilder();

        for (List<String> implicant : reduced) {
            if (!result.isEmpty() && (reduced.indexOf(implicant) != reduced.size())) {
                result.append(FCNF ? " * " : " + ");
            }
            StringBuilder constituent = new StringBuilder().append("(");
            for (ListIterator<String> opIterator = implicant.listIterator(); opIterator.hasNext(); ) {
                int opIndex = opIterator.nextIndex();
                constituent.append(opIterator.next());
                if (opIndex != (implicant.size() - 1)) {
                    constituent.append(FCNF ? " + " : " * ");
                }
            }
            constituent.append(")");
            result.append(constituent);
        }

        resultBuffer.append(result);

        return resultBuffer.toString();
    }

    private List<List<String>> calculativeReduction(boolean FCNF) {
        List<List<String>> resultBuffer = FCNF ? bufferedCalculativeFCNF : bufferedCalculativeFDNF;
        if (!resultBuffer.isEmpty())
            return resultBuffer;

        List<List<String>> buffer = FCNF ? noMatchInfo(bufferedPCNFPrimes) : noMatchInfo(bufferedPDNFPrimes);

        if (buffer.isEmpty()) {
            if (FCNF) {
                getSCNF();
            } else {
                getSDNF();
            }
        }

        List<List<String>> copy = new ArrayList<>(buffer);

        for (int index = 0; index < buffer.size(); index++) {
            List<List<String>> remainder = new ArrayList<>(buffer);
            List<String> currentImplicant = remainder.remove(index);

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

    public List<List<String>> QuineMcCluskey(boolean FCNF) {
        List<List<String>> resultBuffer = FCNF ? bufferedQMCCFCNF : bufferedQMCCFDNF;
        if (!resultBuffer.isEmpty())
            return resultBuffer;

        List<Pair<List<String>, List<Integer>>> buffer = FCNF ? bufferedPCNFPrimes : bufferedPDNFPrimes;

        List<List<String>> result = new ArrayList<>();

        for (var pair : buffer) {
            List<List<Integer>> areas = buffer
                    .stream()
                    .map(Pair::getValue)
                    .filter(val -> !val.equals(pair.getValue()))
                    .toList();
            for (Integer implicant : pair.getValue()) {
                if (uniqueCoverage(implicant, areas)) {
                    result.add(pair.getKey());
                }
            }
        }

        resultBuffer.addAll(result);

        return resultBuffer;
    }

    private List<List<String>> implicantMatrixFromNumeric(List<Integer> numeric, boolean flip) {
        return numeric
                .stream()
                .map(val -> String.format("%" + countOperands() + "s", Integer.toBinaryString(val)).replaceAll(" ", "0"))
                .map(str -> {
                    List<String> result = new ArrayList<>();
                    for (int i = 0; i < str.length(); i++) {
                        result.add(((str.charAt(i) == '0' == flip) ? "!" : "") + bufferedOperands.get(i));
                    }
                    return result;
                }).toList();
    }

    private boolean uniqueCoverage(Integer checking, List<List<Integer>> toCheck) {
        for (List<Integer> implicantAreaCheck : toCheck) {
            for (Integer check : implicantAreaCheck) {
                if (checking.equals(check)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String constructFromList(boolean CNF, List<List<String>> list) {
        StringBuilder result = new StringBuilder();
        for (List<String> implicant : list) {
            if (!result.isEmpty() && (list.indexOf(implicant) != list.size())) {
                result.append(CNF ? " * " : " + ");
            }
            StringBuilder constituent = new StringBuilder().append("(");
            for (ListIterator<String> opIterator = implicant.listIterator(); opIterator.hasNext(); ) {
                int opIndex = opIterator.nextIndex();
                constituent.append(opIterator.next());
                if (opIndex != (implicant.size() - 1)) {
                    constituent.append(CNF ? " + " : " * ");
                }
            }
            constituent.append(")");
            result.append(constituent);
        }
        return result.toString();
    }

    public List<List<String>> buildKMap() {
        if (!bufferedKMap.isEmpty()) {
            return bufferedKMap;
        }

        final int size = getWidth() - 1;

        int rows = (1 << (size >> 1)); //Basically gets rid of the first bit
        int cols = (1 << size) / rows; //Somehow not the same as (2 * size) / rows

        String[][] result = new String[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[bufferedGrayRow.get(i)][bufferedGrayCol.get(j)] =
                        contents.get(j * rows + i).get(size) ? "1" : "0";
            }
        }

        bufferedKMap.addAll(
                Arrays.stream(result)
                        .map(Arrays::asList)
                        .toList()
        );

        return bufferedKMap;
    }

    private boolean[][] kMapToMatrix() {
        buildKMap();

        boolean[][] matrix = new boolean[bufferedKMap.size()][bufferedKMap.get(0).size()];

        for (ListIterator<List<String>> rowIter = bufferedKMap.listIterator(); rowIter.hasNext(); ) {
            final int rowIndex = rowIter.nextIndex();
            List<String> row = rowIter.next();
            for (ListIterator<String> colIter = row.listIterator(); colIter.hasNext(); ) {
                final int colIndex = colIter.nextIndex();
                String value = colIter.next();
                matrix[rowIndex][colIndex] = value.equals("1");
            }
        }

        return matrix;
    }

    private int[][] grayMatrix() {
        int[][] matrix = new int[bufferedGrayRow.size()][bufferedGrayCol.size()];

        for (int x = 0; x < bufferedGrayCol.size(); x++) {
            for (int y = 0; y < bufferedGrayRow.size(); y++) {
                matrix[y][x] = bufferedGrayCol.get(x) * bufferedGrayRow.size()
                        + bufferedGrayRow.get(y);
            }
        }

        return matrix;
    }

    public Pair<Integer, Integer> getMatrixValueOverlap(int x, int y, boolean[][] matrix) {
        return new Pair<>(x % matrix[0].length, y % matrix.length);
    }

    public List<List<Integer>> traverseKMap(boolean ones) {
        boolean[][] matrixKMap = kMapToMatrix();

        final int rows = bufferedKMap.size();
        final int cols = bufferedKMap.get(0).size();
        List<List<Integer>> result = new ArrayList<>();
        boolean[][] checked = new boolean[rows][cols];
        final int maxRect = (int) Math.floor(Math.log(Math.max(rows, cols)) / Math.log(2));
        for (int i = 0; i < rows; i++) {
            for (int yLevels = maxRect; yLevels >= 0; yLevels--) {
                for (int j = 0; j < cols; j++) {
                    for (int xLevels = maxRect; xLevels >= 0; xLevels--) {
                        int rectX = (int) Math.pow(2, xLevels);
                        int rectY = (int) Math.pow(2, yLevels);
                        checkMatrixSegment(rectX, rectY, i, j, matrixKMap, checked, ones)
                                .ifPresent(result::add);
                    }
                }
            }
        }
        List<List<Integer>> filtered = result
                .stream()
                .filter(impl -> impl
                        .stream()
                        .anyMatch(i -> uniqueCoverage(i, result
                                .stream()
                                .filter(val -> !val.equals(impl))
                                .toList())))
                .toList();

        return filtered;
    }

    private List<String> primesFromMatrix(List<Integer> numeric, boolean ones) {
        List<List<String>> implicants = implicantMatrixFromNumeric(numeric, ones);
        List<String> prime = new ArrayList<>();
        for (String operand : bufferedOperands) {
            if (implicants.stream().allMatch(impl -> impl.contains(operand))) {
                prime.add(operand);
            }
            if (implicants.stream().allMatch(impl -> impl.contains("!" + operand))) {
                prime.add("!" + operand);
            }
        }
        return prime;
    }

    public String getKMapFDNF() {
        return getKMapReduction(false);
    }

    public String getKMapFCNF() {
        return getKMapReduction(true);
    }

    private String getKMapReduction(boolean FCNF) {
        List<List<String>> result = new ArrayList<>();
        for (List<Integer> numerics : traverseKMap(!FCNF)) {
            result.add(primesFromMatrix(numerics, !FCNF));
        }
        return constructFromList(FCNF, result);
    }

    private Optional<List<Integer>> checkMatrixSegment(int width, int height, int yOffset, int xOffset,
                                                       boolean[][] matrix, boolean[][] checked, boolean ones) {
        boolean allChecked = true;
        List<Integer> implicant = new ArrayList<>();
        boolean[][] localChecked = new boolean[checked.length][checked[0].length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Pair<Integer, Integer> coordinates = getMatrixValueOverlap(x + xOffset, y + yOffset, matrix);
                int actualX = coordinates.getKey();
                int actualY = coordinates.getValue();
                allChecked = allChecked & checked[actualY][actualX];
            }
        }

        if (allChecked)
            return Optional.empty();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Pair<Integer, Integer> coordinates = getMatrixValueOverlap(x + xOffset, y + yOffset, matrix);
                int actualX = coordinates.getKey();
                int actualY = coordinates.getValue();
                if ((matrix[actualY][actualX] != ones)) {
                    return Optional.empty();
                }
                if (!localChecked[actualY][actualX])
                    implicant.add(grayMatrix()[actualY][actualX]);
                localChecked[actualY][actualX] = true;
            }
        }

        for (int y = 0; y < localChecked.length; y++) {
            for (int x = 0; x < localChecked[0].length; x++) {
                checked[y][x] |= localChecked[y][x];
            }
        }
        return Optional.of(implicant);
    }

    public String getKMapString() {
        buildKMap();

        final int rows = bufferedKMap.size();
        final int cols = bufferedKMap.get(0).size();

        String[] rowHead = new String[rows];
        String[] colHead = new String[cols];
        for (int i = 0; i < rows; i++)
            rowHead[i] = Integer.toBinaryString(bufferedGrayRow.get(i));
        for (int i = 0; i < cols; i++)
            colHead[i] = Integer.toBinaryString(bufferedGrayCol.get(i));

        StringBuilder result = new StringBuilder();

        result.append(String.format("%8s", " "));
        for (int i = 0; i < cols; i++) {
            result.append(String.format("%8s", colHead[i]));
        }
        result.append("\n");

        for (int i = 0; i < rows; i++) {
            result.append(String.format("%8s", rowHead[i]));
            for (int j = 0; j < cols; j++) {
                result.append(String.format("%8s", bufferedKMap.get(i).get(j)));
            }
            result.append("\n");
        }
        return result.toString();
    }

    private List<Integer> grayCode(int size) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < size * 2; i++) {
            result.add(i ^ (i / 2));
        }
        return result;
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