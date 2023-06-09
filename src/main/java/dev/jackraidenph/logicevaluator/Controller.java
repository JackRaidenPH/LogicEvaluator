package dev.jackraidenph.logicevaluator;

import dev.jackraidenph.logicevaluator.logic.KarnaughMap;
import dev.jackraidenph.logicevaluator.logic.TruthTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class Controller implements Initializable {

    @FXML
    private Button evaluateButton;

    @FXML
    private Label indexRes;

    @FXML
    private Label numericPCNFRes;

    @FXML
    private Label numericPDNFRes;

    @FXML
    private Label pcnfRes;

    @FXML
    private Label pdnfRes;

    @FXML
    private Label scnfRes;

    @FXML
    private Label sdnfRes;

    @FXML
    private Label cfcnfRes;

    @FXML
    private Label cfdnfRes;

    @FXML
    private Label qmccfcnfRes;

    @FXML
    private Label qmccfdnfRes;

    @FXML
    private Label kmapFDNFRes;

    @FXML
    private Label kmapFCNFRes;

    @FXML
    private ChoiceBox<String> presetDroplist;

    @FXML
    private TableView<List<Boolean>> truthTableView;

    @FXML
    private TextField inputField;

    @FXML
    void onEvaluate(ActionEvent event) {
        if (inputField.getText().isEmpty())
            return;

        truthTableView.getItems().clear();
        truthTableView.getColumns().clear();

        TruthTable truthTable = new TruthTable(inputField.getText());

        List<String> names = new ArrayList<>(truthTable.getOperands());
        names.add(inputField.getText());
        setColumns(truthTable, names);
        setOutputs(truthTable);
    }

    private static final Map<String, TruthTable> PRESETS = new HashMap<>() {{
        put("SUM_VAL", new TruthTable("(A^B)^C"));
        put("SUM_CARRY", new TruthTable("(A*B)+(C*(A^B))"));
        put("8421E9O8", new TruthTable(
                List.of(new String[]{"A", "B", "C", "D"}), new ArrayList<>() {{
            add(List.of(false, false, false, false, true));
            add(List.of(false, false, false, true, true));
            add(List.of(false, false, true, false, true));
            add(List.of(false, false, true, true, true));
            add(List.of(false, true, false, false, true));
            add(List.of(false, true, false, true, true));
            add(List.of(false, true, true, false, true));
        }}));
        put("8421E9O4", new TruthTable(
                List.of(new String[]{"A", "B", "C", "D"}), new ArrayList<>() {{
            add(List.of(false, false, false, false, false));
            add(List.of(false, false, false, true, false));
            add(List.of(false, false, true, false, false));
            add(List.of(false, false, true, true, true));
            add(List.of(false, true, false, false, true));
            add(List.of(false, true, false, true, true));
            add(List.of(false, true, true, false, true));
        }}));
        put("8421E9O2", new TruthTable(
                List.of(new String[]{"A", "B", "C", "D"}), new ArrayList<>() {{
            add(List.of(false, false, false, false, false));
            add(List.of(false, false, false, true, true));
            add(List.of(false, false, true, false, true));
            add(List.of(false, false, true, true, false));
            add(List.of(false, true, false, false, false));
            add(List.of(false, true, false, true, true));
            add(List.of(false, true, true, false, true));
        }}));
        put("8421E9O1", new TruthTable(
                List.of(new String[]{"A", "B", "C", "D"}), new ArrayList<>() {{
            add(List.of(false, false, false, false, true));
            add(List.of(false, false, false, true, false));
            add(List.of(false, false, true, false, true));
            add(List.of(false, false, true, true, false));
            add(List.of(false, true, false, false, true));
            add(List.of(false, true, false, true, false));
            add(List.of(false, true, true, false, true));
        }}));
        put("16SUBH4", new TruthTable(List.of(new String[]{"q4p", "q3p", "q2p", "q1p", "V"}),
                makeTable(0)));
        put("16SUBH3", new TruthTable(List.of(new String[]{"q4p", "q3p", "q2p", "q1p", "V"}),
                makeTable(1)));
        put("16SUBH2", new TruthTable(List.of(new String[]{"q4p", "q3p", "q2p", "q1p", "V"}),
                makeTable(2)));
        put("16SUBH1", new TruthTable(List.of(new String[]{"q4p", "q3p", "q2p", "q1p", "V"}),
                makeTable(3)));
    }};

    private static boolean[] decrement(boolean[] subtractFrom) {
        boolean v1, v2, v3, B = false;
        boolean[] X = new boolean[4];
        boolean[] Y = new boolean[4];
        System.arraycopy(subtractFrom, 0, X, 0, 4);
        System.arraycopy(new boolean[]{true, false, false, false}, 0, Y, 0, 4);
        for (int i = 0; i < 4; i++) {
            v1 = X[i] ^ Y[i];
            v2 = !X[i] & Y[i];
            v3 = !v1 & B;
            X[i] = v1 ^ B;
            B = v2 | v3;
        }
        return X;
    }

    public static boolean[] magnitudeToBinary(Integer integer) {
        integer = Math.abs(integer);
        boolean[] out = new boolean[4];
        for (int i = 0; i < 4; i++)
            out[i] = (integer & (1 << i)) != 0;
        return out;
    }

    public static List<List<Boolean>> makeTable(int h) {
        List<List<Boolean>> table = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            for (boolean bool : new boolean[]{false, true}) {
                List<Boolean> row = new ArrayList<>();

                boolean[] prev = magnitudeToBinary(i);
                for (boolean qtp : prev) {
                    row.add(qtp);
                }
                Collections.reverse(row);

                row.add(bool);

                for (boolean qtp : (bool ? decrement(prev) : prev)) {
                    row.add(qtp);
                }
                Collections.reverse(row.subList(5, 9));

                row.add((row.get(h) != row.get(h + 5)) && bool);

                row.remove(5);
                row.remove(5);
                row.remove(5);
                row.remove(5);

                table.add(row);
            }
        }
        return table;
    }

    private void setColumns(TruthTable truthTable, List<String> names) {
        int truthTableWidth = truthTable.getWidth();

        for (int columnId = 0; columnId < truthTableWidth; columnId++) {
            String name = names.get(columnId);
            TableColumn<List<Boolean>, String> column = new TableColumn<>(name);
            column.setSortable(false);
            int finalCID = columnId;
            column.setCellValueFactory(data ->
                    new ReadOnlyObjectWrapper<>(data.getValue().get(finalCID) ? "1" : "0"));
            column.prefWidthProperty().bind(truthTableView.widthProperty().divide(truthTableWidth).subtract(4));
            customiseFactory(column);
            truthTableView.getColumns().add(column);
        }

        for (List<Boolean> row : truthTable.getContents()) {
            truthTableView.getItems().add(row);
        }
    }

    private void setOutputs(TruthTable truthTable) {
        pdnfRes.setText("PDNF: " + truthTable.getPDNF());
        pcnfRes.setText("PCNF: " + truthTable.getPCNF());
        numericPDNFRes.setText("Numeric PDNF: " + truthTable.getNumericPDNF());
        numericPCNFRes.setText("Numeric PCNF: " + truthTable.getNumericPCNF());
        indexRes.setText("Index form: " + truthTable.getIndexForm());
        sdnfRes.setText("SDNF form: " + truthTable.getSDNF());
        scnfRes.setText("SCNF form: " + truthTable.getSCNF());
        cfdnfRes.setText("Calculated FDNF form: " + truthTable.getCalculativeFDNF());
        cfcnfRes.setText("Calculated FCNF form: " + truthTable.getCalculativeFCNF());
        if (!truthTable.getExpression().isBlank()) {
            qmccfdnfRes.setText("Quine-McCluskey FDNF form: " + truthTable.getQuineMcCluskeyFDNF());
            qmccfcnfRes.setText("Quine-McCluskey FCNF form: " + truthTable.getQuineMcCluskeyFCNF());
            KarnaughMap kmap = new KarnaughMap(truthTable);
            kmapFDNFRes.setText("Karnaugh Map FDNF: " + kmap.getKMapFDNF());
            kmapFCNFRes.setText("Karnaugh Map FCNF: " + kmap.getKMapFCNF());
        }
    }

    private void customiseFactory(TableColumn<List<Boolean>, String> column) {
        column.setCellFactory(cell -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : getItem());
                if (!empty) {
                    setAlignment(Pos.CENTER);
                    setBackground(Background.fill(item.equals("1") ? Color.GREEN : Color.RED));
                }
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        presetDroplist.setValue("None");
        presetDroplist.getItems().add("None");
        presetDroplist.getItems().addAll(PRESETS.keySet());
        presetDroplist.setOnAction(this::onPresetChoice);
    }

    @FXML
    private void onPresetChoice(ActionEvent event) {
        truthTableView.getItems().clear();
        truthTableView.getColumns().clear();

        String choice = presetDroplist.getValue();
        if (PRESETS.containsKey(choice)) {
            evaluateButton.setDisable(true);
            inputField.setDisable(true);

            TruthTable truthTable = PRESETS.get(choice);
            inputField.setText(truthTable.getExpression());

            List<String> names = new ArrayList<>(truthTable.getOperands());
            names.add(choice);
            setColumns(truthTable, names);
            //try {
            setOutputs(truthTable);
            //} catch (ArrayIndexOutOfBoundsException exception) {
            //System.out.println("Can't construct K-Map for partial Truth Table!");
            //}
        } else {
            evaluateButton.setDisable(false);
            inputField.setDisable(false);
            inputField.setText("");
        }
    }
}
