#!/usr/bin/env bash
set -euo pipefail
task_here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
task_java='/usr/java/jdk1.8.0_191/bin/java'
[[ -f "$HOME/pos-fx-prueba/ultima-sesion.txt" ]] || { echo 'Abre primero el ejemplo con probar.sh.' >&2; exit 1; }
IFS= read -r task_session < "$HOME/pos-fx-prueba/ultima-sesion.txt"
task_action="${1:-inspect}"
if [[ $# -gt 0 ]]; then shift; fi
exec "$task_java" -jar "$task_here/fx-inspector.jar" "$task_action" "$task_session" "$@"
