#!/usr/bin/env sh
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"
MAIN_CLASS="org.gradle.wrapper.GradleWrapperMain"

run_with_system_gradle() {
  if command -v gradle >/dev/null 2>&1; then
    echo "Aviso: gradle-wrapper.jar no encontrado. Usando Gradle disponible en el sistema." >&2
    exec gradle "$@"
  fi
  echo "No se encontró gradle-wrapper.jar ni una instalación de Gradle en el PATH." >&2
  exit 1
}

if [ -f "$WRAPPER_JAR" ]; then
  if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXEC="$JAVA_HOME/bin/java"
  else
    JAVA_EXEC="$(command -v java || true)"
  fi

  if [ -z "$JAVA_EXEC" ]; then
    echo "No se encontró una instalación de Java para ejecutar Gradle." >&2
    exit 1
  fi

  exec "$JAVA_EXEC" -classpath "$WRAPPER_JAR" "$MAIN_CLASS" "$@"
else
  run_with_system_gradle "$@"
fi
