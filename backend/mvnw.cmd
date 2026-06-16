@echo off
setlocal

SET "MAVEN_WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar"
SET "MAVEN_PROJECTBASEDIR=%~dp0"

if exist "%MAVEN_WRAPPER_JAR%" (
    if defined JAVA_HOME (
        SET "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
        SET "JAVA_CMD=java"
    )

    "%JAVA_CMD%" -classpath "%MAVEN_WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
    exit /b %ERRORLEVEL%
)

mvn %*
