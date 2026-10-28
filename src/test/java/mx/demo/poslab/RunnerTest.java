package mx.demo.poslab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;

class RunnerTest {
    public static class Child {
        public static void main(String[] args) throws Exception {
            if (args.length > 0) Thread.sleep(60_000);
        }
    }
    Process start(String... args) throws Exception {
        java.util.List<String> command = new java.util.ArrayList<>(Compat.list(
                java.nio.file.Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), Child.class.getName()));
        command.addAll(Compat.list(args)); return new ProcessBuilder(command).start();
    }
    @Test void killsHungReaderWithinTimeout() throws Exception {
        Process child = start("sleep");
        try { assertFalse(Runner.finish(child, 1)); assertFalse(child.isAlive()); }
        finally { child.destroyForcibly(); }
    }
    @Test void acceptsNormalReaderCompletion() throws Exception {
        Process child = start();
        try { assertTrue(Runner.finish(child, 5)); assertEquals(0, child.exitValue()); }
        finally { child.destroyForcibly(); }
    }
}
