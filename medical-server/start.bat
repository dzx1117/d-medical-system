@echo off
chcp 65001 >nul
setlocal

rem 项目本地工具链（首次运行由 Claude 下载到 .tools 目录）
set "TOOLS=%~dp0..\.tools"
set "JAVA_HOME=%TOOLS%\jdk-17.0.20.1+1"
set "MAVEN_HOME=%TOOLS%\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo ============================================
echo  智慧医院管理系统 - 后端启动
echo ============================================
echo JAVA_HOME  = %JAVA_HOME%
echo MAVEN_HOME = %MAVEN_HOME%
echo.

java -version
echo.

cd /d "%~dp0"
call mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles=local

endlocal
