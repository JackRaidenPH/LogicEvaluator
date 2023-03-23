module dev.jackraidenph.logicevaluator {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens dev.jackraidenph.logicevaluator to javafx.fxml;
    exports dev.jackraidenph.logicevaluator;
}