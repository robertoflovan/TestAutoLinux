package mx.demo.posfx;

import java.nio.file.*;
import java.util.Properties;

/** Synthetic menus, no customer report or running POS. Run with Java 8. */
public final class FxOptionCheckTest {
    private static Properties menu() {
        Properties p = new Properties();
        p.setProperty("status", "OK"); p.setProperty("truncated", "false"); p.setProperty("nodeCount", "0"); p.setProperty("windowCount", "1");
        node(p, "w/root", "HBox", "actions", "");
        node(p, "w/root/7/0", "Label", "", "[2]");
        node(p, "w/root/7/0/0", "skin.LabeledText", "", "[2]");
        node(p, "w/root/7/1/0", "Label", "", "Returns...");
        node(p, "w/root/7/1/0/0", "skin.LabeledText", "", "Returns...");
        node(p, "w/root/7/1/1", "IconView", "", ">");
        node(p, "other/root/0", "Label", "", "[2]");
        node(p, "other/root/1", "Label", "", "Wrong area");
        node(p, "w/root/9/0", "Label", "", "[7]");
        node(p, "w/root/9/1", "Label", "", "Returns...");
        node(p, "title", "Text", "heading", "Menu");
        return p;
    }
    private static void node(Properties p, String path, String type, String id, String text) {
        int count = Integer.parseInt(p.getProperty("nodeCount"));
        String n = "node." + count + ".";
        p.setProperty(n + "path", path); p.setProperty(n + "type", "javafx.scene.control." + type);
        p.setProperty(n + "id", id); p.setProperty(n + "text", text);
        p.setProperty("nodeCount", Integer.toString(count + 1));
    }
    private static void expect(Properties p, String id, String number, String expected, int code) {
        int actual = FxClient.checkOption(p, Integer.parseInt(p.getProperty("nodeCount")), id, number, expected);
        if (actual != code) throw new AssertionError("Expected exit " + code + " but got " + actual);
    }
    private static void liveProtocol(String action, String... arguments) throws Exception {
        Path session = Files.createTempDirectory("fx-reader-protocol-");
        FilesProtocol.write(session.resolve("session.properties"), new Properties());
        Thread responder = new Thread(() -> {
            try {
                for (int attempt = 0; attempt < 200; attempt++) {
                    try (DirectoryStream<Path> requests = Files.newDirectoryStream(session, "*.request")) {
                        for (Path request : requests) {
                            String id = request.getFileName().toString().replace(".request", "");
                            Properties p = menu(); p.setProperty("requestId", id);
                            FilesProtocol.write(session.resolve(id + ".properties"), p); return;
                        }
                    }
                    Thread.sleep(25);
                }
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        responder.setDaemon(true); responder.start();
        String[] args = new String[arguments.length + 2]; args[0] = action; args[1] = session.toString();
        System.arraycopy(arguments, 0, args, 2, arguments.length);
        if (FxClient.run(args) != 0) throw new AssertionError("Protocol test failed for " + action);
        responder.join(1000);
    }
    public static void main(String[] args) throws Exception {
        expect(menu(), "actions", "2", "Returns...", 0);
        expect(menu(), "actions", "2", "Return...", 3);
        expect(menu(), "actions", "3", "Returns...", 3);
        expect(menu(), "missing", "2", "Returns...", 3);
        Properties p = menu(); node(p, "second/root", "HBox", "actions", ""); expect(p, "actions", "2", "Returns...", 3);
        p = menu(); node(p, "w/root/8/0", "Label", "", "[2]"); expect(p, "actions", "2", "Returns...", 3);
        p = menu(); node(p, "w/root/7/2", "Label", "", "Extra caption"); expect(p, "actions", "2", "Returns...", 3);
        p = menu(); p.setProperty("truncated", "true"); expect(p, "actions", "2", "Returns...", 3);
        p = menu(); p.setProperty("node.3.textTruncated", "true"); expect(p, "actions", "2", "Returns...", 3);
        p = menu(); p.setProperty("node.3.protected", "true"); expect(p, "actions", "2", "Returns...", 3);
        p = menu(); p.setProperty("node.3.text", "Devolución…"); expect(p, "actions", "2", "Devolución…", 0);
        expect(p, "actions", "2", "Devolución...", 3);
        liveProtocol("check-option", "actions", "2", "Returns...");
        liveProtocol("check", "heading", "Menu");
        liveProtocol("inspect");
        System.out.println("PASS: option scoping, reordered cards, skin duplicates, exact text, ambiguous/missing/protected/partial data, UTF-8 and existing file protocol.");
    }
}
