package mx.demo.poslab;

import java.util.*;

public final class Options {
    private static final Set<String> ALLOWED = Set.of("backend", "app", "pid", "out", "python", "max-nodes",
            "max-depth", "max-text", "timeout", "samples", "interval", "name", "role", "id", "path", "text",
            "field", "equals", "contains", "before", "after", "result", "list-only");
    final String command;
    final Map<String, String> values = new LinkedHashMap<>();
    Options(String... args) {
        command = args.length == 0 ? "help" : args[0];
        for (int i = 1; i < args.length; i += 2) {
            if (!args[i].startsWith("--") || !ALLOWED.contains(args[i].substring(2)))
                throw new IllegalArgumentException("Opción desconocida: " + args[i]);
            if (i + 1 >= args.length) throw new IllegalArgumentException("Falta valor para " + args[i]);
            if (values.put(args[i].substring(2), args[i + 1]) != null)
                throw new IllegalArgumentException("Opción repetida: " + args[i]);
        }
        number("max-nodes", 2000, 1, 20000); number("max-depth", 25, 0, 100);
        number("max-text", 2000, 1, 100000); number("timeout", 45, 1, 300);
        number("samples", 3, 2, 20); number("interval", 2000, 100, 60000);
        if (has("pid")) { try { if (Long.parseLong(get("pid", "")) <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("--pid debe ser un entero positivo"); } }
        if (has("app") && get("app", "").isBlank()) throw new IllegalArgumentException("--app no puede estar vacío");
        if (!Set.of("jna", "python", "both").contains(get("backend", "jna")))
            throw new IllegalArgumentException("--backend debe ser jna, python o both");
    }
    String get(String key, String fallback) { return values.getOrDefault(key, fallback); }
    boolean has(String key) { return values.containsKey(key); }
    int number(String key, int fallback, int min, int max) {
        try {
            int n = Integer.parseInt(get(key, Integer.toString(fallback)));
            if (n < min || n > max) throw new NumberFormatException(); return n;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("--" + key + " debe estar entre " + min + " y " + max); }
    }
    int maxNodes() { return number("max-nodes", 2000, 1, 20000); }
    int maxDepth() { return number("max-depth", 25, 0, 100); }
    int maxText() { return number("max-text", 2000, 1, 100000); }
    int timeout() { return number("timeout", 45, 1, 300); }
    boolean target(Model.App app) {
        return (!has("app") || includes(app.name, get("app", "")))
                && (!has("pid") || app.pid == Long.parseLong(get("pid", "0")));
    }
    void requireTarget() {
        if (!has("app") && !has("pid")) throw new IllegalArgumentException("Especifica --app NOMBRE o --pid PID; primero ejecuta list");
    }
    static boolean includes(String value, String fragment) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }
}
