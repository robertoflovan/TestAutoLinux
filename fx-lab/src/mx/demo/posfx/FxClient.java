package mx.demo.posfx;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class FxClient {
    public static void main(String[] args) {
        int exit;
        try { exit = run(args); }
        catch (Exception e) { System.err.println("ERROR: " + e.getMessage()); exit = 2; }
        System.exit(exit);
    }
    static int run(String[] args) throws Exception {
        if (args.length < 2 || !(args[0].equals("inspect") || args[0].equals("check") || args[0].equals("check-option") || args[0].equals("stop")))
            throw new IllegalArgumentException("Uso: inspect SESION | check SESION ID TEXTO | check-option SESION CONTENEDOR NUMERO TEXTO | stop SESION");
        if (args[0].equals("check") && args.length != 4) throw new IllegalArgumentException("check requiere ID y texto exacto");
        if (args[0].equals("check-option") && (args.length != 5 || args[2].isEmpty() || !args[3].matches("[0-9]+")))
            throw new IllegalArgumentException("check-option requiere ID del contenedor, numero (sin corchetes) y texto exacto");
        Path session = Paths.get(args[1]).toAbsolutePath();
        if (!Files.isRegularFile(session.resolve("session.properties"))) throw new IllegalArgumentException("No hay agente en esa carpeta");
        if (args[0].equals("stop")) {
            Files.write(session.resolve("STOP"), new byte[0]);
            System.out.println("Solicitud de detener el inspector enviada; no se cierra la aplicacion."); return 0;
        }
        if (Files.exists(session.resolve("STOP"))) throw new IllegalStateException("Inspector detenido; abre una sesion nueva");
        String id = UUID.randomUUID().toString();
        Path request = session.resolve(id + ".request"), response = session.resolve(id + ".properties");
        Files.createFile(request);
        try {
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (!Files.isRegularFile(response) && System.nanoTime() < deadline) Thread.sleep(100);
            if (!Files.isRegularFile(response)) throw new IllegalStateException("Sin respuesta nueva del agente: revisa si la ventana sigue abierta y la carpeta de sesion es correcta");
            Properties p = FilesProtocol.read(response);
            if (!id.equals(p.getProperty("requestId"))) throw new IllegalStateException("Respuesta no corresponde a esta solicitud");
            if (!"OK".equals(p.getProperty("status"))) throw new IllegalStateException(p.getProperty("error", "Captura fallida"));
            List<String> lines = new ArrayList<>();
            int nodes = Integer.parseInt(p.getProperty("nodeCount", "0"));
            lines.add("Ventanas=" + p.getProperty("windowCount") + " Nodos=" + nodes + " Parcial=" + p.getProperty("truncated"));
            for (int i = 0; i < nodes; i++) {
                String n = "node." + i + ".";
                lines.add(p.getProperty(n + "path") + " | " + p.getProperty(n + "type") + " | id=" + printable(p.getProperty(n + "id", "")) + " | text=" + ("true".equals(p.getProperty(n + "protected")) ? "[PROTEGIDO]" : printable(p.getProperty(n + "text", ""))));
            }
            Path report = session.resolve(id + ".txt"); Files.write(report, lines, StandardCharsets.UTF_8);
            System.out.println(lines.get(0)); System.out.println("Reporte: " + report);
            if (args[0].equals("inspect")) return nodes > 0 && !Boolean.parseBoolean(p.getProperty("truncated")) ? 0 : 4;
            if (Boolean.parseBoolean(p.getProperty("truncated"))) throw new IllegalStateException("Captura parcial: no se valida con un arbol incompleto");
            if (args[0].equals("check-option")) return checkOption(p, nodes, args[2], args[3], args[4]);
            List<String> matches = new ArrayList<>();
            for (int i = 0; i < nodes; i++) if (args[2].equals(p.getProperty("node." + i + ".id"))) matches.add("node." + i + ".");
            if (matches.size() != 1) { System.out.println("FAIL: se requiere un unico ID; coincidencias=" + matches.size()); return 3; }
            String n = matches.get(0);
            if (Boolean.parseBoolean(p.getProperty(n + "protected")) || !p.containsKey(n + "text") || Boolean.parseBoolean(p.getProperty(n + "textTruncated"))) {
                System.out.println("FAIL: campo protegido o sin texto completo legible"); return 3;
            }
            String actual = p.getProperty(n + "text");
            boolean passed = args[3].equals(actual);
            System.out.println((passed ? "PASS" : "FAIL") + ": " + args[2] + " = " + printable(actual));
            return passed ? 0 : 3;
        } finally { Files.deleteIfExists(request); }
    }
    static int checkOption(Properties p, int nodes, String container, String number, String expected) {
        if (Boolean.parseBoolean(p.getProperty("truncated"))) return optionFailure("captura parcial");
        List<String> roots = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            String n = "node." + i + ".";
            if (container.equals(p.getProperty(n + "id"))) roots.add(p.getProperty(n + "path", ""));
        }
        if (roots.size() != 1 || roots.get(0).isEmpty()) return optionFailure("contenedor no unico o ausente: " + container);
        String prefix = roots.get(0) + "/", marker = "[" + number + "]", group = null;
        int markers = 0;
        for (int i = 0; i < nodes; i++) {
            String n = "node." + i + ".", path = p.getProperty(n + "path", "");
            if (!path.startsWith(prefix) || !readableLabel(p, n) || !marker.equals(p.getProperty(n + "text"))) continue;
            String relative = path.substring(prefix.length());
            int slash = relative.indexOf('/');
            if (slash < 1) return optionFailure("numero sin tarjeta contenedora reconocible");
            group = prefix + relative.substring(0, slash) + "/";
            markers++;
        }
        if (markers != 1) return optionFailure("se requiere una unica opcion " + marker + "; coincidencias=" + markers);
        List<String> captions = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            String n = "node." + i + ".", path = p.getProperty(n + "path", ""), text = p.getProperty(n + "text", "");
            // Read the Label control once; its LabeledText skin is not another option.
            if (path.startsWith(group) && "javafx.scene.control.Label".equals(p.getProperty(n + "type")) && !marker.equals(text) && !text.isEmpty()) {
                if (!readableLabel(p, n)) return optionFailure("etiqueta protegida o texto incompleto");
                captions.add(text);
            }
        }
        if (captions.size() != 1) return optionFailure("la tarjeta no tiene una unica etiqueta de texto; coincidencias=" + captions.size());
        String actual = captions.get(0);
        boolean passed = expected.equals(actual);
        System.out.println((passed ? "PASS" : "FAIL") + ": " + container + " opcion " + marker + " = " + printable(actual));
        if (!passed) System.out.println("Esperado: " + printable(expected));
        return passed ? 0 : 3;
    }
    private static boolean readableLabel(Properties p, String n) {
        return "javafx.scene.control.Label".equals(p.getProperty(n + "type"))
                && p.containsKey(n + "text") && !Boolean.parseBoolean(p.getProperty(n + "protected"))
                && !Boolean.parseBoolean(p.getProperty(n + "textTruncated"));
    }
    private static int optionFailure(String message) { System.out.println("FAIL: " + message); return 3; }
    private static String printable(String s) { return s.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t"); }
}
