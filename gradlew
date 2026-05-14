#!/usr/bin/env sh
# gradlew
APP_NAME="Gradle"
WRAPPER_DIR="${0%/*}/gradle/wrapper"
CLASSPATH="$WRAPPER_DIR/gradle-wrapper.jar"
set -e
if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper jar not found."
  exit 1
fi
if [ -z "$JAVA_HOME" ]; then
  if [ -x "/usr/libexec/java_home" ]; then
    JAVA_HOME="$(/usr/libexec/java_home)"
  fi
fi
if [ -n "$JAVA_HOME" ]; then
  JAVA_HOME="${JAVA_HOME%/}"
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="java"
fi
exec "$JAVA_CMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
