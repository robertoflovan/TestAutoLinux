package mx.demo.poslab;

import java.awt.GraphicsEnvironment;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static mx.demo.poslab.Model.*;

public final class Main {
    public static void main(String[] args) {
        try { System.exit(run(new Options(args))); }
        catch (IllegalArgumentException e) { System.err.println("Uso inválido: " + e.getMessage() + "\nEjecuta: java -jar pos-accessibility-lab.jar help"); System.exit(2); }
        catch (Exception e) { System.err.println("Error: " + e.getMessage()); System.exit(2); }
    }
    static int run(Options o) throws Exception {
        if (Compat.set("help", "--help", "-h").contains(o.command)) { help(); return 0; }
        if (o.command.equals("internal-jna")) {
            Snapshot s = NativeProbe.capture(o); Json.write(java.nio.file.Paths.get(o.get("result", "native-result.json")), s); return s.ok ? 0 : 2;
        }
        if (!Compat.set("doctor", "list", "inspect", "suite", "find", "check", "watch", "diff").contains(o.command))
            throw new IllegalArgumentException("Comando desconocido: " + o.command);
        if (Compat.set("inspect", "suite", "find", "check", "watch").contains(o.command)) o.requireTarget();
        String backend = o.get("backend", o.command.equals("suite") || o.command.equals("doctor") ? "both" : "jna");
        if (backend.equals("both") && !Compat.set("suite", "doctor", "list").contains(o.command))
            throw new IllegalArgumentException("Usa --backend jna o python para este comando");
        if (o.command.equals("check")) {
            // Validar la expresión antes de leer aplicaciones.
            Analysis.check(Snapshot.failure("", ""), o);
        }
        Path out = java.nio.file.Paths.get(o.get("out", "reports/" + Instant.now().toString().replaceAll("[:.]", "-"))).toAbsolutePath();
        Files.createDirectories(out);
        List<String> backends = backend.equals("both") ? Compat.list("jna", "python") : Compat.list(backend);
        List<Snapshot> snapshots = new ArrayList<>();
        String notes;
        if (o.command.equals("diff")) {
            if (!o.has("before") || !o.has("after")) throw new IllegalArgumentException("diff requiere --before y --after");
            Snapshot a = Json.read(java.nio.file.Paths.get(o.get("before", ""))), b = Json.read(java.nio.file.Paths.get(o.get("after", "")));
            if (!a.ok || !b.ok) throw new IllegalArgumentException("No se comparan capturas fallidas; revisa primero sus errores");
            List<Map<String, Object>> changes = Analysis.diff(a, b); Json.write(out.resolve("changes.json"), changes);
            notes = "Cambios observados: " + changes.size() + ". Comparación por ruta posicional; una reordenación puede parecer un cambio. No son IDs permanentes. Capturas parciales requieren revisión.";
            snapshots.addAll(Compat.list(a, b));
        } else if (o.command.equals("doctor")) {
            Json.write(out.resolve("environment.json"), environment());
            notes = "Revisión de entorno y enumeración del registro. Un registro vacío NO demuestra incompatibilidad. Abre el control GTK y el POS en la misma sesión y vuelve a probar.";
            for (String b : backends) snapshots.add(capture(b, o, out, b + "-list", true));
        } else if (o.command.equals("suite")) {
            StringBuilder details = new StringBuilder("Dos lecturas independientes por backend. No cambies la pantalla durante esta prueba. Las diferencias son indicios; no prueban estabilidad general.\n");
            for (String b : backends) {
                Snapshot first = capture(b, o, out, b + "-1", false); snapshots.add(first);
                if (first.ok) {
                    Thread.sleep(o.number("interval", 2000, 100, 60000));
                    Snapshot second = capture(b, o, out, b + "-2", false); snapshots.add(second);
                    if (second.ok) {
                        List<Map<String, Object>> changes = Analysis.diff(first, second);
                        Json.write(out.resolve(b + "-changes.json"), changes);
                        details.append(b).append(": cambios por ruta entre lecturas = ").append(changes.size()).append('\n');
                    }
                }
            }
            details.append("Si JNA lee los campos relevantes: candidato para mantener la integración en Java. Si sólo Python los lee: candidato para puente GObject. Ambos usan AT-SPI; no son dos infraestructuras independientes.\n")
                    .append("Antes de descartar AT-SPI: valida el control GTK, la sesión, la configuración del toolkit y que los campos estén presentes. Confirma valores conocidos con check.");
            notes = details.toString();
        } else if (o.command.equals("watch")) {
            int count = o.number("samples", 3, 2, 20);
            List<Map<String, Object>> comparisons = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Snapshot s = capture(backend, o, out, backend + "-" + (i + 1), false); snapshots.add(s);
                if (i > 0 && s.ok && snapshots.get(i - 1).ok) {
                    Map<String, Object> comparison = new LinkedHashMap<>(); comparison.put("muestra", i + 1);
                    comparison.put("cambios", Analysis.diff(snapshots.get(i - 1), s)); comparisons.add(comparison);
                }
                if (i + 1 < count) Thread.sleep(o.number("interval", 2000, 100, 60000));
            }
            Json.write(out.resolve("changes.json"), comparisons);
            notes = "Observación por sondeo. Puedes cambiar manualmente el producto/pantalla mientras se toman muestras. Sin cambios observados no se demuestra que el texto no se actualice. Rutas posicionales, no identificadores permanentes.";
        } else {
            boolean list = o.command.equals("list");
            for (String b : backends) snapshots.add(capture(b, o, out, b + (list ? "-list" : "-snapshot"), list));
            Snapshot s = snapshots.get(0);
            if (list) {
                for (Snapshot snap : snapshots) for (App app : snap.apps)
                    System.out.printf("[%s] PID=%d  app=%s  toolkit=%s%n", snap.backend, app.pid, printable(app.name), printable(app.toolkit));
                notes = "Inventario de aplicaciones; no se ha explorado su árbol. Usa el PID para evitar ambigüedades.";
            } else if (o.command.equals("find")) {
                List<Node> matches = Analysis.select(s, o); Json.write(out.resolve("matches.json"), matches);
                notes = "Elementos encontrados: " + matches.size() + ". Los filtros name/role/text son subcadenas sin distinción de mayúsculas; id/path son exactos. Revisa errores y truncamiento.";
                System.out.println(notes);
            } else if (o.command.equals("check")) {
                Analysis.Check check = Analysis.check(s, o); Json.write(out.resolve("check.json"), check);
                notes = (check.passed() ? "PASS: " : "FAIL: ") + check.reason(); System.out.println(notes);
            } else notes = "Exploración de sólo lectura. Distingue name (etiqueta accesible), text (contenido) y value (valor numérico); revisa qué propiedad expone cada campo.";
        }
        Report.write(out, snapshots, notes);
        System.out.println("Reporte: " + out.resolve("report.html"));
        for (Snapshot s : snapshots) System.out.println(s.backend + ": " + Analysis.summary(s).get("veredicto") + " · " + s.nodes.size() + " elementos");
        if (o.command.equals("check")) return Analysis.check(snapshots.get(0), o).passed() ? 0 : 3;
        if (snapshots.stream().noneMatch(s -> s.ok)) return 2;
        if (o.command.equals("find")) return !Analysis.select(snapshots.get(0), o).isEmpty() ? 0 : 3;
        if (Compat.set("list", "doctor", "diff").contains(o.command)) return 0;
        return snapshots.stream().anyMatch(s -> "ELEMENTOS_LEIBLES".equals(Analysis.summary(s).get("veredicto"))) ? 0 : 4;
    }
    private static Snapshot capture(String backend, Options o, Path out, String stem, boolean list) throws Exception {
        System.out.println("Leyendo " + backend + " (máximo " + o.timeout() + " s)...");
        return Runner.capture(backend, o, out, stem, list);
    }
    private static String printable(String text) { return text.replaceAll("[\\p{Cntrl}]", " "); }
    static Map<String, Object> environment() {
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("os", System.getProperty("os.name")); env.put("osVersion", System.getProperty("os.version"));
        env.put("arch", System.getProperty("os.arch")); env.put("java", System.getProperty("java.version"));
        env.put("awtHeadless", GraphicsEnvironment.isHeadless());
        for (String key : Compat.list("DISPLAY", "WAYLAND_DISPLAY", "DBUS_SESSION_BUS_ADDRESS", "AT_SPI_BUS_ADDRESS", "XDG_RUNTIME_DIR"))
            env.put(key + "_present", System.getenv(key) != null && !Compat.blank(System.getenv(key)));
        for (String key : Compat.list("XDG_SESSION_TYPE", "XDG_CURRENT_DESKTOP", "NO_AT_BRIDGE", "GTK_MODULES", "QT_ACCESSIBILITY", "QT_LINUX_ACCESSIBILITY_ALWAYS_ON"))
            env.put(key, System.getenv(key));
        env.put("nota", "AWT headless no determina la disponibilidad de AT-SPI. Wayland no implica por sí mismo que no se puedan leer elementos.");
        return env;
    }
    private static void help() {
        System.out.println("POS Accessibility Lab 1.1.0 · Java 8 (1.8)+ · sólo lectura\n"
                + "Uso: bash run.sh COMANDO [--opción valor ...]\n"
                + "  doctor                         Entorno + prueba del registro con ambas vías\n"
                + "  list [--backend both]          Nombres, PID y toolkit de aplicaciones\n"
                + "  suite --app NOMBRE             Dos lecturas por vía, reporte comparativo\n"
                + "  inspect --pid PID              Árbol, nombres, textos, valores y estados\n"
                + "  find --pid PID --name Total    Busca elementos en una captura nueva\n"
                + "  check --pid PID --id total --field text --equals '125.50'\n"
                + "  watch --pid PID --samples 3 --interval 5000\n"
                + "  diff --before A.json --after B.json\n"
                + "\n"
                + "Selección: --app subcadena (no regex) y/o --pid entero\n"
                + "Lectores: --backend jna|python; both sólo en doctor/list/suite\n"
                + "Filtros find/check: --name, --role, --text (subcadena sin mayúsculas),\n"
                + "  --id, --path (exactos). check exige un único elemento/aplicación,\n"
                + "  --field text|name|value y exactamente --equals o --contains (sensibles a mayúsculas).\n"
                + "Límites: --max-nodes 2000 --max-depth 25 --max-text 2000 --timeout 45\n"
                + "Salida: --out CARPETA; Python: --python /usr/bin/python3\n"
                + "Salidas 0=lectura/operación satisfactoria, 2=uso/infraestructura,\n"
                + "  3=búsqueda/validación fallida, 4=diagnóstico parcial o no concluyente.\n"
                + "Ver README.md para instalación, control GTK y cómo interpretar resultados.\n");
    }
}
