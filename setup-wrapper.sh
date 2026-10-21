#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p gradle/wrapper
jar_file=gradle/wrapper/gradle-wrapper.jar
expected=81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f
if [ ! -f "$jar_file" ]; then
  curl -fLSs 'https://raw.githubusercontent.com/gradle/gradle/v8.13.0/gradle/wrapper/gradle-wrapper.jar' -o "$jar_file"
fi
actual=$(sha256sum "$jar_file" | cut -d ' ' -f 1)
if [ "$actual" != "$expected" ]; then
  rm -f "$jar_file"
  echo 'Gradle wrapper SHA-256 checksum mismatch.' >&2
  exit 1
fi
echo 'Gradle 8.13 official wrapper verified.'
