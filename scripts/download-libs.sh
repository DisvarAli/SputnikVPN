#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LIBS_DIR="$ROOT/app/libs"
OUT_FILE="$LIBS_DIR/libv2ray.aar"
URL="https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.5.19/libv2ray.aar"

mkdir -p "$LIBS_DIR"

if [[ -f "$OUT_FILE" ]] && [[ $(stat -c%s "$OUT_FILE" 2>/dev/null || stat -f%z "$OUT_FILE") -gt 50000000 ]]; then
  echo "libv2ray.aar already present"
  exit 0
fi

echo "Downloading libv2ray.aar..."
curl -fsSL "$URL" -o "$OUT_FILE"
echo "Done: $OUT_FILE"
