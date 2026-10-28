#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if ! command -v java >/dev/null 2>&1; then
  echo 'Necesitas Java 8 (1.8) o superior. Consulta README.md.' >&2; exit 2
fi
JAR="$ROOT/target/pos-accessibility-lab.jar"
if [[ ! -f "$JAR" ]]; then JAR="$ROOT/pos-accessibility-lab.jar"; fi
if [[ ! -f "$JAR" ]]; then
  echo 'No se encontró el JAR. Compila con mvn clean package o usa el ZIP portable.' >&2; exit 2
fi
exec java -Dfile.encoding=UTF-8 -jar "$JAR" "$@"
