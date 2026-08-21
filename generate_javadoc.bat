@echo off
REM ===================================================================
REM  Builds the Javadoc of the whole system into the javadoc folder.
REM  Open javadoc\index.html when it finishes.
REM
REM  The test classes are left out: the Javadoc is the documentation of
REM  the system, and the tests are a development tool.
REM ===================================================================
setlocal enabledelayedexpansion

echo [1/3] Preparing the javadoc folder...
if exist javadoc rmdir /s /q javadoc

echo [2/3] Collecting the source files...
if exist jdoc-sources.txt del jdoc-sources.txt
call :collect "%~dp0src\common"
call :collect "%~dp0src\server"
call :collect "%~dp0src\client"

echo [3/3] Generating...
javadoc -encoding UTF-8 -charset UTF-8 -d javadoc -windowtitle "Clothing Store Chain" @jdoc-sources.txt
if errorlevel 1 (
    echo.
    echo JAVADOC FAILED.
    del jdoc-sources.txt
    exit /b 1
)

del jdoc-sources.txt
echo.
echo Done. Open javadoc\index.html
exit /b 0

:collect
if not exist "%~1" exit /b 0
for /r "%~1" %%f in (*.java) do (
    set "sourceFilePath=%%f"
    echo "!sourceFilePath:\=/!">>jdoc-sources.txt
)
exit /b 0
