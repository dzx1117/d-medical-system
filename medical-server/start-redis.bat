@echo off

rem Start the bundled portable Redis on port 6379.
rem
rem NOTE: this file is intentionally pure ASCII. cmd.exe reads .bat files byte by
rem byte using the current OEM codepage, so any non-ASCII text (Chinese comments,
rem etc.) can be misparsed -- a UTF-8 lead/trail byte may land on 0x5C (backslash),
rem which cmd treats as a line-continuation, breaking the whole script.
rem Keep .bat files ASCII-only.

rem .tools lives one level ABOVE the repo root, and this script sits in
rem <repo>\medical-server\, so we need two levels up.
set "TOOLS=%~dp0..\..\.tools"

if not exist "%TOOLS%\redis\redis-server.exe" (
    echo ERROR: redis-server.exe not found at:
    echo   %TOOLS%\redis\redis-server.exe
    echo Make sure the .tools folder sits next to the repo folder.
    pause
    exit /b 1
)

echo Starting Redis on port 6379...
echo (keep this window open)
echo.
"%TOOLS%\redis\redis-server.exe" --port 6379 --save "" --appendonly no
