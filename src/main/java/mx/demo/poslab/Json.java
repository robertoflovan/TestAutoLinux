package mx.demo.poslab;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

final class Json {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
    static void write(Path path, Object object) throws IOException {
        if (path.toAbsolutePath().getParent() != null) Files.createDirectories(path.toAbsolutePath().getParent());
        Compat.write(path, GSON.toJson(object));
    }
    static Model.Snapshot read(Path path) throws IOException {
        Model.Snapshot s = GSON.fromJson(Compat.read(path), Model.Snapshot.class);
        if (s == null || s.schemaVersion != 1 || s.nodes == null || s.apps == null || s.warnings == null)
            throw new IOException("Formato de snapshot inválido: " + path);
        return s;
    }
}
