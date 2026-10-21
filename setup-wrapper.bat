@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-wrapper.ps1"
if errorlevel 1 (
  echo Wrapper setup failed. Check internet connection and retry.
  exit /b 1
)
echo Setup complete.
