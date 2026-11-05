@echo off
REM Minecraft AI Builder - double-click to start everything.
REM First run: creates .env, sets up Python, asks you to accept the Minecraft EULA.
setlocal
cd /d "%~dp0"
title Minecraft AI Builder

REM --- 1. Check required software ---
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed. Install Java 21 or newer from https://adoptium.net
    pause
    exit /b 1
)
py -3.12 --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python 3.12 is not installed. Install it from https://www.python.org/downloads/
    pause
    exit /b 1
)

REM --- 2. OpenAI API key ---
if not exist ".env" (
    copy ".env.example" ".env" >nul
    echo.
    echo FIRST RUN: Notepad is opening your .env file.
    echo Replace YOUR_API_KEY_HERE with your OpenAI API key, save, close Notepad,
    echo then double-click Start.bat again.
    echo.
    notepad ".env"
    pause
    exit /b 0
)
for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"
if "%API_KEY%"=="YOUR_API_KEY_HERE" (
    echo [ERROR] Put your OpenAI API key in the .env file first.
    notepad ".env"
    pause
    exit /b 1
)

REM --- 3. Python packages (first run only) ---
if not exist "service\.venv\Scripts\python.exe" (
    echo Setting up Python packages. First run only - this takes a few minutes...
    py -3.12 -m venv "service\.venv"
    if errorlevel 1 goto :fail
    "service\.venv\Scripts\python.exe" -m pip install -r "service\requirements.txt"
    if errorlevel 1 goto :fail
)

REM --- 4. Minecraft EULA (first run only) ---
findstr /c:"eula=true" "server\eula.txt" >nul 2>&1
if errorlevel 1 goto :eula
:eula_done

REM --- 5. Start the AI service (own window) and the Minecraft server (this window) ---
set PYTHONIOENCODING=utf-8
start "AI Service" /D "%~dp0service" cmd /k .venv\Scripts\python.exe main.py
cd /d "%~dp0server"
java -Xmx4G -Xms2G -jar paper.jar nogui
pause
exit /b 0

:eula
echo.
echo Running a Minecraft server requires accepting the Minecraft EULA:
echo   https://aka.ms/MinecraftEULA
set /p AGREE="Do you accept the Minecraft EULA? (Y/N): "
if /i not "%AGREE%"=="Y" (
    echo You must accept the EULA to run the server.
    pause
    exit /b 1
)
echo eula=true> "server\eula.txt"
goto :eula_done

:fail
echo [ERROR] Python setup failed. Delete the service\.venv folder and try again.
pause
exit /b 1
