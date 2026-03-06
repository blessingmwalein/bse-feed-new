@echo off
:: Maven Wrapper script for Windows
:: Downloads Maven if not present locally

setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd" (
    echo Downloading Apache Maven %MAVEN_VERSION%...
    if not exist "%MAVEN_HOME%" mkdir "%MAVEN_HOME%"
    
    curl -fsSL "%MAVEN_URL%" -o "%MAVEN_HOME%\maven.zip"
    if errorlevel 1 (
        echo Error: Failed to download Maven. Ensure curl is available.
        exit /b 1
    )
    
    powershell -Command "Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force"
    del /q "%MAVEN_HOME%\maven.zip"
    echo Maven %MAVEN_VERSION% installed.
)

set "MAVEN_HOME=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%"
set "PATH=%MAVEN_HOME%\bin;%PATH%"

mvn %*
