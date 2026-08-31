@echo off
REM ===================================================================
REM  Starts one SWING GUI client of the chain.
REM  Run compile.bat once, and start run_server.bat, before this script.
REM ===================================================================

if not exist "%~dp0out" (
    echo The out folder was not found. Run compile.bat first.
    exit /b 1
)

start "Clothing Chain GUI Client" java -cp "%~dp0out" client.gui.LoginFrame
