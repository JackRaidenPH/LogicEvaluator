package dev.jackraidenph.logicevaluator.logic;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class KarnaughMap {
    private final boolean[][] matrix;
    private final int[][] grayMatrix;
    private final int binaryScale;
    private final List<String> operands;

    public KarnaughMap(TruthTable table) {
        this.operands = new ArrayList<>(table.getOperands());

        final int size = table.getWidth() - 1;

        final int grayRowLength = (size - size / 2) * 2;
        final int grayColLength = (size / 2) * 2;

        final int maxGray = ((grayRowLength - 1) ^ ((grayRowLength - 1) / 2)) * grayRowLength
                + (grayColLength - 1) ^ ((grayColLength - 1) / 2);
        binaryScale = (int) (Math.floor(Math.log(maxGray) / Math.log(2)) + 1);

        grayMatrix = new int[grayRowLength][grayColLength];

        for (int y = 0; y < grayColLength; y++) {
            for (int x = 0; x < grayRowLength; x++) {
                grayMatrix[y][x] = (x ^ (x / 2)) * grayRowLength
                        + y ^ (y / 2);
            }
        }

        matrix = new boolean[grayRowLength][grayColLength];


        for (int i = 0; i < grayRowLength; i++) {
            for (int j = 0; j < grayColLength; j++) {
                matrix[grayMatrix[i][0]][grayMatrix[j][0]] =
                        table.getContents().get(j * grayRowLength + i).get(size);
            }
        }
    }

    public Pair<Integer, Integer> getMatrixValueOverlap(int x, int y, boolean[][] matrix) {
        return new Pair<>(x % matrix[0].length, y % matrix.length);
    }

    public List<List<Integer>> traverseKMap(boolean ones) {

        final int rows = grayMatrix.length;
        final int cols = grayMatrix[0].length;
        List<List<Integer>> result = new ArrayList<>();
        boolean[][] checked = new boolean[rows][cols];
        final int maxRect = (int) Math.floor(Math.log(Math.max(rows, cols)) / Math.log(2));
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                for (int squareSize = maxRect; squareSize > 0; squareSize--) {
                    int square = (int) Math.pow(2, squareSize);
                    checkMatrixSegment(square, square, i, j, matrix, checked, ones)
                            .ifPresent(result::add);
                }
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int xLevels = maxRect; xLevels >= 0; xLevels--) {
                for (int j = 0; j < cols; j++) {
                    for (int yLevels = maxRect; yLevels >= 0; yLevels--) {
                        if ((xLevels == yLevels) && yLevels != 0)
                            continue;
                        int rectX = (int) Math.pow(2, xLevels);
                        int rectY = (int) Math.pow(2, yLevels);
                        checkMatrixSegment(rectX, rectY, i, j, matrix, checked, ones)
                                .ifPresent(result::add);
                    }
                }
            }
        }

        BiFunction<List<List<Integer>>, List<Integer>, List<List<Integer>>> listWithoutEntry =
                (list, entry) -> list.stream().filter(val -> !val.equals(entry)).toList();
        BiFunction<List<List<Integer>>, List<Integer>, Boolean> hasUnique = (searchIn, list) -> list.stream()
                .anyMatch(i -> TruthTable.uniqueCoverage(i, listWithoutEntry.apply(searchIn, list)));

        return result
                .stream()
                .filter(impl -> hasUnique.apply(result, impl))
                .toList();
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
                    implicant.add(grayMatrix[actualY][actualX]);
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
        return TruthTable.constructFromList(FCNF, result);
    }

    private List<String> primesFromMatrix(List<Integer> numeric, boolean ones) {
        List<List<String>> implicants = TruthTable.implicantMatrixFromNumeric(numeric, operands, ones);
        List<String> prime = new ArrayList<>();
        for (String operand : operands) {
            if (implicants.stream().allMatch(impl -> impl.contains(operand))) {
                prime.add(operand);
            }
            if (implicants.stream().allMatch(impl -> impl.contains("!" + operand))) {
                prime.add("!" + operand);
            }
        }
        return prime;
    }

    @Override
    public String toString() {

        final int rows = grayMatrix.length;
        final int cols = grayMatrix[0].length;

        StringBuilder result = new StringBuilder();

        String scaleFormat = "%" + binaryScale + "s";

        result.append(String.format(scaleFormat, " "));
        for (int i = 0; i < cols; i++) {
            result.append(String.format(scaleFormat, grayMatrix[0][i]));
        }
        result.append("\n");

        for (int i = 0; i < rows; i++) {
            result.append(String.format(scaleFormat, grayMatrix[i][0]));
            for (int j = 0; j < cols; j++) {
                result.append(String.format(scaleFormat, matrix[i][j]));
            }
            result.append("\n");
        }
        return result.toString();
    }
}
