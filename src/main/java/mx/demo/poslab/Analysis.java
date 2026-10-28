package mx.demo.poslab;

import java.util.*;
import java.util.stream.Collectors;
import static mx.demo.poslab.Model.*;

final class Analysis {
    static List<Node> select(Snapshot snapshot, Options o) {
        return snapshot.nodes.stream().filter(n ->
                (!o.has("name") || Options.includes(n.name, o.get("name", ""))) &&
                (!o.has("role") || Options.includes(n.role, o.get("role", ""))) &&
                (!o.has("text") || Options.includes(n.text, o.get("text", ""))) &&
                (!o.has("id") || Objects.equals(n.accessibleId, o.get("id", ""))) &&
                (!o.has("path") || Objects.equals(n.path, o.get("path", "")))).toList();
    }
    static String field(Node n, String field) {
        return switch (field) {
            case "text" -> n.text; case "name" -> n.name;
            case "value" -> n.value == null ? null : n.value.toString();
            default -> throw new IllegalArgumentException("--field debe ser text, name o value");
        };
    }
    record Check(boolean passed, String reason, int matches, String actual) {}
    static Check check(Snapshot s, Options o) {
        String field = o.get("field", "text");
        if (!Set.of("text", "name", "value").contains(field)) throw new IllegalArgumentException("--field inválido");
        if (o.has("equals") == o.has("contains")) throw new IllegalArgumentException("Usa exactamente una de --equals o --contains");
        if (!s.ok) return new Check(false, "Error de infraestructura: " + s.error, 0, null);
        List<Node> matches = select(s, o);
        if (s.matchedApps != 1) return new Check(false, "Se requiere exactamente una aplicación; usa --pid", matches.size(), null);
        if (matches.size() != 1) return new Check(false, "Se requiere exactamente un elemento; precisa --id, --role o --path", matches.size(), null);
        Node n = matches.get(0);
        if (s.truncated || n.textTruncated || !n.errors.isEmpty())
            return new Check(false, "Lectura incompleta o con errores; revisa límites y errores del elemento", 1, null);
        String actual = field(n, field);
        if (actual == null || n.protectedText) return new Check(false, "Propiedad no disponible/protegida", 1, null);
        boolean pass = o.has("equals") ? actual.equals(o.get("equals", "")) : actual.contains(o.get("contains", ""));
        return new Check(pass, pass ? "Validación correcta" : "El valor no coincide", 1, actual);
    }
    static Map<String, Object> summary(Snapshot s) {
        Map<String, Object> result = new LinkedHashMap<>();
        long errors = s.nodes.stream().filter(n -> !n.errors.isEmpty()).count();
        long texts = s.nodes.stream().filter(n -> n.text != null).count();
        long names = s.nodes.stream().filter(n -> n.depth > 0 && !n.name.isBlank()).count();
        long ids = s.nodes.stream().filter(n -> !n.accessibleId.isBlank()).count();
        long values = s.nodes.stream().filter(n -> n.value != null).count();
        long meaningful = s.nodes.stream().filter(n -> n.depth > 0 &&
                ((!n.name.isBlank() && !container(n.role)) || (n.text != null && !n.text.isEmpty()) || n.value != null)).count();
        boolean partial = s.truncated || errors > 0 || s.nodes.stream().anyMatch(n -> n.textTruncated)
                || !s.warnings.isEmpty() || s.apps.stream().anyMatch(a -> a.selected && !a.errors.isEmpty());
        String verdict = !s.ok ? "ERROR_DE_ENTORNO" : s.matchedApps == 0 ? "APLICACION_NO_ENCONTRADA"
                : s.matchedApps > 1 ? "OBJETIVO_AMBIGUO" : partial ? "LECTURA_PARCIAL"
                : meaningful > 0 ? "ELEMENTOS_LEIBLES" : "SOLO_CONTENEDORES_O_SIN_CONTENIDO";
        result.put("backend", s.backend); result.put("veredicto", verdict); result.put("aplicacionesObjetivo", s.matchedApps);
        result.put("elementos", s.nodes.size()); result.put("nombresDescendientes", names); result.put("lecturasTexto", texts);
        result.put("valoresNumericos", values); result.put("ids", ids); result.put("elementosConErrores", errors);
        result.put("truncado", s.truncated); result.put("error", s.error);
        result.put("interpretacion", "Encontrar elementos no garantiza que todos los campos del POS sean accesibles. Valida producto, total y mensajes con valores conocidos.");
        return result;
    }
    private static boolean container(String role) {
        return Set.of("application", "frame", "window", "panel", "root pane", "layered pane", "filler", "desktop frame").contains(role);
    }
    static List<Map<String, Object>> diff(Snapshot before, Snapshot after) {
        Map<String, Node> a = before.nodes.stream().collect(Collectors.toMap(n -> n.path, n -> n, (x,y) -> x, LinkedHashMap::new));
        Map<String, Node> b = after.nodes.stream().collect(Collectors.toMap(n -> n.path, n -> n, (x,y) -> x, LinkedHashMap::new));
        Set<String> paths = new LinkedHashSet<>(a.keySet()); paths.addAll(b.keySet());
        List<Map<String, Object>> changes = new ArrayList<>();
        for (String path : paths) {
            Node x = a.get(path), y = b.get(path);
            if (x == null || y == null || !fingerprint(x).equals(fingerprint(y))) {
                Map<String, Object> change = new LinkedHashMap<>(); change.put("path", path);
                change.put("tipo", x == null ? "agregado" : y == null ? "eliminado" : "cambiado");
                change.put("antes", x); change.put("despues", y); changes.add(change);
            }
        }
        return changes;
    }
    private static List<Object> fingerprint(Node n) {
        return Arrays.asList(n.pid, n.app, n.name, n.role, n.accessibleId, n.text, n.value, n.states, n.childCount);
    }
}
