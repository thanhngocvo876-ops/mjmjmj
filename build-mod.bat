@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ================================================
echo   狐妖小红娘 Mod - 构建 JAR 文件
echo ================================================
echo.

REM 使用你电脑上已有的 Gradle 8.8（wrapper 缓存）
set "GRADLE_BIN=C:\Users\Administrator\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8\bin\gradle.bat"

if not exist "%GRADLE_BIN%" (
    echo [错误] 找不到 Gradle，请检查路径:
    echo %GRADLE_BIN%
    pause
    exit /b 1
)

echo [1/2] 正在构建，首次运行需要下载依赖，请耐心等待（约5-15分钟）...
echo.
"%GRADLE_BIN%" build --no-daemon --console=plain
if errorlevel 1 (
    echo.
    echo [失败] 构建出错！常见原因：
    echo   1. 网络问题导致 Forge 依赖下载失败（多试几次，或开代理）
    echo   2. Java 环境问题（需要 JDK 17）
    echo.
    pause
    exit /b 1
)

echo.
echo [2/2] 构建成功！
echo.
echo jar 文件在: %~dp0build\libs\
echo   （名字类似 mjmod-1.0.0.jar，要使用请选不带 -sources 和 -javadoc 的那个）
echo.
pause