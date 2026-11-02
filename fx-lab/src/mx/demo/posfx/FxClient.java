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
        if (args.length < 2 || !(args[0].equals("inspect") || args[0].equals("check") || args[0].equals("stop")))
            throw new IllegalArgumentException("Uso: inspect SESION | check SESION ID TEXTO | stop SESION");
        if (args[0].equals("check") && args.length != 4) throw new IllegalArgumentException("check requiere ID y texto exacto");
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
            if (!args[0].equals("check")) return nodes > 0 && !Boolean.parseBoolean(p.getProperty("truncated")) ? 0 : 4;
            if (Boolean.parseBoolean(p.getProperty("truncated"))) throw new IllegalStateException("Captura parcial: no se valida con un arbol incompleto");
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
    private static String printable(String s) { return s.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t"); }
}
