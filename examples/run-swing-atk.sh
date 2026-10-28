#!/usr/bin/env bash
# Carga el puente extraido solo en una ventana de prueba, con el JDK 8 existente.
set -euo pipefail
task_source_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
task_jdk="${1:-/usr/java/jdk1.8.0_191}"
task_bridge="${2:-$HOME/pos-atk-prueba/extraido/usr/lib64/java-atk-wrapper}"
task_classes="$HOME/pos-atk-prueba/clases"
for task_file in "$task_jdk/bin/java" "$task_jdk/bin/javac" "$task_bridge/java-atk-wrapper.jar" "$task_bridge/libatk-wrapper.so" "$task_source_dir/PosSwingControl.java"; do
    if [[ ! -f "$task_file" ]]; then
        printf 'Falta el archivo: %s\n' "$task_file" >&2
        exit 1
    fi
done
task_version="$("$task_jdk/bin/java" -version 2>&1)"
printf '%s\n' "$task_version"
if [[ "$task_version" != *'version "1.8.'* ]]; then
    printf 'Esta prueba requiere una ruta explicita a un JDK 8 existente. No se instalara Java.\n' >&2
    exit 1
fi
mkdir -p "$task_classes"
"$task_jdk/bin/javac" -encoding UTF-8 -d "$task_classes" "$task_source_dir/PosSwingControl.java"
printf 'Se abrira una ventana independiente. Dejala abierta y usa otra terminal para inspeccionarla.\n'
export LD_LIBRARY_PATH="$task_bridge:$task_jdk/jre/lib/amd64:$task_jdk/jre/lib/amd64/server${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
exec "$task_jdk/bin/java" -Dfile.encoding=UTF-8 -Djava.library.path="$task_bridge" -Djavax.accessibility.assistive_technologies=org.GNOME.Accessibility.AtkWrapper -cp "$task_classes:$task_bridge/java-atk-wrapper.jar" PosSwingControl
