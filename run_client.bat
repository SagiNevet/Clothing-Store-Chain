@echo off
REM ===================================================================
REM  Starts one client of the chain.
REM  Run compile.bat once, and start run_server.bat, before this script.
REM
REM  Run this script TWICE to demonstrate two branches at the same time:
REM  log in as 1001 (Tel Aviv) in one window and 2001 (Jerusalem) in the
REM  other. The server address and port come from config.properties.
REM ===================================================================

if not exist "%~dp0out" (
    echo The out folder was not found. Run compile.bat first.
    exit /b 1
)

start "Chain Client" java -cp "%~dp0out" client.gui.LoginFrame
