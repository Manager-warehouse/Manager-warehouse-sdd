@echo off
SET JAVA_CMD="C:\Program Files\Java\jdk-21.0.11\bin\java.exe"
SET MAVEN_WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
SET MAVEN_PROJECTBASEDIR=%~dp0

%JAVA_CMD% -classpath %MAVEN_WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
