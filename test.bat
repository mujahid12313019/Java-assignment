@echo off
setlocal
if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
)
set "PATH=%JAVA_HOME%\bin;C:\Program Files\apache-maven-3.9.6\bin;C:\Program Files\Git\cmd;%PATH%"

echo =========================================================================
echo  Executing Automated Test Suite (JUnit 5 & Concurrency Tests)
echo =========================================================================
echo.

call "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" test
pause
