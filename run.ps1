# run.ps1 - PowerShell launcher for Multithreaded Desktop Task Management System
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;C:\Program Files\apache-maven-3.9.6\bin;C:\Program Files\Git\cmd;$env:PATH"

Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host " Launching Multithreaded Desktop Task Management System (JavaFX) " -ForegroundColor Green
Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host "JAVA_HOME: $env:JAVA_HOME"
Write-Host ""

& "C:\Program Files\apache-maven-3.9.6\bin\mvn.cmd" javafx:run
