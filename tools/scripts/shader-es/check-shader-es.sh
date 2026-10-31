#!/usr/bin/env bash
# Converts every engine shader to GLSL ES 3.20 and validates both the desktop (4.10 core) and the ES
# versions with glslangValidator.
#
# Needs: a JDK (javac/java) and glslangValidator on PATH, or GLSLANG=/path/to/glslangValidator.
#   Linux:  apt install glslang-tools      (or: npm i glslang-validator-prebuilt-predownloaded)
#   macOS:  brew install glslang
#
# Usage: tools/scripts/shader-es/check-shader-es.sh
set -euo pipefail

root="$(cd "$(dirname "$0")/../../.." && pwd)"
shader_dir="$root/tt/src/main/java/com/oddlabs/tt/render/shader"
glslang="${GLSLANG:-glslangValidator}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

command -v "$glslang" >/dev/null || { echo "glslangValidator not found (set GLSLANG=...)" >&2; exit 2; }

mkdir -p "$work/classes" "$work/out"
javac -d "$work/classes" "$shader_dir/ShaderDialect.java" "$root/tools/scripts/shader-es/Assemble.java"
java -cp "$work/classes" Assemble "$shader_dir" "$work/out"

status=0
for kind in 410 es; do
  ok=0
  bad=0
  for f in "$work"/out/*-"$kind".*; do
    if "$glslang" "$f" >"$work/log" 2>&1; then
      ok=$((ok + 1))
    else
      bad=$((bad + 1))
      status=1
      echo "--- $(basename "$f")"
      head -8 "$work/log"
    fi
  done
  echo "== $kind: ok=$ok bad=$bad =="
done
exit $status
