@echo off
REM Fala Ja - one-click start for Windows
chcp 65001 >nul
cd /d "%~dp0"

REM --- find Python ("py" launcher first, then "python") ---
set "PY=py -3"
%PY% --version >nul 2>nul || set "PY=python"
%PY% --version >nul 2>nul || goto nopython
echo Using:
%PY% --version

REM --- install packages ---
echo.
echo Installing packages (only slow the first time)...
%PY% -m pip install --disable-pip-version-check -q -r requirements.txt
if errorlevel 1 (
  echo.
  echo Package installation failed. Please send the messages above to Claude.
  pause
  exit /b 1
)

REM --- create .env with the API keys ---
if not exist ".env" (
  copy ".env.example" ".env" >nul
  echo.
  echo Notepad opens now: enter your two API keys after the "=" signs,
  echo then SAVE ^(Ctrl+S^) and CLOSE Notepad to continue.
  notepad ".env"
)

REM --- open the browser shortly after the server starts ---
start "" cmd /c "timeout /t 4 >nul & start http://localhost:8000"

echo.
echo Server is starting. Keep this window open while you use the app.
echo Close it to stop the app.
echo.
%PY% -m uvicorn server:app --port 8000
echo.
echo The server stopped. If there is an error above, send it to Claude.
pause
exit /b 0

:nopython
echo.
echo Python was not found.
echo Install it from the page that opens now. IMPORTANT: tick
echo "Add python.exe to PATH" at the bottom of the first installer screen.
echo Then double-click this file again.
start "" https://www.python.org/downloads/
pause
exit /b 1
