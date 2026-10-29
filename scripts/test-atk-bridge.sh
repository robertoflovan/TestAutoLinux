#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_build="$task_root/target/atk-build"
task_logs="$task_build/test-logs"
mkdir -p "$task_logs" "$task_build/classes"
cd "$task_logs"
export AT_SPI_BUS_ADDRESS
AT_SPI_BUS_ADDRESS="$(gdbus call --session --dest org.a11y.Bus --object-path /org/a11y/bus --method org.a11y.Bus.GetAddress | cut -d "'" -f2)"
[[ "$AT_SPI_BUS_ADDRESS" == unix:* ]]
xprop -root -f AT_SPI_BUS 8s -set AT_SPI_BUS "$AT_SPI_BUS_ADDRESS"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -d "$task_build/classes" "$task_root/examples/PosSwingControl.java"
export LD_LIBRARY_PATH="$JAVA_HOME/jre/lib/amd64:$JAVA_HOME/jre/lib/amd64/server"
task_pid=''
cleanup() { if [[ -n "$task_pid" ]]; then kill "$task_pid" 2>/dev/null || true; wait "$task_pid" 2>/dev/null || true; fi; }
trap cleanup EXIT
launch() {
    local task_dir="$1"
    "$JAVA_HOME/bin/java" -Djava.library.path="$task_dir" -Djavax.accessibility.assistive_technologies=org.GNOME.Accessibility.AtkWrapper -cp "$task_build/classes:$task_dir/java-atk-wrapper.jar" PosSwingControl > "$2" 2>&1 &
    task_pid=$!
    sleep 5
    kill -0 "$task_pid"
}
check_total() {
    "$JAVA_HOME/bin/java" -jar "$task_root/pos-accessibility-lab.jar" check --backend jna --pid "$task_pid" --name TotalVenta --field text --equals "$1" --out "$task_logs/$2"
}
launch "$task_build/original" original.log
set +e
check_total 125.50 original-query
task_original_check=$?
sleep 5
kill -0 "$task_pid" 2>/dev/null
task_original_alive=$?
set -e
printf 'original_check_exit=%s original_liveness_exit=%s\n' "$task_original_check" "$task_original_alive" > comparison.txt
cleanup
task_pid=''
launch "$task_build/delivery" corrected.log
for task_i in 1 2 3; do
    check_total 125.50 "corrected-initial-$task_i"
    sleep 3
    kill -0 "$task_pid"
done
task_window="$(xdotool search --sync --pid "$task_pid" --name 'POS - Prueba Swing ATK' | head -n1)"
xdotool windowfocus --sync "$task_window"
xdotool key --clearmodifiers ctrl+a
xdotool type --clearmodifiers '1002'
xdotool key Return
sleep 2
for task_i in 1 2 3; do
    check_total 80.00 "corrected-changed-$task_i"
    sleep 3
    kill -0 "$task_pid"
done
sleep 10
kill -0 "$task_pid"
echo 'Corrected: six value checks passed, application remained alive after reader disconnects and final 10s wait.' | tee -a comparison.txt
cp comparison.txt "$task_build/delivery/TEST-RESULTS.txt"
