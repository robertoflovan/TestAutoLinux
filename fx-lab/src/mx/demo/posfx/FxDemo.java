package mx.demo.posfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.geometry.Insets;

public final class FxDemo extends Application {
    @Override public void start(Stage stage) {
        GridPane pane = new GridPane(); pane.setPadding(new Insets(20)); pane.setVgap(12); pane.setHgap(12);
        TextField code = new TextField("1001"); code.setId("CodigoProducto");
        Label product = new Label("Cafe de prueba"); product.setId("DescripcionProducto");
        TextField total = new TextField("125.50"); total.setId("TotalVenta"); total.setEditable(false);
        PasswordField password = new PasswordField(); password.setId("ClaveDemo"); password.setText("NO_EXPORTAR_CLAVE_7391");
        Button button = new Button("Consultar producto");
        Runnable update = () -> {
            boolean second = "1002".equals(code.getText().trim());
            product.setText(second ? "Te de prueba" : "Cafe de prueba"); total.setText(second ? "80.00" : "125.50");
        };
        code.setOnAction(e -> update.run()); button.setOnAction(e -> update.run());
        pane.addRow(0, new Label("Codigo 1001 o 1002:"), code);
        pane.addRow(1, new Label("Producto:"), product);
        pane.addRow(2, new Label("Total:"), total);
        pane.addRow(3, new Label("Clave ficticia (no leer):"), password);
        pane.addRow(4, new Label("Escribe codigo y pulsa Enter"), button);
        // Avoid relying on an automatic initial size on older JavaFX/window managers.
        stage.setTitle("POS - Prueba JavaFX Inspector");
        stage.setScene(new Scene(pane, 640, 360));
        stage.setMinWidth(600); stage.setMinHeight(320);
        stage.show(); stage.centerOnScreen(); code.requestFocus();
        System.out.println("Ejemplo JavaFX abierto: " + stage.getWidth() + " x " + stage.getHeight()
                + " en x=" + stage.getX() + ", y=" + stage.getY());
    }
    public static void main(String[] args) { launch(args); }
}
