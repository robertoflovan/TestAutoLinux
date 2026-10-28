package mx.demo.poslab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static mx.demo.poslab.Model.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalysisTest {
    @TempDir Path temp;
    Snapshot sample() {
        Snapshot s = new Snapshot(); s.ok = true; s.backend = "test-fixture"; s.matchedApps = 1;
        Node total = new Node(); total.path = "app[0]/1"; total.app = "POS"; total.pid = 123;
        total.name = "TotalVenta"; total.role = "text"; total.accessibleId = "total";
        total.textSupported = true; total.text = "125.50"; total.depth = 1; s.nodes.add(total);
        return s;
    }
    Options check(String... extra) {
        List<String> args = new ArrayList<>(List.of("check", "--pid", "123", "--id", "total", "--equals", "125.50"));
        args.addAll(List.of(extra)); return new Options(args.toArray(String[]::new));
    }
    @Test void validatesActualText() { assertTrue(Analysis.check(sample(), check()).passed()); }
    @Test void doesNotMistakeNameForText() {
        Snapshot s = sample(); s.nodes.get(0).text = "0.00";
        assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void missingTextIsNotEmptyText() {
        Snapshot s = sample(); s.nodes.get(0).text = null;
        assertFalse(Analysis.check(s, new Options("check", "--equals", "")).passed());
        s.nodes.get(0).text = "";
        assertTrue(Analysis.check(s, new Options("check", "--equals", "")).passed());
    }
    @Test void refusesAmbiguousElement() {
        Snapshot s = sample(); s.nodes.add(s.nodes.get(0)); assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void refusesAmbiguousApplication() {
        Snapshot s = sample(); s.matchedApps = 2; assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void refusesPartialTreeEvenWhenValueMatches() {
        Snapshot s = sample(); s.truncated = true; assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void refusesTruncatedText() {
        Snapshot s = sample(); s.nodes.get(0).textTruncated = true; assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void refusesProviderError() {
        Snapshot s = sample(); s.nodes.get(0).errors.add("text: timeout"); assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void refusesProtectedText() {
        Snapshot s = sample(); s.nodes.get(0).protectedText = true; assertFalse(Analysis.check(s, check()).passed());
    }
    @Test void failedCaptureCannotPass() { assertFalse(Analysis.check(Snapshot.failure("jna", "sin bus"), check()).passed()); }
    @Test void requiresAnExpectation() { assertThrows(IllegalArgumentException.class, () -> Analysis.check(sample(), new Options("check"))); }
    @Test void rejectsTwoExpectations() { assertThrows(IllegalArgumentException.class, () -> Analysis.check(sample(), check("--contains", "125"))); }
    @Test void namesAndRolesUseLiteralSubstrings() {
        assertEquals(1, Analysis.select(sample(), new Options("find", "--name", "TOTAL", "--role", "text")).size());
        assertEquals(0, Analysis.select(sample(), new Options("find", "--name", "Total.*")).size());
    }
    @Test void idsAreExact() { assertTrue(Analysis.select(sample(), new Options("find", "--id", "tot")).isEmpty()); }
    @Test void containsDoesNotUseRegexOrCaseFolding() {
        Snapshot s = sample(); s.nodes.get(0).text = "VENTA COMPLETA";
        assertTrue(Analysis.check(s, new Options("check", "--contains", "COMPLETA")).passed());
        assertFalse(Analysis.check(s, new Options("check", "--contains", "completa")).passed());
    }
    @Test void reportsNativeNumericValues() {
        Snapshot s = sample(); s.nodes.get(0).value = 125.5;
        assertTrue(Analysis.check(s, new Options("check", "--field", "value", "--equals", "125.5")).passed());
    }
    @Test void infrastructureFailureIsNotInaccessibilityVerdict() {
        assertEquals("ERROR_DE_ENTORNO", Analysis.summary(Snapshot.failure("jna", "bus")).get("veredicto"));
    }
    @Test void missingAppIsNotInaccessibleApp() {
        Snapshot s = sample(); s.matchedApps = 0; s.nodes.clear();
        assertEquals("APLICACION_NO_ENCONTRADA", Analysis.summary(s).get("veredicto"));
    }
    @Test void frameTitleAloneIsNotProofOfReadableFields() {
        Snapshot s = sample(); Node n = s.nodes.get(0); n.text = null; n.role = "frame";
        assertEquals("SOLO_CONTENEDORES_O_SIN_CONTENIDO", Analysis.summary(s).get("veredicto"));
    }
    @Test void nonemptyLabelNameIsUsefulEvenWithoutTextInterface() {
        Snapshot s = sample(); Node n = s.nodes.get(0); n.text = null; n.role = "label";
        assertEquals("ELEMENTOS_LEIBLES", Analysis.summary(s).get("veredicto"));
    }
    @Test void warnsOnPartialData() {
        Snapshot s = sample(); s.truncated = true;
        assertEquals("LECTURA_PARCIAL", Analysis.summary(s).get("veredicto"));
    }
    @Test void detectsTextChangesWithoutNodeCountChange() {
        Snapshot a = sample(), b = sample(); b.nodes.get(0).text = "80.00";
        assertEquals(1, Analysis.diff(a, b).size()); assertEquals(0, Analysis.diff(a, sample()).size());
    }
    @Test void roundTripKeepsNullAndUnicode() throws Exception {
        Snapshot s = sample(); s.nodes.get(0).name = "Descripción del café"; s.nodes.get(0).text = null;
        Path path = temp.resolve("snapshot.json"); Json.write(path, s);
        Snapshot actual = Json.read(path); assertEquals("Descripción del café", actual.nodes.get(0).name); assertNull(actual.nodes.get(0).text);
    }
    @Test void htmlEscapesUntrustedApplicationText() throws Exception {
        Snapshot s = sample(); s.nodes.get(0).text = "<script>alert('x')</script>";
        Report.write(temp, List.of(s), "<img src=x>");
        String html = Files.readString(temp.resolve("report.html"));
        assertFalse(html.contains("<script>")); assertFalse(html.contains("<img src=x>")); assertTrue(html.contains("&lt;script&gt;"));
    }
    @Test void rejectsInvalidBoundsAndUnknownFlags() {
        assertThrows(IllegalArgumentException.class, () -> new Options("inspect", "--max-nodes", "-1"));
        assertThrows(IllegalArgumentException.class, () -> new Options("inspect", "--pid", "0"));
        assertThrows(IllegalArgumentException.class, () -> new Options("inspect", "--max-depth", "a"));
        assertThrows(IllegalArgumentException.class, () -> new Options("inspect", "--typo", "a"));
        assertThrows(IllegalArgumentException.class, () -> new Options("inspect", "--pid"));
    }
    @Test void staleSnapshotCannotPassAfterLaunchFailure() throws Exception {
        Path stale = temp.resolve("attempt.json"); Json.write(stale, sample());
        Snapshot s = Runner.capture("python", new Options("inspect", "--python", temp.resolve("nonexistent-python").toString()), temp, "attempt", false);
        assertFalse(s.ok); assertFalse(Json.read(stale).ok);
    }
    @Test void targetCombinesPidAndLiteralApplicationName() {
        App a = new App(); a.name = "Caja POS"; a.pid = 123;
        assertTrue(new Options("inspect", "--app", "pos", "--pid", "123").target(a));
        assertFalse(new Options("inspect", "--app", "pos", "--pid", "124").target(a));
    }
}
