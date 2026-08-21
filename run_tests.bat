@echo off
REM ===================================================================
REM  Compiles and runs the JUnit tests.
REM
REM  This is the ONLY script that uses a jar, and that jar is only a
REM  development tool: the system itself, compiled by compile.bat, does
REM  not depend on any external library.
REM
REM  If lib\junit-platform-console-standalone-1.10.2.jar is missing,
REM  download it once from Maven Central:
REM  https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar
REM ===================================================================
setlocal enabledelayedexpansion

set "JUNIT_JAR=%~dp0lib\junit-platform-console-standalone-1.10.2.jar"

if not exist "%JUNIT_JAR%" (
    echo The JUnit jar was not found at %JUNIT_JAR%
    echo Download it from Maven Central and place it in the lib folder.
    exit /b 1
)

echo [1/3] Preparing the test output folder...
REM The folder is deleted and rebuilt every time. Without this, a test class
REM that was renamed or removed would leave its old .class file behind, and the
REM runner would keep executing a test that no longer exists in the source.
if exist out-test rmdir /s /q out-test
mkdir out-test

echo [2/3] Collecting the source files, including the tests...
if exist test-sources.txt del test-sources.txt
for /r "%~dp0src" %%f in (*.java) do (
    set "sourceFilePath=%%f"
    echo "!sourceFilePath:\=/!">>test-sources.txt
)

echo [3/3] Compiling and running the tests...
javac -encoding UTF-8 -cp "%JUNIT_JAR%" -d out-test @test-sources.txt
if errorlevel 1 (
    echo.
    echo TEST COMPILATION FAILED.
    del test-sources.txt
    exit /b 1
)
del test-sources.txt

REM The tests start a real server, which reads and writes real files. They are
REM pointed at their own workspace folder so that a test can never leave a test
REM product or a test customer inside the demonstration data.
java -Dchainstore.workDir=test-workspace -jar "%JUNIT_JAR%" execute ^
     --class-path out-test --select-package test --details=tree
