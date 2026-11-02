#!/usr/bin/env bash
# Prepare private copies of the customer's normal restart chain.
# --iniciar explicitly runs that chain, including its normal POS/VLC termination.
set -euo pipefail
umask 077
task_mode="${1:---preparar}"
[[ "$task_mode" == --preparar || "$task_mode" == --iniciar ]] || { echo 'Uso: --preparar o --iniciar' >&2; exit 1; }
task_java='/usr/java/jdk1.8.0_191/bin/java'
task_expected_host='QACOL-CERT-VIRREY'
task_bridge="$HOME/pos-atk-prueba/puente-corregido-v1"
task_start="$HOME/iniciaGeopos.sh"
task_current="$HOME/geopos/current"
die() { printf 'ERROR: %s\nNo se ha ejecutado el cierre habitual.\n' "$*" >&2; exit 1; }
[[ "$(hostname)" == "$task_expected_host" ]] || die 'Este lanzador corresponde al equipo Colombia validado.'
for task_tool in awk sha256sum readlink mktemp flock ldd diff; do command -v "$task_tool" >/dev/null || die "Falta $task_tool"; done
task_cwd="$(readlink -f "$task_current")"
[[ "$task_cwd" == */geopos2oxxo-colombia-2.0.0.2.101 ]] || die 'La instalacion actual no coincide con la revisada.'
for task_file in "$task_start" "$task_current/runPos.sh" "$task_current/terminatePos.sh" "$task_current/configuration/customization.properties" "$task_bridge/java-atk-wrapper.jar" "$task_bridge/libatk-wrapper.so"; do
    [[ -r "$task_file" ]] || die "No se puede leer $task_file"
done
[[ -x "$task_java" ]] || die "No existe el Java esperado: $task_java"
task_version="$("$task_java" -version 2>&1)"
[[ "$task_version" == *'version "1.8.0_191"'* ]] || die 'La version de Java difiere de 8u191.'
task_runtime="$(awk -F= '/^[ \t]*geopos[.]java[.]runtime[ \t]*=/ {sub(/^[^=]*=/, ""); gsub(/^[ \t]+|[ \t\r]+$/, ""); value=$0} END {print value}' "$task_current/configuration/customization.properties")"
if [[ -n "$task_runtime" ]]; then task_config_java="${task_runtime}java"; else task_config_java="$(command -v java || true)"; fi
[[ "$(readlink -f "$task_config_java")" == "$task_java" ]] || die 'La configuracion normal selecciona otro Java; requiere revision.'
[[ -z "${JAVA_TOOL_OPTIONS:-}${_JAVA_OPTIONS:-}${JDK_JAVA_OPTIONS:-}" ]] || die 'Hay opciones Java de entorno adicionales; requiere revision.'
(cd "$task_bridge" && sha256sum -c SHA256SUMS) || die 'Fallo la integridad del puente.'
task_dependencies="$(LD_LIBRARY_PATH="$task_bridge:${task_java%/bin/java}/jre/lib/amd64:${task_java%/bin/java}/jre/lib/amd64/server${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" ldd "$task_bridge/libatk-wrapper.so" 2>&1)" || die "$task_dependencies"
[[ "$task_dependencies" != *'not found'* ]] || die "$task_dependencies"
bash -n "$task_start" || die 'Error de sintaxis en iniciaGeopos.sh'
bash -n "$task_current/runPos.sh" || die 'Error de sintaxis en runPos.sh'
task_plan="$(mktemp -d "$HOME/pos-atk-prueba/reinicio-puente-XXXXXXXX")"
# Validate a unique, known launch line. Do not guess how to rewrite other layouts.
awk '
    /^[ \t]*sh[ \t]+runPos[.]sh[ \t]*$/ {print "sh \"$POS_ATK_RUN_COPY\""; count++; next}
    {print}
    END {if (count != 1) exit 1}
' "$task_start" > "$task_plan/iniciaGeopos-con-puente.sh" || die 'El lanzador no contiene una unica llamada sh runPos.sh reconocible.'
awk -v java="$task_java" '
    index($0, "javax.accessibility.assistive_technologies") || index($0, "java-atk-wrapper.jar") {exit 2}
    index($0, "${GEOPOS_RUNTIME}") && index($0, "uy.com.geocom.geopos.core.CVPOSInstance") {
        if (sub(/-classpath[ \t]+/, "-Djavax.accessibility.assistive_technologies=org.GNOME.Accessibility.AtkWrapper -classpath \"$POS_ATK_BRIDGE/java-atk-wrapper.jar\":") != 1) exit 2
        sub(/\$\{GEOPOS_RUNTIME\}/, "\"" java "\"")
        count++
    }
    {print}
    END {if (count != 1) exit 1}
' "$task_current/runPos.sh" > "$task_plan/runPos-con-puente.sh" || die 'No se reconoce una unica linea Java completa; no se ejecutara.'
bash -n "$task_plan/iniciaGeopos-con-puente.sh"
bash -n "$task_plan/runPos-con-puente.sh"
sha256sum "$task_start" "$task_current/runPos.sh" "$task_current/terminatePos.sh" "$task_current/configuration/customization.properties" > "$task_plan/originales.sha256"
{
    printf 'Java: %s\nInstalacion: %s\nPuente: %s\n' "$task_java" "$task_cwd" "$task_bridge"
    echo 'Con --iniciar se conserva el cierre normal de CASHIER, callvlc.sh y VLC.'
    echo 'Se conservan los parametros y los logs/output.log del procedimiento habitual.'
    for task_pair in inicio pos; do
        if [[ "$task_pair" == inicio ]]; then task_a="$task_start"; task_b="$task_plan/iniciaGeopos-con-puente.sh"; else task_a="$task_current/runPos.sh"; task_b="$task_plan/runPos-con-puente.sh"; fi
        diff -u "$task_a" "$task_b" || [[ $? == 1 ]]
    done
} > "$task_plan/cambios.txt"
printf 'Copias preparadas: %s\nCambios: %s/cambios.txt\n' "$task_plan" "$task_plan"
if [[ "$task_mode" == --preparar ]]; then
    echo 'Solo preparacion: no se ha cerrado ni iniciado ninguna aplicacion.'
    exit 0
fi
exec 9>"$HOME/pos-atk-prueba/cashier-atk.lock"
flock -n 9 || die 'Hay otro lanzador de prueba activo.'
[[ "$(readlink -f "$task_current")" == "$task_cwd" ]] || die 'La instalacion cambio durante la preparacion.'
sha256sum -c "$task_plan/originales.sha256" || die 'Cambio un archivo original durante la preparacion.'
export POS_ATK_BRIDGE="$task_bridge"
export POS_ATK_RUN_COPY="$task_plan/runPos-con-puente.sh"
export LD_LIBRARY_PATH="$task_bridge${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
echo 'Ejecutando la secuencia de reinicio habitual con el puente. Incluye sus cierres de CASHIER y VLC.'
cd "$HOME"
exec bash "$task_plan/iniciaGeopos-con-puente.sh"
