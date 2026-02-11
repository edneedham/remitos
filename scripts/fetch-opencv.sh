#!/bin/sh
set -e

OPENCV_VERSION="4.12.0"
ZIP_NAME="opencv-${OPENCV_VERSION}-android-sdk.zip"
ZIP_URL="https://github.com/opencv/opencv/releases/download/${OPENCV_VERSION}/${ZIP_NAME}"
TARGET_DIR="opencv-sdk"

mkdir -p "$TARGET_DIR"

curl -L -o "$ZIP_NAME" "$ZIP_URL"
unzip -q "$ZIP_NAME" -d "$TARGET_DIR"
rm "$ZIP_NAME"

echo "OpenCV SDK extracted to $TARGET_DIR/OpenCV-android-sdk"
