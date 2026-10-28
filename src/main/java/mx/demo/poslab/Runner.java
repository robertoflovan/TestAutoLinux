package mx.demo.poslab;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static mx.demo.poslab.Model.*;

final class Runner {
    static Snapshot capture(String backend, Options options, Path directory, String stem, boolean list) throws Exception {
        Files.createDirectories(directory);
        Path result = directory.resolve(stem + ".json").toAbsolutePath();
        // Nunca se interpreta una salida vieja como una captura nueva.
        Files.deleteIfExists(result);
        List<String> command = new ArrayList<>();
        if (backend.equals("jna")) {
            String javaExecutable = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
            command.addAll(Compat.list(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", System.getProperty("java.class.path"), Main.class.getName(), "internal-jna"));
        } else {
            Path bridge = directory.resolve("atspi_probe.py").toAbsolutePath();
            try (java.io.InputStream resource = Runner.class.getResourceAsStream("/bridge/atspi_probe.py")) {
                if (resource == null) throw new IllegalStateException("No se encontró el puente Python dentro del JAR");
                Files.copy(resource, bridge, StandardCopyOption.REPLACE_EXISTING);
            }
            command.addAll(Compat.list(options.get("python", "python3"), bridge.toString()));
        }
        for (String key : Compat.list("app", "pid", "max-nodes", "max-depth", "max-text", "timeout"))
            if (options.has(key)) command.addAll(Compat.list("--" + key, options.get(key, "")));
        if (list) command.addAll(Compat.list("--list-only", "true"));
        command.addAll(Compat.list("--result", result.toString()));
        Snapshot s;
        try {
            Process process = new ProcessBuilder(command).redirectOutput(directory.resolve(stem + ".stdout.log").toFile())
                    .redirectError(directory.resolve(stem + ".stderr.log").toFile()).start();
            try {
                if (!finish(process, options.timeout())) {
                    s = Snapshot.failure(backend, "Timeout de " + options.timeout() + " s. Revisa los logs; aumenta --timeout o reduce --max-nodes.");
                } else if (Files.isRegularFile(result)) {
                    s = Json.read(result);
                    if (process.exitValue() != 0 && s.ok) s = Snapshot.failure(backend, "El lector terminó con código " + process.exitValue() + "; revisa stderr.log");
                } else s = Snapshot.failure(backend, "El lector terminó sin JSON (código " + process.exitValue() + "). Revisa stderr.log.");
            } catch (InterruptedException e) { process.destroyForcibly(); Thread.currentThread().interrupt(); throw e; }
        } catch (java.io.IOException | com.google.gson.JsonParseException e) {
            s = Snapshot.failure(backend, "No se pudo ejecutar/leer el backend: " + e.getMessage());
        }
        Json.write(result, s);
        return s;
    }
    static boolean finish(Process process, int seconds) throws InterruptedException {
        try {
            if (process.waitFor(seconds, TimeUnit.SECONDS)) return true;
            process.destroyForcibly(); process.waitFor(2, TimeUnit.SECONDS); return false;
        } catch (InterruptedException e) {
            process.destroyForcibly(); Thread.currentThread().interrupt(); throw e;
        }
    }
}
