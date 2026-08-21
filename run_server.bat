@echo off
REM ===================================================================
REM  Starts the server of the chain.
REM  Run compile.bat once before this script.
REM
REM  The port is taken from config.properties (server.port, 5000 by
REM  default). An optional argument overrides it:  run_server.bat 5050
REM ===================================================================

if not exist "%~dp0out" (
    echo The out folder was not found. Run compile.bat first.
    exit /b 1
)

java -cp "%~dp0out" server.core.ChainServer %1
