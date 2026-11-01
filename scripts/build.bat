@echo off
cd /d "%~dp0.."

if not defined JAVA_HOME (
  if exist "C:\Program Files\Java\jdk-13.0.2" set "JAVA_HOME=C:\Program Files\Java\jdk-13.0.2"
)

set MVN=d:\ispark_prj\tools\apache-maven-3.9.6\bin\mvn.cmd
if not exist "%MVN%" set MVN=mvn

echo JAVA_HOME=%JAVA_HOME%
call "%MVN%" -DskipTests package
if errorlevel 1 (
  echo Build failed
  pause
  exit /b 1
)
echo Build OK: target\flowdesk.war
pause
