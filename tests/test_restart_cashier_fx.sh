#!/usr/bin/env bash
# All provider commands and Java are harmless fixtures, never real POS processes.
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_home="$(mktemp -d)"
mkdir -p "$task_home/bin" "$task_home/geopos/installations/geopos2oxxo-colombia-2.0.0.2.101/configuration" "$task_home/pos-fx-prueba/fx-lab-v1"
task_cwd="$task_home/geopos/installations/geopos2oxxo-colombia-2.0.0.2.101"
ln -s "$task_cwd" "$task_home/geopos/current"
cat > "$task_home/bin/java" <<'JAVA'
#!/bin/bash
if [[ "$1" == -version ]]; then echo 'java version "1.8.0_191"'; exit 0; fi
echo java >> "$TASK_TEST_HOME/events"
printf '%s\n' "$@" > "$TASK_TEST_HOME/arguments"
printf '%s\n' "$PWD" > "$TASK_TEST_HOME/workdir"
printf '%s\n' "$LD_LIBRARY_PATH" > "$TASK_TEST_HOME/libraries"
JAVA
printf '#!/bin/sh\necho "all dependencies resolved (fixture)"\n' > "$task_home/bin/ldd"
chmod +x "$task_home/bin/java" "$task_home/bin/ldd"
cat > "$task_home/iniciaGeopos.sh" <<'START'
#!/bin/bash
echo provider-initial-close >> "$TASK_TEST_HOME/events"
cd "$TASK_TEST_HOME/geopos/current"
sh runPos.sh
START
cat > "$task_cwd/terminatePos.sh" <<'STOP'
echo provider-pos-and-vlc-close >> "$TASK_TEST_HOME/events"
STOP
cat > "$task_cwd/runPos.sh" <<'POS'
export LD_LIBRARY_PATH="vendor-path:${LD_LIBRARY_PATH:-}"
sh terminatePos.sh
GEOPOS_RUNTIME="$TASK_TEST_HOME/bin/java"
${GEOPOS_RUNTIME} -Dfile.encoding=ISO-8859-1 -javaagent:./lib/agent.jar -classpath 'libs with spaces/*:other/*' '-Dliteral=$(touch MUST_NOT_EXIST)' uy.com.geocom.geopos.core.CVPOSInstance 'config file.xml' America/Bogota
POS
printf 'geopos.java.runtime=%s/bin/\n' "$task_home" > "$task_cwd/configuration/customization.properties"
cd "$task_home/pos-fx-prueba/fx-lab-v1"
printf fixture > fx-inspector.jar
sha256sum fx-inspector.jar > SHA256SUMS
cd "$task_home"
sed -e 's/\$HOME/\$TASK_TEST_HOME/g' -e "s|^task_java=.*|task_java='$task_home/bin/java'|" -e "s|^task_expected_host=.*|task_expected_host='$(hostname)'|" "$task_root/scripts/restart-cashier-fx.sh" > runner.sh
export TASK_TEST_HOME="$task_home" PATH="$task_home/bin:$PATH"
sha256sum iniciaGeopos.sh geopos/current/runPos.sh geopos/current/terminatePos.sh > before.sha256
export LD_LIBRARY_PATH=existing-vendor
bash runner.sh --preparar
[[ ! -f events ]]
bash runner.sh --iniciar
[[ "$(cat events)" == $'provider-initial-close\nprovider-pos-and-vlc-close\njava' ]]
grep -Fx -- '-Djavax.accessibility.assistive_technologies=' arguments
grep -Fx -- 'libs with spaces/*:other/*' arguments
grep -Fx -- '-Dliteral=$(touch MUST_NOT_EXIST)' arguments
grep -Fx -- '-javaagent:./lib/agent.jar' arguments
[[ ! -e MUST_NOT_EXIST && ! -e "$task_cwd/MUST_NOT_EXIST" ]]
[[ "$(readlink -f "$(cat workdir)")" == "$task_cwd" ]]
[[ "$(cat libraries)" == 'vendor-path:existing-vendor' ]]
IFS= read -r task_session < "$TASK_TEST_HOME/pos-fx-prueba/ultima-sesion-cashier.txt"
grep -Fx -- "-javaagent:$TASK_TEST_HOME/pos-fx-prueba/fx-lab-v1/fx-inspector.jar=$task_session" arguments
[[ -d "$task_session" ]]
[[ ! -e "$TASK_TEST_HOME/pos-fx-prueba/ultima-sesion.txt" ]]
! grep -E 'AtkWrapper|java-atk-wrapper' arguments
sha256sum -c before.sha256
# Reject unrecognized source before any of its normal closing operations.
printf '\n${GEOPOS_RUNTIME} -classpath x uy.com.geocom.geopos.core.CVPOSInstance\n' >> geopos/current/runPos.sh
task_events="$(cat events)"
if bash runner.sh --iniciar; then echo 'Unexpected acceptance of duplicate launch' >&2; exit 1; fi
[[ "$(cat events)" == "$task_events" ]]
echo 'PASS: preparation inert, normal restart order preserved, vendor native path/classpath/AspectJ args preserved; JavaFX agent added; demo session untouched, originals untouched, ambiguous source rejected before closure.'
