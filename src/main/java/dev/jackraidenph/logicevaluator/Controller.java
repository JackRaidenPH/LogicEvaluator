package dev.jackraidenph.logicevaluator;

import dev.jackraidenph.logicevaluator.logic.TruthTable;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.*;

public class Controller {

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
    private Label kmapRes;

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
        int truthTableWidth = truthTable.getWidth();

        for (int columnId = 0; columnId < truthTableWidth; columnId++) {
            String name = columnId < truthTableWidth - 1
                    ? truthTable.getOperands().get(columnId)
                    : inputField.getText();
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

        pdnfRes.setText("PDNF: " + truthTable.getPDNF());
        pcnfRes.setText("PCNF: " + truthTable.getPCNF());
        numericPDNFRes.setText("Numeric PDNF: " + truthTable.getNumericPDNF());
        numericPCNFRes.setText("Numeric PCNF: " + truthTable.getNumericPCNF());
        indexRes.setText("Index form: " + truthTable.getIndexForm());
        sdnfRes.setText("SDNF form: " + truthTable.getSDNF());
        scnfRes.setText("SCNF form: " + truthTable.getSCNF());
        cfdnfRes.setText("Calculated FDNF form: " + truthTable.getCalculativeFDNF());
        cfcnfRes.setText("Calculated FCNF form: " + truthTable.getCalculativeFCNF());
        qmccfdnfRes.setText("Quine-McCluskey FDNF form: " + truthTable.getQuineMcCluskeyFDNF());
        qmccfcnfRes.setText("Quine-McCluskey FCNF form: " + truthTable.getQuineMcCluskeyFCNF());
        kmapRes.setText("Karnaugh Map:\n" + truthTable.getKMapString());
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
}
