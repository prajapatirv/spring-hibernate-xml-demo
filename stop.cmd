@echo off
rem One-click stop: stop.cmd [-Port 8081]
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop.ps1" %*
exit /b %ERRORLEVEL%
