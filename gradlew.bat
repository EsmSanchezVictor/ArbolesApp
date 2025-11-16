@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
SET DIR=%~dp0
SET WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
SET MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain

IF EXIST "%WRAPPER_JAR%" (
    IF DEFINED JAVA_HOME (
        SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
        IF EXIST "%JAVA_EXE%" (
            "%JAVA_EXE%" -classpath "%WRAPPER_JAR%" %MAIN_CLASS% %*
            EXIT /B %ERRORLEVEL%
        )
    )
    java -classpath "%WRAPPER_JAR%" %MAIN_CLASS% %*
    EXIT /B %ERRORLEVEL%
)

ECHO Aviso: gradle-wrapper.jar no encontrado. Se usara Gradle del sistema. 1>&2
WHERE gradle >NUL 2>&1
IF ERRORLEVEL 1 (
    ECHO No hay instalacion de Gradle disponible en el PATH. 1>&2
    EXIT /B 1
)
gradle %*
EXIT /B %ERRORLEVEL%
