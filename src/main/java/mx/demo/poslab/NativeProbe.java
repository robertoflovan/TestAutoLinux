package mx.demo.poslab;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;
import java.util.*;
import java.util.function.Function;
import static mx.demo.poslab.Model.*;

/** Enlace de sólo lectura a libatspi; se ejecuta en un proceso aislado. */
public final class NativeProbe {
    public interface Atspi extends Library {
        int atspi_init(); int atspi_exit(); void atspi_set_timeout(int timeout, int startup);
        Pointer atspi_get_desktop(int index);
        Pointer atspi_accessible_get_name(Pointer p, PointerByReference error);
        Pointer atspi_accessible_get_description(Pointer p, PointerByReference error);
        Pointer atspi_accessible_get_role_name(Pointer p, PointerByReference error);
        Pointer atspi_accessible_get_accessible_id(Pointer p, PointerByReference error);
        Pointer atspi_accessible_get_toolkit_name(Pointer p, PointerByReference error);
        int atspi_accessible_get_process_id(Pointer p, PointerByReference error);
        int atspi_accessible_get_child_count(Pointer p, PointerByReference error);
        Pointer atspi_accessible_get_child_at_index(Pointer p, int index, PointerByReference error);
        Pointer atspi_accessible_get_state_set(Pointer p);
        int atspi_state_set_contains(Pointer p, int state);
        Pointer atspi_accessible_get_text_iface(Pointer p);
        int atspi_text_get_character_count(Pointer p, PointerByReference error);
        Pointer atspi_text_get_text(Pointer p, int start, int end, PointerByReference error);
        Pointer atspi_accessible_get_value_iface(Pointer p);
        double atspi_value_get_current_value(Pointer p, PointerByReference error);
        Pointer atspi_accessible_get_action_iface(Pointer p);
        int atspi_action_get_n_actions(Pointer p, PointerByReference error);
        Pointer atspi_action_get_action_name(Pointer p, int index, PointerByReference error);
    }
    public interface GLib extends Library { void g_free(Pointer p); void g_error_free(Pointer p); }
    public interface GObject extends Library { void g_object_unref(Pointer p); }
    @Structure.FieldOrder({"domain", "code", "message"})
    public static class GError extends Structure {
        public int domain, code; public Pointer message;
        public GError(Pointer p) { super(p); read(); }
    }
    private final Atspi lib;
    private final GLib glib;
    private final GObject objects;
    private final Options options;
    private final Snapshot snapshot = new Snapshot();
    private final long deadline;
    private static final class Entry {
        final Pointer pointer;
        final String path;
        final int depth;
        final App app;
        Entry(Pointer pointer, String path, int depth, App app) {
            this.pointer = pointer; this.path = path; this.depth = depth; this.app = app;
        }
    }
    private static final Map<Integer, String> STATES = new LinkedHashMap<>();
    static {
        STATES.put(1,"active"); STATES.put(3,"busy"); STATES.put(4,"checked"); STATES.put(6,"defunct");
        STATES.put(7,"editable"); STATES.put(8,"enabled"); STATES.put(11,"focusable"); STATES.put(12,"focused");
        STATES.put(23,"selected"); STATES.put(24,"sensitive"); STATES.put(25,"showing"); STATES.put(30,"visible");
        STATES.put(31,"manages_descendants"); STATES.put(43,"read_only");
    }
    private NativeProbe(Options options) {
        this.options = options;
        lib = Native.load("atspi", Atspi.class);
        glib = Native.load("glib-2.0", GLib.class);
        objects = Native.load("gobject-2.0", GObject.class);
        deadline = System.nanoTime() + Math.max(1, options.timeout() - 3) * 1_000_000_000L;
        snapshot.backend = "jna";
    }
    static Snapshot capture(Options options) {
        if (!Platform.isLinux()) return Snapshot.failure("jna", "Esta vía requiere Linux; el equipo actual es " + System.getProperty("os.name"));
        try { return new NativeProbe(options).run(); }
        catch (Exception | LinkageError e) { return Snapshot.failure("jna", e.toString()); }
    }
    private Snapshot run() {
        boolean initialized = false;
        try {
            int status = lib.atspi_init();
            if (status > 1) throw new IllegalStateException("atspi_init devolvió " + status);
            initialized = true; lib.atspi_set_timeout(800, 1500);
            Pointer desktop = lib.atspi_get_desktop(0);
            if (desktop == null) throw new IllegalStateException("AT-SPI no devolvió un escritorio. Verifica sesión gráfica y D-Bus.");
            try {
                int count = integer("desktop.children", snapshot.warnings, err -> lib.atspi_accessible_get_child_count(desktop, err), -1);
                if (count < 0) throw new IllegalStateException("No se pudo consultar el registro AT-SPI: " + snapshot.warnings);
                for (int i = 0; i < Math.min(count, 512); i++) {
                    if (expired()) break;
                    final int index = i;
                    Pointer root = call("desktop.child[" + i + "]", snapshot.warnings,
                            err -> lib.atspi_accessible_get_child_at_index(desktop, index, err), null);
                    if (root == null) continue;
                    try {
                        App app = new App(); app.desktopIndex = i;
                        app.name = string("name", app.errors, err -> lib.atspi_accessible_get_name(root, err));
                        app.pid = Integer.toUnsignedLong(integer("pid", app.errors, err -> lib.atspi_accessible_get_process_id(root, err), 0));
                        app.toolkit = string("toolkit", app.errors, err -> lib.atspi_accessible_get_toolkit_name(root, err));
                        snapshot.apps.add(app);
                        app.selected = options.target(app);
                        if (app.selected) {
                            snapshot.matchedApps++;
                            if (!options.has("list-only")) traverse(root, app);
                        }
                    } finally { objects.g_object_unref(root); }
                }
                if (count > 512) { snapshot.truncated = true; snapshot.warnings.add("Más de 512 aplicaciones: inventario limitado"); }
            } finally { objects.g_object_unref(desktop); }
            snapshot.ok = true;
            if (snapshot.apps.isEmpty()) snapshot.warnings.add("Registro vacío: no demuestra que el POS sea inaccesible. Comprueba sesión y control GTK.");
        } finally { if (initialized) lib.atspi_exit(); }
        return snapshot;
    }
    private boolean expired() {
        if (System.nanoTime() < deadline) return false;
        snapshot.truncated = true;
        if (!snapshot.warnings.contains("Tiempo de exploración agotado")) snapshot.warnings.add("Tiempo de exploración agotado");
        return true;
    }
    private void traverse(Pointer root, App app) {
        // root pertenece a run(); los descendientes de la cola tienen referencias propias.
        Deque<Entry> queue = new ArrayDeque<>();
        queue.add(new Entry(root, "app[" + app.desktopIndex + "]", 0, app));
        try {
            while (!queue.isEmpty()) {
                if (expired() || snapshot.nodes.size() >= options.maxNodes()) { snapshot.truncated = true; break; }
                Entry entry = queue.removeFirst();
                try {
                    Node node = read(entry); snapshot.nodes.add(node);
                    if (entry.depth >= options.maxDepth()) {
                        if (node.childCount > 0) snapshot.truncated = true;
                        continue;
                    }
                    int available = options.maxNodes() - snapshot.nodes.size() - queue.size();
                    int take = Math.min(Math.max(0, node.childCount), Math.max(0, available));
                    if (take < node.childCount) snapshot.truncated = true;
                    for (int i = 0; i < take; i++) {
                        if (expired()) break;
                        final int index = i;
                        Pointer child = call("child[" + i + "]", node.errors,
                                err -> lib.atspi_accessible_get_child_at_index(entry.pointer, index, err), null);
                        if (child != null) queue.addLast(new Entry(child, entry.path + "/" + i, entry.depth + 1, app));
                        else { snapshot.truncated = true; }
                    }
                } finally { if (entry.depth != 0) objects.g_object_unref(entry.pointer); }
            }
        } finally { for (Entry entry : queue) if (entry.depth != 0) objects.g_object_unref(entry.pointer); }
    }
    private Node read(Entry entry) {
        Pointer p = entry.pointer;
        Node n = new Node(); n.path = entry.path; n.depth = entry.depth; n.app = entry.app.name; n.pid = entry.app.pid;
        n.name = string("name", n.errors, err -> lib.atspi_accessible_get_name(p, err));
        n.role = string("role", n.errors, err -> lib.atspi_accessible_get_role_name(p, err));
        n.accessibleId = string("accessibleId", n.errors, err -> lib.atspi_accessible_get_accessible_id(p, err));
        n.description = string("description", n.errors, err -> lib.atspi_accessible_get_description(p, err));
        n.childCount = integer("children", n.errors, err -> lib.atspi_accessible_get_child_count(p, err), -1);
        n.protectedText = n.role.toLowerCase(Locale.ROOT).contains("password");
        Pointer states = lib.atspi_accessible_get_state_set(p);
        if (states != null) {
            try { STATES.forEach((value, name) -> { if (lib.atspi_state_set_contains(states, value) != 0) n.states.add(name); }); }
            finally { objects.g_object_unref(states); }
        }
        Pointer text = lib.atspi_accessible_get_text_iface(p);
        n.textSupported = text != null;
        if (text != null) {
            try {
                if (!n.protectedText) {
                    int count = integer("characterCount", n.errors, err -> lib.atspi_text_get_character_count(text, err), -1);
                    if (count >= 0) {
                        n.characterCount = count; n.textTruncated = count > options.maxText();
                        n.text = call("text", n.errors, err -> utf8(lib.atspi_text_get_text(text, 0, Math.min(count, options.maxText()), err)), null);
                    }
                }
            } finally { objects.g_object_unref(text); }
        }
        Pointer value = lib.atspi_accessible_get_value_iface(p);
        n.valueSupported = value != null;
        if (value != null) {
            try {
                if (!n.protectedText) {
                    Double number = call("value", n.errors, err -> lib.atspi_value_get_current_value(value, err), null);
                    if (number != null && Double.isFinite(number)) n.value = number;
                    else if (number != null) n.errors.add("value: valor no finito");
                }
            } finally { objects.g_object_unref(value); }
        }
        Pointer action = lib.atspi_accessible_get_action_iface(p);
        if (action != null) {
            try {
                int count = integer("actions", n.errors, err -> lib.atspi_action_get_n_actions(action, err), 0);
                for (int i = 0; i < Math.min(count, 16); i++) {
                    final int index = i;
                    n.actions.add(string("action[" + i + "]", n.errors, err -> lib.atspi_action_get_action_name(action, index, err)));
                }
            } finally { objects.g_object_unref(action); }
        }
        return n;
    }
    private <T> T call(String field, List<String> errors, Function<PointerByReference, T> function, T fallback) {
        PointerByReference error = new PointerByReference();
        try {
            T value = function.apply(error);
            if (error.getValue() != null) {
                GError detail = new GError(error.getValue());
                errors.add(field + ": " + (detail.message == null ? "GError " + detail.code : detail.message.getString(0, "UTF-8")));
                return fallback;
            }
            return value;
        } catch (Exception | LinkageError e) { errors.add(field + ": " + e.getMessage()); return fallback; }
        finally { if (error.getValue() != null) glib.g_error_free(error.getValue()); }
    }
    private int integer(String field, List<String> errors, Function<PointerByReference, Integer> f, int fallback) {
        return call(field, errors, f, fallback);
    }
    private String string(String field, List<String> errors, Function<PointerByReference, Pointer> function) {
        // Convertir/liberar antes de comprobar GError evita filtrar la cadena si el proveedor devolviese ambos.
        return call(field, errors, err -> {
            Pointer p = function.apply(err);
            if (p == null) return "";
            try { return p.getString(0, "UTF-8"); } finally { glib.g_free(p); }
        }, "");
    }
    private String utf8(Pointer p) {
        if (p == null) return null;
        try { return p.getString(0, "UTF-8"); } finally { glib.g_free(p); }
    }
}
