@echo off
setlocal

rem Keep this file pure ASCII -- see the note in medical-server/start-redis.bat.

cd /d "%~dp0"

if not exist node_modules (
    echo [1/2] First run - installing dependencies...
    call npm install
    echo.
)

echo [2/2] Starting dev server...
echo Open: http://localhost:5173
echo.

call npm run dev

endlocal
