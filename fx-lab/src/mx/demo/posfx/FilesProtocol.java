package mx.demo.posfx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

final class FilesProtocol {
    static void write(Path path, Properties data) throws IOException {
        Path tmp = Files.createTempFile(path.getParent(), ".writing-", ".tmp");
        try {
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) { data.store(w, "POS JavaFX Inspector 1"); }
            try { Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tmp); }
    }
    static Properties read(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(r); }
        return p;
    }
    static Properties error(String message) {
        Properties p = new Properties(); p.setProperty("status", "ERROR"); p.setProperty("error", message); return p;
    }
}
