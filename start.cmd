@echo off
rem One-click start: start.cmd [-Port 8081] [-Rebuild]
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" %*
exit /b %ERRORLEVEL%
