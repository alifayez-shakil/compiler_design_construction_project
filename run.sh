#!/usr/bin/env bash
# Compile and run the compiler against every test in tests/.
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p build/compiler
javac -d build/compiler src/compiler/*.java
java -cp build/compiler compiler.Main
