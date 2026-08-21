@echo off
REM ===================================================================
REM  Compiles the system into the out folder.
REM  Requires nothing but an installed JDK - no Maven, no Gradle, no IDE.
REM
REM  The src\test folder is NOT compiled here on purpose: the tests are
REM  the only code that needs the JUnit jar, and the system itself must
REM  compile and run without any external library at all.
REM  Use run_tests.bat to compile and run the tests.
REM
REM  Note about sources.txt: javac reads the file list from an argument
REM  file, and inside such a file a backslash is an escape character.
REM  The paths are therefore written with forward slashes, which Windows
REM  accepts just as well, and wrapped in quotes so that a folder name
REM  containing spaces still works.
REM ===================================================================
setlocal enabledelayedexpansion

echo [1/3] Preparing the output folder...
if not exist out mkdir out

echo [2/3] Collecting the source files...
if exist sources.txt del sources.txt
call :collect "%~dp0src\common"
call :collect "%~dp0src\server"
call :collect "%~dp0src\client"

echo [3/3] Compiling...
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo COMPILATION FAILED. Fix the errors above and run compile.bat again.
    del sources.txt
    exit /b 1
)

del sources.txt
echo.
echo Compilation finished successfully. The classes are in the out folder.
exit /b 0

:collect
if not exist "%~1" exit /b 0
for /r "%~1" %%f in (*.java) do (
    set "sourceFilePath=%%f"
    echo "!sourceFilePath:\=/!">>sources.txt
)
exit /b 0
