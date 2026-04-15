#!/bin/zsh
set -euo pipefail

# ---- Config ----
APP_NAME="Flappy Bird"
APP_VERSION="1.0.0"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
TARGET_DIR="$PROJECT_DIR/target"
DIST_DIR="$PROJECT_DIR/dist"
BUILD_DIR="$PROJECT_DIR/build/macos"
ICONSET_DIR="$BUILD_DIR/AppIcon.iconset"
ICON_SOURCE_ICO="$PROJECT_DIR/src/main/resources/sprites/favicon.ico"
ICON_1024_PNG="$BUILD_DIR/icon-1024.png"
ICNS_FILE="$BUILD_DIR/FlappyBird.icns"
MAIN_CLASS="com.kartersanamo.flappyBird.Main"

cd "$PROJECT_DIR"

echo "==> Checking required tools..."
command -v mvn >/dev/null || { echo "Error: mvn not found"; exit 1; }
command -v jpackage >/dev/null || { echo "Error: jpackage not found (install/use a full JDK)"; exit 1; }
command -v sips >/dev/null || { echo "Error: sips not found"; exit 1; }
command -v iconutil >/dev/null || { echo "Error: iconutil not found"; exit 1; }

echo "==> Building JAR with Maven..."
mvn clean package

# Pick the shaded app jar (exclude original-* and sources/javadoc jars)
JAR_FILE="$(ls -1 "$TARGET_DIR"/FlappyBird-*.jar 2>/dev/null | grep -v 'original-' | head -n 1 || true)"
if [[ -z "$JAR_FILE" ]]; then
  echo "Error: Could not find built FlappyBird jar in $TARGET_DIR"
  exit 1
fi
MAIN_JAR="$(basename "$JAR_FILE")"
echo "Using JAR: $MAIN_JAR"

echo "==> Preparing icon assets..."
mkdir -p "$ICONSET_DIR" "$DIST_DIR"

sips -s format png "$ICON_SOURCE_ICO" --out "$ICON_1024_PNG" >/dev/null

for size in 16 32 128 256 512; do
  sips -z "$size" "$size" "$ICON_1024_PNG" --out "$ICONSET_DIR/icon_${size}x${size}.png" >/dev/null
  sips -z $((size*2)) $((size*2)) "$ICON_1024_PNG" --out "$ICONSET_DIR/icon_${size}x${size}@2x.png" >/dev/null
done

iconutil -c icns "$ICONSET_DIR" -o "$ICNS_FILE"

echo "==> Packaging macOS app image..."
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --input "$TARGET_DIR" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --icon "$ICNS_FILE" \
  --dest "$DIST_DIR" \
  --app-version "$APP_VERSION" \
  --vendor "Karter Sanamo"

APP_PATH="$DIST_DIR/$APP_NAME.app"
if [[ ! -d "$APP_PATH" ]]; then
  echo "Error: App bundle not created at $APP_PATH"
  exit 1
fi

echo "==> Installing to /Applications (sudo required)..."
sudo rm -rf "/Applications/$APP_NAME.app"
sudo cp -R "$APP_PATH" "/Applications/"

echo "==> Done."
echo "Launch with:"
echo "open -a \"$APP_NAME\""
