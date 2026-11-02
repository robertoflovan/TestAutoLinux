#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_out="$task_root/target/fx-lab-build"
mkdir -p "$task_out/classes" "$task_out/fx-lab-v1"
javac -source 8 -target 8 -encoding UTF-8 -d "$task_out/classes" "$task_root"/fx-lab/src/mx/demo/posfx/*.java
printf 'Manifest-Version: 1.0\nPremain-Class: mx.demo.posfx.FxAgent\nMain-Class: mx.demo.posfx.FxClient\n\n' > "$task_out/agent.mf"
jar cfm "$task_out/fx-lab-v1/fx-inspector.jar" "$task_out/agent.mf" -C "$task_out/classes" mx/demo/posfx/FilesProtocol.class -C "$task_out/classes" mx/demo/posfx/FxSnapshot.class -C "$task_out/classes" mx/demo/posfx/FxAgent.class -C "$task_out/classes" mx/demo/posfx/FxClient.class
printf 'Manifest-Version: 1.0\nMain-Class: mx.demo.posfx.FxDemo\n\n' > "$task_out/demo.mf"
jar cfm "$task_out/fx-lab-v1/fx-demo.jar" "$task_out/demo.mf" -C "$task_out/classes" mx/demo/posfx/FxDemo.class
cp "$task_root"/fx-lab/{probar.sh,leer.sh,README.md} "$task_out/fx-lab-v1/"
cp -r "$task_root/fx-lab/src" "$task_out/fx-lab-v1/"
(cd "$task_out/fx-lab-v1" && sha256sum fx-inspector.jar fx-demo.jar > SHA256SUMS)
mkdir -p "$task_out/fx-reader-v1.2/src/mx/demo/posfx"
printf 'Manifest-Version: 1.0\nMain-Class: mx.demo.posfx.FxClient\n\n' > "$task_out/reader.mf"
jar cfm "$task_out/fx-reader-v1.2/fx-reader.jar" "$task_out/reader.mf" -C "$task_out/classes" mx/demo/posfx/FilesProtocol.class -C "$task_out/classes" mx/demo/posfx/FxClient.class
cp "$task_root"/fx-lab/reader/{leer-cashier.sh,README.md} "$task_out/fx-reader-v1.2/"
cp "$task_root"/fx-lab/src/mx/demo/posfx/{FxClient.java,FilesProtocol.java} "$task_out/fx-reader-v1.2/src/mx/demo/posfx/"
(cd "$task_out/fx-reader-v1.2" && sha256sum fx-reader.jar > SHA256SUMS)
