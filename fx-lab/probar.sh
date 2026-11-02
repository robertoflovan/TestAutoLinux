#!/usr/bin/env bash
set -euo pipefail
umask 077
task_here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
task_java='/usr/java/jdk1.8.0_191/bin/java'
[[ -x "$task_java" ]] || { echo "No existe $task_java. No se instalara Java." >&2; exit 1; }
[[ -z "${JAVA_TOOL_OPTIONS:-}${_JAVA_OPTIONS:-}${JDK_JAVA_OPTIONS:-}" ]] || { echo 'Hay opciones Java de entorno; revisar antes de abrir el ejemplo.' >&2; exit 1; }
task_version="$("$task_java" -version 2>&1)"
printf '%s\n' "$task_version"
[[ "$task_version" == *'version "1.8.0_191"'* ]] || { echo 'Version distinta de la esperada.' >&2; exit 1; }
(cd "$task_here" && sha256sum -c SHA256SUMS)
mkdir -p "$HOME/pos-fx-prueba"
task_session="$(mktemp -d "$HOME/pos-fx-prueba/sesion-XXXXXXXX")"
printf '%s\n' "$task_session" > "$HOME/pos-fx-prueba/ultima-sesion.txt"
echo "Sesion: $task_session"
echo 'Se abrira solo el ejemplo JavaFX. Mantener esta terminal abierta. No se carga ATK.'
exec "$task_java" -Djavax.accessibility.assistive_technologies= "-javaagent:$task_here/fx-inspector.jar=$task_session" -jar "$task_here/fx-demo.jar"
