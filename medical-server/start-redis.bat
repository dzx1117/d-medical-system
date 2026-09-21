@echo off
chcp 65001 >nul

rem 启动本地 Redis（项目自带便携版，端口 6379）
set "TOOLS=%~dp0..\.tools"

if not exist "%TOOLS%\redis\redis-server.exe" (
    echo 未找到 Redis，请确认 .tools\redis 目录存在
    pause
    exit /b 1
)

echo 启动 Redis (端口 6379)...
"%TOOLS%\redis\redis-server.exe" --port 6379 --save "" --appendonly no
