@echo off
setlocal

rem Keep this file pure ASCII -- see the note in start-redis.bat.
rem Project-local toolchain, in .tools one level above the repo root.
set "TOOLS=%~dp0..\..\.tools"
set "JAVA_HOME=%TOOLS%\jdk-17.0.20.1+1"
set "MAVEN_HOME=%TOOLS%\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo ============================================
echo  Medical System - Backend
echo ============================================
echo JAVA_HOME  = %JAVA_HOME%
echo MAVEN_HOME = %MAVEN_HOME%
echo.

java -version
echo.

cd /d "%~dp0"
call mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles=local

endlocal
