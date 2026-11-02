#!/usr/bin/env bash
# Runs only on a disposable graphical test session; never launches a customer POS.
set -euo pipefail
umask 077
ulimit -c 0
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_build="$task_root/target/fx-lab-build"
task_kit="$task_build/fx-lab-v1"
task_logs="$task_build/test-logs"
mkdir -p "$task_logs"
openbox > "$task_logs/window-manager.log" 2>&1 &
task_wm=$!
task_pid=''
cleanup() {
    if [[ -n "$task_pid" ]]; then kill "$task_pid" 2>/dev/null || true; wait "$task_pid" 2>/dev/null || true; fi
}
trap 'cleanup; kill "$task_wm" 2>/dev/null || true' EXIT
sleep 2
launch() {
    task_session="$(mktemp -d "$task_logs/session-XXXXXXXX")"
    java -Djavax.accessibility.assistive_technologies= "-javaagent:$task_kit/fx-inspector.jar=$task_session" -jar "$task_kit/fx-demo.jar" > "$task_session/demo.log" 2>&1 &
    task_pid=$!
    task_window="$(timeout 30 xdotool search --sync --onlyvisible --pid "$task_pid" --name 'POS - Prueba JavaFX Inspector' | head -n1)"
    sleep 2
    kill -0 "$task_pid"
    [[ -s "$task_session/session.properties" ]]
}
client() { java -jar "$task_kit/fx-inspector.jar" "$1" "$task_session" "${@:2}"; }
fail_check() {
    local task_rc=0
    client check "$@" > "$task_logs/rejected-$1.log" 2>&1 || task_rc=$?
    cat "$task_logs/rejected-$1.log"
    [[ "$task_rc" == 3 ]]
}
launch
client inspect
for task_i in 1 2 3; do client check TotalVenta 125.50; kill -0 "$task_pid"; done
fail_check TotalVenta 999.00
fail_check NoExiste 125.50
fail_check ClaveDemo NO_EXPORTAR_CLAVE_7391
if grep -r -F 'NO_EXPORTAR_CLAVE_7391' "$task_session"; then echo 'Password leaked'; exit 1; fi
xdotool windowactivate --sync "$task_window"
xdotool mousemove --window "$task_window" 350 30 click 1
xdotool key --clearmodifiers ctrl+a
xdotool type --clearmodifiers '1002'
xdotool key Return
sleep 2
for task_i in 1 2 3; do client check TotalVenta 80.00; kill -0 "$task_pid"; done
sleep 5
kill -0 "$task_pid"
# Successful responses stay in the folder; a dead agent must not reuse them.
cleanup
task_pid=''
task_rc=0
client check TotalVenta 80.00 > "$task_logs/dead-agent.log" 2>&1 || task_rc=$?
cat "$task_logs/dead-agent.log"
[[ "$task_rc" == 2 ]]
grep -F 'Sin respuesta nueva' "$task_logs/dead-agent.log"
launch
client check TotalVenta 125.50
client stop
sleep 2
kill -0 "$task_pid"
if grep -F 'pos-fx-inspector' <(jstack "$task_pid"); then echo 'Worker did not stop'; exit 1; fi
task_rc=0
client check TotalVenta 125.50 > "$task_logs/stopped-agent.log" 2>&1 || task_rc=$?
[[ "$task_rc" == 2 ]]
printf 'PASS: real JavaFX tree, repeated 125.50 and 80.00 after keyboard input, app alive after reader disconnects, wrong/missing/protected values rejected, password omitted, no stale PASS, inspector stop keeps application alive.\n' | tee "$task_kit/TEST-RESULTS.txt"
java -version 2> "$task_kit/BUILD-JAVA.txt"
printf 'Source commit: %s\nCI: %s\n' "${GITHUB_SHA:-local}" "https://github.com/${GITHUB_REPOSITORY:-local}/actions/runs/${GITHUB_RUN_ID:-local}" > "$task_kit/BUILD-INFO.txt"
