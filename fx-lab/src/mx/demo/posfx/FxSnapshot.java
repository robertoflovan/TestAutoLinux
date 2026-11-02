package mx.demo.posfx;

import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.*;
import java.lang.reflect.Method;
import java.util.*;

final class FxSnapshot {
    private final Properties data = new Properties();
    private int count;
    private boolean truncated;
    private final long deadline = System.nanoTime() + 200_000_000L;

    static Properties capture() throws Exception {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("Lectura fuera del hilo JavaFX");
        return new FxSnapshot().read();
    }
    private Properties read() throws Exception {
        // Java 8 exposes impl_getWindows, an internal/deprecated API. Later FX uses getWindows.
        Method registry;
        try { registry = Window.class.getMethod("getWindows"); }
        catch (NoSuchMethodException e) { registry = Window.class.getMethod("impl_getWindows"); }
        Object value = registry.invoke(null);
        Iterator<?> windows = value instanceof Iterable ? ((Iterable<?>) value).iterator() : (Iterator<?>) value;
        int w = 0;
        while (windows.hasNext()) {
            if (w >= 100 || limited()) { truncated = true; break; }
            Window window = (Window) windows.next();
            if (!window.isShowing() || window.getScene() == null) continue;
            String title = window instanceof Stage ? ((Stage) window).getTitle() : window.getClass().getSimpleName();
            put("window." + w + ".title", title);
            visit(window.getScene().getRoot(), "window[" + w + "]/root", 0, true, title);
            w++;
        }
        put("status", "OK"); put("capturedAt", System.currentTimeMillis());
        put("windowCount", w); put("nodeCount", count); put("truncated", truncated);
        put("windowRegistry", registry.getName());
        put("note", "Solo arbol de escenas visible. Canvas/WebView y filas virtualizadas pueden requerir otra via. IDs pueden repetirse.");
        return data;
    }
    private boolean limited() { return count >= 1000 || System.nanoTime() >= deadline; }
    private void visit(Node node, String path, int depth, boolean ancestorsVisible, String window) {
        if (limited() || depth > 25) { truncated = true; return; }
        boolean visible = ancestorsVisible && node.isVisible() && node.getOpacity() > 0;
        if (!visible) return;
        String prefix = "node." + count++ + ".";
        put(prefix + "type", node.getClass().getName()); put(prefix + "id", node.getId());
        put(prefix + "path", path); put(prefix + "window", window);
        put(prefix + "disabled", node.isDisabled());
        // Never read the password or descend into its skin/text nodes.
        if (node instanceof PasswordField) { put(prefix + "protected", true); return; }
        put(prefix + "protected", false);
        put(prefix + "accessibleText", node.getAccessibleText());
        put(prefix + "role", node.getAccessibleRole());
        String text = null;
        if (node instanceof TextInputControl) text = ((TextInputControl) node).getText();
        else if (node instanceof Labeled) text = ((Labeled) node).getText();
        else if (node instanceof Text) text = ((Text) node).getText();
        if (text != null) {
            put(prefix + "textTruncated", text.length() > 2000);
            put(prefix + "text", text);
        }
        if (node instanceof Parent) {
            int child = 0;
            for (Node n : ((Parent) node).getChildrenUnmodifiable()) {
                if (limited()) { truncated = true; break; }
                visit(n, path + "/" + child++, depth + 1, visible, window);
            }
        }
    }
    private void put(String key, Object value) {
        String text = value == null ? "" : value.toString();
        if (text.length() > 2000) { text = text.substring(0, 2000); truncated = true; }
        data.setProperty(key, text);
    }
}
