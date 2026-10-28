package mx.demo.poslab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Operaciones comunes que conservan compatibilidad con Java 8. */
final class Compat {
    private Compat() {}
    @SafeVarargs static <T> List<T> list(T... values) { return Arrays.asList(values); }
    @SafeVarargs static <T> Set<T> set(T... values) { return new HashSet<T>(Arrays.asList(values)); }
    static boolean blank(String text) { return text.codePoints().allMatch(Character::isWhitespace); }
    static String read(Path path) throws IOException { return new String(Files.readAllBytes(path), StandardCharsets.UTF_8); }
    static void write(Path path, CharSequence text) throws IOException { Files.write(path, text.toString().getBytes(StandardCharsets.UTF_8)); }
}
