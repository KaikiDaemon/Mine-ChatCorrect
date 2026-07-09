#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.14"
PROJECT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_HOME="$PROJECT_DIR/.gradle/local/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
DIST_DIR="$PROJECT_DIR/.gradle/local"
DIST_ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
DIST_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_DIR"

  if [ ! -f "$DIST_ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -L "$DIST_URL" -o "$DIST_ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$DIST_ZIP" "$DIST_URL"
    else
      echo "Neither curl nor wget is available to download Gradle." >&2
      exit 1
    fi
  fi

  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$DIST_ZIP" -d "$DIST_DIR"
  elif command -v python3 >/dev/null 2>&1; then
    python3 - "$DIST_ZIP" "$DIST_DIR" <<'PY'
import sys
import zipfile

zip_path = sys.argv[1]
out_dir = sys.argv[2]

with zipfile.ZipFile(zip_path) as zf:
    zf.extractall(out_dir)
PY
  elif command -v python >/dev/null 2>&1; then
    python - "$DIST_ZIP" "$DIST_DIR" <<'PY'
import sys
import zipfile

zip_path = sys.argv[1]
out_dir = sys.argv[2]

with zipfile.ZipFile(zip_path) as zf:
    zf.extractall(out_dir)
PY
  else
    echo "unzip, python3, or python is required to extract Gradle." >&2
    exit 1
  fi
fi

exec "$GRADLE_BIN" "$@"
