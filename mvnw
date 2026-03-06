#!/bin/sh
# Maven Wrapper script for Unix/Linux/macOS/Git Bash
# Downloads Maven if not present locally

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_HOME="${MAVEN_PROJECTBASEDIR}/.mvn/wrapper/maven"
MAVEN_VERSION="3.9.9"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"

if [ ! -d "${MAVEN_HOME}/apache-maven-${MAVEN_VERSION}" ]; then
    echo "Downloading Apache Maven ${MAVEN_VERSION}..."
    mkdir -p "${MAVEN_HOME}"
    
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL "${MAVEN_URL}" -o "${MAVEN_HOME}/maven.zip"
    elif command -v wget > /dev/null 2>&1; then
        wget -q "${MAVEN_URL}" -O "${MAVEN_HOME}/maven.zip"
    else
        echo "Error: Neither curl nor wget found."
        exit 1
    fi
    
    unzip -qo "${MAVEN_HOME}/maven.zip" -d "${MAVEN_HOME}"
    rm -f "${MAVEN_HOME}/maven.zip"
    echo "Maven ${MAVEN_VERSION} installed."
fi

export MAVEN_HOME="${MAVEN_HOME}/apache-maven-${MAVEN_VERSION}"
export PATH="${MAVEN_HOME}/bin:${PATH}"

exec mvn "$@"
