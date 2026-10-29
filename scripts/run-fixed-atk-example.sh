#!/usr/bin/env bash
# Included as probar.sh in the experimental bridge download. No installation.
set -euo pipefail
task_here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
task_jdk="/usr/java/jdk1.8.0_191"
cd "$task_here"
if [[ "$(uname -s)" != Linux || "$(uname -m)" != x86_64 ]]; then
    echo 'Esta biblioteca experimental requiere Linux x86_64.' >&2
    exit 1
fi
sha256sum -c SHA256SUMS
if [[ ! -x "$task_jdk/bin/java" || ! -x "$task_jdk/bin/javac" ]]; then
    echo "No se encontro el JDK existente en $task_jdk; no se instalara Java." >&2
    exit 1
fi
task_dependencies="$(LD_LIBRARY_PATH="$task_here:$task_jdk/jre/lib/amd64:$task_jdk/jre/lib/amd64/server" ldd "$task_here/libatk-wrapper.so" 2>&1)" || {
    printf '%s\n' "$task_dependencies" >&2
    exit 1
}
if [[ "$task_dependencies" == *'not found'* ]]; then
    printf 'Faltan dependencias; no se abrira la prueba:\n%s\n' "$task_dependencies" >&2
    exit 1
fi
printf 'Verificaciones iniciales correctas. Se abrira SOLO el ejemplo con Java 8u191.\n'
exec bash "$task_here/run-swing-atk.sh" "$task_jdk" "$task_here"
