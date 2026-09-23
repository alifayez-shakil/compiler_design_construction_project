@echo off
cd /d "%~dp0"

if not exist build\compiler mkdir build\compiler

javac -d build\compiler src\compiler\*.java
if errorlevel 1 exit /b 1

java -cp build\compiler compiler.Main
