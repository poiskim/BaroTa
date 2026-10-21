#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo 'Missing wrapper JAR. Run ./setup-wrapper.sh first.' >&2
  exit 1
fi
if [ -n "${JAVA_HOME:-}" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi
exec "$JAVA" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
