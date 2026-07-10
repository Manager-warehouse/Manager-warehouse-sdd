@echo off
cd /d "%~dp0"
set SPRING_PROFILES_ACTIVE=secrets
set PORT=8081
call .\mvnw.cmd spring-boot:run
