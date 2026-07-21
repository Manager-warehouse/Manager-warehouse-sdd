@echo off
cd /d "%~dp0"
set SPRING_PROFILES_ACTIVE=secrets
set PORT=8080
call .\mvnw.cmd spring-boot:run
