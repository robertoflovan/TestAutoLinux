import java.awt.GridLayout;
import java.lang.management.ManagementFactory;
import javax.swing.*;

/** Ventana independiente con datos ficticios para probar el puente Java ATK. */
public final class PosSwingControl {
    public static void main(String[] args) {
        System.out.println("Java de la prueba: " + System.getProperty("java.version"));
        System.out.println("Runtime: " + System.getProperty("java.home"));
        System.out.println("PID de la prueba: " + ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("POS - Prueba Swing ATK");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            JTextField code = field("CodigoProducto", "1001", true);
            JTextField description = field("DescripcionProducto", "Cafe de prueba", false);
            JTextField total = field("TotalVenta", "125.50", false);
            addRow(panel, "Codigo (1001 o 1002):", code);
            addRow(panel, "Producto:", description);
            addRow(panel, "Total:", total);
            JButton update = new JButton("Consultar producto");
            Runnable refresh = () -> {
                String value = code.getText().trim();
                description.setText("1001".equals(value) ? "Cafe de prueba" : "1002".equals(value) ? "Te de prueba" : "Codigo desconocido");
                total.setText("1001".equals(value) ? "125.50" : "1002".equals(value) ? "80.00" : "0.00");
            };
            code.addActionListener(event -> refresh.run());
            update.addActionListener(event -> refresh.run());
            panel.add(new JLabel("Escribe el codigo y pulsa Enter."));
            panel.add(update);
            window.add(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            code.requestFocusInWindow();
        });
    }

    private static JTextField field(String name, String value, boolean editable) {
        JTextField field = new JTextField(value, 22);
        field.setEditable(editable);
        field.getAccessibleContext().setAccessibleName(name);
        return field;
    }

    private static void addRow(JPanel panel, String text, JTextField field) {
        JLabel label = new JLabel(text);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(field);
    }
}
