@echo off
REM FlowDesk 내장 실행 (H2, 포트 8080) — 운영은 WSL Tomcat WAR 권장
cd /d "%~dp0.."

if not defined JAVA_HOME (
  if exist "C:\Program Files\Java\jdk-13.0.2" set "JAVA_HOME=C:\Program Files\Java\jdk-13.0.2"
)

set MVN=d:\ispark_prj\tools\apache-maven-3.9.6\bin\mvn.cmd
if not exist "%MVN%" set MVN=mvn

echo Starting FlowDesk (spring-boot:run) on 0.0.0.0:8080 ...
call "%MVN%" -DskipTests spring-boot:run
