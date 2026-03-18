#!/bin/bash
set -e

echo "=== Installation TP2 IFT630 - STS Concurrent ==="

# Verifier Java
if ! command -v java &> /dev/null; then
    echo "ERREUR: Java 21 requis mais non installe"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "ERREUR: Java 21+ requis (version detectee: $JAVA_VERSION)"
    exit 1
fi

# Verifier Maven
if ! command -v mvn &> /dev/null; then
    echo "ERREUR: Maven requis mais non installe"
    exit 1
fi

# Creer dossiers
mkdir -p logs results scenarios

# Compiler
echo "Compilation avec Maven..."
mvn clean compile

echo "=== Installation terminee avec succes ==="
echo "Executez: make run"
