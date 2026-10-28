package mx.demo.poslab;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static mx.demo.poslab.Model.*;

final class Report {
    static void write(Path out, List<Snapshot> snapshots, String notes) throws Exception {
        List<Map<String, Object>> summaries = snapshots.stream().map(Analysis::summary).toList();
        Json.write(out.resolve("summary.json"), summaries);
        StringBuilder text = new StringBuilder("POS ACCESSIBILITY LAB\n\n").append(notes).append("\n\n");
        for (Map<String, Object> summary : summaries) {
            summary.forEach((key,value) -> text.append(key).append(": ").append(value).append('\n'));
            text.append('\n');
        }
        Files.writeString(out.resolve("summary.txt"), text, StandardCharsets.UTF_8);
        StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Diagnóstico de accesibilidad del POS</title>
                <style>body{font:16px system-ui,sans-serif;margin:2rem;color:#172238;background:#f4f7fb}main{max-width:1400px;margin:auto}section{background:white;padding:1.2rem;margin:1rem 0;border:1px solid #d5deea;border-radius:12px}pre{white-space:pre-wrap;overflow-wrap:anywhere}table{border-collapse:collapse;width:100%;font-size:13px}th,td{padding:.6rem;border-bottom:1px solid #d5deea;text-align:left;vertical-align:top;white-space:pre-wrap;overflow-wrap:anywhere}th{background:#edf2fa}code{font-family:monospace}.scroll{overflow-x:auto}h1{font-size:1.8rem}small{color:#475569}</style>
                <main><h1>Diagnóstico de accesibilidad del punto de venta</h1>
                """);
        html.append("<section><pre>").append(escape(notes)).append("</pre><small>Consulta los JSON para los datos completos. Sin OCR, capturas ni envío de entradas.</small></section>");
        for (Snapshot s : snapshots) {
            html.append("<section><h2>").append(escape(s.backend)).append(" · ").append(escape(s.capturedAt)).append("</h2><pre>")
                    .append(escape(Json.GSON.toJson(Analysis.summary(s)))).append("</pre>");
            if (!s.warnings.isEmpty()) html.append("<pre>").append(escape(s.warnings.toString())).append("</pre>");
            html.append("<details><summary>Aplicaciones detectadas</summary><pre>").append(escape(Json.GSON.toJson(s.apps))).append("</pre></details>");
            html.append("<div class=scroll><table><thead><tr><th>Ruta / ID</th><th>Rol / estados</th><th>Nombre</th><th>Texto leído</th><th>Valor</th><th>Errores</th></tr></thead><tbody>");
            for (Node n : s.nodes) {
                html.append("<tr><td>").append(escape(n.path + "\n" + n.accessibleId)).append("</td><td>")
                        .append(escape(n.role + "\n" + n.states)).append("</td><td>").append(escape(n.name)).append("</td><td>")
                        .append(escape(n.protectedText ? "[protegido]" : n.text == null ? "[no disponible]" : n.text))
                        .append(n.textTruncated ? " [truncado]" : "").append("</td><td>").append(escape(String.valueOf(n.value)))
                        .append("</td><td>").append(escape(String.join("\n", n.errors))).append("</td></tr>");
            }
            html.append("</tbody></table></div></section>");
        }
        html.append("</main></html>");
        Files.writeString(out.resolve("report.html"), html, StandardCharsets.UTF_8);
    }
    static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
