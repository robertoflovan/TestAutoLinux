#!/usr/bin/env bash
set -euo pipefail
task_java='/usr/java/jdk1.8.0_191/bin/java'
task_kit="$HOME/pos-fx-prueba/fx-lab-v1"
task_pointer="$HOME/pos-fx-prueba/ultima-sesion-cashier.txt"
[[ -r "$task_pointer" ]] || { echo 'Inicia primero CASHIER con restart-cashier-fx.sh --iniciar.' >&2; exit 1; }
IFS= read -r task_session < "$task_pointer"
task_action="${1:-inspect}"
if [[ $# -gt 0 ]]; then shift; fi
exec "$task_java" -jar "$task_kit/fx-inspector.jar" "$task_action" "$task_session" "$@"
