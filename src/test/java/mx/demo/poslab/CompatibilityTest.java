package mx.demo.poslab;

import com.sun.jna.Native;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityTest {
    @TempDir Path temp;
    @Test void loadsJnaNativeRuntime() {
        assertTrue(Native.POINTER_SIZE == 4 || Native.POINTER_SIZE == 8);
    }
    @Test void retainsUnicodeBlankHandling() {
        assertTrue(Compat.blank(" \t\n\u2003"));
        assertFalse(Compat.blank(" Total "));
    }
    @Test void checkSerializationKeepsThePublicReportFormat() {
        Analysis.Check check = new Analysis.Check(true, "Validación correcta", 1, "125.50");
        com.google.gson.JsonObject json = Json.GSON.toJsonTree(check).getAsJsonObject();
        assertTrue(json.get("passed").getAsBoolean());
        assertEquals(1, json.get("matches").getAsInt());
        assertEquals("125.50", json.get("actual").getAsString());
    }
    @Test void utf8FilesRoundTripOnJava8() throws Exception {
        Path path = temp.resolve("texto.txt");
        Compat.write(path, "Café, té y descripción\n");
        assertEquals("Café, té y descripción\n", Compat.read(path));
    }
}
