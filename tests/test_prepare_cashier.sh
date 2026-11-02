#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_home="$(mktemp -d)"
task_cwd="$task_home/geopos/installations/geopos2oxxo-colombia-2.0.0.2.101"
mkdir -p "$task_cwd/classes" "$task_home/pos-atk-prueba/puente-corregido-v1"
ln -s "$task_cwd" "$task_home/geopos/current"
cat > "$task_cwd/CVPOSInstance.java" <<'JAVA'
package uy.com.geocom.geopos.core;
public class CVPOSInstance {
    public static void main(String[] args) throws Exception { Thread.sleep(120000); }
}
JAVA
javac -d "$task_cwd/classes" "$task_cwd/CVPOSInstance.java"
task_java="$(readlink -f "$(command -v java)")"
# Only the test copy accepts the runner JDK; the delivered script remains pinned.
sed "s|^task_java=.*|task_java='$task_java'|" "$task_root/scripts/prepare-cashier-atk.sh" > "$task_home/prepare.sh"
cd "$task_home/pos-atk-prueba/puente-corregido-v1"
printf 'not loaded during preparation\n' > libatk-wrapper.so
printf 'not loaded during preparation\n' > java-atk-wrapper.jar
sha256sum libatk-wrapper.so java-atk-wrapper.jar > SHA256SUMS
cd "$task_cwd"
HOME="$task_home" GEOPOS_HOME="$task_cwd" "$task_java" '-Dtest.literal=$(touch MUST_NOT_EXIST)' -cp "$task_cwd/classes" uy.com.geocom.geopos.core.CVPOSInstance 'argument with spaces' > fixture.log 2>&1 &
task_pid=$!
trap 'kill "$task_pid" 2>/dev/null || true; wait "$task_pid" 2>/dev/null || true' EXIT
sleep 1
HOME="$task_home" bash "$task_home/prepare.sh" "$task_pid"
kill -0 "$task_pid"
task_launcher="$(find "$task_home/pos-atk-prueba" -name iniciar-prueba.sh -print -quit)"
[[ -n "$task_launcher" ]]
HOME="$task_home" bash "$task_launcher"
kill -0 "$task_pid"
if HOME="$task_home" bash "$task_launcher" --iniciar > "$task_home/guard.log" 2>&1; then
    echo 'Launcher failed to refuse existing POS' >&2; exit 1
fi
grep -q 'CASHIER sigue abierto' "$task_home/guard.log"
kill -0 "$task_pid"
[[ ! -e MUST_NOT_EXIST ]]
# Parse the emitted exec line as Bash arguments without executing Java.
task_exec="$(grep '^exec /' "$task_launcher")"
task_exec="${task_exec#exec }"
eval "set -- ${task_exec%%> *}"
[[ "$1" == "$task_java" && "$2" == -Djavax.accessibility.assistive_technologies=org.GNOME.Accessibility.AtkWrapper ]]
[[ "$3" == '-Dtest.literal=$(touch MUST_NOT_EXIST)' && "$7" == 'argument with spaces' ]]
[[ "$5" == "$task_cwd/classes:$task_home/pos-atk-prueba/puente-corregido-v1/java-atk-wrapper.jar" ]]
[[ ! -e MUST_NOT_EXIST ]]
echo 'PASS: preparation never launched/stopped POS, default launcher inert, existing-process guard, exact argument quoting and classpath append.'
