#!/usr/bin/env bash

CURPATH=$(pwd)
TARGET_CLASSES_PATH="target/classes/lib/macos_x86-64"
TARGET_PATH="target"

function exitWithError() {
  cd ${CURPATH}
  echo "*** An error occurred. Please check log messages. ***"
  exit $1
}

mkdir -p "$TARGET_CLASSES_PATH"

cd "$TARGET_PATH"
cmake ../../../ || exitWithError $?
make || exitWithError $?
rm -f "$CURPATH/${TARGET_CLASSES_PATH}/libbrotli.dylib"
cp "./libbrotli.dylib" "$CURPATH/${TARGET_CLASSES_PATH}" || exitWithError $?

cd ${CURPATH}
