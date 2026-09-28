# test.ps1 - PowerShell test runner
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\apache-maven-3.9.6\bin;C:\Program Files\Git\cmd;$env:PATH"

Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host " Running Automated Tests (JUnit 5 + Concurrency Suite) " -ForegroundColor Green
Write-Host "=========================================================================" -ForegroundColor Cyan

& "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" test
