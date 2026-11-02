#!/usr/bin/env bash
set -euo pipefail
task_here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
task_pointer="$HOME/pos-fx-prueba/ultima-sesion-cashier.txt"
[[ -r "$task_pointer" ]] || { echo 'No se encontro una sesion CASHIER del inspector.' >&2; exit 1; }
(cd "$task_here" && sha256sum -c SHA256SUMS >&2)
IFS= read -r task_session < "$task_pointer"
task_action="${1:-inspect}"
if [[ $# -gt 0 ]]; then shift; fi
exec /usr/java/jdk1.8.0_191/bin/java -jar "$task_here/fx-reader.jar" "$task_action" "$task_session" "$@"
