package dev.jackraidenph.logicevaluator.logic;

import javafx.util.Pair;

import java.util.*;
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

        for (int y = 0; y < grayRowLength; y++) {
            for (int x = 0; x < grayColLength; x++) {
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

    public List<Implicant> traverseKMap(boolean ones) {

        final int rows = grayMatrix.length;
        final int cols = grayMatrix[0].length;
        List<Implicant> result = new ArrayList<>();
        boolean[][] checked = new boolean[rows][cols];
        final int maxRect = (int) Math.floor(Math.log(Math.max(rows, cols)) / Math.log(2));

        List<Pair<Integer, Integer>> areas = new ArrayList<>();
        for (int xLevels = maxRect; xLevels >= 0; xLevels--) {
            for (int yLevels = maxRect; yLevels >= 0; yLevels--) {
                int rectX = (int) Math.pow(2, xLevels);
                int rectY = (int) Math.pow(2, yLevels);
                areas.add(new Pair<>(rectX, rectY));
            }
        }

        areas.sort(Comparator.comparing((Pair<Integer, Integer> p) -> {
            int max = Math.max(p.getKey(), p.getValue());
            int min = Math.min(p.getKey(), p.getValue());
            double distance = Math.sqrt(min * min + max * max);
            return (max * min) / distance;
        }).reversed());

        segmentIteration:
        for (Pair<Integer, Integer> dimensions : areas) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    checkMatrixSegment(dimensions.getKey(), dimensions.getValue(), i, j, matrix, checked, ones)
                            .ifPresent(result::add);
                    if (Arrays.deepEquals(checked, matrix))
                        break segmentIteration;
                }
            }
        }

        return result
                .stream()
                .distinct()
                .filter(impl -> impl.isEssential(result))
                .toList();
    }

    private Optional<Implicant> checkMatrixSegment(int width, int height, int yOffset, int xOffset,
                                                   boolean[][] matrix, boolean[][] checked, boolean ones) {
        boolean allChecked = true;
        Implicant implicant = new Implicant();
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
        List<Term> result = new ArrayList<>();
        for (Implicant numerics : traverseKMap(!FCNF)) {
            result.add(numerics.reducedTerm(operands, !FCNF));
        }
        return TruthTable.constructFromList(FCNF, result);
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
