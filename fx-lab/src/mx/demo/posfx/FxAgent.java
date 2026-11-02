package mx.demo.posfx;

import javafx.application.Platform;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.*;

/** Startup-only agent. No transformers, retransformation, native code, sockets or UI actions. */
public final class FxAgent {
    public static void premain(String directory, Instrumentation unused) {
        try {
            if (directory == null || directory.trim().isEmpty()) throw new IllegalArgumentException("Falta carpeta de sesion");
            Path session = Paths.get(directory).toAbsolutePath();
            Files.createDirectories(session);
            Files.createFile(session.resolve("agent.lock")); // Never reuse another agent's folder.
            Properties info = new Properties();
            info.setProperty("pid", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
            info.setProperty("java", System.getProperty("java.version"));
            info.setProperty("agent", "pos-fx-inspector-1");
            FilesProtocol.write(session.resolve("session.properties"), info);
            Thread worker = new Thread(() -> run(session), "pos-fx-inspector");
            worker.setDaemon(true); worker.start();
            System.out.println("Inspector JavaFX: " + session);
        } catch (Exception | LinkageError e) {
            System.err.println("Inspector JavaFX desactivado: " + e);
        }
    }
    private static void run(Path session) {
        FutureTask<Properties> pending = null;
        try {
            while (!Files.exists(session.resolve("STOP"))) {
                try (DirectoryStream<Path> requests = Files.newDirectoryStream(session, "*.request")) {
                    for (Path request : requests) {
                        if (Files.exists(session.resolve("STOP"))) break;
                        String id = request.getFileName().toString().replace(".request", "");
                        if (!id.matches("[a-f0-9-]{36}")) continue;
                        Files.deleteIfExists(request);
                        Properties result;
                        if (pending != null && !pending.isDone()) {
                            result = FilesProtocol.error("JavaFX ocupado: la consulta anterior sigue pendiente; no se encolan mas lecturas.");
                        } else {
                            pending = new FutureTask<>(FxSnapshot::capture);
                            try {
                                Platform.runLater(pending);
                                result = pending.get(3, TimeUnit.SECONDS);
                            } catch (TimeoutException e) {
                                result = FilesProtocol.error("Tiempo de espera agotado en el hilo JavaFX.");
                            } catch (Exception | LinkageError e) {
                                pending = null;
                                result = FilesProtocol.error("No se pudo leer JavaFX: " + e);
                            }
                        }
                        result.setProperty("requestId", id);
                        FilesProtocol.write(session.resolve(id + ".properties"), result);
                        break; // At most one capture per polling interval.
                    }
                }
                Thread.sleep(500);
            }
            if (pending != null) pending.cancel(false);
        } catch (Exception | LinkageError e) {
            try { FilesProtocol.write(session.resolve("agent-error.properties"), FilesProtocol.error(e.toString())); }
            catch (Exception ignored) { }
        }
    }
}
