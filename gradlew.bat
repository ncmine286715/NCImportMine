@rem Gradle startup script for Windows

@if "%DEBUG%"=="" @echo off

set APP_NAME=Gradle
set APP_BASE_NAME=%~n0

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar

set JAVA_EXE=java.exe

%JAVA_EXE% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
