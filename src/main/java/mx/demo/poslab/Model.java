package mx.demo.poslab;

import java.time.Instant;
import java.util.*;

public final class Model {
    private Model() {}
    public static class App {
        public String name = "", toolkit = "";
        public long pid;
        public int desktopIndex;
        public boolean selected;
        public List<String> errors = new ArrayList<>();
    }
    public static class Node {
        public String path = "", app = "", name = "", role = "", accessibleId = "", description = "";
        public long pid;
        public int depth, childCount;
        public String text; // null = no disponible; "" = lectura válida de texto vacío.
        public Integer characterCount;
        public Double value;
        public boolean textSupported, valueSupported, protectedText, textTruncated;
        public List<String> states = new ArrayList<>(), actions = new ArrayList<>(), errors = new ArrayList<>();
    }
    public static class Snapshot {
        public int schemaVersion = 1;
        public String backend = "", capturedAt = Instant.now().toString(), error;
        public boolean ok, truncated;
        public int matchedApps;
        public List<App> apps = new ArrayList<>();
        public List<Node> nodes = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public static Snapshot failure(String backend, String message) {
            Snapshot s = new Snapshot(); s.backend = backend; s.error = message; return s;
        }
    }
}
