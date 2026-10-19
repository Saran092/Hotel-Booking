@echo off
REM Builds the Angular frontend and copies the output into Spring's static/ folder.
setlocal

cd /d "%~dp0frontend"
call npm run build
if errorlevel 1 (
  echo Angular build failed.
  exit /b 1
)

set DEST=%~dp0src\main\resources\static
if exist "%DEST%" rmdir /s /q "%DEST%"
mkdir "%DEST%"
xcopy /e /y /q "dist\frontend\browser\*" "%DEST%\"
echo Frontend copied to %DEST%
endlocal
