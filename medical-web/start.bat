@echo off
chcp 65001 >nul
setlocal

echo ============================================
echo  智慧医院管理系统 - 前端启动
echo ============================================
echo.

cd /d "%~dp0"

if not exist node_modules (
    echo [1/2] 首次运行，正在安装依赖...
    call npm install
    echo.
)

echo [2/2] 启动开发服务器...
echo 访问地址: http://localhost:5173
echo.

call npm run dev

endlocal
