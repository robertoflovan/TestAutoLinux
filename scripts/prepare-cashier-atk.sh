#!/usr/bin/env bash
# Reads an existing CASHIER process and creates a separate, reviewable launcher.
# This script never stops or launches the POS and never executes its vendor scripts.
set -euo pipefail
umask 077
task_pid="${1:-}"
task_main='uy.com.geocom.geopos.core.CVPOSInstance'
task_java='/usr/java/jdk1.8.0_191/bin/java'
task_bridge="$HOME/pos-atk-prueba/puente-corregido-v1"
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ "$task_pid" =~ ^[0-9]+$ ]] || die 'Uso: bash prepare-cashier-atk.sh PID_ACTUAL_DE_CASHIER'
for task_tool in readlink sha256sum mktemp flock pgrep; do command -v "$task_tool" >/dev/null || die "Falta $task_tool; no se instalara nada."; done
[[ -r "/proc/$task_pid/cmdline" && -r "/proc/$task_pid/environ" ]] || die 'No se puede leer ese proceso con el usuario actual.'
task_exe="$(readlink -f "/proc/$task_pid/exe")"
[[ "$task_exe" == "$task_java" ]] || die "El proceso usa $task_exe; se esperaba $task_java."
task_cwd="$(readlink -f "/proc/$task_pid/cwd")"
task_current="$(readlink -f "$HOME/geopos/current")"
[[ "$task_cwd" == "$task_current" ]] || die 'La carpeta del proceso no coincide con geopos/current.'
[[ "$task_cwd" == */geopos2oxxo-colombia-2.0.0.2.101 ]] || die 'Esta preparacion corresponde al ambiente Colombia 2.0.0.2.101.'
[[ -f "$task_bridge/libatk-wrapper.so" && -f "$task_bridge/java-atk-wrapper.jar" ]] || die 'Falta el puente corregido v1.'
(cd "$task_bridge" && sha256sum -c SHA256SUMS) || die 'Fallo la integridad del puente.'
task_before="$(cat "/proc/$task_pid/cmdline" | sha256sum)"
mapfile -d '' -t task_args < "/proc/$task_pid/cmdline"
task_found=0
task_cp_index=-1
for ((task_i=1; task_i<${#task_args[@]}; task_i++)); do
    task_arg="${task_args[$task_i]}"
    if [[ "$task_arg" == "$task_main" ]]; then task_found=1; break; fi
    case "$task_arg" in
        -cp|-classpath) task_cp_index=$((task_i+1)); ((task_i+=1));;
        -Djavax.accessibility.assistive_technologies=*|-Djava.library.path=*) die 'Ya hay configuracion de accesibilidad o java.library.path; requiere revision.';;
    esac
done
[[ "$task_found" == 1 && "$task_cp_index" -gt 0 ]] || die 'No se reconoce el comando CASHIER con classpath explicito.'
task_env=()
task_ld=''
while IFS= read -r -d '' task_entry; do
    case "$task_entry" in
        LD_LIBRARY_PATH=*) task_ld="${task_entry#*=}";;
        GEOPOS_HOME=*|JRE_HOME=*|PATH=*|LANG=*|LC_ALL=*|LC_CTYPE=*|TZ=*) task_env+=("$task_entry");;
        JAVA_TOOL_OPTIONS=?*|_JAVA_OPTIONS=?*|JDK_JAVA_OPTIONS=?*) die 'El POS usa opciones Java de entorno; deben revisarse antes de preparar la prueba.';;
    esac
done < "/proc/$task_pid/environ"
[[ "$(cat "/proc/$task_pid/cmdline" | sha256sum)" == "$task_before" ]] || die 'El proceso cambio mientras se leia; repetir con el PID actual.'
task_args[0]="$task_java"
task_args[$task_cp_index]="${task_args[$task_cp_index]}:$task_bridge/java-atk-wrapper.jar"
task_args=("${task_args[0]}" '-Djavax.accessibility.assistive_technologies=org.GNOME.Accessibility.AtkWrapper' "${task_args[@]:1}")
task_env+=("LD_LIBRARY_PATH=$task_bridge${task_ld:+:$task_ld}")
task_plan="$(mktemp -d "$HOME/pos-atk-prueba/cashier-plan-XXXXXXXX")"
task_launcher="$task_plan/iniciar-prueba.sh"
{
    printf '#!/usr/bin/env bash\nset -euo pipefail\numask 077\n'
    printf 'task_expected_host=%q\ntask_cwd=%q\ntask_bridge=%q\ntask_plan=%q\n' "$(hostname)" "$task_cwd" "$task_bridge" "$task_plan"
    cat <<'LAUNCHER'
if [[ "${1:-}" != --iniciar ]]; then
    echo 'Este archivo es un lanzador de prueba. Revisar primero configuracion.txt.'
    echo 'Solo despues de cerrar CASHIER normalmente: bash iniciar-prueba.sh --iniciar'
    exit 0
fi
[[ "$(hostname)" == "$task_expected_host" ]] || { echo 'Equipo distinto al de preparacion.' >&2; exit 1; }
[[ "$(readlink -f "$HOME/geopos/current")" == "$task_cwd" ]] || { echo 'La instalacion current cambio; volver a preparar.' >&2; exit 1; }
exec 9>"$HOME/pos-atk-prueba/cashier-atk.lock"
flock -n 9 || { echo 'Ya existe un lanzador de prueba activo.' >&2; exit 1; }
if pgrep -f '[u]y.com.geocom.geopos.core.CVPOSInstance' >/dev/null; then
    echo 'CASHIER sigue abierto. No se cerrara ni se iniciara otra instancia.' >&2
    exit 1
fi
(cd "$task_bridge" && sha256sum -c SHA256SUMS)
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS
cd "$task_cwd"
LAUNCHER
    for task_entry in "${task_env[@]}"; do printf 'export %q\n' "$task_entry"; done
    printf 'printf "PID de la prueba CASHIER: %%s\\n" "$$"\n'
    printf 'printf "Log de la prueba: %%s\\n" %q\n' "$task_plan/cashier.log"
    printf 'exec '
    printf '%q ' "${task_args[@]}"
    printf '> %q 2>&1\n' "$task_plan/cashier.log"
} > "$task_launcher"
bash -n "$task_launcher"
{
    printf 'Equipo: %s\nPID de referencia: %s\nJava: %s\nCarpeta: %s\nPuente: %s\n' "$(hostname)" "$task_pid" "$task_exe" "$task_cwd" "$task_bridge"
    printf '\nArgumentos originales conservados; se agrega la propiedad de accesibilidad y el JAR del puente.\n'
    printf 'Se conserva LD_LIBRARY_PATH del proceso, agregando el puente al principio.\n'
    printf 'El lanzador no llama a iniciaGeopos.sh, runPos.sh ni terminatePos.sh.\n'
    printf 'Se ejecutara en primer plano; conservar la terminal abierta durante la prueba.\n'
    printf 'Los logs Java van a esta carpeta; no se reemplaza logs/output.log.\n'
    printf 'Se conservan los argumentos activos; si cambia la configuracion, volver a preparar.\n\n'
    cat "$task_launcher"
} > "$task_plan/configuracion.txt"
printf '\nPREPARADO. CASHIER no se ha cerrado ni iniciado.\nReferencia para revisar: %s/configuracion.txt\n' "$task_plan"
